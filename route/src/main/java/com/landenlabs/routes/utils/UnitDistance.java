/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.utils;

/**
 * Distance unit conversion.
 */
public enum UnitDistance {

    METERS {
        public double toMeters(double d) {
            return d;
        }
        public double toMiles(double d) {
            return d / METERS_PER_MILES;
        }
    },
    MILES {
        public double toMeters(double d) {
            return d * METERS_PER_MILES;
        }
        public double toMiles(double d) {
            return d;
        }
    };

    static final double METERS_PER_MILES = 1609.34;

    public double toMiles(double duration) {
        throw new AbstractMethodError();
    }

    public double toMeters(double duration) {
        throw new AbstractMethodError();
    }

}
