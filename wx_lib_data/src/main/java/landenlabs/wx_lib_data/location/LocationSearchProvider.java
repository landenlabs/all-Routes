/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package landenlabs.wx_lib_data.location;


import static landenlabs.wx_lib_data.location.SunLocationProvider.getSunLocationNearPoint;
import static landenlabs.wx_lib_data.location.SunLocationProvider.getSunLocationsAny;
import static landenlabs.wx_lib_data.location.SunLocationProvider.getSunLocationsCity;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.widget.EditText;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.MutableLiveData;

import com.wsi.wxdata.WxLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import landenlabs.wx_lib_data.logger.ALog;

/**
 * An auxiliary class that is used to get locations from the server by
 * specifying different search parameters.
 *
 * Sun end-points:
 *  http://api.weather.com/v3/location/search?query=dunkin&language=en-US&locationType=poi&format=json&proximity=33.54,-84.3
 *  https://api.weather.com/v3/location/search?query=salem&language=en-US&locationType=poi,city&format=json&proximity=33.54,-84.3&apiKey=
 *
 * Sun Documentation:
 *  Query
 *      https://docs.google.com/document/d/1xSpijI9MgWWfHaFX4wo_tB0GjtNeHZqGyp3XVOaAPl4/edit#
 *  Geocode
 *      https://docs.google.com/document/d/14BKNJwPiU8T6UNFBzPn5NuNcAJjFcSWmMIc2TSqg51Q/edit#
 *
 */
public class LocationSearchProvider  {

    private static SunLocationProvider.ApiInfo apiInfo;
    private SearchThread searchThread;

    private static final int MAX_CACHE_SIZE = 10;   // Keep cache small to avoid false hashed key hits.
    private static final LinkedHashMap<String, List<WxLocation>> GPS_CACHE =
            new LinkedHashMap<String, List<WxLocation>>() {
                protected boolean removeEldestEntry(Entry<String, List<WxLocation>> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            };


    // ---------------------------------------------------------------------------------------------

    /**
     * Create search provider.
     * @param langAbbr  See WxDataHolder.settings.language.apiValue,  ex: en-US
     */
    public LocationSearchProvider(@NonNull Context context, @NonNull StringBuffer langAbbr) {
        apiInfo = new SunLocationProvider.ApiInfo(getString2x(context, "job1"), langAbbr);
    }

    @NonNull
    private static String getString2x(@NonNull Context context, @NonNull String name) {
        int keyRes = context.getResources().getIdentifier(name + "_x1", "string", context.getPackageName());
        if (keyRes > 0) {
            String value = context.getResources().getString(keyRes);
            // Reverse uuencoding of string.  See build.gradle for encoding.
            byte[] decoded = Base64.decode(value, Base64.DEFAULT);
            return new String(decoded);
        }
        return "";
    }

    public void done() {
        if (searchThread != null) {
            searchThread.interrupt();
            searchThread = null;
        }
        SearchThread.runCnt.set(0);
    }

    public MutableLiveData<String> observeEditText(EditText editText) {
        final MutableLiveData<String> subject = new MutableLiveData<>();
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence searchText, int start, int before, int count) {
                subject.postValue(searchText.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        return subject;
    }

    /**
     * Using Sun Location REST api, find places (cities and POI) near me.
     * @param searchString  Partial or full City name or Point of Interest
     * @param nearLoc  Optional location to prefer near by.
     */
    @MainThread
    @NonNull
    public MutableLiveData<List<WxLocation>> runSearchLocationOnThread(
            @Nullable Context context,
            @NonNull String searchString,
            @Nullable SLatLng nearLoc) {
        if (searchThread != null) {
            ALog.i.tagMsg(this, "Search active thread count=", SearchThread.runCnt.get());
            if (SearchThread.runCnt.get() > 1) {
                searchThread.interrupt();   // Stop last thread if 2 are running.
            }
        }

        MutableLiveData<List<WxLocation>> liveWxLocSearch = new MutableLiveData<>();
        searchThread = new SearchThread(context, searchString, nearLoc, liveWxLocSearch);
        searchThread.start();
        return liveWxLocSearch;
    }

    public boolean isIdle() {
        return searchThread == null || SearchThread.runCnt.get() == 0;
    }

    /**
     * Searching locations for the specified string. Depending on the specified
     * string, it performs the following searches:
     *   by latitude,longitude
     *   by city
     *   by business or address
     *
     * @param searchString
     *            {@code String} that contains search request
     * @return {@code List<Location>} with locations that match the search
     *         request, or {@code null} if nothing was found
     */
    @WorkerThread
    @Nullable
    public static List<WxLocation> searchThreadWorker(
            @Nullable Context context,
            @NonNull String searchString,
            @Nullable SLatLng nearLoc)   {

        if (TextUtils.isEmpty(searchString)) {
            return null;
        }

        searchString = searchString.trim();
        List<WxLocation> locations = null;

        // Determine if search string contains Laitude,Longitude
        StringTokenizer st = new StringTokenizer(searchString, ",");
        if (st.countTokens() == 2) {
            String part1 = st.nextToken().trim();   // Latitude
            String part2 = st.nextToken().trim();   // Longitude
            try {
                double lat = Float.parseFloat(part1);
                double lng = Float.parseFloat(part2);
                locations = findLocationNear(lat, lng);
            } catch (NumberFormatException ignore) {
                ALog.none.tagMsg(ALog.TAG_PREFIX, ignore);
            }
        }

        // If nothing found, do default city/POI query.
        if (locations == null || locations.isEmpty()) {
            locations = findLocations(searchString, nearLoc);
        }
        return locations;
    }

    /**
     * Return zero or one City or POI near latitude and longitude coordinates.
     * Cache values since they never change.
     */
    @NonNull
    @WorkerThread
    private static List<WxLocation> findLocationNear( double latitude, double longitude)  {
        String latStr = String.format(Locale.US, "%.4f", latitude);
        String lngStr = String.format(Locale.US, "%.4f", longitude);

        String cacheKey = latStr + "," + lngStr;
        List<WxLocation> locations;

        synchronized (GPS_CACHE) {
            if (GPS_CACHE.containsKey(cacheKey)) {
                ALog.d.tagMsg("find GPS return cache key=", cacheKey);
                return GPS_CACHE.get(cacheKey);
            }

            locations = getSunLocationNearPoint(latStr, lngStr, apiInfo);
            if (!locations.isEmpty()) {
                GPS_CACHE.put(cacheKey, locations);
            }
        }
        return locations;
    }

    /**
     * Thread method to find City, POI or Address near proximity location.
     */
    @NonNull
    @WorkerThread
    private static List<WxLocation> findLocations(String searchString, @Nullable SLatLng nearLoc)  {
        List<WxLocation> locations = getSunLocationsAny(searchString, apiInfo, nearLoc);
        if (locations.size() < 4) {
            List<WxLocation> cities = getSunLocationsCity(searchString, apiInfo, nearLoc);
            locations.addAll(cities);
        }
        return locations;
    }

    // =============================================================================================
    static class SearchThread extends Thread {
        private Context context;
        private MutableLiveData<List<WxLocation>> liveWxLocSearch;
        private final String searchString;
        private final SLatLng nearLoc;

        public final static AtomicInteger runCnt  = new AtomicInteger(0);

        SearchThread(@Nullable Context context, @NonNull String searchString, @Nullable SLatLng nearLoc,
                MutableLiveData<List<WxLocation>> liveWxLocSearch) {
            this.context = context;
            this.searchString = searchString;
            this.nearLoc = nearLoc;
            this.liveWxLocSearch = liveWxLocSearch;
            runCnt.incrementAndGet();
        }
        @Override
        public void run() {
            List<WxLocation> locList = searchThreadWorker(context, searchString, nearLoc);
            liveWxLocSearch.postValue(locList);
            liveWxLocSearch = null;
            context = null;
            runCnt.decrementAndGet();
        }
    }
}
