package com.zhourh.rhwebapi;

import com.zhourh.webapi.response.ApiResult;

public class GithubApiResult<T> implements ApiResult<T> {
    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public String getError() {
        return null;
    }

    @Override
    public T getData() {
        return null;
    }
}
