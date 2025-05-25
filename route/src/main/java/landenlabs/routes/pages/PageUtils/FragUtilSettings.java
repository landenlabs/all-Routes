/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package com.landenlabs.routes.pages.PageUtils;

import static com.landenlabs.routes.utils.Ui.layoutFromStyle;

import android.content.Context;
import android.graphics.Color;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.StyleRes;

import com.landenlabs.routes.R;

import java.util.Locale;

/**
 * Helper to manage status/setting rows with expandable row list view.
 */
public class FragUtilSettings {

    @StyleRes
    private final static int textStyle = R.style.text20Settings;
    private final static int labelStyle = R.style.text20SettingsLabel;
    private final static int valueStyle = R.style.text20SettingsValue;

    public static TextView addCol(GridLayout gridVw, CharSequence label) {
        Context context = gridVw.getContext();
        TextView labelTv = new TextView(new ContextThemeWrapper(context, textStyle));
        labelTv.setText(label);
        labelTv.setId(View.generateViewId());
        gridVw.addView(labelTv);

        GridLayout.LayoutParams lp = (GridLayout.LayoutParams) labelTv.getLayoutParams();
        lp.columnSpec = GridLayout.spec(0, 2);
        lp.setGravity(Gravity.CENTER);
        labelTv.setLayoutParams(lp);
        return labelTv;
    }

    public static View addRow(GridLayout gridVw, String label, CharSequence value) {
        /*
        GridLayout.LayoutParams lpKey= new GridLayout.LayoutParams(GridLayout.spec(
                GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED,GridLayout.FILL,1f)
        );
        lpKey.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lpKey.width = 0;
         */

        Context context = gridVw.getContext();
        TextView labelTv = new TextView(new ContextThemeWrapper(context, labelStyle));
        labelTv.setText(label);
        // gridVw.addView(labelTv);
        gridVw.addView(labelTv, 0, ViewGroup.LayoutParams.WRAP_CONTENT);
        ((GridLayout.LayoutParams) labelTv.getLayoutParams()).columnSpec =
                 GridLayout.spec(GridLayout.UNDEFINED,GridLayout.FILL, 1f);

        TextView valueTv = new TextView(new ContextThemeWrapper(context, valueStyle));
        valueTv.setText(value);
        // gridVw.addView(valueTv);
        gridVw.addView(valueTv,0, ViewGroup.LayoutParams.WRAP_CONTENT);
        ((GridLayout.LayoutParams) valueTv.getLayoutParams()).columnSpec =
                GridLayout.spec(GridLayout.UNDEFINED,GridLayout.FILL, 1f);
        return valueTv;
    }

    public static void addRowTB(GridLayout gridVw, String label, boolean value, String nameOn, String nameOff, View.OnClickListener onClickTB) {
        Context context = gridVw.getContext();
        TextView labelTv = new TextView(new ContextThemeWrapper(context, labelStyle));
        labelTv.setText(label);
        gridVw.addView(labelTv);

        ToggleButton valueTB = new ToggleButton(new ContextThemeWrapper(context, R.style.text20Settings));
        valueTB.setTextOn(nameOn);
        valueTB.setTextOff(nameOff);
        valueTB.setChecked(value);
        gridVw.addView(valueTB);
        valueTB.setOnClickListener(onClickTB);
        GridLayout.LayoutParams lp = (GridLayout.LayoutParams) valueTB.getLayoutParams();
        layoutFromStyle(context, R.style.text20Settings, lp);
        valueTB.setLayoutParams(lp);
        valueTB.setBackgroundColor(Color.BLACK);
    }

    public static void addRowBtn(GridLayout gridVw, String nameOn, View.OnClickListener onClick) {
        Context context = gridVw.getContext();
        Button valueTB = new Button(new ContextThemeWrapper(context, R.style.text20Settings));
        valueTB.setText(nameOn);
        gridVw.addView(valueTB);
        valueTB.setOnClickListener(onClick);
        GridLayout.LayoutParams lp = (GridLayout.LayoutParams) valueTB.getLayoutParams();
        layoutFromStyle(context, R.style.text20Settings, lp);
        lp.columnSpec = GridLayout.spec(0, 2);
        lp.setGravity(Gravity.CENTER);
        valueTB.setLayoutParams(lp);
    }

    public static void addRowBtn(GridLayout gridVw, String label, String value, View.OnClickListener onClick) {
        Context context = gridVw.getContext();
        TextView labelTv = new TextView(new ContextThemeWrapper(context, labelStyle));
        labelTv.setText(label);
        gridVw.addView(labelTv);

        Button valueTB = new Button(new ContextThemeWrapper(context, R.style.text20Settings));
        valueTB.setText(value);
        valueTB.setOnClickListener(onClick);
        gridVw.addView(valueTB);
    }

    public static void addColor(GridLayout card, String colorName, @ColorInt int colorVal) {
        addRow(card, String.format(Locale.US, "%s %08X", colorName, colorVal), " _________ ")
                .setBackgroundColor(colorVal);
    }

    public static void setCard(View card, String str, @DrawableRes int iconRes) {
        TextView tv = ((View) card.getParent()).findViewById(R.id.card_title);
        tv.setText(str);
        ImageView iv = ((View) card.getParent()).findViewById(R.id.card_icon);
        iv.setImageResource(iconRes);
    }
}
