
/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.Record;

import static landenlabs.routes.Record.RecordBase.ConfigSrv;
import static landenlabs.routes.Record.RecordBase.IdSrv;
import static landenlabs.routes.Record.RecordBase.PauseSrv;
import static landenlabs.routes.Record.RecordBase.ResumeSrv;
import static landenlabs.routes.Record.RecordBase.StartSrv;
import static landenlabs.routes.Record.RecordBase.StopSrv;
import static landenlabs.routes.logger.Analytics.Event.RECORD_STOP;
import static landenlabs.routes.utils.FmtTime.ageMilli;
import static landenlabs.routes.utils.FmtTime.formatAge;
import static landenlabs.routes.utils.GpsUtils.getCurrentLocation;
import static landenlabs.routes.utils.SysUtils.getFusedLocation;
import static landenlabs.routes.utils.SysUtils.getNotificationManager;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ServiceCompat;

import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import landenlabs.routes.GlobalHolder;
import landenlabs.routes.data.ArrayGps;
import landenlabs.routes.data.GpsPoint;
import landenlabs.routes.data.RouteSettings;
import landenlabs.routes.data.Track;
import landenlabs.routes.events.EventAction;
import landenlabs.routes.events.EventBase;
import landenlabs.routes.events.EventStatus;
import landenlabs.routes.events.EventTrack;
import landenlabs.routes.logger.Analytics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

import landenlabs.wx_lib_data.logger.ALog;


/**
 * Foreground service to capture GPS coordinates quickly and save as a route.
 */
public final class RecordService extends Service implements GlobalHolder.EventListener, Runnable, Handler.Callback {

    private final LocationCallback locationCallback = new LocCallback();
    private final ServiceBinder serviceBinder = new ServiceBinder();
    private Handler timer;
    private static final  int MSG_UPD_GPS = 1;

    public Integer notifyId;
    private static boolean isRecordingState = false;
    public Configuration config = new Configuration();

    private GlobalHolder globalHolder;
    private final ArrayGps recPoints = new ArrayGps();
    private Track recTrack = new Track(recPoints);
    private static int recNumPoints = 0;


    // ---------------------------------------------------------------------------------------------

    public static boolean isRecording() {
        return isRecordingState;
    }

    public static Intent createStopRecordingIntent(@NotNull Context context) {
        return (new Intent(context, RecordService.class)).putExtra(StopSrv, true);
    }

    public static Intent createPauseRecordingIntent(@NotNull Context context) {
        return (new Intent(context, RecordService.class)).putExtra(PauseSrv, true);
    }

    public static Intent createResumeRecordingIntent(@NotNull Context context) {
        return (new Intent(context, RecordService.class)).putExtra(ResumeSrv, true);
    }

    public static void requestStopRecording(@NotNull Context context) {
        context.startService(createStopRecordingIntent(context));
    }

    public static void requestPauseRecording(@NotNull Context context) {
        context.startService(createPauseRecordingIntent(context));
    }

    public static void requestResumeRecording(@NotNull Context context) {
        context.startService(createResumeRecordingIntent(context));
    }

    public static void requestStartRecording(@NotNull Context context, int notifyId, @NotNull GlobalHolder globalHolder) {
        Bundle configBundle = globalHolder.recordConfig.asBundle();
        Intent intent = new Intent(context, RecordService.class);
        intent.putExtra(StartSrv, true);
        intent.putExtra(IdSrv, notifyId);
        intent.putExtra(ConfigSrv, configBundle);
        if (Build.VERSION.SDK_INT < 34) {
            context.startService(intent);
            // context.startForegroundService(intent);
        } else {
            context.startForegroundService(intent);
        }
    }

    public static int getNumPoints() {
        return recNumPoints;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        getFusedLocation(getApplicationContext()).removeLocationUpdates(locationCallback);
        globalHolder.removeEventListener(this.getClass().getSimpleName());
        this.stopForeground(true);
        if (timer != null) {
            timer.removeCallbacks(this);
        }
    }

    @NotNull
    public IBinder onBind(@Nullable Intent intent) {
        return (IBinder) this.serviceBinder;
    }

    /**
     * @param intent  The Intent supplied to {@link android.content.Context#startService},
     *                as given.  This may be null if the service is being restarted after
     *                its process has gone away, and it had previously returned anything
     *                except {@link #START_STICKY_COMPATIBILITY}.
     * @param flags   Additional data about this start request.
     * @param startId A unique integer representing this specific request to
     *                start.  Use with {@link #stopSelfResult(int)}.
     */
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        globalHolder = GlobalHolder.getInstance(getApplicationContext());
        ALog.d.tagMsg(this, "service onStartCommand ", intent);
        if (Build.VERSION.SDK_INT >= 34) {
            startAsForegroundService();
        }
        if (intent != null) {
            globalHolder.addEventListener(this.getClass().getSimpleName(), this);
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                if (bundle.containsKey(StopSrv)) {
                    this.stopRecording();
                    return super.onStartCommand(intent, flags, startId);
                } else if (bundle.containsKey(PauseSrv)) {
                    this.pauseRecording();
                    return super.onStartCommand(intent, flags, startId);
                } else if (bundle.containsKey(ResumeSrv)) {
                    this.resumeRecording();
                    return super.onStartCommand(intent, flags, startId);
                } else if (bundle.containsKey(StartSrv)) {
                    this.startRecording(intent);
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Promotes the service to a foreground service, showing a notification to the user.
     * <p>
     * This needs to be called within 10 seconds of starting the service or the system will throw an exception.
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void startAsForegroundService() {
        // promote service to foreground service
        ServiceCompat.startForeground(
                this,
                1,
                globalHolder.notifier.notification(getApplicationContext(), " recording"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
    }

    public void stopRecording() {
        pauseRecording();
        if (Track.isValid(recTrack) && !recTrack.isSummary) {
            globalHolder.trackGrid.saveTrack(recTrack);
        }
        recTrack = null;
        ALog.d.tagMsg(this, "service stopRecording ");
        GlobalHolder.sendEvent(globalHolder, new EventStatus("Stop", ALog.INFO));
        this.stopSelf();

        Analytics.send(RECORD_STOP);
    }

    public void pauseRecording() {
        ALog.d.tagMsg(this, "service pauseRecording ");
        GlobalHolder.sendEvent(globalHolder, new EventStatus("Pause", ALog.INFO));
        if (globalHolder.notifier != null) {
            if (notifyId != null) {
                // EventBus.getDefault().post(new Events.RecordingPausedEvent(notifyId));
                getNotificationManager(getApplicationContext())
                        .notify(notifyId, globalHolder.notifier.setPaused(true)
                                .notification(getApplicationContext(), "Recording paused"));
                getFusedLocation(getApplicationContext()).removeLocationUpdates(locationCallback);
                isRecordingState = false;
            }
        }
    }

    @SuppressLint({"MissingPermission"})
    public void resumeRecording() {
        ALog.d.tagMsg(this, "service resumeRecording ");
        GlobalHolder.sendEvent(globalHolder, new EventStatus("Resume", ALog.INFO));
        if (globalHolder.notifier != null) {
            if (notifyId != null) {
                requestGps("Resume" );
            }
        }
    }

    private void startRecording(Intent intent) {
        recPoints.clear();
        recNumPoints = 0;
        ALog.d.tagMsg(this, "service startRecording ", intent);
        GlobalHolder.sendEvent(globalHolder, new EventStatus("Start", ALog.INFO));

        if (intent != null) {
            Bundle bundle = intent.getExtras();
            Object newGpxId = null;
            if (bundle != null) {
                newGpxId = bundle.get(IdSrv);
            }

            if (newGpxId instanceof Integer) {
                notifyId = (Integer) newGpxId;
                recTrack = new Track(recPoints);
                requestGps("Start");
            }
        }
    }

    public void requestGps(@NonNull String why) {
        config = globalHolder.recordConfig;
        // notifier = new RecordNotify(getApplicationContext(), notifyId);
        Notification notification = globalHolder.notifier.notification(getApplicationContext(),
                why = " recording");
        startForeground((int) notifyId, notification);

        isRecordingState = true;
        requestGPS();
        startTimer();
    }

    @Override
    public void onEvent(@androidx.annotation.Nullable EventBase event) {
        if  (event instanceof EventAction) {
            EventAction eventAction = (EventAction) event;
            if (eventAction.action == EventAction.Action.REQ_REC_TRACK) {
                GlobalHolder.sendEvent(globalHolder, new EventTrack(recTrack));
            }
        }
    }

    @Override
    public void run() {
        // Timer
        startTimer();
    }

    @SuppressLint({"MissingPermission"})
    private void  requestGPS() {
        getFusedLocation(getApplicationContext())
                .requestLocationUpdates(config.locationRequest(),
                        locationCallback, this.getMainLooper());
    }

    private void startTimer() {
        if (timer == null) {
            timer = new Handler(Looper.myLooper(), this);
        }
        timer.sendMessageDelayed( Message.obtain(timer, MSG_UPD_GPS), RouteSettings.gpsSaveMilli);
    }

    @SuppressLint("MissingPermission")
    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_UPD_GPS:
                handleLocation(getCurrentLocation(getApplicationContext()));
                startTimer();
                return true;
        }
        return false;
    }

    public void handleLocation(@Nullable Location loc) {
        if (loc != null) {
            if (ageMilli(loc.getTime()) < RouteSettings.MAX_GPS_AGE_MILLI) {
                if (loc.getTime() != recPoints.last(GpsPoint.NO_POINT).milli) {
                    recPoints.insert(new GpsPoint(loc));
                    recNumPoints = recPoints.size();

                    if (Track.isValid(recTrack) && !recTrack.isSummary) {
                        globalHolder.trackGrid.saveTrack(recTrack);
                        // globalHolder.gpsDataBase.addTrack(recTrack);
                    }
                }
                if (Track.isValid(recTrack)) {
                    GlobalHolder.sendEvent(globalHolder, new EventTrack(recTrack.clone()));
                }
                prevLoc = loc;
                return;
            }
        }
        String msg = (prevLoc != null) ? formatAge(prevLoc.getTime(), "NO GPS for %1$d %2$s") : "No GPS";
        GlobalHolder.sendEvent(globalHolder, new EventStatus(msg, ALog.WARN));
        requestGPS();
    }
    /*
    private void onLocationChanged(Location location) {
        ALog.d.tagMsg(this, "service onLocationChanged ", location);
        if (location != null) {
            if (location.getAccuracy() > (float) 40) {
                ALog.i.tagMsg(this, "accuracy ", location.getAccuracy());
            }
        }
    }
     */

    // =============================================================================================
    // https://github.com/android/location-samples/tree/432d3b72b8c058f220416958b444274ddd186abd
    // https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest
    public static class Configuration {
        private static final String TAG = "LocationActivity";
        private static final long GPS_INTERVAL = TimeUnit.SECONDS.toMillis(10);

        public static Configuration fromBundle(Bundle bundle) {
            return new Configuration(); // TODO - extract settings from bundle
        }

        public LocationRequest locationRequest() {
            LocationRequest.Builder requestBuilder = new LocationRequest.Builder(GPS_INTERVAL);
            if (Build.VERSION.SDK_INT >= 31) {
                // requestBuilder.setQuality(LocationRequest.QUALITY_HIGH_ACCURACY);
            }

            //Def requestBuilder.setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL);
            //Def  requestBuilder.setDurationMillis(Long.MAX_VALUE);
            //Def  requestBuilder.setMaxUpdates(Integer.MAX_VALUE);
            requestBuilder.setPriority(RouteSettings.gpsRequestPermission);
            requestBuilder.setIntervalMillis(RouteSettings.gpsRequestMilli);
            requestBuilder.setMinUpdateIntervalMillis(RouteSettings.gpsRequestMilli /2);
            requestBuilder.setMaxUpdateDelayMillis(RouteSettings.gpsRequestMilli *2);

            return requestBuilder.build();
        }

        public Bundle asBundle() {
            return new Bundle();    // TODO - store settings in bundle
        }
    }

    Location prevLoc = null;
    // =============================================================================================
    private class LocCallback extends LocationCallback {
        public void onLocationAvailability(@NonNull LocationAvailability availability) {
            ALog.d.tagMsg(this, "service availability=", availability);
            String msg = "No GPS available";
            if (prevLoc != null) {
                msg = formatAge(prevLoc.getTime(), "NO GPS for %1$d %2$s");
            }
            GlobalHolder.sendEvent(globalHolder, new EventStatus(msg, ALog.WARN));
            requestGps("Error" );
        }

        public void onLocationResult(@NonNull LocationResult result) {
            ALog.d.tagMsg(this, "service result=", result);
            Location loc = result.getLastLocation();
            handleLocation(loc);
        }
    }

    // =============================================================================================
    public final class ServiceBinder extends Binder {
        public RecordService getService() {
            ALog.d.tagMsg(this, "service bound");
            return RecordService.this;
        }
    }

}
