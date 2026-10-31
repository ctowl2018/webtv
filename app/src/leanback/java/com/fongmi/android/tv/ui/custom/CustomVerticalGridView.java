package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.BaseGridView;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.KeyUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CustomVerticalGridView extends VerticalGridView {

    private List<View> views;
    private boolean pressDown;
    private boolean pressUp;
    private boolean moveTop;
    //列表顶部“非内容行”的行数：分类页过滤器展开时等于过滤器行数，默认 0
    private int headerOffset;

    public CustomVerticalGridView(@NonNull Context context) {
        this(context, null);
    }

    public CustomVerticalGridView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomVerticalGridView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setMoveTop(true);
        //海报聚焦会放大，放大后的边缘会超出列表内容区；这里在代码里再兜一次，
        //避免框架/父容器把裁剪重新打开，导致第一行海报的圆角顶部被切平。
        setClipChildren(false);
        setClipToPadding(false);
    }

    @Override
    protected void initAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
        super.initAttributes(context, attrs);
        setOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable ViewHolder child, int position, int subposition) {
                //导航栏常驻：竖向浏览内容时不再自动收起（需求要求导航位置固定）。
                //这里只在回到首行时兜底恢复一次显示，防止历史状态把它留在 GONE。
                if (pressUp && position <= headerOffset) showHeader();
            }
        });
    }

    /** 导航里应当被聚焦的位置（由宿主页面告知，避免依赖标签网格自身的 selectedPosition 丢失） */
    private int headerSelectedPosition = RecyclerView.NO_POSITION;

    /** 内容区最后一次聚焦的控件：从导航按「下」时回到这里，而不是永远回到第一张海报 */
    private View lastFocus;

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus, newFocus) -> {
            if (oldFocus != null && findContainingViewHolder(oldFocus) != null) lastFocus = oldFocus;
        });
    }

    /** 回到内容区最后一次聚焦的海报；没有记录或已不可用时返回 false，由调用方兜底 */
    public boolean requestLastFocus() {
        View target = lastFocus;
        if (target == null || !target.isAttachedToWindow() || target.getVisibility() != View.VISIBLE) return false;
        return target.requestFocus();
    }

    /** 告知导航当前应聚焦第几项（分类页 = 当前页下标 + 首页偏移） */
    public void setHeaderSelectedPosition(int position) {
        headerSelectedPosition = position;
    }

    /** 列表顶部属于“非内容行”的行数（过滤器展开时 = 过滤器行数），默认 0 */
    public void setHeaderOffset(int offset) {
        headerOffset = Math.max(0, offset);
    }

    public void setHeader(FragmentActivity activity, int... layoutIds) {
        if (activity == null) return;
        views = Arrays.stream(layoutIds).mapToObj(id -> (View) activity.findViewById(id)).filter(Objects::nonNull).toList();
        //自己记一份导航的选中位置：标签网格一旦失焦，自身的 selectedPosition 会变成 NO_POSITION，
        //返回键回导航时就不知道该落到哪一项了。
        for (View view : views) {
            if (view instanceof BaseGridView) ((BaseGridView) view).addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
                @Override
                public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable ViewHolder child, int position, int subposition) {
                    if (position != RecyclerView.NO_POSITION) headerSelectedPosition = position;
                }
            });
        }
    }

    public void setMoveTop(boolean moveTop) {
        this.moveTop = moveTop;
    }

    public void hideHeader() {
        if (views != null) for (View view : views) view.setVisibility(View.GONE);
    }

    public void showHeader() {
        if (views != null) for (View view : views) view.setVisibility(View.VISIBLE);
    }

    public boolean isHeaderVisible() {
        if (views == null) return false;
        //不写死 R.id.recycler：同一份分类内容可能挂在分类页（recycler）或首页（typeRecycler）
        for (View view : views) if (view.getVisibility() == View.VISIBLE) return true;
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (!KeyUtil.isActionDown(event)) return super.dispatchKeyEvent(event);
        pressUp = KeyUtil.isUpKey(event);
        pressDown = KeyUtil.isDownKey(event);
        //首行按上：把焦点交回顶部导航，并且是导航里「当前选中的那一项」。
        //不能交给系统的几何焦点搜索：标签常挤在左侧、海报铺到最右侧，正上方没有可聚焦控件时，
        //搜索会一路找到右上角的搜索/收藏/设置图标，把整条分类导航跳过去。
        if (pressUp && moveTop && isInFirstRow() && focusHeaderSelection()) return true;
        return super.dispatchKeyEvent(event);
    }

    /**
     * 是否位于列表「最顶上那一行」。
     * 注意这里必须是 position == 0 而不是 position <= headerOffset：
     * 过滤器展开时 headerOffset>0，第一条内容行的位置正好等于 headerOffset，
     * 若按 <= 判定，从海报按上键会被直接抢到导航栏，跳过中间的二级过滤器行。
     */
    private boolean isInFirstRow() {
        View child = getFocusedChild();
        if (child == null) return false;
        return getChildAdapterPosition(child) == 0;
    }

    /**
     * 把焦点交回顶部导航的「选中项」。
     * 不能只对导航容器 requestFocus()——那样焦点落在容器上，没有任何标签高亮，看起来像焦点丢了。
     */
    private boolean focusHeaderSelection() {
        if (views == null) return false;
        showHeader();
        for (View view : views) {
            if (!(view instanceof BaseGridView)) continue;
            if (focusSelectedChild((BaseGridView) view)) return true;
            //导航刚由 GONE 变 VISIBLE 时还没布局，下一帧再试
            view.post(() -> focusSelectedChild((BaseGridView) view));
            return true;
        }
        return false;
    }

    private boolean focusSelectedChild(@NonNull BaseGridView grid) {
        //优先用宿主告知的位置；标签网格失去焦点后自身的 selectedPosition 可能已变成 NO_POSITION
        int position = headerSelectedPosition;
        if (position == RecyclerView.NO_POSITION) position = grid.getSelectedPosition();
        if (position == RecyclerView.NO_POSITION) position = 0;
        RecyclerView.ViewHolder holder = grid.findViewHolderForAdapterPosition(position);
        return holder != null && holder.itemView.requestFocus();
    }

    public boolean moveToTop() {
        if (views == null || getSelectedPosition() == 0 || getAdapter() == null || getAdapter().getItemCount() == 0) return false;
        for (View view : views) if (view.getId() == R.id.recycler) view.requestFocus();
        scrollToPosition(0);
        showHeader();
        return true;
    }
}
