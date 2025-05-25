/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.wx_lib_data.location;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Simple holder of Latitude, Longitude
 * used by:
 *      LocationProvider
 *      SunLocationProvider
 */
public  class SLatLng {
    public float latitude;
    public float longitude;

    public SLatLng(float latitude, float longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.US, "%.2f,%.2f", latitude, longitude);
    }
}
