package com.huimi.admin.controller.apkHistory;

import com.huimi.admin.controller.BaseController;
import com.huimi.common.entity.ResultEntity;
import com.huimi.common.entity.dtgrid.DtGrid;
import com.huimi.common.tools.StringUtil;
import com.huimi.common.utils.StringUtils;
import com.huimi.core.constant.ConfigNID;
import com.huimi.core.constant.EnumConstants;
import com.huimi.core.exception.BussinessException;
import com.huimi.core.po.bizApkHistory.BizApkHistory;
import com.huimi.core.service.apkHistory.BizApkHistoryService;
import com.huimi.core.service.cache.RedisService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.List;

import static com.huimi.common.entity.ResultEntity.fail;

/**
 * create by lja on 2020/7/30 16:45
 */
@RestController
@RequestMapping(BaseController.BASE_URI + "/apkHistory/json")
public class BizApkHistoryJsonController extends BaseController {
    @Resource
    private BizApkHistoryService bizApkHistoryService;
    @Resource
    private RedisService redisService;

    @ResponseBody
    @RequestMapping("/list")
    public DtGrid listJson(HttpServletRequest request, int rows, int page) throws Exception {
        Integer pageSize = rows == 0 ? 1 : rows;
        Integer pageNumber = page == 0 ? 1 : page;
        DtGrid dtGrid = new DtGrid();
        dtGrid.setNowPage(pageNumber);
        dtGrid.setPageSize(pageSize);
        StringBuilder whereSql = new StringBuilder();
        String name = request.getParameter("search_val");
        if (StringUtils.isNotBlank(name)) {
            whereSql.append(" and name like '%").append(name).append("%'");
        }
        dtGrid.setWhereSql(whereSql.toString());
        dtGrid.setSortSql("order by create_time DESC");
        dtGrid = bizApkHistoryService.getDtGridList(dtGrid);
        return dtGrid;
    }


    /**
     * ????????????????????????
     */
    @ResponseBody
    @RequestMapping("/add")
//    @RequiresPermissions("sys:config:save")
    public ResultEntity addJson(BizApkHistory bizApkHistory) {
        try {
            checked("add", bizApkHistory);
            if (EnumConstants.HistoryState.YES.value == bizApkHistory.getState().intValue()) {
                //???????????????????????????
                redisService.put(ConfigNID.HistoryStateApkRedisUrl, bizApkHistory.getDataUrl());
                //?????????????????????
                redisService.put(ConfigNID.HistoryStateQcCodeRedisUrl, bizApkHistory.getQrCodeUrl());
            }
            bizApkHistory.setCreateTime(new Timestamp(System.currentTimeMillis()));
            if (bizApkHistoryService.insert(bizApkHistory) == 0) {
                return fail("?????????????????????????????????");
            }
            return ResultEntity.success();
        } catch (BussinessException e) {
            return fail(e.getMessage());
        }
    }


    /**
     * ????????????????????????
     */
    @ResponseBody
    @RequestMapping("/update")
//    @RequiresPermissions("sys:config:save")
    public ResultEntity updateJson(BizApkHistory bizApkHistory) {
        try {
            checked("update", bizApkHistory);
            if (EnumConstants.HistoryState.YES.value == bizApkHistory.getState().intValue()) {
                //???????????????????????????
                redisService.put(ConfigNID.HistoryStateApkRedisUrl, bizApkHistory.getDataUrl());
                //?????????????????????
                redisService.put(ConfigNID.HistoryStateQcCodeRedisUrl, bizApkHistory.getQrCodeUrl());
            }
            bizApkHistory.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            if (bizApkHistoryService.updateByPrimaryKeySelective(bizApkHistory) == 0) {
                return fail("?????????????????????????????????");
            }
            return ResultEntity.success();
        } catch (BussinessException e) {
            return fail(e.getMessage());
        }
    }


    /**
     * ????????????
     */
    @ResponseBody
    @RequestMapping("/del")
//    @RequiresPermissions("sys:config:save")
    public ResultEntity updateJson(String ids) {
        try {
            if (StringUtil.isBlank(ids)) {
                return fail("??????????????????????????????");
            }
            String[] strings = ids.split(",");
            for (String id : strings) {
                if (bizApkHistoryService.deleteByPrimaryKey(Integer.parseInt(id)) == 0) {
                    return fail("?????????????????????????????????");
                }
            }
            return ResultEntity.success();
        } catch (BussinessException e) {
            return fail(e.getMessage());
        }
    }


    /**
     * ??????
     */
    public void checked(String type, BizApkHistory bizApkHistory) {
        //????????????????????????????????????
        List<BizApkHistory> list = bizApkHistoryService.findByStateList(EnumConstants.HistoryState.YES.value);

        //???????????????????????????
        List<BizApkHistory> nameList = bizApkHistoryService.findByNameList(bizApkHistory.getName());
        if ("add".equals(type)) {
            if (EnumConstants.HistoryState.YES.value == bizApkHistory.getState().intValue()) {
                if (CollectionUtils.isNotEmpty(list)) {
                    throw new BussinessException("????????????????????????????????????");
                }
            }

            //?????????????????????
            if (CollectionUtils.isNotEmpty(nameList)) {
                throw new BussinessException("?????????????????????");
            }

        } else {
            if (EnumConstants.HistoryState.YES.value == bizApkHistory.getState().intValue()) {
                //????????????????????????????????? ????????????????????????
                if (CollectionUtils.isNotEmpty(list) && !bizApkHistory.getId().equals(list.get(0).getId())) {
                    throw new BussinessException("????????????????????????????????????");
                }
            }
            //????????????????????? ????????????????????????
            if (CollectionUtils.isNotEmpty(nameList) && !bizApkHistory.getId().equals(nameList.get(0).getId())) {
                throw new BussinessException("?????????????????????");
            }
        }


    }

}

