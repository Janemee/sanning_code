package com.huimi.core.service.task.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.huimi.common.baseMapper.GenericMapper;
import com.huimi.common.baseMapper.GenericPo;
import com.huimi.common.entity.ResultEntity;
import com.huimi.common.entity.dtgrid.DtGrid;
import com.huimi.common.tools.StringUtil;
import com.huimi.common.utils.*;
import com.huimi.core.constant.ConfigNID;
import com.huimi.core.constant.Constants;
import com.huimi.core.constant.EnumConstants;
import com.huimi.core.entity.LiveHotSubTask;
import com.huimi.core.exception.BussinessException;
import com.huimi.core.mapper.liveHotSubTask.LiveHotSubTaskMapper;
import com.huimi.core.mapper.task.TaskDetailsMapper;
import com.huimi.core.mapper.task.TaskMapper;
import com.huimi.core.model.LiveHotSubTask.LiveHotSubTaskModel;
import com.huimi.core.po.comment.CommentTemplate;
import com.huimi.core.po.equipment.Equipment;
import com.huimi.core.po.equipment.EquipmentModel;
import com.huimi.core.po.system.Admin;
import com.huimi.core.service.apkHistory.BizApkHistoryService;
import com.huimi.core.service.cache.RedisService;
import com.huimi.core.service.comment.CommentTemplateService;
import com.huimi.core.service.equipment.EquipmentService;
import com.huimi.core.service.liveHotSubTask.LiveHotSubTaskService;
import com.huimi.core.service.system.AdminService;
import com.huimi.core.service.task.TaskDetailsService;
import com.huimi.core.service.task.TaskService;
import com.huimi.core.task.*;
import com.huimi.core.util.TaskUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

@Service
@Scope("prototype")
@Transactional(rollbackFor = Exception.class)
public class TaskServiceImpl implements TaskService {
    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);
    @Resource
    private TaskMapper taskMapper;
    @Resource
    private TaskDetailsService taskDetailsService;
    @Resource
    private RedisService redisService;
    @Resource
    private AdminService adminService;
    @Resource
    private BizApkHistoryService bizApkHistoryService;
    @Resource
    private LiveHotSubTaskService liveHotSubTaskService;
    @Resource
    private EquipmentService equipmentService;
    @Resource
    private CommentTemplateService commentTemplateService;
    @Resource
    private TaskDetailsMapper taskDetailsMapper;
    @Resource
    private LiveHotSubTaskMapper liveHotSubTaskMapper;

    public static String remarks = "?????????????????????????????????????????????";

    @Override
    public DtGrid findOneDetailed(DtGrid dtGrid) {
        HashMap<String, String> hashMap = new HashMap<>();
        if (dtGrid.getNcColumnsType() != null && dtGrid.getNcColumnsType().size() > 0) {
            for (String key : dtGrid.getNcColumnsType().keySet()) {
                for (int i = 0; i < dtGrid.getNcColumnsType().get(key).size(); i++) {
                    hashMap.put((String) dtGrid.getNcColumnsType().get(key).get(i), key);
                }
                dtGrid.setNcColumnsTypeList(hashMap);
            }
        }
        // ??????????????????Sql
        String whereSql = dtGrid.getWhereSql();
        // ????????????Sql
        String sortSql = dtGrid.getSortSql();

        Map<String, Object> params = new HashMap<>();
        params.put("whereSql", whereSql);
        params.put("sortSql", sortSql);
        long recordCount = taskMapper.findOneDetailedCount(params);
        int pageSize = dtGrid.getPageSize();
        int pageNum = (int) recordCount / dtGrid.getPageSize() + (recordCount % dtGrid.getPageSize() > 0 ? 1 : 0);

        dtGrid.setPageCount(pageNum);
        dtGrid.setRecordCount(recordCount);

        params.put("nowPage", (dtGrid.getNowPage() - 1) * pageSize);
        params.put("pageSize", pageSize);

        List<TaskDetails> list = taskMapper.findOneDetailed(params);
        dtGrid.setExhibitDatas(JsonUtils.toGenericObject(JsonUtils.toJson(list), new TypeReference<List<Object>>() {
        }));
        return dtGrid;
    }

    @Override
    public DtGrid findAll(Integer isLiveHot) {
        List<Task> retList = taskMapper.findAll(isLiveHot);
        for (Task task : retList) {
            if (StringUtils.isBlank(task.getTaskStartTime())) {
                task.setTaskStartTime(task.getCreateTime());
            }
        }
        DtGrid dtGrid = new DtGrid();

        dtGrid.setExhibitDatas(JsonUtils.toGenericObject(JsonUtils.toJson(retList), new TypeReference<List<Object>>() {
        }));
        return dtGrid;
    }

    @Override
    public DtGrid findLiveHotTask() {
        return null;
    }

    @Override
    public List<Task> findLiveHeart(Integer adminId, String platform) {
        Admin admin = adminService.selectByPrimaryKey(adminId);
        List<Task> liveHeart;
        if (StringUtils.isBlank(admin.getParentId())) {
            List<String> roleAdmin = adminService.findRoleAdmin("1");
            liveHeart = taskMapper.findLiveHeartAdmin(roleAdmin, platform);
        } else {
            liveHeart = taskMapper.findLiveHeart(admin.getCode(), platform);
        }

        if (liveHeart != null) {
            for (Task task : liveHeart) {
                task.setTaskType(EnumConstants.TaskType.LIVE_HOT.desc + task.getId());
            }
        }
        return liveHeart;
    }

    @Override
    public List<Task> findLiveTrobleHeart(Integer adminId) {
        Admin admin = adminService.selectByPrimaryKey(adminId);
        List<Task> liveHeart;
        if (StringUtils.isBlank(admin.getParentId())) {
            liveHeart = taskMapper.findLiveTrobleHeart(null);
        } else {
            liveHeart = taskMapper.findLiveTrobleHeart(admin.getCode());
        }

        if (liveHeart != null) {
            for (Task task : liveHeart) {
                task.setTaskType(EnumConstants.TaskType.LIVE_HOT.desc + task.getId());
            }
        }
        return liveHeart;
    }

    @Override
    public GenericMapper<Task, Integer> _getMapper() {
        return taskMapper;
    }

    /**
     * ??????????????????
     *
     * @param deviceUid ????????????
     * @param type      ????????????
     */
    @Override
    public List<TaskModel> findCodeTask(String deviceUid, EnumConstants.TaskType type, String platform) {
        List<TaskModel> list = new ArrayList<>();
        //??????????????????
        TaskAllModel taskAllModel = this.getTaskAllModel(deviceUid, type, platform);
        if (taskAllModel == null) {
            return list;
        }
        list.add(getTaskModel(taskAllModel));
        return list;
    }

    /**
     * ??????????????????
     *
     * @param deviceUid ????????????
     */
    @Override
    public List<Object> findTikTokTask(String deviceUid, EnumConstants.TaskType type, String platform) {
        HashMap<String, Object> dataMap = new HashMap<>();
        //?????????????????????
        List<Object> list = new ArrayList<>();
        //????????????
        TaskAllModel taskAllModel = this.getTaskAllModel(deviceUid, type, platform);
        //??????????????? ???????????????
        if (taskAllModel == null) {
            return list;
        }
        TaskDataTowModel taskDataTowModel = new TaskDataTowModel();
        taskDataTowModel.setTask_id(taskAllModel.getTaskDetailUuid());
        taskDataTowModel.setTask_type(taskAllModel.getTaskType());
        //????????????
        dataMap.put("task_over_date", taskAllModel.getTaskEndTime());
        if (EnumConstants.TaskType.OVER == type) {
            //????????????
            EquipmentModel equipmentModel = new EquipmentModel(deviceUid, taskAllModel.getTaskDetailUuid(), taskAllModel.getTaskType());
            equipmentModel.setTask_over_date(taskAllModel.getTaskEndTime());
            list.add(equipmentModel);
            return list;
        }

        //????????????
        if (EnumConstants.TaskType.EQUIPMENT_UPGRADE == type) {
            //???????????????????????????????????????
            String downLoadApkUrl = redisService.get(ConfigNID.HistoryStateApkRedisUrl);
            //?????????????????????
            String downLoadQrCode = redisService.get(ConfigNID.HistoryStateQcCodeRedisUrl);
            EquipmentModel equipmentModel = new EquipmentModel();
            equipmentModel.setDevice_code(deviceUid);
            equipmentModel.setTask_type(type.code);
            equipmentModel.setTask_id(taskAllModel.getTaskDetailUuid());
            equipmentModel.setDown_load_apk_url(downLoadApkUrl);
            equipmentModel.setDown_load_qrCode_url(downLoadQrCode);
            equipmentModel.setApk_upgrade_type(EnumConstants.EquipmentUpgradeType.getCode(taskAllModel.getApkUpgrade()));
            equipmentModel.setTask_over_date(taskAllModel.getTaskEndTime());
            list.add(equipmentModel);
            return list;
        }

        if (EnumConstants.TaskType.ONE_KEY_EXPLOSIVE_POWDER.code.equals(type.code)) {
            //????????????
            //????????????
            dataMap.put("task_time", taskAllModel.getTaskStartTime());
            //????????????
            dataMap.put("task_run_code", taskAllModel.getTaskRunCode());
            //????????????
            dataMap.put("comment_template", getCommentTemplateModel(taskAllModel));
            taskDataTowModel.setTask_data(dataMap);
        }
        if (EnumConstants.TaskType.MATRIX_PUSH_FLOW.code.equals(type.code)) {
            //????????????
            //????????????
            dataMap.put("task_time", taskAllModel.getTaskStartTime());
            //????????????
            dataMap.put("task_content", taskAllModel.getComment());
            //????????????
            dataMap.put("task_run_code", taskAllModel.getTaskRunCode());
            //????????????
            dataMap.put("content", taskAllModel.getLiveInContent());
            //????????????
            dataMap.put("comment_template", getCommentTemplateModel(taskAllModel));
            taskDataTowModel.setTask_data(dataMap);
        }
        if (EnumConstants.TaskType.PRIVATE_LETTER.code.equals(type.code)) {
            //??????
            //????????????
            dataMap.put("number", taskAllModel.getNumber());
            dataMap.put("letter_type", taskAllModel.getLetterType());
            taskDataTowModel.setTask_data(dataMap);
        }
        if (EnumConstants.TaskType.SAME_CITY_DRAINAGE.code.equals(type.code)) {
            //????????????
            //????????????
            dataMap.put("task_time", taskAllModel.getTaskStartTime());
            //????????????
            dataMap.put("task_content", taskAllModel.getComment());
            //????????????
            dataMap.put("task_run_code", taskAllModel.getTaskRunCode());
            //????????????
            dataMap.put("city", taskAllModel.getLiveInContent());
            //????????????
            dataMap.put("comment_template", getCommentTemplateModel(taskAllModel));
            taskDataTowModel.setTask_data(dataMap);
        }
        if (EnumConstants.TaskType.AUTO.code.equals(type.code)) {
            //????????????
            //????????????
            dataMap.put("task_time", taskAllModel.getTaskStartTime());
            //????????????
            dataMap.put("task_content", taskAllModel.getComment());
            //????????????
            dataMap.put("task_run_code", taskAllModel.getTaskRunCode());
            //?????????????????????
            dataMap.put("vertical_domain_keywords", taskAllModel.getLiveInContent());
            //????????????
            dataMap.put("comment_template", getCommentTemplateModel(taskAllModel));
            taskDataTowModel.setTask_data(dataMap);
        }

        if (EnumConstants.TaskType.AUTO.code.equals(type.code)) {
            list.add(getTaskModel(taskAllModel));
        }
        if (EnumConstants.TaskType.PRIVATE_LETTER.code.equals(type.code)) {
            list.add(getTaskModel(taskAllModel));
        }
        return list;
    }


    /**
     * ????????????????????????????????????
     */
    public TaskAllModel getTaskAllModel(String deviceUid, EnumConstants.TaskType taskType, String platform) {
        //???????????????
        TaskAllModel taskAllModel = taskMapper.findByTaskTypeAndDeviceUid(deviceUid, taskType.code, EnumConstants.taskStatus.RUN.value, platform);
        //??????????????????(????????????)
        if (taskType.type == EnumConstants.TaskType.LIVE_HOT.type) {
            taskAllModel = taskMapper.findCodeTask(deviceUid, taskType.code, EnumConstants.taskStatus.RUN.value, platform);
        }
        return taskAllModel;
    }

    /**
     * ????????????????????????
     */
    public TaskModel getTaskModel(TaskAllModel taskAllModel) {
        //?????????
        TaskModel taskModel = new TaskModel();
        //?????????
        TaskDataModel taskDataModel = new TaskDataModel();
        //?????????
        CommentTemplateModel commentTemplateModel = new CommentTemplateModel();
        taskDataModel.setTask_type(taskAllModel.getTaskType());

        taskDataModel.setComment_interval(taskAllModel.getCommentInterval() + "");
        taskDataModel.setTask_content(taskAllModel.getTaskContent());
        taskDataModel.setTask_expect_running(taskAllModel.getTaskExpectRunning() + "");
        taskDataModel.setTask_run_code(taskAllModel.getTaskRunCode());
        taskDataModel.setNumber(taskAllModel.getNumber() + "");
        taskDataModel.setLetter_type(taskAllModel.getLetterType());
        taskDataModel.setTask_start_time(DateUtil.dateStr(taskAllModel.getTaskStartTime(), "yyyy-MM-dd HH:mm:ss"));
        taskDataModel.setTask_end_time(DateUtil.dateStr(taskAllModel.getTaskEndTime(), "yyyy-MM-dd HH:mm:ss"));
        //??????????????????
        taskDataModel.setLive_in_content(taskAllModel.getAnalysisContent());
        taskDataModel.setLive_in_type(taskAllModel.getLiveInType());
        taskDataModel.setComment_interval(taskAllModel.getCommentInterval() + "");

        commentTemplateModel.setName(taskAllModel.getName());
        commentTemplateModel.setComment(taskAllModel.getComment());
        commentTemplateModel.setLive(taskAllModel.getLive());
        commentTemplateModel.setTurn(taskAllModel.getTurns());
        commentTemplateModel.setLetter(taskAllModel.getLetter());

        taskDataModel.setComment_template(commentTemplateModel);
        taskDataModel.setDevice_code(taskAllModel.getTaskDetailUuid());
        taskDataModel.setKey_word(taskAllModel.getKeyWord());

        //???????????????????????? ?????????????????????
        String overTaskDate = (DateUtil.dateStr(DateUtils.rollMinute(new Date(), 10), "yyyyMMdd HH:mm:ss"));
        //????????????????????????
        if (taskAllModel.getTaskExpectRunning() != null && taskAllModel.getTaskExpectRunning() > 0) {
            //???????????? + ????????????
            overTaskDate = (DateUtil.dateStr(DateUtils.rollMinute(new Date(), taskAllModel.getTaskExpectRunning()), "yyyyMMdd HH:mm:ss"));
        }

        taskDataModel.setTask_over_date(overTaskDate);
        taskModel.setTask_id(taskAllModel.getTaskDetailUuid());
        taskModel.setTask_type(taskAllModel.getTaskType());
        taskModel.setTask_data(taskDataModel);
        return taskModel;
    }

    public CommentTemplateModel getCommentTemplateModel(TaskAllModel taskAllModel) {
        //?????????
        CommentTemplateModel commentTemplateModel = new CommentTemplateModel();
        commentTemplateModel.setName(taskAllModel.getName());
        commentTemplateModel.setComment(taskAllModel.getComment());
        commentTemplateModel.setLive(taskAllModel.getLive());
        commentTemplateModel.setTurn(taskAllModel.getTurns());
        commentTemplateModel.setLetter(taskAllModel.getLetter());
        return commentTemplateModel;
    }


    /**
     * ????????????
     *
     * @param deviceUid  ??????uid
     * @param taskType   ????????????
     * @param taskId     ??????od
     * @param taskStatus ????????????
     * @param comment    ????????????
     */
    @Override
    public void taskNotify(String deviceUid, EnumConstants.TaskType taskType, String taskId, String taskStatus, String comment) {
        //?????????????????????
        String status = EnumConstants.taskStatus.getValue(taskStatus);
        String remarks = "??????????????????????????????" + EnumConstants.taskStatus.getDesc(status);
        //????????? ??????????????????????????????
        if (taskId.matches("^[0-9]*$")) {
            //?????????????????????
            int flag = liveHotSubTaskService.updateByLiveHotTaskState(deviceUid, taskId, status, remarks);
            if (flag == 0) {
                throw new BussinessException("??????????????????");
            }
            return;
        }

        boolean flag = taskDetailsService.updateTaskDetailState(taskId, status);
        if (!flag) {
            throw new BussinessException("??????????????????");
        }
        //????????????????????????
        EnumConstants.TaskContentType taskContentType = EnumConstants.TaskContentType.getTaskType(comment);
        //??????????????? ??????????????????????????????
        if (!EnumConstants.taskStatus.DONE.value.equals(status) && taskContentType == null
                && !EnumConstants.TaskType.AUTO.equals(taskType)) {
            return;
        }
        //??????????????????
        TaskDetails taskDetails = taskDetailsService.findByUuid(taskId, deviceUid);
        //???????????????????????????????????????
        //????????????
        if (EnumConstants.TaskContentType.LOOK.code.equals(comment)) {
            //?????????????????????
            taskDetails.setPlaytimes(taskDetails.getPlaytimes() + 1);
        }
        //????????????
        if (EnumConstants.TaskContentType.CLICK.code.equals(comment)) {
            taskDetails.setFabulous(taskDetails.getFabulous() + 1);
        }
        //????????????
        if (EnumConstants.TaskContentType.LOOK_INDEX.code.equals(comment)) {
            taskDetails.setHomepages(taskDetails.getHomepages() + 1);
        }
        //????????????
        if (EnumConstants.TaskContentType.FORWARD.code.equals(comment)) {
            taskDetails.setForwards(taskDetails.getForwards() + 1);
        }
        //????????????
        if (EnumConstants.TaskContentType.FOLLOW.code.equals(comment)) {
            taskDetails.setFollows(taskDetails.getFollows() + 1);
        }
        //????????????
        if (EnumConstants.TaskContentType.PRIVATE_LETTER.code.equals(comment)) {
            taskDetails.setLetters(taskDetails.getLetters() + 1);
        }
        //????????????
        if (EnumConstants.TaskContentType.LOOK.code.equals(comment)) {
            taskDetails.setComments(taskDetails.getComments() + 1);
        }
        taskDetails.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        taskDetails.setRemarks(remarks);
        if (taskDetailsService.updateByPrimaryKeySelective(taskDetails) == 0) {
            throw new BussinessException("??????????????????");
        }
    }

    @Override
    public void addSuperHeatTask(Task task, ArrayList<Equipment> equipmentArrayList, String millis) {
        if (null == task) {
            throw new BussinessException("??????????????????");
        }
        this.insert(task);
        //??????????????????
        List<TaskDetails> taskDetailsList = new ArrayList<>();
        //????????????????????????
        Integer states = 0;
        if (!EnumConstants.TaskRunCode.DELAY.code.equals(task.getTaskRunCode())) {
            states = 1;
        }
        //?????????????????????uid
        List<String> deviceUidList = new ArrayList<>();
        for (Equipment equipment : equipmentArrayList) {
            TaskDetails taskDetails = new TaskDetails();

            String taskValue = "task_" + EnumConstants.TaskType.LIVE_HOT.value + "_" + equipment.getDeviceUid() + "_" + millis;
            //??????redis??????key
            taskDetails.setTaskId(task.getId());
            taskDetails.setDeviceUid(equipment.getDeviceUid());
            taskDetails.setTaskType(EnumConstants.TaskType.LIVE_HOT.value);
            taskDetails.setState(states);
            taskDetails.setPlatform(task.getPlatform());
            taskDetails.setTaskDetailUuid(taskValue);
            taskDetailsList.add(taskDetails);
            //???????????? ??????uid:??????id?????????UID????????????????????????,??????ID???????????????
            deviceUidList.add(equipment.getDeviceUid() + ":" + taskValue);
        }
        //???????????? ???????????????IO??????
        if (0 == taskDetailsMapper.insertList(taskDetailsList)) {
            throw new BussinessException("????????????????????????????????????");
        }
        System.out.println("????????????----" + System.currentTimeMillis());
        //??????????????????
        if (states == 1) {
            sendSuperHotTaskBySocket(equipmentArrayList, task, millis);
        }
    }

    @Override
    public void addInteraction(Task task, ArrayList<Equipment> allEquipments) {
        //??????????????????
        TaskDetails taskDetails = new TaskDetails();
        this.insert(task);
        for (Equipment equipment : allEquipments) {
            String key = EnumConstants.TaskType.LIVE_CHAT.value + ":" + equipment.getDeviceUid();
            String taskValue = "task_" + EnumConstants.TaskType.LIVE_CHAT.value + "_" +
                    equipment.getDeviceUid() + "_" + System.currentTimeMillis();
            redisService.put(key, "task_id:" + taskValue, 5); //????????????????????????5???
            taskDetails.setTaskId(task.getId());
            taskDetails.setDeviceUid(equipment.getDeviceUid());
            taskDetails.setState(1);
            taskDetails.setTaskType(EnumConstants.TaskType.LIVE_CHAT.value);
            taskDetails.setTaskDetailUuid(taskValue);
            taskDetails.setPlatform(task.getPlatform());
            taskDetailsService.insert(taskDetails);

        }

    }

    @Override
    public void addFans(Task task, ArrayList<Equipment> allEquipments, String light) {
        TaskDetails taskDetails = new TaskDetails();
        try {
            this.insert(task);
            for (Equipment equipment : allEquipments) {
                String key = EnumConstants.PLAT_FROM_TYPE.KUAISHOU.value + ":" + EnumConstants.TaskType.JOIN_FAN_GROUP.value + ":" + equipment.getDeviceUid();
                String taskKey = "task_" + EnumConstants.TaskType.JOIN_FAN_GROUP.value + "_" + equipment.getDeviceUid() + "_" + System.currentTimeMillis();
                redisService.put(key, "task_id:" + taskKey, 5); //????????????????????????5???
                taskDetails.setTaskId(task.getId());
                taskDetails.setDeviceUid(equipment.getDeviceUid());
                taskDetails.setState(1);
//                taskDetails.setGiveLight(Integer.valueOf(light));
                taskDetails.setTaskType(EnumConstants.TaskType.JOIN_FAN_GROUP.value);
                taskDetails.setTaskDetailUuid(taskKey);
                taskDetails.setPlatform(task.getPlatform());
                taskDetailsService.insert(taskDetails);
            }


        } catch (BussinessException e) {
            throw new BussinessException(e.getMessage());
        }
    }

    /**
     * ????????????
     *
     * @param allEquipments ????????????
     * @param taskType      ????????????
     */
    @Override
    public Task addTask(ArrayList<Equipment> allEquipments, EnumConstants.TaskType taskType, TaskAdminPramsModel taskAdminPramsModel, Integer adminId) {
        long start = System.currentTimeMillis();
        //????????????
        Task task = new Task();
        task.setTaskType(taskType.code);
        task.setDelFlag(GenericPo.DELFLAG.NO.code);
        String taskRunCode = StringUtil.isBlank(taskAdminPramsModel.getTaskRunCode()) ? EnumConstants.TaskRunCode.NOW.code : taskAdminPramsModel.getTaskRunCode();
        //??????????????????????????????  ???????????????????????????
        String taskContent = StringUtil.isBlank(taskAdminPramsModel.getTaskContent()) ? taskType.desc : taskAdminPramsModel.getTaskContent();

        int taskExpectRunning = taskAdminPramsModel.getTaskExpectRunning() == null || taskAdminPramsModel.getTaskExpectRunning() == 0 ? EnumConstants.ExpireTime.zero.value : taskAdminPramsModel.getTaskExpectRunning();
        task.setCommentInterval(taskAdminPramsModel.getCommentInterval());
        task.setTaskExpectRunning(taskExpectRunning);
        task.setCommentTemplateId(taskAdminPramsModel.getCommentTemplateId());
        task.setTaskContent(taskContent);
        task.setTaskRunCode(taskRunCode);
        task.setTaskName(taskAdminPramsModel.getTaskName());
        task.setSysAdminId(adminId);
        task.setPlatform(taskAdminPramsModel.getPlatform());
        task.setLiveInContent(taskAdminPramsModel.getLiveInContent());
        if (StringUtils.isNotBlank(taskAdminPramsModel.getLiveInContent())) {
            String analysisContent = TaskUtil.saveLiveInContent(taskAdminPramsModel.getLiveInContent());
            task.setAnalysisContent(analysisContent);
        }
        task.setCreateTime(new Timestamp(System.currentTimeMillis()));
        task.setIsLiveHot(taskAdminPramsModel.getIsLiveHot());
        if (EnumConstants.TaskType.EQUIPMENT_UPGRADE.equals(taskType)) {
            //??????????????????????????????apk??????
            if (bizApkHistoryService.findByStateOne(EnumConstants.HistoryState.YES.value) == null) {
                throw new BussinessException("?????????????????????apk??????????????????apk???????????????");
            }
        }
        //???????????????????????????
        if (1 == taskType.taskType) {
            task.setIsLiveHot(1);
        }
        //????????????
        int state = Integer.parseInt(EnumConstants.taskStatus.RUN.value);
        //????????????????????????????????????
        if (taskRunCode.equals(EnumConstants.TaskRunCode.NOW.code)) {
            task.setTaskStartTime(new Timestamp(System.currentTimeMillis()));
            task.setTaskStartTime(new Date());
            Calendar nowTime = Calendar.getInstance();
            nowTime.add(Calendar.MINUTE, taskExpectRunning);
            task.setTaskEndTime(nowTime.getTime());
        } else {
            state = (Integer.parseInt(EnumConstants.taskStatus.WAIT.value));
            task.setTaskStartTime(taskAdminPramsModel.getTaskStartDateTime());
            Calendar instance = Calendar.getInstance();
            instance.setTime(taskAdminPramsModel.getTaskStartDateTime());
            instance.add(Calendar.MINUTE, taskExpectRunning);
            task.setTaskEndTime(instance.getTime());
        }
        //????????????
        if (this.insert(task) == 0) {
            throw new BussinessException("??????????????????");
        }
        for (Equipment equipment : allEquipments) {
            String longTimeStr = "task_" + taskType.value + "_" + equipment.getDeviceUid() + "_" + System.currentTimeMillis();
            //??????????????????
            TaskDetails taskDetails = new TaskDetails();
            taskDetails.setCity(taskAdminPramsModel.getCity());
            taskDetails.setApkUpgrade(taskAdminPramsModel.getApkUpgrade());
            taskDetails.setTaskId(task.getId());
            taskDetails.setDeviceUid(equipment.getDeviceUid());
            taskDetails.setState(state);
            taskDetails.setNumber(taskAdminPramsModel.getNumber());
            taskDetails.setLetterType(taskAdminPramsModel.getLetterType());
            taskDetails.setTaskType(taskType.value);
            taskDetails.setTaskDetailUuid(longTimeStr);
            taskDetails.setKeyWord(taskAdminPramsModel.getKeyWord());
            taskDetails.setPlatform(task.getPlatform());
            if (EnumConstants.TaskType.OVER.equals(taskType)) {
                //????????????????????????????????? ?????????????????????
                taskDetailsService.updateByEquipmentUid(equipment.getDeviceUid(), EnumConstants.taskStatus.DONE.value, "?????????????????????????????????????????????????????????");
                //??????????????? ??????????????????
            }
            //??????????????????
            taskDetailsService.insert(taskDetails);
        }
        //????????????
        sendTaskBySocket(allEquipments, task);
        long end = System.currentTimeMillis();
        System.out.println("????????????" + allEquipments.size() + "--?????????????????????---?????????" + (end - start) + "????????????" + (DateUtil.dateStr4(start) + "????????????" + DateUtil.dateStr4(end)));
        return task;
    }

    void sendTaskBySocket(ArrayList<Equipment> allEquipments, Task task) {
        for (Equipment equipment : allEquipments) {
            //??????????????????
            List<Object> tikTokTask = findTikTokTask(equipment.getDeviceUid(), EnumConstants.TaskType.getTaskType(task.getTaskType()), task.getPlatform());
            ResultEntity<List<Object>> resultEntity = new ResultEntity<>();
            resultEntity.setCode(ResultEntity.SUCCESS);
            resultEntity.setData(tikTokTask);
            resultEntity.setMsg("success");
            resultEntity.setTotal(tikTokTask.size());
            String channelId = redisService.get(Constants.DEVICE_CHANNEL + equipment.getDeviceUid());
            System.out.println("deviceId:" + equipment.getDeviceUid() + ",channelId:" + channelId);
            resultEntity.setUuid(channelId);
            resultEntity.setUrl(equipment.getDeviceUid());
            //????????????
            redisService.publish(Constants.TIKTOK_TASK, JSON.toJSONString(resultEntity));
            log.info("????????????????????????????????????" + task.getTaskType() + "???????????????" + task.getTaskName() + "????????????" + equipment.getDeviceCode());
        }
    }

    /**
     * ??????????????????
     *
     * @param allEquipments ????????????
     * @param taskType      ????????????
     */
    @Override
    public List<LiveHotSubTask> addImmediatelyTask(ArrayList<Equipment> allEquipments, EnumConstants.TaskType taskType, TaskAdminPramsModel taskAdminPramsModel) {
        this.checkedDetailsPrams(taskAdminPramsModel, allEquipments);
        Integer taskDetailIdState = 0;
        List<LiveHotSubTask> liveHotSubTasks = new ArrayList<>();
        for (Equipment equipment : allEquipments) {
            LiveHotSubTask liveHotSubTask = new LiveHotSubTask();
            if (taskType.equals(EnumConstants.TaskType.INSANE_CLICK)) {
                LiveHotSubTask insaneClickSubTask = new LiveHotSubTask();
                insaneClickSubTask.setTaskType(taskType.value);
                insaneClickSubTask.setTaskDetailId(getHotTaskId(taskAdminPramsModel.getHeart(), equipment.getDeviceUid()));
                insaneClickSubTask.setState(EnumConstants.TaskRunCode.NOW.value);
                List<LiveHotSubTask> insaneClick = liveHotSubTaskService.select(insaneClickSubTask);
                if (insaneClick.size() > 0) {
                    continue;
                }
            }

            liveHotSubTask.setTaskType(taskType.value);
            liveHotSubTask.setDeviceUid(equipment.getDeviceUid());
            liveHotSubTask.setState(Integer.parseInt(EnumConstants.taskStatus.RUN.value));
            liveHotSubTask.setPlatform(taskAdminPramsModel.getPlatform());
            //?????????????????????????????????????????????
            if (taskType.value.equals(EnumConstants.TaskType.OVER.value) && taskAdminPramsModel.getOverType() == (EnumConstants.overType.SUBSIDIARY.value)) {
                Integer taskDetailId = getHotTaskId(taskAdminPramsModel.getHeart(), equipment.getDeviceUid());
                List<LiveHotSubTask> toTaskDetailsId = liveHotSubTaskService.findToTaskDetailsId(taskDetailId, EnumConstants.taskStatus.RUN.value, EnumConstants.TaskType.OVER.value);
                StringBuilder liveInContent = new StringBuilder();
                for (int i = 0; i < toTaskDetailsId.size(); i++) {
                    if (i == toTaskDetailsId.size() - 1) {
                        liveInContent.append(toTaskDetailsId.get(i).getUuid());
                    } else {
                        liveInContent.append(toTaskDetailsId.get(i).getUuid()).append(",");
                    }
                    liveHotSubTask.setLiveInContent(liveInContent.toString());
                }
                liveHotSubTaskService.updateByTaskDetailsId(taskDetailId, EnumConstants.taskStatus.DONE.value, remarks, EnumConstants.TaskType.OVER.value);
            } else if (taskType.value.equals(EnumConstants.TaskType.OVER.value) && taskAdminPramsModel.getOverType() == (EnumConstants.overType.MAINTASK.value)) {
                TaskDetails taskDetails = new TaskDetails();
                taskDetails.setId(getHotTaskId(taskAdminPramsModel.getHeart(), equipment.getDeviceUid()));
                taskDetails.setState(Integer.valueOf(EnumConstants.taskStatus.DONE.value));
                taskDetails.setRemarks("????????????????????????");
                taskDetailsService.updateByIdAndVersionSelective(taskDetails);
                taskDetailIdState = taskDetails.getId();
            } else {
                liveHotSubTask.setLiveInContent(taskAdminPramsModel.getLiveInContent());
            }

            Integer taskDetailId = this.getHotTaskId(taskAdminPramsModel.getHeart(), equipment.getDeviceUid());
            liveHotSubTask.setTaskDetailId(taskDetailId);
            //????????????????????????????????????????????????????????????????????????id
            if (taskDetailIdState != 0) {
                liveHotSubTask.setTaskDetailId(taskDetailIdState);
            }
            liveHotSubTask.setGiftBox(taskAdminPramsModel.getGiftBox());
            liveHotSubTask.setGiftNumber(taskAdminPramsModel.getGiftNumber());
            liveHotSubTask.setGiftPage(taskAdminPramsModel.getGiftPage());
            liveHotSubTask.setRedEnvelopeTime(taskAdminPramsModel.getRedEnvelopeTime());
            liveHotSubTask.setGiveLight(taskAdminPramsModel.getLight());
            //??????????????? ??????????????????
            liveHotSubTask.setUuid(System.currentTimeMillis() + RandomUtils.randomNumbers(5));
            liveHotSubTask.setTaskExpectRunning(taskAdminPramsModel.getTaskExpectRunning());
            liveHotSubTask.setCommentInterval(taskAdminPramsModel.getCommentInterval());
            liveHotSubTask.setMakeListNumber(taskAdminPramsModel.getMakeListNum());
            liveHotSubTask.setClickNumber(taskAdminPramsModel.getClickNumber());
            liveHotSubTasks.add(liveHotSubTask);
        }
        if (CollectionUtil.isNotEmpty(liveHotSubTasks)) {
            //??????sql???????????? ???????????????
            liveHotSubTaskMapper.insertList(liveHotSubTasks);
            //??????redis??????/????????????????????????
            sendSubTaskBySocket(liveHotSubTasks);
        }
        return liveHotSubTasks;
    }

    public Integer getHotTaskId(Long taskId, String deviceUid) {
        TaskDetails taskDetails = taskDetailsService.findByTaskId(taskId, deviceUid);
        if (taskDetails == null) {
            return 0;
        }
        return taskDetails.getId();
    }

    /**
     * ???????????????????????????????????????
     */
    public void checkedDetailsPrams(TaskAdminPramsModel taskAdminPramsModel, ArrayList<Equipment> allEquipments) {
        EnumConstants.TaskType taskType = EnumConstants.TaskType.getTaskType(taskAdminPramsModel.getTaskType());

        if (taskType.equals(EnumConstants.TaskType.OVER) && taskAdminPramsModel.getOverType() == (EnumConstants.overType.SUBSIDIARY.value)) {
            for (Equipment equipment : allEquipments) {

                Integer taskDetailId = getHotTaskId(taskAdminPramsModel.getHeart(), equipment.getDeviceUid());
                //???????????????????????????
                List<LiveHotSubTask> LiveHot = liveHotSubTaskService.findToDistantOver(EnumConstants.TaskType.OVER.value, getHotTaskId(taskAdminPramsModel.getHeart(), equipment.getDeviceUid()), EnumConstants.taskStatus.RUN.value);
                if (LiveHot.size() > 0) {
                    throw new BussinessException("?????????????????????????????????");
                } else {
                    List<LiveHotSubTask> toTaskDetailsId = liveHotSubTaskService.findToTaskDetailsId(taskDetailId, EnumConstants.taskStatus.RUN.value, EnumConstants.TaskType.OVER.value);
                    if (toTaskDetailsId.size() == 0) {
                        throw new BussinessException("???????????????????????????????????????");
                    }
                }
            }
        }

    }


    @Override
    public void addMakeList(Task task, ArrayList<Equipment> allEquipments, Integer makeListNum) {
        //??????????????????
        TaskDetails taskDetails = new TaskDetails();
        try {
            this.insert(task);

            for (Equipment equipment : allEquipments) {
                String key = EnumConstants.TaskType.FOLLOW_MAKER_A_LIST.value + ":" + equipment.getDeviceUid();
                String s = redisService.get(key);
                String taskValue = "task_" + EnumConstants.TaskType.FOLLOW_MAKER_A_LIST.value + "_" + equipment.getDeviceUid() + "_" + System.currentTimeMillis();
                if (s == null) {
                    redisService.put(key, "task_id:" + taskValue, 5);
                    taskDetails.setTaskId(task.getId());
                    taskDetails.setDeviceUid(equipment.getDeviceUid());
                    taskDetails.setState(1);
//                    taskDetails.setMakeListNumber(makeListNum);
                    taskDetails.setTaskType(EnumConstants.TaskType.FOLLOW_MAKER_A_LIST.value);
                    taskDetails.setTaskDetailUuid(taskValue);
                    taskDetailsService.insert(taskDetails);
                }
            }
        } catch (BussinessException e) {
            throw new BussinessException(e.getMessage());
        }
    }

    @Override
    public void addRedEnvelope(Task task, ArrayList<Equipment> allEquipments, Integer redEnvelopeTime) {
        //??????????????????
        TaskDetails taskDetails = new TaskDetails();
        try {
            this.insert(task);
            for (Equipment equipment : allEquipments) {
                String key = EnumConstants.TaskType.GRAB_A_RED_ENVELOPE.value + ":" + equipment.getDeviceUid();
                String s = redisService.get(key);

                String taskValue = "task_" + EnumConstants.TaskType.GRAB_A_RED_ENVELOPE.value + "_"
                        + equipment.getDeviceUid() + "_" + System.currentTimeMillis();
                if (s == null) {
                    redisService.put(key, "task_id:" + taskValue, 5);
                    taskDetails.setTaskId(task.getId());
                    taskDetails.setDeviceUid(equipment.getDeviceUid());
                    taskDetails.setState(1);
//                    taskDetails.setRedEnvelopeTime(redEnvelopeTime);
                    taskDetails.setTaskType(EnumConstants.TaskType.GRAB_A_RED_ENVELOPE.value);
                    taskDetails.setTaskDetailUuid(taskValue);
                    taskDetailsService.insert(taskDetails);
                }
            }
        } catch (BussinessException e) {
            throw new BussinessException(e.getMessage());
        }
    }

    /**
     * ???????????????????????????
     *
     * @param taskType    ????????????
     * @param liveHotUuid ??????????????????uuid
     */
    @Override
    public List<Object> findLiveHotSubTask(EnumConstants.TaskType taskType, String liveHotUuid, String platform) {
        //???????????????
        LiveHotSubTaskModel taskAllModel = liveHotSubTaskService.findByLiveHotTaskUuid(taskType.code, liveHotUuid, platform);
        //?????????????????????
        List<Object> list = new ArrayList<>();
        //????????????
        Map<String, Object> objectMap = new HashMap<>();
        //??????????????????
        if (taskAllModel == null) {
            return list;
        }
        if (taskType.equals(EnumConstants.TaskType.OVER)) {
            objectMap.put("device_code", taskAllModel.getDeviceUid());
            objectMap.put("task_id", taskAllModel.getUuid());
            objectMap.put("task_type", taskAllModel.getTaskType());
            objectMap.put("main_task_id", "");
            objectMap.put("sub_task_id", "");
            //???????????????????????????
            if (StringUtil.isBlank(taskAllModel.getContent())) {
                objectMap.put("main_task_id", taskAllModel.getTaskDetailUuid());
            } else {
                objectMap.put("sub_task_id", taskAllModel.getContent());
            }
            list.add(objectMap);
            return list;
        }
        if (taskType.equals(EnumConstants.TaskType.LIVE_CHAT)) {
            RightNowModel data = new RightNowModel();
            data.setTask_type(taskType.code);
            data.setTask_id(taskAllModel.getUuid());
            RightNowTaskDataModel taskDataModel = new RightNowTaskDataModel();
            taskDataModel.setTask_type(taskType.code);
            taskDataModel.setDevice_code(taskAllModel.getDeviceUid());
            RightNowTaskModel rightNowTaskModel = new RightNowTaskModel();
            rightNowTaskModel.setLive(taskAllModel.getContent());
            taskDataModel.setComment_template(rightNowTaskModel);
            data.setTask_data(taskDataModel);
            list.add(data);
            return list;
        }
        if (taskType.equals(EnumConstants.TaskType.LOOK_SHOPPING)) {
            //????????????
            //????????????????????????
//            objectMap.put("comment_interval", taskAllModel.getCommentInterval());
            //??????????????????
            objectMap.put("task_expect_running", taskAllModel.getTaskExpectRunning());
            list.add(new TaskDataTowModel(taskAllModel.getUuid(), taskAllModel.getTaskType(), objectMap));
            return list;
        }
        if (EnumConstants.TaskType.FOLLOW_PK_OPPONENT.equals(taskType)) {
            //????????????
            list.add(new EquipmentModel(taskAllModel.getDeviceUid(), taskAllModel.getUuid(), taskAllModel.getTaskType()));
            return list;
        }
        //????????????
        if (EnumConstants.TaskType.FOLLOW_MAKER_A_LIST.equals(taskType)) {
            //????????????
            objectMap.put("pay_attention_to_the_number", taskAllModel.getMakeListNumber());
            list.add(new TaskDataTowModel(taskAllModel.getUuid(), taskAllModel.getTaskType(), objectMap));
            return list;
        }
        //?????????
        if (EnumConstants.TaskType.GRAB_A_RED_ENVELOPE.equals(taskType)) {
            objectMap.put("red_envelope_time", taskAllModel.getRedEnvelopeTime());
            list.add(new TaskDataTowModel(taskAllModel.getUuid(), taskAllModel.getTaskType(), objectMap));
            return list;
        }
        //????????????
        if (EnumConstants.TaskType.GIVE_GIFT.equals(taskType)) {
            objectMap.put("gift_number", taskAllModel.getGiftNumber());
            objectMap.put("gift_box", taskAllModel.getGiftBox());
            objectMap.put("gift_page", taskAllModel.getGiftPage());
            list.add(new TaskDataTowModel(taskAllModel.getUuid(), taskAllModel.getTaskType(), objectMap));
            return list;
        }
        //????????????
        if (EnumConstants.TaskType.FANS_GROUP.equals(taskType)) {
            objectMap.put("gift_number", taskAllModel.getGiftNumber());
            objectMap.put("gift_box", taskAllModel.getGiftBox());
            objectMap.put("gift_page", taskAllModel.getGiftPage());
            list.add(new TaskDataTowModel(taskAllModel.getUuid(), taskAllModel.getTaskType(), objectMap));
            return list;
        }
        //???????????????
        if (EnumConstants.TaskType.JOIN_FAN_GROUP.equals(taskType)) {
            //??????????????????
            objectMap.put("give_light_plate_flag", taskAllModel.getGiveLight());
            list.add(new TaskDataTowModel(taskAllModel.getUuid(), taskAllModel.getTaskType(), objectMap));
            return list;
        }
        //????????????
        if (EnumConstants.TaskType.INSANE_CLICK.equals(taskType)) {
            //????????????
            objectMap.put("click_number", taskAllModel.getClickNumber());
            //????????????
            objectMap.put("task_expect_running", taskAllModel.getTaskExpectRunning());
            //??????????????????
            objectMap.put("comment_interval", taskAllModel.getCommentInterval());
            list.add(new TaskDataTowModel(taskAllModel.getUuid(), taskAllModel.getTaskType(), objectMap));
            return list;
        }

        list.add(new EquipmentModel(taskAllModel.getDeviceUid(), taskAllModel.getUuid(), taskAllModel.getTaskType()));
        return list;
    }

    @Override
    public void hotSubTaskNotify(String deviceUid, EnumConstants.TaskType task_type, String taskId, String taskStatus) {
        //?????????????????????
        String status = EnumConstants.taskStatus.getValue(taskStatus);
        String notifyTestFlag = redisService.get(ConfigNID.NOTIFY_TEST_FLAG);
        String remakes = "??????????????????????????????:" + EnumConstants.taskStatus.getDesc(status);
        if (StringUtil.isNotBlank(notifyTestFlag) && notifyTestFlag.equals(EnumConstants.taskStatus.RUN.value)) {
            //?????????  ?????????????????????
            status = EnumConstants.taskStatus.RUN.value;
            TaskDetails taskDetails = taskDetailsService.findByUuid(taskId, deviceUid);
            taskDetails.setRemarks("??????--> " + remakes);
            taskDetailsService.updateByPrimaryKeySelective(taskDetails);
        }
        if (task_type.equals(EnumConstants.TaskType.LIVE_HOT)) {
            //????????????????????????
            boolean flag = taskDetailsService.updateTaskDetailState(taskId, status);
            if (!flag) {
                throw new BussinessException("??????????????????");
            }
            //?????????????????????  ??????????????????????????????
            if (EnumConstants.taskStatus.DONE.value.equals(status)) {
                //????????????????????????????????????
                TaskDetails taskDetails = taskDetailsService.findByUuid(taskId, deviceUid);
                //??????????????????????????????????????? ?????????????????????
                liveHotSubTaskService.updateByTaskDetailsId(taskDetails.getId(), EnumConstants.taskStatus.DONE.value,
                        "???????????????????????????????????????????????????????????????", EnumConstants.TaskType.LIVE_HOT.code);
            }

            return;
        }
        //?????????????????????
        LiveHotSubTask liveHotSubTask = liveHotSubTaskService.findByUuid(taskId, deviceUid);
        liveHotSubTask.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        liveHotSubTask.setState(Integer.parseInt(status));
        //??????????????????
        int flag = liveHotSubTaskService.updateByPrimaryKeySelective(liveHotSubTask);
        if (flag == 0) {
            throw new BussinessException("??????????????????");
        }
    }

    @Override
    public List<Integer> FindByAgent(String code) {
        Equipment selectEquipment = new Equipment();
        selectEquipment.setSysAdminCode(code);
        List<Equipment> allEquipment = equipmentService.select(selectEquipment);
        ArrayList<Integer> allEquipmentId = new ArrayList<>();
        //???????????????????????????
        if (allEquipment.size() == 0) {
            return allEquipmentId;
        }
        if (StringUtils.isNotBlank(allEquipment)) {
            for (Equipment equipment : allEquipment) {
                allEquipmentId.add(equipment.getId());
            }
        }
        return taskDetailsService.findByAgent(allEquipmentId);

    }

    @Override
    public void findCloseTask() {
        //???????????????????????????
        List<Integer> closeTask = taskMapper.findCloseTask(Integer.valueOf(EnumConstants.taskStatus.DONE.value));
        if (closeTask.size() > 0) {
            taskMapper.deleteByTaskId(closeTask);
            taskMapper.deleteTaskDetail(closeTask);
        }
    }

    @Override
    public void activeOverTask(String deviceUid) {
        String msg = "?????????????????????????????????..........??????????????????????????????????????????????????????????????????";
        log.info(msg);
        int flag = taskDetailsService.updateTaskOverByDeviceUid(deviceUid, msg);
        log.info("????????????????????????" + flag);
        flag = liveHotSubTaskService.updateTaskOverByDeviceUid(deviceUid, msg);
        log.info("???????????????????????????????????????" + flag);
        log.info("???????????????");
    }

    @Override
    public DtGrid findByTaskPage(DtGrid dtGrid, Map<String, Object> pramMap, int page, int rows) {
        //????????????
        int count = taskMapper.findByTaskListCount(pramMap);
        //????????????
        int pageNumber = (page - 1) * rows;
        //????????????
        List list = taskMapper.findByTaskList(pramMap, pageNumber, rows);
        //???????????????
        int pageCount = new BigDecimal(count).divide(new BigDecimal(rows)).setScale(0, BigDecimal.ROUND_UP).intValue();
        dtGrid.setExhibitDatas(list);
        dtGrid.setPageCount(pageCount);
        dtGrid.setRecordCount(count);
        return dtGrid;
    }

    /**
     * ??????????????????
     *
     * @param equipmentList ????????????
     * @param task          ????????????
     */
    void sendSuperHotTaskBySocket(List<Equipment> equipmentList, Task task, String millis) {
        long s1 = System.currentTimeMillis();
        long s2 = System.currentTimeMillis();
        //?????????????????????
        int i = 0;
        for (Equipment equipment : equipmentList) {
            int state = Integer.parseInt(EnumConstants.taskStatus.RUN.value);
            if (!task.getTaskRunCode().equals(EnumConstants.TaskRunCode.NOW.code)) {
                state = (Integer.parseInt(EnumConstants.taskStatus.WAIT.value));
            }
            String taskId = "task_" + task.getTaskType() + "_" + equipment.getDeviceUid() + "_" + millis;
            //??????????????????
            TaskDetails taskDetails = new TaskDetails();
            taskDetails.setCity("");
            taskDetails.setApkUpgrade(1);
            taskDetails.setTaskId(task.getId());
            taskDetails.setDeviceUid(equipment.getDeviceUid());
            taskDetails.setState(state);
            taskDetails.setNumber(0);
            taskDetails.setLetterType("letterAdmin");
            taskDetails.setTaskType(task.getTaskType());
            taskDetails.setTaskDetailUuid(taskId);
            TaskAllModel taskAllModel = new TaskAllModel();
            taskAllModel.setTaskDetailUuid(taskId);
            taskAllModel.setTaskType(task.getTaskType());
            taskAllModel.setCommentInterval(task.getCommentInterval());
            taskAllModel.setTaskContent(task.getTaskContent());
            taskAllModel.setTaskExpectRunning(task.getTaskExpectRunning());
            taskAllModel.setTaskRunCode(task.getTaskRunCode());
            taskAllModel.setNumber(taskDetails.getNumber());
            taskAllModel.setLetterType(taskDetails.getLetterType());
            taskAllModel.setTaskStartTime(taskDetails.getTaskStartTime());
            taskAllModel.setTaskEndTime(task.getTaskEndTime());
            taskAllModel.setAnalysisContent(task.getAnalysisContent());
            taskAllModel.setLiveInContent(task.getAnalysisContent());
            taskAllModel.setLiveInType(task.getLiveInType());

            Long commentTemplateId = task.getCommentTemplateId();
            CommentTemplate commentTemplate = commentTemplateService.findById(commentTemplateId);
            taskAllModel.setName(commentTemplate.getName());
            taskAllModel.setComment((commentTemplate.getComment()));
            taskAllModel.setLive(commentTemplate.getLive());
            taskAllModel.setTurns(commentTemplate.getTurns());
            taskAllModel.setLetter(commentTemplate.getLetter());
            TaskModel taskModel = getTaskModel(taskAllModel);
            List<TaskModel> list = new ArrayList<>();
            list.add(taskModel);
            ResultEntity<List<TaskModel>> resultEntity = new ResultEntity<>();
            resultEntity.setCode(ResultEntity.SUCCESS);
            resultEntity.setData(list);
            resultEntity.setMsg("success");
            resultEntity.setTotal(list.size());
            String channelId = redisService.get(Constants.DEVICE_CHANNEL + taskDetails.getDeviceUid());
            System.out.println("deviceId:" + taskDetails.getDeviceUid() + ",channelId:" + channelId);
            resultEntity.setUuid(channelId);
            resultEntity.setUrl(equipment.getDeviceUid());
            //????????????
            String message = JSON.toJSONString(resultEntity);

            redisService.publish(Constants.TIKTOK_LIVE_HOT, message);
            System.out.println("single send task to client took: " + (System.currentTimeMillis() - s2));
            log.info("?????????????????????" + i++);
            log.info("Total send task to client took: " + (System.currentTimeMillis() - s1));
        }
    }

    /**
     * ???????????????
     *
     * @param liveHotSubTasks ???????????????
     */
    void sendSubTaskBySocket(List<LiveHotSubTask> liveHotSubTasks) {
        for (LiveHotSubTask subTask : liveHotSubTasks) {
            long s1 = System.currentTimeMillis();
            //?????????????????????
            List list = new ArrayList();
            //????????????
            Map<String, Object> objectMap = new HashMap<>();
            String taskType = subTask.getTaskType();
            if (taskType.equals(EnumConstants.TaskType.OVER.code)) {
                objectMap.put("device_code", subTask.getDeviceUid());
                objectMap.put("task_id", subTask.getUuid());
                objectMap.put("task_type", subTask.getTaskType());
                objectMap.put("main_task_id", "");
                objectMap.put("sub_task_id", "");
                //???????????????????????????
                if (StringUtil.isBlank(subTask.getLiveInContent())) {
                    objectMap.put("main_task_id", subTask.getTaskDetailId());
                } else {
                    objectMap.put("sub_task_id", subTask.getLiveInContent());
                }
                list.add(objectMap);
            }
            if (taskType.equals(EnumConstants.TaskType.LIVE_CHAT.code)) {
                RightNowModel data = new RightNowModel();
                data.setTask_type(taskType);
                data.setTask_id(subTask.getUuid());
                RightNowTaskDataModel taskDataModel = new RightNowTaskDataModel();
                taskDataModel.setTask_type(taskType);
                taskDataModel.setDevice_code(subTask.getDeviceUid());
                RightNowTaskModel rightNowTaskModel = new RightNowTaskModel();
                rightNowTaskModel.setLive(subTask.getLiveInContent());
                taskDataModel.setComment_template(rightNowTaskModel);
                data.setTask_data(taskDataModel);
                list.add(data);
            }
            if (taskType.equals(EnumConstants.TaskType.LOOK_SHOPPING.code)) {
                //????????????
                //??????????????????(???)
                objectMap.put("seconds_interval_view", subTask.getCommentInterval());
                //??????????????????(???)
                objectMap.put("seconds_per_view", subTask.getClickNumber());
                //?????????
                objectMap.put("keywords", subTask.getLiveInContent());
                //??????????????????(??????)
                objectMap.put("task_expect_running", subTask.getTaskExpectRunning());
                list.add(new TaskDataTowModel(subTask.getUuid(), subTask.getTaskType(), objectMap));

            }
            if (EnumConstants.TaskType.FOLLOW_PK_OPPONENT.code.equals(taskType)) {
                //????????????
                list.add(new EquipmentModel(subTask.getDeviceUid(), subTask.getUuid(), subTask.getTaskType()));

            }
            //????????????
            if (EnumConstants.TaskType.FOLLOW_MAKER_A_LIST.code.equals(taskType)) {
                //????????????
                objectMap.put("pay_attention_to_the_number", subTask.getMakeListNumber());
                list.add(new TaskDataTowModel(subTask.getUuid(), subTask.getTaskType(), objectMap));

            }
            //?????????
            if (EnumConstants.TaskType.GRAB_A_RED_ENVELOPE.code.equals(taskType)) {
                objectMap.put("red_envelope_time", subTask.getRedEnvelopeTime());
                list.add(new TaskDataTowModel(subTask.getUuid(), subTask.getTaskType(), objectMap));

            }
            //????????????
            if (EnumConstants.TaskType.GIVE_GIFT.code.equals(taskType)) {
                objectMap.put("gift_number", subTask.getGiftNumber());
                objectMap.put("gift_box", subTask.getGiftBox());
                objectMap.put("gift_page", subTask.getGiftPage());
                list.add(new TaskDataTowModel(subTask.getUuid(), subTask.getTaskType(), objectMap));
            }
            //?????????
            if (EnumConstants.TaskType.FANS_GROUP.code.equals(taskType)) {
                objectMap.put("gift_number", subTask.getGiftNumber());
                objectMap.put("gift_box", subTask.getGiftBox());
                objectMap.put("gift_page", subTask.getGiftPage());
                list.add(new TaskDataTowModel(subTask.getUuid(), subTask.getTaskType(), objectMap));
            }
            //???????????????
            if (EnumConstants.TaskType.JOIN_FAN_GROUP.code.equals(taskType)) {
                //??????????????????
                objectMap.put("give_light_plate_flag", subTask.getGiveLight());
                list.add(new TaskDataTowModel(subTask.getUuid(), subTask.getTaskType(), objectMap));
            }
            //????????????
            if (EnumConstants.TaskType.INSANE_CLICK.code.equals(taskType)) {
                //????????????
                objectMap.put("click_number", subTask.getClickNumber());
                //????????????
                objectMap.put("task_expect_running", subTask.getTaskExpectRunning());
                //??????????????????
                objectMap.put("comment_interval", subTask.getCommentInterval());
                list.add(new TaskDataTowModel(subTask.getUuid(), subTask.getTaskType(), objectMap));
            }

            list.add(new EquipmentModel(subTask.getDeviceUid(), subTask.getUuid(), subTask.getTaskType()));

            ResultEntity<List<TaskModel>> resultEntity = new ResultEntity<>();
            resultEntity.setCode(ResultEntity.SUCCESS);
            resultEntity.setData(list);
            resultEntity.setMsg("success");
            resultEntity.setTotal(list.size());
            String channelId = redisService.get(Constants.DEVICE_CHANNEL + subTask.getDeviceUid());
            System.out.println("deviceId:" + subTask.getDeviceUid() + ",channelId:" + channelId);
            resultEntity.setUuid(channelId);
            resultEntity.setUrl(subTask.getDeviceUid());
            //????????????
            redisService.publish(Constants.TIKTOK_LIVE_CHAT, JSON.toJSONString(resultEntity));
            System.out.println("send single sub task took:" + (System.currentTimeMillis() - s1));
            log.info(taskType + "?????????????????????" + subTask.getDeviceUid() + "---???????????????id" + subTask.getId());
        }

    }


    /**
     * ?????????????????? (??????)
     *
     * @param deviceUidList ????????????uid:taskId
     * @param task          ????????????
     */
    void sendSuperHotTaskBySocket(List<String> deviceUidList, Task task) {
        long s1 = System.currentTimeMillis();
        long s2 = System.currentTimeMillis();
        //??????????????????
        TaskAllModel taskAllModel = new TaskAllModel();
        taskAllModel.setTaskType(task.getTaskType());
        taskAllModel.setCommentInterval(task.getCommentInterval());
        taskAllModel.setTaskContent(task.getTaskContent());
        taskAllModel.setTaskExpectRunning(task.getTaskExpectRunning());
        taskAllModel.setTaskRunCode(task.getTaskRunCode());
        taskAllModel.setNumber(0);
        taskAllModel.setLetterType("letterAdmin");
        taskAllModel.setTaskStartTime(new Date());
        taskAllModel.setTaskEndTime(task.getTaskEndTime());
        taskAllModel.setAnalysisContent(task.getAnalysisContent());
        taskAllModel.setLiveInContent(task.getAnalysisContent());
        taskAllModel.setLiveInType(task.getLiveInType());
        taskAllModel.setDeviceUidList(deviceUidList);
        //??????????????????
        CommentTemplate commentTemplate = commentTemplateService.findById(task.getCommentTemplateId());
        taskAllModel.setName(commentTemplate.getName());
        taskAllModel.setComment((commentTemplate.getComment()));
        taskAllModel.setLive(commentTemplate.getLive());
        taskAllModel.setTurns(commentTemplate.getTurns());
        taskAllModel.setLetter(commentTemplate.getLetter());
        TaskModel taskModel = getTaskModel(taskAllModel);
        List<TaskModel> list = new ArrayList<>();
        list.add(taskModel);
        ResultEntity<List<TaskModel>> resultEntity = new ResultEntity<>();
        resultEntity.setCode(ResultEntity.SUCCESS);
        resultEntity.setData(list);
        resultEntity.setMsg("success");
        resultEntity.setTotal(list.size());
//        String channelId = redisService.get(Constants.DEVICE_CHANNEL + taskDetails.getDeviceUid());
//        resultEntity.setUuid(channelId);
//        resultEntity.setUrl(equipment.getDeviceUid());
        //????????????
        String message = JSON.toJSONString(resultEntity);
        redisService.publish(Constants.TIKTOK_LIVE_HOT, message);
        System.out.println("single send task to client took: " + (System.currentTimeMillis() - s2));
        log.info("Total send task to client took: " + (System.currentTimeMillis() - s1));
    }

    /**
     * ??????????????? ????????????
     *
     * @param deviceUidList ???????????????uid??????
     * @param subTask       ???????????????
     */
    void sendSubTaskBySocket(List<String> deviceUidList, LiveHotSubTask subTask) {
        long s1 = System.currentTimeMillis();
        //?????????????????????
        List list = new ArrayList();
        //????????????
        Map<String, Object> objectMap = new HashMap<>();

        String taskType = subTask.getTaskType();
        if (taskType.equals(EnumConstants.TaskType.OVER.code)) {
            objectMap.put("device_code", subTask.getDeviceUid());
            objectMap.put("task_id", subTask.getUuid());
            objectMap.put("task_type", subTask.getTaskType());
            objectMap.put("main_task_id", "");
            objectMap.put("sub_task_id", "");
            //???????????????????????????
            if (StringUtil.isBlank(subTask.getLiveInContent())) {
                objectMap.put("main_task_id", subTask.getTaskDetailId());
            } else {
                objectMap.put("sub_task_id", subTask.getLiveInContent());
            }
            list.add(objectMap);
        }
        if (taskType.equals(EnumConstants.TaskType.LIVE_CHAT.code)) {
            RightNowModel data = new RightNowModel();
            data.setTask_type(taskType);
            data.setTask_id(subTask.getUuid());
            RightNowTaskDataModel taskDataModel = new RightNowTaskDataModel();
            taskDataModel.setTask_type(taskType);
            taskDataModel.setDevice_code(subTask.getDeviceUid());
            RightNowTaskModel rightNowTaskModel = new RightNowTaskModel();
            rightNowTaskModel.setLive(subTask.getLiveInContent());
            taskDataModel.setComment_template(rightNowTaskModel);
            data.setTask_data(taskDataModel);
            list.add(data);
        }
        if (taskType.equals(EnumConstants.TaskType.LOOK_SHOPPING.code)) {
            //????????????
            //??????????????????(???)
            objectMap.put("seconds_interval_view", subTask.getCommentInterval());
            //??????????????????(???)
            objectMap.put("seconds_per_view", subTask.getClickNumber());
            //?????????
            objectMap.put("keywords", subTask.getLiveInContent());
            //??????????????????(??????)
            objectMap.put("task_expect_running", subTask.getTaskExpectRunning());
            list.add(new TaskDataTowModel(subTask.getUuid(), subTask.getTaskType(), objectMap));

        }
        if (EnumConstants.TaskType.FOLLOW_PK_OPPONENT.code.equals(taskType)) {
            //????????????
            list.add(new EquipmentModel(subTask.getDeviceUid(), subTask.getUuid(), subTask.getTaskType()));

        }
        //????????????
        if (EnumConstants.TaskType.FOLLOW_MAKER_A_LIST.code.equals(taskType)) {
            //????????????
            objectMap.put("pay_attention_to_the_number", subTask.getMakeListNumber());
            list.add(new TaskDataTowModel(subTask.getUuid(), subTask.getTaskType(), objectMap));

        }
        //?????????
        if (EnumConstants.TaskType.GRAB_A_RED_ENVELOPE.code.equals(taskType)) {
            objectMap.put("red_envelope_time", subTask.getRedEnvelopeTime());
            list.add(new TaskDataTowModel(subTask.getUuid(), subTask.getTaskType(), objectMap));

        }
        //????????????
        if (EnumConstants.TaskType.GIVE_GIFT.code.equals(taskType)) {
            objectMap.put("gift_number", subTask.getGiftNumber());
            objectMap.put("gift_box", subTask.getGiftBox());
            objectMap.put("gift_page", subTask.getGiftPage());
            list.add(new TaskDataTowModel(subTask.getUuid(), subTask.getTaskType(), objectMap));
        }
        //?????????
        if (EnumConstants.TaskType.FANS_GROUP.code.equals(taskType)) {
            objectMap.put("gift_number", subTask.getGiftNumber());
            objectMap.put("gift_box", subTask.getGiftBox());
            objectMap.put("gift_page", subTask.getGiftPage());
            list.add(new TaskDataTowModel(subTask.getUuid(), subTask.getTaskType(), objectMap));
        }
        //???????????????
        if (EnumConstants.TaskType.JOIN_FAN_GROUP.code.equals(taskType)) {
            //??????????????????
            objectMap.put("give_light_plate_flag", subTask.getGiveLight());
            list.add(new TaskDataTowModel(subTask.getUuid(), subTask.getTaskType(), objectMap));
        }
        //????????????
        if (EnumConstants.TaskType.INSANE_CLICK.code.equals(taskType)) {
            //????????????
            objectMap.put("click_number", subTask.getClickNumber());
            //????????????
            objectMap.put("task_expect_running", subTask.getTaskExpectRunning());
            //??????????????????
            objectMap.put("comment_interval", subTask.getCommentInterval());
            list.add(new TaskDataTowModel(subTask.getUuid(), subTask.getTaskType(), objectMap));
        }

        list.add(new EquipmentModel(subTask.getDeviceUid(), subTask.getUuid(), subTask.getTaskType()));

        //????????????????????????
        Map<String, Object> sendData = new HashMap<>();

        //????????????
        sendData.put("taskInfo", list);
        //?????????????????????????????????
        sendData.put("deviceUidList", deviceUidList);

        ResultEntity<List<TaskModel>> resultEntity = new ResultEntity<>();
        resultEntity.setCode(ResultEntity.SUCCESS);
        resultEntity.setData(list);
        resultEntity.setMsg("success");
        resultEntity.setTotal(list.size());
        String channelId = redisService.get(Constants.DEVICE_CHANNEL + subTask.getDeviceUid());
        System.out.println("deviceId:" + subTask.getDeviceUid() + ",channelId:" + channelId);
        resultEntity.setUuid(channelId);
        resultEntity.setUrl(subTask.getDeviceUid());
        //????????????
        redisService.publish(Constants.TIKTOK_LIVE_CHAT, JSON.toJSONString(resultEntity));
        System.out.println("send single sub task took:" + (System.currentTimeMillis() - s1));
        log.info(taskType + "?????????????????????" + subTask.getDeviceUid() + "---???????????????id" + subTask.getId());
    }
}
