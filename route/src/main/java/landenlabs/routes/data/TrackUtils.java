/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.data;

import static landenlabs.routes.utils.GpsUtils.metersBetween;
import static landenlabs.routes.utils.GpsUtils.sphericalTravel;
import static java.lang.Math.toRadians;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.wsi.mapsdk.utils.WLatLng;

import org.joda.time.DateTime;

import java.util.Comparator;
import java.util.Stack;

import landenlabs.wx_lib_data.logger.ALog;

/**
 * Track utility helper methods.
 */
public class TrackUtils {

    private static final String TAG = TrackUtils.class.getSimpleName();


    public static int RandomInt(int minVal, int maxVal) {
        return minVal + (int)(Math.random() * (maxVal-minVal));
    }
    public static double RandomDbl(double minVal, double maxVal) {
        return minVal + (Math.random() * (maxVal-minVal));
    }

    private static final String PREF_TEST_ID = "TestId";
    private static final int MIN_PTS = 20;
    private static final int MAX_ADD_PTS = 100;
    private static final double MIN_METERS_TRAVEL = 1000;
    private static final double MAX_METERS_TRAVEL = 10000;
    private static final double MIN_METERS_PER_SECOND = 10;
    private static final double MAX_METERS_PER_SECOND = 30;

    public static Track createRandomTrack(SharedPreferences pref, GpsPoint startPt, @Nullable Track shareTrack) {
        int testId = pref.getInt(PREF_TEST_ID, 100) + 1;
        pref.edit().putInt(PREF_TEST_ID, testId).apply();
        ArrayGps points = new ArrayGps();
        Track track = new Track(testId, Track.NAME_TEST + testId, points);

        final int numPts = MIN_PTS + RandomInt(0, MAX_ADD_PTS);
        DateTime startDt = DateTime.now().minusDays(RandomInt(0,6)).minusHours(RandomInt(0,12));
        startPt.milli = startDt.getMillis();
        GpsPoint pt1 = startPt;

        if (shareTrack != null && shareTrack.getPointCnt() > MIN_PTS && Math.random() > 0.5) {
            // Share start of other track.
            int sharePts = RandomInt(0, 10);
            track.name += "_" + Track.NAME_SAME + sharePts;
            long refMilli  = shareTrack.get(0).milli;
            for (int idx = 0; idx < sharePts; idx++) {
                pt1 = shareTrack.get(idx).clone();
                pt1.milli = pt1.milli - refMilli + startDt.getMillis();
                points.add(pt1);
            }
        } else {
            points.add(startPt);
        }

        double ccwFromNorth = RandomDbl(180, 355);  // head west
        while (points.size() < numPts) {
            double distMeters = RandomDbl(MIN_METERS_TRAVEL, MAX_METERS_TRAVEL);
            GpsPoint pt2 = sphericalTravel(pt1, distMeters, ccwFromNorth);
            final double metersPerSecond = RandomDbl(MIN_METERS_PER_SECOND, MAX_METERS_PER_SECOND);
            pt2.milli = pt1.milli + (long)(distMeters / metersPerSecond*1000);
            points.add(pt2);
            pt1 = pt2;
            ccwFromNorth += RandomDbl(-25, 25);
        }
        track.name += "_" + track.getDefName(startPt.milli);
        ALog.i.tagMsg(TAG, "New track ", track);
        return track;
    }


    public static Track createDirectionTrack(SharedPreferences pref, GpsPoint startPt, float angleDeg, double distMeters) {
        int testId = pref.getInt(PREF_TEST_ID, 100) + 1;
        pref.edit().putInt(PREF_TEST_ID, testId).apply();
        ArrayGps points = new ArrayGps();
        Track track = new Track(testId, Track.NAME_TEST + testId, points);

        DateTime startDt = DateTime.now().minusDays(RandomInt(0,6)).minusHours(RandomInt(0,12));
        startPt.milli = startDt.getMillis();
        GpsPoint pt1 = startPt;
        points.add(pt1);

        GpsPoint pt2 = sphericalTravel(pt1, distMeters, angleDeg);
        final double metersPerSecond = RandomDbl(MIN_METERS_PER_SECOND, MAX_METERS_PER_SECOND);
        pt2.milli = pt1.milli + (long)(distMeters / metersPerSecond*1000);
        points.add(pt2);

        track.name += "_" + track.getDefName(startPt.milli);
        ALog.i.tagMsg(TAG, "New track ", track);
        return track;
    }

    /**
     * Sort by similarity of start and end GpsPoint.
     */
    public static class SortByTrip implements Comparator<Track> {

        @Override
        public int compare(Track o1, Track o2) {
            boolean same1 = o1.similar(o2);
            boolean same2 = o1.similarReverse(o2);
            return (same1 || same2) ? 0 : Double.compare(o1.getMeters(), o2.getMeters());
        }
    }

    /**
     * Simplifies the given poly (polyline or polygon) using the Douglas-Peucker decimation
     * algorithm.  Increasing the tolerance will result in fewer points in the simplified polyline
     * or polygon.
     * <p>
     * When the providing a polygon as input, the first and last point of the list MUST have the
     * same latitude and longitude (i.e., the polygon must be closed).  If the input polygon is not
     * closed, the resulting polygon may not be fully simplified.
     * <p>
     * The time complexity of Douglas-Peucker is O(n^2), so take care that you do not call this
     * algorithm too frequently in your code.
     *
     * @param poly      polyline or polygon to be simplified.  Polygon should be closed (i.e.,
     *                  first and last points should have the same latitude and longitude).
     * @param tolerance in meters.  Increasing the tolerance will result in fewer points in the
     *                  simplified poly.
     * @return a simplified poly produced by the Douglas-Peucker algorithm
     */
    public static ArrayGps simplify(ArrayGps poly, double tolerance) {
        final int n = poly.size();
        if (n < 1) {
            throw new IllegalArgumentException("Polyline must have at least 1 point");
        }
        if (tolerance <= 0) {
            throw new IllegalArgumentException("Tolerance must be greater than zero");
        }

        int idx;
        int maxIdx = 0;
        Stack<int[]> stack = new Stack<>();
        double[] dists = new double[n];
        dists[0] = 1;
        dists[n - 1] = 1;
        double maxDist;
        double dist = 0.0;
        int[] current;

        if (n > 2) {
            int[] stackVal = new int[]{0, (n - 1)};
            stack.push(stackVal);
            while (stack.size() > 0) {
                current = stack.pop();
                maxDist = 0;
                for (idx = current[0] + 1; idx < current[1]; ++idx) {
                    dist = distanceToLine(poly.get(idx), poly.get(current[0]),
                            poly.get(current[1]));
                    if (dist > maxDist) {
                        maxDist = dist;
                        maxIdx = idx;
                    }
                }
                if (maxDist > tolerance) {
                    dists[maxIdx] = maxDist;
                    int[] stackValCurMax = {current[0], maxIdx};
                    stack.push(stackValCurMax);
                    int[] stackValMaxCur = {maxIdx, current[1]};
                    stack.push(stackValMaxCur);
                }
            }
        }

        // Generate the simplified line
        idx = 0;
        ArrayGps simplifiedLine = new ArrayGps();
        for (GpsPoint l : poly) {
            if (dists[idx] != 0) {
                simplifiedLine.add(l);
            }
            idx++;
        }

        return simplifiedLine;
    }

    /**
     * Computes the distance on the sphere between the point p and the line segment start to end.
     *
     * @param p     the point to be measured
     * @param start the beginning of the line segment
     * @param end   the end of the line segment
     * @return the distance in meters (assuming spherical earth)
     */
    public static double distanceToLine(final GpsPoint p, final GpsPoint start, final GpsPoint end) {
        if (start.equals(end)) {
            return computeDistanceBetween(end, p);
        }

        // Implementation of http://paulbourke.net/geometry/pointlineplane/ or http://geomalgorithms.com/a02-_lines.html
        final double s0lat = toRadians(p.latitude);
        final double s0lng = toRadians(p.longitude);
        final double s1lat = toRadians(start.latitude);
        final double s1lng = toRadians(start.longitude);
        final double s2lat = toRadians(end.latitude);
        final double s2lng = toRadians(end.longitude);

        double lonCorrection = Math.cos(s1lat);
        double s2s1lat = s2lat - s1lat;
        double s2s1lng = (s2lng - s1lng) * lonCorrection;
        final double u = ((s0lat - s1lat) * s2s1lat + (s0lng - s1lng) * lonCorrection * s2s1lng)
                / (s2s1lat * s2s1lat + s2s1lng * s2s1lng);
        if (u <= 0) {
            return computeDistanceBetween(p, start);
        }
        if (u >= 1) {
            return computeDistanceBetween(p, end);
        }
        WLatLng su = new WLatLng(start.latitude + u * (end.latitude - start.latitude), start.longitude + u * (end.longitude - start.longitude));
        return computeDistanceBetween(p, su);
    }

    // Return distance in meters
    public static double computeDistanceBetween(GpsPoint p1, GpsPoint p2) {
        return metersBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude);
    }
    public static double computeDistanceBetween(GpsPoint p1, WLatLng p2) {
        return metersBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude);
    }
}
