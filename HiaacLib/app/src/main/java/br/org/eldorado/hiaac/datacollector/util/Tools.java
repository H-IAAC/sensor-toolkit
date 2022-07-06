package br.org.eldorado.hiaac.datacollector.util;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import java.util.ArrayList;

public class Tools {
    public static final int CRONOMETER = 1;

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
        if (hours >= 24 && type != CRONOMETER) {
            return String.format("%d days", hours/24);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static ArrayList<String> createHertzList(int[] values) {
        ArrayList<String> list = new ArrayList<>();
        for (int v : values) {
            list.add(v + " Hz");
        }

        return list;
    }
}
