package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import android.graphics.Color;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.leanback.widget.VerticalGridView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.C;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.request.transition.Transition;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.DanmakuApi;
import com.fongmi.android.tv.api.SiteApi;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityVideoBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.CustomTarget;
import com.fongmi.android.tv.model.SearchProgress;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.player.PlayerHelper;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.lut.LutSetting;
import com.fongmi.android.tv.player.lut.LutPreset;
import com.fongmi.android.tv.player.lut.LutStore;
import com.fongmi.android.tv.playback.PlaybackEventCollector;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.setting.DanmakuSetting;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.PreloadSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.setting.SiteBlockSetting;
import com.fongmi.android.tv.setting.SiteHealthStore;
import com.fongmi.android.tv.ui.adapter.ArrayAdapter;
import com.fongmi.android.tv.ui.adapter.EpisodeAdapter;
import com.fongmi.android.tv.ui.adapter.FlagAdapter;
import com.fongmi.android.tv.ui.adapter.ParseAdapter;
import com.fongmi.android.tv.ui.adapter.PartAdapter;
import com.fongmi.android.tv.ui.adapter.QualityAdapter;
import com.fongmi.android.tv.ui.adapter.QuickAdapter;
import com.fongmi.android.tv.ui.custom.AudioStageController;
import com.fongmi.android.tv.ui.custom.PlayerOsdController;
import com.fongmi.android.tv.ui.custom.CustomKeyDownVod;
import com.fongmi.android.tv.ui.custom.CustomMovement;
import com.fongmi.android.tv.ui.custom.CustomSeekView;
import com.fongmi.android.tv.ui.custom.FlagSelectionListener;
import com.fongmi.android.tv.ui.dialog.ContentDialog;
import com.fongmi.android.tv.ui.dialog.ControlDialog;
import com.fongmi.android.tv.ui.dialog.DanmakuDialog;
import com.fongmi.android.tv.ui.dialog.BaseAlertDialog;
import com.fongmi.android.tv.ui.dialog.BaseBottomSheetDialog;
import com.fongmi.android.tv.ui.dialog.BaseSideSheetDialog;
import com.fongmi.android.tv.ui.dialog.PlayerEngineDialog;
import com.fongmi.android.tv.ui.dialog.QuickSearchDialog;
import com.fongmi.android.tv.ui.dialog.SpeedSettingDialog;
import com.fongmi.android.tv.ui.dialog.ChapterDialog;
import com.fongmi.android.tv.ui.dialog.SubtitleSettingDialog;
import com.fongmi.android.tv.ui.dialog.TitleDialog;
import com.fongmi.android.tv.ui.custom.BrandToolbar;
import com.fongmi.android.tv.ui.dialog.TrackDialog;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Sniffer;
import com.fongmi.android.tv.utils.Task;
import com.fongmi.android.tv.utils.Tip;
import com.fongmi.android.tv.utils.Traffic;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.crawler.SpiderDebug;
import com.github.bassaer.library.MDColor;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class VideoActivity extends PlaybackActivity implements CustomKeyDownVod.Listener, TrackDialog.Listener, ControlDialog.Listener, ArrayAdapter.OnClickListener, FlagAdapter.OnClickListener, EpisodeAdapter.OnClickListener, QualityAdapter.OnClickListener, QuickAdapter.OnClickListener, ParseAdapter.OnClickListener, Clock.Callback {

    private ActivityVideoBinding mBinding;
    private PlayerOsdController mOsd;
    private ViewGroup.LayoutParams mFrameParams;
    private Observer<Result> mObserveDetail;
    private Observer<Result> mObservePlayer;
    private Observer<Result> mObserveSearch;
    private EpisodeAdapter mEpisodeAdapter;
    private QualityAdapter mQualityAdapter;
    private ArrayAdapter mArrayAdapter;
    private ParseAdapter mParseAdapter;
    private QuickAdapter mQuickAdapter;
    private FlagAdapter mFlagAdapter;
    private PartAdapter mPartAdapter;
    private CustomKeyDownVod mKeyDown;
    private SiteViewModel mViewModel;
    private List<String> mBroken;
    private History mHistory;
    private String preloadContext;
    private boolean fullscreen;
    private boolean initAuto;
    private boolean autoMode;
    private boolean useParse;
    private boolean detailRequested;
    private boolean detailHealthRecorded;
    private boolean playHealthRecorded;
    private Runnable mR1;
    private Runnable mR2;
    private Runnable mR3;
    private Runnable mR4;
    private Clock mClock;
    private BrandToolbar mBrand;
    private View mFocus1;
    private boolean blockCenterKeyUp;
    //确认键按下时焦点是否落在控制条内：松手时控制条若已被收走，这次松手不能落到播放器头上
    private boolean enterDownOnControlBar;
    //弹窗（弹幕设置 / 字幕 / 音轨）打开期间控制条保持常驻，不再自动隐藏
    private boolean mPanelHolding;
    //最近一次检测到弹窗的时间，用来判断弹窗是否真的全部关闭
    private long mPanelLastSeen;
    private Boolean mPlayPauseShown;
    //用户刚下发的播放/暂停意图，等 onIsPlayingChanged 回执确认后清空
    private Boolean mPlayPausePending;
    private int mPlayPauseRetry;
    private Result mPendingDetail;
    private Result mPendingPlayer;
    private String playHealthKey;
    private long detailStartTime;
    private long playerStartTime;
    private boolean pendingLutImport;
    private AudioStageController mAudio;
    private QuickSearchDialog mQuickSearchDialog;
    private boolean quickSearchDialogClosed;
    //命令下发后等真实状态回执的兜底检查间隔/次数（仅缓冲、命令未落地时才会重试）
    private static final long PLAY_PAUSE_CONFIRM_DELAY = 400;
    private static final int PLAY_PAUSE_CONFIRM_RETRY = 5;
    private final Runnable mPlayPauseConfirm = this::reconcilePlayPause;

    private final ActivityResultLauncher<Intent> mLutDir = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) return;
        LutStore.setUserDir(result.getData().getData(), result.getData().getFlags());
        Notify.show(R.string.lut_directory_selected);
        mBinding.lutQuick.refreshList();
        if (pendingLutImport) {
            pendingLutImport = false;
            chooseLutFile();
        }
    });

    private final ActivityResultLauncher<Intent> mLutFile = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) return;
        String path = FileChooser.getPathFromUri(result.getData().getData());
        if (TextUtils.isEmpty(path)) {
            Notify.show(R.string.lut_import_failed);
            return;
        }
        Task.submit(() -> {
            try {
                LutPreset preset = LutStore.importFile(path);
                App.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Notify.show(R.string.lut_imported);
                    mBinding.lutQuick.selectImported(preset, player(), mBinding.exo, this::onLutChanged);
                });
            } catch (Exception e) {
                if (SpiderDebug.isEnabled()) SpiderDebug.log("lut", "import failed path=%s error=%s", path, e.getMessage());
                App.post(() -> Notify.show(Notify.getError(R.string.lut_import_failed, e)));
            }
        });
    });

    private final ActivityResultLauncher<Intent> mKaraokeTrackFile = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) return;
        String path = FileChooser.getPathFromUri(result.getData().getData());
        if (mAudio != null) mAudio.onKaraokeTrackFilePicked(path);
    });

    public static void push(FragmentActivity activity, String text) {
        if (FileChooser.isValid(activity, Uri.parse(text))) file(activity, FileChooser.getPathFromUri(Uri.parse(text)));
        else start(activity, Sniffer.getUrl(text));
    }

    public static void file(FragmentActivity activity, String path) {
        if (TextUtils.isEmpty(path) || !isPlayableFilePath(path)) return;
        String name = new File(path).getName();
        start(activity, SiteApi.PUSH, "file://" + path, name);
    }

    private static boolean isPlayableFilePath(String path) {
        String value = path.replace('\\', '/');
        if (value.contains("/../") || value.endsWith("/..") || value.contains("/./")) return false;
        if (value.startsWith("/data/data/") || value.startsWith("/data/user/") || value.startsWith("/proc/") || value.startsWith("/system/")) return false;
        String lower = value.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".avi") || lower.endsWith(".ts") || lower.endsWith(".m3u8") || lower.endsWith(".flv") || lower.endsWith(".mp3") || lower.endsWith(".flac") || lower.endsWith(".m4a") || lower.endsWith(".wav") || lower.endsWith(".webm") || lower.endsWith(".mov") || lower.endsWith(".m2ts") || lower.endsWith(".rmvb") || lower.endsWith(".wmv") || lower.endsWith(".mpg") || lower.endsWith(".mpeg");
    }

    public static void cast(Activity activity, History history) {
        start(activity, history.getSiteKey(), history.getVodId(), history.getVodName(), history.getVodPic(), null, false, true);
    }

    public static void collect(Activity activity, String key, String id, String name, String pic) {
        start(activity, key, id, name, pic, null, true, false);
    }

    public static void start(Activity activity, String url) {
        start(activity, SiteApi.PUSH, url, url);
    }

    public static void start(Activity activity, String key, String id, String name) {
        start(activity, key, id, name, null);
    }

    public static void start(Activity activity, String key, String id, String name, String pic) {
        start(activity, key, id, name, pic, null);
    }

    public static void start(Activity activity, String key, String id, String name, String pic, String mark) {
        start(activity, key, id, name, pic, mark, false, false);
    }

    public static void start(Activity activity, String key, String id, String name, String pic, String mark, boolean collect, boolean cast) {
        long launch = System.currentTimeMillis();
        SpiderDebug.log("video-flow", "launch request key=%s id=%s name=%s collect=%s cast=%s", key, id, name, collect, cast);
        Intent intent = new Intent(activity, VideoActivity.class);
        intent.putExtra("launchTime", launch);
        intent.putExtra("collect", collect);
        intent.putExtra("cast", cast);
        intent.putExtra("mark", mark);
        intent.putExtra("name", name);
        intent.putExtra("pic", ImgUtil.cache(pic));
        intent.putExtra("key", key);
        intent.putExtra("id", id);
        activity.startActivity(intent);
        SpiderDebug.log("video-flow", "launch dispatched cost=%dms key=%s id=%s", System.currentTimeMillis() - launch, key, id);
    }

    private boolean isCast() {
        return getIntent().getBooleanExtra("cast", false);
    }

    private String getName() {
        return Objects.toString(getIntent().getStringExtra("name"), "");
    }

    private String getPic() {
        return Objects.toString(getIntent().getStringExtra("pic"), "");
    }

    private String getMark() {
        return Objects.toString(getIntent().getStringExtra("mark"), "");
    }

    private String getKey() {
        return Objects.toString(getIntent().getStringExtra("key"), "");
    }

    private String getId() {
        return Objects.toString(getIntent().getStringExtra("id"), "");
    }

    private String getHistoryKey() {
        return getKey().concat(AppDatabase.SYMBOL).concat(getId()).concat(AppDatabase.SYMBOL) + VodConfig.getCid();
    }

    private Site getSite() {
        return VodConfig.get().getSite(getKey());
    }

    private Flag getFlag() {
        return mFlagAdapter.getActivated();
    }

    private Episode getEpisode() {
        return mEpisodeAdapter.getActivated();
    }

    private int getScale() {
        return mHistory != null && mHistory.getScale() != -1 ? mHistory.getScale() : PlayerSetting.getScale();
    }

    private boolean isReplay() {
        return Setting.getReset() == 1;
    }

    private boolean isFromCollect() {
        return getIntent().getBooleanExtra("collect", false);
    }

    private long getLaunchTime() {
        return getIntent().getLongExtra("launchTime", 0);
    }

    private long getLaunchCost(long now) {
        long launchTime = getLaunchTime();
        return launchTime <= 0 ? 0 : now - launchTime;
    }

    @Override
    protected ViewBinding getBinding() {
        long start = System.currentTimeMillis();
        mBinding = ActivityVideoBinding.inflate(getLayoutInflater());
        SpiderDebug.log("video-flow", "inflate cost=%dms sinceLaunch=%dms", System.currentTimeMillis() - start, getLaunchCost(start));
        return mBinding;
    }

    @Override
    protected PlaybackService.NavigationCallback getNavigationCallback() {
        return mNavigationCallback;
    }

    @Override
    protected PlayerView getExoView() {
        return mBinding.exo;
    }

    @Override
    protected CustomSeekView getSeekView() {
        return mBinding.control.seek;
    }

    @Override
    protected void onServiceConnected() {
        SpiderDebug.log("video-flow", "service ready sinceLaunch=%dms key=%s id=%s", getLaunchCost(System.currentTimeMillis()), getKey(), getId());
        player().setDanmakuController(mBinding.exo.getDanmakuController());
        player().applyDanmakuState();
        mBinding.control.action.danmaku.setSelected(DanmakuSetting.isEnabled());
        if (!detailRequested) checkId();
        if (mPendingDetail != null) {
            Result result = mPendingDetail;
            mPendingDetail = null;
            setDetail(result);
        }
        if (mPendingPlayer != null) {
            Result result = mPendingPlayer;
            mPendingPlayer = null;
            setPlayer(result);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String oldId = getId();
        super.onNewIntent(intent);
        String id = Objects.toString(intent.getStringExtra("id"), "");
        if (TextUtils.isEmpty(id) || id.equals(oldId)) return;
        getIntent().putExtras(intent);
        saveHistory();
        checkId();
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        long start = System.currentTimeMillis();
        SpiderDebug.log("video-flow", "initView start sinceLaunch=%dms key=%s id=%s", getLaunchCost(start), getKey(), getId());
        if (!isCast() && hasInitialPreview()) showInitialPreview();
        super.initView(savedInstanceState);
        SpiderDebug.log("video-flow", "initView after playback cost=%dms", System.currentTimeMillis() - start);
        mFrameParams = mBinding.video.getLayoutParams();
        mBrand = BrandToolbar.attach(this, mBinding.brand);
        mClock = Clock.create(mBinding.widget.clock);
        mKeyDown = CustomKeyDownVod.create(this);
        mObserveDetail = this::setDetail;
        mObservePlayer = this::setPlayer;
        mObserveSearch = this::setSearch;
        mBroken = new ArrayList<>();
        mR1 = () -> {
            //双保险：暂停时不允许自动隐藏控制条
            if (player().isPlaying()) hideControl();
        };
        mR2 = this::updateFocus;
        mR3 = this::setTraffic;
        mR4 = this::showEmpty;
        SpiderDebug.log("video-flow", "initView state ready cost=%dms", System.currentTimeMillis() - start);
        checkCast();
        SpiderDebug.log("video-flow", "initView preview ready cost=%dms", System.currentTimeMillis() - start);
        setRecyclerView();
        // See the mobile counterpart: a null include binding must not crash playback.
        if (mBinding.osd != null) {
            mOsd = new PlayerOsdController(mBinding.osd.getRoot(), mBinding.osd.osdTopLeft, mBinding.osd.osdTopRight, mBinding.osd.osdBottomLeft, mBinding.osd.osdBottomRight, mBinding.osd.osdMiniProgress, new PlayerOsdController.Source() {
                @Override public PlayerManager getPlayer() { return service() == null ? null : player(); }
                @Override public String getTitle() { return mBinding.name.getText().toString(); }
            });
        }
        SpiderDebug.log("video-flow", "initView recycler ready cost=%dms", System.currentTimeMillis() - start);
        setVideoView();
        SpiderDebug.log("video-flow", "initView video view ready cost=%dms", System.currentTimeMillis() - start);
        setViewModel();
        checkId();
        SpiderDebug.log("video-flow", "initView end cost=%dms sinceLaunch=%dms", System.currentTimeMillis() - start, getLaunchCost(System.currentTimeMillis()));
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void initEvent() {
        mBinding.keep.setOnClickListener(view -> onKeep());
        mBinding.video.setOnClickListener(view -> onVideo());
        mBinding.change1.setOnClickListener(view -> onChange());
        mBinding.content.setOnClickListener(view -> onContent());
        //控制条固定按钮：上集、下集、弹幕、字幕、音轨、倍速、片头、片尾、播放设置
        //其余按钮在布局中保持 gone，仅用于底部控制面板（ControlDialog）的转发与状态同步
        mBinding.control.action.text.setOnClickListener(this::onTrack);
        mBinding.control.action.audio.setOnClickListener(this::onTrack);
        mBinding.control.action.video.setOnClickListener(this::onTrack);
        mBinding.control.action.speed.setUpListener(this::onSpeedAdd);
        mBinding.control.action.speed.setDownListener(this::onSpeedSub);
        //倍速独立图标按钮：与隐藏的 speed 视图共用同一套逻辑
        mBinding.control.action.speedBtn.setUpListener(this::onSpeedAdd);
        mBinding.control.action.speedBtn.setDownListener(this::onSpeedSub);
        mBinding.control.action.speedBtn.setOnClickListener(view -> onSpeedPanel());
        mBinding.control.action.speedBtn.setOnLongClickListener(view -> {
            suppressCenterKey();
            return onSpeedLong();
        });
        mBinding.control.action.ending.setUpListener(this::onEndingAdd);
        mBinding.control.action.ending.setDownListener(this::onEndingSub);
        mBinding.control.action.opening.setUpListener(this::onOpeningAdd);
        mBinding.control.action.opening.setDownListener(this::onOpeningSub);
        mBinding.control.action.text.setUpListener(this::onSubtitleClick);
        mBinding.control.action.text.setDownListener(this::onSubtitleClick);
        //方向键在视图层就被消费掉，Activity 收不到，这里补回调维持控制条状态
        mBinding.control.action.text.setConsumeListener(this::onControlKeyConsumed);
        mBinding.control.action.speedBtn.setConsumeListener(this::onControlKeyConsumed);
        mBinding.control.action.opening.setConsumeListener(this::onControlKeyConsumed);
        mBinding.control.action.ending.setConsumeListener(this::onControlKeyConsumed);
        mBinding.control.action.speed.setConsumeListener(this::onControlKeyConsumed);
        //播放/暂停：同一个按钮按播放状态轮换图标（换图在 onPaused/onPlay 内部即时完成）
        mBinding.control.action.playPause.setOnClickListener(view -> togglePlayPause());
        mBinding.control.action.next.setOnClickListener(view -> checkNext());
        mBinding.control.action.prev.setOnClickListener(view -> checkPrev());
        mBinding.control.action.panelBtn.setOnClickListener(view -> onControlPanel());
        mBinding.control.action.scale.setOnClickListener(view -> onScale());
        mBinding.control.action.lut.setOnClickListener(view -> onLut());
        mBinding.control.action.share.setOnClickListener(view -> onShare());
        mBinding.control.action.panel.setOnClickListener(view -> onControlPanel());
        mBinding.control.action.speed.setOnClickListener(view -> onSpeedPanel());
        mBinding.control.action.reset.setOnClickListener(view -> onReset());
        mBinding.control.action.title.setOnClickListener(view -> onTitle());
        mBinding.control.action.chapter.setOnClickListener(view -> onChapter());
        mBinding.control.action.player.setOnClickListener(view -> onPlayerPanel());
        mBinding.control.action.player.setOnLongClickListener(view -> {
            suppressCenterKey();
            return onPlayerKernel();
        });
        mBinding.control.action.decode.setOnClickListener(view -> onDecode());
        mBinding.control.action.ending.setOnClickListener(view -> onEnding());
        mBinding.control.action.repeat.setOnClickListener(view -> onRepeat());
        mBinding.control.action.replay.setOnClickListener(view -> onReplay());
        mBinding.control.action.change2.setOnClickListener(view -> onChange());
        mBinding.control.action.fullscreen.setOnClickListener(view -> onFullscreen());
        //弹幕：单击开/关，长按打开弹幕设置面板
        mBinding.control.action.danmaku.setOnClickListener(view -> onDanmakuToggle());
        mBinding.control.action.danmaku.setOnLongClickListener(view -> {
            //长按松手时系统还会补一次 click；面板弹出后控制条已隐藏，
            //Enter 的 ACTION_UP 会落回 Activity 被当成“播放/暂停”，这里先立起抑制窗口再弹面板。
            suppressCenterKey();
            onDanmaku();
            return true;
        });
        mBinding.control.action.opening.setOnClickListener(view -> onOpening());
        mBinding.control.action.speed.setOnLongClickListener(view -> {
            suppressCenterKey();
            return onSpeedLong();
        });
        mBinding.control.action.reset.setOnLongClickListener(view -> {
            suppressCenterKey();
            return onResetToggle();
        });
        mBinding.video.setOnTouchListener((view, event) -> mKeyDown.onTouchEvent(event));
        mBinding.flag.addOnChildViewHolderSelectedListener(new FlagSelectionListener(mBinding.flag, mFlagAdapter, this));
        mBinding.episode.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (child != null && mBinding.video != mFocus1) mFocus1 = child.itemView;
            }
        });
        mBinding.episode.setOnKeyListener((view, keyCode, event) -> onEpisodeKey(event));
        mBinding.array.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (mEpisodeAdapter.getItemCount() > 40 && position > 1) scrollToEpisode(mArrayAdapter.getStart(position));
            }
        });
        bindControlFocus();
        bindControlLongClick();
    }

    /**
     * 控制条按钮统一消费长按。
     * 系统只在控件“可长按”时才做长按判定；没设长按监听器的控件会一直按普通点击处理，
     * 于是「长按播放/暂停」在松手时又被补了一次 click，把视频切了状态。
     * 这里给所有可聚焦控件补一个空转的长按监听：长按被吃掉、松手不再触发 click；
     * 已经有长按动作的按钮（弹幕、倍速等）保持原样，不覆盖。
     */
    private void bindControlLongClick() {
        bindLongClick(mBinding.control.action.getRoot());
        bindLongClick(mBinding.control.seek);
    }

    private void bindLongClick(View view) {
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) bindLongClick(group.getChildAt(i));
        }
        if (!view.isFocusable() || view.isLongClickable()) return;
        view.setOnLongClickListener(v -> {
            //松手的 ACTION_UP 一并吞掉，避免落到播放器头上被当成播放/暂停
            suppressCenterKey();
            return true;
        });
    }

    /**
     * 控制条的焦点链：进度条在上、按钮行在下（见 view_control_vod.xml）。
     *
     * <p><b>按上</b>：按钮行任意控件 → 进度条；进度条自己 → 停住（它已经在最上沿）。
     * <p><b>按下</b>：只有进度条会向下走进按钮行，落点是「播放/暂停」；
     * 按钮行内部按下**不移动焦点**（各控件的 nextFocusDown 指向自己）。
     *
     * <p>为什么必须显式绑：早期只给 prev/next/danmaku/audio/panelBtn 补过绑定，
     * 而且下方向写成了「指向自己」，playPause / 字幕 / 倍速 / 片头 / 片尾 完全没绑。
     * 框架找不到显式目标就退化成几何搜索，而控制条贴在屏幕底部、下方没有可聚焦控件，
     * 焦点直接被甩出控制条（实测按「下」后按钮行上再无高亮块，遥控器像是失灵）。
     *
     * <p>控制条外的焦点约束不在这个方法里，见 {@link #onVerticalKey(KeyEvent)}。
     */
    private void bindControlFocus() {
        View timeBar = mBinding.control.seek.getFocusView();
        if (timeBar == null) return;
        int timeBarId = timeBar.getId();
        //进度条在控制条最上沿：上/左/右都停在自己身上，只有「下」落到「播放/暂停」
        timeBar.setNextFocusUpId(timeBarId);
        timeBar.setNextFocusDownId(R.id.playPause);
        timeBar.setNextFocusLeftId(timeBarId);
        timeBar.setNextFocusRightId(timeBarId);
        //按钮行：向上回进度条；向下指向自己 = 按下键不做任何焦点移动
        View[] slots = {
                mBinding.control.action.playPause,
                mBinding.control.action.prev,
                mBinding.control.action.next,
                mBinding.control.action.danmaku,
                mBinding.control.action.text,
                mBinding.control.action.audio,
                mBinding.control.action.speedBtn,
                mBinding.control.action.panelBtn,
                mBinding.control.action.opening,
                mBinding.control.action.ending,
        };
        for (View view : slots) {
            if (view == null) continue;
            view.setNextFocusUpId(timeBarId);
            view.setNextFocusDownId(view.getId());
        }
        //水平方向两端封口：边界控件指向自己，按到头就停在原地。
        //早先写的是 setNextFocusLeftId(View.NO_ID)，但 NO_ID 本身就是「没指定」的默认值，
        //等于什么都没做，实际仍由几何搜索兜底 —— 语义含糊，画面边缘一旦没有候选就会丢焦点。
        mBinding.control.action.playPause.setNextFocusLeftId(R.id.playPause);
        mBinding.control.action.prev.setNextFocusLeftId(R.id.playPause);
        mBinding.control.action.ending.setNextFocusRightId(R.id.ending);
    }

    /**
     * CustomUpDownView（字幕 / 倍速 / 片头 / 片尾）会把方向键消费在视图层，
     * Activity 的 dispatchKeyEvent 收不到，这里补上「重置自动隐藏计时」。
     */
    private void onControlKeyConsumed() {
        if (isVisible(mBinding.control.getRoot())) setR1Callback();
    }

    private void setRecyclerView() {
        mBinding.flag.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.flag.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.flag.setAdapter(mFlagAdapter = new FlagAdapter(this));
        int episodeColumn = getEpisodeColumn();
        mBinding.episode.setNumColumns(episodeColumn);
        mBinding.episode.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.episode.setVerticalSpacing(ResUtil.dp2px(8));
        mBinding.episode.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_LOW_EDGE);
        mBinding.episode.setWindowAlignmentPreferKeyLineOverLowEdge(false);
        mBinding.episode.setWindowAlignmentPreferKeyLineOverHighEdge(false);
        mBinding.episode.setWindowAlignmentOffset(0);
        mBinding.episode.setWindowAlignmentOffsetPercent(0);
        mBinding.episode.setItemAlignmentOffset(0);
        mBinding.episode.setItemAlignmentOffsetPercent(0);
        mBinding.episode.setAdapter(mEpisodeAdapter = new EpisodeAdapter(this));
        mEpisodeAdapter.setColumn(episodeColumn);
        mBinding.quality.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.quality.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.quality.setAdapter(mQualityAdapter = new QualityAdapter(this));
        mBinding.array.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.array.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.array.setAdapter(mArrayAdapter = new ArrayAdapter(this));
        mBinding.part.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.part.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.part.setAdapter(mPartAdapter = new PartAdapter(item -> initSearch(item, false)));
        mBinding.quick.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.quick.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.quick.setAdapter(mQuickAdapter = new QuickAdapter(this));
        mBinding.control.parse.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.control.parse.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.control.parse.setAdapter(mParseAdapter = new ParseAdapter(this));
        mParseAdapter.addAll(VodConfig.get().getParses());
    }

    private void setVideoView() {
        mBinding.control.action.danmaku.setVisibility(DanmakuSetting.isLoad() ? View.VISIBLE : View.GONE);
        mBinding.control.action.reset.setText(ResUtil.getStringArray(R.array.select_reset)[Setting.getReset()]);
        setLut();
        setAudioStage();
    }

    private void setAudioStage() {
        if (mAudio != null || mBinding.audioStage == null || mBinding.lyrics == null) return;
        mAudio = new AudioStageController(new AudioStageController.Host() {
            @Override public FragmentActivity activity() { return VideoActivity.this; }
            @Override public PlayerManager player() { return service() == null ? null : VideoActivity.this.player(); }
            @Override public PlaybackService service() { return VideoActivity.this.service(); }
            @Override public History history() { return mHistory; }
            @Override public Site getSite() { return VideoActivity.this.getSite(); }
            @Override public String getSiteKey() { return getKey(); }
            @Override public Flag getFlag() { return safeFlag(); }
            @Override public Episode getEpisode() { return safeEpisode(); }
            @Override public String getVodName() { return mBinding.name.getText().toString(); }
            @Override public String getVodPic() { return mHistory == null ? "" : mHistory.getVodPic(); }
            @Override public List<Episode> getQueueEpisodes() { Flag flag = safeFlag(); return flag == null ? new ArrayList<>() : flag.getEpisodes(); }
            @Override public void setQueueEpisodes(List<Episode> items) { setEpisodeAdapter(items); }
            @Override public void playEpisode(Episode episode) { if (episode != null) VideoActivity.this.onItemClick(episode); }
            @Override public void playNext() { checkNext(); }
            @Override public void playPrev() { checkPrev(); }
            @Override public void launchKaraokeTrackFileChooser() { FileChooser.from(mKaraokeTrackFile).show("*/*", new String[]{"text/plain", "audio/midi", "audio/x-midi", "application/octet-stream", "*/*"}); }
            @Override public void onStageVisibilityChanged(boolean visible) { if (!visible) mBinding.video.requestFocus(); }
        }, mBinding.audioStage, mBinding.lyrics);
        mBinding.control.action.immersiveAudio.setOnClickListener(view -> { if (mAudio != null) mAudio.toggleImmersiveAudioMode(); });
    }

    private Flag safeFlag() {
        try {
            return mFlagAdapter == null || mFlagAdapter.getItemCount() == 0 ? null : getFlag();
        } catch (Exception e) {
            return null;
        }
    }

    private Episode safeEpisode() {
        try {
            return mEpisodeAdapter == null || mEpisodeAdapter.getItemCount() == 0 ? null : getEpisode();
        } catch (Exception e) {
            return null;
        }
    }

    private void setLut() {
        mBinding.control.action.lut.setText(LutSetting.getButtonText());
    }

    private void onLutChanged() {
        setLut();
    }

    private void onShare() {
        PlayerHelper.share(this, player().getUrl(), player().getHeaders(), mBinding.name.getText());
        setRedirect(true);
    }

    private void onControlPanel() {
        //播放设置面板也是「从控制条按钮弹出」，期间控制条常驻
        showPanel(() -> ControlDialog.create().parent(mBinding).history(mHistory).parse(isUseParse()).player(player()).show(this));
    }

    private void onSpeedPanel() {
        showPanel(() -> SpeedSettingDialog.show(this, player()));
    }

    private void onPlayerPanel() {
        showPanel(() -> PlayerEngineDialog.show(this, player()));
    }

    @Override
    public void onScale(int tag) {
        setScale(tag);
    }

    @Override
    public void onParse(Parse item) {
        onItemClick(item);
    }

    @Override
    public void onLutPanel() {
        onLut();
    }

    @Override
    public void onTrackPanel(int type) {
        //播放设置面板转发过来的轨道入口，同样按“弹窗期间常驻”处理
        showPanel(() -> TrackDialog.create().type(type).player(player()).show(this));
    }

    @Override
    public void onTitlePanel() {
        onTitle();
    }

    @Override
    public void onDanmakuPanel() {
        onDanmaku();
    }

    private void onLut() {
        mBinding.lutQuick.toggle(player(), mBinding.exo, this::onLutChanged, new com.fongmi.android.tv.ui.custom.LutQuickPanel.ImportCallback() {
            @Override
            public void onImportLut() {
                onLutImport();
            }

            @Override
            public void onSelectLutDir() {
                onLutDir();
            }
        });
        focusLutQuickIfVisible();
    }

    private void focusLutQuickIfVisible() {
        mBinding.lutQuick.post(() -> {
            if (mBinding.lutQuick.getVisibility() == View.VISIBLE) mBinding.lutQuick.focusSelectedEntry();
        });
    }

    private void onLutImport() {
        if (!LutStore.hasUserDir()) {
            pendingLutImport = true;
            chooseLutDir();
            return;
        }
        chooseLutFile();
    }

    private void onLutDir() {
        pendingLutImport = false;
        chooseLutDir();
    }

    private void chooseLutFile() {
        FileChooser.from(mLutFile).show("*/*", new String[]{"application/octet-stream", "text/*", "image/*", "*/*"});
    }

    private void chooseLutDir() {
        mLutDir.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
    }

    private int getEpisodeColumn() {
        return mEpisodeAdapter == null ? 8 : mEpisodeAdapter.getColumn();
    }

    private void setDecode() {
        mBinding.control.action.decode.setText(player().getDecodeText());
        mBinding.control.action.player.setText(player().getPlayerText());
    }

    private void setScale(int scale) {
        mHistory.setScale(scale);
        mBinding.exo.setResizeMode(scale);
        mBinding.control.action.scale.setText(ResUtil.getStringArray(R.array.select_scale)[scale]);
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.getResult().observe(this, mObserveDetail);
        mViewModel.getPlayer().observe(this, mObservePlayer);
        mViewModel.getPreload().observe(this, this::setPreload);
        mViewModel.getSearch().observe(this, mObserveSearch);
        mViewModel.getSearchProgress().observe(this, this::setSearchProgress);
    }

    private void checkCast() {
        if (isCast() && !isFullscreen()) enterFullscreen();
        else if (hasInitialPreview()) showInitialPreview();
        else mBinding.progressLayout.showProgress();
    }

    private void checkId() {
        if (detailRequested) return;
        detailRequested = true;
        if (getId().startsWith("push://")) getIntent().putExtra("key", SiteApi.PUSH).putExtra("id", getId().substring(7));
        if (getId().isEmpty() || getId().startsWith("msearch:")) setEmpty(false);
        else getDetail();
    }

    private void getDetail() {
        detailStartTime = System.currentTimeMillis();
        detailHealthRecorded = false;
        SpiderDebug.log("video-flow", "detail start key=%s id=%s name=%s", getKey(), getId(), getName());
        mViewModel.detailContent(getKey(), getId());
    }

    private void getDetail(Vod item) {
        saveHistory();
        getIntent().putExtra("key", item.getSiteKey());
        getIntent().putExtra("pic", item.getPic());
        getIntent().putExtra("id", item.getId());
        mBinding.scroll.scrollTo(0, 0);
        mClock.setCallback(null);
        updateNavigationKey();
        player().reset();
        player().stop();
        getDetail();
    }

    private void setDetail(Result result) {
        long cost = System.currentTimeMillis() - detailStartTime;
        SpiderDebug.log("video-flow", "detail finish cost=%dms empty=%s msg=%s", cost, result.getList().isEmpty(), result.getMsg());
        recordDetailHealth(result, cost);
        if (service() == null) {
            mPendingDetail = result;
            SpiderDebug.log("video-flow", "detail pending service key=%s id=%s", getKey(), getId());
            return;
        }
        if (result.getList().isEmpty()) setEmpty(result.hasMsg());
        else setDetail(result.getVod());
        Notify.show(result.getMsg());
    }

    private void setEmpty(boolean finish) {
        if (isFromCollect() || finish) {
            finish();
        } else if (getName().isEmpty()) {
            showEmpty();
        } else {
            mBinding.name.setText(getName());
            App.post(mR4, 10000);
            checkSearch(false);
        }
    }

    private void showEmpty() {
        mBinding.progressLayout.showEmpty();
    }

    private void setDetail(Vod item) {
        item.checkPic(getPic());
        item.checkName(getName());
        mBinding.progressLayout.showContent();
        mBinding.name.setText(item.getName());
        mFlagAdapter.addAll(item.getFlags());
        mBinding.video.requestFocus();
        App.removeCallbacks(mR4);
        checkHistory(item);
        checkFlag(item);
        checkKeepImg();
        setText(item);
        updateKeep();
    }

    private void setText(Vod item) {
        mBinding.content.setTag(item.getContent());
        setText(mBinding.year, R.string.detail_year, item.getYear());
        setText(mBinding.area, R.string.detail_area, item.getArea());
        setText(mBinding.type, R.string.detail_type, item.getTypeName());
        setText(mBinding.site, R.string.detail_site, getSite().getName());
        setText(mBinding.director, R.string.detail_director, item.getDirector());
        setText(mBinding.actor, R.string.detail_actor, item.getActor());
        setText(mBinding.remark, 0, item.getRemarks());
    }

    private void setText(TextView view, int resId, String text) {
        if (TextUtils.isEmpty(text) && !TextUtils.isEmpty(view.getText())) return;
        view.setText(Sniffer.buildClickable(resId > 0 ? getString(resId, text) : text, this::clickableSpan), TextView.BufferType.SPANNABLE);
        view.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
        view.setLinkTextColor(MDColor.YELLOW_500);
        CustomMovement.bind(view);
    }

    private ClickableSpan clickableSpan(Result result) {
        return new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                VodActivity.start(getActivity(), getKey(), result);
                setRedirect(true);
            }
        };
    }

    private void getPlayer(Flag flag, Episode episode) {
        invalidatePreload();
        mBinding.widget.title.setText(getString(R.string.detail_title, mBinding.name.getText(), episode.getName()));
        playerStartTime = System.currentTimeMillis();
        beginPlayHealth();
        try {
            SpiderDebug.log("video-flow", "player start key=%s flag=%s episode=%s url=%s", getKey(), flag.getFlag(), episode.getName(), episode.getUrl());
            mViewModel.playerContent(getKey(), flag.getFlag(), episode.getUrl());
        } catch (Throwable t) {
            SpiderDebug.log(t);
            String msg = t.getMessage();
            onError(msg != null ? msg : t.getClass().getSimpleName());
            return;
        }
        mBinding.widget.title.setSelected(true);
        updateHistory(episode);
        PlaybackEventCollector.get().setPlayer(player());
        PlaybackEventCollector.get().updateHistory(mHistory);
        showProgress();
    }

    private void setPlayer(Result result) {
        if (isFinishing() || isDestroyed()) return;
        SpiderDebug.log("video-flow", "player finish cost=%dms useParse=%s multi=%s msg=%s", System.currentTimeMillis() - playerStartTime, result.shouldUseParse(), result.getUrl().isMulti(), result.getMsg());
        if (service() == null) {
            mPendingPlayer = result;
            SpiderDebug.log("video-flow", "player pending service key=%s id=%s", getKey(), getId());
            return;
        }
        mQualityAdapter.addAll(result);
        setUseParse(result.shouldUseParse());
        setQualityVisible(result.getUrl().isMulti());
        result.getUrl().set(mQualityAdapter.getPosition());
        if (result.hasArtwork()) setArtwork(result.getArtwork());
        if (result.hasDesc()) mBinding.content.setTag(result.getDesc());
        if (result.hasPosition()) mHistory.setPosition(result.getPosition());
        mBinding.control.parse.setVisibility(isUseParse() ? View.VISIBLE : View.GONE);
        startPlayer(getHistoryKey(), result, isUseParse(), getSite().getTimeout(), buildMetadata());
        String danmakuContext = getDanmakuContext();
        if (DanmakuApi.canSearch()) DanmakuApi.search(mHistory.getVodName(), getEpisode().getName(), danmakuContext, this::getDanmakuContext, danmaku -> {
            if (DanmakuSetting.isSpiderFirst() && !result.getDanmaku().isEmpty()) player().addDanmaku(danmaku);
            else player().setDanmaku(danmaku);
            player().applyDanmakuState();
            mBinding.control.action.danmaku.setSelected(DanmakuSetting.isEnabled());
        });
        else DanmakuApi.cancel();
        preloadNextEpisode();
    }

    private String getDanmakuContext() {
        if (mHistory == null || getEpisode() == null) return "";
        return getKey() + "\n" + getId() + "\n" + getFlag().getFlag() + "\n" + getEpisode().getUrl();
    }

    private void preloadNextEpisode() {
        invalidatePreload();
        if (player().getPlayerType() != PlayerSetting.EXO || !PreloadSetting.isPreload(PlayerSetting.EXO)) return;
        Episode next = mEpisodeAdapter.getNext();
        if (next == null || next.isSelected() || TextUtils.isEmpty(next.getUrl())) return;
        preloadContext = getKey() + "\n" + getFlag().getFlag() + "\n" + getEpisode().getUrl() + "\n" + next.getUrl();
        mViewModel.preloadContent(getKey(), getFlag().getFlag(), next.getUrl());
    }

    private void setPreload(Result result) {
        if (preloadContext == null || result == null || result.hasMsg() || result.shouldUseParse() || result.needParse() || TextUtils.isEmpty(result.getRealUrl())) return;
        Episode next = mEpisodeAdapter.getNext();
        if (next == null) return;
        String expected = getKey() + "\n" + getFlag().getFlag() + "\n" + getEpisode().getUrl() + "\n" + next.getUrl();
        if (!preloadContext.equals(expected)) return;
        player().preloadNext(result, getHistoryKey(), buildMetadata());
    }

    private void invalidatePreload() {
        preloadContext = null;
        if (mViewModel != null) mViewModel.cancelPreload();
        if (service() != null) player().clearNextPreload();
    }

    private void recordDetailHealth(Result result, long cost) {
        if (detailHealthRecorded) return;
        detailHealthRecorded = true;
        boolean success = result != null && !result.getList().isEmpty();
        String error = result == null ? "" : result.hasMsg() ? result.getMsg() : success ? "" : "empty";
        SiteHealthStore.recordDetail(getKey(), success, cost, error);
    }

    private void beginPlayHealth() {
        playHealthKey = getKey();
        playHealthRecorded = false;
    }

    private void recordPlayHealth(boolean success, String error) {
        if (playHealthRecorded) return;
        playHealthRecorded = true;
        SiteHealthStore.recordPlay(TextUtils.isEmpty(playHealthKey) ? getKey() : playHealthKey, success, error);
    }

    @Override
    public void onItemClick(Flag item) {
        if (isFinishing() || isDestroyed()) return;
        if (mFlagAdapter.getItemCount() == 0 || item.isSelected()) return;
        mFlagAdapter.setSelected(item);
        mBinding.flag.setSelectedPosition(mFlagAdapter.indexOf(item));
        notifyItemChanged(mBinding.flag, mFlagAdapter);
        setEpisodeAdapter(item.getEpisodes());
        setQualityVisible(false);
        seamless(item);
    }

    private void setEpisodeAdapter(List<Episode> items) {
        setEpisodeAdapter(items, true);
    }

    private void setEpisodeAdapter(List<Episode> items, boolean scrollToCurrent) {
        mBinding.episode.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        int column = EpisodeAdapter.getColumn(items);
        mBinding.episode.setNumColumns(column);
        mEpisodeAdapter.setColumn(column);
        mEpisodeAdapter.addAll(items);
        setArrayAdapter(items.size());
        updateFocus();
        updateEpisodeWindow();
        if (scrollToCurrent) scrollToCurrentEpisode();
        setR2Callback();
    }

    private void seamless(Flag flag) {
        Episode episode = flag.find(mHistory.getVodRemarks(), getMark().isEmpty());
        setQualityVisible(episode != null && episode.isSelected() && mQualityAdapter.getItemCount() > 1);
        if (episode == null || episode.isSelected()) return;
        selectEpisode(episode, false);
    }

    @Override
    public void onItemClick(Episode item) {
        if (shouldEnterFullscreen(item)) return;
        selectEpisode(item, true);
    }

    @Override
    public boolean onItemLongClick(Episode item) {
        Setting.putCompactEpisodeTitle(!Setting.isCompactEpisodeTitle());
        mEpisodeAdapter.refreshDisplayNames();
        Notify.show(Setting.isCompactEpisodeTitle() ? R.string.compact_episode_on : R.string.compact_episode_off);
        return true;
    }

    private void selectEpisode(Episode item, boolean scrollToEpisode) {
        int oldPosition = mEpisodeAdapter.getSelectedPosition();
        mFlagAdapter.toggle(item);
        int newPosition = mEpisodeAdapter.indexOf(item);
        if (newPosition == RecyclerView.NO_POSITION) newPosition = mEpisodeAdapter.getSelectedPosition();
        mEpisodeAdapter.notifySelectionChanged(oldPosition, newPosition);
        SpiderDebug.log("video-episode", "select old=%s new=%s focus=%s scroll=%s name=%s", oldPosition, newPosition, mBinding.episode.hasFocus(), scrollToEpisode, item.getName());
        if (scrollToEpisode && !mBinding.episode.hasFocus()) scrollToEpisode(newPosition);
        onRefresh();
    }

    private void setQualityVisible(boolean visible) {
        mBinding.quality.setVisibility(visible ? View.VISIBLE : View.GONE);
        updateFocus();
        setR2Callback();
    }

    @Override
    public void onItemClick(Result result) {
        beginPlayHealth();
        startPlayer(getHistoryKey(), result, isUseParse(), getSite().getTimeout(), buildMetadata());
    }

    private void reverseEpisode(boolean scroll) {
        mFlagAdapter.reverse();
        setEpisodeAdapter(getFlag().getEpisodes(), scroll);
        if (scroll) scrollToCurrentEpisode();
        else scrollToFirstEpisode();
    }

    private void scrollToCurrentEpisode() {
        scrollToEpisode(mEpisodeAdapter.getPosition());
    }

    private void scrollToFirstEpisode() {
        scrollToEpisode(0, true);
    }

    private void scrollToEpisode(int position) {
        scrollToEpisode(position, false);
    }

    private void scrollToEpisode(int position, boolean requestFocus) {
        if (position < 0 || position >= mEpisodeAdapter.getItemCount()) return;
        mBinding.episode.post(() -> {
            updateEpisodeWindowNow();
            mBinding.episode.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                mBinding.episode.setSelectedPosition(position);
                if (requestFocus) mBinding.episode.requestFocus();
            });
        });
    }

    private void updateEpisodeWindow() {
        if (mEpisodeAdapter == null || mEpisodeAdapter.getItemCount() == 0) return;
        mBinding.episode.post(this::updateEpisodeWindowNow);
    }

    private void updateEpisodeWindowNow() {
        int height = getEpisodeWindowHeight();
        if (height <= 0) return;
        ViewGroup.LayoutParams params = mBinding.episode.getLayoutParams();
        if (params instanceof LinearLayoutCompat.LayoutParams layoutParams) {
            if (layoutParams.height == height && layoutParams.weight == 0) return;
            layoutParams.height = height;
            layoutParams.weight = 0;
            mBinding.episode.setLayoutParams(layoutParams);
        } else if (params.height != height) {
            params.height = height;
            mBinding.episode.setLayoutParams(params);
        }
    }

    private int getEpisodeWindowHeight() {
        int column = Math.max(1, mEpisodeAdapter.getColumn());
        int totalRows = Math.max(1, (mEpisodeAdapter.getItemCount() + column - 1) / column);
        int rowHeight = ResUtil.dp2px(40);
        int spacing = mBinding.episode.getVerticalSpacing();
        int maxRows = ResUtil.getScreenHeight() < ResUtil.dp2px(560) ? 2 : 3;
        int rows = Math.min(totalRows, maxRows);
        return rowHeight * rows + spacing * Math.max(0, rows - 1) + mBinding.episode.getPaddingTop() + mBinding.episode.getPaddingBottom();
    }

    @Override
    public void onItemClick(Parse item) {
        setParse(item);
        onRefresh();
    }

    private void setParse(Parse item) {
        VodConfig.get().setParse(item);
        notifyItemChanged(mBinding.control.parse, mParseAdapter);
    }

    private void setArrayAdapter(int size) {
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.play_reverse));
        items.add(getString(mHistory.getRevPlayText()));
        mBinding.array.setVisibility(size > 1 ? View.VISIBLE : View.GONE);
        if (mHistory.isRevSort()) for (int i = size; i > 0; i -= 40) items.add(i + "-" + Math.max(i - 39, 1));
        else for (int i = 0; i < size; i += 40) items.add((i + 1) + "-" + Math.min(i + 40, size));
        mArrayAdapter.addAll(items);
        updateFocus();
    }

    private int findFocusDown(int index) {
        List<Integer> orders = Arrays.asList(R.id.flag, R.id.quality, R.id.array, R.id.episode);
        for (int i = 0; i < orders.size(); i++) if (i > index) if (isVisible(findViewById(orders.get(i)))) return orders.get(i);
        return 0;
    }

    private int findFocusUp(int index) {
        List<Integer> orders = Arrays.asList(R.id.flag, R.id.quality, R.id.array, R.id.episode);
        for (int i = orders.size() - 1; i >= 0; i--) if (i < index) if (isVisible(findViewById(orders.get(i)))) return orders.get(i);
        return 0;
    }

    private void updateFocus() {
        mArrayAdapter.setNextFocus(findFocusUp(2), findFocusDown(2));
        mEpisodeAdapter.setNextFocusUp(findFocusUp(3));
        mFlagAdapter.setNextFocusDown(findFocusDown(0));
        mEpisodeAdapter.setNextFocusDown(findFocusDown(3));
    }

    private boolean onEpisodeKey(KeyEvent event) {
        if (!KeyUtil.isActionDown(event) || !KeyUtil.isUpKey(event)) return false;
        RecyclerView.ViewHolder holder = mBinding.episode.findContainingViewHolder(getCurrentFocus());
        if (holder == null) return false;
        int position = holder.getBindingAdapterPosition();
        int column = Math.max(1, mEpisodeAdapter.getColumn());
        if (position == RecyclerView.NO_POSITION || position >= column) return false;
        int target = findFocusUp(3);
        if (target == 0) return false;
        View view = findViewById(target);
        if (view == null || view.getVisibility() != View.VISIBLE) return false;
        view.requestFocus();
        return true;
    }

    @Override
    public void onRevSort() {
        mHistory.setRevSort(!mHistory.isRevSort());
        reverseEpisode(false);
    }

    @Override
    public void onRevPlay(TextView view) {
        mHistory.setRevPlay(!mHistory.isRevPlay());
        view.setText(mHistory.getRevPlayText());
        Notify.show(mHistory.getRevPlayHint());
    }

    private boolean shouldEnterFullscreen(Episode item) {
        boolean enter = !isFullscreen() && item.isSelected();
        if (enter) enterFullscreen();
        return enter;
    }

    private void enterFullscreen() {
        mFocus1 = getCurrentFocus();
        mBinding.video.requestFocus();
        mBinding.video.setForeground(null);
        mBinding.video.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        mBinding.flag.setSelectedPosition(mFlagAdapter.getPosition());
        //全屏播放时品牌栏让位给画面（详情页才显示）
        mBinding.brand.getRoot().setVisibility(View.GONE);
        mKeyDown.setFull(true);
        setFullscreen(true);
    }

    private void exitFullscreen() {
        mBinding.video.setForeground(ResUtil.getDrawable(R.drawable.selector_video));
        mBinding.video.setLayoutParams(mFrameParams);
        getFocus1().requestFocus();
        mBinding.brand.getRoot().setVisibility(View.VISIBLE);
        mKeyDown.setFull(false);
        setFullscreen(false);
        hideInfo();
    }

    private void onContent() {
        if (mBinding.content.getTag() == null) return;
        ContentDialog.create().content(mBinding.content.getTag().toString()).show(this);
    }

    private void onKeep() {
        Keep keep = Keep.find(getHistoryKey());
        Notify.show(keep != null ? R.string.keep_del : R.string.keep_add);
        if (keep != null) keep.delete();
        else createKeep();
        checkKeepImg();
    }

    private void onVideo() {
        if (!isFullscreen()) enterFullscreen();
    }

    private void onChange() {
        checkSearch(true);
    }

    private void onFullscreen() {
        if (isFullscreen()) exitFullscreen();
        else enterFullscreen();
        showControl(mBinding.control.action.playPause);
    }

    private void onRepeat() {
        player().setRepeatOne(!player().isRepeatOne());
        mBinding.control.action.repeat.setSelected(player().isRepeatOne());
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        mBinding.control.action.repeat.setSelected(player().isRepeatOne());
    }

    private void checkNext() {
        checkNext(true);
    }

    private void checkNext(boolean notify) {
        if (mHistory.isRevPlay()) onPrev(notify);
        else onNext(notify);
    }

    private void checkPrev() {
        if (mHistory.isRevPlay()) onNext(true);
        else onPrev(true);
    }

    private void onNext(boolean notify) {
        Episode item = mEpisodeAdapter.getNext();
        if (item == null) {
            if (notify) Notify.show(R.string.error_play_next);
            return;
        }
        if (!item.isSelected()) onItemClick(item);
        else if (notify) Notify.show(mHistory.isRevPlay() ? R.string.error_play_prev : R.string.error_play_next);
    }

    private void onPrev(boolean notify) {
        Episode item = mEpisodeAdapter.getPrev();
        if (item == null) {
            if (notify) Notify.show(R.string.error_play_prev);
            return;
        }
        if (!item.isSelected()) onItemClick(item);
        else if (notify) Notify.show(mHistory.isRevPlay() ? R.string.error_play_next : R.string.error_play_prev);
    }

    private void onScale() {
        int index = getScale();
        String[] array = ResUtil.getStringArray(R.array.select_scale);
        setScale(index == array.length - 1 ? 0 : ++index);
    }

    private void onSpeed() {
        mBinding.control.action.speed.setText(player().addSpeed());
        mHistory.setSpeed(player().getSpeed());
    }

    private void onSpeedAdd() {
        updateSpeed(player().addSpeed(0.25f));
    }

    private void onSpeedSub() {
        updateSpeed(player().subSpeed(0.25f));
    }

    private boolean onSpeedLong() {
        updateSpeed(player().toggleSpeed());
        return true;
    }

    /**
     * 倍速变化后的状态同步：隐藏的 speed 视图保留数值文本（底部控制面板 ControlDialog 读取），
     * 控制条上的倍速按钮只显示图标，数值改在控制条上方提示，避免按了上/下没有反馈。
     */
    private void updateSpeed(String text) {
        mBinding.control.action.speed.setText(text);
        mHistory.setSpeed(player().getSpeed());
        Notify.show(ResUtil.getString(R.string.play_speed_label) + " " + text + "x");
    }

    private void onReset() {
        if (isReplay()) onReplay();
        else onRefresh();
    }

    private void onReplay() {
        mHistory.setPosition(C.TIME_UNSET);
        if (player().isEmpty()) onRefresh();
        else player().setMediaItem();
    }

    private void onRefresh() {
        saveHistory();
        player().stop();
        player().clear();
        mClock.setCallback(null);
        if (mFlagAdapter.getItemCount() == 0) return;
        if (mEpisodeAdapter.getItemCount() == 0) return;
        getPlayer(getFlag(), getEpisode());
    }

    private boolean onResetToggle() {
        Setting.putReset(Math.abs(Setting.getReset() - 1));
        mBinding.control.action.reset.setText(ResUtil.getStringArray(R.array.select_reset)[Setting.getReset()]);
        return true;
    }

    private void onOpening() {
        long position = player().getPosition();
        long duration = player().getDuration();
        if (player().canSetOpening(position, duration)) setOpening(position);
    }

    private void onOpeningAdd() {
        setOpening(Math.max(0, Math.max(0, mHistory.getOpening()) + 1000));
    }

    private void onOpeningSub() {
        setOpening(Math.max(0, Math.max(0, mHistory.getOpening()) - 1000));
    }

    private void setOpening(long opening) {
        mHistory.setOpening(opening);
        //控制条按钮 = 图标 + 文字：未设定时显示“跳过片头”，设定后显示具体秒数
        mBinding.control.action.opening.setText(opening <= 0 ? getString(R.string.play_skip_op) : Util.timeMs(opening));
    }

    private void onEnding() {
        long position = player().getPosition();
        long duration = player().getDuration();
        if (player().canSetEnding(position, duration)) setEnding(duration - position);
    }

    private void onEndingAdd() {
        setEnding(Math.max(0, Math.max(0, mHistory.getEnding()) + 1000));
    }

    private void onEndingSub() {
        setEnding(Math.max(0, Math.max(0, mHistory.getEnding()) - 1000));
    }

    private void setEnding(long ending) {
        //ending 的存储语义沿用 main：距结尾还剩多久。这里只补一个上限约束——
        //原代码不设上限，连按上键能把片尾顶到比片长还大，阈值 duration-ending 变负会导致误判，
        //因此夹紧到 0..duration-1s，其余逻辑与 main 完全一致。
        long duration = player().getDuration();
        if (duration > 0) ending = Math.min(ending, Math.max(0, duration - 1000));
        ending = Math.max(0, ending);
        mHistory.setEnding(ending);
        mBinding.control.action.ending.setText(ending <= 0 ? getString(R.string.play_skip_ed) : Util.timeMs(endingPoint(ending)));
    }

    /**
     * 片尾按钮显示的是“片尾从哪一刻开始”（duration - ending），与片头显示绝对位置保持同一语义。
     * 早期直接显示 ending（距结尾还剩多久），在 40:00 按下片尾却显示 05:05，和进度条对不上，
     * 很容易被当成 bug。存储值不变，只是把展示换算成绝对时间。
     */
    private String endingText(long ending) {
        return ending <= 0 ? getString(R.string.play_skip_ed) : Util.timeMs(endingPoint(ending));
    }

    /** 片尾文案要等拿到 duration 才能换算成绝对时间，所以每帧便宜地对一下（不一致才 setText）。 */
    private void refreshEndingText() {
        if (mHistory == null) return;
        String want = endingText(mHistory.getEnding());
        CharSequence now = mBinding.control.action.ending.getText();
        if (now == null || !want.contentEquals(now)) mBinding.control.action.ending.setText(want);
    }

    private long endingPoint(long ending) {
        long duration = player().getDuration();
        return duration > 0 ? Math.max(0, duration - ending) : ending;
    }

    /**
     * 播放/暂停按钮的图标随播放状态轮换：播放中=暂停条，暂停/未播=三角。
     * 状态来源分三层，缺一不可：
     * 1) requestPlayPause：用户操作的第一时间按“意图”换图，消除按键到回调之间的空窗；
     * 2) confirmPlayPause：onIsPlayingChanged 回执为准，任何来源（遥控器/耳机/系统媒体键）的真实状态变化都在这里收敛；
     * 3) reconcilePlayPause：命令没落地（缓冲/失败）时的兜底，最终以真实状态为准。
     */
    private void updatePlayPause() {
        if (mBinding.control.action.playPause == null) return;
        //意图未确认前以意图为准，避免被“尚未生效”的旧状态顶回去
        applyPlayPause(mPlayPausePending != null ? mPlayPausePending : player().isPlaying());
    }

    /** 用户按下播放/暂停：先换图再发命令，UI 不再等 1s 一次的时钟 tick。 */
    private void requestPlayPause(boolean playing) {
        mPlayPausePending = playing;
        mPlayPauseRetry = 0;
        App.post(mPlayPauseConfirm, PLAY_PAUSE_CONFIRM_DELAY);
        applyPlayPause(playing);
    }

    /** 播放器真实状态回执：以它为准，清掉未确认的意图。 */
    private void confirmPlayPause(boolean playing) {
        mPlayPausePending = null;
        App.removeCallbacks(mPlayPauseConfirm);
        applyPlayPause(playing);
    }

    /**
     * 兜底：命令迟迟没有生效时用真实状态校正。
     * 缓冲/空片时 isPlaying 恒为 false，此时不能用它覆盖用户意图（否则刚按播放图标又跳回三角），
     * 先重试几轮等真正开播；仍不生效才承认命令失败。
     */
    private void reconcilePlayPause() {
        if (mPlayPausePending == null) return;
        if (isBuffering() || player().isEmpty()) {
            if (mPlayPauseRetry++ < PLAY_PAUSE_CONFIRM_RETRY) App.post(mPlayPauseConfirm, PLAY_PAUSE_CONFIRM_DELAY);
            return;
        }
        confirmPlayPause(player().isPlaying());
    }

    /** 真正落图的唯一入口：只在显示态变化时换图，避免每帧重建 drawable。 */
    private void applyPlayPause(boolean playing) {
        if (mBinding.control.action.playPause == null) return;
        if (mPlayPauseShown != null && mPlayPauseShown == playing) return;
        mPlayPauseShown = playing;
        mBinding.control.action.playPause.setImageResource(playing ? R.drawable.ic_control_pause_tone : R.drawable.ic_control_play_tone);
    }

    private void onChoose() {
        PlayerHelper.choose(this, player().getUrl(), player().getHeaders(), player().isVod(), player().getPosition(), mBinding.widget.title.getText());
        setRedirect(true);
    }

    private boolean onPlayerKernel() {
        mClock.setCallback(null);
        try {
            player().togglePlayer();
            setDecode();
        } catch (Throwable t) {
            SpiderDebug.log(t);
        }
        return true;
    }

    private void onDecode() {
        mClock.setCallback(null);
        try {
            player().toggleDecode();
            setDecode();
        } catch (Throwable t) {
            SpiderDebug.log(t);
        }
    }

    private void onTrack(View view) {
        //面板打开期间控制条常驻，等面板关掉再恢复自动隐藏
        showPanel(() -> TrackDialog.create().type(Integer.parseInt(view.getTag().toString())).player(player()).show(this));
    }

    private void onTitle() {
        showPanel(() -> TitleDialog.create().player(player()).show(this));
    }

    private void onChapter() {
        showPanel(() -> ChapterDialog.create().player(player()).show(this));
    }

    /**
     * 长按弹幕按钮：打开弹幕设置面板
     */
    private void onDanmaku() {
        //弹幕设置面板打开期间控制条常驻
        showPanel(() -> DanmakuDialog.create().player(player()).show(this));
    }

    /**
     * 单击弹幕按钮：切换弹幕开/关
     */
    private void onDanmakuToggle() {
        //长按打开面板后，系统还会补发一次 click，这里不能再把弹幕关掉
        if (isCenterKeySuppressed()) return;
        DanmakuSetting.putShow(!DanmakuSetting.isShow());
        player().applyDanmakuState();
        mBinding.control.action.danmaku.setSelected(DanmakuSetting.isEnabled());
        Notify.show(DanmakuSetting.isEnabled() ? R.string.danmaku_on : R.string.danmaku_off);
    }

    private void onToggle() {
        if (isVisible(mBinding.control.getRoot())) hideControl();
        else showControl(mBinding.control.action.playPause);
    }

    private void showProgress() {
        mBinding.progress.getRoot().setVisibility(View.VISIBLE);
        App.post(mR3, 0);
        hideCenter();
        hideError();
    }

    private void hideProgress() {
        mBinding.progress.getRoot().setVisibility(View.GONE);
        App.removeCallbacks(mR3);
        Traffic.reset();
    }

    private void showError(String text) {
        mBinding.widget.error.setVisibility(View.VISIBLE);
        mBinding.widget.text.setText(text);
        hideProgress();
    }

    private void hideError() {
        mBinding.widget.error.setVisibility(View.GONE);
        mBinding.widget.text.setText("");
    }

    private void showTopInfo() {
        mBinding.widget.top.setVisibility(View.VISIBLE);
        mBinding.widget.size.setText(player().getSizeText());
    }

    private void hideInfo() {
        mBinding.widget.top.setVisibility(View.GONE);
        mBinding.widget.center.setVisibility(View.GONE);
    }


    //“…”更多按钮已移除：其菜单项（换源/内核/解码/缩放/视频轨/播放设置）分别由播放页的换源按钮
    //与“播放设置”面板（ControlDialog）承接，控制条上只保留一个螺母形的播放设置图标入口。

    /**
     * 呼出播放控制条。
     * 初始焦点由调用方显式传入：上键呼出传「片头/片尾」，其余呼出方式一律传「播放/暂停」。
     */
    private void showControl(View view) {
        if (mOsd != null) mOsd.setControlsVisible(true);
        showTopInfo();
        mBinding.control.getRoot().setVisibility(View.VISIBLE);
        updatePlayPause();
        requestControlFocus(view);
        setR1Callback();
    }

    /**
     * 控制条里有一批长期 GONE 的按钮（面板转发用），还有弹幕未加载时被隐藏的弹幕按钮。
     * 直接对它们 requestFocus() 会静默失败，控制条显示出来了却没有焦点，遥控器就像失灵一样。
     * 目标不可聚焦时统一回退到「播放/暂停」：控制条每次呼出的初始焦点都落在这个按钮上。
     */
    private void requestControlFocus(View view) {
        View target = canTakeControlFocus(view) ? view : mBinding.control.action.playPause;
        if (target == null) return;
        if (target.requestFocus()) return;
        //控件刚 setVisibility(VISIBLE) 时可能还没完成布局，下一帧再试一次
        target.post(() -> {
            if (isVisible(mBinding.control.getRoot()) && !target.hasFocus()) target.requestFocus();
        });
    }

    private boolean canTakeControlFocus(View view) {
        //这里不用 isShown()：控制条刚从 GONE 变 VISIBLE 的同一帧里它可能仍返回 false，
        //会让明明存在的目标按钮被判定为“不可聚焦”，从而回退到默认的「下一集」。
        return view != null && isInControlBar(view) && view.getVisibility() == View.VISIBLE && view.isEnabled() && view.isFocusable();
    }

    private boolean isInControlBar(View view) {
        if (view == null || mBinding == null) return false;
        ViewGroup bar = mBinding.control.getRoot();
        Object parent = view;
        while (parent instanceof View) {
            if (parent == bar) return true;
            parent = ((View) parent).getParent();
        }
        return false;
    }

    /**
     * 长按按钮弹出面板时立起抑制标记：面板是异步弹出的，松手的 ACTION_UP 往往还落在
     * Activity 上，而此刻控制条已隐藏，会被 CustomKeyDownVod 当成一次 Enter 松开 → 切换播放/暂停。
     * 标记只对“这一次按下”有效，下一次按下会重新置位，不会吞掉用户后续的确认键。
     */
    private void suppressCenterKey() {
        blockCenterKeyUp = true;
    }

    private boolean isCenterKeySuppressed() {
        return blockCenterKeyUp;
    }

    private void hideControl() {
        //弹窗打开期间控制条常驻：等所有弹窗都关掉再恢复自动隐藏
        if (mPanelHolding) return;
        if (mOsd != null) mOsd.setControlsVisible(false);
        //隐藏前先把焦点收回播放器：控件即将 GONE 时系统会清空窗口焦点，
        //再次呼出控制条就可能拿不到“上次停在哪个按钮”，表现为偶发焦点丢失。
        if (isInControlBar(getCurrentFocus()) && !mBinding.video.hasFocus()) mBinding.video.requestFocus();
        mBinding.control.getRoot().setVisibility(View.GONE);
        if (player().isPlaying()) mBinding.widget.top.setVisibility(View.GONE);
        App.removeCallbacks(mR1);
    }

    /**
     * 从控制条按钮弹出面板的统一入口。
     *
     * <p>面板存续期间控制条常驻：不隐藏、也不跑自动隐藏倒计时；所有面板关闭后
     * 重新挂上 10 秒自动隐藏（暂停态依旧常驻，见 {@link #setR1Callback()}）。
     * 凡是从控制条上弹出的面板都走这里，避免漏掉某个入口导致面板开着控制条自己消失。
     */
    private void showPanel(Runnable action) {
        holdControl();
        action.run();
    }

    /**
     * 弹窗打开期间把控制条“挂起”：不隐藏、也不走自动隐藏倒计时。
     * 用于弹幕设置面板、字幕设置、音轨选择这类「点了控制条按钮就弹出的面板」——
     * 面板开着的时候控制条如果自己消失，面板关掉后画面会突然少了一截。
     */
    private void holdControl() {
        //控制条本来就没显示：没什么可挂起的，面板照常弹
        if (isGone(mBinding.control.getRoot())) return;
        mPanelHolding = true;
        mPanelLastSeen = SystemClock.uptimeMillis();
        App.removeCallbacks(mR1);
        App.removeCallbacks(mPanelCheck);
        App.post(mPanelCheck, 300);
    }

    /**
     * 轮询弹窗是否已经全部关闭：全部关掉后解除挂起，控制条恢复超时自动隐藏。
     * 「轨道面板 → 字幕设置」是串联打开的，中间有一小段没有弹窗的空档，
     * 这里留一段宽限期，避免把串联切换误判成已经关闭。
     */
    private final Runnable mPanelCheck = new Runnable() {

        @Override
        public void run() {
            if (!mPanelHolding) return;
            if (hasPanel()) {
                mPanelLastSeen = SystemClock.uptimeMillis();
                App.post(this, 300);
                return;
            }
            if (SystemClock.uptimeMillis() - mPanelLastSeen < 600) {
                App.post(this, 300);
                return;
            }
            mPanelHolding = false;
            if (isVisible(mBinding.control.getRoot())) setR1Callback();
        }
    };

    /** 当前是否还有我方弹窗（弹幕设置 / 字幕 / 音轨等）挂在界面上 */
    private boolean hasPanel() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof BaseBottomSheetDialog) return true;
            if (fragment instanceof BaseSideSheetDialog) return true;
            if (fragment instanceof BaseAlertDialog) return true;
        }
        return false;
    }

    private void hideCenter() {
        mBinding.widget.action.setImageResource(R.drawable.ic_widget_play);
        mBinding.widget.center.setVisibility(View.GONE);
        if (isGone(mBinding.control.getRoot())) mBinding.widget.top.setVisibility(View.GONE);
    }

    private void setTraffic() {
        Traffic.setSpeed(mBinding.progress.traffic);
        App.post(mR3, 1000);
    }

    private void setR1Callback() {
        App.removeCallbacks(mR1);
        //暂停时控制条常显：不排自动隐藏，直到用户按返回/退出或恢复播放
        if (!player().isPlaying()) return;
        App.post(mR1, Constant.INTERVAL_HIDE);
    }

    private void setR2Callback() {
        App.post(mR2, 500);
    }

    private void setArtwork(String url) {
        if (mHistory != null) mHistory.setVodPic(url);
        loadArtwork(url);
    }

    private void setArtwork() {
        if (mHistory == null) return;
        loadArtwork(mHistory.getVodPic());
    }

    private void loadArtwork(String url) {
        ImgUtil.load(this, url, new CustomTarget<>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                mBinding.exo.setDefaultArtwork(resource);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                mBinding.exo.setDefaultArtwork(errorDrawable);
            }
        });
    }

    private void setPartAdapter() {
        mPartAdapter.clear();
        mBinding.part.setVisibility(View.GONE);
    }

    private void checkFlag(Vod item) {
        boolean empty = item.getFlags().isEmpty();
        mBinding.flag.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            startFlow();
        } else {
            onItemClick(mHistory.getFlag());
            if (mHistory.isRevSort()) reverseEpisode(true);
        }
    }

    private void checkHistory(Vod item) {
        mHistory = History.find(getHistoryKey());
        mHistory = mHistory == null ? createHistory(item) : mHistory;
        if (!TextUtils.isEmpty(getMark())) mHistory.setVodRemarks(getMark());
        if (Setting.isIncognito() && mHistory.getKey().equals(getHistoryKey())) mHistory.delete();
        mBinding.control.action.opening.setText(mHistory.getOpening() <= 0 ? getString(R.string.play_skip_op) : Util.timeMs(mHistory.getOpening()));
        refreshEndingText();
        applyHistorySpeed();
        mHistory.setVodName(item.getName());
        setArtwork(item.getPic());
        setScale(getScale());
        setPartAdapter();
    }

    private boolean hasInitialPreview() {
        return !getName().isEmpty() || !getPic().isEmpty();
    }

    private void showInitialPreview() {
        mBinding.progressLayout.showContent();
        mBinding.name.setText(getName());
        if (!getPic().isEmpty()) setArtwork(getPic());
        mBinding.video.requestFocus();
    }

    private History createHistory(Vod item) {
        History history = new History();
        history.setKey(getHistoryKey());
        history.setCid(VodConfig.getCid());
        history.setVodName(item.getName());
        history.findEpisode(item.getFlags());
        return history;
    }

    private void saveHistory() {
        saveHistory(false);
    }

    private void saveHistory(boolean exit) {
        if (mHistory == null || !mHistory.canSave() || Setting.isIncognito()) return;
        History history = mHistory.copy();
        Task.execute(() -> {
            history.merge().save();
            if (exit) RefreshEvent.history();
        });
    }

    private void syncHistory() {
        if (mHistory == null || Setting.isIncognito()) return;
        History history = mHistory.copy();
        Task.execute(history::save);
    }

    private void updateHistory(Episode item) {
        boolean sameEpisode = item.matchesName(mHistory.getEpisode());
        mHistory.setPosition(sameEpisode ? mHistory.getPosition() : C.TIME_UNSET);
        if (!sameEpisode) mHistory.setDuration(C.TIME_UNSET);
        mHistory.setVodFlag(getFlag().getFlag());
        mHistory.setVodRemarks(item.getName());
        mHistory.setEpisodeUrl(item.getUrl());
    }

    private void checkKeepImg() {
        mBinding.keep.setCompoundDrawablesWithIntrinsicBounds(Keep.find(getHistoryKey()) == null ? R.drawable.ic_detail_keep_off : R.drawable.ic_detail_keep_on, 0, 0, 0);
    }

    private void createKeep() {
        Keep keep = new Keep();
        keep.setKey(getHistoryKey());
        keep.setCid(VodConfig.getCid());
        keep.setVodPic(mHistory.getVodPic());
        keep.setVodName(mHistory.getVodName());
        keep.setSiteName(getSite().getName());
        keep.setCreateTime(System.currentTimeMillis());
        keep.save();
    }

    private void updateKeep() {
        Keep keep = Keep.find(getHistoryKey());
        if (keep != null) {
            keep.setVodName(mHistory.getVodName());
            keep.setVodPic(mHistory.getVodPic());
            keep.save();
        }
    }

    private void updateVod(Vod item) {
        boolean id = !item.getId().isEmpty();
        boolean pic = !item.getPic().isEmpty();
        boolean name = !item.getName().isEmpty();
        if (id) getIntent().putExtra("id", item.getId());
        if (id) mHistory.replace(getHistoryKey());
        if (name) mHistory.setVodName(item.getName());
        if (name) mBinding.name.setText(item.getName());
        if (name) mBinding.widget.title.setText(item.getName());
        updateFlag(getFlag(), item.getFlags());
        if (pic) setArtwork(item.getPic());
        if (pic || name) setMetadata();
        if (pic || name) syncHistory();
        if (pic || name) updateKeep();
        if (id) updateNavigationKey();
        if (name) setPartAdapter();
        setText(item);
    }

    private void updateFlag(Flag activated, List<Flag> items) {
        items.forEach(item -> mFlagAdapter.getItems().stream()
                .filter(item::equals).findFirst().ifPresentOrElse(target -> {
                    target.mergeEpisodes(item.getEpisodes(), mHistory.isRevSort());
                    if (target.equals(activated)) setEpisodeAdapter(target.getEpisodes());
                }, () -> mFlagAdapter.add(item)));
    }

    private final PlaybackService.NavigationCallback mNavigationCallback = new PlaybackService.NavigationCallback() {
        @Override
        public void onNext() {
            checkNext();
        }

        @Override
        public void onPrev() {
            checkPrev();
        }

        @Override
        public void onStop() {
            finish();
        }

        @Override
        public void onReplay() {
            VideoActivity.this.onReplay();
        }
    };

    @Override
    protected String getPlaybackKey() {
        return getHistoryKey();
    }

    @Override
    protected void onPrepare() {
        setDecode();
        setPosition();
        applyHistorySpeed();
    }

    private void applyHistorySpeed() {
        if (mHistory == null) return;
    }

    @Override
    protected void onTracksChanged() {
        setTrackVisible();
        mClock.setCallback(this);
        if (mAudio != null) {
            mAudio.onTracksChanged();
            mAudio.updateImmersiveAction();
        }
    }

    @Override
    protected void onTitlesChanged() {
        setTitleVisible();
        if (mAudio != null) mAudio.updateImmersiveAction();
    }

    @Override
    protected void onError(String msg) {
        recordPlayHealth(false, msg);
        //命令已经失败，作废未确认的播放意图，避免图标卡在乐观状态
        mPlayPausePending = null;
        App.removeCallbacks(mPlayPauseConfirm);
        Track.delete(player().getKey());
        mClock.setCallback(null);
        player().resetTrack();
        player().reset();
        player().stop();
        showError(msg);
        startFlow();
    }

    @Override
    protected void onReclaim() {
        Result result = mViewModel.getPlayer().getValue();
        if (result != null) setPlayer(result);
    }

    @Override
    protected void onStateChanged(int state) {
        PlaybackEventCollector.get().onPlaybackStateChanged(player(), state);
        switch (state) {
            case Player.STATE_BUFFERING:
                showProgress();
                break;
            case Player.STATE_READY:
                recordPlayHealth(true, "");
                hideProgress();
                player().reset();
                if (mAudio != null) mAudio.onStateChanged(state);
                break;
            case Player.STATE_ENDED:
                checkEnded(true);
                break;
        }
    }

    @Override
    protected void onPlayingChanged(boolean isPlaying) {
        PlaybackEventCollector.get().onIsPlayingChanged(player(), isPlaying);
        //真实播放状态回执：按钮图标立刻与之对齐（不再等 onTimeChanged 的 1s tick）
        confirmPlayPause(isPlaying);
        if (mAudio != null) mAudio.onPlayingChanged(isPlaying);
        if (isPlaying) {
            hideCenter();
        } else if (isPaused()) {
            //暂停不再弹居中的播放图标/时间叠层：只保留顶部片名与时长信息。
            //居中叠层现在只服务于「快进/快退」的过程提示（见 onSeeking），暂停态不再借用它。
            if (isFullscreen()) showTopInfo();
            else hideInfo();
        }
    }

    @Override
    protected void onSizeChanged(VideoSize size) {
        mBinding.widget.size.setText(player().getSizeText());
    }

    @Override
    public void onSubtitleClick() {
        //字幕设置面板打开期间控制条常驻
        showPanel(() -> SubtitleSettingDialog.create().view(mBinding.exo.getSubtitleView()).player(player()).show(this));
    }

    @Override
    public void onTimeChanged(long time) {
        if (!isOwner()) return;
        long position, duration;
        mHistory.setCreateTime(time);
        mHistory.setPosition(position = player().getPosition());
        mHistory.setDuration(duration = player().getDuration());
        if (mAudio != null) mAudio.onTimeChanged();
        PlaybackEventCollector.get().onProgress(mHistory, player());
        if (mHistory.canSave() && mHistory.canSync()) syncHistory();
        //片尾文案依赖 duration，拿到时长后换算成“片尾从哪一刻开始”
        refreshEndingText();
        //片尾判定沿用 main 分支原始实现：到达 duration - ending 即认为本集结束。
        if (mHistory.getEnding() > 0 && duration > 0 && mHistory.getEnding() + position >= duration) {
            checkEnded(false);
        }
        updatePlayPause();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (isRedirect()) return;
        if (event.getType() == RefreshEvent.Type.DETAIL) getDetail();
        else if (event.getType() == RefreshEvent.Type.PLAYER) onRefresh();
        else if (event.getType() == RefreshEvent.Type.VOD) updateVod(event.getVod());
        else if (event.getType() == RefreshEvent.Type.SUBTITLE) player().setSub(Sub.from(event.getPath()));
        else if (event.getType() == RefreshEvent.Type.DANMAKU) player().setDanmaku(Danmaku.from(event.getPath()));
    }

    private void setPosition() {
        if (mHistory != null) player().seekTo(Math.max(mHistory.getOpening(), mHistory.getPosition()));
    }

    private void checkEnded(boolean notify) {
        checkNext(notify);
    }

    private void setTrackVisible() {
        boolean hasText = player().haveTrack(C.TRACK_TYPE_TEXT) || player().isVod();
        mBinding.control.action.text.setVisibility(hasText ? View.VISIBLE : View.GONE);
        //字幕图标按“是否已选中字幕轨”显示激活态
        mBinding.control.action.text.setSelected(hasText && isTextSelected());
        mBinding.control.action.audio.setVisibility(player().haveTrack(C.TRACK_TYPE_AUDIO) ? View.VISIBLE : View.GONE);
        mBinding.control.action.video.setVisibility(player().haveTrack(C.TRACK_TYPE_VIDEO) ? View.VISIBLE : View.GONE);
    }

    private boolean isTextSelected() {
        try {
            Tracks tracks = player().getCurrentTracks();
            for (Tracks.Group group : tracks.getGroups()) {
                if (group.getType() != C.TRACK_TYPE_TEXT) continue;
                for (int i = 0; i < group.length; i++) if (group.isTrackSelected(i)) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void setTitleVisible() {
        mBinding.control.action.title.setVisibility(player().haveTitle() ? View.VISIBLE : View.GONE);
        mBinding.control.action.chapter.setVisibility(player().haveChapter() ? View.VISIBLE : View.GONE);
    }

    private MediaMetadata buildMetadata() {
        String title = mHistory.getVodName();
        String episode = getEpisode().getName();
        boolean empty = episode.isEmpty() || title.equals(episode);
        String artist = empty ? "" : episode;
        return PlayerManager.buildMetadata(title, artist, mHistory.getVodPic());
    }

    private void setMetadata() {
        player().setMetadata(buildMetadata());
    }

    private void startFlow() {
        if (!getSite().isChangeable()) return;
        if (isUseParse()) checkParse();
        else checkFlag();
    }

    private void checkParse() {
        int position = mParseAdapter.getPosition();
        boolean last = position == mParseAdapter.getItemCount() - 1;
        boolean pass = position == 0 || last;
        if (last) initParse();
        if (pass) checkFlag();
        else nextParse(position);
    }

    private void initParse() {
        if (mParseAdapter.getItemCount() == 0) return;
        setParse(mParseAdapter.first());
    }

    private void checkFlag() {
        int position = isGone(mBinding.flag) ? -1 : mFlagAdapter.getPosition();
        if (position == mFlagAdapter.getItemCount() - 1) checkSearch(false);
        else nextFlag(position);
    }

    private void checkSearch(boolean force) {
        if (mQuickAdapter.getItemCount() == 0) initSearch(mBinding.name.getText().toString(), true);
        else if (isAutoMode() || force) nextSite();
    }

    private void initSearch(String keyword, boolean auto) {
        setAutoMode(auto);
        setInitAuto(auto);
        startSearch(keyword);
        mBinding.part.setTag(keyword);
    }

    private boolean isPass(Site item) {
        if (SiteBlockSetting.isBlocked(item)) return false;
        if (isAutoMode() && !item.isChangeable()) return false;
        return item.isSearchable();
    }

    private void startSearch(String keyword) {
        mQuickAdapter.clear();
        dismissQuickSearchDialog();
        quickSearchDialogClosed = false;
        if (!isInitAuto()) showQuickSearchDialog(new ArrayList<>());
        List<Site> sites = new ArrayList<>();
        for (Site site : VodConfig.get().getSites()) if (isPass(site)) sites.add(site);
        SiteHealthStore.sortSites(sites);
        mViewModel.searchContent(sites, keyword, true);
    }

    private void setSearch(Result result) {
        List<Vod> items = result.getList();
        items.removeIf(this::mismatch);
        mQuickAdapter.addAll(items);
        mBinding.quick.setVisibility(View.GONE);
        if (!isInitAuto() && !items.isEmpty()) showQuickSearchDialog(items);
        if (isInitAuto()) nextSite();
        if (items.isEmpty()) return;
        App.removeCallbacks(mR4);
    }

    private void setSearchProgress(SearchProgress progress) {
        if (progress == null || isInitAuto()) return;
        showQuickSearchDialog(new ArrayList<>());
        if (mQuickSearchDialog != null) mQuickSearchDialog.setProgress(progress.current(), progress.total(), progress.finished());
    }

    private void showQuickSearchDialog(List<Vod> items) {
        if (quickSearchDialogClosed) return;
        if (mQuickSearchDialog != null) {
            mQuickSearchDialog.addAll(items);
            return;
        }
        QuickSearchDialog dialog = QuickSearchDialog.create().listener(this).items(items);
        dialog.dismissListener(d -> {
            if (mQuickSearchDialog != dialog) return;
            mQuickSearchDialog = null;
            quickSearchDialogClosed = true;
        });
        mQuickSearchDialog = dialog;
        dialog.show(this);
    }

    private void dismissQuickSearchDialog() {
        QuickSearchDialog dialog = mQuickSearchDialog;
        mQuickSearchDialog = null;
        if (dialog != null) dialog.dismissAllowingStateLoss();
    }

    @Override
    public void onItemClick(Vod item) {
        setAutoMode(false);
        getDetail(item);
    }

    private boolean mismatch(Vod item) {
        if (getId().equals(item.getId())) return true;
        if (mBroken.contains(item.getId())) return true;
        String keyword = Objects.toString(mBinding.part.getTag(), "");
        if (isAutoMode()) return !item.getName().equals(keyword);
        else return !item.getName().contains(keyword);
    }

    private void nextParse(int position) {
        Parse parse = mParseAdapter.get(position + 1);
        Notify.show(getString(R.string.play_switch_parse, parse.getName()));
        onItemClick(parse);
    }

    private void nextFlag(int position) {
        Flag flag = mFlagAdapter.get(position + 1);
        Notify.show(getString(R.string.play_switch_flag, flag.getFlag()));
        onItemClick(flag);
    }

    private void nextSite() {
        if (mQuickAdapter.getItemCount() == 0) return;
        int position = mQuickAdapter.getBestPosition();
        Vod item = mQuickAdapter.get(position);
        Notify.show(getString(R.string.play_switch_site, item.getSiteName()));
        mQuickAdapter.remove(position);
        mBroken.add(getId());
        setInitAuto(false);
        getDetail(item);
    }

    private void onPaused() {
        //先换图再发命令：controller() 是 MediaController 代理，pause() 走 IPC 异步生效，
        //调用后立刻查 isPlaying 仍是 true，旧写法会把图标留在“暂停条”上直到下次时钟 tick
        requestPlayPause(false);
        controller().pause();
        //正在倒计时的自动隐藏要取消，否则暂停后控制条仍会自己消失
        App.removeCallbacks(mR1);
        //暂停即呼出控制条：用户按下暂停多半就是想操作，没必要再手动唤一次
        if (isGone(mBinding.control.getRoot())) showControl(mBinding.control.action.playPause);
    }

    private void onPlay() {
        requestPlayPause(true);
        if (mHistory != null && isEnded()) controller().seekTo(mHistory.getOpening());
        if (!player().isEmpty() && isIdle()) controller().prepare();
        controller().play();
        if (isVisible(mBinding.control.getRoot())) setR1Callback();
    }

    /**
     * 播放/暂停切换：控制条上的「播放/暂停」按钮与进度条焦点按确定键共用这一套逻辑，
     * 两处行为必须一致，否则会出现「按钮切了、进度条没切」的割裂感。
     */
    private void togglePlayPause() {
        if (service() == null || controller() == null) return;
        if (player().isPlaying()) onPaused();
        else onPlay();
    }

    /**
     * 焦点是否落在播放进度条上（真正接收遥控器焦点的是进度条本体，不是外层容器）。
     */
    private boolean isSeekFocused() {
        View focus = getCurrentFocus();
        return focus != null && focus == mBinding.control.seek.getFocusView();
    }

    private boolean onSeekBack() {
        controller().seekBack();
        return true;
    }

    private boolean onSeekForward() {
        controller().seekForward();
        return true;
    }

    private boolean isFullscreen() {
        return fullscreen;
    }

    private void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        mBinding.control.action.fullscreen.setText(fullscreen ? R.string.play_exit_fullscreen : R.string.play_fullscreen);
    }

    private boolean isInitAuto() {
        return initAuto;
    }

    private void setInitAuto(boolean initAuto) {
        this.initAuto = initAuto;
    }

    private boolean isAutoMode() {
        return autoMode;
    }

    private void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
    }

    public boolean isUseParse() {
        return useParse;
    }

    public void setUseParse(boolean useParse) {
        this.useParse = useParse;
    }

    private View getFocus1() {
        return mFocus1 == null || mFocus1.getVisibility() != View.VISIBLE ? mBinding.video : mFocus1;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isFullscreen() && KeyUtil.isMenuKey(event)) onToggle();
        //控制条显示期间任何按键都重置自动隐藏计时（暂停态 setR1Callback 内部直接返回，保持常驻）
        if (isVisible(mBinding.control.getRoot())) setR1Callback();
        if (KeyUtil.isEnterKey(event)) {
            if (KeyUtil.isActionDown(event)) {
                //只有“首次按下”才重置标记：长按期间系统会持续补发 ACTION_DOWN 重复事件，
                //每次都重置的话，长按处理器刚立起的抑制窗口会被立刻抹掉，松手照样切播放/暂停。
                if (event.getRepeatCount() == 0) {
                    blockCenterKeyUp = false;
                    enterDownOnControlBar = isVisible(mBinding.control.getRoot()) && isInControlBar(getCurrentFocus());
                }
                //焦点落在进度条上时按确定键 = 播放/暂停切换（进度条本身不吃确认键）
                if (event.getRepeatCount() == 0 && isSeekFocused()) {
                    togglePlayPause();
                    return true;
                }
            } else {
                boolean suppressed = blockCenterKeyUp;
                blockCenterKeyUp = false;
                //按下时控制条还在、松手时已经不在了：中途弹了面板把控制条收走，
                //这次松手不能再落到播放器头上被当成“播放/暂停”。
                boolean barGone = enterDownOnControlBar && isGone(mBinding.control.getRoot());
                enterDownOnControlBar = false;
                if (suppressed || barGone) return true;
            }
        }
        if (onEpisodeKey(event)) return true;
        if (onVerticalKey(event)) return true;
        if (isFullscreen() && isGone(mBinding.control.getRoot()) && mKeyDown.hasEvent(event) && service() != null) return mKeyDown.onKeyDown(event);
        if (KeyUtil.isMediaFastForward(event)) return onSeekForward();
        if (KeyUtil.isMediaRewind(event)) return onSeekBack();
        return super.dispatchKeyEvent(event);
    }

    /**
     * 上/下方向键的焦点仲裁 —— 播放页唯一的判定点，两条规则互斥且互补。
     *
     * <p>判据只有一条：{@link #isInControlBar(View)} 给出的「焦点是不是落在控制条内」。
     * 控制条可见时，整页可聚焦的东西只有两处 —— 控制条本身、以及铺满全屏的播放画面，
     * 所以「在控制条内 / 不在控制条内」天然构成一个不重不漏的划分。
     *
     * <p>规则一（焦点在控制条内）：放行给视图层处理。
     * 进度条按「下」进按钮行、落点是「播放/暂停」；按钮行内部按「下」不动
     * （nextFocusDown 指向自己）；按钮行按「上」统一回进度条 —— 见 {@link #bindControlFocus()}。
     * <b>这里必须放行</b>：CustomUpDownView 靠首次 ACTION_DOWN 建立键盘长按判定，
     * 若在此处 return true，倍速与片头片尾的长按连发就再也收不到事件。
     *
     * <p>规则二（焦点在控制条外）：一律不移动焦点，把按下与松手整对消费掉。
     * 默认行为是交给 FocusFinder 做几何搜索：控制条贴在画面底部、四周没有可聚焦候选，
     * 焦点会被甩到画面或别的按钮上，表现为「按一下方向键焦点就没了」。
     *
     * <p>控制条不可见时两条规则都不成立：此时方向键属于手势层
     * （呼出控制条 / 快进快退 / 上滑调倍速）或详情页的 nextFocus 链，由各自逻辑接管。
     * 详情页（非全屏）即便控制条意外残留，规则二也不生效，免得详情页的按钮导航被误伤。
     */
    private boolean onVerticalKey(KeyEvent event) {
        if (!KeyUtil.isUpKey(event) && !KeyUtil.isDownKey(event)) return false;
        if (!isVisible(mBinding.control.getRoot())) return false;
        //规则一：控制条内的上下键交给视图层，Activity 不插手
        if (isInControlBar(getCurrentFocus())) return false;
        //规则二只作用于全屏播放态：详情页的按钮、选集、筛选列表仍走各自的 nextFocus 链
        return isFullscreen();
    }

    @Override
    public void onSeeking(long time) {
        mBinding.widget.center.setVisibility(View.VISIBLE);
        mBinding.widget.duration.setText(player().getDurationTime());
        mBinding.widget.position.setText(player().getPositionTime(time));
        mBinding.widget.action.setImageResource(time > 0 ? R.drawable.ic_widget_forward : R.drawable.ic_widget_rewind);
        hideProgress();
    }

    @Override
    public void onSeekEnd(long time) {
        mKeyDown.reset();
        seekTo(time);
    }

    @Override
    public void onSpeedUp() {
        if (!player().isPlaying()) return;
        mBinding.widget.speed.setVisibility(View.VISIBLE);
        mBinding.widget.speed.startAnimation(ResUtil.getAnim(R.anim.forward));
        mBinding.control.action.speed.setText(player().setSpeed(PlayerSetting.getSpeed()));
    }

    @Override
    public void onSpeedEnd() {
        mBinding.widget.speed.clearAnimation();
        mBinding.widget.speed.setVisibility(View.GONE);
        mBinding.control.action.speed.setText(player().setSpeed(mHistory.getSpeed()));
    }

    @Override
    public void onKeyUp() {
        //上键呼出：初始焦点只按当前进度决定 —— 进度够靠前落「片头」，够靠后落「片尾」，
        //两头都不满足（片长太短、进度卡在中间算不出可设区间）就退回「播放/暂停」。
        long position = player().getPosition();
        long duration = player().getDuration();
        if (player().canSetOpening(position, duration)) {
            showControl(mBinding.control.action.opening);
        } else if (player().canSetEnding(position, duration)) {
            showControl(mBinding.control.action.ending);
        } else {
            showControl(mBinding.control.action.playPause);
        }
    }

    @Override
    public void onKeyDown() {
        //下键呼出：初始焦点固定为「播放/暂停」
        showControl(mBinding.control.action.playPause);
    }

    @Override
    public void onKeyCenter() {
        //先记下“这次是按暂停还是按播放”：pause() 是异步的，调完立刻查 isPlaying 仍为 true，
        //用旧写法会把刚呼出的控制条又收起来。
        boolean toPause = player().isPlaying();
        if (toPause) onPaused();
        else if (player().isEmpty()) onRefresh();
        else onPlay();
        //确定键也是控制条的呼出入口：还没出来就呼出、焦点交给「播放/暂停」；
        //已经在显示就只续期自动隐藏计时，不打断用户当前停留的按钮。
        if (isVisible(mBinding.control.getRoot())) setR1Callback();
        else showControl(mBinding.control.action.playPause);
    }

    @Override
    public void onSingleTap() {
        if (isFullscreen()) onToggle();
    }

    @Override
    public void onDoubleTap() {
        if (isFullscreen()) onKeyCenter();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || requestCode != 1001) return;
        PlaybackService service = service();
        var controller = controller();
        if (service == null || controller == null) return;
        PlayerHelper.onExternalResult(data, service::dispatchNext, controller::seekTo);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mOsd != null) mOsd.start();
        mClock.stop().start();
        if (mBrand != null) mBrand.start();
        //播放页不显示任何提示窗（提示会压在控制条按钮上），进入时整体关闭
        Tip.setBlocked(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Tip.setBlocked(false);
        if (mOsd != null) mOsd.stop();
        if (mBrand != null) mBrand.stop();
        // The media service can unbind while the activity is still stopping (session teardown,
        // background kill); every player touch here must survive a null service.
        if (service() != null) PlaybackEventCollector.get().onStop(player());
        if (PlayerSetting.isBackgroundOff()) mClock.stop();
    }

    @Override
    protected void onBackInvoked() {
        if (mAudio != null && mAudio.interceptBack()) {
            mAudio.consumeBack();
            return;
        }
        if (isVisible(mBinding.control.getRoot())) {
            hideControl();
        } else if (isVisible(mBinding.widget.center)) {
            hideCenter();
        } else if (isFullscreen()) {
            exitFullscreen();
        } else {
            mViewModel.stopSearch();
            if (isTaskRoot()) startActivity(new Intent(this, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            super.onBackInvoked();
        }
    }

    @Override
    protected void onDestroy() {
        invalidatePreload();
        if (mAudio != null) mAudio.release();
        if (mOsd != null) mOsd.release();
        mClock.release();
        saveHistory(true);
        DanmakuApi.cancel();
        RefreshEvent.keep();
        App.removeCallbacks(mR1, mR2, mR3, mR4);
        App.removeCallbacks(mPlayPauseConfirm);
        App.removeCallbacks(mPanelCheck);
        SiteHealthStore.flush();
        PlaybackEventCollector.get().setPlayer(null);
        PlaybackEventCollector.get().updateHistory(null);
        super.onDestroy();
    }
}
