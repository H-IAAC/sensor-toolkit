package br.org.eldorado.hiaac.datacollector.api;

import com.google.gson.JsonObject;

import okhttp3.MultipartBody;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.Call;

public interface ApiInterface {
    @Multipart
    @POST("/api/file")
    Call<StatusResponse> uploadFile(@Part MultipartBody.Part file, @Part MultipartBody.Part experiment,
                                    @Part MultipartBody.Part subject, @Part MultipartBody.Part name);

    @Multipart
    @POST("/api/config")
    Call<StatusResponse> uploadConfigFile(@Part MultipartBody.Part file, @Part MultipartBody.Part experiment,
                                    @Part MultipartBody.Part subject, @Part MultipartBody.Part activity, @Part MultipartBody.Part name);

    @GET("/api/timestamp")
    Call<JsonObject> getServerTime();

//    @GET("/api/timestamp")
//    Call<JsonObject> getServerTime();
}
