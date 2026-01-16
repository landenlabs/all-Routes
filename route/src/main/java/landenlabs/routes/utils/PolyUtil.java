/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.utils;

import static landenlabs.routes.utils.GpsUtils.metersBetween;
import static java.lang.Math.toRadians;

import com.wsi.mapsdk.utils.WLatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Collection of GIS utility methods.
 * <p>
 * From Google  map GIS library
 * https://github.com/googlemaps/android-maps-utils
 * https://developers.google.com/maps/documentation/javascript/reference/geometry
 * <p>
 * https://github.com/googlemaps/android-maps-utils/blob/main/library/src/main/java/com/google/maps/android/SphericalUtil.java
 * <p>
 * Apache 2.0 license
 * https://github.com/googlemaps/android-maps-utils/blob/main/LICENSE
 */
public class PolyUtil {

    public static final int TILE_WIDTH_PIXELS_ZOOM0 = 256;
    public static final double EARTH_MEAN_RADIUS_METERS = 6378140;
    public static final double EQUATOR_PERIMETER_METERS = EARTH_MEAN_RADIUS_METERS * 2 * Math.PI;
    public static final double METERS_PER_PIXEL = EQUATOR_PERIMETER_METERS / TILE_WIDTH_PIXELS_ZOOM0;
    public static final int EQUATOR_DEGREES = 360;
    // https://github.com/googlemaps/android-maps-utils/blob/main/library/src/main/java/com/google/maps/android/SphericalUtil.java
    public static final float EARTH_RADIUS_METERS = 6378137f; // meters WGS84 Major axis

    /**
     * Decodes an encoded path string into a sequence of LatLngs.
     */
    public static List<WLatLng> decode(final String encodedPath) {
        int len = encodedPath.length();

        // For speed we preallocate to an upper bound on the final length, then
        // truncate the array before returning.
        final List<WLatLng> path = new ArrayList<WLatLng>();
        int index = 0;
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int result = 1;
            int shift = 0;
            int b;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 1;
            shift = 0;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            path.add(new WLatLng(lat * 1e-5, lng * 1e-5));
        }

        return path;
    }

    /**
     * Encodes a sequence of LatLngs into an encoded path string.
     */
    public static String encode(final List<WLatLng> path) {
        long lastLat = 0;
        long lastLng = 0;

        final StringBuffer result = new StringBuffer();

        for (final WLatLng point : path) {
            long lat = Math.round(point.latitude * 1e5);
            long lng = Math.round(point.longitude * 1e5);

            long dLat = lat - lastLat;
            long dLng = lng - lastLng;

            encode(dLat, result);
            encode(dLng, result);

            lastLat = lat;
            lastLng = lng;
        }
        return result.toString();
    }

    private static void encode(long v, StringBuffer result) {
        v = v < 0 ? ~(v << 1) : v << 1;
        while (v >= 0x20) {
            result.append(Character.toChars((int) ((0x20 | (v & 0x1f)) + 63)));
            v >>= 5;
        }
        result.append(Character.toChars((int) (v + 63)));
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
    public static List<WLatLng> simplify(List<WLatLng> poly, double tolerance) {
        final int n = poly.size();
        if (n < 1) {
            throw new IllegalArgumentException("Polyline must have at least 1 point");
        }
        if (tolerance <= 0) {
            throw new IllegalArgumentException("Tolerance must be greater than zero");
        }

        boolean closedPolygon = isClosedPolygon(poly);
        WLatLng lastPoint = null;

        // Check if the provided poly is a closed polygon
        if (closedPolygon) {
            // Add a small offset to the last point for Douglas-Peucker on polygons (see #201)
            final double OFFSET = 0.00000000001;
            lastPoint = poly.get(poly.size() - 1);
            // LatLng.latitude and .longitude are immutable, so replace the last point
            poly.remove(poly.size() - 1);
            poly.add(new WLatLng(lastPoint.latitude + OFFSET, lastPoint.longitude + OFFSET));
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

        if (closedPolygon) {
            // Replace last point w/ offset with the original last point to re-close the polygon
            poly.remove(poly.size() - 1);
            poly.add(lastPoint);
        }

        // Generate the simplified line
        idx = 0;
        ArrayList<WLatLng> simplifiedLine = new ArrayList<>();
        for (WLatLng l : poly) {
            if (dists[idx] != 0) {
                simplifiedLine.add(l);
            }
            idx++;
        }

        return simplifiedLine;
    }

    /**
     * Returns true if the provided list of points is a closed polygon (i.e., the first and last
     * points are the same), and false if it is not
     *
     * @param poly polyline or polygon
     * @return true if the provided list of points is a closed polygon (i.e., the first and last
     * points are the same), and false if it is not
     */
    public static boolean isClosedPolygon(List<WLatLng> poly) {
        WLatLng firstPoint = poly.get(0);
        WLatLng lastPoint = poly.get(poly.size() - 1);
        return firstPoint.equals(lastPoint);
    }

    /**
     * Computes the distance on the sphere between the point p and the line segment start to end.
     *
     * @param p     the point to be measured
     * @param start the beginning of the line segment
     * @param end   the end of the line segment
     * @return the distance in meters (assuming spherical earth)
     */
    public static double distanceToLine(final WLatLng p, final WLatLng start, final WLatLng end) {
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


    // ---------------------------------------------------------------------------------------------

    public static WLatLng move(final WLatLng inLatLng, double lat, double lng) {
        return new WLatLng(inLatLng.latitude + lat, inLatLng.longitude + lng);
    }

    public static WLatLng sweep(final WLatLng inLatLng, double radiusDeg, double rotateDeg) {
        double rotateRad = toRadians(rotateDeg);
        double lat = radiusDeg * Math.cos(rotateRad);
        double lng = radiusDeg * Math.sin(rotateRad);
        return new WLatLng(inLatLng.latitude + lat, inLatLng.longitude + lng);
    }

    public static double metersAtLat(double latitudeDeg, double deg) {
        final double latScale = Math.cos(toRadians(latitudeDeg));
        return EQUATOR_PERIMETER_METERS / EQUATOR_DEGREES * deg * latScale;
    }

    public static double metersAtLatZoom(double latitudeDeg, double zoom, double widthPixels) {
        final double latScale = Math.cos(toRadians(latitudeDeg));
        double metersPerPixel = METERS_PER_PIXEL * latScale / Math.pow(2, zoom);
        return widthPixels * metersPerPixel;
    }

    public static double degreesAtLatZoom(double latitudeDeg, double zoom, double widthPixels) {
        final double latScale = Math.cos(toRadians(latitudeDeg));
        return latScale * EQUATOR_DEGREES * metersAtLatZoom(0.0f, zoom, widthPixels) / EQUATOR_PERIMETER_METERS;
    }

    public static double getZoomForDegrees(double mapWidthDeg, double latitudeDeg, int mapWidthPx) {
        return getZoomForMetersWide(metersAtLat(latitudeDeg, mapWidthDeg), mapWidthPx, latitudeDeg);
    }

    // http://luan-itworld.blogspot.hk/2013/07/calculate-google-map-zoom-level-on.html
    public static double getZoomForDegrees(double diameterDegree, int width, int height) {
        final int GLOBE_WIDTH = TILE_WIDTH_PIXELS_ZOOM0; // a constant in Google's map projection
        final int ZOOM_MAX = 21;
        final double LN2 = .693147180559945309417;

        double latFraction = diameterDegree / 180;
        double lngFraction = diameterDegree / 360;
        double latZoom = Math.log((double) height / GLOBE_WIDTH / latFraction) / LN2;
        double lngZoom = Math.log((double) width / GLOBE_WIDTH / lngFraction) / LN2;
        return Math.min(Math.min(latZoom, lngZoom), ZOOM_MAX);
    }

    public static double getZoomForMetersWide(
            final double mapWidthMeters,
            final double mapWidthPixels,
            final double latitudeDeg) {
        final double latScale = Math.cos(toRadians(latitudeDeg));
        // final double zoomRate = EQUATOR_PERIMETER_METTERS * mapWidthPixels * latScale / ( mapWidthMeters * 256.0 );

        final double TILE_WIDTH_METERS_ZOOM0 = EQUATOR_PERIMETER_METERS;
        final double metersPerPixelz0 = TILE_WIDTH_METERS_ZOOM0 / TILE_WIDTH_PIXELS_ZOOM0;
        final double zoomRate = latScale * metersPerPixelz0 * (mapWidthPixels / mapWidthMeters);

        // Convert to power of 2
        return Math.log(zoomRate) / Math.log(2.0);      // is zoom 0 based or 1 based ?
    }


    // Return distance in meters
    public static double computeDistanceBetween(WLatLng p1, WLatLng p2) {
        return metersBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude);
    }

}