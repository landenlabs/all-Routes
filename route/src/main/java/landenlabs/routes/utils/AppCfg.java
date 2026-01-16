/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.utils;

import android.app.Activity;
import android.content.pm.ApplicationInfo;

public class AppCfg {
    public static boolean DEBUG_APP = true;

    public static void init(Activity activity) {
        if (activity != null && activity.getApplicationInfo() != null) {
            init(activity.getApplicationInfo());
        }
    }
    public static void init(ApplicationInfo appInfo) {
        DEBUG_APP = ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
    }

    public static boolean isDebug() {
        return DEBUG_APP;
    }
}
