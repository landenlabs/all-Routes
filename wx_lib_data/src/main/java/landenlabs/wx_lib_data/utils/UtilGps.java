/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package landenlabs.wx_lib_data.utils;

import static android.content.Context.LOCATION_SERVICE;
import static android.location.LocationManager.GPS_PROVIDER;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import landenlabs.wx_lib_data.Constants;
import landenlabs.wx_lib_data.logger.ALog;

/**
 * Manage device GPS services.
 * @noinspection JavadocLinkAsPlainText, unused
 */
@SuppressWarnings("SameParameterValue")
public class UtilGps  {

    private static final String TAG = UtilGps.class.getSimpleName();

    // GPS request criteria
    // Rate to get to request new GPS value
    public static final long GPS_REQ_MILLI = TimeUnit.SECONDS.toMillis(1);
    // GPS min update threshold passed in GPS request
    public static final float GPS_UPD_METERS = 2000; // 2km = 1.24 miles (~ 75 MPH in 1 minute)

    // Rate to perform Lookup and load weather data when
    public static final long GPS_LOOKUP_MILLI = TimeUnit.MINUTES.toMillis(1);
    // Min GPS distance travel to perform Lookup and load weather data while driving
    public static final float GPS_DRIVE_KM = 10.0f;  // 10km = 6.2 miles
    // Min GPS distance travel to perform Lookup and load weather data while parked
    public static final float GPS_PARKED_KM = GPS_UPD_METERS/1000f;

    public static final String MISSING_GPS_LOC_STR = "0,0";

    private LocationManager locationManager;

    public interface MotionState {
        boolean isMoving();
    }

    // ---------------------------------------------------------------------------------------------

    public static boolean noGpsPermission(@NonNull Context appContext) {
        return !anyGpsPermission(appContext);
    }

    public static boolean anyGpsPermission(@NonNull Context appContext) {
        return appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        //   ||  appContext.checkSelfPermission( Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasGpsBgPermission(@NonNull Context appContext) {
        return (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)  // Q = 29
                || appContext.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasGps(@NonNull Context context) {
        return !noGpsPermission(context) && !TextUtils.isEmpty(getGpsProvider(getLocationManager(context)));
    }

    public static LocationManager getLocationManager(@NonNull Context appContext) {
        return (LocationManager) appContext.getSystemService(LOCATION_SERVICE);
    }

    public static String getGpsProvider(@NonNull LocationManager locationManager) {
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
    public static Location getLastKnownLocation(@NonNull Context appContext) {
        LocationManager locationManager = getLocationManager(appContext);
        Location location = null;

        if (anyGpsPermission(appContext)) {
            location = getLastKnownLocation(locationManager, LocationManager.GPS_PROVIDER);
            if (location == null && Build.VERSION.SDK_INT >= 31) {
                location = getLastKnownLocation(locationManager, LocationManager.FUSED_PROVIDER);
            }
            if (location == null) {
                location = getLastKnownLocation(locationManager, LocationManager.NETWORK_PROVIDER);
            }
            if (location == null) {
                location = getLastKnownLocation(locationManager, LocationManager.PASSIVE_PROVIDER);
            }
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
            ALog.none.tagMsg(ALog.TAG_PREFIX, ignore);
        }
        return null;
    }
    public static String stringOf(@Nullable Location location) {
        return location == null
                ? "NoLoc"
                : String.format(Locale.US, "%.2f,%.2f %s",
                location.getLatitude(), location.getLongitude(), location.getProvider());
    }


    /**
     * Eliminate noise and collapse the accuracy to two digits (about 1.11km at the equator)
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
    public static final double PRECISION = 100;
    public static void adjustPrecision(@NonNull Location location) {
        location.setLatitude(adjustPrecision(location.getLatitude()));
        location.setLongitude(adjustPrecision(location.getLongitude()));
    }
    public static double adjustPrecision(double latlng) {
        return Math.round(latlng * PRECISION) / PRECISION;
    }


    public static double kilometersBetween(@NonNull Location loc1, @NonNull Location loc2) {
        return kilometersBetweenLatLng(loc1.getLatitude(), loc1.getLongitude(), loc2.getLatitude(), loc2.getLongitude());
    }

    /**
     * @return true if distance less than 1km
     */
    public static boolean isNear(double latDeg1, double lngDeg1, double latDeg2, double lngDeg2) {
        double km = kilometersBetweenLatLng(latDeg1, lngDeg1, latDeg2, lngDeg2);
        return (km < Constants.GPS_LOCATION_TOLERANCE_KM);
    }
    /**
     * @return true if distance less than 0.1km
     */
    public static boolean isSimilar(double latDeg1, double lngDeg1, double latDeg2, double lngDeg2) {
        double km = kilometersBetweenLatLng(latDeg1, lngDeg1, latDeg2, lngDeg2);
        return (km < 0.1);
    }
    public  static double kilometersBetweenLatLng(double latDeg1, double lngDeg1, double latDeg2, double lngDeg2) {
        double dValue = Math.sin(Math.toRadians(latDeg1))
                * Math.sin(Math.toRadians(latDeg2))
                + Math.cos(Math.toRadians(latDeg1))
                * Math.cos(Math.toRadians(latDeg2))
                * Math.cos(Math.toRadians(lngDeg2 - lngDeg1));

        // Fix to work around Android math error when same points [30.44, -91.19] generate NaN.
        // Clamp dValue to <= 1.0 to prevent Not-a-number
        double ans = Constants.EARTH_RADIUS_METERS / 1000.0 * Math.acos(Math.min(dValue, 1.0));
        if (Double.isNaN(ans)) {
            return 0;
        }

        return ans;
    }
}
