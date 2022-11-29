package br.org.eldorado.sensoragent.util;

public class RemoteApplicationTime {

    private static final String TAG = "RemoteApplicationTime";
    private static long currentRemoteTime;
    private static long initialLocalTime;

    static {
        currentRemoteTime= System.currentTimeMillis();
        initialLocalTime = System.currentTimeMillis();
    }

    public static long getCurrentRemoteTimeMillis() {
        long offset = System.currentTimeMillis() - initialLocalTime;
        return currentRemoteTime+offset;
    }

    public static void setCurrentTimeMillis(long time) {
        currentRemoteTime = time;
    }
}
