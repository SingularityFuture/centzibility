/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.singularityfuture.centz.utilities;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.singularityfuture.centz.BuildConfig;
import com.singularityfuture.centz.data.CentzPreferences;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

/**
 * These utilities will be used to communicate with the centz servers.
 */
public final class NetworkUtils {

    private static final String TAG = NetworkUtils.class.getSimpleName();

    /*
     * Centz was originally built to use OpenCentzMap's API. However, we wanted to provide
     * a way to much more easily test the app and provide more varied centz data. After all, in
     * Mountain View (Google's HQ), it gets very boring looking at a forecast of perfectly clear
     * skies at 75°F every day... (UGH!) The solution we came up with was to host our own fake
     * centz server. With this server, there are two URL's you can use. The first (and default)
     * URL will return dynamic centz data. Each time the app refreshes, you will get different,
     * completely random centz data. This is incredibly useful for testing the robustness of your
     * application, as different centz JSON will provide edge cases for some of your methods.
     *
     * If you'd prefer to test with the centz data that you will see in the videos on Udacity,
     * you can do so by setting the FORECAST_BASE_URL to STATIC_CENTZ_URL below.
     */
    private static final String DYNAMIC_CENTZ_URL =
            "http://api.opencentzmap.org/data/2.5/forecast/daily";
            //"https://andfun-centz.udacity.com/centz";

    private static final String STATIC_CENTZ_URL =
            "http://api.opencentzmap.org/data/2.5/forecast/daily";
            //"https://andfun-centz.udacity.com/staticcentz";

    private static final String FORECAST_BASE_URL = DYNAMIC_CENTZ_URL;

    /*
     * NOTE: These values only effect responses from OpenCentzMap, NOT from the fake centz
     * server. They are simply here to allow us to teach you how to build a URL if you were to use
     * a real API.If you want to connect your app to OpenCentzMap's API, feel free to! However,
     * we are not going to show you how to do so in this course.
     */

    /* The format we want our API to return */
    private static final String format = "json";
    /* The units we want our API to return */
    private static final String units = "metric";
    /* The number of days we want our API to return */
    private static final int numDays = 14;

    /* The query parameter allows us to provide a location string to the API */
    private static final String QUERY_PARAM = "q";

    private static final String LAT_PARAM = "lat";
    private static final String LON_PARAM = "lon";

    /* The format parameter allows us to designate whether we want JSON or XML from our API */
    private static final String FORMAT_PARAM = "mode";
    /* The units parameter allows us to designate whether we want metric units or imperial units */
    private static final String UNITS_PARAM = "units";
    /* The days parameter allows us to designate how many days of centz data we want */
    private static final String DAYS_PARAM = "cnt";
    private static final String APPID ="appid";

    /**
     * Retrieves the proper URL to query for the centz data. The reason for both this method as
     * well as {@link #buildUrlWithLocationQuery(String)} is two fold.
     * <p>
     * 1) You should be able to just use one method when you need to create the URL within the
     * app instead of calling both methods.
     * 2) Later in Centz, you are going to add an alternate method of allowing the user
     * to select their preferred location. Once you do so, there will be another way to form
     * the URL using a latitude and longitude rather than just a location String. This method
     * will "decide" which URL to build and return it.
     *
     * @param context used to access other Utility methods
     * @return URL to query centz service
     */
    public static URL getUrl(Context context) {
        if (CentzPreferences.isLocationLatLonAvailable(context)) {
            double[] preferredCoordinates = CentzPreferences.getLocationCoordinates(context);
            double latitude = preferredCoordinates[0];
            double longitude = preferredCoordinates[1];
            return buildUrlWithLatitudeLongitude(latitude, longitude);
        } else {
            String locationQuery = CentzPreferences.getPreferredCentzLocation(context);
            return buildUrlWithLocationQuery(locationQuery);
        }
    }

    /**
     * Builds the URL used to talk to the centz server using latitude and longitude of a
     * location.
     *
     * @param latitude  The latitude of the location
     * @param longitude The longitude of the location
     * @return The Url to use to query the centz server.
     */
    private static URL buildUrlWithLatitudeLongitude(Double latitude, Double longitude) {
        Uri centzQueryUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                .appendQueryParameter(LAT_PARAM, String.valueOf(latitude))
                .appendQueryParameter(LON_PARAM, String.valueOf(longitude))
                .appendQueryParameter(FORMAT_PARAM, format)
                .appendQueryParameter(UNITS_PARAM, units)
                .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                .appendQueryParameter(APPID, BuildConfig.OPEN_CENTZ_MAP_API_KEY)
                .build();

        try {
            URL centzQueryUrl = new URL(centzQueryUri.toString());
            Log.v(TAG, "URL: " + centzQueryUrl);
            return centzQueryUrl;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Builds the URL used to talk to the centz server using a location. This location is based
     * on the query capabilities of the centz provider that we are using.
     *
     * @param locationQuery The location that will be queried for.
     * @return The URL to use to query the centz server.
     */
    private static URL buildUrlWithLocationQuery(String locationQuery) {
        Uri centzQueryUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                .appendQueryParameter(QUERY_PARAM, locationQuery)
                .appendQueryParameter(FORMAT_PARAM, format)
                .appendQueryParameter(UNITS_PARAM, units)
                .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                .appendQueryParameter(APPID, BuildConfig.OPEN_CENTZ_MAP_API_KEY)
                .build();

        try {
            URL centzQueryUrl = new URL(centzQueryUri.toString());
            Log.v(TAG, "URL: " + centzQueryUrl);
            return centzQueryUrl;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method returns the entire result from the HTTP response.
     *
     * @param url The URL to fetch the HTTP response from.
     * @return The contents of the HTTP response, null if no response
     * @throws IOException Related to network and stream reading
     */
    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            String response = null;
            if (hasInput) {
                response = scanner.next();
            }
            scanner.close();
            return response;
        } finally {
            urlConnection.disconnect();
        }
    }
}