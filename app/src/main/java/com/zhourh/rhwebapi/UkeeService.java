package com.zhourh.rhwebapi;

import java.util.List;

import io.reactivex.Flowable;
import retrofit2.http.GET;

public interface UkeeService {

    @GET("help/questions")
    Flowable<UApiResult<List<CommonQuestionDO>>> getQuestions();
}
