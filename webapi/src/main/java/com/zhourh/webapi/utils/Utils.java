package com.zhourh.webapi.utils;

/**
 * Created by zhourh on 2017/3/28.
 */

public class Utils {
    public  static <T> T checkNotNull(T object, String message) {
        if (object == null) {
            throw new NullPointerException(message);
        }
        return object;
    }
}
