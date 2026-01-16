/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.events;

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
