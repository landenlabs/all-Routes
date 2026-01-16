/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.pages.PagesSmartAux;

import android.content.Context;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import landenlabs.routes.R;

/**
 * Recycler "helper" to pass state information to  presentation SmartAlerts.
 */
public class SmartViewHelper {

    public final int clickTagId;
    public final View.OnClickListener onClick;
    public final String distanceFmt;

    public SmartViewHelper(@NonNull Context context, @IdRes int clickTagId, View.OnClickListener onClick ) {
        this.clickTagId = clickTagId;
        this.onClick = onClick;
        distanceFmt = context.getString(R.string.distance_fmt);
    }

}
