/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package com.landenlabs.routes.pages.PageUtils;

import android.content.Context;
import android.location.Geocoder;
import android.view.View;

import com.wsi.mapsdk.map.WSIMap;

import java.util.Locale;

/**
 * Geographic GIS utility class.
 */
@SuppressWarnings({"ConstantConditions", "unused", "SingleStatementInBlock", "UnaryPlus"})
public class GeoUtils {
    /*
     * Latitude/Longitude accuracy
     *
     https://gis.stackexchange.com/questions/8650/measuring-accuracy-of-latitude-and-longitude
     places  degrees      N/S or E/W     E/W at         E/W at       E/W at
     at                   equator        lat=23N/S      lat=45N/S    lat=67N/S
     ------- -------      ----------     ----------     ---------    ---------
     0       1            111.32 km      102.47 km      78.71 km     43.496 km
     1       0.1          11.132 km      10.247 km      7.871 km     4.3496 km
     2       0.01         1.1132 km      1.0247 km      787.1 m      434.96 m
     3       0.001        111.32 m       102.47 m       78.71 m      43.496 m
     4       0.0001       11.132 m       10.247 m       7.871 m      4.3496 m
     */
    private final View mapHolder;
    private final String mapApiKey; // getString(R.string.google_maps_key)
    private final Geocoder mGeocoder;

    private final WSIMap mMap;
    public GeoUtils(Context context, View mapHolder, WSIMap gMap, String mapApiKey) {
        this.mapHolder = mapHolder;
        this.mMap = gMap;
        this.mapApiKey = mapApiKey;
        mGeocoder = new Geocoder(context, Locale.getDefault());
    }

 /*
    void addDrivingPath(WLatLng latLng, TextView locationTv, TextView addressTv, TextView scaleTv) {
        Locale locale = Locale.getDefault();
        locationTv.setText(String.format(locale, "%.5f,%.5f", latLng.latitude, latLng.longitude));
        locationTv.setTextColor(0xff800000); // RED
        try {
            List<Address> addressList = mGeocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addressList != null && addressList.size() > 0) {
                StringBuilder sb = new StringBuilder();
                Address address = addressList.get(0);
                sb.append(String.format(locale, "GeoPos: %.5f,%.5f\n", address.getLatitude(), address.getLongitude()));
                sb.append(String.format("Line0: %s\n", address.getAddressLine(0)));
                sb.append(String.format("AdminArea: %s\n", address.getAdminArea()));
                //        sb.append(String.format("CountryCode: %s\n", address.getCountryCode()));
                sb.append(String.format("CountryName: %s\n", address.getCountryName()));
                sb.append(String.format("Locality: %s\n", address.getLocality()));
                sb.append(String.format("PostalCode: %s\n", address.getPostalCode()));
                addressTv.setText(sb.toString());
                addressTv.setTextColor(0xff008000);  // GREEN

                if (true) {

//                    address.getPostalCode();
//                    address.getLocality()
//                    address.getAdminArea()
//                    address.getLocality()
//                    address.getAdminArea()
//                    latLng.latitude, latLng.longitude
//
//                        https://developers.google.com/maps/documentation/directions/get-directions
//
//                        origin=<address>
//                        origin=<latitude>,<longitude>
//
//                        avoid=tools|highways|ferries|indoor
//                        language=en    // https://developers.google.com/maps/faq#languagesupport
//                        mode=driving|walking|bicycling|transit
//                        units=metric|imperial
//                        waypoints=<wayAddress1>[|<wayAddress2>]...


                    // String urlFmt = "https://maps.googleapis.com/maps/api/directions/json?origin=%s&destination=%s&waypoints=ADDRESS_X|ADDRESS_Y&key=%s";
                    String urlJsonFmt = "https://maps.googleapis.com/maps/api/directions/json?origin=%s&destination=%s&key=%s";
                    String urlXmlnFmt = "https://maps.googleapis.com/maps/api/directions/xmk?origin=%s&destination=%s&key=%s";
                    String addr1 = "400 Minuteman Rd, Andover MA 01810, USA";
                    String addr2 = "10 Hunters Run, Salem NH 03079, USA";
                    String url = String.format(urlJsonFmt, addr1, addr2, mapApiKey);


                    // getWSIAddress(binding.wsilocation, url, addr1, addr2);
                }

                mMap.clear();
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(latLng.latitude, latLng.longitude)));
                mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        .position(new LatLng(address.getLatitude(), address.getLongitude())));

                // .title(""));

                float minZoom = mMap.getMinZoomLevel();
                float maxZoom = mMap.getMaxZoomLevel();
                float zoom = mMap.getCameraPosition().zoom;

                DisplayMetrics metrics = mapHolder.getResources().getDisplayMetrics();
                // float mapWidthDp = binding.map.getWidth() / metrics.scaledDensity;
                float mapWidthPx = mapHolder.getWidth();
                double deg = PolyUtil.degreesAtLatZoom(latLng.latitude, zoom, mapWidthPx);

                if (true) {
                    // CameraUpdate cameraUpd = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
                    // mMap.moveCamera(cameraUpd);
                    scaleTv.setText(String.format(locale,
                            "Zoom %.2f\n Deg:%.2f\nZoom Min:%.2f Max:%.2f\nWidth:%dPx\n",
                            zoom, deg, minZoom, maxZoom, (int) mapWidthPx));
                }

                if (false) {
                    mMap.clear();
                    double degCircle = deg / 8;
                    double radiusMeters =
                            PolyUtil.metersAtLatZoom(latLng.latitude, zoom, mapWidthPx) / 8;
                    CircleOptions co = new CircleOptions().center(latLng).radius(radiusMeters);
                    co.strokeColor(Color.RED);
                    co.strokeWidth(2.0f);
                    co.fillColor(0x40808000);

                    mMap.addCircle(co);
                    mMap.addMarker(
                            new MarkerOptions().position(PolyUtil.move(latLng, -degCircle / 2, 0)));
                    mMap.addMarker(
                            new MarkerOptions().position(PolyUtil.move(latLng, +degCircle / 2, 0)));
                    mMap.addMarker(
                            new MarkerOptions().position(PolyUtil.move(latLng, 0, +degCircle / 2)));
                    mMap.addMarker(
                            new MarkerOptions().position(PolyUtil.move(latLng, 0, -degCircle / 2)));

                    PolygonOptions polygon =
                            new PolygonOptions().strokeColor(Color.BLUE).fillColor(0x40000080);
                    for (int rotate = 0; rotate <= 360; rotate += 10)
                        polygon.add(PolyUtil.sweep(latLng, degCircle / 2, rotate));
                    mMap.addPolygon(polygon);
                }

            } else {
                mMap.clear();
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(latLng.latitude, latLng.longitude))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                );

                addressTv.setText(R.string.noGeoCodeResult);

            }
        } catch (Exception ex) {
            addressTv.setText(ex.getLocalizedMessage());
        }
    }

    private void getWSIAddress(final TextView view, String fullUrl, String origin, String destination) {
        final OkHttpClient client = new OkHttpClient();
        // final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        // RequestBody formBody = RequestBody.create(JSON, pushJsonMsg);
        final Request getRequest = new Request.Builder()
                .url(fullUrl)
                .build();

        Thread thread = new Thread(() -> {
            try {
                Response response = client.newCall(getRequest).execute();
                String bodystring = response.body().string();

                // InputSource inputSource = new InputSource(new StringReader(xmlstring));
                // parser.parse(inputSource,handler);

                // Toast.makeText(mWsiApp, "GCM Pushed, code=" + response.code() + "\n" + pushJsonMsg.replaceAll(",", ",\n"), Toast.LENGTH_LONG).show();
                // HttpUtils.HTTP_UNAUTHORIZED = 401


                String msg = " code" + response.code() + "\n";
                setWSIAddress(view, msg);

                JSONObject responseJson = new JSONObject(bodystring);

//                   "routes" : [
//                      {
//                         "bounds" : {
//                            "northeast" : {
//                               "lat" : 42.7612427,
//                               "lng" : -71.1904835
//                            },
//                            "southwest" : {
//                               "lat" : 42.6890108,
//                               "lng" : -71.2147968
//                            }
//                         },

                JSONArray routes = responseJson.getJSONArray("routes");
                JSONObject route = routes.getJSONObject(0);
                JSONObject bounds = route.getJSONObject("bounds");
                JSONObject northest = bounds.getJSONObject("northeast");
                JSONObject southwest = bounds.getJSONObject("southwest");
                LatLng ne = new LatLng(northest.getDouble("lat"), northest.getDouble("lng"));
                LatLng sw = new LatLng(southwest.getDouble("lat"), southwest.getDouble("lng"));
                LatLngBounds box = new LatLngBounds.Builder()
                        .include(ne)
                        .include(sw)
                        .build();

                view.post(() -> mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(box, 20)));

                // https://medium.com/@trientran/android-working-with-google-maps-and-directions-api-44765433f19
                // https://medium.com/android-news/google-maps-directions-api-5b2e11dee9b0

                // GeoApiContext geoApiContext = new GeoApiContext();
                GeoApiContext geoApiContext = new GeoApiContext.Builder()
                        .queryRateLimit(3)
                        .apiKey(mapApiKey)
                        .connectTimeout(1, TimeUnit.SECONDS)
                        .readTimeout(1, TimeUnit.SECONDS)
                        .writeTimeout(1, TimeUnit.SECONDS)
                        .build();

                DirectionsResult result = DirectionsApi.newRequest(geoApiContext)
                        .mode(TravelMode.DRIVING)
                        .origin(origin)
                        .destination(destination).departureTime(Instant.now())
                        .await();
                List<LatLng> decodedPath = PolyUtil.decode(
                        result.routes[0].overviewPolyline.getEncodedPath());
                PolylineOptions polylineOptions = new PolylineOptions().addAll(decodedPath);
                polylineOptions.color(Color.RED);
                polylineOptions.width(10.0f);

                view.post(() -> mMap.addPolyline(polylineOptions));
            } catch (Exception ex) {
                // Toast.makeText(mWsiApp, "FAILED GCM Pushed " + ex.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                String msg = "GCM push failed " + ex.getLocalizedMessage();
                // FileLog.e(TAG, msg);
                setWSIAddress(view, msg);
            }
        });

        thread.start();
    }

    private void setWSIAddress(final TextView view, final String msg) {
        view.post(() -> view.setText(msg));
    }
    */
}
