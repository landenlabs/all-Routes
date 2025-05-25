/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes;

import static org.junit.Assert.assertNotNull;

import android.content.Context;

import com.landenlabs.gpx_lib.GPXParser;
import com.landenlabs.gpx_lib.domain.Gpx;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

import landenlabs.wx_lib_data.logger.ALog;

/**
 * RoboElect Unit execute on local system (mocked)
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@Config(sdk = 28)  // Remove when project upgraded to Java 9
@RunWith(RobolectricTestRunner.class)
public class RE_TestGpx {
    private static final String TAG = RE_TestGpx.class.getSimpleName();
    Context appContext;

    // RoboElectric
    protected Context getAppContext() {
        return androidx.test.core.app.ApplicationProvider.getApplicationContext();
    }

    @Before
    public void setUp() {
        // boolean C.USE_DEBUG_PROVISIONING_INFO = true;
        ALog.MIN_LEVEL = ALog.DEBUG;
        appContext = getAppContext();
    }

    @Test
    public void testGpxRoute1() throws IOException, XmlPullParserException {
        InputStream input = getAppContext().getAssets().open("route1.gpx");
        // https://github.com/ticofab/android-gpx-parser/tree/master
        Gpx gpx = new GPXParser().parse(input);
        assertNotNull(gpx); // testing that there is no crash, really
    }


    public void sendMockGps() {
        /*
        https://stackoverflow.com/questions/2531317/how-to-mock-location-on-device/2587369#2587369
        locationProvider = FusedLocationProviderClient(context)
        locationProvider.setMockMode(true)

        val loc = Location(providerName)
        val mockLocation = Location(providerName) // a string
        mockLocation.latitude = latitude  // double
        mockLocation.longitude = longitude
        mockLocation.altitude = loc.altitude
        mockLocation.time = System.currentTimeMillis()
        mockLocation.accuracy = 1f
        mockLocation.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mockLocation.bearingAccuracyDegrees = 0.1f
            mockLocation.verticalAccuracyMeters = 0.1f
            mockLocation.speedAccuracyMetersPerSecond = 0.01f
        }
//        locationManager.setTestProviderLocation(providerName, mockLocation)
        locationProvider.setMockLocation(mockLocation)
         */
    }
}
