package com.fongmi.android.tv.ui.custom;

import android.annotation.SuppressLint;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.RowPresenter;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.utils.ResUtil;

public class CustomRowPresenter extends ListRowPresenter {

    private final int spacing;
    private final int strategy;

    public CustomRowPresenter(int spacing) {
        this(spacing, FocusHighlight.ZOOM_FACTOR_SMALL);
    }

    @SuppressLint("RestrictedApi")
    public CustomRowPresenter(int spacing, int focusZoomFactor) {
        this(spacing, focusZoomFactor, HorizontalGridView.FOCUS_SCROLL_ITEM);
    }

    public CustomRowPresenter(int spacing, int focusZoomFactor, int strategy) {
        super(focusZoomFactor);
        this.spacing = spacing;
        this.strategy = strategy;
        setShadowEnabled(false);
        setSelectEffectEnabled(false);
        setKeepChildForeground(false);
    }

    @Override
    @SuppressLint("RestrictedApi")
    protected void initializeRowViewHolder(RowPresenter.ViewHolder holder) {
        super.initializeRowViewHolder(holder);
        ViewHolder vh = (ViewHolder) holder;
        vh.getGridView().setFocusScrollStrategy(strategy);
        vh.getGridView().setHorizontalSpacing(ResUtil.dp2px(spacing));
        //海报聚焦是整卡放大，放大后的上下边缘会超出这一行的边界。
        //默认 clipChildren=true 会把第一行海报的圆角顶部直接切掉（看起来像被导航栏遮住），这里放开裁剪。
        vh.getGridView().setClipChildren(false);
        vh.getGridView().setClipToPadding(false);
        //从行网格一路向上放开裁剪，直到列表本身：中间任何一层裁剪都会把放大的海报切掉
        ViewParent parent = vh.getGridView().getParent();
        while (parent instanceof ViewGroup && !(parent instanceof RecyclerView)) {
            ((ViewGroup) parent).setClipChildren(false);
            ((ViewGroup) parent).setClipToPadding(false);
            parent = parent.getParent();
        }
    }
}