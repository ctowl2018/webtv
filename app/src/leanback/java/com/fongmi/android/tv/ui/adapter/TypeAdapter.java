package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Cache;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.databinding.AdapterTypeBinding;
import com.fongmi.android.tv.databinding.AdapterTypeHomeBinding;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类标签适配器。withHome=true 时在列表首位插入「首页」项，
 * 让它和分类标签落在同一个 HorizontalGridView 里——同一个容器、同一套间距与行高，
 * 不会再出现「首页」悬在网格外面、滚动时只有分类在走的情况。
 * <p>
 * 「首页」是视图类型上的头项，不进 mItems，所以 get()/indexOf() 仍然只按分类下标工作，
 * 分类页（VodActivity）那种按位置传参的逻辑不受影响（该页用 withHome=false）。
 */
public class TypeAdapter extends RecyclerView.Adapter<TypeAdapter.ViewHolder> {

    private static final int TYPE_HOME = 0;
    private static final int TYPE_CLASS = 1;

    private final OnClickListener mListener;
    private final List<Class> mItems;
    private final boolean withHome;
    //分类标签上按「上键」是否触发刷新：首页需要（上键=进入该分类），分类页不需要
    private final boolean upRefresh;

    public TypeAdapter(OnClickListener listener) {
        this(listener, false, true);
    }

    public TypeAdapter(OnClickListener listener, boolean withHome) {
        this(listener, withHome, true);
    }

    public TypeAdapter(OnClickListener listener, boolean withHome, boolean upRefresh) {
        mListener = listener;
        mItems = new ArrayList<>();
        this.withHome = withHome;
        this.upRefresh = upRefresh;
    }

    public void addAll(List<Class> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public Class get(int position) {
        return mItems.get(position);
    }

    /** 分类数量（不含「首页」项） */
    public int size() {
        return mItems.size();
    }

    public int indexOf(Class item) {
        return mItems.indexOf(item);
    }

    private int offset() {
        return withHome ? 1 : 0;
    }

    @Override
    public int getItemCount() {
        return mItems.size() + offset();
    }

    @Override
    public int getItemViewType(int position) {
        return withHome && position == 0 ? TYPE_HOME : TYPE_CLASS;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HOME) return new ViewHolder(AdapterTypeHomeBinding.inflate(inflater, parent, false));
        return new ViewHolder(AdapterTypeBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HOME) {
            holder.home.getRoot().setNextFocusDownId(R.id.recycler);
            holder.home.getRoot().setOnClickListener(v -> mListener.onHomeClick());
            return;
        }
        Class item = mItems.get(position - offset());
        holder.binding.text.setText(item.getTypeName());
        holder.binding.getRoot().setSelected(item.isSelected());
        holder.binding.getRoot().setNextFocusUpId(R.id.title);
        holder.binding.getRoot().setNextFocusDownId(R.id.recycler);
        holder.binding.text.setCompoundDrawablePadding(ResUtil.dp2px(4));
        holder.binding.text.setCompoundDrawablesWithIntrinsicBounds(0, 0, getIcon(item), 0);
        //upRefresh=true：上键=刷新（首页用它进入该分类）。
        //upRefresh=false：给一个空监听，上键仍被标签吞掉但不做任何事 ——
        //既不会刷新页面，也不会因为焦点离开标签而让 Pager 跟着切到相邻分类。
        holder.binding.text.setListener(upRefresh ? () -> mListener.onRefresh(item) : () -> {
        });
        holder.binding.getRoot().setOnClickListener(v -> mListener.onItemClick(item));
    }

    private int getIcon(Class item) {
        return Cache.get(item).isEmpty() ? 0 : item.getFilter() ? R.drawable.ic_vod_filter_off : R.drawable.ic_vod_filter_on;
    }

    public interface OnClickListener {

        void onItemClick(Class item);

        void onRefresh(Class item);

        /** 「首页」项被点击（仅 withHome=true 时可能触发） */
        void onHomeClick();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterTypeBinding binding;
        private final AdapterTypeHomeBinding home;

        ViewHolder(@NonNull AdapterTypeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.home = null;
        }

        ViewHolder(@NonNull AdapterTypeHomeBinding home) {
            super(home.getRoot());
            this.binding = null;
            this.home = home;
        }
    }
}
