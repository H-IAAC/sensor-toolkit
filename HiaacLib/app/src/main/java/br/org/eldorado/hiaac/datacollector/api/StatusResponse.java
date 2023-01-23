package br.org.eldorado.hiaac.datacollector.api;

import com.google.gson.annotations.SerializedName;

public class StatusResponse {
    @SerializedName("status")
    public String status;

    public String getStatus() {
        return status;
    }
}
