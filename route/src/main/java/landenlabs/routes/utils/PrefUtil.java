/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * TODO - Create encrypted shared preferences.
 *
 * https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences
 */
public class PrefUtil {

    private static final String TAG = PrefUtil.class.getSimpleName();

    /*
    public static SharedPreferences getSharedPref(@NonNull Context context, @NonNull String name) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            return EncryptedSharedPreferences.create(
                    name,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (FileNotFoundException ex) {
            // First run - this error is normal.
            // ALog.e.tagMsg(TAG, "Get shared pref for ", name, " failed:", ex);
        } catch (Exception ex) {
            ALog.e.tagMsg(TAG, "Get shared pref for ", name, " failed:", ex);
        }

        // Fallback to non-encrypted shared preferences.
        return context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }
     */
    public static SharedPreferences getSharedPref2(@NonNull Context context, @NonNull String name) {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }
}
