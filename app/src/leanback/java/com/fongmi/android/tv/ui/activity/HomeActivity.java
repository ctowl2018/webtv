package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.ViewPager;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Cache;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.db.BackupManager;
import com.fongmi.android.tv.event.CastEvent;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.service.DLNARendererService;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.ui.adapter.BaseDiffCallback;
import com.fongmi.android.tv.ui.adapter.TypeAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.base.TypeHost;
import com.fongmi.android.tv.ui.custom.CustomLogoBar;
import com.fongmi.android.tv.ui.custom.CustomRowPresenter;
import com.fongmi.android.tv.ui.custom.CustomSelector;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.ui.fragment.FolderFragment;
import com.fongmi.android.tv.ui.fragment.HomePageFragment;
import com.fongmi.android.tv.ui.presenter.HeaderPresenter;
import com.fongmi.android.tv.ui.presenter.HistoryPresenter;
import com.fongmi.android.tv.ui.presenter.ProgressPresenter;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.LocalNetworkPermission;
import com.fongmi.android.tv.utils.NsdDeviceDiscovery;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;
import com.google.common.collect.Lists;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class HomeActivity extends BaseActivity implements CustomLogoBar.Listener, VodPresenter.OnClickListener, HistoryPresenter.OnClickListener, TypeAdapter.OnClickListener, TypeHost {

    public static final String EXTRA_FOCUS_HOME = "focus_home";
    //导航选中 → 翻页的触发阈值：与分类页（VodActivity）保持同一数值
    private static final long PAGE_SWITCH_DELAY = 100;

    private ActivityHomeBinding mBinding;
    private ArrayObjectAdapter mHistoryAdapter;
    private ArrayObjectAdapter mAdapter;
    private HistoryPresenter mPresenter;
    private SiteViewModel mViewModel;
    private TypeAdapter mTypeAdapter;
    private Result mResult;
    private Result mHomeResult;
    private Clock mClock;
    private boolean loadingHomeCategory;
    private boolean mPendingHomeFocus;
    private long mExitPressedTime;
    private View mOldTypeView;

    private Site getHome() {
        return VodConfig.get().getHome();
    }

    private Config getConfig() {
        return VodConfig.get().getConfig();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkAction(intent);
        //从分类页经「首页」按钮回到首页：焦点落在「首页」按钮上
        if (intent.getBooleanExtra(EXTRA_FOCUS_HOME, false)) mPendingHomeFocus = true;
    }

    /**
     * 首页恢复时系统会把焦点还原到离开前的控件，单次 requestFocus 容易被覆盖，
     * 因此在一个很短的窗口内兜底几次。
     * 注意：这里必须点名聚焦导航行第一项（「首页」），不能只调 typeRecycler.requestFocus()——
     * 网格会把焦点还给上一次停留的分类标签，表现为“从分类页回来焦点还停在原分类上”。
     */
    private void focusHomeButton(int attempt) {
        if (isFinishing() || attempt > 4) return;
        View home = navHomeView();
        if (home == null) {
            //「首页」被滚出屏幕时先滚回起点，下一轮再聚焦
            mBinding.typeRecycler.scrollToPosition(0);
            App.post(() -> focusHomeButton(attempt + 1), 80);
            return;
        }
        if (home.isFocused()) return;
        if (home.requestFocus()) return;
        App.post(() -> focusHomeButton(attempt + 1), 80);
    }

    /** 分类行里「首页」那一项的视图；不在屏幕上（被滚走）时返回 null */
    private View navHomeView() {
        RecyclerView.ViewHolder holder = mBinding.typeRecycler.findViewHolderForAdapterPosition(0);
        return holder == null ? null : holder.itemView;
    }

    /* ───────────────────────── 导航 ↔ 分页（与分类页同一套实现） ───────────────────────── */

    /**
     * 把首页内容从竖向容器里摘出来，交给 {@link HomePageFragment} 作为 Pager 的第 0 页挂载。
     * 视图对象本身始终由 Activity 持有（mBinding.progressLayout 不受影响），
     * 所以适配器、ViewModel、焦点逻辑全都不用改。
     */
    public View takeHomeContent() {
        View content = mBinding == null ? null : mBinding.progressLayout;
        if (content != null && content.getParent() instanceof ViewGroup) ((ViewGroup) content.getParent()).removeView(content);
        return content;
    }

    private void detachHomeContent() {
        takeHomeContent();
    }

    private void setPager() {
        mBinding.pager.setAdapter(new PageAdapter(getSupportFragmentManager()));
        mBinding.pager.setOffscreenPageLimit(1);
    }

    private void setPagerEvent() {
        //标签 → 翻页：与分类页一致的 100ms 触发阈值
        mBinding.typeRecycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                onTypeSelected(child);
            }
        });
        //翻页 → 标签：同样只在值不同时才写回，避免与上面的 Runnable 互相触发形成回环
        mBinding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                syncSelectedType(position);
            }
        });
    }

    private void onTypeSelected(@Nullable RecyclerView.ViewHolder child) {
        if (mOldTypeView != null) mOldTypeView.setSelected(false);
        if ((mOldTypeView = child != null ? child.itemView : null) == null) return;
        mOldTypeView.setSelected(true);
        App.removeCallbacks(mTypeRunnable);
        App.post(mTypeRunnable, PAGE_SWITCH_DELAY);
    }

    /** 导航选中项 → 页码：首页是第 0 页，分类 i 是第 i+1 页，二者共用同一个下标 */
    private final Runnable mTypeRunnable = new Runnable() {
        @Override
        public void run() {
            int selected = mBinding.typeRecycler.getSelectedPosition();
            if (selected < 0 || selected >= mBinding.pager.getAdapter().getCount()) return;
            if (mBinding.pager.getCurrentItem() != selected) mBinding.pager.setCurrentItem(selected);
        }
    };

    private void syncSelectedType(int position) {
        if (mBinding.typeRecycler.getSelectedPosition() != position) mBinding.typeRecycler.setSelectedPosition(position);
        FolderFragment fragment = getPageFragment(position);
        if (fragment != null) fragment.setNavSelected(position);
        if (isInTypeBar(getCurrentFocus())) mBinding.typeRecycler.requestFocus();
    }

    private int getPage() {
        return mBinding.pager.getAdapter() == null ? 0 : mBinding.pager.getCurrentItem();
    }

    /**
     * 取某一页的分类碎片；第 0 页（首页）不是分类碎片，返回 null。
     * <p>
     * 先按「分类 typeId」在 FragmentManager 里找，而不是靠 ViewPager 的
     * {@code android:switcher:<id>:<pos>} tag —— tag 的拼法属于 FragmentStatePagerAdapter 的内部实现，
     * 一旦对不上就会永远返回 null，表现为「筛选打不开、按↓进不去内容」这类静默失效。
     */
    private FolderFragment getPageFragment(int page) {
        if (page <= 0 || mTypeAdapter == null || page > mTypeAdapter.size()) return null;
        Class type = mTypeAdapter.get(page - 1);
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (!(fragment instanceof FolderFragment)) continue;
            FolderFragment folder = (FolderFragment) fragment;
            Class item = folder.getType();
            if (folder.isAdded() && item != null && item.getTypeId().equals(type.getTypeId())) return folder;
        }
        //兜底：按 tag 再找一次（正常情况下上面的循环已经命中）
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("android:switcher:" + mBinding.pager.getId() + ":" + page);
        return fragment instanceof FolderFragment ? (FolderFragment) fragment : null;
    }

    private boolean isInTypeBar(View view) {
        while (view != null) {
            if (view == mBinding.typeRecycler) return true;
            ViewParent parent = view.getParent();
            view = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    /** 切到某个分类页（下标 = 分类下标 + 1） */
    private void switchPage(int page) {
        if (page < 0 || page >= mBinding.pager.getAdapter().getCount()) return;
        if (mBinding.pager.getCurrentItem() != page) mBinding.pager.setCurrentItem(page);
        if (mBinding.typeRecycler.getSelectedPosition() != page) mBinding.typeRecycler.setSelectedPosition(page);
    }

    private void resetPager() {
        mBinding.pager.setAdapter(new PageAdapter(getSupportFragmentManager()));
        mBinding.pager.setOffscreenPageLimit(1);
        mBinding.pager.setCurrentItem(0);
        mBinding.typeRecycler.setSelectedPosition(0);
    }

    /** 焦点是否在当前页的内容区：第 0 页是首页内容，其余页是分类碎片的内容 */
    private boolean isInContent(View view) {
        if (view == null) return false;
        int page = getPage();
        if (page > 0) {
            FolderFragment fragment = getPageFragment(page);
            return fragment != null && fragment.isInContent(view);
        }
        while (view != null) {
            if (view == mBinding.recycler) return true;
            ViewParent parent = view.getParent();
            view = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    private boolean focusFirstFocusable(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) continue;
            if (child instanceof ViewGroup group && focusFirstFocusable(group)) return true;
            if (child.isFocusable() && child.requestFocus()) return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_App);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        SpiderDebug.log("startup", "home initView start cost=%sms", System.currentTimeMillis() - App.time());
        mResult = Result.empty();
        mHomeResult = Result.empty();
        mClock = Clock.create(mBinding.clock);
        mBinding.progressLayout.showProgress();
        detachHomeContent();
        setToolbar();
        setRecyclerView();
        //TypeAdapter 建好之后再挂分页：PageAdapter.getCount() 依赖它
        setPager();
        setViewModel();
        setAdapter();
        //首页被系统回收后重建时同样需要把焦点放进内容区
        mPendingHomeFocus = getIntent().getBooleanExtra(EXTRA_FOCUS_HOME, false);
        runAfterFirstFrame(this::initAfterFirstFrame);
        SpiderDebug.log("startup", "home initView end cost=%sms", System.currentTimeMillis() - App.time());
    }

    private void initAfterFirstFrame() {
        SpiderDebug.log("startup", "home first frame cost=%sms", System.currentTimeMillis() - App.time());
        App.post(this::initConfig, 80);
        //首帧已画好，组合标再停留一会儿就淡出；电视上首帧到可见界面有延迟，留足 0.9 秒避免一闪而过
        App.post(this::hideSplash, 900);
        App.post(() -> PermissionUtil.requestFile(this, allGranted -> PermissionUtil.requestNotify(this)), 1800);
        App.post(() -> LocalNetworkPermission.request(this, granted -> {
            if (granted) DLNARendererService.start(this);
        }), 2500);
    }

    /**
     * 启动闪屏：系统 SplashScreen 放不下横向品牌组合，这里用同底色的一层绘出「图标 + 影视 Videos」。
     * 版式与标题栏完全一致（同一套 dp 尺寸与 24dp 边距，见 activity_home.xml），
     * 闪屏淡出时 logo 不会发生位置/大小跳动。
     */
    private void hideSplash() {
        if (mBinding.splashLayer.getVisibility() != View.VISIBLE) return;
        mBinding.splashLayer.animate().alpha(0f).setDuration(320)
                .withEndAction(() -> mBinding.splashLayer.setVisibility(View.GONE)).start();
    }

    private void runAfterFirstFrame(Runnable runnable) {
        View root = mBinding.getRoot();
        root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (root.getViewTreeObserver().isAlive()) root.getViewTreeObserver().removeOnPreDrawListener(this);
                root.post(runnable);
                return true;
            }
        });
    }

    @Override
    protected void initEvent() {
        mBinding.logoBar.setListener(this);
        setPagerEvent();
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (mPresenter.isDelete()) setHistoryDelete(false);
            }
        });
    }

    private void setToolbar() {
        //搜索、收藏、设置不再占用首页内容区，改为右上角仅图标入口（时钟左侧）
        mBinding.btnSearch.setOnClickListener(v -> SearchActivity.start(this));
        mBinding.btnKeep.setOnClickListener(v -> KeepActivity.start(this));
        mBinding.btnSetting.setOnClickListener(v -> SettingActivity.start(this));
    }

    /** 「首页」是分类行里的第一项（见 TypeAdapter 的 withHome），点击行为在这里 */
    @Override
    public void onHomeClick() {
        //已经在首页时，「首页」按钮回到列表顶部
        if (mBinding.progressLayout.isProgress()) return;
        mBinding.recycler.scrollToPosition(0);
        requestContentFocus();
    }

    private void checkAction(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            VideoActivity.push(this, intent.getStringExtra(Intent.EXTRA_TEXT));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            PermissionUtil.requestFile(this, allGranted -> checkType(intent));
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String keyword = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(keyword)) SearchActivity.start(this, keyword);
        }
    }

    private void checkType(Intent intent) {
        VideoActivity.push(this, intent.getData().toString());
    }

    @SuppressLint("RestrictedApi")
    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(Integer.class, new HeaderPresenter());
        selector.addPresenter(String.class, new ProgressPresenter());
        selector.addPresenter(Vod.class, new VodPresenter(this, Style.list()));
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16, FocusHighlight.ZOOM_FACTOR_SMALL, HorizontalGridView.FOCUS_SCROLL_ALIGNED), HistoryPresenter.class);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
        mBinding.typeRecycler.setHorizontalSpacing(ResUtil.dp2px(16));
        mBinding.typeRecycler.setRowHeight(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.typeRecycler.setAdapter(mTypeAdapter = new TypeAdapter(this, true));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.getResult().observe(this, result -> {
            boolean categoryResult = isHomeCategoryResult(result);
            mAdapter.remove("progress");
            if (!categoryResult) {
                Cache.clear().put(result);
                setTypes(mHomeResult = result);
            }
            mResult = result;
            addVideo(result);
        });
    }

    private boolean isHomeCategoryResult(Result result) {
        return loadingHomeCategory && result.getTypes().isEmpty();
    }

    private void setAdapter() {
        mHistoryAdapter = new ArrayObjectAdapter(mPresenter = new HistoryPresenter(this));
        mAdapter.add(R.string.home_history);
        mAdapter.add(R.string.home_recommend);
    }

    private void setTitle() {
        //源名称常显（不再定时隐藏）：有源时一直显示在品牌组合右侧，没配置源时直接留白，避免和品牌字标重复
        List<String> items = Arrays.asList(getHome().getName(), getConfig().getName());
        Optional<String> optional = items.stream().filter(s -> !TextUtils.isEmpty(s)).findFirst();
        if (optional.isPresent()) {
            mBinding.title.animate().cancel();
            mBinding.title.setAlpha(1f);
            mBinding.title.setText(optional.get());
            mBinding.title.setVisibility(View.VISIBLE);
        } else {
            mBinding.title.setVisibility(View.INVISIBLE);
        }
    }

    private void initConfig() {
        SpiderDebug.log("startup", "config load start cost=%sms", System.currentTimeMillis() - App.time());
        VodConfig.get().init().load(getCallback());
        WallConfig.get().init();
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                SpiderDebug.log("startup", "config load success cost=%sms", System.currentTimeMillis() - App.time());
                showContent();
            }

            @Override
            public void error(String msg) {
                SpiderDebug.log("startup", "config load error cost=%sms msg=%s", System.currentTimeMillis() - App.time(), msg);
                Notify.show(msg);
                showContent();
            }
        };
    }

    private void showContent() {
        SpiderDebug.log("startup", "home showContent start cost=%sms", System.currentTimeMillis() - App.time());
        mBinding.progressLayout.showContent();
        checkAction(getIntent());
        setTitle();
        setLogo();
        getHistory();
        getVideo();
        setFocus();
        SpiderDebug.log("startup", "home showContent end cost=%sms", System.currentTimeMillis() - App.time());
    }


    private void setFocus() {
        setFocus(0);
    }

    /**
     * 进入首页焦点应落在内容区（品牌组合只在用户主动上移时才聚焦）。
     * 第 0 页现在由 Fragment 挂载、内容又要等网络，因此在小窗口内反复重试，
     * 只有彻底落空才退到导航行——否则焦点会被系统默认分给品牌栏。
     */
    private void setFocus(int attempt) {
        mBinding.title.setSelected(true);
        View focus = getCurrentFocus();
        if (isInContent(focus) || isInTypeBar(focus)) return;
        if (mBinding.pager.getCurrentItem() != 0) return;
        //直接请求网格本身在 Leanback 里不一定会落到子项上，这里点名聚焦内容区（首张卡片）
        if (requestContentFocus()) return;
        if (attempt < 40) App.post(() -> setFocus(attempt + 1), 80);
        else requestHomeFocus();
    }

    private void getVideo() {
        mBinding.recycler.setVisibility(View.VISIBLE);
        mResult = Result.empty();
        mHomeResult = Result.empty();
        loadingHomeCategory = false;
        clearRecommendRows();
        mAdapter.add("progress");
        mViewModel.homeContent();
    }

    private void setTypes(Result result) {
        //「首页」是导航网格的第一项，没有分类时网格也要留着，否则首页入口会跟着消失
        boolean changed = isTypesChanged(result.getTypes());
        mTypeAdapter.addAll(result.getTypes());
        mBinding.typeRecycler.setVisibility(View.VISIBLE);
        //分类列表变了（首次加载 / 换源）才重建分页，避免普通刷新把用户正在看的页踢回首页
        if (changed && mBinding.pager.getAdapter() != null) resetPager();
    }

    private boolean isTypesChanged(List<Class> types) {
        int old = mTypeAdapter.size();
        if (old != (types == null ? 0 : types.size())) return true;
        for (int i = 0; i < old; i++) {
            if (!mTypeAdapter.get(i).getTypeId().equals(types.get(i).getTypeId())) return true;
        }
        return false;
    }

    private void addVideo(Result result) {
        App.post(this::settleFocus, 160);
        if (!loadingHomeCategory && result.getList().isEmpty() && !result.getTypes().isEmpty()) {
            Class type = result.getTypes().get(0);
            SpiderDebug.log("home", "home list empty, auto open first category key=%s tid=%s", getHome().getKey(), type.getTypeId());
            loadingHomeCategory = true;
            mAdapter.add("progress");
            mViewModel.categoryContent(getHome().getKey(), type.getTypeId(), "1", true, new java.util.HashMap<>());
            return;
        }
        loadingHomeCategory = false;
        Style style = result.getStyle(getHome().getStyle());
        if (style.isList()) mAdapter.addAll(mAdapter.size(), result.getList());
        else addGrid(result.getList(), style);
    }

    /**
     * 首页内容行会整批替换（历史行 / 推荐行 / 分页行），被替换掉的子项如果正持有焦点，
     * 系统会把焦点退回窗口里的第一个可聚焦控件（品牌栏）。这里在内容稳定后把它放回内容区；
     * 焦点已经在内容区或导航行时不动，避免抢走用户的位置。
     */
    /** 分类页内容尚未就绪时的聚焦重试（最多 5 次，约 0.75s） */
    private void retryContentFocus(int attempt) {
        if (isFinishing() || attempt > 5) return;
        App.post(() -> {
            FolderFragment fragment = getPageFragment(getPage());
            if (fragment != null && fragment.requestContentFocus()) return;
            retryContentFocus(attempt + 1);
        }, 150);
    }

    private void settleFocus() {
        if (isFinishing() || mBinding.pager.getCurrentItem() != 0) return;
        View focus = getCurrentFocus();
        if (isInContent(focus) || isInTypeBar(focus)) return;
        requestContentFocus();
    }

    private void clearRecommendRows() {
        mAdapter.remove("progress");
        int index = getRecommendIndex();
        if (mAdapter.size() > index) mAdapter.removeItems(index, mAdapter.size() - index);
    }

    private void addGrid(List<Vod> items, Style style) {
        List<ListRow> rows = new ArrayList<>();
        VodPresenter presenter = new VodPresenter(this, style);
        for (List<Vod> part : Lists.partition(items, Product.getColumn(style))) {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
            adapter.addAll(0, part);
            rows.add(new ListRow(adapter));
        }
        mAdapter.addAll(mAdapter.size(), rows);
    }

    private void getHistory() {
        getHistory(false);
    }

    private void getHistory(boolean renew) {
        List<History> items = History.get();
        int historyIndex = getHistoryIndex();
        int recommendIndex = getRecommendIndex();
        boolean exist = recommendIndex - historyIndex == 2;
        if (renew) mHistoryAdapter = new ArrayObjectAdapter(mPresenter = new HistoryPresenter(this));
        if ((items.isEmpty() && exist) || (renew && exist)) mAdapter.removeItems(historyIndex, 1);
        if ((!items.isEmpty() && !exist) || (renew && exist)) mAdapter.add(historyIndex, new ListRow(mHistoryAdapter));
        mHistoryAdapter.setItems(items, new BaseDiffCallback<History>());
    }

    private void setHistoryDelete(boolean delete) {
        mPresenter.setDelete(delete);
        mHistoryAdapter.notifyArrayItemRangeChanged(0, mHistoryAdapter.size());
    }

    private void clearHistory() {
        mAdapter.removeItems(getHistoryIndex(), 1);
        History.delete(VodConfig.getCid());
        mPresenter.setDelete(false);
        mHistoryAdapter.clear();
    }

    private int getHistoryIndex() {
        return mAdapter.indexOf(R.string.home_history) + 1;
    }

    private int getRecommendIndex() {
        return mAdapter.indexOf(R.string.home_recommend) + 1;
    }

    private void setLogo() {
        ImgUtil.logo(mBinding.logo);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        switch (event.type()) {
            case VOD:
                RefreshEvent.history();
                RefreshEvent.home();
                setLogo();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        switch (event.getType()) {
            case HOME:
                setTitle();
                SpiderDebug.log("site-dialog", "home refresh start key=%s", getHome().getKey());
                getVideo();
                SpiderDebug.log("site-dialog", "home refresh end key=%s", getHome().getKey());
                break;
            case HISTORY:
                getHistory();
                break;
            case SIZE:
                getVideo();
                getHistory(true);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        switch (event.type()) {
            case SEARCH:
                SearchActivity.start(this, event.text());
                break;
            case PUSH:
                VideoActivity.push(this, event.text());
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCastEvent(CastEvent event) {
        if (VodConfig.get().getConfig().equals(event.config())) {
            VideoActivity.cast(this, event.history().save(VodConfig.getCid()));
        } else {
            VodConfig.load(event.config(), getCallback(event));
        }
    }

    private Callback getCallback(CastEvent event) {
        return new Callback() {
            @Override
            public void success() {
                onCastEvent(event);
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
            }
        };
    }

    /**
     * 分类标签：横向移动焦点即自动切页（见 {@link #mTypeRunnable}），
     * 这里只负责「立刻切到该页」；如果它已经是当前页，则与分类页一致——展开/收起筛选。
     */
    @Override
    public void onItemClick(Class item) {
        int page = mTypeAdapter.indexOf(item) + 1;
        if (page <= 0) return;
        if (mBinding.pager.getCurrentItem() != page) {
            switchPage(page);
            return;
        }
        updateFilter(item, page);
    }

    /** 上键：直接切到该分类页（不再另开分类页） */
    @Override
    public void onRefresh(Class item) {
        int page = mTypeAdapter.indexOf(item) + 1;
        if (page > 0) switchPage(page);
    }

    private void updateFilter(Class item, int page) {
        FolderFragment fragment = getPageFragment(page);
        if (fragment == null) return;
        item.setFilter(!item.getFilter());
        fragment.toggleFilter(item.getFilter());
        mTypeAdapter.notifyItemRangeChanged(page, 1);
    }

    /** 首页的导航网格是 typeRecycler（分类页是 recycler） */
    @Override
    public int getTypeBarId() {
        return R.id.typeRecycler;
    }

    /** 收起当前分类已展开的筛选（进入下一级目录时由 FolderFragment 调用） */
    @Override
    public void closeFilter() {
        int page = getPage();
        if (page <= 0) return;
        Class item = mTypeAdapter.get(page - 1);
        if (item.getFilter()) updateFilter(item, page);
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.isAction()) mViewModel.action(getHome().getKey(), item.getAction());
        else if (getHome().isIndex()) CollectActivity.start(this, item.getName());
        else VideoActivity.start(this, getHome().getKey(), item.getId(), item.getName(), item.getPic());
    }

    @Override
    public boolean onLongClick(Vod item) {
        if (item.isAction()) return false;
        CollectActivity.start(this, item.getName());
        return true;
    }

    @Override
    public void onItemClick(History item) {
        VideoActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onItemDelete(History item) {
        mHistoryAdapter.remove(item.delete());
        if (mHistoryAdapter.size() > 0) return;
        mAdapter.removeItems(getHistoryIndex(), 1);
        mPresenter.setDelete(false);
    }

    @Override
    public boolean onLongClick() {
        if (mPresenter.isDelete()) clearHistory();
        else setHistoryDelete(true);
        return true;
    }

    @Override
    public void showDialog() {
        long start = System.currentTimeMillis();
        SpiderDebug.log("site-dialog", "open requested cost=%sms", System.currentTimeMillis() - App.time());
        SiteDialog.create().show(this);
        SpiderDebug.log("site-dialog", "show returned delay=%sms", System.currentTimeMillis() - start);
    }

    @Override
    public void onRefresh() {
        getVideo();
    }

    @Override
    public void reloadConfig() {
        VodConfig.get().clear().config(getConfig()).load(new Callback() {
            @Override
            public void start() {
                mBinding.progressLayout.showProgress();
            }

            @Override
            public void success() {
                showContent();
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
                showContent();
            }
        });
    }

    @Override
    public void setSite(Site item) {
        SpiderDebug.log("site-dialog", "set site key=%s name=%s", item.getKey(), item.getName());
        VodConfig.get().setHome(item);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyUtil.isMenuKey(event)) {
            showDialog();
            return true;
        }
        if (KeyUtil.isActionDown(event) & KeyUtil.isUpKey(event) && mBinding.typeRecycler.hasFocus()) return requestLogoFocus();
        //内容区向上：只有第一行才回到分类行，其余行交给 Leanback 在同一列内上移
        //（第 0 页才需要这段；分类页的上键回导航由 CustomVerticalGridView 自己处理）
        if (getPage() == 0 && KeyUtil.isActionDown(event) & KeyUtil.isUpKey(event) && isInContent(getCurrentFocus()) && isTopContentRow()) return requestHomeFocus();
        if (KeyUtil.isActionDown(event) & KeyUtil.isDownKey(event) && mBinding.typeRecycler.hasFocus()) return requestContentFocus();
        if (KeyUtil.isActionDown(event) & KeyUtil.isDownKey(event) && getCurrentFocus() == mBinding.logoBar) return requestHomeFocus();
        return super.dispatchKeyEvent(event);
    }

    private boolean requestLogoFocus() {
        return mBinding.logoBar.requestFocus();
    }

    /**
     * 把焦点交回分类行，落在「上次停留的那个标签」上。
     * 不能只调 typeRecycler.requestFocus()：网格本身在某些时刻（刚布局完、没有选中项）不接受焦点，
     * 返回 false 之后事件会落到系统的几何焦点搜索上，从内容区按上键就直接飞到右上角的工具栏图标了。
     * 因此这里点名聚焦选中位置的 itemView，并在一小段时间内重试。
     */
    private boolean requestHomeFocus() {
        return requestHomeFocus(0);
    }

    private boolean requestHomeFocus(int attempt) {
        int position = Math.max(mBinding.typeRecycler.getSelectedPosition(), 0);
        RecyclerView.ViewHolder holder = mBinding.typeRecycler.findViewHolderForAdapterPosition(position);
        if (holder != null && holder.itemView.requestFocus()) return true;
        if (mBinding.typeRecycler.requestFocus()) return true;
        if (attempt >= 4) return false;
        //标签还没布局完：先滚到它，下一帧再试
        mBinding.typeRecycler.scrollToPosition(position);
        App.post(() -> requestHomeFocus(attempt + 1), 40);
        return true;
    }

    /**
     * 当前聚焦的卡片是否位于内容区的第一行。
     * 只有第一行按上键才应该离开内容区、回到分类导航；其余行必须留在内容区内，
     * 由 Leanback 按几何位置在同一列向上移到上一行，否则任何一行按上都会跳到顶部导航。
     */
    private boolean isTopContentRow() {
        int first = getFirstContentRow();
        if (first < 0) return true;
        int current = getFocusedRowPosition();
        if (current < 0) current = mBinding.recycler.getSelectedPosition();
        return current <= first;
    }

    /** 内容区里第一个内容行的适配器位置：标题（String/Header）不算内容行，跳过。 */
    private int getFirstContentRow() {
        for (int i = 0; i < mAdapter.size(); i++) {
            if (mAdapter.get(i) instanceof ListRow) return i;
        }
        return -1;
    }

    /** 当前焦点所在的整行（VerticalGridView 的直接子项）在适配器中的位置。 */
    private int getFocusedRowPosition() {
        View view = getCurrentFocus();
        while (view != null && view.getParent() != mBinding.recycler) {
            ViewParent parent = view.getParent();
            if (!(parent instanceof View)) return -1;
            view = (View) parent;
        }
        return view == null ? -1 : mBinding.recycler.getChildAdapterPosition(view);
    }

    private boolean requestContentFocus() {
        int page = getPage();
        if (page > 0) {
            FolderFragment fragment = getPageFragment(page);
            if (fragment != null && fragment.requestContentFocus()) return true;
            //分类页是懒加载的：刚切过去、内容还在拉取时这一帧聚焦会落空。
            //吞掉按键并在后面几帧继续重试，避免事件落到系统焦点搜索上把焦点扔给工具栏。
            retryContentFocus(1);
            return true;
        }
        if (mBinding.recycler.getVisibility() != View.VISIBLE || mBinding.recycler.getChildCount() == 0) return false;
        View child = mBinding.recycler.getFocusedChild();
        if (child == null) child = mBinding.recycler.getChildAt(0);
        if (child != null && child.requestFocus()) return true;
        return focusFirstFocusable(mBinding.recycler);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mClock.start();
        if (LocalNetworkPermission.isGranted(this)) {
            NsdDeviceDiscovery.register();
            DLNARendererService.start(this);
        }
        if (mPendingHomeFocus) {
            mPendingHomeFocus = false;
            App.post(() -> focusHomeButton(0), 60);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mClock.stop();
        mExitPressedTime = 0;
    }

    @Override
    protected void onBackInvoked() {
        if (mBinding.progressLayout.isProgress()) {
            showContent();
        } else if (mPresenter.isDelete()) {
            setHistoryDelete(false);
        } else if (getPage() > 0) {
            //与分类页一致的顺序：先收筛选，再退回「首页」页
            Class item = mTypeAdapter.get(getPage() - 1);
            if (item.getFilter()) updateFilter(item, getPage());
            else backToHomePage();
        } else if (mBinding.recycler.getSelectedPosition() != 0) {
            mBinding.recycler.scrollToPosition(0);
        } else {
            onExitBack();
        }
    }

    /** 从分类页退回「首页」页，焦点落回「首页」标签 */
    private void backToHomePage() {
        mBinding.pager.setCurrentItem(0);
        mBinding.typeRecycler.setSelectedPosition(0);
        mBinding.typeRecycler.requestFocus();
    }

    private void onExitBack() {
        //连按两次返回键退出，不再弹确认对话框
        long now = System.currentTimeMillis();
        if (now - mExitPressedTime <= 2000) {
            mExitPressedTime = 0;
            confirmExit();
        } else {
            mExitPressedTime = now;
            Notify.show(R.string.exiting_tip);
        }
    }

    private void confirmExit() {
        if (PlaybackService.isRunning()) moveTaskToBack(true);
        else super.onBackInvoked();
    }

    /**
     * 分页适配器：第 0 页是「首页」页，第 1..N 页是各分类（FolderFragment）。
     * 与分类页的 PageAdapter 同一套写法，只是第一页换成首页内容。
     */
    class PageAdapter extends FragmentStatePagerAdapter {

        public PageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            if (position == 0) return new HomePageFragment();
            return FolderFragment.newInstance(getHome().getKey(), mTypeAdapter.get(position - 1));
        }

        @Override
        public int getCount() {
            //「首页」页 + 各分类页（TypeAdapter 尚未创建时也要能安全返回）
            return mTypeAdapter == null ? 1 : Math.max(mTypeAdapter.getItemCount() - 1, 0) + 1;
        }
    }

    @Override
    protected void onDestroy() {
        DLNARendererService.stop(this);
        VodConfig.get().clear();
        BackupManager.backup();
        OkHttp.get().clear();
        Source.get().exit();
        Server.get().stop();
        super.onDestroy();
    }

}
