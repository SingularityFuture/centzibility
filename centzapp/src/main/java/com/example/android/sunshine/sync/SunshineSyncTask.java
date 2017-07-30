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
package com.example.android.sunshine.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;

import com.example.android.sunshine.data.SunshinePreferences;
import com.example.android.sunshine.data.WeatherContract;
import com.example.android.sunshine.utilities.NetworkUtils;
import com.example.android.sunshine.utilities.NotificationUtils;
import com.example.android.sunshine.utilities.OpenWeatherJsonUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.net.URL;

public class SunshineSyncTask implements DataApi.DataListener{

    /*implements
    DataApi.DataListener,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener*/

    /**
     * Performs the network request for updated weather, parses the JSON from that request, and
     * inserts the new weather information into our ContentProvider. Will notify the user that new
     * weather has been loaded if the user hasn't been notified of the weather within the last day
     * AND they haven't disabled notifications in the preferences screen.
     *
     * @param context Used to access utility methods and the ContentResolver
     */

    private static final String MAX_TEMP = "com.example.android.sunshine.key.max_temp";
    private static final String MIN_TEMP = "com.example.android.sunshine.key.min_temp";
    private static final String CURRENT_TIME= "com.example.android.sunshine.key.time";
    private static final String TAG = "Sync Task";
    private static final String WEATHER_ID = "com.example.android.sunshine.key.weather_id";
    //private static final int REQUEST_RESOLVE_ERROR = 1000;
    private static boolean mResolvingError = false;
    private static GoogleApiClient mGoogleApiClient;
    private static Context mContext;

    synchronized static public void syncWeather(Context context) {
        mContext=context;

        try {
            /*
             * The getUrl method will return the URL that we need to get the forecast JSON for the
             * weather. It will decide whether to create a URL based off of the latitude and
             * longitude or off of a simple location as a String.
             */
            URL weatherRequestUrl = NetworkUtils.getUrl(context);

            /* Use the URL to retrieve the JSON */
            String jsonWeatherResponse = NetworkUtils.getResponseFromHttpUrl(weatherRequestUrl);

            /* Parse the JSON into a list of weather values */
            ContentValues[] weatherValues = OpenWeatherJsonUtils
                    .getWeatherContentValuesFromJson(context, jsonWeatherResponse);

            double max_temp = weatherValues[0].getAsInteger(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP);
            double min_temp = weatherValues[0].getAsInteger(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP);
            int weather_id = weatherValues[0].getAsInteger(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID);
            double max_temp_correct_units;
            double min_temp_correct_units;
            if (!SunshinePreferences.isMetric(context)) {
                max_temp_correct_units= (max_temp * 1.8) + 32;
                min_temp_correct_units= (min_temp * 1.8) + 32;
            }
            else {
                max_temp_correct_units=max_temp;
                min_temp_correct_units=min_temp;
            }

            /*
             * In cases where our JSON contained an error code, getWeatherContentValuesFromJson
             * would have returned null. We need to check for those cases here to prevent any
             * NullPointerExceptions being thrown. We also have no reason to insert fresh data if
             * there isn't any to insert.
             */
            if (weatherValues != null && weatherValues.length != 0) {
                /* Get a handle on the ContentResolver to delete and insert data */
                ContentResolver sunshineContentResolver = context.getContentResolver();

                /* Delete old weather data because we don't need to keep multiple days' data */
                sunshineContentResolver.delete(
                        WeatherContract.WeatherEntry.CONTENT_URI,
                        null,
                        null);

                /* Insert our new weather data into Sunshine's ContentProvider */
                sunshineContentResolver.bulkInsert(
                        WeatherContract.WeatherEntry.CONTENT_URI,
                        weatherValues);

                /*
                 * Finally, after we insert data into the ContentProvider, determine whether or not
                 * we should notify the user that the weather has been refreshed.
                 */
                boolean notificationsEnabled = SunshinePreferences.areNotificationsEnabled(context);

                /*
                 * If the last notification was shown was more than 1 day ago, we want to send
                 * another notification to the user that the weather has been updated. Remember,
                 * it's important that you shouldn't spam your users with notifications.
                 */
                long timeSinceLastNotification = SunshinePreferences
                        .getEllapsedTimeSinceLastNotification(context);

                boolean oneDayPassedSinceLastNotification = false;

                if (timeSinceLastNotification >= DateUtils.DAY_IN_MILLIS) {
                    oneDayPassedSinceLastNotification = true;
                }

                /*
                 * We only want to show the notification if the user wants them shown and we
                 * haven't shown a notification in the past day.
                 */
                if (notificationsEnabled && oneDayPassedSinceLastNotification) {
                    NotificationUtils.notifyUserOfNewWeather(context);
                }

                final SunshineSyncTask sync_instance = new SunshineSyncTask();
                /* If the code reaches this point, we have successfully performed our sync */
                mGoogleApiClient = new GoogleApiClient.Builder(context)
                        .addApi(Wearable.API)
                        .addConnectionCallbacks(new ConnectionCallbacks() {
                            @Override
                            public void onConnected (Bundle connectionHint){
                                Log.d(TAG, "onConnected: " + connectionHint);
                                mResolvingError = false;
                                // Now you can use the Data Layer API
                                Wearable.DataApi.addListener(mGoogleApiClient,sync_instance);
                                //sync_instance.addListener(mGoogleApiClient);
                            }
                            @Override
                            public void onConnectionSuspended ( int cause){
                                Log.d(TAG, "onConnectionSuspended: " + cause);
                            }
                        })
                        .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener(){
                            @Override
                            public void onConnectionFailed(ConnectionResult connectionResult){
                                Log.d(TAG, "onConnectionFailed");
                                if (!mResolvingError) {
                                    if (connectionResult.hasResolution()) {
            /*                try {
                                mResolvingError = true;
                                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
                            } catch (IntentSender.SendIntentException e) {
                                // There was an error with the resolution intent. Try again.
                                mGoogleApiClient.connect();
                            }*/
                                    } else {
                                        Log.e(TAG, "Connection to Google API client has failed");
                                        mResolvingError = false;
                                        //SunshineSyncTask sync_instance = new SunshineSyncTask();
                                        Wearable.DataApi.removeListener(mGoogleApiClient,sync_instance);
                                        //sync_instance.removeListener(mGoogleApiClient);
                                    }
                                }

                            }
                        })
                        .build();
                if (!mResolvingError) {
                    mGoogleApiClient.connect();
                }

                PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/weather_info");
                putDataMapReq.getDataMap().putInt(MAX_TEMP, (int) max_temp_correct_units);
                putDataMapReq.getDataMap().putInt(MIN_TEMP, (int) min_temp_correct_units);
                putDataMapReq.getDataMap().putInt(WEATHER_ID, weather_id);
                putDataMapReq.getDataMap().putLong(CURRENT_TIME, System.currentTimeMillis());
                PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                putDataReq.setUrgent();
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq)
                        .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                            @Override
                            public void onResult(DataApi.DataItemResult dataItemResult) {
                                Log.d(TAG, "Sending data was successful: " + dataItemResult.getStatus()
                                        .isSuccess());
                            }
                        });

            }

        } catch (Exception e) {
            /* Server probably invalid */
            e.printStackTrace();
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                Log.d(TAG, "DataItem changed: " + event.getDataItem().getUri());
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/sunshine_installed") == 0) {
                    syncWeather(mContext);
                }
            }
        }
    }
}