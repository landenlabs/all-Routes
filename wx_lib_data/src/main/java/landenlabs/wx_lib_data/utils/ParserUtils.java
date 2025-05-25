/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package landenlabs.wx_lib_data.utils;

import android.graphics.Color;
import android.text.TextUtils;

import org.xml.sax.Attributes;

import landenlabs.wx_lib_data.logger.ALog;

/**
 * Generic data parsing routines.
 */
public class ParserUtils {

    /**
     * Represents available values for boolean "true" value: {TRUE, YES, true, yes, 1}.
     */
    private static final String[] BOOLEAN_TRUE_VALUES =
            new String[]{"TRUE", "YES", "true", "yes", "1"};
    private static final String TAG = ParserUtils.class.getSimpleName();

    private ParserUtils() {
        // Utility static classs should not have public or default constructor.
    }

    /**
     * Use to parse {@code boolean} value from {@code String}.
     *
     * @param value {@code String} that holds {@code boolean} value that should be parsed
     * @return {@code boolean} the is parsed from the string, for more details
     * see {@link Boolean#parseBoolean(String)}
     */
    public static boolean booleanValue(String value) {
        return Boolean.parseBoolean(value);
    }

    /**
     * Parses integer value from given string.
     *
     * @param value        {@code String} that holds {@code int} value that should be
     *                     parsed
     * @param defaultValue value that should be returned if method can't parse
     *                     {@code int} from the {@code String}
     * @return {@code int} that is parsed from the string, or
     * {@code defaultValue} if fails to parse integer from the string
     */
    public static int intValue(String value, int defaultValue) {
        int result = defaultValue;
        if (!TextUtils.isEmpty(value)) {
            try {
                result = Integer.parseInt(value);
            } catch (NumberFormatException nfe) {
                ALog.e.tagMsg(TAG,
                        "intValue :: unable to parse integer from string [" + value + "]");
            }
        }
        return result;
    }

    /**
     * Decode integer value from given string.
     *
     * @param value        {@code String} that holds {@code int} value that should be
     *                     parsed
     *                     DecodableString:
     *                     Signopt DecimalNumeral
     *                     Signopt 0x HexDigits
     *                     Signopt 0X HexDigits
     *                     Signopt # HexDigits
     *                     Signopt 0 OctalDigits
     *                     Sign:
     *                     -
     *                     +
     * @param defaultValue value that should be returned if method can't parse
     *                     {@code int} from the {@code String}
     * @return {@code int} that is parsed from the string, or
     * {@code defaultValue} if fails to parse integer from the string
     */
    public static int intDecode(String value, int defaultValue) {
        int result = defaultValue;
        if (!TextUtils.isEmpty(value)) {
            try {
                result = Integer.decode(value);
            } catch (NumberFormatException nfe) {
                ALog.e.tagMsg(TAG,
                        "intValue :: unable to parse integer from string [" + value + "]");
            }
        }
        return result;
    }

    /**
     * Decode long value from given string.
     *
     * @param value        {@code String} that holds {@code int} value that should be
     *                     parsed
     *                     DecodableString:
     *                     Signopt DecimalNumeral
     *                     Signopt 0x HexDigits
     *                     Signopt 0X HexDigits
     *                     Signopt # HexDigits
     *                     Signopt 0 OctalDigits
     *                     Sign:
     *                     -
     *                     +
     * @param defaultValue value that should be returned if method can't parse
     *                     {@code int} from the {@code String}
     * @return {@code int} that is parsed from the string, or
     * {@code defaultValue} if fails to parse integer from the string
     */
    public static long longDecode(String value, long defaultValue) {
        long result = defaultValue;
        if (!TextUtils.isEmpty(value)) {
            try {
                result = Long.decode(value);
            } catch (NumberFormatException nfe) {
                ALog.e.tagMsg(TAG,
                        "intValue :: unable to parse integer from string [" + value + "]");
            }
        }
        return result;
    }

    /**
     * Parses float value from from given string.
     *
     * @param value        {@code String} that holds {@code float} value that should be
     *                     parsed
     * @param defaultValue value that should be returned if method can't parse
     *                     {@code float} from the {@code String}
     * @return {@code float} that is parsed from the string, or
     * {@code defaultValue} if fails to parse float from the string
     */
    public static float floatValue(String value, float defaultValue) {
        float result = defaultValue;
        if (!TextUtils.isEmpty(value)) {
            try {
                result = Float.parseFloat(value);
            } catch (NumberFormatException nfe) {
                ALog.e.tagMsg(TAG,
                        "floatValue :: unable to parse float from string [" + value + "]");
            }
        }
        return result;
    }

    /**
     * Parses double value from given string.
     *
     * @param value        string that holds {@code double} value
     * @param defaultValue {@code double} value that should be returned if fails to parse {@code double} from
     *                     given string
     * @return parsed {@code double} value, or default value if fails to parse {@code double} from
     * given string
     */
    public static double doubleValue(String value, double defaultValue) {
        double result = defaultValue;
        if (!TextUtils.isEmpty(value)) {
            try {
                result = Double.parseDouble(value);
            } catch (NumberFormatException nfe) {
                ALog.e.tagMsg(TAG, "doubleValue :: unable to parse double from string [",
                        value + "]");
            }
        }
        return result;
    }

    /**
     * Parses long value from given string.
     *
     * @param value        string that should parsed
     * @param defaultValue value that should be returned in case given string does not contain long value
     * @return long value that is parsed from given string
     */
    public static long longValue(String value, long defaultValue) {
        long result = defaultValue;
        if (!TextUtils.isEmpty(value)) {
            try {
                result = Long.parseLong(value);
            } catch (NumberFormatException nfe) {
                ALog.e.tagMsg(TAG, "longValue :: unable to parse long from string [", value, "]");
            }
        }
        return result;
    }

    /**
     * Use to get boolean value of input {@code value}.
     *
     * @param value target {@code String} value
     * @return {@code true} if value is one of {TRUE, true, YES, yes, 1} values,
     * {@code false} - otherwise
     */
    public static boolean getBooleanValue(String value) {
        for (String tl : BOOLEAN_TRUE_VALUES) {
            if (tl.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtains string value from given XML attributes using given attribute name.
     *
     * @param attributes XMl attributes from which value should be obtained
     * @param name       name of attribute that holds string value
     * @return string value of attribute with given name
     */
    public static String stringValue(Attributes attributes, String name) {
        return attributes.getValue(name);
    }

    /**
     * Obtains single color value using given parameters.
     *
     * @param r red color intensity, string should contain integer number
     * @param g green color intensity, string should contain integer number
     * @param b blue color intensity, string should contain integer number
     * @return single color value that is obtained using given parameters
     */
    public static int colorValue(String r, String g, String b) {
        return Color.rgb(intValue(r, 0), intValue(g, 0), intValue(b, 0));
    }

    /**
     * Obtains single color value using given parameters.
     *
     * @param a alpha value, string should contain float (0..1) number, def 1.0
     * @param r red color intensity, string should contain integer number
     * @param g green color intensity, string should contain integer number
     * @param b blue color intensity, string should contain integer number
     * @return single color value that is obtained using given parameters
     */
    public static int colorFloatAlpha(String a, String r, String g, String b) {
        return Color
                .argb((int) (floatValue(a, 1.0f) * 255), intValue(r, 0), intValue(g, 0),
                        intValue(b, 0));
    }

    /**
     * Obtains single hex color value.
     *
     * @param value hex color value #rrggbbaa
     * @return single color value that is obtained using given parameters
     */
    public static int colorHexValue(String value, int defaultValue) {
        int result = defaultValue;
        if (!TextUtils.isEmpty(value)) {
            try {
                // FIXME: Workaround - added on 04/2015. Remove when server side is ready.
                if (!value.contains("#")) {
                    value = "#" + value;
                }
                result = Color.parseColor(value);
                // Force opaque if fully transparent.
                result |= ((result & 0xff000000) == 0) ? 0xff000000 : 0;
            } catch (NumberFormatException nfe) {
                ALog.e.tagMsg(TAG, "intValue :: unable to parse hex color from string [", value,
                        "]");
            }
        }
        return result;
    }

    /**
     * Obtains integer value from given XML attributes using given attribute name.
     *
     * @param attributes   XML attributes from which integer value should be obtained
     * @param name         name of XML attribute that holds integer value
     * @param defaultValue value that should be returned in case give XML attributes does not contain
     *                     attribute with given name or attribute does not contain integer value
     * @return obtained integer value or default value in case fails to parse integer from attribute
     * value or given XML attributes does not contain attribute with given name
     */
    public static int intValue(Attributes attributes, String name, int defaultValue) {
        return intValue(stringValue(attributes, name), defaultValue);
    }

    /**
     * Obtains long value from given XML attributes using given attribute name.
     *
     * @param attributes   XML attributes from which long value should be obtained
     * @param name         name of XML attribute that holds long value
     * @param defaultValue value that should be returned in case give XML attributes does not contain
     *                     attribute with given name or attribute does not contain long value
     * @return obtained long value or default value in case fails to parse long from attribute value
     * or given XML attributes does not contain attribute with given name
     */
    public static long longValue(Attributes attributes, String name, long defaultValue) {
        return longValue(stringValue(attributes, name), defaultValue);
    }

    /**
     * Obtains float value from given XML attributes using given attribute name.
     *
     * @param attributes   XML attributes from which float value should be obtained
     * @param name         name of XML attribute that holds float value
     * @param defaultValue value that should be returned in case give XML attributes does not contain
     *                     attribute with given name or attribute does not contain float value
     * @return obtained float value or default value in case fails to parse float from attribute
     * value or given XML attributes does not contain attribute with given name
     */
    public static float floatValue(Attributes attributes, String name, float defaultValue) {
        return floatValue(stringValue(attributes, name), defaultValue);
    }

    /**
     * Obtains double value from given XML attributes using given attribute name.
     *
     * @param attributes   XML attributes from which double value should be obtained
     * @param name         name of XML attribute that holds double value
     * @param defaultValue value that should be returned in case give XML attributes does not contain
     *                     attribute with given name or attribute does not contain double value
     * @return obtained double value or default value in case fails to parse double from attribute
     * value or given XML attributes does not contain attribute with given name
     */
    public static double doubleValue(Attributes attributes, String name, double defaultValue) {
        return doubleValue(stringValue(attributes, name), defaultValue);
    }

    /**
     * Obtains boolean value from given XML attributes using given attribute name.
     *
     * @param attributes XML attributes from which boolean value should be obtained
     * @param name       name of XML attribute that holds boolean value
     * @return obtained double value or default value in case fails to parse boolean from attribute
     * value or given XML attributes does not contain attribute with given name
     */
    public static boolean booleanValue(Attributes attributes, String name) {
        return booleanValue(stringValue(attributes, name));
    }

    /**
     * Holds value patterns that should be used by multiple application components.
     */
    public static class PATTERNS {
        public static final String WHITE_SPACE_STRING = "^\\s*$";
        public static final String STRING_MULTIPLE_WHITE_SPACE = "\\s\\s+";
        public static final String VALID_EMAIL_REGEX_PATTERN =
                "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
    }

}
