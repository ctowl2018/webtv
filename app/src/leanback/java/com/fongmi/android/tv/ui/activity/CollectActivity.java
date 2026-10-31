package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Collect;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityCollectBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.setting.SiteHealthStore;
import com.fongmi.android.tv.ui.adapter.CollectAdapter;
import com.fongmi.android.tv.ui.adapter.SearchAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.BrandToolbar;
import com.fongmi.android.tv.ui.custom.CustomScroller;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.crawler.SpiderDebug;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CollectActivity extends BaseActivity implements CollectAdapter.OnClickListener, SearchAdapter.OnClickListener, CustomScroller.Callback {
    /** 左侧源/全部列表宽度（dp），与 activity_collect.xml 中 collect 的宽度一致 */
    private static final int SOURCE_LIST_WIDTH_DP = 196;
    /** 海报网格与左侧源列表之间的留白（dp） */
    private static final int GRID_START_DP = 16;
    /** 海报网格右侧留白（dp），与首页/分类页 24dp 对齐 */
    private static final int GRID_END_DP = 24;
    /** 海报列间距（dp），与首页/分类页一致 */
    private static final int GRID_SPACING_DP = 16;

    private BrandToolbar mBrand;

    private ActivityCollectBinding mBinding;
    private CollectAdapter mCollectAdapter;
    private SearchAdapter mSearchAdapter;
    private CustomScroller mScroller;
    private SiteViewModel mViewModel;
    private RecyclerView.OnScrollListener mImageScrollListener;
    private List<Site> mSites;
    private final List<Vod> mPendingItems = new ArrayList<>();
    private Runnable mApplyCollect;
    private int mPendingCollectPosition = RecyclerView.NO_POSITION;
    private boolean mScrolling;
    private boolean mLeavingForPlayback;

    public static void start(Activity activity, String keyword) {
        start(activity, keyword, null);
    }

    public static void start(Activity activity, String keyword, String siteKey) {
        Intent intent = new Intent(activity, CollectActivity.class);
        intent.putExtra("keyword", keyword);
        intent.putExtra("siteKey", siteKey);
        activity.startActivity(intent);
    }

    private String getKeyword() {
        return Objects.toString(getIntent().getStringExtra("keyword"), "");
    }

    private String getSiteKey() {
        return Objects.toString(getIntent().getStringExtra("siteKey"), "");
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityCollectBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getIntent().putExtras(intent);
        if (mViewModel != null) mViewModel.stopSearch();
        saveKeyword();
        search();
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mBrand = BrandToolbar.attach(this, mBinding.brand);
        setRecyclerView();
        setViewModel();
        saveKeyword();
        setSites();
        search();
    }

    private void setRecyclerView() {
        int count = getCount();
        //先按海报宽度收敛左侧站点列表宽度，再建网格
        getSourceWidth();
        mScroller = new CustomScroller(this);
        mBinding.collect.setHasFixedSize(true);
        mBinding.collect.setItemAnimator(null);
        mBinding.collect.setVerticalSpacing(ResUtil.dp2px(12));
        mBinding.collect.setAdapter(mCollectAdapter = new CollectAdapter(this));
        mBinding.collect.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                scheduleCollect(position, 260);
            }
        });
        mBinding.recycler.setClipChildren(false);
        mBinding.recycler.setClipToPadding(false);
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setItemAnimator(null);
        mBinding.recycler.setItemViewCacheSize(count * 3);
        mBinding.recycler.setLayoutManager(new GridLayoutManager(this, count));
        mBinding.recycler.addItemDecoration(new SpaceItemDecoration(count, GRID_SPACING_DP));
        mBinding.recycler.addOnScrollListener(mScroller);
        mBinding.recycler.addOnScrollListener(mImageScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (!canLoadImage()) return;
                ensureSearchRows(count, 2);
                preloadNextRows(count);
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (!canLoadImage()) return;
                boolean scrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
                if (scrolling == mScrolling) return;
                mScrolling = scrolling;
                if (mScrolling) {
                    ensureSearchRows(count, 2);
                    preloadNextRows(count);
                } else {
                    Glide.with(CollectActivity.this).resumeRequests();
                    flushPendingItems();
                    ensureSearchRows(count, 2);
                    preloadNextRows(count);
                }
            }
        });
        //卡片物理尺寸与首页/分类页完全一致：直接取 Product 的行列规格，不再按当前列宽反算
        int[] spec = Product.getSpec(Style.rect());
        mBinding.recycler.setAdapter(mSearchAdapter = new SearchAdapter(this, spec[0], spec[1]));
    }

    private boolean canLoadImage() {
        return !isFinishing() && !isDestroyed();
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class).init();
        mViewModel.getSearch().observe(this, this::setCollect);
        mViewModel.getResult().observe(this, this::setSearch);
    }

    private void saveKeyword() {
        List<String> items = Setting.getKeyword().isEmpty() ? new ArrayList<>() : App.gson().fromJson(Setting.getKeyword(), new TypeToken<List<String>>() {}.getType());
        items.remove(getKeyword());
        items.add(0, getKeyword());
        if (items.size() > 9) items.remove(9);
        Setting.putKeyword(App.gson().toJson(items));
    }

    private void setSites() {
        String siteKey = getSiteKey();
        mSites = new ArrayList<>();
        for (Site site : VodConfig.get().getSites()) {
            if (!site.isSearchable()) continue;
            if (!siteKey.isEmpty() && !site.getKey().equals(siteKey)) continue;
            mSites.add(site);
        }
        SiteHealthStore.sortSites(mSites);
    }

    private void search() {
        removeApplyCollect();
        mCollectAdapter.clear();
        mSearchAdapter.clear();
        mPendingItems.clear();
        mScroller.reset();
        //文案放在品牌工具栏里（首页源名称的位置/样式），不再单独占一行
        mBinding.brand.brandTitle.setText(getString(R.string.collect_result, getKeyword()));
        mBinding.brand.brandTitle.setVisibility(View.VISIBLE);
        if (mSites.isEmpty()) return;
        mCollectAdapter.add(Collect.all());
        mViewModel.searchContent(mSites, getKeyword(), false);
    }

    private int getCount() {
        //列数恒为「首页列数 - 1」：左边多了一列站点名，右侧海报区按海报自身的物理尺寸排布
        return Math.max(1, Product.getColumn(Style.rect()) - 1);
    }

    private int getItemWidth(int count) {
        //可用宽度 = 屏宽 - 源列表 - 网格左侧留白 - 右侧留白；再扣掉 (count-1) 个 16dp 的列间距，
        //保证 GridLayoutManager 每格的剩余宽度正好等于 SpaceItemDecoration 的间距，不产生额外的散缝。
        int available = ResUtil.getScreenWidth() - getSourceWidth() - ResUtil.dp2px(GRID_START_DP + GRID_END_DP);
        int spacing = ResUtil.dp2px(GRID_SPACING_DP) * (count - 1);
        return (available - spacing) / count;
    }

    /** 左侧站点列表宽度：不超过一个海报的宽度 */
    private int getSourceWidth() {
        int spec = Product.getSpec(Style.rect())[0];
        int width = Math.min(ResUtil.dp2px(SOURCE_LIST_WIDTH_DP), spec);
        if (mBinding != null) {
            android.view.ViewGroup.LayoutParams params = mBinding.collect.getLayoutParams();
            if (params.width != width) {
                params.width = width;
                mBinding.collect.setLayoutParams(params);
            }
        }
        return width;
    }

    private int getItemHeight(int count) {
        return (int) (getItemWidth(count) / 0.72f);
    }

    private void setCollect(Result result) {
        if (mLeavingForPlayback) return;
        if (result == null || result.getList().isEmpty()) return;
        mCollectAdapter.add(Collect.create(result.getList()));
        mCollectAdapter.add(result.getList());
        if (mCollectAdapter.getPosition() == 0) addSearchItems(result.getList());
    }

    private void setSearch(Result result) {
        if (mLeavingForPlayback) return;
        if (result == null) return;
        mScroller.endLoading(result);
        Collect activated = mCollectAdapter.getActivated();
        boolean same = !result.getList().isEmpty() && activated.getSite().equals(result.getVod().getSite());
        if (same) activated.getList().addAll(result.getList());
        if (same) addSearchItems(result.getList());
    }

    private void addSearchItems(List<Vod> items) {
        if (mScrolling) mPendingItems.addAll(items);
        else mSearchAdapter.appendSource(items, getCount() * 4);
    }

    private void flushPendingItems() {
        if (mPendingItems.isEmpty()) return;
        mSearchAdapter.appendSource(new ArrayList<>(mPendingItems), getCount() * 4);
        mPendingItems.clear();
    }

    private void preloadNextRows(int count) {
        RecyclerView.LayoutManager manager = mBinding.recycler.getLayoutManager();
        if (!(manager instanceof GridLayoutManager layoutManager)) return;
        int first = layoutManager.findFirstVisibleItemPosition();
        int last = layoutManager.findLastVisibleItemPosition();
        if (first < 0 || last < 0) return;
        int direction = last >= first ? last + 1 : first + 1;
        mSearchAdapter.ensureLoaded(direction, count * 2);
    }

    private void ensureSearchRows(int count, int rows) {
        RecyclerView.LayoutManager manager = mBinding.recycler.getLayoutManager();
        if (!(manager instanceof GridLayoutManager layoutManager)) return;
        int last = layoutManager.findLastVisibleItemPosition();
        if (last < 0) return;
        mSearchAdapter.ensureLoaded(last + 1, count * rows);
    }

    @Override
    public void onItemClick(int position, Collect item) {
        scheduleCollect(position, 0);
    }

    private void scheduleCollect(int position, long delayMillis) {
        if (position < 0 || position >= mCollectAdapter.getItemCount()) return;
        Collect item = mCollectAdapter.get(position);
        boolean same = mCollectAdapter.getPosition() == position;
        mCollectAdapter.setSelected(position);
        mScroller.reset();
        mScroller.setPage(item.getPage());
        mPendingItems.clear();
        if (same && delayMillis > 0 && mPendingCollectPosition == position) return;
        applyCollectDeferred(position, item, delayMillis);
    }

    private void applyCollectDeferred(int position, Collect item, long delayMillis) {
        removeApplyCollect();
        mPendingCollectPosition = position;
        mApplyCollect = () -> {
            if (isFinishing() || isDestroyed()) return;
            if (mCollectAdapter.getPosition() != position) return;
            setSearchItemsLazy(new ArrayList<>(item.getList()));
        };
        App.post(mApplyCollect, delayMillis);
    }

    private void removeApplyCollect() {
        if (mApplyCollect != null) App.removeCallbacks(mApplyCollect);
        mApplyCollect = null;
        mPendingCollectPosition = RecyclerView.NO_POSITION;
    }

    private void setSearchItemsLazy(List<Vod> items) {
        mBinding.recycler.scrollToPosition(0);
        mSearchAdapter.setSource(items, getCount() * 4);
        mBinding.recycler.post(() -> {
            ensureSearchRows(getCount(), 2);
            preloadNextRows(getCount());
        });
    }

    @Override
    public void onItemClick(Vod item) {
        long start = System.currentTimeMillis();
        setResult(Activity.RESULT_OK);
        mLeavingForPlayback = true;
        removeApplyCollect();
        SpiderDebug.log("collect-flow", "item click site=%s id=%s name=%s folder=%s", item.getSiteKey(), item.getId(), item.getName(), item.isFolder());
        if (item.isFolder()) {
            VodActivity.start(this, item.getSiteKey(), Result.folder(item));
        } else {
            VideoActivity.collect(this, item.getSiteKey(), item.getId(), item.getName(), item.getPic());
        }
        SpiderDebug.log("collect-flow", "activity launch requested cost=%dms", System.currentTimeMillis() - start);
        App.post(() -> {
            long cleanup = System.currentTimeMillis();
            if (mViewModel != null) mViewModel.stopSearch();
            mPendingItems.clear();
            if (canLoadImage()) Glide.with(this).pauseRequests();
            SpiderDebug.log("collect-flow", "leave cleanup cost=%dms", System.currentTimeMillis() - cleanup);
        }, 200);
    }

    @Override
    public boolean onItemKey(int position, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN || position < 0) return false;
        int count = getCount();
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) return onSearchDown(position, count);
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) return position < count;
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) return position % count == count - 1;
        return false;
    }

    private boolean onSearchDown(int position, int count) {
        int next = position + count;
        if (next + count >= mSearchAdapter.getItemCount()) flushPendingItems();
        mSearchAdapter.ensureLoaded(next + 1, count * 3);
        boolean bottom = next >= mSearchAdapter.getItemCount();
        if (bottom) mScroller.checkMore();
        return bottom;
    }

    @Override
    public boolean onLoadMore(String page) {
        Collect activated = mCollectAdapter.getActivated();
        if ("all".equals(activated.getSite().getKey())) return false;
        mViewModel.searchContent(activated.getSite(), getKeyword(), false, page);
        activated.setPage(Integer.parseInt(page));
        return true;
    }

    @Override
    protected void onBackInvoked() {
        removeApplyCollect();
        mViewModel.stopSearch();
        super.onBackInvoked();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBrand.start();
        mLeavingForPlayback = false;
        if (canLoadImage()) Glide.with(this).resumeRequests();
    }

    @Override
    protected void onDestroy() {
        if (mBrand != null) mBrand.stop();
        if (mBinding != null) {
            mBinding.recycler.removeOnScrollListener(mScroller);
            if (mImageScrollListener != null) mBinding.recycler.removeOnScrollListener(mImageScrollListener);
        }
        if (mViewModel != null) mViewModel.stopSearch();
        removeApplyCollect();
        mPendingItems.clear();
        SiteHealthStore.flush();
        super.onDestroy();
    }
}
