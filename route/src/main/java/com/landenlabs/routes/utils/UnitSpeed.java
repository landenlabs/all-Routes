/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.utils;

/**
 * Speed unit conversion.
 */
public enum UnitSpeed {

    KILOMETERS_PER_HOUR {
        public double toMetersPerSecond(double d) {  return d / 3.6f;   }
        public double toKilometersPerHour(double d) {  return d;  }
        public double toMilesPerHour(double d) {  return d * MPH_PER_KMH;  }
    },
    METERS_PER_SECOND {
        public double toMetersPerSecond(double d) {  return d;   }
        public double toKilometersPerHour(double d) {  return d * 3.6f;  }
        public double toMilesPerHour(double d) {  return d * MILEHOURS_PER_METERSEC;  }
    },
    MILES_PER_HOUR {
        public double toMetersPerSecond(double d) { return d / MILEHOURS_PER_METERSEC; }
        public double toKilometersPerHour(double d) {  return d / MPH_PER_KMH;  }
        public double toMilesPerHour(double d) {   return d; }
    };

    static final double MILEHOURS_PER_METERSEC = 2.23694;
    static final double MPH_PER_KMH = 0.621371;

    public double toMilesPerHour(double duration) {
        throw new AbstractMethodError();
    }

    public double toMetersPerSecond(double duration) {
        throw new AbstractMethodError();
    }
    public double toKilometersPerHour(double duration) {
        throw new AbstractMethodError();
    }

}
