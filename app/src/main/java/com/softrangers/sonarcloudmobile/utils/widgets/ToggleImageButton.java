package com.softrangers.sonarcloudmobile.utils.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.ImageButton;

import com.softrangers.sonarcloudmobile.R;

/**
 * Created by eduard on 3/19/16.
 */
public class ToggleImageButton extends ImageButton implements Checkable {

    private OnCheckedChangeListener onCheckedChangeListener;
    private boolean isChecked;

    public ToggleImageButton(Context context) {
        super(context);

    }

    public ToggleImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setChecked(attrs);
    }

    public ToggleImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ToggleImageButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private void setChecked(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ToggleImageButton);
        setChecked(a.getBoolean(R.styleable.ToggleImageButton_android_checked, false));
        a.recycle();
    }

    @Override
    public void setChecked(boolean checked) {
        setSelected(checked);
        isChecked = checked;
        if (onCheckedChangeListener != null) {
            onCheckedChangeListener.onCheckedChanged(this, checked);
        }
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void toggle() {
        setChecked(!isChecked());
    }

    @Override
    public boolean performClick() {
        toggle();
        return super.performClick();
    }

    public OnCheckedChangeListener getOnCheckedChangeListener() {
        return onCheckedChangeListener;
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
        this.onCheckedChangeListener = onCheckedChangeListener;
    }

    public interface OnCheckedChangeListener {
        void onCheckedChanged(ToggleImageButton buttonView, boolean isChecked);
    }
}
