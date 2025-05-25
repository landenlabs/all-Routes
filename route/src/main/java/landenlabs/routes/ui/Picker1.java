/*
 * Copyright (c) 2020 Dennis Lang (LanDen Labs) landenlabs@gmail.com
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @author Dennis Lang
 * @see http://LanDenLabs.com/
 */

package com.landenlabs.routes.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.NumberPicker;

import com.landenlabs.routes.R;

@SuppressWarnings("SameParameterValue")
public class Picker1 extends NumberPicker {

    public Picker1(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public Picker1(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public Picker1(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, 0, 0);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        // setDividerDrawable(null);
        // setShowDividers(SHOW_DIVIDER_NONE);
        // setDividerPadding(0);
        // mSelectionDivider = null;
        // setSelectionDividerHeight(0);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.Picker1, defStyleAttr, defStyleRes);
        try
        {
            CharSequence[] entries = a.getTextArray(R.styleable.Picker1_android_entries);
            if (entries != null)
            {
                String[] strings = new String[entries.length];
                int idx=0;
                for (CharSequence ch: entries){
                    strings[idx++] = ch.toString();
                }

                setMinValue(0);
                setMaxValue(strings.length-1);
                setDisplayedValues(strings);

                // setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
                setWrapSelectorWheel(false);
            }
        }
        finally
        {
            a.recycle();
        }
    }
}
