package com.zhourh.rhwebapi;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.zhourh.webapi.core.ApiSubscriber;
import com.zhourh.webapi.core.WebApi;

import java.security.UnrecoverableKeyException;
import java.util.List;

import okhttp3.logging.HttpLoggingInterceptor;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WebApi webApi = new WebApi.Builder().baseUrl("https://www.ybadminton.com/")
                .logLevel(HttpLoggingInterceptor.Level.BODY)
                .addService(0, UkeeService.class)
                .addTrustDomain("www.ybadminton.com")
                .build(getApplication());

        UkeeService ukeeService = (UkeeService) webApi.getService(0);

        webApi.request(ukeeService.getQuestions(), new ApiSubscriber<List<CommonQuestionDO>>() {
            @Override
            public void onNext(List<CommonQuestionDO> commonQuestionDOS) {
                Log.d("MainActivity", commonQuestionDOS.toString());
            }
        });

    }
}
