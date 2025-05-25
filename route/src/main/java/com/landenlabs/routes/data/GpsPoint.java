/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.data;

import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;

import com.weather.pangea.util.measurements.SpeedUnit;
import com.wsi.mapsdk.utils.WLatLng;

import org.joda.time.DateTime;

import java.util.Locale;

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

/**
 * Hold a GPS data point.
 */
public class GpsPoint {

    public static final GpsPoint NO_POINT = new GpsPoint(0, 0, 0);
    public static final String GPS_TIME_FMT = "HH:mm:ss";

    public long milli;
    public double latitude;
    public double longitude;

    public float accuracyRadiusMeters = 0;
    public float metersPerSecond = 0;
    public double altitudeMeters = 0;
    public float bearingDegrees = 0;
    public char provider = 'g';   // fuse, network, gps, none.

    public GpsPoint() {
        milli = 0;
        latitude = longitude = 0;
    }
    public GpsPoint(double latitude, double longitude) {
        this.milli = 0;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    public GpsPoint(long milli, double latitude, double longitude) {
        this.milli = milli;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    public GpsPoint(@NonNull  Location location) {
        this.milli = location.getTime();
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        accuracyRadiusMeters = location.getAccuracy();
        metersPerSecond = location.getSpeed();
        altitudeMeters = location.getAltitude();
        bearingDegrees = location.getBearing();
        switch (location.getProvider()) {
            case LocationManager.GPS_PROVIDER:
                provider = 'g';
                break;
            case LocationManager.FUSED_PROVIDER:
                provider = 'f';
                break;
            case LocationManager.NETWORK_PROVIDER:
                provider = 'n';
                break;
            case LocationManager.PASSIVE_PROVIDER:
                provider = 'p';
                break;
            default:
                provider = '-';
                break;
        }
    }
    public GpsPoint(@NonNull GpsPoint other) {
        milli = other.milli;
        latitude = other.latitude;
        longitude = other.longitude;
        accuracyRadiusMeters = other.accuracyRadiusMeters;
        metersPerSecond = other.metersPerSecond;
        altitudeMeters = other.altitudeMeters;
        bearingDegrees = other.bearingDegrees;
        provider = other.provider;
    }

    @NonNull
    public GpsPoint clone() {
        return new GpsPoint(this);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.US, "GpsPoint(%s %.3f,%.3f)",
                new DateTime(milli).toString("h:m a"),
                latitude, longitude);
    }

    @NonNull
    public String logString() {
        return String.format(Locale.US, "%s %7.3f,%8.3f %c %2.0f %2.0f",
                new DateTime(milli).toString(GPS_TIME_FMT),
                latitude,
                longitude,
                provider,
                SpeedUnit.METERS_PER_SECOND.toMilesPerHour(metersPerSecond),
                accuracyRadiusMeters
                );
    }

    public WLatLng toLatLng() {
        return new WLatLng(latitude, longitude);
    }
}
