/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes;

import android.app.Activity;
import android.content.Context;

import androidx.lifecycle.LifecycleOwner;

import landenlabs.routes.data.ArrayGps;
import landenlabs.routes.data.GpsPoint;
import landenlabs.routes.data.Track;
import landenlabs.routes.db.FirebaseCloud;
import landenlabs.routes.db.SqlDb;

import landenlabs.wx_lib_data.logger.ALog;

public class Tester {

    public static void test1(Activity context) {
        ArrayGps route1 = new ArrayGps();
        long seconds = System.currentTimeMillis();
        route1.add(new GpsPoint(seconds + 100, 1.1, 123.45));
        route1.add(new GpsPoint(seconds + 101, 2.2, 123.45));
        route1.add(new GpsPoint(seconds + 102, 3.3, 123.45));

        Track t1 = new Track(101, "test", route1);
        String encStr = t1.getPathEncoded();
        ArrayGps route1Dup = Track.decodeGps(encStr);
        boolean match = route1.equals(route1Dup);
        ALog.d.tagMsg(context, "enc dec match=", match);
    }

    public static void test2(Context context, LifecycleOwner owner) {
        /*
        https://console.firebase.google.com/u/0/project/wsi-test-1/database/wsi-test-1/data
        https://firebase.google.com/docs/database/admin/retrieve-data
        https://firebase.google.com/docs/database/android/start#java
         */
        FirebaseCloud firebaseCloud = new FirebaseCloud();
        // Test Firebase RealTime database
        ArrayGps route1 = new ArrayGps();
        route1.add(new GpsPoint(100, 1.1, 123.45));
        route1.add(new GpsPoint(101, 2.2, 123.45));
        route1.add(new GpsPoint(102, 3.3, 123.45));
        firebaseCloud.saveRoute("route1", route1);
        firebaseCloud.getRoute("route1").observe(owner, t -> {
            ALog.d.tagMsg(context, "loaded list ", t);
        });
    }

    public static void test3(Activity context) {
        // Initialize the connection with the Database
        SqlDb gpsDataBase = new SqlDb(context);

        // Prepare the current track
        if (gpsDataBase.getLastTrackID() == 0) {
            ArrayGps route1 = new ArrayGps();
            long seconds = System.currentTimeMillis();
            route1.add(new GpsPoint(seconds + 100, 1.1, 123.45));
            route1.add(new GpsPoint(seconds + 101, 2.2, 123.45));
            route1.add(new GpsPoint(seconds + 102, 3.3, 123.45));

            Track t1 = new Track(101, "test", route1);
            gpsDataBase.addTrack(t1);                                          // Creation of the first track if the DB is empty
        }
        int trackId = gpsDataBase.getLastTrackID();
    }
}
