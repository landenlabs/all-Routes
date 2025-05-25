/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.pages.PagesSmartAux;

import static com.landenlabs.routes.utils.FmtTime.getNearest;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.routes.R;
import com.landenlabs.routes.data.ArrayListEx;
import com.landenlabs.routes.data.GpsPoint;
import com.weather.pangea.model.overlay.Icon;
import com.wsi.mapsdk.utils.DrawUtils;
import com.wsi.mapsdk.utils.WLatLng;
import com.wsi.wxdata.WxAlertHeadlines;
import com.wsi.wxdata.WxFifteenMinute;
import com.wsi.wxdata.WxLifestyleDriving;
import com.wsi.wxdata.WxLocation;
import com.wsi.wxdata.WxTime;

import org.joda.time.DateTime;

import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import landenlabs.wx_lib_data.logger.ALog;

/**
 * SmartAlert data holder.
 */
public class SmartAlert {

    public final GpsPoint pt;
    public final WxTime timeRange;
    public WxLocation wxLocation;
    public final int metersTraveled;
    public final DateTime dateTime;

    public boolean hasValidData = false;
    public boolean hasValidLoc = false;
    public WxAlertHeadlines.Alert alert = null;
    public float precipRate = 0;        // imperial inches/hour, metric cm?/hour
    public float snowRate = 0;          // imperial inches/hour, metric cm?/hour
    public float temperatureC = 0;
    public float windSpeedKm = 0;
    public DateTime drivingDifficultyTime = null;
    public int drivingDifficultyIndex = 0; //  index = 0=none, 1=windy, 2=foggy, 3=wet, 4=ponding, 5=snowy, 6=icy

    public static String[] DDI_NAMES = new String[] {
            "0=None",
            "1-windy",
            "2-foggy",
            "3-wet",
            "4-ponding",
            "5-snowy",
            "6-icy"
    };
    private static final int[] imageDDI7 = new int[] {
            R.drawable.smart_none,      // 0=none
            R.drawable.smart_wind,      // 1=windy
            R.drawable.smart_fog,       // 2=foggy
            R.drawable.smart_rain1,     // 3=wet
            R.drawable.smart_rain3,     // 4=ponding
            R.drawable.smart_snow,      // 5=snowy
            R.drawable.smart_ice        // 6=icy
    };
    private static final Icon[] iconDDI7 = new Icon[7];
    private static Icon iconError;
    private static Icon iconNone;

    public static final String SIG_WARNING = "W";
    public static final String SIG_WATCH = "A";
    public static final String SIG_ADVISORY = "Y";
    public static final String SIG_STATEMENT = "S";
    public static final long ALERT_MILLI_TOLERANCE = TimeUnit.MINUTES.toMillis(30);

    public static float DRIVE_SPEED_MPH = 60f;
    public static float KM_PER_MILE = 1.609f;
    public static float DRIVE_SPEED_KPH = DRIVE_SPEED_MPH * KM_PER_MILE;

    // ---------------------------------------------------------------------------------------------

    public SmartAlert(GpsPoint pt, WxTime timeRange, int metersTraveled) {
        this.pt = pt;
        this.timeRange = timeRange;
        this.wxLocation = createAt(pt.toLatLng());
        this.metersTraveled = metersTraveled;
        this.dateTime = new DateTime(pt.milli);
    }

    public static final String LOC_TYPE_CITY = "city";
    public static WxLocation createAt(@NonNull WLatLng gps) {
        return new WxLocation(
                "gps", "", "", "", Locale.getDefault().getCountry(), "",
                gps.getLatitude(), gps.getLongitude(),
                TimeZone.getDefault().getID(),
                "", LOC_TYPE_CITY);
    }

    public void setWeather(@Nullable WxFifteenMinute data) {
        ALog.d.tagMsg(this, "Store fifteen weather ", data);
        if (data != null) {
            int nearestIdx = getNearest(dateTime, data.validTimeLocal);
            precipRate = ArrayListEx.get(data.precipRate, nearestIdx, 0f);
            snowRate = ArrayListEx.get(data.snowRate, nearestIdx, 0f);
            temperatureC =  ArrayListEx.get(data.temperature, nearestIdx, 0);
            windSpeedKm = ArrayListEx.get(data.windSpeed, nearestIdx, 0);
        } else {
            precipRate = snowRate = 0;
            windSpeedKm = temperatureC = 0;
        }
    }

    public void setDriving(@Nullable WxLifestyleDriving data) {
        if (data != null) {
            if (data.drivingDifficultyIndex != null) {
                int nearestIdx = getNearest(dateTime, data.drivingDifficultyIndex.fcstValidLocal);
                drivingDifficultyTime = ArrayListEx.get(data.drivingDifficultyIndex.fcstValidLocal, nearestIdx, null);
                drivingDifficultyIndex = ArrayListEx.get(data.drivingDifficultyIndex.drivingDifficultyIndex, nearestIdx, 0);
            } else if (data.drivingDifficultyIndexCurrent != null) {
                drivingDifficultyIndex = data.drivingDifficultyIndexCurrent.drivingDifficultyIndex;
            }
        } else {
            drivingDifficultyIndex = 0;
        }
    }

    public void setAlert(@Nullable WxAlertHeadlines data) {
        if (data != null && data.alerts != null && data.alerts.size() > 0) {
            long ourMilli = dateTime.getMillis();
            for (WxAlertHeadlines.Alert newAlert : data.alerts) {
                long deltaMilli = Math.abs(ourMilli - newAlert.expireTimeLocal.getMillis());
                if (deltaMilli < ALERT_MILLI_TOLERANCE)  {
                    this.alert = newAlert;
                    break;
                }
            }
        } else {
            alert = null;
        }
    }

    public static void initIcons(@NonNull Context context) {
        if (iconNone == null) {
            iconNone = DrawUtils.getMarkerIconFromDrawableSized(context, R.drawable.smart_none, null);
            iconError = DrawUtils.getMarkerIconFromDrawableSized(context, R.drawable.smart_error, null);
            for (int idx = 0; idx < imageDDI7.length; idx++) {
                iconDDI7[idx] = DrawUtils.getMarkerIconFromDrawableSized(context, imageDDI7[idx], null);
            }
        }
    }

    public Icon getIcon(@NonNull Context context) {
        initIcons(context);
        Icon icon = iconError;
        if (hasValidLoc) {
            icon = hasValidData ? iconDDI7[drivingDifficultyIndex] : iconNone;
        }
        return icon;
    }

    @DrawableRes
    public int getImageRes(Context context) {
        int imgRes = R.drawable.smart_error;
        if (hasValidLoc) {
            imgRes = hasValidData ? imageDDI7[drivingDifficultyIndex] : R.drawable.smart_none;
        }
        return imgRes;
    }

    @NonNull
    public String alertType() {
        return (alert != null) ? String.format(Locale.US, "%s %s", alert.phenomena, alert.significance) : "--";
    }
    @NonNull
    public String alertExpire() {
        if (alert != null) {
            // org.joda.time.Duration duration = new org.joda.time.Duration(alert.expireTimeLocal.getMillis() - System.currentTimeMillis());
            // return periodFmter.print(duration.toPeriod());
            DateTime expireDt = new DateTime(alert.expireTimeLocal.getMillis());
            if (expireDt.isBefore(DateTime.now().plusHours(12))) {
                return "Till " + expireDt.toString("hh:mm a");
            }
        }
        return "";
    }
}
