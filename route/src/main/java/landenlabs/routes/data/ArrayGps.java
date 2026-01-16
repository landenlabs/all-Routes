/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.data;


/**
 * Array of GPS points in sorted milli time order,
 */
public class ArrayGps extends  ArrayListEx<GpsPoint> {

    public ArrayGps() {
    }
    public ArrayGps(int capacity) {
        super(capacity);
    }
    public ArrayGps(ArrayGps points) {
        super(points);
    }

    synchronized
    public void insert(GpsPoint gpsPoint) {
        for (int idx = 0; idx < size(); idx++) {
            if (gpsPoint.milli < get(idx).milli) {
                add(idx, gpsPoint);
                return;
            }
        }

        add(gpsPoint);
    }
}
