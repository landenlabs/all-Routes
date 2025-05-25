/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package com.landenlabs.routes.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Event sent when new data arrives in cache
 */
public class EventStatus extends EventBase {

    @Nullable
    public final CharSequence msg;
    public final int level;

    public EventStatus(@Nullable CharSequence msg, int level) {
        this.msg = msg;
        this.level = level;
    }

    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + " msg=" + msg;
    }
}
