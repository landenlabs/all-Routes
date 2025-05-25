/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.wx_lib_data.logger;

import android.content.Context;
import android.content.res.Resources;
import android.text.SpannableString;
import android.text.SpannedString;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.Arrays;
import java.util.List;

public class ALogUtils {

    @StringRes
    public static final int NO_STRING_RES = 0;
    public static final String NULL_STR = "(null)";

    // ---------------------------------------------------------------------------------------------

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
        } else if (value instanceof List) {
            return String.format("List[%d]", ((List<?>) value).size()) + joinStrings("\n", ((List<?>) value).toArray());
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
            if (ALogUtils.hasText(valueStr) && !NULL_STR.equals(valueStr)) {
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

}

