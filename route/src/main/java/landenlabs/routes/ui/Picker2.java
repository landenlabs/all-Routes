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

package landenlabs.routes.ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.MainThread;


import landenlabs.routes.R;

import java.util.ArrayList;

/**
 * Simple list picker wheel, shows three rows, center selected.
 */
@SuppressWarnings("SameParameterValue")
public class Picker2 extends FrameLayout {

    public static final boolean DEF_WHEEL = true;
    public static final int DEF_VIS_ITEMS = 3;
    public static final int DEF_GRAVITY_ITEMS = Gravity.CENTER;
    public static final int DEF_WIDTH_ITEMS = ViewGroup.LayoutParams.MATCH_PARENT;

    private PickerAdapter adapter;
    private boolean isWheel = DEF_WHEEL;
    private int visibleItems = DEF_VIS_ITEMS;
    private int gravityItems = DEF_GRAVITY_ITEMS;
    private int widthItems = DEF_WIDTH_ITEMS;

    public final ArrayList<String> items = new ArrayList<>();

    public Picker2(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public Picker2(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public Picker2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.ui_picker2, this);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.Picker2, defStyleAttr, defStyleRes);
        try
        {
            CharSequence[] entries = a.getTextArray(R.styleable.Picker2_android_entries);
            if (entries != null)  {
                items.ensureCapacity(entries.length);
                for (CharSequence entry : entries) {
                    items.add(entry.toString());
                }
            }

            setWrapSelectorWheel(a.getBoolean(R.styleable.Picker2_wheel, DEF_WHEEL));
            setVisibleItems(a.getInt(R.styleable.Picker2_visibleItems, DEF_VIS_ITEMS));
            setGravityItems(a.getInt(R.styleable.Picker2_android_gravity, DEF_GRAVITY_ITEMS));
            setWidthItems(a.getInt(R.styleable.Picker2_widthItems, DEF_WIDTH_ITEMS));
        }
        finally
        {
            a.recycle();
        }
    }

    public int selected() {
        return adapter.selected();
    }

    public void setWrapSelectorWheel(boolean isWheel) {
        this.isWheel = isWheel;
    }
    public void setVisibleItems(int visibleItems) {
        this.visibleItems = visibleItems;
    }
    public void setGravityItems(int gravityItems) {
        this.gravityItems = gravityItems;
    }
    public void setWidthItems(int widthItems) {
        this.widthItems = widthItems;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        AbsListView listView = findViewById(R.id.picker_list);
        adapter = new PickerAdapter(getContext(), items, listView);
        adapter.setWheel(isWheel);
        adapter.setVisibleItems(visibleItems);
        adapter.setGravityItems(gravityItems);
        adapter.setWidthItems(widthItems);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onDetachedFromWindow() {
        AbsListView listView = findViewById(R.id.picker_list);
        listView.setAdapter(null);
        adapter = null;
        super.onDetachedFromWindow();
    }

    // =============================================================================================
    @MainThread
    private static class PickerAdapter extends BaseAdapter
            implements AbsListView.OnScrollListener {
        private final Rect rect = new Rect();

        private final ArrayList<String> items;
        private final AbsListView listView;

        private Point centerPoint = null;
        private final DisplayMetrics dm  = Resources.getSystem().getDisplayMetrics();
        private final Rect screenRect = new Rect(0, 0, dm.widthPixels, dm.heightPixels);

        // Configuration.
        private boolean isWheel = true;
        private int visibleItems = DEF_VIS_ITEMS;
        private int gravityItems = DEF_GRAVITY_ITEMS;
        private int widthItems = DEF_WIDTH_ITEMS;

        // -----------------------------------------------------------------------------------------

        PickerAdapter(Context context, ArrayList<String> items, AbsListView listView) {
            this.items = items;
            this.listView = listView;
            listView.setOnScrollListener(this);
        }

        @Override
        public int getCount() {
            return isWheel ? Integer.MAX_VALUE : items.size()+2;
        }

        @Override
        public Object getItem(int position) {  return null; }

        @Override
        public long getItemId(int position) {
            return position % items.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d("test", "getView pos=" + position);
            TextView itemView;
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                itemView = new TextView(parent.getContext());
                itemView.setAllCaps(false);
                itemView.setGravity(gravityItems);
                itemView.setHeight(itemView.getResources().getDimensionPixelSize(R.dimen.picker_row_height));
            } else {
                itemView = (TextView) convertView;
                ListView.LayoutParams lp = (ListView.LayoutParams)itemView.getLayoutParams();
                if (lp != null) {
                    lp.width = widthItems;
                    itemView.setLayoutParams(lp);
                }
            }

            if (centerPoint == null && listView.isShown()) {
                listView.getGlobalVisibleRect(rect);
                if (screenRect.contains(rect) && rect.height() > 0) {
                    centerPoint = new Point(rect.centerX(), rect.centerY());
                }
            }

            if (items.isEmpty()) {
                 itemView.setText("-list-is-empty-");
            } else {
                int itemPos = position;
                String msg = "";
                if (isWheel) {
                    itemPos = position % items.size();
                    msg = items.get(itemPos);
                } else {
                    if (position == 0 || position == items.size()+1) {
                        itemPos = -1;
                    } else {
                        itemPos = position - 1;
                        msg = items.get(itemPos);
                    }
                }
                itemView.setText(msg);
                itemView.setTag(R.id.picker_list, itemPos);
                selectItem(itemView, itemPos);
            }
            return itemView;
        }

        void setWheel(boolean isWheel) {
            this.isWheel = isWheel;
        }

        void setVisibleItems(int visibleItems)  {
            this.visibleItems = visibleItems;
        }
        void setGravityItems(int gravityItems)  {
            this.gravityItems = gravityItems;
        }
        void setWidthItems(int widthItems)  {
            this.widthItems = widthItems;
        }

        int selected() {
            if (centerPoint == null) {
                return 1;
            }
            for (int row = 0; row < items.size(); row++) {
                View itemView = listView.getChildAt(row);
                if (isSelected(itemView)) {
                    return (Integer)itemView.getTag(R.id.picker_list);
                }
            }
            return 1;
        }

        private boolean isSelected(View itemView) {
            itemView.getGlobalVisibleRect(rect);
            return (centerPoint.y >= rect.top && centerPoint.y <= rect.bottom);
        }

        private void selectRow(int pos) {
            if (items.size() > 0) {
                pos = pos % items.size();
                selectItem(listView.getChildAt(pos), pos);
            }
        }

        private void updateSelected(int numRows) {
            for (int row = 0; row < numRows; row++)  selectRow(row);
        }

        private void selectItem(Object view, int pos) {
            if (view instanceof TextView) {
                TextView itemView = (TextView) view;

                boolean selected = (centerPoint != null)
                        ? isSelected(itemView)
                        : pos == visibleItems/2;
                itemView.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
                // itemView.setBackgroundColor(selected ? 0 : 0x10000000);
                itemView.setTextColor(selected ? Color.BLACK : 0x80000000);
                itemView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                        selected ? 20f : 16);
            }
        }

        // -----------------------------------------------------------------------------------------
        // AbsListView.OnScrollListener
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            updateSelected(visibleItems);
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            updateSelected(visibleItemCount);
        }
    }
}
