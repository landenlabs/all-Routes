package landenlabs.routes.logger;

import android.app.Activity;

import com.google.firebase.analytics.FirebaseAnalytics;

public class Analytics {

    private static FirebaseAnalytics analytics;

    public static void init(Activity activity) {
        analytics = FirebaseAnalytics.getInstance(activity);
    }

    public static enum Event { PAGE_DEV, PAGE_RECORDER, PAGE_ROUTES, PAGE_SETTINGS, PAGE_SUMMARY, PAGE_WEATHER, RECORD_STOP };

    public static void send(Event event) {
        analytics.logEvent(event.name(), null);
    }
}
