/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package com.landenlabs.routes.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.routes.data.Track;

/**
 * Event sent when new data collected.
 */
public class EventTrack extends EventBase {

    @Nullable
    public final Track data;

    public EventTrack(@Nullable Track result) {
        data = result;
    }

    @NonNull
    @Override
    public String toString() {
        String dataStr = (data == null) ? "NoTrack" : (data.getClass().getSimpleName() + " " + data);
        return getClass().getSimpleName() + " data=" + dataStr;
    }

    @NonNull
    public String logString() {
        return (data != null) ? data.logString() : "NoTrack";
    }
}
