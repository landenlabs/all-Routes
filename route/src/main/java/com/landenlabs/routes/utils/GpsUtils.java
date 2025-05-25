/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.utils;

import static android.location.LocationManager.GPS_PROVIDER;
import static com.landenlabs.routes.utils.PolyUtil.EARTH_RADIUS_METERS;
import static com.landenlabs.routes.utils.SysUtils.getFusedLocation;
import static com.landenlabs.routes.utils.SysUtils.getLocationManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.landenlabs.routes.data.GpsPoint;
import com.weather.pangea.geom.LatLng;
import com.wsi.mapsdk.utils.WLatLng;
import com.wsi.mapsdk.utils.WLatLngBounds;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import landenlabs.wx_lib_data.logger.ALog;

/*
 * ELatitude/Longitude accuracy
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
public class GpsUtils {
    private final static String TAG = GpsUtils.class.getSimpleName();
    private static final float MAX_PANGEA_LAT = 85.0f;

    @SuppressLint("MissingPermission")
    public static void requestLocation(@NonNull Context context) {
        long GPS_INTERVAL = TimeUnit.SECONDS.toMillis(10);
        LocationRequest request = new LocationRequest.Builder(GPS_INTERVAL).build();
        getFusedLocation(context).requestLocationUpdates(request, new LocationCallback() {
            @Override
            public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
            }
        }, context.getMainLooper());
    }
    public static String logString(@Nullable Location location) {
        if (location == null)
            return "Null Location";
        return String.format(Locale.US, "%.3f,%.3f", location.getLatitude(), location.getLongitude());
    }

    @Nullable
    public static Location getCurrentLocation(@NonNull Context context) {
        LocationManager locationManager = getLocationManager(context);
        Location location = null;

        location = getLastKnownLocation(locationManager, LocationManager.GPS_PROVIDER);
        if (location == null && android.os.Build.VERSION.SDK_INT >= 31) {
            location = getLastKnownLocation(locationManager, LocationManager.FUSED_PROVIDER);
        }
        if (location == null) {
            location = getLastKnownLocation(locationManager, LocationManager.NETWORK_PROVIDER);
        }
        if (location == null) {
            location = getLastKnownLocation(locationManager, LocationManager.PASSIVE_PROVIDER);
        }
        ALog.d.tagMsg(TAG, "getLastKnownLocation gps=", location);
        return location;
    }

    @SuppressLint("MissingPermission")
    @Nullable
    public static Location getLastKnownLocation(@NonNull LocationManager locationManager, @NonNull String provider) {
        try {
            return locationManager.getLastKnownLocation(provider);
        } catch (Exception ignore) {
            // ALog.w.tagMsg(TAG, "getLastKnownLocation gps=", ignore);
        }
        return null;
    }

    public static String getGpsProvider(@Nullable Context context, @Nullable LocationManager locationManager) {
        if (locationManager == null) {
            locationManager = getLocationManager(context);
        }

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        // criteria.setAltitudeRequired(false);
        // criteria.setSpeedRequired(false);
        // criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setCostAllowed(false);

        String provider = locationManager.getBestProvider(criteria, true);
        if (locationManager.isProviderEnabled(GPS_PROVIDER))    // NETWORK_PROVIDER;
            provider = GPS_PROVIDER;    // Force GPS, FUSE does not seem to work in emulator.

        return provider;
    }

    @Nullable
    public static WLatLng toWLatLng(@Nullable Location location) {
        if (location == null)
            return null;
        return new WLatLng(location.getLatitude(), location.getLongitude());
    }

    @Nullable
    public static WLatLng toWLatLng(@Nullable LatLng latLng) {
        if (latLng == null)
            return null;
        return new WLatLng(latLng.getLatitude(), latLng.getLongitude());
    }

    @NonNull
    public static WLatLngBounds minBounds(@NonNull WLatLngBounds bounds, double minSpan) {
        return minBounds(bounds, minSpan, minSpan);
    }
    @NonNull
    public static WLatLngBounds minBounds(@NonNull WLatLngBounds bounds, double spanLat, double spanLng) {
        double paddingLng = Math.max(0, spanLng - bounds.getLongitudeSpan()) /2;
        double paddingLat = Math.max(0, spanLat - bounds.getLatitudeSpan()) /2;
        return WLatLngBounds.builder()
                .include(new WLatLng(validLat(bounds.southwest.latitude - paddingLat), validLng(bounds.southwest.longitude - paddingLng)))
                .include(new WLatLng(validLat(bounds.northeast.latitude + paddingLat), validLng(bounds.northeast.longitude + paddingLng)))
                .build();
    }

    @NonNull
    public static WLatLngBounds padBounds(@NonNull WLatLngBounds bounds, float percentHeight, float percentWidth) {
        double paddingLng = bounds.getLongitudeSpan() * percentWidth;
        double paddingLat =bounds.getLatitudeSpan() * percentHeight;
        return WLatLngBounds.builder()
                .include(new WLatLng(validLat(bounds.southwest.latitude - paddingLat), validLng(bounds.southwest.longitude - paddingLng)))
                .include(new WLatLng(validLat(bounds.northeast.latitude + paddingLat / 2), validLng(bounds.northeast.longitude + paddingLng)))
                .build();
    }

    static double getLongitudeSpan(WLatLngBounds bounds) {
        double deltaLng = bounds.northeast.longitude - bounds.southwest.longitude;
        if (deltaLng < 0.0) deltaLng += 360.0;
        return deltaLng;
    }

    static double getLatitudeSpan(WLatLngBounds bounds) {
        return bounds.northeast.longitude - bounds.southwest.latitude;
    }

    public static WLatLngBounds union(@Nullable WLatLngBounds b1, @Nullable WLatLngBounds b2) {
        WLatLngBounds.Builder builder = new WLatLngBounds.Builder();
        if (b1 != null)
            builder.include(b1.northeast).include(b1.southwest);
        if (b2 != null)
            builder.include(b2.northeast).include(b2.southwest);
        return builder.build();
    }

    public static double validLat(double lat) {
        return Math.min(Math.max(lat, -MAX_PANGEA_LAT), MAX_PANGEA_LAT);
    }

    public static double validLng(double longitude) {
        return (longitude % 360 + 540) % 360 - 180; // wrap longitude to domain -180..+180
    }

    /**
     * Returns the LatLng resulting from moving a distance from an origin
     * in the specified heading (expressed in degrees clockwise from north).
     * @param fromDeg     The LatLng from which to start.
     * @param distanceMeters The distance to travel.
     * @param headingDegrees  The heading in degrees clockwise from north.
     */
    public static GpsPoint sphericalTravel(
            GpsPoint fromDeg, double distanceMeters, double headingDegrees) {
        final double EARTH_RADIUS_METERS = 6378137;
        distanceMeters /=  EARTH_RADIUS_METERS;
        headingDegrees = Math.toRadians(headingDegrees);
        // http://williams.best.vwh.net/avform.htm#LL
        double fromLat = Math.toRadians(fromDeg.latitude);
        double fromLng = Math.toRadians(fromDeg.longitude);
        double cosDistance = Math.cos(distanceMeters);
        double sinDistance = Math.sin(distanceMeters);
        double sinFromLat = Math.sin(fromLat);
        double cosFromLat = Math.cos(fromLat);
        double sinLat = cosDistance * sinFromLat + sinDistance * cosFromLat * Math.cos(headingDegrees);
        double dLng = Math.atan2(
                sinDistance * cosFromLat * Math.sin(headingDegrees),
                cosDistance - sinFromLat * sinLat);
        return new GpsPoint(Math.toDegrees(Math.asin(sinLat)), Math.toDegrees(fromLng + dLng));
    }


    public static double metersBetween(double lat1, double lng1, double lat2, double lng2) {
        double dValue = Math.sin(Math.toRadians(lat1))
                * Math.sin(Math.toRadians(lat2))
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.cos(Math.toRadians(lng2 - lng1));

        // Fix to work around Android math error when same points [30.44, -91.19] generate NaN.
        // Clamp dValue to <= 1.0 to prevent Not-a-number
        double ans = EARTH_RADIUS_METERS * Math.acos(Math.min(dValue, 1.0));
        if (Double.isNaN(ans)) {
            return 0;
        }

        return ans;
    }
}
