package com.landenlabs.gpx_lib.domain;

/**
 * A way point (wpt element).
 */
public class WayPoint extends Point {

    private WayPoint(Builder builder) {
        super(builder);
    }

    public static class Builder extends Point.Builder {

        @Override
        public WayPoint build() {
            return new WayPoint(this);
        }
    }

}
