/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.wx_lib_data.utils;

import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import landenlabs.wx_lib_data.Constants;
import landenlabs.wx_lib_data.logger.ALog;

/**
 * Parse json object helper routines.
 */
public class ParseJson {
    private static final String TAG = ParseJson.class.getSimpleName();

    public static boolean isEmpty(String value) {
        return TextUtils.isEmpty(value) || value.equals("null");
    }

    /*
     * Extract Json Object, convert exception to null.
     */
    @Nullable
    public static JSONObject getJSONObject(@Nullable JSONObject jsonObject, @NonNull String name) {

        if (jsonObject == null)
            return null;
        try {
            return jsonObject.has(name) ? jsonObject.getJSONObject(name) : null;
        } catch (JSONException jex) {
            return null;
        }
    }

    /**
     * Extract float from json string or json double, convert exception to defValue.
     */
    public static float getFloat(@NonNull JSONObject jsonObject, @NonNull String name, float defValue) {
        try {
            String value = jsonObject.getString(name);
            if (ParseJson.isEmpty(value))
                return defValue;

            return Float.parseFloat(value);
        } catch (JSONException jex) {
            try {
                return (float)jsonObject.getDouble(name);
            } catch (JSONException jex2) {
                ALog.w.tagMsg(TAG, "Json getFloat error ", jex2);
                return defValue;
            }
        }
    }

    /**
     * Extract float from json, convert exception to NOT_A_NUMBER_F.
     */
    public static float getFloat(@NonNull JSONObject jsonObject, @NonNull String name) {
        return getFloat(jsonObject, name, Constants.NOT_A_NUMBER_F);
    }

    /**
     * Extract float from json, convert exception to defValue.
     */
    public static float getFloat(@NonNull JSONArray jsonArray, int idx, float defValue) {
        try {
            String value = jsonArray.getString(idx);
            if (ParseJson.isEmpty(value))
                return defValue;

            return Float.parseFloat(value);
        } catch (JSONException jex) {
            ALog.w.tagMsg(TAG, "Json getFloatArray error ", jex);
            return defValue;
        }
    }

    /**
     * Extract float from json, convert exception to NOT_A_NUMBER_F.
     */
    public static float getFloat(@NonNull JSONArray jsonArray, int idx) {
        return getFloat(jsonArray, idx, Constants.NOT_A_NUMBER_F);
    }

    /**
     * Extract int from json, convert exception to defValue.
     */
    public static int getInt(@Nullable JSONObject jsonObject, @NonNull String name, int defValue) {
        try {
            return (jsonObject != null) ? jsonObject.getInt(name) : defValue;
        } catch (JSONException jex) {
            ALog.w.tagMsg(TAG, "Json getInt error", jex);
            return defValue;
        }
    }

    /**
     * Extract int from json, convert exception to defValue.
     */
    public static int getInt(@Nullable JSONArray jsonArray, int idx, int defValue) {
        try {
            return (jsonArray != null) ? jsonArray.getInt(idx) : defValue;
        } catch (JSONException jex) {
            ALog.w.tagMsg(TAG, "Json getInt error", jex);
            return defValue;
        }
    }


    /**
     * Extract int from json, on exception return defValue.
     */
    public static int parseInt(@Nullable JSONObject jsonObject, String name, int defValue) {
        try {
            return (jsonObject != null) ? ParserUtils.intValue(jsonObject.getString(name), 0)
                    : defValue;
        } catch (JSONException jex) {
            ALog.w.tagMsg(TAG, "Json parseInt error ", jex);
            return defValue;
        }
    }


    /**
     * Extract int from json, convert exception to defValue.
     */
    public static long getLong(@Nullable JSONObject jsonObject, @NonNull String name, long defValue) {
        try {
            return (jsonObject != null) ? jsonObject.getLong(name) : defValue;
        } catch (JSONException jex) {
            ALog.w.tagMsg(TAG, "Json getLong error", jex);
            return defValue;
        }
    }

    /**
     * Extract string from json, on exception return defValue.
     */
    public static String getString(@Nullable JSONObject jsonObject, String name, String defValue) {
        try {
            if (jsonObject == null)
                return defValue;
            String value = jsonObject.getString(name);
            if (ParseJson.isEmpty(value))
                return defValue;
            return value;

        } catch (JSONException jex) {
            ALog.w.tagMsg(TAG, "Json getString error ", jex);
            return defValue;
        }
    }

    /**
     * Extract string from json, on exception return defValue.
     */
    public static String getStringIf(@Nullable JSONObject jsonObject, String name, String defValue) {
        try {
            if (jsonObject == null)
                return defValue;
            String value = jsonObject.getString(name);
            if (ParseJson.isEmpty(value))
                return defValue;
            return value;

        } catch (JSONException jex) {
            // ALog.w.tagMsg(TAG, "Json getString error ", jex);
            return defValue;
        }
    }

    /**
     * Tries to extract string from JSON by one of {@param names} key. If not value found by one
     * of given keys {@param defValue} will be returned.
     */
    public static String getString(@Nullable JSONObject jsonObject, String[] names, String defValue) {
        if (jsonObject == null)
            return defValue;
        for (String name : names) {
            try {
                String value = jsonObject.getString(name);
                if (!ParseJson.isEmpty(value)) {
                    return value;
                }
            } catch (JSONException jex) {
                ALog.w.tagMsg(TAG, "Json getString error ", jex);
            }
        }
        return defValue;
    }

    public static String getString(@Nullable JSONObject jsonObject, String name) {
        return getString(jsonObject, name, "");
    }


    /**
     * Extract string from json, on exception return defValue.
     */
    public static String getString(@Nullable JSONArray jsonArray, int idx, String defValue) {
        try {
            if (jsonArray == null)
                return defValue;
            String value = jsonArray.getString(idx);
            if (ParseJson.isEmpty(value))
                return defValue;
            return value;

        } catch (JSONException jex) {
            ALog.w.tagMsg(TAG, "Json getString error ", jex);
            return defValue;
        }
    }

    public static String getString(@Nullable JSONArray jsonArray, int idx) {
        return getString(jsonArray, idx, "");
    }

    /**
     * @return parsed date else NO_DATE.
     */
    public static Date getDate(@Nullable JSONObject jsonObject, @NonNull String name, TimeZone timeZone, String pattern) {
        String dateStr = getString(jsonObject, name, "");
        return getDate(dateStr, timeZone, pattern);
    }

    private static Date getDate(@Nullable JSONObject jsonObject, @NonNull String name, TimeZone timeZone, String pattern,
            String alternativePattern) {
        String dateStr = getString(jsonObject, name, "");
        return getDate(dateStr, timeZone, pattern, alternativePattern);
    }

    public static Date getDate(@Nullable JSONArray jsonArray, int idx, TimeZone timeZone, String pattern) {
        String dateStr = getString(jsonArray, idx, "");
        return getDate(dateStr, timeZone, pattern);
    }

    private static Date getDate(String dateStr, TimeZone timeZone, String pattern) {
        if (ParseJson.isEmpty(dateStr)) {
            return ParseDateUtils.NO_DATE; // Calendar.getInstance().getTime();
        } else {
            return ParseDateUtils.parseDate(dateStr, timeZone, pattern);
        }
    }

    private static Date getDate(String dateStr, TimeZone timeZone, String... patterns) {
        if (ParseJson.isEmpty(dateStr)) {
            return ParseDateUtils.NO_DATE; // Calendar.getInstance().getTime();
        } else {
            return ParseDateUtils.parseDate(dateStr, timeZone, patterns);
        }
    }

    /**
     * @return parsed date else defaultValue.
     */
    public static Date getDate(@Nullable JSONObject jsonObject, @NonNull String name, TimeZone timeZone, String pattern, Date defaultValue) {
        Date date = getDate(jsonObject, name, timeZone, pattern);
        return ParseDateUtils.hasNoDate(date) ? defaultValue : date;
    }

    /**
     * @return parsed date else defaultValue.
     */
    public static Date getDate(@Nullable JSONObject jsonObject, @NonNull String name, TimeZone timeZone, String pattern,
            String alternativePattern, Date defaultValue) {
        Date date = getDate(jsonObject, name, timeZone, pattern, alternativePattern);
        return ParseDateUtils.hasNoDate(date) ? defaultValue : date;
    }


    /**
     * @return parsed date else defaultValue.
     */
    public static Date getDate(String dateStr, TimeZone timeZone, String pattern, Date defaultValue) {
        Date date = getDate(dateStr, timeZone, pattern);
        return ParseDateUtils.hasNoDate(date) ? defaultValue : date;
    }
    /**
     * Return LinkedHashMap of parameters (key, value) pairs.
     * Supports one level of nesting, key name is parentKey.childKey
     */
    @NonNull
    public static LinkedHashMap<String, String> getMap(JSONObject jsObj, String name) {
        LinkedHashMap<String, String> parameters = null;
        try {
            if (jsObj.has(name)) {
                JSONObject targetJSONObj = jsObj.getJSONObject(name);
                parameters = getMap(targetJSONObj);
            }
        } catch (JSONException ex) {
            ALog.w.tagMsg(TAG, "Json getMap error ", ex);
        }
        return (parameters == null) ? new LinkedHashMap<>() : parameters;
    }

    public static LinkedHashMap<String, String> getMap(JSONObject targetJSONObj) {
        LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
        if (targetJSONObj != null) {
            for (Iterator<String> typeIter = targetJSONObj.keys(); typeIter.hasNext(); ) {
                String parentKey = typeIter.next();

                try {
                    String val = targetJSONObj.getString(parentKey);
                    parameters.put(parentKey, val);
                    continue;
                } catch (JSONException jex) {
                    // Pare of type string failed, so try next type.
                }

                try {
                    JSONObject jsChild = targetJSONObj.getJSONObject(parentKey);
                    for (Iterator<String> childIter = jsChild.keys(); childIter.hasNext(); ) {
                        String childKey = childIter.next();
                        parameters.put(parentKey + "." + childKey, jsChild.getString(childKey));
                    }
                    continue;
                } catch (JSONException jex) {
                    // Parse of type Object failed, so try next type.
                }

                try {
                    JSONArray jsonArray = targetJSONObj.getJSONArray(parentKey);
                    List<String> values = new ArrayList<>();
                    for (int arrIdx = 0; arrIdx != jsonArray.length(); arrIdx++) {
                        String value = jsonArray.getString(arrIdx);
                        values.add(value);
                    }
                    parameters.put(parentKey, values.toString());
                } catch (JSONException jex) {
                    // Parse of type array failed, so give up.
                }
            }
        }

        return parameters;
    }

    public static Object getObjMap(Object obj) {
        if (obj instanceof String)
            return obj.toString();

        if (obj instanceof JSONObject) {
            JSONObject targetJSONObj = (JSONObject)obj;
            LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
            for (Iterator<String> typeIter = targetJSONObj.keys(); typeIter.hasNext(); ) {
                String parentKey = typeIter.next();

                try {
                    JSONArray jsonArray = targetJSONObj.getJSONArray(parentKey);
                    List<Object> values = new ArrayList<>();
                    for (int arrIdx = 0; arrIdx != jsonArray.length(); arrIdx++) {
                        values.add(getObjMap(jsonArray.getJSONObject(arrIdx)));
                    }
                    parameters.put(parentKey, values);
                    continue;
                } catch (JSONException jex) {
                    // Parse of type array failed, so give up.
                }

                try {
                    JSONObject jsChild = targetJSONObj.getJSONObject(parentKey);
                    Map<String, Object> items = new HashMap<>();
                    for (Iterator<String> childIter = jsChild.keys(); childIter.hasNext(); ) {
                        String childKey = childIter.next();
                        Object childValue = jsChild.get(childKey);
                        if (childValue instanceof JSONObject) {
                            items.put(childKey, getObjMap((JSONObject) childValue));
                        } else {
                            items.put(childKey, childValue.toString());
                        }
                    }
                    parameters.put(parentKey, items);
                    continue;
                } catch (JSONException jex) {
                    // Parse of type Object failed, so try next type.
                }

                try {
                    String val = targetJSONObj.getString(parentKey);
                    parameters.put(parentKey, val);
                    continue;
                } catch (JSONException jex) {
                    // Pare of type string failed, so try next type.
                }

                parameters.put(parentKey, "-?-");
            }

            return parameters;
        }

        return obj;
    }

    /**
     * Get list from potential array or object json element
     */
    public static Set<String> getJsonStringSet(JSONObject object, String key) {
        JsonVariant what = JsonVariant.opt(object, key);
        Set<String> list = new LinkedHashSet<>();
        try {
            switch (what) {
                case ARRAY:
                    JSONArray jsonArray = object.optJSONArray(key);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        list.add(jsonArray.optString(i));
                    }
                    break;
                case OBJECT:
                    String stringValue = object.getJSONObject(key).toString();
                    list.add(stringValue);
                    break;
                case VALUE:
                    list.add(object.getString(key));
                    break;
            }
        } catch (Exception e) {
            // ignore and return empty list
        }
        return list;
    }

    /**
     * Get list from potential array or object json element
     */
    public static ArrayList<String> getJsonStringList(JSONObject object, String key) {
        ArrayList<String> list = new ArrayList<>();
        try {
            JSONArray jsonArray = object.getJSONArray(key);

            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.optString(i));
            }
        } catch (Exception ex) {
            // ignore and return empty list
        }
        return list;
    }

    /**
     * Get list from potential array or object json element
     */
    public static ArrayList<Double> getJsonDoubleList(JSONObject object, String key) {
        ArrayList<Double> list = new ArrayList<>();
        try {
            JSONArray jsonArray = object.getJSONArray(key);

            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.optDouble(i));
            }
        } catch (Exception ex) {
            // ignore and return empty list
        }
        return list;
    }

    /**
     * Enum to define what is key in Json object
     */
    public enum JsonVariant {
        OBJECT,
        ARRAY,
        VALUE,
        NULL;

        public static JsonVariant opt(JSONObject parentObject, String key) {
            try {
                Object jsonVar = parentObject.get(key);
                if (jsonVar != null) {
                    String firstChar = String.valueOf(jsonVar.toString().charAt(0));
                    switch (firstChar) {
                        case "[":
                            return ARRAY;
                        case "{":
                            return OBJECT;
                        default:
                            return VALUE;
                    }
                }
            } catch (Exception e) {
                return NULL;
            }

            return NULL;
        }
    }

    public static String encodeIdTimeToString(String str, long timeStamp) {
        return new JSONArray().put(str).put(timeStamp).toString();
    }

    /**
     * Returns pair object. First - unique id, Second - timestamp.
     */
    @Nullable
    public static Pair<String, Long> decodeStringToIdTime(String jsonString) {
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            return new Pair<>(jsonArray.getString(0), jsonArray.getLong(1));
        } catch (JSONException e) {
            ALog.w.tagMsg(TAG,  "decodeStringToIdTime failed on " + jsonString);
            return null;
        }
    }

}
