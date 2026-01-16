/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.utils;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wsi.wxdata.datetime.WxDateTime;

import org.joda.time.DateTime;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Help with Time/Date presentation.
 */
public class FmtTime {
    public static int milliToSec(long milli) {
        return Math.abs((int)TimeUnit.MILLISECONDS.toSeconds(milli));
    }
    public static int secToMilli(long sec) {
        return Math.abs((int)TimeUnit.SECONDS.toMillis(sec));
    }
    public static long ageMilli(long prevMilli) {
        return System.currentTimeMillis() - prevMilli;
    }
    public static String formatAge(long prevMilli, String fmtStr) {
        long milliSince = System.currentTimeMillis() - prevMilli;
        long val = milliSince / 1000;
        String unit = "sec";
        if (val > 60*2) {
            val = val / 60;
            unit = "min";
        }
        return String.format(Locale.US, fmtStr, val, unit); // "NO GPS for %d %s"
    }
    public static final PeriodFormatter periodFmter =
            new PeriodFormatterBuilder()
                    .appendDays()
                    .appendSuffix(" day", " days")
                    .appendSeparator(" ")
                    .printZeroIfSupported()
                    .minimumPrintedDigits(2)
                    .appendHours()
                    .appendSeparator(":")
                    .appendMinutes()
                    .printZeroIfSupported()
                    .minimumPrintedDigits(2)
                    .appendSeparator(":")
                    .appendSeconds()
                    .minimumPrintedDigits(2)
                    .toFormatter();

    public static CharSequence fmtDuration(long millis) {
        if (millis < 0) {
            String msg = "Bad Time";
            SpannableString ss = new SpannableString(msg);
            ss.setSpan(new ForegroundColorSpan(Color.RED), 0, msg.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return ss;
        }

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long min = TimeUnit.MILLISECONDS.toMinutes(millis);
        if (hours > 0)
            return String.format(Locale.US, "%.1f hrs", hours + min / 60f);
        else
            return String.format(Locale.US, "%d min", min);
    }

    public static int getNearest(@NonNull WxDateTime wantDT, @NonNull ArrayList<WxDateTime> listDT) {
        int nearIdx = 0;
        if (!listDT.isEmpty()) {
            long nearMill = Math.abs(wantDT.getMillis() - listDT.get(0).getMillis());
            for (int idx = 1; idx < listDT.size(); idx++) {
                long deltaMill = Math.abs(wantDT.getMillis() - listDT.get(idx).getMillis());
                if (deltaMill < nearMill) {
                    nearMill = deltaMill;
                    nearIdx = idx;
                }
            }
        }
        return nearIdx;
    }

    public static String toString(@Nullable WxDateTime dt, @NonNull String fmt) {
        return (dt != null) ? dt.toString(fmt) : "";
    }

}
