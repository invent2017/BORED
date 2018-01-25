package com.projectbored.app;

import android.content.Context;
import android.util.AttributeSet;
import android.support.v7.widget.AppCompatEditText;
import android.view.KeyEvent;
import android.widget.EditText;

/**
 * Created by LikHern on 25/1/2018.
 */

public class UnselectableEditText extends AppCompatEditText {

    public UnselectableEditText(Context context) {
        super(context);
    }

    public UnselectableEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UnselectableEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {

        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            this.clearFocus();
        }
        return super.onKeyPreIme(keyCode, event);
    }
}
