package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.Util;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.TimeBar;

import com.fongmi.android.tv.R;

import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CustomSeekView extends FrameLayout implements Player.Listener, TimeBar.OnScrubListener {

    private static final int MAX_UPDATE_INTERVAL_MS = 1000;
    private static final int MIN_UPDATE_INTERVAL_MS = 200;
    //聚焦光带高度（轨道本身 4dp）与两端内缩：只加厚视觉高度，绝不缩放控件本体，
    //否则 DefaultTimeBar 内部画的正圆滑块会被纵向拉成椭圆。
    private static final float FOCUS_GLOW_HEIGHT_DP = 10f;
    private static final float FOCUS_GLOW_INSET_DP = 6f;

    private final StringBuilder timeBuilder = new StringBuilder();
    private final Formatter timeFormatter = new Formatter(timeBuilder, Locale.getDefault());
    private final TextView positionView;
    private final TextView durationView;
    private final DefaultTimeBar timeBar;
    private final View focusGlow;
    private final Runnable runnable;
    private long currentDuration;
    private long currentPosition;
    private long currentBuffered;
    private boolean scrubbing;
    private boolean attached;
    private Player player;

    public CustomSeekView(Context context) {
        this(context, null);
    }

    public CustomSeekView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomSeekView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_control_seek, this);
        positionView = findViewById(R.id.position);
        durationView = findViewById(R.id.duration);
        timeBar = findViewById(R.id.timeBar);
        runnable = this::updateProgress;
        timeBar.addListener(this);
        float density = getResources().getDisplayMetrics().density;
        int padH = (int) (density * 6);
        int padV = (int) (density * 2);
        setPadding(padH, padV, padH, padV);
        //聚焦光带垫在最下层（index 0），位置在 onLayout 里跟着 timeBar 的轨道走
        focusGlow = new View(context);
        focusGlow.setBackgroundResource(R.drawable.shape_control_seek_focus_glow);
        focusGlow.setAlpha(0f);
        focusGlow.setVisibility(INVISIBLE);
        addView(focusGlow, 0, new FrameLayout.LayoutParams(0, 0));
        //进度条的聚焦态不用焦点框：外描一圈白边在电视上更像“被框住”，与按钮的焦点样式也不统一。
        //改为跟随 timeBar 的焦点把已播放段/时间文字提亮，并在轨道下方淡入一条更厚的光带，
        //视觉上“变粗”；拖动滑块（thumb）固定用品牌橙，任何状态下都能看清进度位置，滑块仍是正圆。
        timeBar.setOnFocusChangeListener((view, hasFocus) -> applyFocusStyle(hasFocus));
        applyFocusStyle(false);
        resetView();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (focusGlow == null || timeBar == null || !(timeBar.getParent() instanceof View)) return;
        float density = getResources().getDisplayMetrics().density;
        View host = (View) timeBar.getParent();
        int height = Math.round(density * FOCUS_GLOW_HEIGHT_DP);
        int inset = Math.round(density * FOCUS_GLOW_INSET_DP);
        int centerY = host.getTop() + timeBar.getTop() + timeBar.getHeight() / 2;
        int startX = host.getLeft() + timeBar.getLeft() + inset;
        int endX = host.getLeft() + timeBar.getRight() - inset;
        if (endX <= startX) return;
        focusGlow.layout(startX, centerY - height / 2, endX, centerY + height / 2);
    }

    private void applyFocusStyle(boolean focused) {
        int accent = ContextCompat.getColor(getContext(), R.color.control_focus);
        //滑块固定用品牌橙：无论有没有焦点都能一眼看到当前进度位置
        int scrubber = ContextCompat.getColor(getContext(), R.color.control_scrubber);
        timeBar.setPlayedColor(focused ? accent : Color.WHITE);
        timeBar.setScrubberColor(scrubber);
        positionView.setTextColor(focused ? accent : Color.WHITE);
        durationView.setTextColor(focused ? accent : Color.WHITE);
        focusGlow.animate().cancel();
        if (focused) {
            focusGlow.setVisibility(VISIBLE);
            focusGlow.animate().alpha(1f).setDuration(160).start();
        } else {
            focusGlow.animate().alpha(0f).setDuration(140).withEndAction(() -> focusGlow.setVisibility(INVISIBLE)).start();
        }
    }

    public void setPlayer(Player player) {
        this.player = player;
        player.addListener(this);
        if (attached) updateTimeline();
    }

    /**
     * 暴露真正接收遥控器焦点的进度条本体。
     * 控制条里进度条与按钮行的上下顺序会随布局调整，焦点链路由使用方在代码里显式绑定，
     * 避免像 XML 里写死 @id/xxx 那样在该布局不存在同名控件时（直播控制条没有 prev）
     * 指向一个不存在的目标，导致焦点原地不动或丢失。
     */
    public View getFocusView() {
        return timeBar;
    }

    private String stringToTime(long time) {
        return Util.getStringForTime(timeBuilder, timeFormatter, time);
    }

    private void updateTimeline() {
        if (!attached || player == null) return;
        long duration = player.getDuration();
        if (duration < 0) duration = 0;
        currentDuration = duration;
        setKeyTimeIncrement(duration);
        timeBar.setDuration(duration);
        durationView.setText(stringToTime(duration));
        updateProgress();
    }

    private void updateProgress() {
        removeCallbacks(runnable);
        if (!attached || player == null) return;
        long position = player.getCurrentPosition();
        long buffered = player.getBufferedPosition();
        long duration = player.getDuration();
        if (duration < 0) duration = 0;
        if (duration != currentDuration) {
            currentDuration = duration;
            setKeyTimeIncrement(duration);
            timeBar.setDuration(duration);
            durationView.setText(stringToTime(duration));
        }
        if (position != currentPosition) {
            currentPosition = position;
            if (!scrubbing) {
                timeBar.setPosition(position);
                positionView.setText(stringToTime(position));
            }
        }
        if (buffered != currentBuffered) {
            currentBuffered = buffered;
            timeBar.setBufferedPosition(buffered);
        }
        if (player.isPlaying()) {
            postDelayed(runnable, delayMs(position));
        } else {
            postDelayed(runnable, MAX_UPDATE_INTERVAL_MS);
        }
    }

    private void resetView() {
        positionView.setText("00:00");
        durationView.setText("00:00");
        timeBar.setPosition(currentPosition = 0);
        timeBar.setDuration(currentDuration = 0);
        timeBar.setBufferedPosition(currentBuffered = 0);
    }

    private void setKeyTimeIncrement(long duration) {
        if (duration > TimeUnit.HOURS.toMillis(3)) {
            timeBar.setKeyTimeIncrement(TimeUnit.MINUTES.toMillis(5));
        } else if (duration > TimeUnit.MINUTES.toMillis(30)) {
            timeBar.setKeyTimeIncrement(TimeUnit.MINUTES.toMillis(1));
        } else if (duration > TimeUnit.MINUTES.toMillis(15)) {
            timeBar.setKeyTimeIncrement(TimeUnit.SECONDS.toMillis(30));
        } else if (duration > TimeUnit.MINUTES.toMillis(10)) {
            timeBar.setKeyTimeIncrement(TimeUnit.SECONDS.toMillis(15));
        } else if (duration > 0) {
            timeBar.setKeyTimeIncrement(TimeUnit.SECONDS.toMillis(10));
        }
    }

    private long delayMs(long position) {
        float speed = player.getPlaybackParameters().speed;
        long mediaTimeUntilNextFullSecondMs = 1000 - position % 1000;
        long mediaTimeDelayMs = Math.min(timeBar.getPreferredUpdateDelay(), mediaTimeUntilNextFullSecondMs);
        long delayMs = (long) (mediaTimeDelayMs / Math.max(speed, 0.1f));
        return Util.constrainValue(delayMs, MIN_UPDATE_INTERVAL_MS, MAX_UPDATE_INTERVAL_MS);
    }

    private void seekToTimeBarPosition(long positionMs) {
        player.seekTo(positionMs);
        updateProgress();
        player.play();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        updateTimeline();
    }

    @Override
    protected void onDetachedFromWindow() {
        attached = false;
        removeCallbacks(runnable);
        super.onDetachedFromWindow();
    }

    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        resetView();
    }

    @Override
    public void onScrubStart(@NonNull TimeBar timeBar, long position) {
        scrubbing = true;
        positionView.setText(stringToTime(position));
    }

    @Override
    public void onScrubMove(@NonNull TimeBar timeBar, long position) {
        positionView.setText(stringToTime(position));
    }

    @Override
    public void onScrubStop(@NonNull TimeBar timeBar, long position, boolean canceled) {
        scrubbing = false;
        if (!canceled) seekToTimeBarPosition(position);
    }
}
