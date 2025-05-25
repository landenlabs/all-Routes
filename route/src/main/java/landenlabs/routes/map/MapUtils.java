/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package com.landenlabs.routes.map;


import static com.landenlabs.routes.utils.GpsUtils.metersBetween;

import androidx.annotation.NonNull;

import com.landenlabs.routes.data.GpsPoint;
import com.wsi.mapsdk.utils.WLatLng;

/**
 * ToDo -move into single GPS/GIS/Util class
 */
public class MapUtils {

    /**
     * isSimilar() is replacement for isEqual() to handle variability from GPS while standing still.
     *
     * @return true if distance less than  0.1km
     *
     *      https://gis.stackexchange.com/questions/8650/measuring-accuracy-of-latitude-and-longitude
     *      places  degrees      N/S or E/W     E/W at         E/W at       E/W at
     *      at                   equator        lat=23N/S      lat=45N/S    lat=67N/S
     *      ------- -------      ----------     ----------     ---------    ---------
     *      0       1            111.32 km      102.47 km      78.71 km     43.496 km
     *      1       0.1          11.132 km      10.247 km      7.871 km     4.3496 km
     *      2       0.01         1.1132 km      1.0247 km      787.1 m      434.96 m
     *      3       0.001        111.32 m       102.47 m       78.71 m      43.496 m
     *      4       0.0001       11.132 m       10.247 m       7.871 m      4.3496 m
     */
    public static boolean isSimilar(@NonNull GpsPoint gp1, @NonNull GpsPoint gp2) {
        return isSimilar(gp1.latitude, gp1.longitude, gp2.latitude, gp2.longitude);
    }
    public static boolean isSimilar(@NonNull WLatLng gp1, @NonNull WLatLng gp2) {
        return isSimilar(gp1.latitude, gp1.longitude, gp2.latitude, gp2.longitude);
    }
    public static boolean isSimilar(double latDeg1, double lngDeg1, double latDeg2, double lngDeg2) {
        double meters = metersBetween(latDeg1, lngDeg1, latDeg2, lngDeg2);
        return (meters < 100);
    }
}
