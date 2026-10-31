package com.fongmi.android.tv.ui.custom;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textview.MaterialTextView;

/**
 * 海报下方的片名条：选中时「底色翻白 + 文字转深色」，并带 160ms 过渡。
 * <p>
 * 为什么不用 selector：XML 的 state list 只能瞬间切换颜色，做不出平滑过渡。
 * 这里用一个自定义 View 自己吃焦点状态（配合布局里的 {@code duplicateParentState="true"}，
 * 父容器（整张海报）获得焦点时子 View 也会收到状态变化），再用 ValueAnimator 在
 * 深色底/白字 ←→ 白底/深字之间插值，所有海报页共用同一份实现，表现自然一致。
 */
public class PosterNameView extends MaterialTextView {

    private static final int COLOR_BG_NORMAL = 0x33000000;
    private static final int COLOR_BG_SELECTED = 0xFFFFFFFF;
    private static final int COLOR_TEXT_NORMAL = 0xFFFFFFFF;
    private static final int COLOR_TEXT_SELECTED = 0xFF121212;
    private static final long DURATION = 160L;
    private static final float CORNER_DP = 8f;

    private final GradientDrawable background = new GradientDrawable();
    private final ArgbEvaluator evaluator = new ArgbEvaluator();
    private ValueAnimator animator;
    private float progress;
    private boolean highlighted;

    public PosterNameView(@NonNull Context context) {
        this(context, null);
    }

    public PosterNameView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PosterNameView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(COLOR_BG_NORMAL);
        setBackground(background);
        setTextColor(COLOR_TEXT_NORMAL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //只保留下方两角圆角，与海报卡片的下沿一致
        float radius = CORNER_DP * getResources().getDisplayMetrics().density;
        background.setCornerRadii(new float[]{0, 0, 0, 0, radius, radius, radius, radius});
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        boolean now = hasState(android.R.attr.state_focused) || hasState(android.R.attr.state_selected);
        if (now == highlighted) return;
        highlighted = now;
        animateTo(now);
    }

    private boolean hasState(int target) {
        for (int state : getDrawableState()) if (state == target) return true;
        return false;
    }

    private void animateTo(boolean selected) {
        if (animator != null) animator.cancel();
        float from = progress;
        float to = selected ? 1f : 0f;
        if (from == to) return;
        animator = ValueAnimator.ofFloat(from, to);
        animator.setDuration((long) (DURATION * Math.abs(to - from)));
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> applyProgress((float) a.getAnimatedValue()));
        animator.start();
    }

    private void applyProgress(float value) {
        progress = value;
        background.setColor((int) evaluator.evaluate(value, COLOR_BG_NORMAL, COLOR_BG_SELECTED));
        setTextColor((int) evaluator.evaluate(value, COLOR_TEXT_NORMAL, COLOR_TEXT_SELECTED));
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }
}
