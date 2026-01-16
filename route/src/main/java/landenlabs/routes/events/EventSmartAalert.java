/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import landenlabs.routes.pages.PagesSmartAux.SmartAlert;

import java.util.Locale;

public class EventSmartAalert extends EventBase {

    @Nullable
    public final SmartAlert data;
    @Nullable
    public final Exception exception;
    public final float percentPending;

    public EventSmartAalert(@Nullable SmartAlert data, @Nullable Exception ex, float percentPending) {
        this.data = data;
        this.exception = ex;
        this.percentPending = percentPending;
    }

    @NonNull
    @Override
    public String toString() {
        String dataStr = (data == null) ? "NoData" : data.getClass().getSimpleName();
        return getClass().getSimpleName() + " data=" + dataStr + String.format(Locale.US, " pending:%.1f", percentPending);
    }

}
