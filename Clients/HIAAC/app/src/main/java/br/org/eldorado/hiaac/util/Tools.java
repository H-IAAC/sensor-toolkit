package br.org.eldorado.hiaac.util;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class Tools {
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
}
