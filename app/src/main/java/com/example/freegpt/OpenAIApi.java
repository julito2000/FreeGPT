package com.example.freegpt;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface OpenAIApi {
    //@POST("api/chat/completions")
    @POST("v1/chat/completions")
    Call<OpenAIResponse> sendMessage(@Body OpenAIRequest body);
}
