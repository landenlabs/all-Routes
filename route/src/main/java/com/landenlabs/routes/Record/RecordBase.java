/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.Record;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.routes.utils.SysUtils;

import landenlabs.wx_lib_data.logger.ALog;

public class RecordBase {
    public static final String StopSrv = "StopSrv";
    public static final String PauseSrv = "PauseSrv";
    public static final String ResumeSrv = "ResumeSrv";
    public static final String StartSrv = "StartSrv";
    public static final String ConfigSrv = "ConfigSrv";
    public static final String IdSrv = "IdSrv";
    private static final String TAG = RecordBase.class.getSimpleName();
    public int notifyId = -1;

    public static void setIntentParameters(@NonNull Intent notifyIntent, RecordNotify recordNotify) {
        notifyIntent.putExtra(IdSrv, recordNotify.notifyId);
    }

    @Nullable
    public static RecordBase getInfo(@Nullable Intent notifyIntent) {
        if (notifyIntent != null) {
            RecordBase base = new RecordBase();
            base.notifyId = notifyIntent.getIntExtra(IdSrv, -1);
            return base;
        }
        return null;
    }

    public static boolean isValid(RecordBase info) {
        return (info != null) && info.isValid();
    }

    public static void clearActive(Context requireContext) {
        // TODO
    }

    public static NotificationManager getNotificationManager(@NonNull Context context) {
        return SysUtils.getServiceSafe(context, Context.NOTIFICATION_SERVICE);
    }

    public static void cancel(@NonNull Context context, @Nullable RecordBase info) {
        if (info != null) {
            ALog.i.tagMsgStack(TAG, 10, "Cancel Notify=", info);
            getNotificationManager(context).cancel(info.notifyId);
        }
        RecordService.requestStopRecording(context);
    }

    public boolean isValid() {
        return true;
    }
}
