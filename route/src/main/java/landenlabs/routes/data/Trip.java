/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.data;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Collection of similar tracks which define single trip.
 * Tracks with similar end points. Both forward and reverse direction.
 */
public class Trip extends Track {

    public Set<Track> tracks = new HashSet<>();

    public Trip() {
        super(new Track());
    }

    public Trip(@NonNull Track track) {
        super(track);
        tracks.add(track.cloneSummary());
    }

    public static ArrayListEx<Trip> tripsFrom(ArrayListEx<Track> tracks) {
        tracks.sort(new TrackUtils.SortByTrip());
        ArrayListEx<Trip> trips = new ArrayListEx<>(tracks.size()/2);
        if (tracks.size() > 0) {
            Trip trip = new Trip(tracks.get(0));
            // trips.add(trip);
            for (int idx = 1; idx < tracks.size(); idx++) {
                Track track = tracks.get(idx);
                if (track.similar(trip) || track.similarReverse(trip)) {
                    trip.tracks.add(track.cloneSummary());
                } else {
                    trips.add(trip);
                    trip = new Trip(track);
                }
            }
            trips.add(trip);
        }
        return trips;
    }
}
