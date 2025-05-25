/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.pages.PageRoutesAux;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.landenlabs.routes.data.ArrayListEx;
import com.landenlabs.routes.data.Track;

/**
 * Recycler Adapter to present collection of routes.
 */
public class RouteAdapter  extends RecyclerView.Adapter<RouteItemHolder> {

    private final RouteViewHelper helper;
    private final ArrayListEx<? extends Track> items;
    private final CreateViewHolder makeViewHolder;

    public interface CreateViewHolder {
        RouteItemHolder createViewHolder(@NonNull ViewGroup parent, int viewType, RouteViewHelper helper);
    }

    public RouteAdapter(RouteViewHelper helper, ArrayListEx<? extends Track> items, CreateViewHolder createViewHolder) {
        this.helper = helper;
        this.items = items;
        this.makeViewHolder = createViewHolder;
    }

    @NonNull
    @Override
    public RouteItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return makeViewHolder.createViewHolder(parent, viewType, helper);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteItemHolder holder, int position) {
        Track item = items.get(position);
        holder.onBindViewHolder(item, position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
