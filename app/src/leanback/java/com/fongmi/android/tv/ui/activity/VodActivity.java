package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.ViewPager;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.databinding.ActivityVodBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.adapter.TypeAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.base.TypeHost;
import com.fongmi.android.tv.ui.custom.BrandToolbar;
import com.fongmi.android.tv.ui.fragment.FolderFragment;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.ResUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Optional;

public class VodActivity extends BaseActivity implements TypeAdapter.OnClickListener, TypeHost {

    //分类标签栏第一位是「首页」项，分类标签的位置 = 分类下标 + TYPE_OFFSET
    private static final int TYPE_OFFSET = 1;

    private ActivityVodBinding mBinding;
    private TypeAdapter mAdapter;
    private BrandToolbar mBrand;
    private View mOldView;

    public static void start(Activity activity, Result result) {
        start(activity, VodConfig.get().getHome().getKey(), result);
    }

    public static void start(Activity activity, String key, Result result) {
        start(activity, key, result, 0);
    }

    public static void start(Activity activity, String key, Result result, int position) {
        if (result == null || result.getTypes().isEmpty()) return;
        Intent intent = new Intent(activity, VodActivity.class);
        intent.putExtra("key", key);
        intent.putExtra("result", result);
        intent.putExtra("position", Math.max(position, 0));
        activity.startActivity(intent);
    }

    private String getKey() {
        return getIntent().getStringExtra("key");
    }

    private Result getResult() {
        return getIntent().getParcelableExtra("result");
    }

    private int getPosition() {
        return Math.min(getIntent().getIntExtra("position", 0), Math.max(mAdapter.getItemCount() - TYPE_OFFSET - 1, 0));
    }

    private Class getType() {
        return mAdapter.get(mBinding.pager.getCurrentItem());
    }

    private FolderFragment getFragment() {
        return (FolderFragment) mBinding.pager.getAdapter().instantiateItem(mBinding.pager, mBinding.pager.getCurrentItem());
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityVodBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mBrand = BrandToolbar.attach(this, mBinding.brand);
        setRecyclerView();
        setTypes();
        setPager();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBrand.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBrand.stop();
    }

    private void goHome() {
        //CLEAR_TOP 复用已在栈底的首页实例，避免层层返回；回到首页后焦点落在「首页」按钮上
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(HomeActivity.EXTRA_FOCUS_HOME, true);
        startActivity(intent);
        finish();
    }

    @Override
    protected void initEvent() {
        mBinding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                syncSelectedType(position);
            }
        });
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                onChildSelected(child);
            }
        });
    }

    /**
     * 翻页 → 标签选中：只在两者不一致时才写回，且只有焦点本来就在标签栏里才抢焦点。
     * 原实现无条件 setSelectedPosition + requestFocus，会和 {@link #mRunnable}（标签 → 翻页）
     * 互相触发形成回环，表现为“没按遥控器，选中的标签却自己往左挪”。
     */
    private void syncSelectedType(int position) {
        int target = position + TYPE_OFFSET;
        if (mBinding.recycler.getSelectedPosition() != target) mBinding.recycler.setSelectedPosition(target);
        getFragment().setNavSelected(target);
        if (isInRecycler(getCurrentFocus())) mBinding.recycler.requestFocus();
    }

    private void setRecyclerView() {
        mBinding.recycler.setHorizontalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        //withHome=true：首页入口并进同一个标签网格；upRefresh=false：标签不再响应上键刷新
        mBinding.recycler.setAdapter(mAdapter = new TypeAdapter(this, true, false));
    }

    private void setTypes() {
        mAdapter.addAll(getResult().getTypes());
    }

    private void setPager() {
        mBinding.pager.setAdapter(new PageAdapter(getSupportFragmentManager()));
        mBinding.pager.setCurrentItem(getPosition());
        mBinding.recycler.setSelectedPosition(getPosition() + TYPE_OFFSET);
        requestTypeFocus(0);
    }

    /**
     * 进入分类页时焦点必须落在分类标签上：initView 阶段分类项还没布局完成，
     * 单次 requestFocus 容易落空，因此在短时间窗口内重试，直到焦点进入分类行。
     */
    private void requestTypeFocus(int attempt) {
        if (isFinishing() || attempt > 8) return;
        if (isInRecycler(getCurrentFocus())) return;
        mBinding.recycler.requestFocus();
        if (isInRecycler(getCurrentFocus())) return;
        App.post(() -> requestTypeFocus(attempt + 1), 60);
    }

    private boolean isInRecycler(View view) {
        while (view != null) {
            if (view == mBinding.recycler) return true;
            ViewParent parent = view.getParent();
            view = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    private void onChildSelected(@Nullable RecyclerView.ViewHolder child) {
        if (mOldView != null) mOldView.setSelected(false);
        if ((mOldView = child != null ? child.itemView : null) == null) return;
        mOldView.setSelected(true);
        App.removeCallbacks(mRunnable);
        App.post(mRunnable, 100);
    }

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            //标签 → 翻页：同样只在目标页不同时才切，避免回环
            int selected = mBinding.recycler.getSelectedPosition() - TYPE_OFFSET;
            if (selected >= 0 && mBinding.pager.getCurrentItem() != selected) mBinding.pager.setCurrentItem(selected);
        }
    };

    private boolean isFilterVisible() {
        return Optional.ofNullable(getType()).map(Class::getFilter).orElse(false);
    }

    private void updateFilter() {
        Optional.ofNullable(getType()).ifPresent(this::updateFilter);
    }

    private void updateFilter(Class item) {
        item.setFilter(!item.getFilter());
        getFragment().toggleFilter(item.getFilter());
        mAdapter.notifyItemRangeChanged(mAdapter.indexOf(item) + TYPE_OFFSET, 1);
    }

    /** 分类页的导航网格就是布局里的 recycler */
    @Override
    public int getTypeBarId() {
        return R.id.recycler;
    }

    @Override
    public void closeFilter() {
        if (isFilterVisible()) updateFilter();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType() == RefreshEvent.Type.CATEGORY) getFragment().onRefresh();
    }

    @Override
    public void onItemClick(Class item) {
        updateFilter(item);
    }

    @Override
    public void onRefresh(Class item) {
        getFragment().onRefresh();
    }

    /** 分类页的分类行不含「首页」项（withHome=false），这里仅满足接口；保留兜底行为 */
    @Override
    public void onHomeClick() {
        goHome();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyUtil.isMenuKey(event)) updateFilter();
        if (KeyUtil.isActionDown(event) && KeyUtil.isDownKey(event) && mBinding.recycler.hasFocus()) return requestContentFocus();
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        //窗口首次拿到焦点后系统才做初始焦点分配，这里再兜底一次，避免焦点被其它控件截走
        if (hasWindowFocus && !isInRecycler(getCurrentFocus())) App.post(() -> requestTypeFocus(0), 80);
    }

    private boolean requestContentFocus() {
        FolderFragment fragment = getFragment();
        return fragment != null && fragment.requestContentFocus();
    }

    @Override
    protected void onBackInvoked() {
        //焦点在海报区：一次返回先把焦点交回当前选中的导航项，再按一次才继续（关过滤器/退上级/退出页面）
        if (isInContent(getCurrentFocus())) {
            focusSelectedType();
            return;
        }
        if (isFilterVisible()) updateFilter();
        else if (getFragment().canBack()) getFragment().goBack();
        else super.onBackInvoked();
    }

    /** 焦点是否在右侧内容区（不在导航标签栏、也不在顶部工具栏） */
    private boolean isInContent(View focus) {
        if (focus == null) return false;
        FolderFragment fragment = getFragment();
        return fragment != null && fragment.isInContent(focus);
    }

    /** 把焦点交回导航标签栏里「当前页对应的那一项」 */
    private void focusSelectedType() {
        int target = mBinding.pager.getCurrentItem() + TYPE_OFFSET;
        mBinding.recycler.setVisibility(View.VISIBLE);
        if (mBinding.recycler.getSelectedPosition() != target) mBinding.recycler.setSelectedPosition(target);
        focusTypeItem(target, 0);
    }

    private void focusTypeItem(int position, int attempt) {
        if (isFinishing() || attempt > 6) return;
        RecyclerView.ViewHolder holder = mBinding.recycler.findViewHolderForAdapterPosition(position);
        if (holder != null && holder.itemView.requestFocus()) return;
        mBinding.recycler.scrollToPosition(position);
        App.post(() -> focusTypeItem(position, attempt + 1), 40);
    }

    class PageAdapter extends FragmentStatePagerAdapter {

        public PageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Class type = mAdapter.get(position);
            return FolderFragment.newInstance(getKey(), type);
        }

        @Override
        public int getCount() {
            //网格里多了「首页」项，页数仍按分类数计算
            return Math.max(mAdapter.getItemCount() - TYPE_OFFSET, 0);
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        }
    }
}
