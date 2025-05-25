/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.data;

import com.wsi.mapsdk.utils.WLatLngBounds;

import org.joda.time.DateTime;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Group tracks or trips by Day-of-week
 */
public class TripDow extends Trip {

    public int dow;

    public TripDow(int dow) {
        this.dow = dow;
    }

    public void compute() {
        name = DayOfWeek.of(dow+1).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        isSummary = true;
        summaryPtSize = tracks.size();
        meters = 0;
        metersPerSecond = 0;

        if (summaryPtSize > 0) {
            WLatLngBounds.Builder builder = new WLatLngBounds.Builder();
            for (Track track : tracks) {
                meters += track.getMeters();
                metersPerSecond += track.getMetersPerSecond();
                WLatLngBounds bounds = track.getBounds();
                builder.include(bounds.southwest);
                builder.include(bounds.northeast);
            }
            bounds = builder.build();

            if (tracks.size() > 0) {
                meters /= tracks.size();
                metersPerSecond /= tracks.size();
            }
        }
    }

    public static ArrayListEx<? extends Track> dowFrom(ArrayListEx<Track> tracks) {
        ArrayListEx<TripDow> listDow = new ArrayListEx<>(7);
        for (int idx = 0; idx < 7; idx++) {
            listDow.add(new TripDow(idx));
        }
        if (tracks.size() > 0) {
            for (int idx = 0; idx < tracks.size(); idx++) {
                Track track = tracks.get(idx);
                int dow = new DateTime(track.getMilliStart()).getDayOfWeek() -1; // 0..6
                listDow.get(dow).tracks.add(track);
            }
        }

        for (int idx = 0; idx < 7; idx++) {
            listDow.get(idx).compute();
        }
        return listDow;
    }
}
