/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package landenlabs.wx_lib_data.logger;


import static landenlabs.wx_lib_data.logger.ALogUtils.joinStrings;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Interface which defines Log println and open methods.
 */

public class ALogOut {

    public Context context = null;
    public LogPrinter outPrn = new SysLog();

    public interface LogPrinter {
        int MAX_TAG_LEN = 0;    // was 100, but lets move user tag into message body
        int MAX_TAG_LEN_API24 = 0;  // was 23

        void println(int priority, String tag, Object... msgs);

        void open(@NonNull Context context);

        int maxTagLen();
    }

    // =============================================================================================
    public static class SysLog implements LogPrinter {

        // IllegalArgumentException	is thrown if the tag.length() > 23
        // for Nougat (7.0) releases (API <= 23) and prior, there is
        // no tag limit of concern after this API level.
        static final int LOG_TAG_LEN = (Build.VERSION.SDK_INT >= 24) ? MAX_TAG_LEN_API24 : MAX_TAG_LEN;

        public void println(int priority, String tag, Object... msgs) {
            Context context = null;
            if (false /* isUnitTest() */) {
                System.out.println(tag + joinStrings(context, msgs));
            } else {
                Log.println(priority, tag, joinStrings(context, msgs));
            }
        }

        public void open(@NonNull Context context) {
        }

        public int maxTagLen() {
            return LOG_TAG_LEN;
        }
    }
}
