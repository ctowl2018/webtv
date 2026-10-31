package com.fongmi.android.tv.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.ui.activity.HomeActivity;

/**
 * 首页分页里的第 0 页（「首页」页）。
 * <p>
 * 首页的内容（最近观看 / 更新推荐 / 各分类行）本来直接写在 activity_home.xml 里，
 * 这层 Fragment 只是给它套一个可被 ViewPager 管理的容器：真正的视图对象由 HomeActivity 持有，
 * 这里通过 {@link HomeActivity#takeHomeContent()} 借来挂上。这样首页内容的适配器、
 * ViewModel、焦点逻辑全部零改动，而它又能像分类页那样作为「第 0 页」参与左右切换。
 */
public class HomePageFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FrameLayout root = new FrameLayout(inflater.getContext());
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View content = takeContent();
        if (content != null) root.addView(content, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        return root;
    }

    private View takeContent() {
        Activity activity = getActivity();
        return activity instanceof HomeActivity ? ((HomeActivity) activity).takeHomeContent() : null;
    }

    @Override
    public void onDestroyView() {
        //只摘掉引用，不销毁视图：视图归 Activity 所有，页面被回收后重新挂载仍保持原状态
        View view = getView();
        if (view instanceof ViewGroup) ((ViewGroup) view).removeAllViews();
        super.onDestroyView();
    }
}
