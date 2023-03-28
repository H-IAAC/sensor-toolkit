package br.org.eldorado.hiaac.datacollector.api;

import com.google.gson.JsonObject;

import okhttp3.MultipartBody;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.Call;
import retrofit2.http.Query;

public interface ApiInterface {
    @Multipart
    @POST("/api/file")
    Call<StatusResponse> uploadFile(@Part MultipartBody.Part file, @Part MultipartBody.Part experiment,
                                    @Part MultipartBody.Part subject, @Part MultipartBody.Part name);

    @Multipart
    @POST("/api/config")
    Call<StatusResponse> uploadConfigFile(@Part MultipartBody.Part file, @Part MultipartBody.Part experiment,
                                    @Part MultipartBody.Part subject, @Part MultipartBody.Part activity);

    @GET("/api/timestamp")
    Call<JsonObject> getServerTime();

    @GET("/api/config")
    Call<JsonObject> getExperimentConfig(@Query("experiment")String experiment, @Query("subject")String subject, @Query("activity")String activity);

    @GET("/api/experiments")
    Call<JsonObject> getAllExperiments();
}
