/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package com.landenlabs.routes.pages;

import static com.landenlabs.routes.logger.Analytics.Event.PAGE_ROUTES;
import static com.landenlabs.routes.utils.GpsUtils.getCurrentLocation;
import static com.landenlabs.routes.utils.GpsUtils.toWLatLng;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.landenlabs.routes.R;
import com.landenlabs.routes.data.ArrayListEx;
import com.landenlabs.routes.data.GpsPoint;
import com.landenlabs.routes.data.Track;
import com.landenlabs.routes.data.TrackCommon;
import com.landenlabs.routes.data.TrackGrid;
import com.landenlabs.routes.data.TrackUtils;
import com.landenlabs.routes.data.Trip;
import com.landenlabs.routes.data.TripDow;
import com.landenlabs.routes.databinding.PageRoutesFragBinding;
import com.landenlabs.routes.events.EventBase;
import com.landenlabs.routes.logger.Analytics;
import com.landenlabs.routes.map.MapMarkers;
import com.landenlabs.routes.map.MapTracks;
import com.landenlabs.routes.map.MapViewer;
import com.landenlabs.routes.pages.PageRoutesAux.RouteAdapter;
import com.landenlabs.routes.pages.PageRoutesAux.RouteItemHolder;
import com.landenlabs.routes.pages.PageRoutesAux.RouteViewHelper;
import com.landenlabs.routes.pages.PageUtils.ToastUtils;
import com.mapbox.mapboxsdk.maps.MapView;
import com.weather.pangea.event.MapLongTouchEvent;
import com.wsi.mapsdk.map.WSIMapType;
import com.wsi.mapsdk.map.WSIMapView;
import com.wsi.mapsdk.utils.WLatLng;

import org.joda.time.DateTime;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import landenlabs.wx_lib_data.logger.ALog;

/**
 * Present list of recorded routes.
 */
@SuppressWarnings("FieldCanBeLocal")
public class PageRoutesFrag extends PageBaseFrag implements View.OnClickListener
        , MapView.OnCameraDidChangeListener
        , MapViewer.OnWSIMapViewChangedCallback
        , MapViewer.MapLongTouchListener {

    private PageRoutesFragBinding binding;
    private RouteAdapter adapter;
    private RouteViewHelper helper;
    private boolean showDelete = false;
    private final ArrayListEx<Track> rawTracks = new ArrayListEx<>();
    private ArrayListEx<? extends Track> tracks = rawTracks;
    private MapTracks mapTracks;
    private MapMarkers mapMarkers;


    private int sortBy = R.id.menu_sortByStart;
    private static final HashMap<Integer, Comparator<Track>> sortMenuIds = new HashMap<>();
    static {
        sortMenuIds.put( R.id.menu_sortByStart, Comparator.comparing(Track::getMilliStart));
        sortMenuIds.put( R.id.menu_sortByDOW, Comparator.comparing(track -> new DateTime(track.getMilliStart()).getDayOfWeek()));
        sortMenuIds.put( R.id.menu_sortByHour, Comparator.comparing(track -> new DateTime(track.getMilliStart()).getHourOfDay()));
        sortMenuIds.put( R.id.menu_sortByLength, Comparator.comparing(Track::getMeters));
        sortMenuIds.put( R.id.menu_sortByTrip, new TrackUtils.SortByTrip());
        sortMenuIds.put( R.id.menu_sortByPoints, Comparator.comparing(Track::getPointCnt));
    }

    // ---------------------------------------------------------------------------------------------

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        authorizeMap(); // Call before inflating MapViewer
        binding = PageRoutesFragBinding.inflate(inflater, container, false);
        init(binding.getRoot());
        Analytics.send(PAGE_ROUTES);
        initUi();
        initList();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.showNavBar(true);
        viewModel.showToolBar(true);
        rawTracks.clear();
        SparseArray<Track> summaryTracks = getGlobal().trackGrid.getSummaryTracks();
        for (int idx = 0; idx < summaryTracks.size(); idx++)  rawTracks.add(summaryTracks.valueAt(idx));
        MapTracks.done(mapTracks);
        MapMarkers.done(mapMarkers);
        initMap(binding.mapViewer);
        getGlobal().addEventListener(this.getClass().getSimpleName(), this);
        refreshUi();
    }

    @Override
    public void onPause() {
        binding.mapViewer.removeOnMapChangedCallback(this);         //  onMapReady()
        binding.mapViewer.removeOnCameraDidChangeListener(this);    //  onCameraDidChange()
        getGlobal().removeEventListener(this.getClass().getSimpleName());
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onEvent(@Nullable EventBase event) {
        rawTracks.clear();
        SparseArray<Track> summaryTracks = getGlobal().trackGrid.getSummaryTracks();
        for (int idx = 0; idx < summaryTracks.size(); idx++)  rawTracks.add(summaryTracks.valueAt(idx));
        refreshUi();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.route_show_map) {
            binding.routeShowMap.setChecked(!binding.routeShowMap.isChecked());
            refreshUi();
        } else if (id == R.id.route_show_trips) {
            binding.routeShowTrips.setChecked(!binding.routeShowTrips.isChecked());
            initList();
            refreshUi();
        } else if (id == R.id.route_show_dow) {
            binding.routeShowDow.setChecked(!binding.routeShowDow.isChecked());
            initList();
            refreshUi();
        } else if (id == R.id.rte_delete) {
            if (helper.selectedSet.isEmpty()) {
                ToastUtils.show(root, "No routes selected to delete");
                return;
            }
            helper.selectedSet.stream().sorted(Comparator.reverseOrder()).forEach( idx -> {
                Track track = tracks.get(idx, null);
                if (track != null) {
                    getGlobal().trackGrid.deleteTrack(track.getId());
                }
                rawTracks.remove(track);
            });

            showDelete = false;
            // tracks.clear();
            helper.selectedSet.clear();
            MapTracks.done(mapTracks);
            MapMarkers.done(mapMarkers);
            refreshUi();
            // adapter.notifyDataSetChanged();
            initList();
            // getGlobal().getDbTracks(FETCH_DETAILS);
        } else if (id == R.id.rte_del_select_all) {
            for (int idx = 0; idx < tracks.size(); idx++)
                helper.selectedSet.add(idx);
            refreshUi();
            adapter.notifyDataSetChanged();
        } else if (id == R.id.rte_del_select_none) {
            helper.selectedSet.clear();
            refreshUi();
            adapter.notifyDataSetChanged();
        } else if (id == R.id.rte_del_toggle_show) {
            binding.rteDelToggleShow.setChecked(! binding.rteDelToggleShow.isChecked());
            helper.showOnlyChecked = binding.rteDelToggleShow.isChecked();
            adapter.notifyDataSetChanged();
        } else if (view.getTag(helper.clickTagId) instanceof Integer) {
            int viewPos = (Integer)view.getTag(helper.clickTagId);
            if (view instanceof CheckBox) {
                CheckBox selected = (CheckBox) view;
                if (selected.isChecked()) {
                    helper.selectedSet.add(viewPos);
                } else {
                    helper.selectedSet.remove(viewPos);
                }
                showTracksOnMap(helper.selectedSet, tracks, getGlobal().trackGrid);
                binding.rteDelete.setText(getString(R.string.rte_delete, helper.selectedSet.size()));
            }
        }
    }

    private void showTracksOnMap(
            @NonNull Set<Integer> selectedSet,
            @NonNull ArrayListEx<? extends Track> tracks,
            @NonNull TrackGrid trackGrid) {
        if (showDelete
                || !binding.routeShowMap.isChecked()
                || !binding.mapViewer.isReady()
                || binding.mapViewer.getTopLayer() == null)
            return;

        mapTracks = MapTracks.create(mapTracks, binding.mapViewer, trackGrid, getViewLifecycleOwner());
        mapMarkers  = new MapMarkers(requireContext(), binding.mapViewer.getWSIMap());

        // WLatLngBounds bounds = null;
        for (int idx : selectedSet) {
            Track track = tracks.get(idx, null);
            if (Track.isValid(track)) {
       //          mapTracks.addTrack(track);
                mapMarkers.addMarker(track.points.first(GpsPoint.NO_POINT).toLatLng(), MapMarkers.START_MARKER);
                mapMarkers.addMarker(track.points.last(GpsPoint.NO_POINT).toLatLng(), MapMarkers.END_MARKER);
                mapTracks.addTrack(track);
            }
        }
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menu.clear();
        menuInflater.inflate(R.menu.side_menu_routes, menu);
        refreshUi();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        @IdRes int id = menuItem.getItemId();
        menuItem.setChecked(!menuItem.isChecked());
        if (id == R.id.menu_delete) {
            showDelete = !showDelete;
            if (!showDelete) helper.showOnlyChecked = false;
            if (binding.routeShowTrips.isChecked() && showDelete) {
                binding.routeShowTrips.setChecked(false);
                initList();
            } else {
                adapter.notifyDataSetChanged();
            }
        } else  if (id == R.id.menu_bounds) {
            MapTracks.showBounds = menuItem.isChecked();
        } else  if (id == R.id.menu_grid) {
            MapTracks.showGrid = menuItem.isChecked();
        } else  if (id == R.id.menu_cells) {
            MapTracks.showCells = menuItem.isChecked();
        } else  if (id == R.id.menu_radar) {
            binding.mapViewer.showRadar(menuItem.isChecked());
        } else if (sortMenuIds.containsKey(menuItem.getItemId())) {
            sortBy = menuItem.getItemId();
            tracks.sort(sortMenuIds.get(sortBy));
            adapter.notifyDataSetChanged();
            ToastUtils.show(root, "Sorted by " + menuItem.getTitle());
        }
        refreshUi();
        return false;
    }

    private boolean isMapShown() {
        return  binding.routeShowMap.isChecked();
    }

    @SuppressLint({"NotifyDataSetChanged", "SetTextI18n"})
    public void refreshUi() {
        binding.rteDeleteGrp.setVisibility(showDelete ? View.VISIBLE : View.GONE);
        binding.mapViewer.setVisibility((showDelete || !isMapShown()) ? View.GONE : View.VISIBLE);
        binding.rteDelete.setText(getString(R.string.rte_delete, helper.selectedSet.size()));
        adapter.notifyDataSetChanged();
        showTracksOnMap(helper.selectedSet, tracks, getGlobal().trackGrid);
        binding.routeNoRoutes.setVisibility(tracks.isEmpty() ? View.VISIBLE : View.GONE);
        binding.routeStatus.setText(String.format(Locale.US, "#%d", tracks.size()));
    }

    private void initUi() {
        binding.rteDelete.setOnClickListener(this);
        binding.rteDelSelectAll.setOnClickListener(this);
        binding.rteDelSelectNone.setOnClickListener(this);
        binding.rteDelToggleShow.setOnClickListener(this);

        binding.routeShowMap.setOnClickListener(this);
        binding.routeShowTrips.setOnClickListener(this);
        binding.routeShowDow.setOnClickListener(this);

        binding.mapViewer.setOnLongClickListener(this);
    }
    private void initList() {
        helper = new RouteViewHelper(requireContext(), R.id.selected, this);
        helper.showCheckbox = true;
        // helper.showOnlyChecked = false;
        helper.selectedSet.clear();
        if (binding.routeShowTrips.isChecked()) {
            // Group tracks by trip (similar start and end)
            tracks = Trip.tripsFrom(rawTracks);
            adapter = new RouteAdapter(helper, tracks, RouteItemHolder::createTripHolderFor);
        } else {
            tracks = rawTracks;
            adapter = new RouteAdapter(helper, tracks, RouteItemHolder::createTrackHolderFor);
        }
        if (binding.routeShowDow.isChecked()) {
            tracks = TripDow.dowFrom(rawTracks);
            adapter = new RouteAdapter(helper, tracks, RouteItemHolder::createDowHolderFor);
        }
        binding.routeList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.routeList.setAdapter(adapter);
        binding.routeList.setHasFixedSize(false);
        tracks.sort(sortMenuIds.get(sortBy));
    }


    //==============================================================================================
    // MapView

    private void authorizeMap() {
        if (!WSIMapView.isAuthorized()) {
            ALog.d.tagMsg(this, "Authorize map");
            MapViewer.initBeforeCreate(requireContext());
        }
    }

    private void initMap(MapViewer mapView) {
        String mapName = mapView.getTag().toString();
        mapView.getWSIMap().setMapType(WSIMapType.LIGHT);
        mapView.addOnMapChangedCallback(this);  // onMapReady()

        // "RadarSmooth";  // twcRadarMossaic + radarFcst
        String rasterName = "NoRaster";
        if (!mapView.setRasterLayer(rasterName))
            ALog.w.tagMsg(mapName, "Failed to set map raster layer " + rasterName);
        if (!mapView.setOverlayLayers(null))
            ALog.w.tagMsg(mapName, "Failed to set map overlay layers ");

        MapViewer.setTimeline(DateTime.now(), mapView);
        mapView.addOnCameraDidChangeListener(this);        // onCameraDidChange()
    }

    @Override
    public void onMapReady(WSIMapView wsiMapView, int why) {
        if (why == MAP_STATE_READY) {
            Location location = getCurrentLocation(requireActivity());
            if (location != null) {
                binding.mapViewer.setCamera(new WLatLng(location.getLatitude(), location.getLongitude()), 13, true, 1.0f);
            }
        }
        refreshUi();
    }
    @Override
    public void onCameraDidChange(boolean animated) {
       //  refreshUi();  causes circular code loop.
    }

    @Override
    public void onLongTouch(MapViewer mapViewer, MapLongTouchEvent event) {
        WLatLng latLng = toWLatLng(event.getCenterLatLng());
        String locStr = String.format(Locale.US, "%.3f,%.3f", latLng.latitude, latLng.longitude);
        // ClipData clip = ClipData.newPlainText("label", locStr);
        // ClipboardManager clipboard = (ClipboardManager) mapViewer.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        // clipboard.setPrimaryClip(clip);

        TrackCommon cellInfo = getGlobal().trackGrid.getCellInfo(latLng);
        if (cellInfo != null) {
            ToastUtils.show(root,  cellInfo.toInfo());
        } else {
            Toast.makeText(mapViewer.getContext(), locStr, Toast.LENGTH_SHORT).show();
        }
    }
}
