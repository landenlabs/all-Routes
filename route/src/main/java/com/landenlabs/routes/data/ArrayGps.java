/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.data;


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
