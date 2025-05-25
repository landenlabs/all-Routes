/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.data;

import static com.wsi.mapsdk.utils.PrefEncrypt.getSharedPref;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.google.android.gms.location.Priority;
import com.weather.pangea.model.overlay.StrokeStyle;
import com.weather.pangea.model.overlay.StrokeStyleBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manager app settings.
 */
public class RouteSettings {
    private static final String TAG = RouteSettings.class.getSimpleName();

    public static final long MAX_GPS_AGE_MILLI = TimeUnit.MINUTES.toMillis(5);
    public static final String DAY_TM_FMT = "EEE hh:mm a";

    // ToDo - set to 5 minutes for non-debug
    public static long gpsRequestMilli = TimeUnit.SECONDS.toMillis(30);
    public static long gpsSaveMilli = gpsRequestMilli /3;
    public static int gpsMinMeters = 10;

    // TODO - add to ui, save/restore from pref
    public static /* Priority */ int  gpsRequestPermission = Priority.PRIORITY_HIGH_ACCURACY;
    public static boolean unitEnglish = true;

    public static final int strokeWidth = 8;
    public static final  List<Integer> dashPattern = Arrays.asList(strokeWidth*3, strokeWidth*2);
    public static StrokeStyle lineStyleStd = new StrokeStyleBuilder().setColor(Color.RED).setOpacity(0.8f).setWidth(strokeWidth*2).build();
    public static StrokeStyle lineStyleTest = new StrokeStyleBuilder().setColor(Color.BLUE).setOpacity(0.5f).setDashPattern(dashPattern).setWidth(strokeWidth).build();
    public static StrokeStyle lineStyleRev = new StrokeStyleBuilder().setColor(Color.RED).setOpacity(0.5f).setDashPattern(dashPattern).setWidth(strokeWidth).build();
    public static StrokeStyle gridStyleRev = new StrokeStyleBuilder().setColor(Color.BLACK).setOpacity(0.5f).setWidth(2).build();

    /*
     * Latitude/Longitude accuracy
     *
     https://gis.stackexchange.com/questions/8650/measuring-accuracy-of-latitude-and-longitude
     places  degrees      N/S or E/W     E/W at         E/W at       E/W at
     at                   equator        lat=23N/S      lat=45N/S    lat=67N/S
     ------- -------      ----------     ----------     ---------    ---------
     0       1            111.32 km      102.47 km      78.71 km     43.496 km
     1       0.1          11.132 km      10.247 km      7.871 km     4.3496 km
     2       0.01         1.1132 km      1.0247 km      787.1 m      434.96 m
     3       0.001        111.32 m       102.47 m       78.71 m      43.496 m
     4       0.0001       11.132 m       10.247 m       7.871 m      4.3496 m
     */
    public static double minBoundsDeg = 0.001 * 5;


    private static final String PREF_GPS_REQ_MILLI = "gpsReqMilli";
    private static final String PREF_GPS_SAVE_MILLI = "gpsSaveMilli";
    private static final String PREF_GPS_MIN_METERS = "gpsMinMeters";
    private static final String PREF_TRACK_CLR = "trackClr";
    private static final String PREF_TEST_CLR = "testClr";
    private static final String PREF_REV_CLR = "revClr";
    private static final String PREF_MIN_BND_DEG = "minBndDeg";

    public static void init(@NonNull Context context) {
        SharedPreferences pref = getSharedPref(context, TAG);
        gpsRequestMilli = pref.getLong(PREF_GPS_REQ_MILLI, gpsRequestMilli);
        gpsSaveMilli = pref.getLong(PREF_GPS_SAVE_MILLI, gpsSaveMilli);
        gpsMinMeters = pref.getInt(PREF_GPS_MIN_METERS, gpsMinMeters);
        minBoundsDeg = pref.getFloat(PREF_MIN_BND_DEG, (float)minBoundsDeg);

        int trackClr = pref.getInt(PREF_TRACK_CLR, lineStyleStd.getColor());
        int testClr = pref.getInt(PREF_TEST_CLR, lineStyleTest.getColor());
        int revClr = pref.getInt(PREF_REV_CLR, lineStyleRev.getColor());

        lineStyleRev = new StrokeStyleBuilder().setColor(revClr).setOpacity(0.5f).setDashPattern(dashPattern).setWidth(strokeWidth).build();
        lineStyleTest = new StrokeStyleBuilder().setColor(testClr).setOpacity(0.5f).setDashPattern(dashPattern).setWidth(strokeWidth).build();
        lineStyleStd = new StrokeStyleBuilder().setColor(trackClr).setOpacity(0.8f).setWidth(strokeWidth*2).build();
    }

    public static void save(@NonNull Context context) {
        SharedPreferences pref = getSharedPref(context, TAG);
        pref.edit()
                .putLong(PREF_GPS_REQ_MILLI, gpsRequestMilli)
                .putLong(PREF_GPS_SAVE_MILLI, gpsSaveMilli)
                .putInt(PREF_GPS_MIN_METERS, gpsMinMeters)
                .putFloat(PREF_MIN_BND_DEG, (float)minBoundsDeg)
                .putInt(PREF_TRACK_CLR, lineStyleStd.getColor())
                .putInt(PREF_TEST_CLR, lineStyleTest.getColor())
                .putInt(PREF_REV_CLR, lineStyleRev.getColor())
                .apply();
    }

    public static StrokeStyle makeStroke(@ColorInt int color, float widthMult) {
        return new StrokeStyleBuilder().setColor(color).setOpacity(0.8f).setWidth(strokeWidth * widthMult).build();
    }
}
