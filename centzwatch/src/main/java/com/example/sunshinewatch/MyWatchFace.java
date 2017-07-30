/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.sunshinewatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 */
public class MyWatchFace extends CanvasWatchFaceService {

    /*
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**hh
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;
    private static final String TAG = "Watch Face Canvas";
    private static final int REQUEST_RESOLVE_ERROR = 1000;
    private static final String MAX_TEMP = "com.example.android.sunshine.key.max_temp";
    private static final String MIN_TEMP = "com.example.android.sunshine.key.min_temp";
    private static final String WEATHER_ID = "com.example.android.sunshine.key.weather_id";
    private static final String INSTALLED = "com.example.android.sunshine.key.installed";

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        //engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine implements
            DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener{

        private final Rect mPeekCardBounds = new Rect();
        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mMuteMode;
        private float mCenterX;
        private float mCenterY;

        Paint mTextPaint;
        Paint mTextPaintThin;
        Paint mTextPaintDate;
        float mTextXOffset;
        float mTextXOffsetDate;
        float mTextYOffset;

        private boolean mAmbient;

        private GoogleApiClient mGoogleApiClient;
        private boolean mResolvingError;
        private int max_temp;
        private int min_temp;
        private int weather_id;
        private int weather_icon= R.drawable.ic_clear; // Set default to clear.

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            mResolvingError = false;
            Wearable.DataApi.addListener(mGoogleApiClient, this);

            PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/sunshine_installed");
            putDataMapReq.getDataMap().putInt(INSTALLED, new Random().nextInt());
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            putDataReq.setUrgent();
            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            Log.d(TAG, "Sending Install Status was successful: " + dataItemResult.getStatus()
                                    .isSuccess());
                        }
                    });
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "onConnectionSuspended");
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            Log.d(TAG, "onDataChanged");
            for (DataEvent event : dataEvents) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    // DataItem changed
                    DataItem item = event.getDataItem();
                    if (item.getUri().getPath().compareTo("/weather_info") == 0) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        max_temp = dataMap.getInt(MAX_TEMP);
                        min_temp = dataMap.getInt(MIN_TEMP);
                        weather_id = dataMap.getInt(WEATHER_ID);
                        weather_icon = getSmallArtResourceIdForWeatherCondition(weather_id);
                        invalidate();
                    }
                } else if (event.getType() == DataEvent.TYPE_DELETED) {
                    // DataItem deleted
                }
            }
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.d(TAG, "onConnectionFailed");
            if (!mResolvingError) {
                if (connectionResult.hasResolution()) {
                } else {
                    Log.e(TAG, "Connection to Google API client has failed");
                    mResolvingError = false;
                    //Wearable.DataApi.removeListener(mGoogleApiClient, this);
                }
            }
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);        // Create the Paint for later use
            mTextPaint = new Paint();
            mTextPaint.setTextSize(40);
            mTextPaint.setColor(Color.WHITE);
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
            mTextPaintThin=mTextPaint;
            mTextPaintThin.setTypeface(Typeface.DEFAULT);
            // In order to make text in the center, we need adjust its position
            mTextXOffset = mTextPaint.measureText("00"+(char)0x00B0+" ");
            mTextYOffset = (mTextPaint.ascent() + mTextPaint.descent()) / 2;

            // Paint for the Date
            mTextPaintDate = new Paint();
            mTextPaintDate.setTextSize(30);
            mTextPaintDate.setColor(Color.WHITE);
            mTextPaintDate.setAntiAlias(true);
            mTextXOffsetDate = mTextPaintDate.measureText("Fri, Jan, 13") / 2;

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(true)
                    .setAcceptsTapEvents(true)
                    .build());

            mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
/*
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
*/
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;
            /* Check and trigger whether or not timer should be running (only in active mode). */
            //updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            if (mAmbient) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawColor(Color.BLUE);
                String max_temp_string= Integer.toString(max_temp);
                String min_temp_string= Integer.toString(min_temp);
                canvas.drawText(max_temp_string+(char) 0x00B0,
                        bounds.centerX() - mTextXOffset,
                        bounds.centerY() - mTextYOffset,
                        mTextPaint);
                canvas.drawText(" "+min_temp_string+(char) 0x00B0,
                        bounds.centerX(),
                        bounds.centerY() - mTextYOffset,
                        mTextPaintThin);
                SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM, d");
                Date d = new Date();
                String date = sdf.format(d);
                canvas.drawText(date,bounds.centerX() - mTextXOffsetDate,
                        bounds.centerY() - mTextYOffset*4,mTextPaintDate);
                Drawable drawable = MyWatchFace.this.getResources().getDrawable(weather_icon,getTheme());
                drawable.setBounds(bounds.centerX()-25, bounds.centerY()+55,bounds.centerX()+25 , bounds.centerY()+105);
                drawable.draw(canvas);

            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                mGoogleApiClient.connect();
                invalidate();
            } else {
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
            }
        }
    }

    public static int getSmallArtResourceIdForWeatherCondition(int weatherId) {

        /*
         * Based on weather code data for Open Weather Map.
         */
        if (weatherId >= 200 && weatherId <= 232) {
            return com.example.sunshinewatch.R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return com.example.sunshinewatch.R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            int temp=com.example.sunshinewatch.R.drawable.ic_rain;
            return com.example.sunshinewatch.R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return com.example.sunshinewatch.R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return com.example.sunshinewatch.R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return com.example.sunshinewatch.R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return com.example.sunshinewatch.R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 771 || weatherId == 781) {
            return com.example.sunshinewatch.R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return com.example.sunshinewatch.R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return com.example.sunshinewatch.R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return com.example.sunshinewatch.R.drawable.ic_cloudy;
        } else if (weatherId >= 900 && weatherId <= 906) {
            return com.example.sunshinewatch.R.drawable.ic_storm;
        } else if (weatherId >= 958 && weatherId <= 962) {
            return com.example.sunshinewatch.R.drawable.ic_storm;
        } else if (weatherId >= 951 && weatherId <= 957) {
            return com.example.sunshinewatch.R.drawable.ic_clear;
        }
        else {
            Log.e(TAG, "Unknown Weather: " + weatherId);
            return com.example.sunshinewatch.R.drawable.ic_storm;
        }
    }
}
