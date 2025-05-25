/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package landenlabs.wx_lib_data.utils;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.joda.time.DateTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import landenlabs.wx_lib_data.Constants;
import landenlabs.wx_lib_data.logger.ALog;

/**
 * Parse datetime in thread safe manner using Joda library.
 */
public final class ParseDateUtils {

    /**
     * Empty state for date.
     */
    public static final Date NO_DATE = new Date(0);

    /**
     * A date format to parse a date, like: Thu, 05 Aug 2010 18:50:10 +0300
     * or                                   Thu, 05 Aug 2010 18:50:10 GMT
     * single Z does not work               Thu, 05 Aug 2010 18:50:10 Z
     * the Z needs to be convert to +0000
     */
    private static final String DATE_FORMAT_RFC822 = "EEE, dd MMM yyyy HH:mm:ss z";

    public static final TimeZone GMT_TIME_ZONE = Constants.GMT;

    public static final String RPC822_TZ_GMT_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String ALT_DATE_PATTERN_TZ = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String ALT_DATE_PATTERN_NO_TZ = "yyyy-MM-dd'T'HH:mm:ss";

    public static final ParseOptions[] DEFAULT_DATETIME_PARSE_OPTIONS =  {
            new ParseOptions(Constants.GMT, RPC822_TZ_GMT_DATE_PATTERN),
            new ParseOptions(ALT_DATE_PATTERN_TZ, ALT_DATE_PATTERN_NO_TZ),
    };

    // ---------------------------------------------------------------------------------------------

    private ParseDateUtils() {
    }

    public static Date parseDate(String date, String... patterns) {
        if (patterns == null || patterns.length == 0) {
            ALog.e.tag("You have to provide at least one pattern to parse date!");
            return null;
        }

        Date result;
        for (String pattern : patterns) {
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            try {
                result = format.parse(date);
                if (result != null) {
                    return result;
                }
            } catch (ParseException ex) {
                // ignore, try to parse with next pattern
            }
        }
        return null;
    }

    public static Date parseDate(String date, Locale locale, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, locale);
        Date result = null;
        try {
            result = format.parse(date);
        } catch (Exception ex) {
            ALog.e.tagMsg("Cannot parse date from string:", date, ex);
        }
        return result;
    }

    public static Date parseDate(String date, TimeZone timeZone, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        if (timeZone != null) {
            format.setTimeZone(timeZone);
        }
        Date result = null;
        try {
            result = format.parse(date);
        } catch (Exception ex) {
            ALog.w.tagMsg("Cannot parse date from string:", date, ex);
        }
        return result;
    }

    public static Date parseDate(String date, ParseOptions... parseOptions) {
        if (parseOptions == null || parseOptions.length == 0) {
            ALog.e.tag("You have to provide at least one ParseOption to parse date!");
            return null;
        }

        Date result;
        for (ParseOptions pattern : parseOptions) {
            result = parseDate(date, pattern.getTimeZone(), pattern.getDateTimePatterns());
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public static Date parseDate(@NonNull String dateStr, @Nullable TimeZone timeZone, String... patterns) {
        if (patterns == null || patterns.length == 0) {
            ALog.e.tag("You have to provide at least one pattern to parse date!");
            return null;
        }

        Date result;
        for (String pattern : patterns) {
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            if (timeZone != null) {
                format.setTimeZone(timeZone);
            }
            try {
                result = format.parse(dateStr);
                if (result != null) {
                    return result;
                }
            } catch (ParseException ex) {
                // ignore, try to parse with next pattern
            }
        }
        return null;
    }

    public static Date parseDate(@NonNull String dateStr, @NonNull Locale locale, @NonNull TimeZone timeZone, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, locale);
        format.setTimeZone(timeZone);
        Date result = null;
        try {
            result = format.parse(dateStr);
        } catch (Exception ex) {
            ALog.w.tagMsg("Cannot parse date from string:", dateStr, ex);
        }
        return result;
    }

    public static Date parseDateRfc822(@NonNull String dateStr) {
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT_RFC822);
        Date result = null;
        try {
            result = format.parse(dateStr);
        } catch (Exception ex) {
            ALog.w.tagMsg("Cannot parse date from string:", dateStr, ex);
        }
        return result;
    }

    public static String formatDate(@NonNull Date date, @NonNull String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.format(date);
    }

    public static String formatDate(@NonNull Date date, @Nullable TimeZone timeZone, @NonNull String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        if (timeZone != null) {
            format.setTimeZone(timeZone);
        }
        return format.format(date);
    }

    public static String formatDate(long timestamp, @NonNull String pattern) {
        return formatDate(new Date(timestamp), pattern);
    }

    public static String formatDate(long timestamp, TimeZone timeZone, @NonNull String pattern) {
        return formatDate(new Date(timestamp), timeZone, pattern);
    }

    public static String formatDate(@NonNull Date date, @NonNull String pattern, @NonNull Locale locale) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, locale);
        return format.format(date);
    }

    public static String formatDate(@NonNull Context context, @NonNull Date date, int stringResource) {
        String pattern = context.getString(stringResource);
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.format(date);
    }

    public static String formatDate(@NonNull Context context, long timestamp, int patternStringResource12h,
            int patternStringResource24h) {
        return formatDate(context, new Date(timestamp), patternStringResource12h, patternStringResource24h);
    }

    public static String formatDate(@NonNull Context context, long timestamp, @Nullable TimeZone timeZone,
            int patternStringResource12h, int patternStringResource24h) {
        return formatDate(context, new Date(timestamp), timeZone,
                patternStringResource12h, patternStringResource24h);
    }

    public static String formatDate(@NonNull Context context,  @NonNull Date date, int patternStringResource12h,
            int patternStringResource24h) {
        return formatDate(context, date, null, patternStringResource12h,
                patternStringResource24h);
    }

    public static String formatDate(@NonNull Context context, @NonNull Date date, TimeZone zone, int patternStringResource) {
        String pattern = context.getString(patternStringResource);
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        format.setTimeZone(zone);
        return format.format(date);
    }

    private static Date BAD_DATE_YR2000 =  new DateTime(2000, 1, 1, 1, 0).toDate();

    public static String formatDate(@NonNull Context context, @Nullable Date date, @Nullable TimeZone timeZone,
            int patternStringResource12h, int patternStringResource24h) {
        if (date == null || date.before(BAD_DATE_YR2000)) {
            return "";
        }

        String pattern = getPatternString(context, patternStringResource12h, patternStringResource24h);
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        if (timeZone != null) {
            format.setTimeZone(timeZone);
        }
        return format.format(date);
    }

    private static boolean hasDate(@Nullable Date date) {
        return date != null && !date.equals(NO_DATE);
    }

    public static boolean hasNoDate(@Nullable Date date) {
        return !hasDate(date);
    }

    public static boolean isCurrentDateGMTBeforeOf(Date other) {
        return isCurrentDateBeforeOf(other, GMT_TIME_ZONE);
    }

    private static boolean isCurrentDateBeforeOf(Date other, TimeZone timeZone) {
        Calendar otherWithTz = Calendar.getInstance(timeZone);
        otherWithTz.setTime(other);
        return Calendar.getInstance(timeZone).before(otherWithTz);
    }

    private static String getPatternString(@NonNull Context context,
            @StringRes int formatStringResFor12h, @StringRes int formatStringResFor24h) {
        return context.getString(
                DateFormat.is24HourFormat(context) ? formatStringResFor24h : formatStringResFor12h);
    }

    public static final class ParseOptions {
        private final TimeZone timeZone;
        private final String[] dateTimePatterns;

        ParseOptions(String... dateTimePatterns) {
            this(null, dateTimePatterns);
        }

        ParseOptions(TimeZone timeZone, String... dateTimePatterns) {
            this.timeZone = timeZone;
            this.dateTimePatterns = dateTimePatterns;
        }

        TimeZone getTimeZone() {
            return timeZone;
        }

        String[] getDateTimePatterns() {
            return dateTimePatterns;
        }
    }

    /*
    @NonNull
    public static String timeAgo(Context context, long relativeMilli, @Nullable String dateFmt, @Nullable TimeZone tz ) {
        boolean isPast = relativeMilli >= 0;
        relativeMilli = Math.abs(relativeMilli);
        double hours = TimeUnit.MILLISECONDS.toHours(relativeMilli);
        String timeStr;
        if (isPast && hours < 24) {
            if (hours < 1) {
                int min = (int) TimeUnit.MILLISECONDS.toMinutes(relativeMilli);
                timeStr = context.getString(R.string.timeago_n_minutes_ago_long, min);
            } else if (hours < 2) {
                timeStr = context.getString(R.string.timeago_one_hour_ago_long);
            } else {
                timeStr = context.getString(R.string.timeago_n_hours_ago_long, (int) hours);
            }
        } else if ((!isPast && hours < 24) || dateFmt == null) {
            if (hours < 1) {
                int min = (int) TimeUnit.MILLISECONDS.toMinutes(relativeMilli);
                timeStr = String.format(Locale.US, "%d ", min)
                        + context.getString(R.string.timeago_minutes_long);
            } else if (hours < 2) {
                timeStr = "1 "
                        + context.getString(R.string.timeago_hour_long);
            } else {
                timeStr = String.format(Locale.US, "%d ", (int)hours)
                        + context.getString(R.string.timeago_hours_long);
            }
        } else {
            // Make new instance everytime for Thread safety.  SimpleDateFormat is not thread safe.
            SimpleDateFormat dateFormat =
                    new SimpleDateFormat(dateFmt, Locale.getDefault());
            dateFormat.setTimeZone(tz);
            timeStr = dateFormat.format(System.currentTimeMillis() + relativeMilli);
        }

        return timeStr;
    }
    */
}
