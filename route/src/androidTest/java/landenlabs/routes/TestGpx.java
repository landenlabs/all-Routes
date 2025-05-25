/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package com.landenlabs.routes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.res.AssetManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.landenlabs.gpx_lib.GPXParser;
import com.landenlabs.gpx_lib.domain.Gpx;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class TestGpx {
    // @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.landenlabs.routes", appContext.getPackageName());
    }

    @Test
    public void testGpxRoute1() throws IOException, XmlPullParserException {
        InputStream input = getAssets().open("route1.gpx");
        // https://github.com/ticofab/android-gpx-parser/tree/master
        Gpx gpx = new GPXParser().parse(input);
        assertNotNull(gpx); // testing that there is no crash, really
    }

    public AssetManager getAssets() {
        return InstrumentationRegistry.getInstrumentation().getContext().getAssets();
    }
}