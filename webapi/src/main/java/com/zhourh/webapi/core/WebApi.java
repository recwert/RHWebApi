package com.zhourh.webapi.core;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.fastjson.FastJsonConverterFactory;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.zhourh.webapi.exception.ApiException;
import com.zhourh.webapi.response.ApiResult;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static com.zhourh.webapi.utils.Utils.checkNotNull;


/**
 * A http request manager use Retrofit, RxJava2, retrofit2-rxjava2-adapter, logging-interceptor
 * when you what to use the @{@linkplain WebApi}, you need to through the following steps
 * 1.for a @{@linkplain WebApi} instance {@linkplain WebApi.Builder#baseUrl(String)#logLevel(HttpLoggingInterceptor.Level)}
 * 2.invoke a request @{@linkplain WebApi#request(Flowable, ApiSubscriber)}
 */
public class WebApi {

    /**
     * The services user defined
     * @see WebApi.Builder#addService(int, Class)
     */
    private Map<Integer, Object> services = new HashMap<>();

    /**
     * Callback when the request error,
     * you can set by @{@linkplain WebApi.Builder#apiErrorCallback(ApiSubscriber.ApiErrorCallback)}
     */
    private ApiSubscriber.ApiErrorCallback apiErrorCallback;

    /**
     * Common counter for all request that is used to generate request ids
     */
    private AtomicInteger requestCounter = new AtomicInteger(0);

    /**
     * All subscribers that user requesting
     * the ApiSubscriber will be removed when request completed
     */
    private ConcurrentHashMap<Integer, ApiSubscriber> subscribers;

    /**
     * @param application please
     * @param retrofit
     * @param servicesClasses
     * @param apiErrorCallback
     */
    private WebApi(@NonNull Application application, @NonNull Retrofit retrofit,
                   @Nullable Map<Integer, Class> servicesClasses,
                   @Nullable ApiSubscriber.ApiErrorCallback apiErrorCallback){
        if (servicesClasses == null || servicesClasses.isEmpty()){
            return;
        }
        checkNotNull(application, "application == null");
        checkNotNull(retrofit, "retrofit == null");
        this.apiErrorCallback = apiErrorCallback;
        Iterator<Integer> iterator = servicesClasses.keySet().iterator();
        while (iterator.hasNext()) {
            Integer serviceId = iterator.next();
            services.put(serviceId, retrofit.create(servicesClasses.get(serviceId)));
        }
        subscribers = new ConcurrentHashMap<>();

    }

    /**
     * Get the service by serviceId, the serviceId need maintained by user
     * @param serviceId {@linkplain WebApi.Builder#addService(int, Class)}
     * @return the service through the Retrofit generated
     */
    public Object getService(@NonNull Integer serviceId){
        if (services == null || services.isEmpty()){
            return null;
        }
        return services.get(serviceId);
    }

    /**
     * Get the service by class
     * @param tClass {@linkplain WebApi.Builder#addService(int, Class)}
     * @param <T> the service through the Retrofit generated
     * @return
     */
    public <T> T getService(@NonNull Class<T> tClass){
        if (services == null || services.isEmpty()){
            return null;
        }
        Iterator<Integer> iterator  = services.keySet().iterator();
        while (iterator.hasNext()){
            Object service = services.get(iterator.next());
            if (service.getClass().equals(tClass)){
                return (T) service;
            }
        }
        return null;
    }



    /**
     * Send a http request
     * @param flowable the service return value, such as {@code Flowable<ApiResult<String>> login(String account, String password)}
     * @param subscriber @{@linkplain ApiSubscriber}
     * @param <T>
     * @return requestId, you can cancel a request through requestId, @{@linkplain WebApi#cancelRequest(int)}
     */
    public <T> int request(@NonNull Flowable<? extends ApiResult<T>> flowable, @NonNull ApiSubscriber<T> subscriber){
        checkNotNull(flowable, "flowable == null");
        checkNotNull(subscriber, "subscriber == null");
        subscriber.setApiErrorCallback(apiErrorCallback);
        int requestId = requestCounter.addAndGet(1);
        ApiSubscriberDecorator subscriberDecorator = new ApiSubscriberDecorator(requestId, subscriber);
        flowable.subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new HttpResultFunction<T>())
                .subscribe(subscriberDecorator);
        subscribers.put(requestId, subscriberDecorator);
        return requestId;
    }

    /**
     * Cancel a request through requestId
     * @param requestId @{@linkplain WebApi#request(Flowable, ApiSubscriber)}
     */
    public void cancelRequest(int requestId){
        ApiSubscriber subscriber = subscribers.get(requestId);
        if (subscriber == null){
            return;
        }
        if (!subscriber.isDisposed()){
            subscriber.dispose();
        }
        subscribers.remove(requestId);
    }

    /**
     * Cancel the multiple requests through requestId, @{@linkplain WebApi#cancelRequest(int)}
     * @param requests the collection of requestId
     */
    public void cancelRequests(Collection<Integer> requests){
        if (requests == null || requests.isEmpty()){
            return;
        }
        Iterator<Integer> iterator = requests.iterator();
        while (iterator.hasNext()){
            cancelRequest(iterator.next());
        }
    }


    /**
     * Check the http response is correct
     * @param <T> the type of response model actually
     */
    private class HttpResultFunction<T> implements Function<ApiResult<T>, T>{

        @Override
        public T apply(ApiResult<T> tApiResult) throws Exception {
            if (!tApiResult.isSuccess()){
                throw new ApiException(tApiResult.getError());
            }
            if (tApiResult.getData() == null){
                return (T) "";
            }
            return tApiResult.getData();
        }

    }


    /**
     * A decorator of a @{@linkplain ApiSubscriber} that remove self when request completed
     * @param <T>
     */
    private class ApiSubscriberDecorator<T> extends ApiSubscriber<T>{

        private int requestId;

        private ApiSubscriber apiSubscriber;

        public ApiSubscriberDecorator(int requestId, ApiSubscriber apiSubscriber) {
            this.requestId = requestId;
            this.apiSubscriber = apiSubscriber;
            setApiErrorCallback(apiSubscriber.getErrorCallback());
        }

        @Override
        public void onNext(T t) {
            apiSubscriber.onNext(t);
        }

        @Override
        public void onError(Throwable e) {
            apiSubscriber.onError(e);
        }

        @Override
        public void onComplete() {
            super.onComplete();
            subscribers.remove(requestId);
        }
    }

    /**
     * Build a new {@link WebApi}.
     * <p>
     * Calling {@link #baseUrl} and @{@link #logLevel} is required before calling {@link #build}.
     * All other methods are optional.
     */
    public static final class Builder {

        private String baseUrl;

        private boolean persistentCookie;

        private HttpLoggingInterceptor.Level logLevel;

        private Map<Integer, Class> serviceClasses = new HashMap<>();

        private Map<String, String> sslCertificates = new HashMap<>();

        private ApiSubscriber.ApiErrorCallback apiErrorCallback;

        private List<Interceptor> interceptors = new ArrayList<>();

        @NonNull
        public Builder baseUrl(String baseUrl){
            checkNotNull(baseUrl, "baseUrl == null");
            this.baseUrl = baseUrl;
            return this;
        }

        @NonNull
        public Builder persistentCookie(boolean persistentCookie){
            this.persistentCookie = persistentCookie;
            return this;
        }

        @NonNull
        public Builder logLevel(@NonNull HttpLoggingInterceptor.Level logLevel){
            checkNotNull(logLevel, "logLevel == null");
            this.logLevel = logLevel;
            return this;
        }

        @NonNull
        public Builder addService(int serviceId, @NonNull Class serviceClass){
            serviceClasses.put(serviceId, serviceClass);
            return this;
        }

        @NonNull
        public Builder addSslCertificate(@NonNull String hostname, @NonNull String certificateAssetPath){
            sslCertificates.put(hostname, certificateAssetPath);
            return this;
        }

        @NonNull
        public Builder addInterceptor(@NonNull Interceptor interceptor){
            interceptors.add(interceptor);
            return this;
        }

        @NonNull
        public Builder apiErrorCallback(@Nullable ApiSubscriber.ApiErrorCallback apiErrorCallback){
            this.apiErrorCallback = apiErrorCallback;
            return this;
        }

        @NonNull
        public WebApi build(@NonNull Application application){
            checkNotNull(baseUrl, "baseUrl == null");
            checkNotNull(logLevel, "logLevel == null");
            checkNotNull(application, "application == null");
            OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
            if (persistentCookie){
                okHttpClientBuilder.cookieJar(new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(application)));
            }
            if (!interceptors.isEmpty()){
                Iterator<Interceptor> iterator = interceptors.iterator();
                while (iterator.hasNext()){
                    okHttpClientBuilder.addInterceptor(iterator.next());
                }
            }
            if (!sslCertificates.isEmpty()){
                Set<Map.Entry<String, String>> certificates = sslCertificates.entrySet();
                if (certificates != null && !certificates.isEmpty()){
                    sslEncrypt(application.getApplicationContext(), okHttpClientBuilder, certificates);
                }
            }

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(logLevel);
            okHttpClientBuilder.addInterceptor(loggingInterceptor);
            Retrofit retrofit = new Retrofit.Builder().baseUrl(baseUrl)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(FastJsonConverterFactory.create())
                    .client(okHttpClientBuilder.build())
                    .build();
            return new WebApi(application, retrofit, serviceClasses, apiErrorCallback);
        }

        private void sslEncrypt(Context context, OkHttpClient.Builder okHttpClientBuilder, Set<Map.Entry<String, String>> certificates){
            try {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null);
                Iterator<Map.Entry<String, String>> iterator = certificates.iterator();
                final List<String> hostnames = new ArrayList<>();
                while (iterator.hasNext()){
                    Map.Entry<String, String>  certificate = iterator.next();
                    hostnames.add(certificate.getKey());
                    InputStream certificateIs = context.getAssets().open(certificate.getValue());
                    if (certificateIs == null){
                        continue;
                    }
                    keyStore.setCertificateEntry(certificate.getKey(), certificateFactory.generateCertificate(certificateIs));
                }
                SSLContext sslContext = SSLContext.getInstance("TLS");
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keyStore);
                sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
                okHttpClientBuilder.sslSocketFactory(sslContext.getSocketFactory());
                okHttpClientBuilder.hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return hostnames.contains(hostname);
                    }
                });

            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

}
