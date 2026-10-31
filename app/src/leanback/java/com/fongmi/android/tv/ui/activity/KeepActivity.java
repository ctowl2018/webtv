package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.databinding.ActivityKeepBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.adapter.KeepAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.BrandToolbar;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Notify;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class KeepActivity extends BaseActivity implements KeepAdapter.OnClickListener {

    private ActivityKeepBinding mBinding;
    private BrandToolbar mBrand;
    private KeepAdapter mAdapter;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, KeepActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityKeepBinding.inflate(getLayoutInflater());
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

    @Override
    protected void initView(Bundle savedInstanceState) {
        mBrand = BrandToolbar.attach(this, mBinding.brand);
        setBrandTitle();
        setRecyclerView();
        getKeep();
    }

    /**
     * logo 右侧的「收藏」文字。
     * 直接复用品牌栏里的文字位（brandTitle）：它与首页的站点名称是同一套规格
     * （14sp / @color/home_source_name / 单行 marquee / 14dp 左间距 / weight 占满中间），
     * 因此字号、字重、颜色、字体、行高与首页完全一致；
     * 宽度不写死，右侧搜索/收藏/设置与时钟仍然贴行尾，任何屏宽下都不会换行或错位。
     */
    private void setBrandTitle() {
        mBinding.brand.brandTitle.setText(R.string.keep);
        mBinding.brand.brandTitle.setVisibility(View.VISIBLE);
        //首页的组合标（CustomLogoBar）自带 5dp 右内边距，这里补上同样的 5dp（14 + 5 = 19dp），
        //让「收藏」与首页站点名称落在同一个起始位置，而不是比它往左偏。
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mBinding.brand.brandTitle.getLayoutParams();
        params.setMarginStart(ResUtil.dp2px(19));
        mBinding.brand.brandTitle.setLayoutParams(params);
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setItemAnimator(null);
        mBinding.recycler.setAdapter(mAdapter = new KeepAdapter(this));
        mBinding.recycler.setLayoutManager(new GridLayoutManager(this, Product.getColumn()));
        mBinding.recycler.addItemDecoration(new SpaceItemDecoration(Product.getColumn(), 16));
    }

    private void getKeep() {
        mAdapter.setItems(Keep.getVod(), () -> mBinding.progressLayout.showContent(true, mAdapter.getItemCount()));
    }

    private void loadConfig(Config config, Keep item) {
        VodConfig.load(config, new Callback() {
            @Override
            public void start() {
                Notify.progress(getActivity());
            }

            @Override
            public void success() {
                Notify.dismiss();
                VideoActivity.start(getActivity(), item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
            }

            @Override
            public void error(String msg) {
                Notify.dismiss();
                Notify.show(msg);
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType() == RefreshEvent.Type.KEEP) getKeep();
    }

    @Override
    public void onItemClick(Keep item) {
        Config config = Config.find(item.getCid());
        if (config == null) CollectActivity.start(this, item.getVodName());
        else if (item.getCid() != VodConfig.getCid()) loadConfig(config, item);
        else VideoActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onItemDelete(Keep item) {
        mAdapter.remove(item.delete(), () -> {
            if (mAdapter.getItemCount() == 0) mAdapter.setDelete(false);
        });
    }

    @Override
    public boolean onLongClick() {
        mAdapter.setDelete(true);
        return true;
    }

    @Override
    protected void onBackInvoked() {
        if (mAdapter.isDelete()) mAdapter.setDelete(false);
        else super.onBackInvoked();
    }
}
