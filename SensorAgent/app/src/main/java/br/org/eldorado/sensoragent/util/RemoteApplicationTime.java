package br.org.eldorado.sensoragent.util;

import java.util.Queue;
import org.apache.commons.collections4.queue.CircularFifoQueue;

public class RemoteApplicationTime {

    //private static Log log = new Log("RemoteApplicationTime");
    private static long currentRemoteTime;
    private static long initialLocalTime;
    private static long offsetAverage;
    private static final Queue<Long> timeOffsets;

    static {
        currentRemoteTime= System.currentTimeMillis();
        initialLocalTime = System.currentTimeMillis();
        timeOffsets = new CircularFifoQueue<Long>(20);
        offsetAverage = 0;
    }

    public static long getCurrentRemoteTimeMillis() {
        long offset = System.currentTimeMillis() - initialLocalTime;
        return currentRemoteTime+offset;
    }

    public static long getServerTime() {
        return currentRemoteTime;
    }

    public static long getOffsetTime() {
        //log.i("timeOffsets: " + timeOffsets);
        //log.i("timeOffsets average: " + offsetAverage);
        return offsetAverage;
    }

    public static void setCurrentTimeMillis(long time, long offset) {
        timeOffsets.add(offset);
        offsetAverage = (long) timeOffsets.stream().mapToLong(a -> a).average().orElse(0);
        currentRemoteTime = time;
        initialLocalTime = System.currentTimeMillis();
    }
}
