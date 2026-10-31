package com.fongmi.android.tv.ui.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.FragmentSettingBinding;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.ConfigListener;
import com.fongmi.android.tv.impl.SiteListener;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.activity.ConfigCenterActivity;
import com.fongmi.android.tv.ui.activity.HomeActivity;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.dialog.ConfigDialog;
import com.fongmi.android.tv.ui.dialog.ManagePageDialog;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.utils.ConfigImport;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class SettingFragment extends BaseFragment implements ConfigListener, SiteListener {

    private FragmentSettingBinding mBinding;
    private String[] size;
    private String[] language;
    private String[] uiScale;

    public static SettingFragment newInstance() {
        return new SettingFragment();
    }

    private HomeActivity getRoot() {
        return (HomeActivity) requireActivity();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        EventBus.getDefault().register(this);
        mBinding.vodUrl.setText(VodConfig.getDesc());
        mBinding.wallUrl.setText(WallConfig.getDesc());
        mBinding.versionText.setText(BuildConfig.VERSION_NAME);
        setOtherText();
        setCacheText();
    }

    private void setOtherText() {
        mBinding.sizeText.setText((size = ResUtil.getStringArray(R.array.select_size))[PlayerSetting.getSize()]);
        mBinding.languageText.setText((language = ResUtil.getStringArray(R.array.select_language))[Setting.getLanguageIndex()]);
        mBinding.uiScaleText.setText((uiScale = ResUtil.getStringArray(R.array.select_ui_scale))[Setting.getUiScaleIndex()]);
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
        mBinding.wall.setOnClickListener(this::onWall);
        mBinding.size.setOnClickListener(this::setSize);
        mBinding.language.setOnClickListener(this::setLanguage);
        mBinding.uiScale.setOnClickListener(this::setUiScale);
        mBinding.cache.setOnClickListener(this::onCache);
        mBinding.managePage.setOnClickListener(view -> ManagePageDialog.show(this));
        mBinding.player.setOnClickListener(this::onPlayer);
        mBinding.danmaku.setOnClickListener(this::onDanmaku);
        mBinding.vod.setOnLongClickListener(this::onVodEdit);
        mBinding.vodHome.setOnClickListener(this::onVodHome);
        mBinding.wall.setOnLongClickListener(this::onWallEdit);
        mBinding.vodHistory.setOnClickListener(this::onVodHistory);
        mBinding.wallDefault.setOnClickListener(this::setWallDefault);
        mBinding.wallRefresh.setOnClickListener(this::setWallRefresh);
        mBinding.wallRefresh.setOnLongClickListener(this::onWallHistory);
    }

    @Override
    public void setConfig(Config config) {
        config.url(ConfigImport.normalize(config.getUrl()));
        if (config.getUrl().startsWith("file")) {
            requireView().post(() -> PermissionUtil.requestFile(this, allGranted -> previewAndLoad(config)));
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
                Notify.progress(requireActivity());
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

    private void onWall(View view) {
        ConfigDialog.create().wall().show(this);
    }

    private boolean onVodEdit(View view) {
        ConfigDialog.create().vod().edit().show(this);
        return true;
    }

    private boolean onWallEdit(View view) {
        ConfigDialog.create().wall().edit().show(this);
        return true;
    }

    private void onVodHome(View view) {
        SiteDialog.create().search().change().show(this);
    }

    private void onVodHistory(View view) {
        ConfigCenterActivity.start(requireActivity(), 0);
    }

    private void onPlayer(View view) {
        getRoot().change(2);
    }

    private void onDanmaku(View view) {
        getRoot().change(3);
    }


    private void setWallDefault(View view) {
        Setting.putWall(Setting.nextDefaultWall());
        Setting.putWallType(0);
        mBinding.wallUrl.setText(Setting.getBuiltInWallName(Setting.getWall()));
        ConfigEvent.wall();
    }

    private void setWallRefresh(View view) {
        Setting.putWall(0);
        WallConfig.get().load(getCallback());
    }

    private boolean onWallHistory(View view) {
        ConfigCenterActivity.start(requireActivity(), 2);
        return true;
    }

    private void setSize(View view) {
        new MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.setting_size).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(size, PlayerSetting.getSize(), (dialog, which) -> {
            mBinding.sizeText.setText(size[which]);
            PlayerSetting.putSize(which);
            RefreshEvent.size();
            dialog.dismiss();
        }).show();
    }

    private void setLanguage(View view) {
        new MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.setting_language).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(language, Setting.getLanguageIndex(), (dialog, which) -> {
            if (which == Setting.getLanguageIndex()) return;
            Setting.putLanguageIndex(which);
            dialog.dismiss();
            RefreshEvent.language();
        }).show();
    }

    private void setUiScale(View view) {
        new MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.setting_ui_scale).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(uiScale, Setting.getUiScaleIndex(), (dialog, which) -> {
            if (which == Setting.getUiScaleIndex()) return;
            Setting.putUiScaleIndex(which);
            dialog.dismiss();
            requireActivity().recreate();
        }).show();
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
        mBinding.wallUrl.setText(WallConfig.getDesc());
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) return;
        setCacheText();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }
}
