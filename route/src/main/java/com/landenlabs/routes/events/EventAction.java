/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Event sent to perform an action
 */
public class EventAction extends EventBase {

    public enum Action { REQ_REC_TRACK }
    public final Action action;

    public EventAction(@Nullable Action action) {
        this.action = action;
    }

    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + "=" + action.name();
    }
}
