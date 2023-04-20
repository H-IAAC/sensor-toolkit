package br.org.eldorado.hiaac.datacollector.api;


import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ClientAPI {

    private static Retrofit retrofit = null;

    public static Retrofit getClient(String address, String port) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .readTimeout(5, TimeUnit.MINUTES)
                .callTimeout(5, TimeUnit.MINUTES)
                .connectTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl("http://" + address + ":" + port)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        return retrofit;
    }
}
