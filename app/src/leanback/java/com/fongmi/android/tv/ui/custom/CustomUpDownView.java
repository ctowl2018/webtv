package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.utils.KeyUtil;
import com.google.android.material.textview.MaterialTextView;

public class CustomUpDownView extends MaterialTextView {

    private UpListener upListener;
    private DownListener downListener;

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

    private boolean hasEvent(KeyEvent event) {
        // Consume only key repeats (held key = continuous adjustment). A short press falls
        // through to the default focus search: previously every up/down press was swallowed,
        // so focus could never leave this row vertically with the D-pad.
        return event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() > 0 && ((upListener != null && KeyUtil.isUpKey(event)) || (downListener != null && KeyUtil.isDownKey(event)));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (hasEvent(event)) return onKeyDown(event);
        else return super.dispatchKeyEvent(event);
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
        return drawables.length > 0 ? drawables[0] : null;
    }

    public interface UpListener {

        void onUp();
    }

    public interface DownListener {

        void onDown();
    }
}
