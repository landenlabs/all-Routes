/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.pages.PageRoutesAux;

import android.content.Context;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import com.landenlabs.routes.R;

import java.util.HashSet;
import java.util.Set;

/**
 * Recycler "helper" to pass state information to alter list presentation.
 */
public class RouteViewHelper {

    public final String durationLbl;
    public final String distanceFmt;
    public final String speedFmt;
    public final String countFmt;
    public final String tracksFmt;

    public final int clickTagId;
    public boolean showCheckbox = false;
    public boolean showOnlyChecked = false;
    public final  View.OnClickListener onClick;
    public final Set<Integer> selectedSet = new HashSet<>();
    public final Set<Integer> expandSet = new HashSet<>();
    public final Set<Integer> detailSet = new HashSet<>();
    public String detailSetName = "";

    public RouteViewHelper(@NonNull Context context, @IdRes int clickTagId,  View.OnClickListener onClick ) {
        durationLbl = context.getString(R.string.duration_lbl);
        distanceFmt = context.getString(R.string.distance_fmt);
        speedFmt = context.getString(R.string.speed_fmt);
        countFmt = context.getString(R.string.count_fmt);
        tracksFmt = context.getString(R.string.tracks_fmt);
        this.clickTagId = clickTagId;
        this.onClick = onClick;
    }

}
