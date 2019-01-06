package com.projectbored.app;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.KeyEvent;

public class ExitableEditText extends AppCompatEditText {

    public ExitableEditText(Context context) {
        super(context);
    }

    public ExitableEditText(Context context, AttributeSet attributeSet) {
        super(context,attributeSet);
    }

    public ExitableEditText(Context context, AttributeSet attributeSet, int defStyleAttribute) {
        super(context, attributeSet, defStyleAttribute);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {

        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            this.clearFocus();
        }

        return super.onKeyPreIme(keyCode, event);
    }
}
