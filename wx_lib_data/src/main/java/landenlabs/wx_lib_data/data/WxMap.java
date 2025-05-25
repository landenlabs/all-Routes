/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package landenlabs.wx_lib_data.data;


import static landenlabs.wx_lib_data.logger.LibLog.LOG_CACHE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wsi.wxdata.BaseWeatherData;
import com.wsi.wxdata.WxLocation;

import landenlabs.wx_lib_data.utils.UtilGps;


/**
 * Custom HashMap.
 * <p/>
 * Get interface -
 * Matches on exact and near match (see MAX_DEGREES)
 * <p/>
 * Put interface -
 * Remove any near matches
 * Remove old items if count > MAX_ITEMS
 * Add item.
 * <p/>
 * Eliminate noise and collapse the accuracy to two digits (about 1.11km at the equator)
 * <p>
 * https://gis.stackexchange.com/questions/8650/measuring-accuracy-of-latitude-and-longitude
 * places  degrees      N/S or E/W     E/W at         E/W at       E/W at
 * at                   equator        lat=23N/S      lat=45N/S    lat=67N/S
 * ------- -------      ----------     ----------     ---------    ---------
 * 0       1            111.32 km      102.47 km      78.71 km     43.496 km
 * 1       0.1          11.132 km      10.247 km      7.871 km     4.3496 km
 * 2       0.01         1.1132 km      1.0247 km      787.1 m      434.96 m
 * 3       0.001        111.32 m       102.47 m       78.71 m      43.496 m
 * 4       0.0001       11.132 m       10.247 m       7.871 m      4.3496 m
 */
public class WxMap<T2 extends BaseWeatherData> {
    private final static int DEF_MAX_ITEMS = 20;
    public final long maxAgeMilli;

    private T2 gpsData;
    private T2 dstData;


    public WxMap(long maxAgeMilli) {
        this.maxAgeMilli = maxAgeMilli;
    }

    public void clear() {
        synchronized (this) {
            gpsData = dstData = null;
        }
    }

    public int size() {
        int gpsCnt = gpsData == null ? 0 : 1;
        int dstCnt = dstData == null ? 0 : 1;
        return gpsCnt + dstCnt;
    }

    /**
     * WARNING - must synchronized on access data.
     */
    public T2 getGpsData() {
        return gpsData;
    }
    public T2 getDstData() {
        return dstData;
    }

    /**
     * Store new item, remove near items and old items.
     */

    public void put(@NonNull WxLocation loc, T2 item) {
        synchronized (this) {
            String dbgLocStr = landenlabs.wx_lib_data.location.WxLocationEx.getLatLngStr(loc);
            // DEBUG hack
            item.setAuxData(item.getTime(), item.getLocation(), item.getUnit(), item.getLanguage(), "cached");

            // 2. Add new item
            if (landenlabs.wx_lib_data.location.WxLocationEx.isGPS(loc)) {
                gpsData = item;
            } else {
                dstData = item;
            }
            LOG_CACHE.d().tagMsg(this, "Cache of ", item.getClass().getSimpleName(), " loc=", dbgLocStr);
        }
    }

    @Nullable
    public T2 get(@Nullable WxLocation loc) {
        if (loc == null)
            return null;

        synchronized (this) {
            T2 data = landenlabs.wx_lib_data.location.WxLocationEx.isGPS(loc) ? gpsData : dstData;
            data = validate(data);
            if (data != null) {
                if (isNear(loc, data.getLocation())) {
                    return data;
                }
            }
        }
        return null;
    }

    public static boolean isNear(@Nullable WxLocation loc1, @Nullable WxLocation loc2) {
        if (loc1 == null || loc2 == null) {
            return loc1 == loc2;
        }
        return UtilGps.isNear(loc1.getLatitude(), loc1.getLongitude(), loc2.getLatitude(), loc2.getLongitude());
    }

    public boolean containsKey(@Nullable WxLocation key) {
        return get(key) != null;
    }

    public void remove(@Nullable WxLocation loc) {
        if (loc != null) {
            synchronized (this) {
                if (landenlabs.wx_lib_data.location.WxLocationEx.isGPS(loc)) {
                    gpsData = null;
                } else {
                    dstData = null;
                }
            }
        }
    }

    /*
    public void clearIfNot(String langAbbr, boolean isMetric) {
        gpsData = clearIfNot(gpsData, langAbbr, isMetric);
        dstData = clearIfNot(dstData, langAbbr, isMetric);
    }

    private T2 clearIfNot(T2 data, String langAbbr, boolean isMetric) {
        return validate(data) == null
                || isMetric(data.getUnit()) != isMetric
                || !data.getLanguage().apiValue().equals(langAbbr)
                ? null : data;
    }
     */

    public  T2 validate(@Nullable T2 data) {
        if (data == null)
            return null;

        long nowMilli = System.currentTimeMillis();
        long dataMilli = data.getLoadAtTime().resolveToMilli();
        long deltaMilli = nowMilli - dataMilli;
        return (deltaMilli < maxAgeMilli) ? data : null;
    }


}
