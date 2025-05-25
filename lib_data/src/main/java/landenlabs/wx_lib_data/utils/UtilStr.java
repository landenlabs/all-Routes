/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.wx_lib_data.utils;

import android.content.Context;
import android.content.res.Resources;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

public class UtilStr {

    @StringRes
    public static final int NO_STRING_RES = 0;  // API 25+   Resources.ID_NULL;
    public static final String NULL_STR = "(null)";
    private static final String TAG = "StrUtils";
    public static final String FULL_STAR = "\u2605";
    public static final String HALF_STAR = "\u00BD";
    public static final String MID_DOT = "\u00b7";

    // ---------------------------------------------------------------------------------------------

    private static int compareToIgnoreCase(String s1, String s2) {
        return Objects.equals(s1, s2)
                ? 0 : (s1 != null
                && (s2 != null)
                ? s1.compareToIgnoreCase(s2)
                : ((s1 == null)
                ? -1 : 1));
    }

    public static boolean equalToIgnoreCase(String s1, String s2) {
        return (compareToIgnoreCase(s1, s2) == 0);
    }

    @SuppressWarnings("StringEquality")
    public static boolean equals(String s1, String s2) {
        return s1 == s2 || (s1 != null && s1.equals(s2)) || TextUtils.isEmpty(s1) == TextUtils.isEmpty(s2);
    }

    /**
     * Return first string with content.
     */
    public static String firstOf(final String... strs) {
        for (String str : strs) {
            if (str != null && str.trim().length() > 0) return str;
        }
        return "";
    }

    public static String trim(@Nullable Object obj, @Nullable String findStr, int maxFind, int maxLength) {
        if (obj == null) return "";
        String str = obj.toString();

        if (hasText(findStr) && maxFind > 0) {
            int pos = 0;
            while (maxFind-- > 0) {
                int nextPos = str.indexOf(findStr, pos);
                if (nextPos != -1)
                    pos = nextPos + findStr.length();
                else
                    break;
            }
            maxLength = Math.min(maxLength, pos);
        }

        return str.substring(0, Math.min(str.length(), maxLength));
    }

    /**
     * Similar to Java String.valueOf(obj) but returns empty string for null, not "null"
     *
     * @return string of obj else ""
     */
    public static String valueOf(Object obj) {
        return (obj == null) ? "" : obj.toString();
    }

    @NonNull
    public static String toLower(@NonNull String str) {
        return str.toLowerCase(Locale.US);
    }
    @NonNull
    public static String toUpper(@NonNull String str) {
        return str.toUpperCase(Locale.US);
    }

    /**
     * @return Simple uppercase  of first character remainder lowercase
     */
    public static String toCapitalize(final String str) {
        if (TextUtils.isEmpty(str) || str.length() == 1)
            return str;
        return Character.toUpperCase(str.charAt(0))
                + str.substring(1).toLowerCase(Locale.US);
    }

    /**
     * @return true if string contains +/-[0-9]...
     */
    public static boolean isInteger(@Nullable String str) {
        if (str != null) {
            final int len = str.length();
            if (len > 0) {
                int cp = Character.codePointAt(str, 0);
                int sIdx = (cp == '-' || cp == '+') ? 1 : 0;
                for (int i = sIdx; i < len; i += Character.charCount(cp)) {
                    cp = Character.codePointAt(str, i);
                    if (!Character.isDigit(cp)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public static String getString(@Nullable Context context, @Nullable Object value) {
        if (value == null) {
            return NULL_STR;
        } else if (value instanceof String
                || value instanceof StringBuilder
                || value instanceof SpannedString
                || value instanceof SpannableString) {
            return value.toString();
        } else if (value instanceof Integer && context != null) {
            try {
                return ((Integer) value == NO_STRING_RES) ? "" : context.getString((Integer) value);
            } catch (Resources.NotFoundException ex) {
                return String.valueOf(value);
            }
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Object[]) {
            return joinArray(context, (Object[]) value);
        } else if (value.getClass().isArray()) {
            String typeName = value.getClass().getSimpleName();
            switch (typeName) {
                case "int[]":
                    return Arrays.toString((int[]) value);
                case "long[]":
                    return Arrays.toString((long[]) value);
                case "float[]":
                    return Arrays.toString((float[]) value);
                case "double[]":
                    return Arrays.toString((double[]) value);
            }
            return joinStrings(context, value.getClass().getSimpleName(), value.toString());
        } else if (value instanceof Collection) {
            return String.format("Collection[%d]", ((Collection<?>)value).size()) + joinStrings("\n", ((Collection<?>) value).toArray());
        } else {
            // throw new UnsupportedOperationException("unknown getString type " + value.getClass().getSimpleName() + value);
            return joinStrings(context, value.getClass().getSimpleName(), value.toString());
        }
    }

    public static String joinStrings(Context context, @NonNull Object... values) {
        StringBuilder sb = new StringBuilder();
        for (Object value : values) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(getString(context, value));
        }
        return sb.toString();
    }

    public static String joinArray(Context context, @NonNull Object[] values) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s[%d]", values.getClass().getSimpleName(), values.length));
        for (Object value : values) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(getString(context, value));
        }
        return sb.toString();
    }

    public static String joinStrings(String sep, @NonNull Object... values) {
        StringBuilder sb = new StringBuilder();
        for (Object value : values) {
            String valueStr = getString(null, value);
            if (UtilStr.hasText(valueStr) && !NULL_STR.equals(valueStr)) {
                if (sb.length() > 0)
                    sb.append(sep);
                sb.append(valueStr);
            }
        }
        return sb.toString();
    }

    public static boolean isEmpty(@Nullable CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static boolean hasText(@Nullable CharSequence str) {
        return str != null && str.length() > 0;
    }
    public static String asString(@Nullable Object obj, @Nullable String def) {
        return (obj != null) ? obj.toString() : def;
    }
    public static String asString(@Nullable Object obj) {
        return getString(null, obj);
    }

    /**
     * Optionally include separator if non-empty strings are joined.
     */
    @NonNull
    public static String join(@NonNull String sep, String ... strs ) {
        if (strs.length == 0)  return "";
        StringBuilder sb = new StringBuilder();
        for (String str : strs) {
            if (!isEmpty(str)) {
                if (sb.length() != 0)
                    sb.append(sep);
                sb.append(str);
            }
        }
        return sb.toString();
    }

    @NonNull
    public static <TT> String join(@NonNull String sep, @Nullable Collection<TT> strs, GetStr<TT> getStr ) {
        if (strs == null || strs.size() == 0)  return "";
        StringBuilder sb = new StringBuilder();
        for (TT item : strs) {
            String str = getStr.get(item);
            if (sb.length() != 0 && !isEmpty(str))
                sb.append(sep);
            sb.append(str);
        }
        return sb.toString();
    }
    public interface GetStr <TT> {
        String get(TT item);
    }

    @NonNull
    public static <TT, CC extends Converter<TT>> ArrayList<TT> split(@NonNull String sep, @Nullable String inStr, CC conv ) {
        ArrayList<TT> outList = new ArrayList<>();
        if (inStr == null || inStr.length() == 0)  return outList;
        for (String item : inStr.split(sep)) {
            outList.add(conv.fromStr(item));
        }
        return outList;
    }
    public interface Converter <TT> {
        TT fromStr(String item) ;
    }
    private static boolean isEmpty(@Nullable String str) {
        return (str == null) || str.length() == 0;
    }

}

