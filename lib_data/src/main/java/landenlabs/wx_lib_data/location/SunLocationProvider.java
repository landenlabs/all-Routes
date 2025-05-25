/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.wx_lib_data.location;

import static landenlabs.wx_lib_data.utils.ParseJson.getJsonDoubleList;
import static landenlabs.wx_lib_data.utils.ParseJson.getJsonStringList;
import static landenlabs.wx_lib_data.utils.UtilNet.loadResourceAsString;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wsi.wxdata.WxLocation;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import landenlabs.wx_lib_data.Constants;
import landenlabs.wx_lib_data.logger.ALog;
import landenlabs.wx_lib_data.utils.ParseJson;

/**
 * Perform SUN Location REST api calls
 *
 * Example
 *   https://api.weather.com/v3/location/search?query=atlanta&locationType=city&language=en-US&format=json&apiKey=<API_KEY>
 *   https://api.weather.com/v3/location/search?query=rdg&locationType=locid&language=en-US&format=json&apiKey=<API_KEY>
 *   https://api.weather.com/v3/location/point?geocode=42.788,-71.20&language=en-US&format=json&apiKey=<API_KEY>
 *
 * Documentation
 *   https://docs.google.com/document/d/1xSpijI9MgWWfHaFX4wo_tB0GjtNeHZqGyp3XVOaAPl4/edit
 */
public class SunLocationProvider {
    private static final String TAG = SunLocationProvider.class.getSimpleName();

    private static final String URL_ANY_QUERY =
            "https://api.weather.com/v3/location/search?query=%s&language=%s&format=json&apiKey=%s&locationType=city,poi,address";
    private static final String URL_LAT_LNG_AS_CITY =
            "https://api.weather.com/v3/location/point?geocode=%s,%s&language=%s&format=json&apiKey=%s&locationType=city"; // &locationType=poi
    private static final String URL_LAT_LNG_AS_POI =
            "https://api.weather.com/v3/location/point?geocode=%s,%s&language=%s&format=json&apiKey=%s&locationType=poi,address,city";
    private static final String URL_CITY_QUERY =
            "https://api.weather.com/v3/location/search?query=%s&locationType=city&language=%s&format=json&apiKey=%s";

    // Required to use class methods.
    public static class ApiInfo {
        public final String apiKey;
        public final StringBuffer langAbbr; // StringBuffer so it can be shared and modified.
        public ApiInfo(@NonNull String apiKey, @NonNull StringBuffer langAbbr) {
            this.apiKey = apiKey;
            this.langAbbr = langAbbr;
        }
    }

    // ---------------------------------------------------------------------------------------------
    @NonNull
    public static List<WxLocation> getSunLocationsAny(@NonNull String city, @NonNull ApiInfo apiInfo, @Nullable SLatLng nearLoc) {
        List<WxLocation> list = new ArrayList<>();
        String url = String.format(URL_ANY_QUERY, city, apiInfo.langAbbr.toString(), apiInfo.apiKey)
                + optionalProximity(nearLoc);
        try {
            String jsonStr = loadResourceAsString(url, Constants.UTF_8);
            parseSunLocations(list, new JSONObject(jsonStr));
            // ALog.i().tagMsg(TAG, "SUN Locations - cities cnt=", list.size(), " for ", url);     // DEBUG - remove this
        } catch (Exception ex) {
            ALog.e.tagMsg(TAG, "Failed for ", url, ex);
        }

        return list;
    }

    // Example
    //   https://api.weather.com/v3/location/search?query=atlanta&locationType=city&language=en-US&format=json&apiKey=<API_KEY>
    //
    //   https://api.weather.com/v3/location/search?query=rdg&locationType=locid&language=en-US&format=json&apiKey=<API_KEY>
    //
    // Documentation
    //   https://docs.google.com/document/d/1xSpijI9MgWWfHaFX4wo_tB0GjtNeHZqGyp3XVOaAPl4/edit
    //
    //  locationType  address, city, locality, locid, poi, pws, state
    //
    @NonNull
    public static List<WxLocation> getSunLocationsCity(@NonNull String city, @NonNull ApiInfo apiInfo, @Nullable SLatLng nearLoc) {
        List<WxLocation> list = new ArrayList<>();
        String url = String.format(URL_CITY_QUERY, city, apiInfo.langAbbr.toString(), apiInfo.apiKey)
                + optionalProximity(nearLoc);
        try {
            String jsonStr = loadResourceAsString(url, Constants.UTF_8);
            parseSunLocations(list, new JSONObject(jsonStr));
            // ALog.i().tagMsg(TAG, "SUN Locations - cities cnt=", list.size(), " for ", url);     // DEBUG - remove this
        } catch (Exception ex) {
            ALog.e.tagMsg(TAG, "Failed for ", url, ex);
        }

        return list;
    }

    private static String optionalProximity(@Nullable SLatLng nearLoc) {
        return (nearLoc != null)
                ? String.format(Locale.US, "&proximity=%.2f,%.2f", nearLoc.latitude, nearLoc.longitude)
                : "";
    }

    /**
     * @return zero or more City or POI near requested position.
     *
     * Atlanta, GA
     *   https://api.weather.com/v3/location/point?geocode=33.74,-84.39&language=en-US&format=json&apiKey=<API_KEY>
     * Salem, NH
     *   https://api.weather.com/v3/location/point?geocode=42.788,-71.20&language=en-US&format=json&apiKey=<API_KEY>
     */
    @NonNull
    public static List<WxLocation> getSunLocationNearPoint(@NonNull String latitudeStr, @NonNull String longitudeStr, @NonNull ApiInfo apiInfo) {
        String langAbbr = apiInfo.langAbbr.toString();
        String url = String.format(URL_LAT_LNG_AS_CITY, latitudeStr, longitudeStr, langAbbr, apiInfo.apiKey);
        List<WxLocation> locations = getSunLocationNearPoint(url);
        if (locations.isEmpty()) {
            ALog.w.tagMsg(TAG, "GPS city lookup failed, use POI lookup");
            url = String.format(URL_LAT_LNG_AS_POI, latitudeStr, longitudeStr, langAbbr, apiInfo.apiKey);
            locations = getSunLocationNearPoint(url);
        }
        return locations;
    }

    /**
     * @return zero or one POI orCity near requested position.
     *
     * Atlanta, GA
     *   https://api.weather.com/v3/location/point?geocode=33.74,-84.39&language=en-US&format=json&apiKey=<API_KEY>
     * Salem, NH
     *   https://api.weather.com/v3/location/point?geocode=42.788,-71.20&language=en-US&format=json&apiKey=<API_KEY>
     */
    @NonNull
    public static List<WxLocation> getSunLocationPoi(@NonNull String latitudeStr, @NonNull String longitudeStr, @NonNull ApiInfo apiInfo) {
        String langAbbr = apiInfo.langAbbr.toString();
        String url = String.format(URL_LAT_LNG_AS_POI, latitudeStr, longitudeStr, langAbbr, apiInfo.apiKey);
        List<WxLocation> locations = getSunLocationNearPoint(url);
        return locations;
    }

    /**
     * @return zero or one location near requested position.
     */
    @NonNull
    private static List<WxLocation> getSunLocationNearPoint(@NonNull String url) {
        List<WxLocation> list = new ArrayList<>();
        try {
            String jsonStr = loadResourceAsString(url, Constants.UTF_8);
            WxLocation loc = parseSunLocationPoint( new JSONObject(jsonStr));
            if (loc != null) {
                list.add(loc);
            }
        } catch (Exception ex) {
            ALog.e.tagMsg(TAG, "Failed for ", url, ex);
        }

        return list;
    }

    /* Sample
            "latitude": 42.788,
            "longitude": -71.201,
            "city": "Salem",
            "locale": {
            "locale1": "Rockingham County",
                "locale2": "Salem",
                "locale3": null,
                "locale4": null
            },
            "neighborhood": null,
            "adminDistrict": "New Hampshire",
            "adminDistrictCode": "NH",
            "postalCode": "03079",
            "postalKey": "03079:US",
            "country": "United States",
            "countryCode": "US",
            "ianaTimeZone": "America\/New_York",
            "displayName": "Salem",
            "dstEnd": "2022-11-06T01:00:00-0500",
            "dstStart": "2022-03-13T03:00:00-0400",
            "dmaCd": "506",
            "placeId": "4cf4e20ba23043bb5a5b8da65e64e617b8e1d40526fd0983cf302f37078105d2",
            "disputedArea": false,
            "disputedCountries": null,
            "disputedCountryCodes": null,
            "disputedCustomers": null,
            "disputedShowCountry": [false],
            "canonicalCityId": "06592a03e9d2b403c16a8252925177010ec4d45e95414d57e2dfa2d2996c2232",
            "countyId": "NHC015",
            "locId": "USNH0199:1:US",
            "locationCategory": null,
            "pollenId": "2B2",
            "pwsId": "KNHSALEM63",
            "regionalSatellite": "ne",
            "tideId": "8440889",
            "type": "postal",
            "zoneId": "NHZ013"
    */
    @Nullable
    private static WxLocation parseSunLocationPoint(@NonNull JSONObject jsonObj) {
        WxLocation wxLocation = null;
        JSONObject locationJObj = ParseJson.getJSONObject(jsonObj, "location");
        if (locationJObj != null) {
            wxLocation = new WxLocation(
                    ParseJson.getString(locationJObj, "displayName", ""),
                    ParseJson.getString(locationJObj, "city", ""),
                    ParseJson.getString(locationJObj, "adminDistrict", ""), // state code
                    ParseJson.getString(locationJObj, "adminDistrictCode", ""), // state code
                    ParseJson.getString(locationJObj, "countryCode", ""),
                    ParseJson.getString(locationJObj, "postalCode", ""),
                    // UtilGps.adjustPrecision(ParseJson.getFloat(locationJObj, "latitude", 0.0f)),
                    // UtilGps.adjustPrecision(ParseJson.getFloat(locationJObj, "longitude", 0.0f)),
                    ParseJson.getFloat(locationJObj, "latitude", 0.0f),
                    ParseJson.getFloat(locationJObj, "longitude", 0.0f),
                    ParseJson.getString(locationJObj, "ianaTimeZone", ""),
                    ParseJson.getStringIf(locationJObj, "address", ""),
                    ParseJson.getString(locationJObj, "type", "")
            );
        }
        return wxLocation;
    }

    @NonNull
    private static  <E extends Number> E get(ArrayList<E> list, int idx, E defValue) {
        return (list != null && idx >= 0 && idx < list.size() && list.get(idx) != null)
                ? list.get(idx) : defValue;
    }
    @NonNull
    private static  String get(ArrayList<String> list, int idx, String defValue) {
        return (list != null && idx >= 0 && idx < list.size() && list.get(idx) != null)
                ? list.get(idx).replace("null", defValue) : defValue;
    }

    private static void parseSunLocations(@NonNull List<WxLocation> list, @NonNull JSONObject jsonObj) {

        JSONObject locationJObj = ParseJson.getJSONObject(jsonObj, "location");
        if (locationJObj != null) {
            ArrayList<String> displayName = getJsonStringList(locationJObj, "displayName");
            ArrayList<String> city = getJsonStringList(locationJObj, "city");
            ArrayList<String> adminDistrict = getJsonStringList(locationJObj, "adminDistrict"); // state name
            ArrayList<String> adminDistrictCode = getJsonStringList(locationJObj, "adminDistrictCode"); // state code
            // ArrayList<String> country = getJsonStringList(locationJObj, "country");
            ArrayList<String> countryCode = getJsonStringList(locationJObj, "countryCode");
            ArrayList<String> postalCode = getJsonStringList(locationJObj, "postalCode");
            ArrayList<Double> latitude = getJsonDoubleList(locationJObj, "latitude");
            ArrayList<Double> longitude = getJsonDoubleList(locationJObj, "longitude");
            ArrayList<String> zoneId = getJsonStringList(locationJObj, "ianaTimeZone");
            ArrayList<String> address = getJsonStringList(locationJObj, "address");
            ArrayList<String> locType = getJsonStringList(locationJObj, "type");

            for (int idx = 0; idx < city.size(); idx++) {
                double lat = get(latitude, idx, Double.valueOf(0));
                double lng = get(longitude, idx, Double.valueOf(0));
                String adminNm = get(adminDistrict, idx, "");

                WxLocation wsiLocation = new WxLocation(
                        get(displayName, idx, adminNm),
                        get(city, idx, ""),
                        get(adminDistrict, idx, ""),
                        get(adminDistrictCode, idx, ""), // state code
                        get(countryCode, idx, ""),
                        get(postalCode, idx, ""),
                        lat,
                        lng,
                        get(zoneId, idx, ""),
                        get(address, idx, ""),
                        get(locType, idx, "")
                       );

                list.add(wsiLocation);
            }
        }
    }
}