package br.org.eldorado.hiaac.datacollector.util;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import br.org.eldorado.sensoragent.model.Accelerometer;
import br.org.eldorado.sensoragent.model.AmbientTemperature;
import br.org.eldorado.sensoragent.model.GPS;
import br.org.eldorado.sensoragent.model.Gravity;
import br.org.eldorado.sensoragent.model.Gyroscope;
import br.org.eldorado.sensoragent.model.LinearAccelerometer;
import br.org.eldorado.sensoragent.model.Luminosity;
import br.org.eldorado.sensoragent.model.MagneticField;
import br.org.eldorado.sensoragent.model.Proximity;
import br.org.eldorado.sensoragent.model.SensorBase;

public class Tools {
    public static final int CHRONOMETER = 1;
    private static final Log log = new Log("Tools");

    public enum pixelType {
        px,
        dp,
        sp
    }

    public static void slideView(View view,
                                 pixelType type,
                                 float currentHeight,
                                 float newHeight) {

        int currentHeightPx = 0;
        int newHeightPx = 0;

        switch (type) {
            case px:
                currentHeightPx = (int) currentHeight;
                newHeightPx = (int) newHeight;
                break;
            case dp:
                currentHeightPx = dpToPx(currentHeight, view.getContext());
                newHeightPx = dpToPx(newHeight, view.getContext());;
                break;
            case sp:
                currentHeightPx = spToPx(currentHeight, view.getContext());
                newHeightPx = spToPx(newHeight, view.getContext());;
                break;
        }

        ValueAnimator slideAnimator = ValueAnimator
                .ofInt(currentHeightPx, newHeightPx)
                .setDuration(500);

        slideAnimator.addUpdateListener(animation1 -> {
            Integer value = (Integer) animation1.getAnimatedValue();
            view.getLayoutParams().height = value.intValue();
            view.requestLayout();
        });

        AnimatorSet animationSet = new AnimatorSet();
        animationSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animationSet.play(slideAnimator);
        animationSet.start();
    }

    public static int dpToPx(float dp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static int spToPx(float sp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    public static ArrayList<String> createTimeFormatedList(int[] values) {
        ArrayList<String> list = new ArrayList<>();
        for (int v : values) {
            list.add(getFormatedTime(v, 0));
        }

        return list;
    }

    public static String getFormatedTime(int v, int type) {
        int hours = v / 3600;
        int days = hours / 24;
        int minutes = (v % 3600) / 60;
        int seconds = v % 60;
        if (hours >= 24 && type != CHRONOMETER) {
            return String.format("%d days", hours/24);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static SensorBase getSensorFromTitleName(String title) {
        switch (title) {
            case Accelerometer.TAG:
                return new Accelerometer();
            case LinearAccelerometer.TAG:
                return new LinearAccelerometer();
            case AmbientTemperature.TAG:
                return new AmbientTemperature();
            case Gyroscope.TAG:
                return new Gyroscope();
            case Luminosity.TAG:
                return new Luminosity();
            case MagneticField.TAG:
                return new MagneticField();
            case Proximity.TAG:
                return new Proximity();
            case Gravity.TAG:
                return new Gravity();
            case GPS.TAG:
                return new GPS();
        }
        return null;
    }

    public static ArrayList<String> createHertzList(List<Integer> values) {
        ArrayList<String> list = new ArrayList<>();
        for (int v : values) {
            if (v == 0) {
                list.add("Add frequency");
                continue;
            }
            list.add(v + " Hz");
        }

        return list;
    }

    public static String getFileExtension(String filename) {
        return filename.substring(filename.length() - 3);
    }

    public static File zipFile(File source) throws IOException {
        if (source == null) {
            throw new IllegalArgumentException("The file to be zipped must not be null!");
        }

        String zipFileName = source.getName();
        zipFileName = zipFileName.substring(0, zipFileName.lastIndexOf(".")+1) + "zip";
        File zipFileDestination = new File(source.getParent(), zipFileName);
        if (zipFileDestination.exists()) {
            zipFileDestination.delete();
        }
        File zipFile = zip(source, zipFileDestination);
        return zipFile;
    }

    private static File zip(File source, File zipFile) throws IOException {
        int relativeStartingPathIndex = zipFile.getAbsolutePath().lastIndexOf("/") + 1;
        if (source != null && zipFile != null) {
            try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream( new FileOutputStream(zipFile)))) {
                if (source.isDirectory()) {
                    zipSubDir(out, source, relativeStartingPathIndex);
                } else {
                    try (BufferedInputStream origin = new BufferedInputStream(new FileInputStream(source))) {
                        zipEntryFile(origin, out, source, relativeStartingPathIndex);
                    }
                }
            } catch (Exception e) {
                log.d("Failed when creating zip file: " + e.getMessage());
            }
        }

        return zipFile;
    }

    private static void zipSubDir(ZipOutputStream out, File dir, int relativeStartingPathIndex) throws IOException {

        File[] files = dir.listFiles();
        if (files != null) {

            for(File file : files) {
                if(file.isDirectory()) {
                    zipSubDir(out, file, relativeStartingPathIndex);
                } else {
                    try (BufferedInputStream origin = new BufferedInputStream(new FileInputStream(file))) {
                        zipEntryFile(origin, out, file, relativeStartingPathIndex);
                    }
                }
            }

        }
    }

    private static void zipEntryFile(BufferedInputStream origin, ZipOutputStream out, File file, int relativeStartingPathIndex) throws IOException {
        String relativePath = file.getAbsolutePath().substring(relativeStartingPathIndex);
        ZipEntry entry = new ZipEntry(relativePath);
        entry.setTime(file.lastModified()); // to keep modification time after unzipping
        out.putNextEntry(entry);
        byte[] data = new byte[2048];
        int count;
        while ((count = origin.read(data, 0, 2048)) != -1) {
            out.write(data, 0, count);
        }
    }

}
