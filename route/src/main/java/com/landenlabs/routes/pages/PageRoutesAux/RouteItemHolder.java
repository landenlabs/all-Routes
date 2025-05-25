/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.pages.PageRoutesAux;

import static com.landenlabs.routes.utils.FmtTime.fmtDuration;
import static com.landenlabs.routes.utils.SysUtils.joinCS;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.landenlabs.routes.R;
import com.landenlabs.routes.data.GpsPoint;
import com.landenlabs.routes.data.Track;
import com.landenlabs.routes.data.Trip;
import com.landenlabs.routes.utils.Ui;
import com.landenlabs.routes.utils.UnitDistance;
import com.landenlabs.routes.utils.UnitSpeed;

import org.joda.time.DateTime;

import java.util.Locale;

import landenlabs.wx_lib_data.logger.ALog;

/**
 * Recycler View holder for Routes
 */
public class RouteItemHolder extends RecyclerView.ViewHolder {

    View actionV;
    TextView nameTv;
    TextView dateTv;
    TextView distanceTv;
    TextView durationTv;
    TextView speedTv;
    TextView countTv;
    TextView tracksTv;
    CheckBox expandCb;
    ViewGroup detailsGv;
    RouteViewHelper helper;

    public RouteItemHolder(@NonNull View view, RouteViewHelper helper) {
        super(view);
        this.helper = helper;
        actionV = itemView.findViewById(R.id.selected);
        nameTv = itemView.findViewById(R.id.name);
        dateTv = itemView.findViewById(R.id.date);
        distanceTv = itemView.findViewById(R.id.distance);
        durationTv = itemView.findViewById(R.id.duration);
        speedTv = itemView.findViewById(R.id.speed);
        countTv = itemView.findViewById(R.id.count);
        tracksTv = itemView.findViewById(R.id.tracks);
        expandCb = itemView.findViewById(R.id.expand);
        detailsGv = itemView.findViewById(R.id.row_details);
    }

    public void onBindViewHolder(Track track, int position) {
        try {
            if (actionV instanceof CheckBox) {
                CheckBox cb = (CheckBox)actionV;
                cb.setTag(R.id.selected, position);
                cb.setVisibility(helper.showCheckbox ? View.VISIBLE : View.GONE);
                cb.setOnClickListener(helper.onClick);
                cb.setChecked(helper.selectedSet.contains(position));
            }
            nameTv.setText(String.format(Locale.US, "[%d] %s", track.getId(), track.getName()));
            DateTime dt = new DateTime(track.getMilliStart());
            dateTv.setText(dt.toString("EEE MMM/dd hh:mm a"));
            distanceTv.setText(String.format(Locale.US, helper.distanceFmt, UnitDistance.METERS.toMiles(track.getMeters())));
            CharSequence periodStr = fmtDuration(track.getDurationMilli());
            durationTv.setText(joinCS(helper.durationLbl, periodStr));
            speedTv.setText(String.format(Locale.US, helper.speedFmt, UnitSpeed.METERS_PER_SECOND.toMilesPerHour(track.getMetersPerSecond())));
            countTv.setText(String.format(Locale.US, helper.countFmt, track.getPointCnt()));

            if (track instanceof Trip) {
                final Trip trip = (Trip)track;
                Ui.setTextIf(tracksTv, String.format(Locale.US, helper.tracksFmt, trip.tracks.size()));
                expandCb.setVisibility(trip.tracks.size() > 1 ? View.VISIBLE : View.INVISIBLE);
                expandCb.setChecked(helper.expandSet.contains(position));
                expandCb.setOnClickListener(v -> { showExpand(expandCb.isChecked(), position, trip);});
                showExpand(expandCb.isChecked(), position, trip);
            } else {
                expandCb.setVisibility(track.getPointCnt() > 1 ? View.VISIBLE : View.INVISIBLE);
                expandCb.setChecked(helper.expandSet.contains(position));
                expandCb.setOnClickListener(v -> { showExpand(expandCb.isChecked(), position, track);});
                showExpand(expandCb.isChecked(), position, track);
            }
        } catch (Exception ex) {
            ALog.e.tagMsg(this, "Route presentation error ", ex);
        }

        boolean show = !helper.showOnlyChecked || helper.selectedSet.contains(position);
        itemView.setVisibility(show ? View.VISIBLE : View.GONE );
    }

    private static final int MAX_DETAIL = 10;
    private void showExpand(boolean expanded, int position, Track track) {
        if (detailsGv != null) {
            detailsGv.removeAllViews();
            if (expanded)
                helper.expandSet.add(position);
            else
                helper.expandSet.remove(position);

            if (expanded) {
                int idx = 0;
                if (track instanceof Trip) {
                    final Trip trip = (Trip)track;
                    final Context contextThemeWrapper = new ContextThemeWrapper(detailsGv.getContext(), R.style.routeCb);
                    for (Track _track : trip.tracks) {
                        if (idx++ > MAX_DETAIL) break;
                        CheckedTextView cb = new CheckedTextView(contextThemeWrapper);
                        cb.setText( _track.logString());
                        detailsGv.addView(cb);
                        cb.setOnClickListener( v -> {
                            CheckedTextView vcb = (CheckedTextView)v;
                            vcb.setChecked(!vcb.isChecked());
                            if (!trip.name.equals(helper.detailSetName)) {
                                helper.detailSet.clear();
                                helper.detailSetName = trip.name;
                            }
                        });
                    }
                } else {
                    final Context contextThemeWrapper = new ContextThemeWrapper(detailsGv.getContext(), R.style.text18);
                    for (GpsPoint point : track.points) {
                        if (idx++ > MAX_DETAIL) break;
                        TextView tv = new TextView(contextThemeWrapper);
                        tv.setText(String.format(Locale.US, "%3d: %s", idx, point.logString()));
                        detailsGv.addView(tv);
                    }
                    if (track.getPointCnt() > MAX_DETAIL) {
                        TextView tv = new TextView(contextThemeWrapper);
                        tv.setText(String.format(Locale.US, "... #pts=%d", track.getPointCnt()));
                        detailsGv.addView(tv);
                    }
                }
            }
        }
    }

    public static RouteItemHolder createTrackHolderFor( @NonNull ViewGroup parent, int viewType, RouteViewHelper helper) {
        View view = LayoutInflater
                .from(parent.getContext()).inflate(R.layout.list_track_row, parent, false);
        return new RouteItemHolder(view, helper);
    }
    public static RouteItemHolder createTripHolderFor( @NonNull ViewGroup parent, int viewType, RouteViewHelper helper) {
        View view = LayoutInflater
                .from(parent.getContext()).inflate(R.layout.list_trip_row, parent, false);
        return new RouteItemHolder(view, helper);
    }
    public static RouteItemHolder createDowHolderFor( @NonNull ViewGroup parent, int viewType, RouteViewHelper helper) {
        View view = LayoutInflater
                .from(parent.getContext()).inflate(R.layout.list_dow_row, parent, false);
        return new RouteItemHolder(view, helper);
    }
}
