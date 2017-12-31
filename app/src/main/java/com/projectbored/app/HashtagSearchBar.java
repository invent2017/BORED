package com.projectbored.app;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.support.v7.widget.AppCompatAutoCompleteTextView;

/**
 * Created by LikHern on 31/12/2017.
 */

public class HashtagSearchBar extends AppCompatAutoCompleteTextView {

    public HashtagSearchBar(Context context) {
        super(context);
    }

    public HashtagSearchBar(Context arg0, AttributeSet arg1) {
        super(arg0, arg1);
    }

    public HashtagSearchBar(Context arg0, AttributeSet arg1, int arg2) {
        super(arg0, arg1, arg2);
    }

    @Override
    public boolean enoughToFilter() {
        return true;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);

        if(focused && getAdapter() != null) {
            performFiltering(getText(), 0);
        }
    }
}
