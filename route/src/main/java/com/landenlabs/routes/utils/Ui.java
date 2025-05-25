/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Adapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;

import java.util.Locale;

import landenlabs.wx_lib_data.logger.ALog;
import landenlabs.wx_lib_data.logger.ALogUtils;

/**
 * UI object manipulation.
 */
public class Ui {

    private static ViewTreeObserver.OnGlobalLayoutListener dummyListener = () -> {
    };

    @SuppressWarnings("unchecked")
    public static <E extends View> E viewById(View rootView, int id) {
        return (E) rootView.findViewById(id);
    }

    public static <E extends View> E needViewById(View rootView, int id) {
        E foundView = rootView.findViewById(id);
        if (foundView == null)
            throw new NullPointerException("layout resource missing");
        return foundView;
    }

    /**
     * Set text if not null.
     */
    public static void setTextIf(@Nullable View tv, @Nullable CharSequence cs) {
        if (tv instanceof TextView) {
            ((TextView) tv).setText((cs != null) ? cs : "");
        }
    }

    public static void setTextIf(@Nullable View tv, @StringRes int strRes) {
        if (tv instanceof TextView) {
            ((TextView) tv).setText(strRes);
        }
    }

    /**
     * Set visibility if view is not null.
     */
    public static void setVisibleIf(int vis, View... views) {
        for (View view : views) {
            if (view != null) view.setVisibility(vis);
        }
    }

    public static void setImageResourceIf(@DrawableRes int imageRes, View... imageViews) {
        for (View view : imageViews) {
            if (view instanceof ImageView) ((ImageView) view).setImageResource(imageRes);
        }
    }

    public static void setImageBgIf(@DrawableRes int imageRes, View... imageViews) {
        for (View view : imageViews) {
            if (view instanceof ImageView) ((ImageView) view).setBackgroundResource(imageRes);
        }
    }

    public static void setImageTintIf(@ColorInt int tintColor, View... imageViews) {
        for (View view : imageViews) {
            if (view instanceof ImageView)
                ((ImageView) view).setImageTintList(ColorStateList.valueOf(tintColor));
        }
    }

    private static float screenScale() {
        // return Math.min(1.0f, displayWidthPx / screenWidthPx);
        return 1.0f; // TODO - fix this
    }

    public static int screenScale(int iValue) {
        return (iValue <= 0) ? iValue : Math.round(iValue * screenScale());
    }

    public static ViewGroup.LayoutParams layoutFromAnyStyle(
            @NonNull Context context, @StyleRes int style, ViewGroup.LayoutParams lp) {
        if (lp instanceof LinearLayout.LayoutParams) {
            return layoutFromStyle(context, style, (LinearLayout.LayoutParams) lp);
        } else if (lp instanceof ViewGroup.MarginLayoutParams) {
            return layoutFromStyle(context, style, (ViewGroup.MarginLayoutParams) lp);
        } else {
            return layoutFromStyle(context, style, lp);
        }
    }

    public static ViewGroup.LayoutParams layoutFromStyle(
            @NonNull Context context, @StyleRes int style, ViewGroup.LayoutParams lp) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(null,
                new int[]{
                        android.R.attr.layout_width,
                        android.R.attr.layout_height,
                },
                0, style);
        try {
            lp.width = screenScale(ta.getLayoutDimension(0, ViewGroup.LayoutParams.WRAP_CONTENT));
            lp.height = screenScale(ta.getLayoutDimension(1, ViewGroup.LayoutParams.WRAP_CONTENT));
        } finally {
            ta.recycle();
        }
        return lp;
    }

    public static ViewGroup.MarginLayoutParams layoutFromStyle(
            @NonNull Context context, @StyleRes int style, ViewGroup.MarginLayoutParams lp) {
        layoutFromStyle(context, style, (ViewGroup.LayoutParams) lp);
        TypedArray ta = context.getTheme().obtainStyledAttributes(null,
                new int[]{
                        android.R.attr.layout_marginTop,
                        android.R.attr.layout_marginBottom,
                        android.R.attr.layout_marginStart,
                        android.R.attr.layout_marginEnd,
                },
                0, style);
        try {
            float scale = screenScale();
            lp.topMargin = screenScale(ta.getLayoutDimension(0, lp.topMargin));
            lp.bottomMargin = screenScale(ta.getLayoutDimension(1, lp.bottomMargin));
            lp.leftMargin = screenScale(ta.getLayoutDimension(2, lp.leftMargin));
            lp.rightMargin = screenScale(ta.getLayoutDimension(3, lp.rightMargin));
        } finally {
            ta.recycle();
        }
        return lp;
    }

    public static LinearLayout.LayoutParams layoutFromStyle(
            @NonNull Context context, @StyleRes int style, LinearLayout.LayoutParams lp) {
        layoutFromStyle(context, style, (ViewGroup.MarginLayoutParams) lp);
        TypedArray ta = context.getTheme().obtainStyledAttributes(null,
                new int[]{
                        android.R.attr.layout_weight,
                },
                0, style);
        try {
            lp.weight = ta.getFloat(0, lp.weight);
        } finally {
            ta.recycle();
        }
        return lp;
    }

    /**
     * Helper to remove listener using appropriate API.
     *
     * @param view        object
     * @param forceRemove Set to true to force removal of listener.
     *                    <p>
     *                    There is a bug in the
     *                    globalLayout, it has a custom copy-on-write implementation which fails
     *                    to remove the listener until the list is used.
     *                    See http://landenlabs.com/android/info/leaks/android-memory-leaks.html#viewtree-listener
     *                    <p>
     *                    Set this to true to force removal.
     *                    <p>
     *                    Set to false if called from globalLayout listener because can't perform
     *                    cleanup while list is active.
     */
    public static void removeOnGlobalLayoutListener(View view,
            ViewTreeObserver.OnGlobalLayoutListener listener,
            boolean forceRemove) {
        if (null != view) {
            view.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
            view.getViewTreeObserver().removeOnGlobalLayoutListener(listener);

            if (forceRemove) {
                // Following call to dispatchOnGlobalLayout causes the internal
                // listener copyOnWriteArray to update freeing the old listener.

                // Need one or more items in list before calling dispatchOnGlobalLayout to
                // fore copy-on-write to execute.
                view.getViewTreeObserver().addOnGlobalLayoutListener(dummyListener);
                view.getViewTreeObserver().dispatchOnGlobalLayout();
                view.getViewTreeObserver().removeOnGlobalLayoutListener(dummyListener);
            }
        }
    }

    /**
     * Dump info about view and its children into StringBuffer returned.
     */
    public static StringBuilder dumpViews(View view, int level) {
        StringBuilder sb = new StringBuilder();
        String prefix;
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
        prefix = sb.toString();

        sb = new StringBuilder();
        if (false) {
            sb.append(String.format(Locale.US, "hasOnClick=%-5b focusable=%-5b inTouch=%-5b shown=%-5b ",
                    view.hasOnClickListeners(),
                    view.isFocusable(),
                    view.isFocusableInTouchMode(),
                    view.isShown()
            ));
        }
        if (true) {
            sb.append(String.format(Locale.US, "Pad[%2d %2d %2d %2d] ",
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    view.getPaddingBottom()));
        }
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            sb.append(String.format(Locale.US, "Margin[%2d %2d %2d %2d] ",
                    mp.leftMargin,
                    mp.topMargin,
                    mp.rightMargin,
                    mp.bottomMargin));
        }

        sb.append(String.format(Locale.US, "Vis=%-5b ", view.getVisibility() == View.VISIBLE));
        sb.append(String.format("%2d ", level));
        sb.append(prefix).append(view.getClass().getSimpleName());
        if ((int) view.getId() > 0) {
            sb.append(" ID=").append(view.getId());
            try {
                String resName = view.getResources().getResourceName(view.getId());
                sb.append("=").append(resName);
            } catch (Exception ex) {
                ALog.e.tagMsg("dumpViews ex=", ex);
            }
        }
        if (view.getTag() != null) {
            sb.append(" tag=").append(ALogUtils.getString(null, view.getTag()));
        }
        if (!TextUtils.isEmpty(view.getContentDescription())) {
            sb.append(" desc=").append(view.getContentDescription());
        }
        if (view instanceof TextView) {
            sb.append(" text=").append(((TextView) view).getText());
        }

        sb.append("\n");

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int idx = 0; idx < viewGroup.getChildCount(); idx++) {
                View child = viewGroup.getChildAt(idx);
                sb.append(dumpViews(child, level + 1));
            }

            if (view instanceof ListView) {
                ExpandableListView expView = (ExpandableListView) view;
                Adapter adapter = expView.getAdapter();
                int cnt = adapter.getCount();
                for (int idx = 0; idx < cnt; idx++) {
                    Object obj = adapter.getItem(idx);
                    if (obj instanceof View) {
                        sb.append(dumpViews((View) obj, level + 1));
                    }
                }
            }
        }

        return sb;
    }

    public static String getTag(@NonNull View view) {
        return (view.getTag() instanceof String) ? view.getTag().toString() : "";
    }

    public static boolean tagHas(@NonNull View view, @NonNull String want) {
        return getTag(view).contains(want);
    }

    @ColorInt
    public static int getColor(@NonNull View view, @ColorRes int colorRes) {
        return view.getResources().getColor(colorRes, view.getContext().getTheme());
    }
}
