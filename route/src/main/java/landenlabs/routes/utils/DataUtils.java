/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package com.landenlabs.routes.utils;

import android.content.Context;
import android.util.Base64;

import androidx.annotation.NonNull;

public class DataUtils {

    @NonNull
    public static String getString1x(@NonNull Context context, @NonNull String name) {
        String tag = name + (com.landenlabs.routes.BuildConfig.DEBUG ? "_d1" : "_x1");
        // Optionally use debug if DEBUG (_d1) build else fallback to RELEASE (_x1)
        int strRes = context.getResources().getIdentifier(tag, "string", context.getPackageName());
        if (strRes == 0 && com.landenlabs.routes.BuildConfig.DEBUG)   // Fallback to RELEASE (_x1)
            strRes = context.getResources().getIdentifier(name + "_x1", "string", context.getPackageName());
        if (strRes > 0) {
            String value = context.getResources().getString(strRes);
            // Reverse uuencoding of string.  See build.gradle for encoding.
            byte[] decoded = Base64.decode(value, Base64.DEFAULT);
            return new String(decoded);
        }
        return name;
    }
}
