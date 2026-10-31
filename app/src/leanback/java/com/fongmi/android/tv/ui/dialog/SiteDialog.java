package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.DialogSiteBinding;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.SiteListener;
import com.fongmi.android.tv.ui.adapter.SiteAdapter;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;

/**
 * 首页点 Logo 弹出的「切换源」面板。
 * <p>
 * 容器换成与播放控制条「播放设置」（ControlDialog）同一套底部面板：
 * 同一张 {@code shape_dialog_glass} 玻璃底、同样的上滑动画、ControlSheetTitle 标题、
 * 同一张 {@code selector_control_sheet_button}（默认/选中/焦点三态）与统一的 13sp 字号。
 * 站点网格、搜索过滤、配置/搜索模式/换源模式/全选/取消 这些功能与数据逻辑原样保留。
 */
public class SiteDialog extends BaseBottomSheetDialog implements SiteAdapter.OnClickListener {

    private static final int GRID_COUNT = 3;
    private static final int ITEM_HEIGHT = 40;
    private static final int ITEM_SPACE = 10;
    private static final int MAX_HEIGHT = 300;

    private RecyclerView.ItemDecoration decoration;
    private DialogSiteBinding binding;
    private FragmentActivity activity;
    private SiteListener listener;
    private SiteAdapter adapter;
    private boolean action;
    private int type;

    public static SiteDialog create() {
        return new SiteDialog();
    }

    public SiteDialog search() {
        type = 1;
        return this;
    }

    public SiteDialog action() {
        action = true;
        return this;
    }

    public void show(FragmentActivity activity) {
        this.activity = activity;
        if (activity instanceof SiteListener) listener = (SiteListener) activity;
        if (activity.isFinishing() || activity.isDestroyed()) return;
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof SiteDialog) return;
        show(activity.getSupportFragmentManager(), null);
    }

    private int getCount() {
        return GRID_COUNT;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogSiteBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        adapter = new SiteAdapter(this);
        setRecyclerHeight();
        binding.keyword.setVisibility(View.GONE);
        binding.action.setVisibility(action ? View.VISIBLE : View.GONE);
        binding.search.setVisibility(action ? View.VISIBLE : View.GONE);
        binding.change.setVisibility(action ? View.VISIBLE : View.GONE);
        binding.select.setVisibility(action ? View.VISIBLE : View.GONE);
        binding.cancel.setVisibility(action ? View.VISIBLE : View.GONE);
        binding.mode.setVisibility(View.GONE);
        setType(type);
        setRecyclerView();
        setMode();
        if (adapter.getItemCount() == 0) {
            Notify.show(R.string.error_empty);
            binding.getRoot().post(this::dismiss);
        }
    }

    @Override
    protected void initEvent() {
        binding.config.setOnClickListener(v -> {
            FragmentActivity activity = getDialogActivity();
            dismiss();
            App.post(() -> HistoryDialog.create().vod().readOnly().show(activity, item -> loadConfig(activity, item)), 100);
        });
        binding.mode.setOnClickListener(this::onMode);
        binding.select.setOnClickListener(v -> adapter.selectAll());
        binding.cancel.setOnClickListener(v -> adapter.cancelAll());
        binding.search.setOnClickListener(v -> setType(v.isSelected() ? 0 : 1));
        binding.change.setOnClickListener(v -> setType(v.isSelected() ? 0 : 2));
        binding.keyword.addTextChangedListener(new CustomTextListener() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                setRecyclerView();
                setMode();
                setRecyclerHeight();
            }
        });
    }

    private void setRecyclerView() {
        if (binding.recycler.getAdapter() == null) binding.recycler.setAdapter(adapter);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        if (decoration == null) binding.recycler.addItemDecoration(decoration = new SpaceItemDecoration(getCount(), ITEM_SPACE));
        if (binding.recycler.getLayoutManager() == null) binding.recycler.setLayoutManager(new GridLayoutManager(getDialogActivity(), getCount()));
    }

    private void setRecyclerHeight() {
        int rows = Math.max(1, (int) Math.ceil((double) adapter.getItemCount() / getCount()));
        int height = rows * ResUtil.dp2px(ITEM_HEIGHT) + Math.max(0, rows - 1) * ResUtil.dp2px(ITEM_SPACE);
        //面板整体要留在屏幕内：列表最多占屏幕高度的 55%，超出的部分由网格自己滚动
        int maxHeight = Math.min(ResUtil.dp2px(MAX_HEIGHT), (int) (ResUtil.getScreenHeight() * 0.55f));
        ViewGroup.LayoutParams params = binding.recycler.getLayoutParams();
        params.height = Math.min(height, maxHeight);
        binding.recycler.setLayoutParams(params);
    }

    private void setType(int type) {
        binding.search.setSelected(type == 1);
        binding.change.setSelected(type == 2);
        binding.select.setClickable(type > 0);
        binding.cancel.setClickable(type > 0);
        adapter.setType(this.type = type);
    }

    private void setMode() {
        binding.mode.setEnabled(false);
    }

    private void onMode(View view) {
        setRecyclerView();
        setMode();
        setRecyclerHeight();
    }

    @Override
    public void onItemClick(Site item) {
        if (listener != null) listener.setSite(item);
        dismiss();
    }

    private void loadConfig(FragmentActivity activity, Config config) {
        if (config.getUrl().equals(VodConfig.getUrl())) return;
        VodConfig.load(config, new Callback() {
            @Override
            public void start() {
                Notify.progress(activity);
            }

            @Override
            public void success() {
                Notify.dismiss();
                LiveConfig.get().clear();
            }

            @Override
            public void error(String msg) {
                Notify.dismiss();
                Notify.show(msg);
            }
        });
    }

    private FragmentActivity getDialogActivity() {
        return getActivity() != null ? getActivity() : activity;
    }
}
