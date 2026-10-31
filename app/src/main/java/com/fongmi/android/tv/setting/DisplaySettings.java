package com.fongmi.android.tv.setting;

public final class DisplaySettings {

    //只保留简体中文：其余语种资源已在构建中移除（resConfigs "zh-rCN"），
    //这里的归一化一律返回简体中文，任何历史偏好值都会被纠正。
    public static final String LANGUAGE_SIMPLIFIED = "zh-CN";

    public static final int UI_SCALE_FOLLOW_SYSTEM = 0;
    public static final int UI_SCALE_STANDARD = 1;
    public static final int UI_SCALE_MILD_COMPACT = 2;
    public static final int UI_SCALE_COMPACT = 3;
    public static final int UI_SCALE_MORE_COMPACT = 4;
    public static final int UI_SCALE_SMALLER = 5;
    private static final int[] UI_SCALES = {UI_SCALE_FOLLOW_SYSTEM, UI_SCALE_STANDARD, UI_SCALE_MILD_COMPACT, UI_SCALE_COMPACT, UI_SCALE_MORE_COMPACT, UI_SCALE_SMALLER};

    private DisplaySettings() {
    }

    public static String normalizeLanguage(String value) {
        return LANGUAGE_SIMPLIFIED;
    }

    public static int languageIndex(String language) {
        return 0;
    }

    public static String languageAt(int index) {
        return LANGUAGE_SIMPLIFIED;
    }

    public static int normalizeUiScale(int scale) {
        for (int option : UI_SCALES) if (option == scale) return scale;
        return UI_SCALE_FOLLOW_SYSTEM;
    }

    public static int uiScaleIndex(int scale) {
        int normalized = normalizeUiScale(scale);
        for (int i = 0; i < UI_SCALES.length; i++) if (UI_SCALES[i] == normalized) return i;
        return 0;
    }

    public static int uiScaleAt(int index) {
        return UI_SCALES[index >= 0 && index < UI_SCALES.length ? index : 0];
    }

    public static float uiScaleFactor(int scale) {
        return switch (normalizeUiScale(scale)) {
            case UI_SCALE_STANDARD -> 0.8f;
            case UI_SCALE_MILD_COMPACT -> 0.75f;
            case UI_SCALE_COMPACT -> 0.7f;
            case UI_SCALE_MORE_COMPACT -> 0.65f;
            case UI_SCALE_SMALLER -> 0.6f;
            default -> 1.0f;
        };
    }
}
