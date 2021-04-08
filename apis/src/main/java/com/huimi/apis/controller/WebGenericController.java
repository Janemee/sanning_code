package com.huimi.apis.controller;

import com.huimi.apis.config.WebInterceptor;
import com.huimi.common.baseMapper.GenericPo;
import com.huimi.common.entity.ResultEntity;
import com.huimi.common.utils.LogUtil;
import com.huimi.core.exception.BussinessException;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * json数据类controller 基础实现
 *
 * @param <PK> 对象主键类型
 * @param <PO> 对象
 */
@Slf4j
public class WebGenericController<PK, PO extends GenericPo<PK>> {

    /**
     * 操作成功
     */
    protected <T> ResultEntity<T> ok() {
        return result(null, "操作成功", ResultEntity.SUCCESS);
    }

    /**
     * 操作成功
     *
     * @param po 返回数据
     */
    protected <T> ResultEntity<T> ok(T po) {
        return result(po, "操作成功", ResultEntity.SUCCESS);
    }
    /**
     * 操作成功
     *
     * @param msg 返回数据
     */
    protected <T> ResultEntity<T> ok(String msg) {
        return result(null, msg, ResultEntity.SUCCESS);
    }

    /**
     * 操作成功
     *
     * @param po 返回数据
     */
    protected <T> ResultEntity<T> ok(T po, String msg) {
        return result(po, msg, ResultEntity.SUCCESS);
    }

    /**
     * 操作失败
     */
    protected <T> ResultEntity<T> fail(String msg) {
        return result(null, msg, ResultEntity.FAIL);
    }
    /**
     * 操作失败
     */
    protected <T> ResultEntity<T> fail(T po,String msg) {
        return result(po, msg, ResultEntity.FAIL);
    }

    /**
     * 操作失败
     */
    protected <T> ResultEntity<T> fail() {
        return result(null, "操作失败", ResultEntity.FAIL);
    }

    /**
     * 返回消息记录
     *
     * @param resultData 返回实体(可空)
     * @param msg        返回消息
     * @param code       成功/错误
     */
    private <T> ResultEntity<T> result(T resultData, String msg, int code) {
        ResultEntity resultEntity = new ResultEntity();
        resultEntity.setData(resultData);
        resultEntity.setCode(code);
        resultEntity.setMsg(msg);
        return resultEntity;
    }

    protected <T> ResultEntity<T> result(String msg, int code) {
        ResultEntity resultEntity = new ResultEntity();
        resultEntity.setData(null);
        resultEntity.setCode(code);
        resultEntity.setMsg(msg);
        return resultEntity;
    }

    /**
     * 返回消息记录
     *
     * @param resultData 返回实体
     */
    protected <T> ResultEntity<T> result(ResultEntity resultData) {
        if (resultData.getCode() == ResultEntity.SUCCESS) {
            return ok();
        }
        return fail(resultData.getMsg());
    }

    /**
     * 全局异常处理
     */
    @ResponseBody
    @ExceptionHandler({Exception.class})
    public ResultEntity exception(Exception e) {
        ResultEntity resultEntity = new ResultEntity();
        resultEntity.setCode(ResultEntity.FAIL);
        resultEntity.setMsg("系统繁忙");
        if (e instanceof BussinessException) {
            LogUtil.error("", e, WebGenericController.class);
            if (((BussinessException) e).getCode() == BussinessException.Code.E401.code) {
                resultEntity.setCode(BussinessException.Code.E401.code);
                resultEntity.setMsg(BussinessException.Code.E401.msg);
            } else if (((BussinessException) e).getCode() == BussinessException.Code.E402.code) {
                resultEntity.setCode(BussinessException.Code.E402.code);
                resultEntity.setMsg(BussinessException.Code.E402.msg);
            } else if (((BussinessException) e).getCode() == BussinessException.Code.E404.code) {
                resultEntity.setCode(BussinessException.Code.E404.code);
                resultEntity.setMsg(BussinessException.Code.E404.msg);
            } else if (((BussinessException) e).getCode() == BussinessException.Code.E405.code) {
                resultEntity.setCode(BussinessException.Code.E405.code);
                resultEntity.setMsg(BussinessException.Code.E405.msg);
            }else if (((BussinessException) e).getCode() == BussinessException.Code.E202.code){
                resultEntity.setCode(BussinessException.Code.E202.code);
                resultEntity.setMsg(BussinessException.Code.E202.msg);
            }else if (((BussinessException) e).getCode() == BussinessException.Code.E203.code){
                resultEntity.setCode(BussinessException.Code.E203.code);
                resultEntity.setMsg(BussinessException.Code.E203.msg);
            }
            resultEntity.setMsg(e.getMessage());
        } else if (e instanceof MySQLIntegrityConstraintViolationException) {
            log.error(e.getMessage());
            e.printStackTrace();
        } else {
            log.error("{}",e.getMessage(),e);
            e.printStackTrace();
        }


        return resultEntity;
    }


    public static HttpServletRequest getRequest() {
        final RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        final ServletRequestAttributes sra = (ServletRequestAttributes) ra;
        return sra.getRequest();
    }

    public static HttpServletResponse getResponse() {
        final RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        final ServletRequestAttributes sra = (ServletRequestAttributes) ra;
        return sra.getResponse();
    }

    /**
     * 获取登录用户id
     *
     * @return Integer
     */
    protected Integer getLoginUserId() {
        return WebInterceptor.getLoginUserId();
    }

    /**
     * 获取登录用户uuid
     *
     * @return String
     */
    protected String getLoginUserUuid() {
        String loginUserUuid = WebInterceptor.getLoginUserUuid(getRequest());
        return null == loginUserUuid ? "" : loginUserUuid;
    }

}