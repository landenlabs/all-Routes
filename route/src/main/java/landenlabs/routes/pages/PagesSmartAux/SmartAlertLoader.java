/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package com.landenlabs.routes.pages.PagesSmartAux;

import static com.landenlabs.routes.logger.AppLog.LOG_GETDATA;

import android.net.TrafficStats;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.landenlabs.routes.data.LiveQueue;
import com.landenlabs.routes.events.EventBase;
import com.landenlabs.routes.events.EventSmartAalert;
import com.wsi.wxdata.WxAlertHeadlines;
import com.wsi.wxdata.WxAlertsFetcher;
import com.wsi.wxdata.WxFifteenMinute;
import com.wsi.wxdata.WxFifteenMinuteFetcher;
import com.wsi.wxdata.WxLanguage;
import com.wsi.wxdata.WxLifestyleDriving;
import com.wsi.wxdata.WxLifestyleFetcher;
import com.wsi.wxdata.WxLocation;
import com.wsi.wxdata.WxTime;
import com.wsi.wxdata.WxUnit;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import landenlabs.wx_lib_data.location.SunLocationProvider;
import landenlabs.wx_lib_data.location.WxLocationEx;
import landenlabs.wx_lib_data.logger.ALog;

/**
 * Load SUN data to provide values for SmartAlert.
 */
public class SmartAlertLoader {

    // --- Global values
    public static WxUnit unit = WxUnit.Imperial;
    public static WxLanguage language = WxLanguage.ENGLISH_US;
    public final int maxFifteenPrecip = 24;

    // --- Provided values
    public LiveQueue<EventBase> loadCompleted;
    private int requestSize = 1;

    // --- Internal values
    private final SunLocationProvider.ApiInfo apiInfo;
    private final ArrayBlockingQueue<SmartAlert> alertQueue = new ArrayBlockingQueue<>(20);
    private Thread wxLoaderThread;

    public long networkReadBytes = 0;

    public SmartAlertLoader(@NonNull String sunApiKey) {
        apiInfo = new SunLocationProvider.ApiInfo(sunApiKey, new StringBuffer(language.apiValue()));
    }

    synchronized
    public void load(@NonNull List<SmartAlert> smartAlerts, @NonNull LiveQueue<EventBase> loadCompleted) {
        done();
        this.loadCompleted = loadCompleted;
        alertQueue.clear();
        if (smartAlerts.size() > 0) {
            alertQueue.addAll(smartAlerts);
            requestSize = smartAlerts.size();
            startWxLoaderThread();
        } else {
            postValue(new EventSmartAalert(null, null, 0));
        }
    }

    public boolean isDone() {
        return alertQueue.isEmpty();
    }

    public float getPendingPercent() {
        return (requestSize > 0) ? (float)alertQueue.size() / requestSize : 1f;
    }

    synchronized
    public void done() {
        ALog.i.tagMsg(this, "Loading SmartAlert DONE");

        if (wxLoaderThread != null) {
            wxLoaderThread.interrupt();
            wxLoaderThread = null;
        }
        loadCompleted = null;
        alertQueue.clear();
        requestSize = 1;
    }

    public static String geoPosToStr(double latOrLng) {
        return String.format(Locale.US, "%.3f", latOrLng);
    }

    private void startWxLoaderThread() {
        if (wxLoaderThread == null) {

            wxLoaderThread = new Thread("LoadSmartAlerts") {
                @Override
                public void run() {
                    Looper.prepare();
                    long rdBytes1 = TrafficStats.getUidRxBytes(android.os.Process.myUid());
                    int startSize = alertQueue.size();
                    while (!isInterrupted() && !alertQueue.isEmpty()) {
                        SmartAlert smartAlert = null;
                        float percentPending = (float)(alertQueue.size()-1)/startSize;
                        try {
                            smartAlert = alertQueue.take();
                            smartAlert.hasValidData = false;
                            smartAlert.hasValidLoc = true;

                            WxLocation loadLocation = smartAlert.wxLocation;
                            WxTime timeRange = smartAlert.timeRange;
                            setWxLocation(smartAlert);

                            CompletableFuture<WxAlertHeadlines> alertComplete = new CompletableFuture<>();
                            CompletableFuture<WxLifestyleDriving> driveComplete = new CompletableFuture<>();
                            CompletableFuture<WxFifteenMinute> fifteenComplete = new CompletableFuture<>();

                            WxAlertsFetcher alertsFetcher =
                                    loadWxAlerts(smartAlert, loadLocation, unit, language, alertComplete);
                            WxLifestyleFetcher<WxLifestyleDriving> driveFetcher =
                                    loadWxDriving(smartAlert, loadLocation, unit, language, timeRange, driveComplete);
                            WxFifteenMinuteFetcher fifteenMinuteFetcher =
                                    loadWxFifteen(smartAlert, loadLocation, language, maxFifteenPrecip, fifteenComplete);

                            CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(alertComplete, driveComplete, fifteenComplete);
                            combinedFuture.get(10, TimeUnit.SECONDS);
                            smartAlert.hasValidData = true;
                            networkReadBytes = TrafficStats.getUidRxBytes(android.os.Process.myUid()) - rdBytes1;
                            ALog.i.tagMsg(this, "Loaded SmartAlert for ", smartAlert.pt, " Pending:", alertQueue.size(), " NetBytes:", networkReadBytes);
                            postValue(new EventSmartAalert(smartAlert, null, percentPending));
                        } catch (Exception ex) {
                            if (!alertQueue.isEmpty()) {
                                networkReadBytes = TrafficStats.getUidRxBytes(android.os.Process.myUid()) - rdBytes1;
                                ALog.e.tagMsg(this, "Load SmartAlert thread error ", ex);
                                postValue(new EventSmartAalert(smartAlert, ex, percentPending));
                            }
                        }
                    }
                    postValue(new EventSmartAalert(null, null, 0));
                }
            };
            wxLoaderThread.start();
        }
    }

    @WorkerThread
    private void setWxLocation(@NonNull final SmartAlert smartAlert) {
        String latStr = geoPosToStr(smartAlert.pt.latitude);
        String lngStr = geoPosToStr(smartAlert.pt.longitude);
        if (false) {
            // Thread blocks
            List<WxLocation> locations = SunLocationProvider.getSunLocationPoi(latStr, lngStr, apiInfo);
            if (locations.size() > 0) {
                smartAlert.wxLocation = locations.get(0);
                smartAlert.hasValidLoc = true;
            }
        } else {
            getWxLocationAsync(smartAlert, 2);
        };
    }

    private void getWxLocationAsync(@NonNull final SmartAlert smartAlert, int retry) {
        if (retry > 0) {
            String latStr = geoPosToStr(smartAlert.pt.latitude);
            String lngStr = geoPosToStr(smartAlert.pt.longitude);
            // Thread does not block
            CompletableFuture.supplyAsync(() -> {
                return SunLocationProvider.getSunLocationPoi(latStr, lngStr, apiInfo);
            }).whenComplete((locations, exception) -> {
                if (locations != null && locations.size() > 0) {
                    smartAlert.wxLocation = locations.get(0);
                    smartAlert.hasValidLoc = true;
                } else {
                    ALog.e.tagMsg(this, "Location fetch ex=", exception);
                    getWxLocationAsync(smartAlert, retry - 1);
                    smartAlert.hasValidLoc = false;
                }
            });
        }
    }

    synchronized
    private void postValue(EventBase event) {
        if (loadCompleted != null) {
            loadCompleted.postValue(event);
        }
    }

    // =============================================================================================

    private WxAlertsFetcher loadWxAlerts(
            @NonNull SmartAlert smartAlert,
            @NonNull WxLocation wxLocation,
            @NonNull WxUnit unit,
            @NonNull WxLanguage language,
            @NonNull CompletableFuture<WxAlertHeadlines> complete) {
        WxTime time = WxTime.now();
        WxAlertsFetcher alertFetcher = new WxAlertsFetcher();
        alertFetcher.setUnit(unit);
        alertFetcher.setLanguage(language);
        alertFetcher.setTime(time);
        alertFetcher.setLocation(wxLocation);
        alertFetcher.addFetchListener((myFetcher, data, error) -> {
            LOG_GETDATA.d().tagMsg("GetData Alert queue error=", error, " data=", data, " loc=", WxLocationEx.log(wxLocation));
            if (data instanceof WxAlertHeadlines && error == null) {
                smartAlert.setAlert((WxAlertHeadlines) data);
                complete.complete((WxAlertHeadlines) data);
            } else if (error == null)
                complete.complete(null);
            else
                complete.cancel(false);
            myFetcher.clearFetchListeners();
        });
        LOG_GETDATA.d().tagMsg("GetData loadWxAlerts request for ", WxLocationEx.log(wxLocation));
        alertFetcher.fetch();
        return alertFetcher;
    }

    private WxLifestyleFetcher<WxLifestyleDriving> loadWxDriving(
            @NonNull SmartAlert smartAlert,
            @NonNull WxLocation wxLocation,
            @NonNull WxUnit unit,
            @NonNull WxLanguage language,
            WxTime timeRange,
            @NonNull CompletableFuture<WxLifestyleDriving> complete) {
        WxLifestyleFetcher<WxLifestyleDriving>  driveFetcher = new WxLifestyleFetcher<>(WxLifestyleDriving.class);
        driveFetcher.setUnit(unit);
        driveFetcher.setLanguage(language);
        driveFetcher.setTime(timeRange);
        driveFetcher.setLocation(wxLocation);
        driveFetcher.addFetchListener((myFetcher, data, error) -> {
            if (data instanceof WxLifestyleDriving && error == null) {
                smartAlert.setDriving((WxLifestyleDriving) data);
                complete.complete((WxLifestyleDriving) data);
            } else if (error == null)
                complete.complete(null);
            else
                complete.cancel(false);
            myFetcher.clearFetchListeners();
        });
        LOG_GETDATA.d().tagMsg(this, "GetData loadWxDriving", timeRange.representsArray() ? "Day" : "Current", " request ", WxLocationEx.log(wxLocation));
        driveFetcher.fetch();
        return driveFetcher;
    }

    private WxFifteenMinuteFetcher loadWxFifteen(
            @NonNull SmartAlert smartAlert,
            @NonNull WxLocation wxLocation,
            @NonNull WxLanguage language,
            int numHours,
            @NonNull CompletableFuture<WxFifteenMinute> complete) {
        WxTime time = WxTime.comingHours(numHours);
        WxFifteenMinuteFetcher fifteenFetcher = new WxFifteenMinuteFetcher();
        fifteenFetcher.setUnit(WxUnit.Metric);  // Force to metric - better fidelity in precip graph.
        fifteenFetcher.setLanguage(language);
        fifteenFetcher.setTime(time);
        fifteenFetcher.setLocation(wxLocation);
        fifteenFetcher.addFetchListener((myFetcher, data, error) -> {
            // LOG_GETDATA.d().tagMsg(this, "GotData loadWxFifteen  ", data, " error=", error);
            if (data instanceof WxFifteenMinute && error == null) {
                smartAlert.setWeather((WxFifteenMinute) data);
                complete.complete((WxFifteenMinute) data);
            } else if (error == null)
                complete.complete(null);
            else
                complete.cancel(false);
            myFetcher.clearFetchListeners();
        });
        LOG_GETDATA.d().tagMsg(this, "GetData loadWxFifteen request ",  WxLocationEx.log(wxLocation));
        fifteenFetcher.fetch();
        return  fifteenFetcher;
    }

}
