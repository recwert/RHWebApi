package com.zhourh.webapi.exception;

/**
 * Created by Huolongguo on 17/1/5.
 */

public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }
}
