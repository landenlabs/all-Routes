/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes;

import static landenlabs.routes.utils.SysUtils.getNotificationManager;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import landenlabs.routes.Record.RecordNotify;
import landenlabs.routes.Record.RecordService;
import landenlabs.routes.data.LiveQueue;
import landenlabs.routes.data.RouteSettings;
import landenlabs.routes.data.Track;
import landenlabs.routes.data.TrackGrid;
import landenlabs.routes.db.SqlDb;
import landenlabs.routes.events.EventBase;
import landenlabs.routes.events.EventDb;
import landenlabs.routes.utils.AppCfg;
import landenlabs.routes.utils.PrefUtil;
import com.wsi.wxdata.WxData;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import landenlabs.wx_lib_data.logger.ALog;

public class GlobalHolder {

    public static final String PREF_NAME = "Routes";
    public static final String PREF_NOTIFY_ID = "NotifyId";
    public static final int DEF_NOTIFY_ID = 1000;
    public static final boolean FETCH_SUMMARY = true;
    public static final boolean FETCH_DETAILS = false;
    private static GlobalHolder instanceRef;
    private static boolean okayToInit = true;

    public WeakReference<Context> contextRef;
    public SqlDb gpsDataBase;
    public SharedPreferences pref;
    public RecordNotify notifier;

    public TrackGrid trackGrid;

    public RecordService.Configuration recordConfig = new RecordService.Configuration();
    public int notifyId = DEF_NOTIFY_ID;

    private final Map<String, EventListener> eventListeners = new HashMap<>();
    private final LiveQueue<EventBase> liveDataQueue = new LiveQueue<>();

    // private final CatchAppExceptions catchAppExceptions;
    public WxData wx;

    public interface EventListener {
        void onEvent(@Nullable EventBase event);
    }

    // ----------------------------------------------------------------------------------------------

    private GlobalHolder(@NonNull Context context) {
        if (okayToInit) {
            okayToInit = false;

            AppCfg.init(context.getApplicationInfo());
            // catchAppExceptions = new CatchAppExceptions(context);
            RouteSettings.init(context);

            contextRef = new WeakReference<>(context);
            SharedPreferences _pref = null;
            try {
                _pref = PrefUtil.getSharedPref2(context, PREF_NAME);
                notifyId = _pref.getInt(PREF_NOTIFY_ID, DEF_NOTIFY_ID);
            } catch (Throwable tr) {
                ALog.e.tagMsg(this, "GlobalHolder init error ", tr);
            }
            pref = _pref;

            notifier = new RecordNotify(context, notifyId);
            gpsDataBase = new SqlDb(context);
            trackGrid = new TrackGrid(gpsDataBase, pref, liveDataQueue);
            trackGrid.loadTracks();

            liveDataQueue.observeForever(event -> {
                synchronized (eventListeners) {
                    ALog.d.tagMsg(this, "onDataChanged observer, Listeners=", eventListeners.size(), ", ", event);
                    for (EventListener eventListener : eventListeners.values()) {
                        eventListener.onEvent(event);
                    }

                    if (event instanceof EventDb) {
                        // Recorder just completed a new track.

                        // Don't need to reload.
                        // trackGrid.loadTracks();
                    }
                }
                liveDataQueue.next();
            });
        }
    }

    /**
     * Used only by Background Scheduler or non-automotive app
     */
    synchronized
    public static GlobalHolder getInstance(@NonNull Context context) {
        if (instanceRef == null) {
            instanceRef = new GlobalHolder(context);
        }
        return instanceRef;
    }

    public static GlobalHolder getInstance() {
        assert (instanceRef != null);
        return instanceRef;
    }

    // -------- Notifications ----------------------------------------------------------------------
    public void notifyTrack(@NonNull Context context, @NonNull Track track) {
        getNotificationManager(context)
                .notify(notifyId, notifier.notification(context,
                        String.format(Locale.US, "Track #pts=%d", track.getPointCnt())));
    }

    // -------- Events -----------------------------------------------------------------------------
    public static void sendEvent(@Nullable GlobalHolder globalHolder, EventBase event) {
        if (globalHolder != null) {
            globalHolder.liveDataQueue.postValue(event);
        }
    }

    public int nextId() {
        notifyId++;
        pref.edit().putInt(PREF_NOTIFY_ID, notifyId).apply();
        return notifyId;
    }

    public void sendEvent(EventBase event) {
        liveDataQueue.postValue(event);
    }

    public void addEventListener(@NonNull String key, @NonNull EventListener listener) {
        synchronized (eventListeners) {
            eventListeners.put(key, listener);
        }
    }

    public void removeEventListener(@NonNull String key) {
        synchronized (eventListeners) {
            eventListeners.remove(key);
        }
    }

    public void clearEventListener() {
        synchronized (eventListeners) {
            eventListeners.clear();
        }
    }
}
