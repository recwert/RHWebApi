# RHWebApi

[![](https://jitpack.io/v/recwert/RHWebApi.svg)](https://jitpack.io/#recwert/RHWebApi)

RHWebApi是基于RxJava2、Retrofit、FastJson封装的Http请求框架

Retrofit已经十分方便开发者进行Http Restful格式的访问了，但是在这之上，如果希望和RxJava2，FastJson等进行结合，还需要一定的代码量，RHWebApi对此进行了封装，让上层开发时只需要关心Http请求的定义就可以了，其他工作都交给RHWebApi，目前RHWebApi封装了以下功能：

- 统一数据返回格式，上层需要实现`ApiResult`接口
- 日志系统，用户可自定义日志级别
- 可统一处理错误信息
- 自动进行线程切换
- 可开关式Cookie存储
- 可取消请求


## 系统要求

RHWebApi 支持Android 2.3(Gingerbread)或以上

## 引入

如果你还在使用Eclipse，就绕道吧，或者自己拷贝源码吧。Android Studio（gradle）继续往下看

在你的Application的build.gradle文件的`allprojects`节点添加`jitpack`库

```groovy
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}

```
在你的app的build.gradle文件的`dependencies`节点添加`RHWebApi`

```groovy
compile 'com.github.recwert:RHWebApi:0.1.3'

```

## 使用

```java
//自定义返回格式，需要实现ApiResult
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

```

```java
//初始化RHWebApi，获取WebApi实例
 WebApi webApi = new WebApi.Builder().baseUrl("http://XXX.XXX.XXX/")
                    .logLevel(HttpLoggingInterceptor.Level.BODY)
                    .persistentCookie(true)
                    .addService(0, UserService.class)
                    .apiErrorCallback(new ApiSubscriber.ApiErrorCallback() {
                        @Override
                        public void onSocketTimeoutException(SocketTimeoutException e) {
                            ToastUtils.showToast("访问服务器超时，请检查网络");
                        }

                        @Override
                        public void onSocketException(SocketException e) {
                            ToastUtils.showToast("无法连接服务器，请检查网络");
                        }

                        @Override
                        public void onProtocolException(ProtocolException e) {
                            ToastUtils.showToast("无法连接服务器，请检查网络");
                        }

                        @Override
                        public void onHttpException(HttpException e) {
                            ToastUtils.showToast("无法连接服务器，请检查网络");
                        }


                        @Override
                        public void onApiException(ApiException e) {
                            ToastUtils.showToast( e.getMessage());
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                        }
                    }).addInterceptor(new YourInterceptor()).build(application);

        }
```

```java
//普通的接口定义
public interface UserService {
 
    @POST("users")
    @FormUrlEncoded
    @Headers("Content-Type: application/x-www-form-urlencoded;charset=utf-8" )
    Flowable<UApiResult<UserDO>> createUser(@Field("phoneNumber") String phoneNumber, 
                                            @Field("password") String password);
                                            

```

```java
//发起请求
int registRequestId = webApi.request(userService.createUser(phoneNumber, password), new ApiSubscriber<UserDO>() {
            @Override
            public void onNext(UserDO userDO) {
                //此处的userDO即为上层需要的数据  
            }
        });
        
//取消请求   
webApi.cancelRequest(registRequestId);
        
```


