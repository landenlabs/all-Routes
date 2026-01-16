/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.pages.PageUtils;

import android.content.res.Resources;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.helper.widget.Flow;
import androidx.lifecycle.MutableLiveData;

import landenlabs.routes.R;
import landenlabs.routes.utils.Ui;

public class ToastUtils {

    public static MutableLiveData<Integer> show(@NonNull final View child, @NonNull String message, String ... btns) {
        final MutableLiveData<Integer> future = new MutableLiveData<>();
        View root = child.getRootView();
        ViewGroup boxVG = root.findViewById(R.id.dialog_toast);
        if (boxVG != null) {
            Rect rootVis = new Rect();
            Rect parentVis = new Rect();
            Rect childVis = new Rect();
            if (root.getGlobalVisibleRect(rootVis)
                    && ((ViewGroup) boxVG.getParent()).getGlobalVisibleRect(parentVis)
                    && child.getGlobalVisibleRect(childVis)) {
                DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
                // int top = metrics.heightPixels / 2 - parentVis.top;
                // boxVG.setTranslationY(top);
                // int left = metrics.widthPixels / 10 * 9;
                // boxVG.setTranslationX(left);
                TextView msgTv = boxVG.findViewById(R.id.toast_msg);
                if (msgTv != null) {
                    msgTv.setText(message);
                    boxVG.setVisibility(View.VISIBLE);
                }
                if (btns.length > 0) {
                    Flow flow = boxVG.findViewById(R.id.toast_btns);
                    flow.setVisibility(View.VISIBLE);

                    for (int idx = 0; idx < flow.getReferencedIds().length; idx++) {
                        int btnId = flow.getReferencedIds()[idx];
                        View btnVw = boxVG.findViewById(btnId);
                        if (idx < btns.length) {
                            final int idx_final = idx;
                            btnVw.setTag(idx_final);
                            Ui.setTextIf(btnVw, btns[idx]);
                            btnVw.setOnClickListener( v -> { future.postValue(idx_final); boxVG.setVisibility(View.GONE); });
                        } else {
                            Ui.setVisibleIf(View.GONE, btnVw);
                        }
                    }
                } else {
                    boxVG.findViewById(R.id.toast_btns).setVisibility(View.GONE);
                }
            }

            boxVG.setOnClickListener(view -> view.setVisibility(View.GONE));
        }

        return future;
    }
}
