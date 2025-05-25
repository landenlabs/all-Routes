/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package landenlabs.wx_lib_data.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import landenlabs.wx_lib_data.logger.ALog;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Global Network routines and okHTTP instance.
 */
public class UtilNet {
    private static final String TAG =  UtilNet.class.getSimpleName();

    static Exception netException = null;
    private static OkHttpClient OK_HTTP_CLIENT;
    private static OkHttpClient.Builder OK_HTTP_BUILDER;
    private static final boolean ADD_INTERCEPTOR = true;

    public static final long CONNECT_SECONDS = 10;
    public static final long READ_SECONDS = 10;
    public static final long WRITE_SECONDS = 10;

    // ---------------------------------------------------------------------------------------------

    synchronized
    public static OkHttpClient.Builder getOkHTTPBuilder() {
        if (OK_HTTP_BUILDER == null) {
            OK_HTTP_BUILDER = new OkHttpClient.Builder()
                    .connectTimeout(CONNECT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(READ_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_SECONDS, TimeUnit.SECONDS)
                    .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
            ;

            if (ADD_INTERCEPTOR) {
                // Measure network bandwidth from TileServer

                // Also logs all URL messages - good for debugging.
                //xx OK_HTTP_BUILDER.addInterceptor(new NetOkInterceptor(null, null));
            }
        }
        return OK_HTTP_BUILDER;
    }

    synchronized
    public static OkHttpClient getOkHttpClient() {
        if (OK_HTTP_CLIENT == null) {
            OK_HTTP_CLIENT = getOkHTTPBuilder().build();
        }
        return OK_HTTP_CLIENT;
    }

    /*
    @Nullable
    public  static Response get(String urlStr, Map<String, String> headers) {
        OkHttpClient okHttpClient = UtilNet.getOkHttpClient();
        final int timeoutMilli = 5000;
        if (okHttpClient.connectTimeoutMillis() != timeoutMilli) {
            okHttpClient = UtilNet.getOkHTTPBuilder()
                    .connectTimeout(timeoutMilli, TimeUnit.MILLISECONDS)
                    .readTimeout(timeoutMilli, TimeUnit.MILLISECONDS)
                    .build();
        }

        Request.Builder requestBuilder = new Request.Builder()
                .url(urlStr);

        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBuilder.addHeader(header.getKey(), header.getValue());
            }
        }
        Request request = requestBuilder.build();

        Response response = null;
        netException = null;

        for (int retry = 0; retry < 1; retry++) {
            try {
                Thread.sleep(retry * 1000);
                response = okHttpClient.newCall(request).execute();
            } catch (Exception ex) {
                netException = ex;
            }
            if (response != null && response.code() == 200) {
                break;
            }
        }

        return response;
    }
     */

    /**
     * Encode special characters and return a valid  URL object
     */
    private static URL encodeIntoURL(String urlString)
            throws MalformedURLException {
        @SuppressWarnings("deprecation")
        URL url = new URL(URLDecoder
                .decode(urlString));    // Decode to handle case where already encoded
        try {
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(),
                    url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            return uri.toURL();
        } catch (URISyntaxException ex) {
            throw new MalformedURLException(ex.getMessage());
        }
    }

    public static String loadResourceAsString(String urlString, String defaultEncoding)
            throws IOException {
        return loadResourceAsString(encodeIntoURL(urlString), defaultEncoding);
    }

    /**
     * Use to load resource as a string from target URL and using specific encoding and pin HTTPS
     *
     * @param url       URL of target resource
     * @param defaultEncoding encoding of target resource
     * @return {@code String} content of target resource
     */
    public static String loadResourceAsString(URL url, String defaultEncoding)
            throws IOException {
        long startMilli = System.currentTimeMillis();
        int responseCode;
        String errMsg = "";
        final int MAX_RETRY = 3;

        ALog.i.tagMsg(TAG, "[BEGIN] LoadAsString [", url, "]");
        for (int retryCnt = 0; retryCnt < MAX_RETRY; retryCnt++) {
            try {
                Thread.sleep(retryCnt * 1000);
                OkHttpClient okHttpClient = getOkHttpClient();
                Request request = new Request.Builder()
                        .url(url)
                        .build();

                // Ensure the response (and underlying response body) is closed.
                try (Response response = okHttpClient.newCall(request).execute()) {
                    responseCode = response.code();
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        response.body().close();
                        ALog.d.tagMsg(TAG, "[GOOD] LoadAsString [", url
                                , "] dur=", (System.currentTimeMillis() - startMilli)
                                , " len=", body.length());
                        return body;
                    }
                }

                long durationMilli = System.currentTimeMillis() - startMilli;
                errMsg = String.format(Locale.US,
                        "milli=%d, code %d", durationMilli, responseCode);
                retryCnt = MAX_RETRY;   // TODO - are there some responseCodes worth retrying ?
            } catch (Exception ioe) {
                long durationMilli = System.currentTimeMillis() - startMilli;
                //xx UtilLogs.logError(url.toString(), ioe, durationMilli);

                errMsg = String.format(Locale.US, "milli=%d, Error=%s", durationMilli, ALog.getErrorMsg(ioe));
                if (!(ioe instanceof SocketTimeoutException)) {
                    retryCnt = MAX_RETRY;   // only retry on timeout
                }
            }
        }
        ALog.e.tagMsg(TAG, "[ERROR] LoadAsString ", errMsg);
        throw new IOException(errMsg);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null)
            return false;
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        return actNw != null
                && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
        );
    }


    /**
     * <uses-permission
     *     android:name="android.permission.ACCESS_WIFI_STATE"/>
     *  See to format:
     *     Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
     */
    public static String getIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            ALog.e.tagMsg(TAG, "get ip address ", ex);
        }
        return "";
    }
}
