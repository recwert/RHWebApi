package com.zhourh.rhwebapi;


import com.zhourh.webapi.response.ApiResult;

import java.util.Map;

/**
 * Created by Huolongguo on 17/2/5.
 */

public class UApiResult<T> implements ApiResult<T> {

    private boolean success;

    private T data;

    private String message;

    private Map extra;


    @Override
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String getError() {
        return message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Map getExtra() {
        return extra;
    }

    public void setExtra(Map extra) {
        this.extra = extra;
    }
}
