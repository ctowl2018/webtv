package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.databinding.AdapterEpisodeSheetBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 选集菜单（控制条「选集」按钮弹出的单列列表）的适配器。
 *
 * <p>与 {@link EpisodeAdapter} 的分工：那个服务详情页的多列集数网格，带焦点上下越界转发；
 * 这个是全屏播放页的单列浮层，只管「一行一集、当前集高亮、点了就播」。
 * 两者共用同一份 {@link Episode} 数据（选中态直接读 {@code isSelected()}），
 * 所以从这里切集之后，详情页网格的高亮会跟着变。
 */
public class EpisodeSheetAdapter extends RecyclerView.Adapter<EpisodeSheetAdapter.ViewHolder> {

    private final List<Episode> mItems;
    private final OnClickListener mListener;

    public EpisodeSheetAdapter(OnClickListener listener) {
        mListener = listener;
        mItems = new ArrayList<>();
    }

    /**
     * 灌入全集并<b>倒序</b>排列：最新一集在最上、第 1 集在最下。
     *
     * <p>菜单是从「选集」按钮往上弹的，底边贴着按钮，底部那一行才是用户视线的起点。
     * 倒序之后配合 {@code layoutEpisodeSheet} 的「行数不超过当前集下标 + 1」，
     * 当前集就正好落在最后一行（离按钮最近的位置），往上是还没看的集数，往下是已经看过的。
     */
    public void addAll(List<Episode> items) {
        mItems.clear();
        for (int i = items.size() - 1; i >= 0; i--) mItems.add(items.get(i));
        notifyDataSetChanged();
    }

    public List<Episode> getItems() {
        return Collections.unmodifiableList(mItems);
    }

    /**
     * 当前正在播放那一集的下标（倒序后）。菜单打开时的初始焦点落在这里，
     * 也是「贴到菜单最后一行」的锚点；一集都没选中时退回第一项（最新一集）。
     */
    public int getPosition() {
        for (int i = 0; i < mItems.size(); i++) if (mItems.get(i).isSelected()) return i;
        return 0;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterEpisodeSheetBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Episode item = mItems.get(position);
        //用 activated 而不是 selected：selected 会让该行永久跑马灯（未聚焦也在滚），
        //而「当前集」只需要一个橙色文字标识，见 @color/selector_episode_sheet_text
        holder.binding.text.setActivated(item.isSelected());
        holder.binding.text.setText(item.getDisplayName());
        //点击挂在文字上而不是根容器：焦点由文字持有，根容器不参与焦点，挂在根上按确定键没反应
        holder.binding.text.setOnClickListener(view -> {
            if (mListener != null) mListener.onItemClick(item);
        });
    }

    public interface OnClickListener {

        void onItemClick(Episode item);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterEpisodeSheetBinding binding;

        ViewHolder(@NonNull AdapterEpisodeSheetBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
