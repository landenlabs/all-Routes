/*
 * Dennis Lang - LanDenLabs.com
 * Copyright (c) 2026 Dennis Lang (LanDen Labs)
 */

package landenlabs.routes.data;



import static landenlabs.routes.utils.PrefUtil.getSharedPref2;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.Priority;

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

    /**
     * Plain line style used with TWCMapView.addMarkerPolyline() (color/width/dash - no
     * separate opacity param, so opacity must be baked into the alpha channel via argbColor()).
     */
    public static class LineStyle {
        @ColorInt
        private final int color;
        private final float opacity;
        private final double width;
        @Nullable
        private final List<Double> dashPattern;

        public LineStyle(@ColorInt int color, float opacity, double width, @Nullable List<Double> dashPattern) {
            this.color = color;
            this.opacity = opacity;
            this.width = width;
            this.dashPattern = dashPattern;
        }

        @ColorInt
        public int getColor() {
            return color;
        }

        public float getOpacity() {
            return opacity;
        }

        public double getWidth() {
            return width;
        }

        @Nullable
        public List<Double> getDashPattern() {
            return dashPattern;
        }

        /** Packed ARGB color (opacity baked into the alpha channel) for addMarkerPolyline(). */
        @ColorInt
        public int argbColor() {
            return Color.argb(Math.round(opacity * 255f), Color.red(color), Color.green(color), Color.blue(color));
        }
    }

    public static final int strokeWidth = 8;
    public static final  List<Double> dashPattern = Arrays.asList((double) (strokeWidth*3), (double) (strokeWidth*2));
    public static LineStyle lineStyleStd = new LineStyle(Color.RED, 0.8f, strokeWidth*2, null);
    public static LineStyle lineStyleTest = new LineStyle(Color.BLUE, 0.5f, strokeWidth, dashPattern);
    public static LineStyle lineStyleRev = new LineStyle(Color.RED, 0.5f, strokeWidth, dashPattern);
    public static LineStyle gridStyleRev = new LineStyle(Color.BLACK, 0.5f, 2, null);

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
        SharedPreferences pref = getSharedPref2(context, TAG);
        gpsRequestMilli = pref.getLong(PREF_GPS_REQ_MILLI, gpsRequestMilli);
        gpsSaveMilli = pref.getLong(PREF_GPS_SAVE_MILLI, gpsSaveMilli);
        gpsMinMeters = pref.getInt(PREF_GPS_MIN_METERS, gpsMinMeters);
        minBoundsDeg = pref.getFloat(PREF_MIN_BND_DEG, (float)minBoundsDeg);

        int trackClr = pref.getInt(PREF_TRACK_CLR, lineStyleStd.getColor());
        int testClr = pref.getInt(PREF_TEST_CLR, lineStyleTest.getColor());
        int revClr = pref.getInt(PREF_REV_CLR, lineStyleRev.getColor());

        lineStyleRev = new LineStyle(revClr, 0.5f, strokeWidth, dashPattern);
        lineStyleTest = new LineStyle(testClr, 0.5f, strokeWidth, dashPattern);
        lineStyleStd = new LineStyle(trackClr, 0.8f, strokeWidth*2, null);
    }

    public static void save(@NonNull Context context) {
        SharedPreferences pref = getSharedPref2(context, TAG);
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

    public static LineStyle makeStroke(@ColorInt int color, float widthMult) {
        return new LineStyle(color, 0.8f, strokeWidth * widthMult, null);
    }
}
