/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.Record;

import static landenlabs.routes.Record.RecordBase.cancel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import landenlabs.wx_lib_data.logger.ALog;

/**
 * Simple receiver to handle "Dismiss" notification button press behavior
 *
 * <li>Clear notification</li>
 */
public class RecordCancel extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ALog.d.tagMsg(this, "onReceive intent=", intent);
        if (intent != null) {
            RecordBase info = RecordBase.getInfo(intent);

            if (RecordBase.isValid(info)) {
                cancel(context, info);
                ALog.d.tagMsg(this, "onReceive cancel ", info);
            }
        }
    }
}