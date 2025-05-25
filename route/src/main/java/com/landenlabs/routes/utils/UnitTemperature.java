/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.utils;

/**
 * Temperature unit conversion.
 */
public enum UnitTemperature {

    CELSIUS {
        public double toCelsius(double celsius) {
            return celsius;
        }
        public double toFahrenheit(double celsius) {
            return 1.8f * celsius + 32;
        }
    },
    FAHRENHEIT {
        public double toCelsius(double fahrenheit) {
            return (fahrenheit - 32) / 1.8f;
        }
        public double toFahrenheit(double fahrenheit) {
            return fahrenheit;
        }
    };

    static final double METERS_PER_MILES = 1609.34;

    public double toCelsius(double duration) {
        throw new AbstractMethodError();
    }

    public double toFahrenheit(double duration) {
        throw new AbstractMethodError();
    }

}
