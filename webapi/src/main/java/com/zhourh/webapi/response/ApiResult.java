package com.zhourh.webapi.response;


import com.zhourh.webapi.core.ApiSubscriber;
import com.zhourh.webapi.core.WebApi;

import io.reactivex.Flowable;

/**
 * Base interface for a response of http request
 * you can see {@linkplain WebApi#request(Flowable, ApiSubscriber)}
 * @param <T> the type of response model actually
 */
public interface ApiResult<T> {

    /**
     * @return the server handle success
     */
    boolean isSuccess();

    /**
     * @return if @{{@link #isSuccess()}} return false, the error of server what to reply client
     */
    String getError();

    /**
     * @return the data server response actually
     */
    T getData();
}
