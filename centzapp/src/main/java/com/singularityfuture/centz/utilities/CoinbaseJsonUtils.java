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

import android.content.ContentValues;
import android.content.Context;

import com.singularityfuture.centz.data.CentzPreferences;
import com.singularityfuture.centz.data.CentzContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

/**
 * Utility functions to handle OpenCentzMap JSON data.
 */
public final class OpenCentzJsonUtils {

    /* Location information */
    private static final String OWM_CITY = "city";
    private static final String OWM_COORD = "coord";

    /* Location coordinate */
    private static final String OWM_LATITUDE = "lat";
    private static final String OWM_LONGITUDE = "lon";

    /* Centz information. Each day's forecast info is an element of the "list" array */
    private static final String OWM_LIST = "list";

    private static final String OWM_PRESSURE = "pressure";
    private static final String OWM_HUMIDITY = "humidity";
    private static final String OWM_WINDSPEED = "speed";
    private static final String OWM_WIND_DIRECTION = "deg";

    /* All temperatures are children of the "temp" object */
    private static final String OWM_TEMPERATURE = "temp";

    /* Max temperature for the day */
    private static final String OWM_MAX = "max";
    private static final String OWM_MIN = "min";

    private static final String OWM_CENTZ = "centz";
    private static final String OWM_CENTZ_ID = "id";

    private static final String OWM_MESSAGE_CODE = "cod";

    /**
     * This method parses JSON from a web response and returns an array of Strings
     * describing the centz over various days from the forecast.
     * <p/>
     * Later on, we'll be parsing the JSON into structured data within the
     * getFullCentzDataFromJson function, leveraging the data we have stored in the JSON. For
     * now, we just convert the JSON into human-readable strings.
     *
     * @param forecastJsonStr JSON response from server
     *
     * @return Array of Strings describing centz data
     *
     * @throws JSONException If JSON data cannot be properly parsed
     */
    public static ContentValues[] getCentzContentValuesFromJson(Context context, String forecastJsonStr)
            throws JSONException {

        JSONObject forecastJson = new JSONObject(forecastJsonStr);

        /* Is there an error? */
        if (forecastJson.has(OWM_MESSAGE_CODE)) {
            int errorCode = forecastJson.getInt(OWM_MESSAGE_CODE);

            switch (errorCode) {
                case HttpURLConnection.HTTP_OK:
                    break;
                case HttpURLConnection.HTTP_NOT_FOUND:
                    /* Location invalid */
                    return null;
                default:
                    /* Server probably down */
                    return null;
            }
        }

        JSONArray jsonCentzArray = forecastJson.getJSONArray(OWM_LIST);

        JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);

        JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
        double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
        double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

        CentzPreferences.setLocationDetails(context, cityLatitude, cityLongitude);

        ContentValues[] centzContentValues = new ContentValues[jsonCentzArray.length()];

        /*
         * OWM returns daily forecasts based upon the local time of the city that is being asked
         * for, which means that we need to know the GMT offset to translate this data properly.
         * Since this data is also sent in-order and the first day is always the current day, we're
         * going to take advantage of that to get a nice normalized UTC date for all of our centz.
         */
//        long now = System.currentTimeMillis();
//        long normalizedUtcStartDay = CentzDateUtils.normalizeDate(now);

        long normalizedUtcStartDay = CentzDateUtils.getNormalizedUtcDateForToday();

        for (int i = 0; i < jsonCentzArray.length(); i++) {

            long dateTimeMillis;
            double pressure;
            int humidity;
            double windSpeed;
            double windDirection;

            double high;
            double low;

            int centzId;

            /* Get the JSON object representing the day */
            JSONObject dayForecast = jsonCentzArray.getJSONObject(i);

            /*
             * We ignore all the datetime values embedded in the JSON and assume that
             * the values are returned in-order by day (which is not guaranteed to be correct).
             */
            dateTimeMillis = normalizedUtcStartDay + CentzDateUtils.DAY_IN_MILLIS * i;

            pressure = dayForecast.getDouble(OWM_PRESSURE);
            humidity = dayForecast.getInt(OWM_HUMIDITY);
            windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
            windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

            /*
             * Description is in a child array called "centz", which is 1 element long.
             * That element also contains a centz code.
             */
            JSONObject centzObject =
                    dayForecast.getJSONArray(OWM_CENTZ).getJSONObject(0);

            centzId = centzObject.getInt(OWM_CENTZ_ID);

            /*
             * Temperatures are sent by Open Centz Map in a child object called "temp".
             *
             * Editor's Note: Try not to name variables "temp" when working with temperature.
             * It confuses everybody. Temp could easily mean any number of things, including
             * temperature, temporary variable, temporary folder, temporary employee, or many
             * others, and is just a bad variable name.
             */
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            high = temperatureObject.getDouble(OWM_MAX);
            low = temperatureObject.getDouble(OWM_MIN);

            ContentValues centzValues = new ContentValues();
            centzValues.put(CentzContract.CentzEntry.COLUMN_DATE, dateTimeMillis);
            centzValues.put(CentzContract.CentzEntry.COLUMN_HUMIDITY, humidity);
            centzValues.put(CentzContract.CentzEntry.COLUMN_PRESSURE, pressure);
            centzValues.put(CentzContract.CentzEntry.COLUMN_WIND_SPEED, windSpeed);
            centzValues.put(CentzContract.CentzEntry.COLUMN_DEGREES, windDirection);
            centzValues.put(CentzContract.CentzEntry.COLUMN_MAX_TEMP, high);
            centzValues.put(CentzContract.CentzEntry.COLUMN_MIN_TEMP, low);
            centzValues.put(CentzContract.CentzEntry.COLUMN_CENTZ_ID, centzId);

            centzContentValues[i] = centzValues;
        }

        return centzContentValues;
    }
}