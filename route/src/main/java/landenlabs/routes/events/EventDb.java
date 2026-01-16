/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.events;

import androidx.annotation.NonNull;


/**
 * Event sent when Database changed
 */
public class EventDb extends EventBase {
    // public Class<? extends BaseWeatherData> classTag;

    public enum DbAction { none, add, delete, error }
    public DbAction action = DbAction.none;

    public EventDb(DbAction action) {
        this.action = action;
    }

    @NonNull
    @Override
    public String toString() {
        String dataStr =  action.name();
        return getClass().getSimpleName() + dataStr;
    }

    @NonNull
    public String logString() {
        return action.name();
    }
}
