package com.fongmi.android.tv.utils;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;

/**
 * 全局提示条（tips）。
 *
 * <p>不用系统 Toast：Android 11+ 起自定义 Toast 视图会被系统替换回默认浅色气泡（带启动图标卡片底），
 * 且 Toast 无法调整位置，在播放页会压住控制条。这里用一层“不抢焦点、不吃触摸”的全屏透明窗口承载统一视图
 * （黑底 + 无黑底三角图标 + 白字），固定在距底 24dp 处；后加入的窗口天然盖在弹窗之上，
 * 因此设置弹窗里弹出的提示也能看到。</p>
 *
 * <p>播放页不需要任何提示：{@link #setBlocked(boolean)} 打开后直接吞掉提示，也不会退回系统 Toast。</p>
 */
public final class Tip {

    private static final long DURATION = 3000;

    private static Dialog dialog;
    private static View content;
    private static TextView textView;
    private static Activity host;
    private static boolean blocked;

    private static final Runnable HIDE = Tip::dismiss;

    private Tip() {
    }

    /** 播放页等场景整体关闭提示：避免提示窗压住控制条，返回 true 表示已处理（不退回系统 Toast） */
    public static void setBlocked(boolean value) {
        blocked = value;
        if (value) dismiss();
    }

    /** @return true 表示已经在当前界面画出提示；false 表示当前没有可用的 Activity（调用方退回系统 Toast） */
    public static boolean show(CharSequence text) {
        if (TextUtils.isEmpty(text)) return true;
        if (blocked) return true;
        Activity activity = App.activity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return false;
        try {
            if (dialog == null || host != activity) create(activity);
            if (dialog == null) return false;
            textView.setText(text);
            applyPosition();
            if (!dialog.isShowing()) dialog.show();
            content.animate().cancel();
            content.setAlpha(0f);
            content.animate().alpha(1f).setDuration(120).start();
            App.post(HIDE, DURATION);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void dismiss() {
        App.removeCallbacks(HIDE);
        if (dialog == null) return;
        Dialog current = dialog;
        View view = content;
        dialog = null;
        content = null;
        textView = null;
        host = null;
        if (view != null) {
            view.animate().cancel();
            view.animate().alpha(0f).setDuration(160).withEndAction(() -> {
                try {
                    if (current.isShowing()) current.dismiss();
                } catch (Exception ignored) {
                }
            }).start();
        } else {
            try {
                if (current.isShowing()) current.dismiss();
            } catch (Exception ignored) {
            }
        }
    }

    private static void create(Activity activity) {
        dismiss();
        Dialog created = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        created.requestWindowFeature(Window.FEATURE_NO_TITLE);
        created.setCancelable(false);
        created.setCanceledOnTouchOutside(false);
        FrameLayout root = new FrameLayout(activity);
        root.setClipChildren(false);
        View view = LayoutInflater.from(activity).inflate(R.layout.view_app_tip, root, false);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        params.bottomMargin = inset();
        root.addView(view, params);
        created.setContentView(root);
        Window window = created.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setWindowAnimations(0);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(0f);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        dialog = created;
        content = view;
        textView = view.findViewById(R.id.tipText);
        host = activity;
    }

    private static void applyPosition() {
        if (content == null) return;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) content.getLayoutParams();
        int value = inset();
        if (params.bottomMargin == value) return;
        params.bottomMargin = value;
        content.setLayoutParams(params);
    }

    private static int inset() {
        return ResUtil.dp2px(24);
    }
}
