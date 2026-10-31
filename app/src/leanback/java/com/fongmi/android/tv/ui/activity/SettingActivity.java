package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.ActivitySettingBinding;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.ConfigListener;
import com.fongmi.android.tv.impl.SiteListener;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.ConfigDialog;
import com.fongmi.android.tv.ui.dialog.ManagePageDialog;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.utils.ConfigImport;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Task;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class SettingActivity extends BaseActivity implements ConfigListener, SiteListener {

    private ActivitySettingBinding mBinding;
    private String[] size;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mBinding.vod.requestFocus();
        mBinding.vodUrl.setText(VodConfig.getDesc());
        mBinding.versionText.setText(BuildConfig.VERSION_NAME);
        setCacheText();
        setOtherText();
    }

    private void setOtherText() {
        mBinding.sizeText.setText((size = ResUtil.getStringArray(R.array.select_size))[PlayerSetting.getSize()]);
        mBinding.managePageText.setText(R.string.manage_page_web);
    }

    private void setCacheText() {
        FileUtil.getCacheSize(new Callback() {
            @Override
            public void success(String result) {
                mBinding.cacheText.setText(result);
            }
        });
    }

    @Override
    protected void initEvent() {
        mBinding.vod.setOnClickListener(this::onVod);
        mBinding.size.setOnClickListener(this::setSize);
        mBinding.cache.setOnClickListener(this::onCache);
        mBinding.managePage.setOnClickListener(view -> ManagePageDialog.show(this));
        mBinding.player.setOnClickListener(this::onPlayer);
        mBinding.danmaku.setOnClickListener(this::onDanmaku);
        mBinding.vod.setOnLongClickListener(this::onVodEdit);
        mBinding.vodHome.setOnClickListener(this::onVodHome);
        mBinding.vodHistory.setOnClickListener(this::onVodHistory);
    }

    @Override
    public void setConfig(Config config) {
        config.url(ConfigImport.normalize(config.getUrl()));
        if (config.getUrl().startsWith("file")) {
            PermissionUtil.requestFile(this, allGranted -> previewAndLoad(config));
        } else {
            previewAndLoad(config);
        }
    }

    private void previewAndLoad(Config config) {
        Task.execute(() -> {
            ConfigImport.Preview preview = ConfigImport.preview(config);
            App.post(() -> handlePreview(config, preview));
        });
    }

    private void handlePreview(Config config, ConfigImport.Preview preview) {
        if (!preview.valid()) {
            Notify.show(getString(R.string.dialog_config_validation_failed, preview.summary()));
            return;
        }
        if (config.getUrl().isEmpty()) {
            Notify.show(R.string.dialog_config_validation_empty);
            load(config);
            return;
        }
        Notify.show(previewMessage(preview));
        load(config);
    }

    private String previewMessage(ConfigImport.Preview preview) {
        String extra = (preview.hasLogo() ? getString(R.string.dialog_config_preview_logo) : "");
        return switch (preview.type()) {
            case 0 -> getString(preview.depot() ? R.string.dialog_config_preview_depot : R.string.dialog_config_preview_vod, preview.primaryCount(), preview.depot() ? "" : extra);
            case 2 -> getString(R.string.dialog_config_preview_wall);
            default -> preview.summary();
        } + (preview.title().isEmpty() ? "" : " · " + preview.title());
    }

    private void load(Config config) {
        switch (config.getType()) {
            case 0:
                VodConfig.load(config, getCallback());
                break;
            case 2:
                Setting.putWall(0);
                WallConfig.load(config, getCallback());
                break;
        }
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void start() {
                Notify.progress(getActivity());
            }

            @Override
            public void success() {
                Notify.dismiss();
                setCacheText();
            }

            @Override
            public void error(String msg) {
                Notify.dismiss();
                Notify.show(msg);
            }
        };
    }

    @Override
    public void setSite(Site item) {
        VodConfig.get().setHome(item);
    }

    private void onVod(View view) {
        ConfigDialog.create().vod().show(this);
    }

    private boolean onVodEdit(View view) {
        ConfigDialog.create().vod().edit().show(this);
        return true;
    }

    private void onVodHome(View view) {
        SiteDialog.create().action().show(this);
    }

    private void onVodHistory(View view) {
        ConfigCenterActivity.start(this, 0);
    }

    private void onPlayer(View view) {
        SettingPlayerActivity.start(this);
    }

    private void onDanmaku(View view) {
        SettingDanmakuActivity.start(this);
    }

    private void setSize(View view) {
        int index = (PlayerSetting.getSize() + 1) % size.length;
        mBinding.sizeText.setText(size[index]);
        PlayerSetting.putSize(index);
        RefreshEvent.size();
    }

    private void onCache(View view) {
        FileUtil.clearCache(new Callback() {
            @Override
            public void success() {
                setCacheText();
            }
        });
    }

            private void initConfig() {
        VodConfig.get().init().load(getCallback());
        WallConfig.get().init().load();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        if (event.type() != ConfigEvent.Type.COMMON) return;
        mBinding.vodUrl.setText(VodConfig.getDesc());
    }

}
