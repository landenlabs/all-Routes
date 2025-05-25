/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.wx_lib_data;

import java.util.TimeZone;

/**
 * Global app constants
 */
public class Constants {
    private Constants() {
    }

    // public static final Locale DEF_LOCALE = Locale.US;
    // public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    public static final TimeZone GMT = TimeZone.getTimeZone("GMT");
    public static final String UTF_8 = "UTF-8";
    public static final double GPS_LOCATION_TOLERANCE_KM = 1;   // Push and headline GPS distance tolerance.
    public static final float NOT_A_NUMBER_F = Float.NaN;
    public static final float EARTH_RADIUS_METERS = 6378137f; // meters WGS84 Major axis

    /*
     https://gis.stackexchange.com/questions/8650/measuring-accuracy-of-latitude-and-longitude
     places  degrees      N/S or E/W     E/W at         E/W at       E/W at
     at                   equator        lat=23N/S      lat=45N/S    lat=67N/S
     ------- -------      ----------     ----------     ---------    ---------
     0       1            111.32 km      102.47 km      78.71 km     43.496 km
     1       0.1          11.132 km      10.247 km      7.871 km     4.3496 km
     2       0.01         1.1132 km      1.0247 km      787.1 m      434.96 m
     3       0.001        111.32 m       102.47 m       78.71 m      43.496 m
     4       0.0001       11.132 m       10.247 m       7.871 m      4.3496 m

        See WxData's  WxLocation.latLngStr() method.
    */
    public static final String FMT_LAT_LNG = "%.2f,%.2f";

    // General
    public static final Character BLANK_CHAR = ' ';
    public static final String STRING_EMPTY = "";
    public static final String STRING_WHITE_SPACE = " ";
    public static final String HOURLY_QUALIFIER_SEPARATOR = "_";
    public static final String PERCENTAGE = "%";
    public static final String DEGREES = "º";
    public static final String NUMERIC_FORMAT = "%d";
    public static final String DRAWABLE = "drawable";
    public static final String WEATHER_FORECAST_ICON_FORMAT = "wx_sun_%02d%c";

    // Views (internal constants not part of any UI)
    public static final String HOME_PAGE = "home";
    public static final String RADAR_PAGE = "radar";
    public static final String HOURLY_PAGE = "hourly";
    public static final String DAILY_PAGE = "daily";
    public static final String DEV_PAGE = "dev";
    public static final String SEARCH_PAGE = "search";
    public static final String SETTINGS_PAGE = "settings";

    // Bundle Extras Keys
    public static final String DAILY_DEEP_DIVE_DAY_ID = "DAILY_DEEP_DIVE_DAY_ID";
    public static final String DAILY_DEEP_DIVE_DATE = "DAILY_DEEP_DIVE_DATE";
    public static final String LOCATION_PAGE_FLAG = "LOCATION_PAGE_FLAG";

    // Insights
    public static final String SHORT_TERM_PRECIP_INSIGHT = "shortTermPrecipInsight";
    public static final String THUNDERSTORM_SOON_INSIGHT = "thunderstormSoonInsight";
    public static final String TEMPERATURE_CHANGE_INSIGHT = "temperatureChangeInsight";
    public static final String SNOW_INSIGHT = "snowInsight";
    public static final String CHANGE_PRECIP_INSIGHT = "chancePrecipInsight";
    public static final String RECORD_TEMP_INSIGHT = "recordTempInsight";
    public static final String FEELS_LIKE_INSIGHT = "feelsLikeInsight";
    public static final String POLLEN_INSIGHT = "pollenInsight";
    public static final String WIND_INSIGHT = "windInsight";
    public static final String SUNNY_DAY_INSIGHT = "sunnyDayInsight";
    public static final String UV_INSIGHT = "uvInsight";
    public static final String PRECIP_INSIGHT = "precipInsight";
    public static final String SNOW_NEAR_INSIGHT = "snowNearInsight";
    public static final String SEVERE_STORM_INSIGHT = "severeStormInsight";
    public static final String PLAN_YOUR_DAY__INSIGHT = "planYourDayInsight";
    public static final String WEEKEND_INSIGHT = "weekendInsight";
    public static final String SUNBURN_RISK__INSIGHT = "sunburnRiskInsight";
    public static final String MIGRAINE_RISK_INSIGHT = "migraineRiskInsight";
    public static final String VITAMIN_D_RISK_INSIGHT = "vitaminDRiskInsight";
    public static final String OUTDOOR_GRILL_INSIGHT = "outdoorGrillInsight";
    public static final String NICEST_DAY_WEEK_INSIGHT = "nicestDayWeekInsight";
    public static final String ENERGY_INSIGHT = "energyInsight";

}
