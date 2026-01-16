/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.pages.PagesSmartAux;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import landenlabs.routes.data.ArrayListEx;

/**
 * Recycler Adapter to present collection of SmartAlerts.
 */
public class SmartAdapter extends RecyclerView.Adapter<SmartItemHolder> {

    private final SmartViewHelper helper;
    private final ArrayListEx<SmartAlert> items;
    private final CreateViewHolder makeViewHolder;

    public interface CreateViewHolder {
        SmartItemHolder createViewHolder(@NonNull ViewGroup parent, int viewType, SmartViewHelper helper);
    }

    public SmartAdapter(SmartViewHelper helper, ArrayListEx<SmartAlert> items, CreateViewHolder createViewHolder) {
        this.helper = helper;
        this.items = items;
        this.makeViewHolder = createViewHolder;
    }

    @NonNull
    @Override
    public SmartItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return makeViewHolder.createViewHolder(parent, viewType, helper);
    }

    @Override
    public void onBindViewHolder(@NonNull SmartItemHolder holder, int position) {
        SmartAlert item = items.get(position);
        holder.onBindViewHolder(item, position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
