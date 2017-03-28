package com.zhourh.webapi.core;


import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import com.zhourh.webapi.exception.ApiException;

import io.reactivex.subscribers.ResourceSubscriber;

/**
 * Base abstract class
 */
public abstract class ApiSubscriber<T> extends ResourceSubscriber<T>{

    /**
     * Callback when the request error,
     * you can set by @{@linkplain WebApi.Builder#apiErrorCallback(ApiErrorCallback)}
     */
    protected ApiErrorCallback errorCallback;

    public void setApiErrorCallback(ApiErrorCallback errorCallback) {
        this.errorCallback = errorCallback;
    }

    public ApiErrorCallback getErrorCallback() {
        return errorCallback;
    }

    @Override
    public void onError(Throwable e) {
        if (errorCallback == null){
            e.printStackTrace();
            return;
        }
        if (e instanceof SocketTimeoutException){
            errorCallback.onSocketTimeoutException((SocketTimeoutException) e);
        }
        if (e instanceof SocketException){
            errorCallback.onSocketException((SocketException) e);
        }
        if (e instanceof ProtocolException){
            errorCallback.onProtocolException((ProtocolException) e);
        }
        if (e instanceof HttpException){
            errorCallback.onHttpException((HttpException) e);
        }
        if (e instanceof ApiException){
            errorCallback.onApiException((ApiException) e);
        }
        errorCallback.onError(e);

    }

    @Override
    public void onComplete() {

    }


    /**
     * Interface definition for a errorCallback to be invoked when the request error.
     */
    public interface ApiErrorCallback{

        /**
         * Call when the request throw a @{@linkplain SocketTimeoutException}
         * @param e
         */
        void onSocketTimeoutException(SocketTimeoutException e);

        /**
         * Call when the request throw a @{@linkplain SocketException}
         * @param e
         */
        void onSocketException(SocketException e);

        /**
         * Call when the request throw a @{@linkplain ProtocolException}
         * @param e
         */
        void onProtocolException(ProtocolException e);

        /**
         * Call when the request throw a @{@linkplain HttpException}
         * @param e
         */
        void onHttpException(HttpException e);

        /**
         * Call when the request throw a @{@linkplain ApiException}
         * @param e
         */
        void onApiException(ApiException e);

        /**
         * Call when request error always
         * @param e
         */
        void onError(Throwable e);
    }
}
