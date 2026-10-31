package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.utils.KeyUtil;
import com.google.android.material.textview.MaterialTextView;

public class CustomUpDownView extends MaterialTextView {

    private UpListener upListener;
    private DownListener downListener;
    private ConsumeListener consumeListener;
    //当前这次按下还没松手的方向键（0 表示没有待处理的按下）
    private int pendingKey;
    //本次按下是否已经进入长按连发
    private boolean adjusting;

    public CustomUpDownView(@NonNull Context context) {
        super(context);
    }

    public CustomUpDownView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setUpListener(UpListener upListener) {
        this.upListener = upListener;
    }

    public void setDownListener(DownListener downListener) {
        this.downListener = downListener;
    }

    /**
     * 按键被本控件消费时回调。
     * 视图层一旦消费了按键，Activity 的 dispatchKeyEvent 就收不到，
     * 控制条「重置自动隐藏计时 / 记住当前焦点」这两件事需要在这里补上。
     */
    public void setConsumeListener(ConsumeListener consumeListener) {
        this.consumeListener = consumeListener;
    }

    private boolean hasEvent(KeyEvent event) {
        //只有绑定了上下监听的方向键才走这里的逻辑
        return (upListener != null && KeyUtil.isUpKey(event)) || (downListener != null && KeyUtil.isDownKey(event));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!hasEvent(event)) return super.dispatchKeyEvent(event);
        if (KeyUtil.isActionDown(event)) {
            if (event.getRepeatCount() == 0) {
                //首次按下先按住焦点不动：长按连发（调倍速/调片头片尾/开字幕设置）
                //依赖焦点一直留在本控件才会继续收到重复事件。
                //早先这一下直接放行，焦点当帧就被移走，后面的连发事件再也收不到。
                pendingKey = event.getKeyCode();
                adjusting = false;
                //先照常走一遍默认分发：系统的键盘长按判定是在这一步建立的，
                //整段吞掉的话长按（倍速切换 / 片头片尾复位）就再也触发不了。
                super.dispatchKeyEvent(event);
                notifyConsumed();
                return true;
            }
            //长按连发：执行调整
            adjusting = true;
            notifyConsumed();
            return onKeyDown(event);
        }
        //松手：短按才把焦点交出去，长按调整完则停在原地
        if (pendingKey == 0) return super.dispatchKeyEvent(event);
        int key = pendingKey;
        boolean wasAdjusting = adjusting;
        pendingKey = 0;
        adjusting = false;
        //松手的方向和按下时对不上（中途焦点被别处抢走），不要乱移焦
        if (!wasAdjusting && event.getKeyCode() == key) moveFocus(key);
        notifyConsumed();
        return true;
    }

    /**
     * 短按时把焦点交给相邻控件，方向取自 nextFocusUp / nextFocusDown。
     * 目标取不到就留在原地：宁可焦点不动，也不要把它丢出控制条。
     * 目标指向自己等价于「这个方向不移动焦点」（按钮行的按下就是这个约定）。
     */
    private void moveFocus(int key) {
        int id;
        if (key == KeyEvent.KEYCODE_DPAD_UP) id = getNextFocusUpId();
        else if (key == KeyEvent.KEYCODE_DPAD_DOWN) id = getNextFocusDownId();
        else return;
        if (id == View.NO_ID || id == getId()) return;
        View target = getRootView().findViewById(id);
        if (target == null || target == this || !target.isFocusable() || target.getVisibility() != View.VISIBLE) return;
        target.requestFocus();
    }

    private void notifyConsumed() {
        if (consumeListener != null) consumeListener.onKeyConsumed();
    }

    @Override
    protected void onDetachedFromWindow() {
        pendingKey = 0;
        adjusting = false;
        super.onDetachedFromWindow();
    }

    private boolean onKeyDown(KeyEvent event) {
        if (upListener != null && KeyUtil.isUpKey(event)) upListener.onUp();
        if (downListener != null && KeyUtil.isDownKey(event)) downListener.onDown();
        return true;
    }

    /**
     * 无文字、只配了 drawableStart 时按图标模式绘制：把图标严格放在控件正中，
     * 避免空文本时自带的 compound drawable 靠左上偏移；文字模式完全走原有逻辑。
     */
    @Override
    protected void onDraw(Canvas canvas) {
        Drawable icon = getCenterIcon();
        if (icon == null) {
            super.onDraw(canvas);
            return;
        }
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int availableWidth = getWidth() - paddingLeft - getPaddingRight();
        int availableHeight = getHeight() - paddingTop - getPaddingBottom();
        int size = Math.min(Math.min(icon.getIntrinsicWidth(), icon.getIntrinsicHeight()), Math.min(availableWidth, availableHeight));
        if (size <= 0) {
            super.onDraw(canvas);
            return;
        }
        int left = paddingLeft + (availableWidth - size) / 2;
        int top = paddingTop + (availableHeight - size) / 2;
        icon.setBounds(left, top, left + size, top + size);
        icon.setState(getDrawableState());
        icon.draw(canvas);
    }

    private Drawable getCenterIcon() {
        if (!TextUtils.isEmpty(getText())) return null;
        Drawable[] drawables = getCompoundDrawablesRelative();
        if (drawables.length > 0 && drawables[0] != null) return drawables[0];
        drawables = getCompoundDrawables();
        if (drawables.length > 0 && drawables[0] != null) return drawables[0];
        return null;
    }

    public interface UpListener {

        void onUp();
    }

    public interface DownListener {

        void onDown();
    }

    public interface ConsumeListener {

        void onKeyConsumed();
    }
}
