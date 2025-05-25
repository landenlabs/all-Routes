/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package com.landenlabs.routes.data;

import android.util.SparseArray;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;

import com.landenlabs.routes.data.TrackGrid.GridPos;

import org.joda.time.DateTime;

import java.util.HashSet;
import java.util.Locale;

/**
 * Compute and store common tracks
 */
public class TrackCommon {

    // Collect grid cell positions
    public final HashSet<GridPos> gridPosSet = new HashSet<>();

    // Count how often a track id appears in gridPosSet
    public final SparseIntArray idCount = new SparseIntArray();

    @NonNull
    public String toInfo() {
        StringBuilder sb = new StringBuilder(String.format(Locale.US, "#Routes=%d\n", idCount.size()));
        for (int idx = 0; idx < Math.min(10, idCount.size()); idx++) {
            sb.append(String.format(Locale.US, "Id:%d  #:%d\n", idCount.keyAt(idx), idCount.valueAt(idx)));
        }
        if (trackScore != null) {
            sb.append(trackScore.toInfo());
        }
        return sb.toString();
    }

    public static class TrackScore {
        public int dowId = -1;
        public int dow = -1;
        public int dowCnt = 0;

        public int hourId = -1;
        public int hour = -1;
        public int hourCnt = 0;

        public int dowHourId = -1;
        public int dowHourCnt = 0;

        @NonNull
        public String toInfo() {
            return String.format(Locale.US, "DOW=%d, #%d Hour=%d, #%d, DowHour #%d", dow, dowCnt, hour, hourCnt, dowHourCnt);
        }
    }
    public TrackScore trackScore = null;


    public void add(GpsPoint gpsPoint, TrackGrid trackGrid) {
        add(new GridPos(gpsPoint, TrackGrid.scale1, TrackGrid.scale2), trackGrid);
    }
    public void add(double lat, double lng, TrackGrid trackGrid) {
        add(new GridPos(lat, lng, TrackGrid.scale1, TrackGrid.scale2), trackGrid);
    }
    public void add(GridPos gridPos, TrackGrid trackGrid) {
        SparseArray<TrackIdList> latArray = trackGrid.grid.get(gridPos.latDegI, null);
        if (latArray != null) {
            TrackIdList lngIdList = latArray.get(gridPos.lngDegI, null);
            if (lngIdList != null) {
                gridPosSet.add(gridPos);
            }
        }
    }
    public void countIds(GridPos gridPos, TrackGrid trackGrid) {
        SparseArray<TrackIdList> latArray = trackGrid.grid.get(gridPos.latDegI, null);
        if (latArray != null) {
            TrackIdList lngIdList = latArray.get(gridPos.lngDegI, null);
            if (lngIdList != null) {
                for (Integer id : lngIdList.ids) {
                    int cnt = idCount.get(id, 0);
                    idCount.put(id, cnt+1);
                    // keepBest(1000);
                }
            }
        }
    }

    public void keepBest(int pruneCnt) {
        int lmt = 1;
        int minCnt = Integer.MAX_VALUE;
        while (idCount.size() > pruneCnt) {
            for (int idx =0; idx < idCount.size(); idx++) {
                if (idCount.valueAt(idx) <= lmt) {
                    idCount.removeAt(idx);
                    if (idCount.size() <= pruneCnt)
                        break;
                } else {
                    minCnt = Math.min(minCnt, idCount.valueAt(idx));
                }
            }
            lmt = minCnt;
        }
    }

    @NonNull
    public TrackScore getScores(TrackGrid trackGrid) {

        idCount.clear();
        for (GridPos gridPos : gridPosSet) {
            countIds(gridPos, trackGrid);
        }

        trackScore = new TrackScore();
        DateTime dt = DateTime.now();
        trackScore.dow = dt.getDayOfWeek();
        trackScore.hour = dt.getHourOfDay();

        for (int idx =0; idx < idCount.size(); idx++) {
            int id = idCount.keyAt(idx);
            int cnt = idCount.valueAt(idx);
            Track track = trackGrid.getTrack(id);
            if (track != null) {
                dt = new DateTime(track.getMilliStart());
                if (dt.getDayOfWeek() == trackScore.dow) {
                    trackScore.dowId = id;
                    trackScore.dowCnt += cnt;
                }
                //   23-1   (23+24)%36=11,   (1+24)%36=25
                if (Math.abs(dt.getHourOfDay() - trackScore.hour) < 2) {
                    trackScore.hourId = id;
                    trackScore.hourCnt += cnt;
                }
                if (trackScore.dowId == trackScore.hourId) {
                    trackScore.dowHourId = trackScore.dowId;
                    trackScore.dowHourCnt += cnt;
                }
            }
        }

        return trackScore;
    }

    private static int hourChange(int hour1, int hour2) {
        return 12 - Math.abs((Math.abs(hour1 - hour2) % 24) - 12);
    }
}
