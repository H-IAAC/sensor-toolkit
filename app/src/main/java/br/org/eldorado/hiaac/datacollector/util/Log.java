package br.org.eldorado.hiaac.datacollector.util;


public class Log {
    private static final String APP_TAG = "HIAAC-";
    private String classTag;

    public Log(String tag) {
        this.classTag = tag;
    }

    public void d(String msg) {
        android.util.Log.d(APP_TAG+classTag, msg);
    }

    public void i(String msg) {
        android.util.Log.i(APP_TAG+classTag, msg);
    }

    public void e(String msg) {
        android.util.Log.e(APP_TAG+classTag, msg);
    }
}
