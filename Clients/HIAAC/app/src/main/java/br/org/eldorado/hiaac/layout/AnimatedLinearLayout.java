package br.org.eldorado.hiaac.layout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import br.org.eldorado.hiaac.util.Tools;

public class AnimatedLinearLayout extends LinearLayout {
    public AnimatedLinearLayout(Context context) {
        super(context);
    }

    public AnimatedLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimatedLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AnimatedLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void expand(float newHeight) {
        postDelayed(() -> {
            for (int i = 0; i < getChildCount(); i++) {
                View v = getChildAt(i);
                v.setVisibility(VISIBLE);
            }
        }, 200);
        Tools.slideView(this, Tools.pixelType.sp, 0, newHeight);
    }

    public void close() {
        postDelayed(() -> {
            for (int i = 0; i < getChildCount(); i++) {
                View v = getChildAt(i);
                v.setVisibility(GONE);
            }
        }, 200);
        Tools.slideView(this, Tools.pixelType.px, getHeight(), 0);
    }
}
