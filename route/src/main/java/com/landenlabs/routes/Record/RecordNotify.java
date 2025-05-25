/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.Record;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.landenlabs.routes.Record.RecordBase.getNotificationManager;
import static com.landenlabs.routes.Record.RecordBase.setIntentParameters;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.landenlabs.routes.MainActivity;
import com.landenlabs.routes.R;

public class RecordNotify {

    private static final String CHANNEL_ALERT_ID = "Alerts";
    private static final String CHANNEL_ALERT_NAME = "Weather Alerts";
    private static final boolean SET_CUSTOM_SOUND = false;
    private static final int SOUND_RES = R.raw.tada;        // R.raw.notify_sound3
    public static boolean showHeadsUp = false;
    private final String title;
    private final String ticker;
    public int notifyId;
    private final String openStr;
    private final String stopStr;
    private final String pauseStr;
    private final String resumeStr;
    private final String cancelStr;

    public RecordNotify(@NonNull Context context, int notifyId) {
        this.notifyId = notifyId;
        this.title = context.getString(R.string.notification_title);
        this.ticker = context.getString(R.string.ticker_text);

        openStr = context.getString(R.string.open);
        stopStr = context.getString(R.string.stop);
        pauseStr = context.getString(R.string.pause);
        cancelStr = context.getString(R.string.cancel);
        resumeStr = context.getString(R.string.resume);

        NotificationManager notificationManager = getNotificationManager(context);

        NotificationChannel alertChannel = new NotificationChannel(
                CHANNEL_ALERT_ID,
                CHANNEL_ALERT_NAME,
                NotificationManager.IMPORTANCE_DEFAULT);    // IMPORTANCE_HIGH
        alertChannel.setDescription("TWC GPS");
        /*
        if (SET_CUSTOM_SOUND) {
            Uri sndUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + SOUND_RES);
            AudioAttributes audioAttr = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            alertChannel.setSound(sndUri, audioAttr);
        }
         */
        notificationManager.createNotificationChannel(alertChannel);
    }

    public RecordNotify setPaused(boolean b) {
        return this;    // TODO - handle pause
    }

    public Notification notification(@NonNull Context context, @NonNull String message) {

        // Create intent to open (detail) active notification
        Intent notifyOpenAppIntent = new Intent(context, MainActivity.class);
        notifyOpenAppIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notifyOpenAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notifyOpenAppIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        setIntentParameters(notifyOpenAppIntent, this);
        PendingIntent pendingNotifyOpenIntent = PendingIntent.getActivity(context, notifyId, notifyOpenAppIntent,
                FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);

        // Create intent to cancel (dismiss) active notification
        Intent notifyCancelIntent = new Intent(context, RecordCancel.class);
        notifyCancelIntent.setAction(Intent.ACTION_DELETE);
        setIntentParameters(notifyCancelIntent, this);
        PendingIntent pendingNotifyCancelIntent = PendingIntent.getBroadcast(context, notifyId, notifyCancelIntent,
                FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);

        NotificationCompat.Builder notifierBuilder;
        notifierBuilder = new NotificationCompat.Builder(context, CHANNEL_ALERT_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.logo)
                .setOnlyAlertOnce(true)
                //        .setAutoCancel(true)
                // .setOngoing(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentIntent(pendingNotifyOpenIntent)
                // .addAction(0, openStr, pendingNotifyOpenIntent)
                // .addAction(0, stopStr, pendingNotifyCancelIntent)
                //.setTicker(ticker)
                ;

        if (!showHeadsUp) {
            notifierBuilder.setCustomHeadsUpContentView(null);
        }

        return notifierBuilder.build();
    }
}
