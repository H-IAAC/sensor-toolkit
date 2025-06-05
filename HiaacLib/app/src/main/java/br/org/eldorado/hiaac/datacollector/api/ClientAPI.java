package br.org.eldorado.hiaac.datacollector.api;


import java.util.concurrent.TimeUnit;

import br.org.eldorado.hiaac.datacollector.util.Preferences;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ClientAPI {
    private static final HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
    public ClientAPI() {
        interceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
    }
    public Retrofit getClient(String address, String port, OkHttpClient httpClient) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://" + address + ":" + port)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build();
        return retrofit;
    }

    public static ApiInterface get(OkHttpClient httpClient) {
        ClientAPI api = new ClientAPI();
        String address = Preferences.getPreferredServer().split(":")[0];
        String port = Preferences.getPreferredServer().split(":")[1];
        return api.getClient(address, port, httpClient).create(ApiInterface.class);
    }

    public static OkHttpClient httpHighTimeout() {
        return new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(5, TimeUnit.MINUTES)
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.MINUTES)
                .build();
    }

    public static OkHttpClient httpLowTimeout() {
        return new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .readTimeout(5, TimeUnit.SECONDS)
                .callTimeout(5, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();
    }
}
