/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.wx_lib_data.location;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.Objects;


/**
 * Holder of Latitude and Longitude.
 */
public class WLatLng {
    public final double latitude;     // degrees
    public final double longitude;    // degrees

    public WLatLng(double latDeg, double lngDeg) {
        this.latitude = latDeg;
        this.longitude = lngDeg;
    }

    @NonNull
    public WLatLng clone() {
        return new WLatLng(latitude, longitude);
    }

    public double getLatitude() {
        return latitude;
    }
    public double getLongitude() {
        return longitude;
    }

    private final static WLatLng EMPTY = new WLatLng(0, 0);
    public static WLatLng empty() {
        return EMPTY;
    }

    /**
     * Normalize longitude to -180 to 180 range.
     */
    static double normalizeLng(double lng) {
        //noinspection IntegerDivisionInFloatingPointContext
        return lng - ((int) lng / 180 * 360);
    }

    public int hashCode() {
        return Objects.hash(this.latitude, this.longitude);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof WLatLng otherLL)) {
            return false;
        } else {
            return Double.doubleToLongBits(this.latitude)
                    == Double.doubleToLongBits(otherLL.latitude)
                    && Double.doubleToLongBits(this.longitude)
                    == Double.doubleToLongBits(otherLL.longitude);
        }
    }

    private static double SQ(double dvale) {
        return dvale * dvale;
    }

    /**
     * Return difference degrees square
     */
    public static double distanceSQ(@NonNull WLatLng mapboxLL1, @NonNull WLatLng mapboxLL2) {
        return SQ(mapboxLL1.getLatitude() - mapboxLL2.getLatitude())
                + SQ(mapboxLL1.getLongitude() - mapboxLL2.getLongitude());
    }

    @NonNull
    public String toString() {
        return toString("wLatLng:(%.3f, %.3f)");
    }

    @NonNull
    public String toString(@NonNull String fmt) {
        return String.format(Locale.US, fmt, latitude, longitude);
    }

    public static boolean isSimilar(double lat1, double lng1, double lat2, double lng2, double maxDelta) {
        return  (Math.abs(lat1 - lat2) <= maxDelta) && (Math.abs(lng1 - lng2) <= maxDelta);
    }
}
