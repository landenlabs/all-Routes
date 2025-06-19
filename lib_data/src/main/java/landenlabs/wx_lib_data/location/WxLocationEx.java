/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.wx_lib_data.location;


import static landenlabs.wx_lib_data.Constants.FMT_LAT_LNG;
import static landenlabs.wx_lib_data.utils.UtilStr.toUpper;

import android.location.Location;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wsi.wxdata.WxLocation;

import org.json.JSONArray;
import org.json.JSONException;

import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import landenlabs.wx_lib_data.utils.UtilStr;


/**
 * Extended WxLocation classes to help manage different use cases:
 *     <li>  GPS
 *     <li>  Map
 *     <li>  Location
 */
public class WxLocationEx {
    private static final String TAG = WxLocationEx.class.getSimpleName();

    @NonNull
    public static String log(@Nullable WxLocation wxLocation) {
        String locStr = "LocIsNull";
        if (wxLocation != null) {
            String tag = wxLocation.tag != null ? wxLocation.tag.toString() : "noTag";
            locStr = String.format(Locale.US, tag + " [%.5f,%.5f] %s (%s)",
                    wxLocation.getLatitude(), wxLocation.getLongitude(),
                    fmtLocationName(wxLocation), wxLocation.locType);
        }
        return locStr;
    }

    @Nullable
    public static String toCountry(@Nullable String countryCode) {
        if (countryCode == null || countryCode.length() != 2) {
            return null;
        }
        return new Locale("", toUpper(countryCode)).getDisplayCountry();
    }

    @NonNull
    public static WxLocation createAt(@NonNull Location gps) {
        return new WxLocation(
                "gps", "", "", "", Locale.getDefault().getCountry(), "",
                gps.getLatitude(), gps.getLongitude(),
                ZoneId.systemDefault().getId(),
                "", LOC_TYPE_CITY, "", null);
    }

    /*
    // First 3 are required,
    //  <latitude>,<longitude>,name,city,admin,adminID,countryCode,postalCode,address
    //           0           1    2    3     4       5           6          7       8
    public static WxLocation fromStr(@NonNull String locationStr) {
        String[] parts = locationStr.split(",");
        if (parts.length >= 3 && isFloat(parts[0]) && isFloat(parts[1])) {
            float latitude = floatValue(parts[0], 0f);
            float longitude = floatValue(parts[1], 0f);
            String name = parts[2];
            return new WxLocation(
                    name, //  getStringEmptyIfNull(displayName),
                    UtilData.array.getIt(parts, 3, name), //  getStringEmptyIfNull(city),
                    UtilData.array.getIt(parts, 4, ""), //  getStringEmptyIfNull(adminDistrict),
                    UtilData.array.getIt(parts, 5, ""), // getStringEmptyIfNull(adminDistrictCode),
                    UtilData.array.getIt(parts, 6, "US"), // getStringEmptyIfNull(countryCode),
                    UtilData.array.getIt(parts, 7, "12345"), // getStringEmptyIfNull(postalCode),
                    latitude,
                    longitude,
                    TimeZone.getDefault().getID(),
                    UtilData.array.getIt(parts, 8, name),  // address,
                    LOC_TYPE_CITY);
        }
        return null;
    }
     */

    public static boolean isFloat(@NonNull String str) {
        return str.matches("-?[0-9]+[.][0-9]*");
    }

    public enum PurposeType {
        GPS,
        DST,
    }

    // WxLocation Purpos field - possible values (may be a partial list)
    public static final String LOC_TYPE_CITY = "city";

    public static boolean isGPS(@Nullable WxLocation wxLocation) {
        return wxLocation != null && wxLocation.tag == PurposeType.GPS;
    }
    public static boolean isDST(@Nullable WxLocation wxLocation) {
        return wxLocation != null && wxLocation.tag == PurposeType.DST;
    }
    public static boolean isLocation(@Nullable WxLocation wxLocation) {
        return wxLocation != null && !isGPS(wxLocation) && !isDST(wxLocation);
    }

    public static PurposeType getPurpose(@Nullable WxLocation wxLocation, PurposeType defPurpose) {
        if (wxLocation != null && wxLocation.tag instanceof PurposeType) {
            return (PurposeType) wxLocation.tag;
        }
        return defPurpose;
    }

    /**
     * Set Location's optional TAG to an enumerated purpose  for easy tracking down stream.
     * NOTE - this is different than the Purpos inside WxLocation which tracks the type of locatoin.
     */
    public static WxLocation setPurpose(@NonNull PurposeType purpose, @Nullable WxLocation wxLocation) {
        if (wxLocation != null) {
            if (wxLocation.tag != null && wxLocation.tag != purpose) {
                wxLocation = wxLocation.clone();
            }
            wxLocation.tag = purpose;
        }
        return wxLocation;
    }


    /**
     * Return front or back of address, where first comma splits the two areas.
     */
    public static String getAddressPart(String address, boolean firstPart, String defaultResult) {
        if (TextUtils.isEmpty(address)) {
            return defaultResult;
        }
        return Pattern.compile(firstPart ? ",.*" : "[^,]*,").matcher(address).replaceFirst("").trim();
    }

    public static String fmtLocationName(@Nullable WxLocation wxLoc) {
        return (wxLoc == null) ? "" : wxLoc.getDescription();
    }

    public static String fmtLocationGPS(@NonNull WxLocation wxLoc) {
        return UtilStr.join(", ",  wxLoc.city, wxLoc.adminDistrict);
    }

    public static String fmtLocationAddress(@Nullable WxLocation wxLoc) {
        return TextUtils.isEmpty(wxLoc.address) ? fmtLocationName(wxLoc) : wxLoc.address;
    }

    public static String getLatLngStr(@NonNull WxLocation wxLocation) {
        return String.format(Locale.US, FMT_LAT_LNG, wxLocation.getLatitude(), wxLocation.getLongitude());
    }

    public static String getLatLngStr(@NonNull Location latLng) {
        return String.format(Locale.US, FMT_LAT_LNG, latLng.getLatitude(), latLng.getLongitude());
    }

    // TODO - consider move into WxLocation class
    private static final float SCALE_INT = 100f;
    public static JSONArray toJsonArray(@NonNull WxLocation loc) {
        return new JSONArray()
            .put(loc.locationName)                //  0
            .put(loc.city)                        //  1
            .put(loc.adminDistrict)               //  2
            .put(loc.adminDistrictCode)           //  3
            .put(loc.countryCode)                 //  4
            .put(loc.postalCode)                  //  5
            .put((int)(loc.latitude*SCALE_INT))   //  6
            .put((int)(loc.longitude*SCALE_INT) ) //  7
            .put(loc.dtZone)                      //  8
            .put(loc.address)                     //  9
            .put(loc.locType)                     //  10
            .put(loc.tag);                        //  11

    }
    public static String toJsonStr(@NonNull WxLocation loc) {
        return toJsonArray(loc).toString();
    }

    @Nullable
    public static WxLocation fromJsonStr(@NonNull String jsonArrayStr) {
        try {
            JSONArray ja = new JSONArray(jsonArrayStr);
            WxLocation loc = new WxLocation(
                    ja.getString(0),                     // locationName
                    ja.getString(1),                     // city
                    ja.getString(2),                     // adminDistrict
                    ja.getString(3),                     // adminDistrictCode
                    ja.getString(4),                     // countryCode
                    ja.getString(5),                     // postal
                    ja.getInt(6)/SCALE_INT,      // latitude
                    ja.getInt(7)/SCALE_INT,     // longitude
                    ja.getString(8),                     // TimeZone
                    ja.getString(9),                     // address
                    ja.getString(10),                     // locType
                    "",     // dma
                    null    // aux
                    );
            loc.tag = ja.getString(11);
            return loc;
        } catch (JSONException ex) {
            return null;
        }
    }

}
