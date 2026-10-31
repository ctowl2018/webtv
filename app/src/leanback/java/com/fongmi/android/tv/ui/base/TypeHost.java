package com.fongmi.android.tv.ui.base;

/**
 * 承载分类内容（{@code FolderFragment} → {@code TypeFragment}）的宿主页面协议。
 * <p>
 * 分类内容既可能挂在分类页（VodActivity），也可能挂在首页的分页里（HomeActivity），
 * 二者顶部导航网格、筛选面板的宿主方法并不相同，因此用这个接口把它们统一起来：
 * 内层碎片只依赖接口，不再强转具体 Activity，跨宿主复用不会崩。
 */
public interface TypeHost {

    /**
     * 宿主的分类导航网格 id。
     * {@code CustomVerticalGridView} 需要它来实现「内容首行按上键回到当前选中的标签」。
     * 注意两个页面这个网格的 id 不同（分类页是 recycler，首页是 typeRecycler），不能用常量写死。
     */
    int getTypeBarId();

    /** 收起当前分类已展开的筛选面板（进入下一级目录时调用） */
    void closeFilter();
}
