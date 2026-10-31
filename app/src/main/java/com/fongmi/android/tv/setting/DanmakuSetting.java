package com.fongmi.android.tv.setting;

import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.media3.ui.danmaku.DanmakuConfig;

import com.fongmi.android.tv.api.config.VodConfig;
import com.github.catvod.utils.Prefers;

public class DanmakuSetting {

    //内置弹幕接口地址留空，由用户在设置中自行配置。
    private static final String DEFAULT_API_URL = "";

    //（旧版内置地址已停用，留空避免误用）
    private static final String LEGACY_API_URL = "";

    //首次安装/升级时把弹幕开关落盘成显式值，避免继续依赖取值默认值
    private static final String KEY_DEFAULTS_APPLIED = "danmaku_defaults_v1";

    private static final float MIN_TEXT_SCALE = 0.5f;
    private static final float MAX_TEXT_SCALE = 3.0f;
    private static final float MIN_TRANSPARENCY = 0.0f;
    private static final float MAX_TRANSPARENCY = 0.9f;
    private static final float MIN_STROKE_WIDTH_MULTIPLIER = 0.05f;
    private static final float MAX_STROKE_WIDTH_MULTIPLIER = 0.3f;
    private static final float MIN_PROJECTION_OFFSET = 0.02f;
    private static final float MAX_PROJECTION_OFFSET = 0.15f;
    private static final long MIN_TIME_OFFSET_MS = -300000L;
    private static final long MAX_TIME_OFFSET_MS = 300000L;
    private static final long MIN_DURATION_MS = 3000L;
    private static final long MAX_DURATION_MS = 15000L;
    private static final long MIN_FIXED_DURATION_MS = 2000L;
    private static final long MAX_FIXED_DURATION_MS = 10000L;
    private static final int MIN_MAX_ON_SCREEN = 10;
    private static final int MAX_MAX_ON_SCREEN = 500;
    private static final float MIN_SCROLL_AREA_RATIO = 0.1f;
    private static final float MAX_SCROLL_AREA_RATIO = 1.0f;
    private static final float MIN_SCROLL_GAP_RATIO = 0.0f;
    private static final float MAX_SCROLL_GAP_RATIO = 5.0f;
    private static final float MIN_LINE_SPACING = 1.0f;
    private static final float MAX_LINE_SPACING = 2.0f;
    private static final int MIN_MAX_SCROLL_LINES = 0;
    private static final int MAX_MAX_SCROLL_LINES = 20;
    private static final int MIN_MAX_FIXED_LINES = 0;
    private static final int MAX_MAX_FIXED_LINES = 10;

    /**
     * 把「弹幕加载 / 弹幕显示 / 自动搜索 / 爬虫优先」的开关在首次运行时写入本地配置。
     * <p>
     * 这些开关的语义是默认开启，但此前只靠 SharedPreferences 的取值默认值生效：
     * 历史版本里 danmaku_auto 的默认值曾是 false，界面显示「已开启」，
     * 而 DanmakuApi.canSearch() 判定为假，首次安装后从不发起弹幕搜索，
     * 必须在设置里把开关关开一次写入显式值才恢复正常。
     * 这里只补写缺失的键，不会覆盖用户已有的选择。
     */
    public static void ensureDefaults() {
        if (Prefers.getBoolean(KEY_DEFAULTS_APPLIED, false)) return;
        SharedPreferences prefers = Prefers.getPrefers();
        if (!prefers.contains("danmaku_load")) putLoad(true);
        if (!prefers.contains("danmaku_show")) putShow(true);
        if (!prefers.contains("danmaku_auto")) putAuto(true);
        if (!prefers.contains("danmaku_spider_first")) putSpiderFirst(true);
        Prefers.put(KEY_DEFAULTS_APPLIED, true);
    }

    public static boolean isLoad() {
        //弹幕默认开启：首次启动无偏好值时按启用处理
        return Prefers.getBoolean("danmaku_load", true);
    }

    public static void putLoad(boolean danmakuLoad) {
        Prefers.put("danmaku_load", danmakuLoad);
    }

    public static void putEnabled(boolean enabled) {
        DanmakuState.Switches switches = DanmakuState.setMaster(enabled);
        putLoad(switches.load());
        putShow(switches.show());
    }

    public static boolean isEnabled() {
        return DanmakuState.isEnabled(isLoad(), isShow());
    }

    public static boolean isAuto() {
        //自动加载默认开启：与 isLoad()/isShow() 的默认值保持一致。
        //DanmakuApi.canSearch() 要求 load && auto 同时为真，此前 auto 默认 false，
        //导致首次安装后「弹幕加载」显示开启、实际却从不发起搜索。
        return Prefers.getBoolean("danmaku_auto", true);
    }

    public static void putAuto(boolean auto) {
        Prefers.put("danmaku_auto", auto);
    }

    public static boolean isSpiderFirst() {
        //爬虫优先默认开启：点播源自带弹幕优先于在线接口结果
        return Prefers.getBoolean("danmaku_spider_first", true);
    }

    public static void putSpiderFirst(boolean spiderFirst) {
        Prefers.put("danmaku_spider_first", spiderFirst);
    }

    public static String getApiUrl() {
        //弹幕接口默认值：内置公共接口，设置页可改
        String url = Prefers.getString("danmaku_api_url", DEFAULT_API_URL);
        if (LEGACY_API_URL.equals(url)) {
            //迁移：存的是旧内置地址（没有真正自定义过）→ 换成新的内置地址
            putApiUrl(DEFAULT_API_URL);
            return DEFAULT_API_URL;
        }
        return url;
    }

    public static void putApiUrl(String url) {
        Prefers.put("danmaku_api_url", url);
    }

    public static boolean isShow() {
        return Prefers.getBoolean("danmaku_show", true);
    }

    public static void putShow(boolean danmakuShow) {
        Prefers.put("danmaku_show", danmakuShow);
    }

    public static float getTextScale() {
        return Math.clamp(Prefers.getFloat("danmaku_text_scale", 1f), MIN_TEXT_SCALE, MAX_TEXT_SCALE);
    }

    public static void putTextScale(float value) {
        Prefers.put("danmaku_text_scale", Math.clamp(value, MIN_TEXT_SCALE, MAX_TEXT_SCALE));
    }

    public static float getTransparency() {
        return Math.clamp(Prefers.getFloat("danmaku_transparency", 0f), MIN_TRANSPARENCY, MAX_TRANSPARENCY);
    }

    public static void putTransparency(float value) {
        Prefers.put("danmaku_transparency", Math.clamp(value, MIN_TRANSPARENCY, MAX_TRANSPARENCY));
    }

    public static boolean isTextBold() {
        return Prefers.getBoolean("danmaku_text_bold");
    }

    public static void putTextBold(boolean value) {
        Prefers.put("danmaku_text_bold", value);
    }

    public static int getStyleMode() {
        return Math.clamp(Prefers.getInt("danmaku_style_mode", DanmakuConfig.STYLE_STROKE), DanmakuConfig.STYLE_NONE, DanmakuConfig.STYLE_PROJECTION);
    }

    public static void putStyleMode(int value) {
        Prefers.put("danmaku_style_mode", Math.clamp(value, DanmakuConfig.STYLE_NONE, DanmakuConfig.STYLE_PROJECTION));
    }

    public static int getColorMode() {
        return Math.clamp(Prefers.getInt("danmaku_color_mode", DanmakuConfig.COLOR_MODE_DEFAULT), DanmakuConfig.COLOR_MODE_DEFAULT, DanmakuConfig.COLOR_MODE_GRADIENT);
    }

    public static void putColorMode(int value) {
        Prefers.put("danmaku_color_mode", Math.clamp(value, DanmakuConfig.COLOR_MODE_DEFAULT, DanmakuConfig.COLOR_MODE_GRADIENT));
    }

    public static float getShadowTransparency() {
        return Math.clamp(Prefers.getFloat("danmaku_shadow_transparency", 0.1f), MIN_TRANSPARENCY, MAX_TRANSPARENCY);
    }

    public static void putShadowTransparency(float value) {
        Prefers.put("danmaku_shadow_transparency", Math.clamp(value, MIN_TRANSPARENCY, MAX_TRANSPARENCY));
    }

    public static float getStrokeWidthMultiplier() {
        return Math.clamp(Prefers.getFloat("danmaku_stroke_width_multiplier", 0.12f), MIN_STROKE_WIDTH_MULTIPLIER, MAX_STROKE_WIDTH_MULTIPLIER);
    }

    public static void putStrokeWidthMultiplier(float value) {
        Prefers.put("danmaku_stroke_width_multiplier", Math.clamp(value, MIN_STROKE_WIDTH_MULTIPLIER, MAX_STROKE_WIDTH_MULTIPLIER));
    }

    public static float getProjectionOffsetX() {
        return Math.clamp(Prefers.getFloat("danmaku_projection_offset_x", 0.08f), MIN_PROJECTION_OFFSET, MAX_PROJECTION_OFFSET);
    }

    public static void putProjectionOffsetX(float value) {
        Prefers.put("danmaku_projection_offset_x", Math.clamp(value, MIN_PROJECTION_OFFSET, MAX_PROJECTION_OFFSET));
    }

    public static float getProjectionOffsetY() {
        return Math.clamp(Prefers.getFloat("danmaku_projection_offset_y", 0.08f), MIN_PROJECTION_OFFSET, MAX_PROJECTION_OFFSET);
    }

    public static void putProjectionOffsetY(float value) {
        Prefers.put("danmaku_projection_offset_y", Math.clamp(value, MIN_PROJECTION_OFFSET, MAX_PROJECTION_OFFSET));
    }

    public static float getProjectionTransparency() {
        return Math.clamp(Prefers.getFloat("danmaku_projection_transparency", 0.2f), MIN_TRANSPARENCY, MAX_TRANSPARENCY);
    }

    public static void putProjectionTransparency(float value) {
        Prefers.put("danmaku_projection_transparency", Math.clamp(value, MIN_TRANSPARENCY, MAX_TRANSPARENCY));
    }

    public static long getDurationMs() {
        return Math.clamp(Prefers.getLong("danmaku_duration", 8000L), MIN_DURATION_MS, MAX_DURATION_MS);
    }

    public static void putDurationMs(long value) {
        Prefers.put("danmaku_duration", Math.clamp(value, MIN_DURATION_MS, MAX_DURATION_MS));
    }

    public static long getFixedDurationMs() {
        return Math.clamp(Prefers.getLong("danmaku_fixed_duration", 5000L), MIN_FIXED_DURATION_MS, MAX_FIXED_DURATION_MS);
    }

    public static void putFixedDurationMs(long value) {
        Prefers.put("danmaku_fixed_duration", Math.clamp(value, MIN_FIXED_DURATION_MS, MAX_FIXED_DURATION_MS));
    }

    public static long getTimeOffsetMs() {
        return Math.clamp(Prefers.getLong("danmaku_time_offset", 0L), MIN_TIME_OFFSET_MS, MAX_TIME_OFFSET_MS);
    }

    public static void putTimeOffsetMs(long value) {
        Prefers.put("danmaku_time_offset", Math.clamp(value, MIN_TIME_OFFSET_MS, MAX_TIME_OFFSET_MS));
    }

    public static int getMaxOnScreen() {
        return Math.clamp(Prefers.getInt("danmaku_max_on_screen", 150), MIN_MAX_ON_SCREEN, MAX_MAX_ON_SCREEN);
    }

    public static void putMaxOnScreen(int value) {
        Prefers.put("danmaku_max_on_screen", Math.clamp(value, MIN_MAX_ON_SCREEN, MAX_MAX_ON_SCREEN));
    }

    public static float getScrollAreaRatio() {
        return Math.clamp(Prefers.getFloat("danmaku_scroll_area_ratio", 0.5f), MIN_SCROLL_AREA_RATIO, MAX_SCROLL_AREA_RATIO);
    }

    public static void putScrollAreaRatio(float value) {
        Prefers.put("danmaku_scroll_area_ratio", Math.clamp(value, MIN_SCROLL_AREA_RATIO, MAX_SCROLL_AREA_RATIO));
    }

    public static int getMaxScrollLines() {
        return Math.clamp(Prefers.getInt("danmaku_max_scroll_lines", 0), MIN_MAX_SCROLL_LINES, MAX_MAX_SCROLL_LINES);
    }

    public static void putMaxScrollLines(int value) {
        Prefers.put("danmaku_max_scroll_lines", Math.clamp(value, MIN_MAX_SCROLL_LINES, MAX_MAX_SCROLL_LINES));
    }

    public static int getMaxTopLines() {
        return Math.clamp(Prefers.getInt("danmaku_max_top_lines", 0), MIN_MAX_FIXED_LINES, MAX_MAX_FIXED_LINES);
    }

    public static void putMaxTopLines(int value) {
        Prefers.put("danmaku_max_top_lines", Math.clamp(value, MIN_MAX_FIXED_LINES, MAX_MAX_FIXED_LINES));
    }

    public static int getMaxBottomLines() {
        return Math.clamp(Prefers.getInt("danmaku_max_bottom_lines", 0), MIN_MAX_FIXED_LINES, MAX_MAX_FIXED_LINES);
    }

    public static void putMaxBottomLines(int value) {
        Prefers.put("danmaku_max_bottom_lines", Math.clamp(value, MIN_MAX_FIXED_LINES, MAX_MAX_FIXED_LINES));
    }

    public static float getLineSpacing() {
        return Math.clamp(Prefers.getFloat("danmaku_line_spacing", 1.4f), MIN_LINE_SPACING, MAX_LINE_SPACING);
    }

    public static void putLineSpacing(float value) {
        Prefers.put("danmaku_line_spacing", Math.clamp(value, MIN_LINE_SPACING, MAX_LINE_SPACING));
    }

    public static float getScrollGapRatio() {
        return Math.clamp(Prefers.getFloat("danmaku_scroll_gap_ratio", 0f), MIN_SCROLL_GAP_RATIO, MAX_SCROLL_GAP_RATIO);
    }

    public static void putScrollGapRatio(float value) {
        Prefers.put("danmaku_scroll_gap_ratio", Math.clamp(value, MIN_SCROLL_GAP_RATIO, MAX_SCROLL_GAP_RATIO));
    }

    public static boolean isShowScroll() {
        return Prefers.getBoolean("danmaku_show_scroll", true);
    }

    public static void putShowScroll(boolean value) {
        Prefers.put("danmaku_show_scroll", value);
    }

    public static boolean isShowTop() {
        return Prefers.getBoolean("danmaku_show_top", true);
    }

    public static void putShowTop(boolean value) {
        Prefers.put("danmaku_show_top", value);
    }

    public static boolean isShowBottom() {
        return Prefers.getBoolean("danmaku_show_bottom", true);
    }

    public static void putShowBottom(boolean value) {
        Prefers.put("danmaku_show_bottom", value);
    }

    public static boolean isShowReverse() {
        return Prefers.getBoolean("danmaku_show_reverse", true);
    }

    public static void putShowReverse(boolean value) {
        Prefers.put("danmaku_show_reverse", value);
    }

    public static boolean isShowPositioned() {
        return Prefers.getBoolean("danmaku_show_positioned", true);
    }

    public static void putShowPositioned(boolean value) {
        Prefers.put("danmaku_show_positioned", value);
    }

    public static boolean isShowSubtitle() {
        return Prefers.getBoolean("danmaku_show_subtitle", true);
    }

    public static void putShowSubtitle(boolean value) {
        Prefers.put("danmaku_show_subtitle", value);
    }

    public static boolean isShowSpecial() {
        return Prefers.getBoolean("danmaku_show_special", true);
    }

    public static void putShowSpecial(boolean value) {
        Prefers.put("danmaku_show_special", value);
    }

    public static String getEffectiveApiUrl() {
        //优先级：用户手动填写 > 点播源配置里的 danmaku > 内置默认接口
        String userUrl = Prefers.getString("danmaku_api_url", "");
        if (LEGACY_API_URL.equals(userUrl)) userUrl = DEFAULT_API_URL;
        if (!TextUtils.isEmpty(userUrl)) return userUrl;
        String configUrl = VodConfig.get().getConfig().getDanmaku();
        if (!TextUtils.isEmpty(configUrl)) return configUrl;
        return DEFAULT_API_URL;
    }

    public static void resetAppearance() {
        DanmakuConfig config = DanmakuConfig.DEFAULT;
        putTextScale(config.textScale);
        putTransparency(config.transparency);
        putTextBold(config.textBold);
        putStyleMode(config.styleMode);
        putShadowTransparency(config.shadowTransparency);
        putStrokeWidthMultiplier(config.strokeWidthMultiplier);
        putProjectionOffsetX(config.projectionOffsetXMultiplier);
        putProjectionOffsetY(config.projectionOffsetYMultiplier);
        putProjectionTransparency(config.projectionTransparency);
        putColorMode(config.colorMode);
    }

    public static void resetTiming() {
        DanmakuConfig config = DanmakuConfig.DEFAULT;
        putDurationMs(config.durationMs);
        putFixedDurationMs(config.fixedDurationMs);
        putTimeOffsetMs(config.timeOffsetMs);
    }

    public static void resetDensity() {
        DanmakuConfig config = DanmakuConfig.DEFAULT;
        putMaxOnScreen(config.maxOnScreen);
        putScrollAreaRatio(config.scrollAreaRatio);
        putScrollGapRatio(config.scrollGapRatio);
        putLineSpacing(config.lineSpacing);
        putMaxScrollLines(config.maxScrollLines);
        putMaxTopLines(config.maxTopLines);
        putMaxBottomLines(config.maxBottomLines);
    }

    public static void resetDisplay() {
        DanmakuConfig config = DanmakuConfig.DEFAULT;
        putShowScroll(config.showScroll);
        putShowTop(config.showTop);
        putShowBottom(config.showBottom);
        putShowReverse(config.showReverse);
        putShowPositioned(config.showPositioned);
        putShowSubtitle(config.showSubtitle);
        putShowSpecial(config.showSpecial);
    }

    public static DanmakuConfig getConfig() {
        return new DanmakuConfig.Builder()
                .setTextScale(getTextScale())
                .setTransparency(getTransparency())
                .setTextBold(isTextBold())
                .setStyleMode(getStyleMode())
                .setShadowTransparency(getShadowTransparency())
                .setStrokeWidthMultiplier(getStrokeWidthMultiplier())
                .setProjectionOffsetXMultiplier(getProjectionOffsetX())
                .setProjectionOffsetYMultiplier(getProjectionOffsetY())
                .setProjectionTransparency(getProjectionTransparency())
                .setColorMode(getColorMode())
                .setDurationMs(getDurationMs())
                .setFixedDurationMs(getFixedDurationMs())
                .setTimeOffsetMs(getTimeOffsetMs())
                .setMaxOnScreen(getMaxOnScreen())
                .setScrollAreaRatio(getScrollAreaRatio())
                .setScrollGapRatio(getScrollGapRatio())
                .setLineSpacing(getLineSpacing())
                .setMaxScrollLines(getMaxScrollLines())
                .setMaxTopLines(getMaxTopLines())
                .setMaxBottomLines(getMaxBottomLines())
                .setShowScroll(isShowScroll())
                .setShowTop(isShowTop())
                .setShowBottom(isShowBottom())
                .setShowReverse(isShowReverse())
                .setShowPositioned(isShowPositioned())
                .setShowSubtitle(isShowSubtitle())
                .setShowSpecial(isShowSpecial())
                .build();
    }
}
