package com.zhourh.rhwebapi;

import io.reactivex.Flowable;
import retrofit2.http.GET;

public interface GitHubService {

    @GET("orgs/octokit/repos")
    Flowable<GithubApiResult<String>> getOctokitRepos();
}
