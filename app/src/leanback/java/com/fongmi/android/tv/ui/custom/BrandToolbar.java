package com.fongmi.android.tv.ui.custom;

import android.app.Activity;

import com.fongmi.android.tv.databinding.ViewBrandToolbarBinding;
import com.fongmi.android.tv.ui.activity.KeepActivity;
import com.fongmi.android.tv.ui.activity.SearchActivity;
import com.fongmi.android.tv.ui.activity.SettingActivity;
import com.fongmi.android.tv.utils.Clock;

/**
 * 分类页 / 搜索页 / 搜索结果页 / 播放详情页共用的品牌工具栏（布局见 view_brand_toolbar.xml）。
 * <p>
 * 尺寸、位置与首页标题栏完全一致（24dp 边距 + 53dp 图标 + 25dp 字标 + 右侧三个入口 + 时钟），
 * 唯一差别是这里的组合标不参与焦点、不响应点击 —— 这些页面没有源切换功能。
 */
public final class BrandToolbar {

    private final Clock clock;

    private BrandToolbar(Clock clock) {
        this.clock = clock;
    }

    public static BrandToolbar attach(Activity activity, ViewBrandToolbarBinding bar) {
        if (bar == null) return new BrandToolbar(null);
        //组合标仅作品牌展示：不可点击、不聚焦（无源切换）
        bar.brandLogo.setClickable(false);
        bar.brandLogo.setFocusable(false);
        bar.brandWord.setClickable(false);
        bar.brandWord.setFocusable(false);
        bar.brandSearch.setOnClickListener(v -> SearchActivity.start(activity));
        bar.brandKeep.setOnClickListener(v -> KeepActivity.start(activity));
        bar.brandSetting.setOnClickListener(v -> SettingActivity.start(activity));
        bar.brandClock.setText(null);
        return new BrandToolbar(Clock.create(bar.brandClock));
    }

    /** 跟随宿主 Activity 的可见周期走，避免时钟在后台空转 */
    public void start() {
        if (clock != null) clock.stop().start();
    }

    public void stop() {
        if (clock != null) clock.stop();
    }
}
