package com.fongmi.android.tv.ui.custom;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.impl.SiteListener;
import com.fongmi.android.tv.utils.KeyUtil;

/**
 * 首页左上角的品牌组合（图标 + 影视 Videos）。
 * 焦点从原来的源名称标题移到这里：同样的点击弹源列表、长按重载配置、上键刷新。
 * 选中态不加焦点框，改为慢速呼吸（透明度 30%~100% 往返），避免框线压住字标。
 */
public class CustomLogoBar extends LinearLayoutCompat {

    private static final long BLINK_DURATION = 1200L;
    private static final float BLINK_MIN_ALPHA = 0.3f;

    private Listener listener;
    private boolean coolDown;
    private ObjectAnimator blink;

    private Site getHome() {
        return VodConfig.get().getHome();
    }

    public CustomLogoBar(@NonNull Context context) {
        super(context);
    }

    public CustomLogoBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomLogoBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
        setOnClickListener(v -> listener.showDialog());
        setOnLongClickListener(v -> {
            listener.reloadConfig();
            return true;
        });
    }

    private boolean hasEvent(KeyEvent event) {
        //左右键留给焦点走位，上键仍用于刷新当前源的首页内容
        return !getHome().isEmpty() && KeyUtil.isUpKey(event) && !coolDown;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused) startBlink();
        else stopBlink();
    }

    private void startBlink() {
        stopBlink();
        blink = ObjectAnimator.ofFloat(this, View.ALPHA, 1f, BLINK_MIN_ALPHA);
        blink.setDuration(BLINK_DURATION);
        blink.setStartDelay(160);
        blink.setRepeatCount(ValueAnimator.INFINITE);
        blink.setRepeatMode(ValueAnimator.REVERSE);
        blink.setInterpolator(new AccelerateDecelerateInterpolator());
        blink.start();
    }

    private void stopBlink() {
        if (blink != null) {
            blink.cancel();
            blink = null;
        }
        setAlpha(1f);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopBlink();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!hasEvent(event)) return super.dispatchKeyEvent(event);
        if (KeyUtil.isActionDown(event)) {
            App.post(() -> coolDown = false, 3000);
            listener.onRefresh();
            coolDown = true;
        }
        return true;
    }

    public interface Listener extends SiteListener {

        void showDialog();

        void onRefresh();

        void reloadConfig();
    }
}
