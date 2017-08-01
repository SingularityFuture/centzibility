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
import android.util.Log;

import com.singularityfuture.centz.R;
import com.singularityfuture.centz.data.CentzPreferences;

/**
 * Contains useful utilities for a centz app, such as conversion between Celsius and Fahrenheit,
 * from kph to mph, and from degrees to NSEW.  It also contains the mapping of centz condition
 * codes in OpenCentzMap to strings.  These strings are contained
 */
public final class CentzUtils {

    private static final String LOG_TAG = CentzUtils.class.getSimpleName();

    /**
     * This method will convert a temperature from Celsius to Fahrenheit.
     *
     * @param temperatureInCelsius Temperature in degrees Celsius(°C)
     *
     * @return Temperature in degrees Fahrenheit (°F)
     */
    private static double celsiusToFahrenheit(double temperatureInCelsius) {
        double temperatureInFahrenheit = (temperatureInCelsius * 1.8) + 32;
        return temperatureInFahrenheit;
    }

    /**
     * Temperature data is stored in Celsius by our app. Depending on the user's preference,
     * the app may need to display the temperature in Fahrenheit. This method will perform that
     * temperature conversion if necessary. It will also format the temperature so that no
     * decimal points show. Temperatures will be formatted to the following form: "21°"
     *
     * @param context     Android Context to access preferences and resources
     * @param temperature Temperature in degrees Celsius (°C)
     *
     * @return Formatted temperature String in the following form:
     * "21°"
     */
    public static String formatTemperature(Context context, double temperature) {
        if (!CentzPreferences.isMetric(context)) {
            temperature = celsiusToFahrenheit(temperature);
        }

        int temperatureFormatResourceId = R.string.format_temperature;

        /* For presentation, assume the user doesn't care about tenths of a degree. */
        return String.format(context.getString(temperatureFormatResourceId), temperature);
    }

    /**
     * This method will format the temperatures to be displayed in the
     * following form: "HIGH° / LOW°"
     *
     * @param context Android Context to access preferences and resources
     * @param high    High temperature for a day in user's preferred units
     * @param low     Low temperature for a day in user's preferred units
     *
     * @return String in the form: "HIGH° / LOW°"
     */
    public static String formatHighLows(Context context, double high, double low) {
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String formattedHigh = formatTemperature(context, roundedHigh);
        String formattedLow = formatTemperature(context, roundedLow);

        String highLowStr = formattedHigh + " / " + formattedLow;
        return highLowStr;
    }

    /**
     * This method uses the wind direction in degrees to determine compass direction as a
     * String. (eg NW) The method will return the wind String in the following form: "2 km/h SW"
     *
     * @param context   Android Context to access preferences and resources
     * @param windSpeed Wind speed in kilometers / hour
     * @param degrees   Degrees as measured on a compass, NOT temperature degrees!
     *                  See https://www.mathsisfun.com/geometry/degrees.html
     *
     * @return Wind String in the following form: "2 km/h SW"
     */
    public static String getFormattedWind(Context context, float windSpeed, float degrees) {
        int windFormat = R.string.format_wind_kmh;

        if (!CentzPreferences.isMetric(context)) {
            windFormat = R.string.format_wind_mph;
            windSpeed = .621371192237334f * windSpeed;
        }

        /*
         * You know what's fun? Writing really long if/else statements with tons of possible
         * conditions. Seriously, try it!
         */
        String direction = "Unknown";
        if (degrees >= 337.5 || degrees < 22.5) {
            direction = "N";
        } else if (degrees >= 22.5 && degrees < 67.5) {
            direction = "NE";
        } else if (degrees >= 67.5 && degrees < 112.5) {
            direction = "E";
        } else if (degrees >= 112.5 && degrees < 157.5) {
            direction = "SE";
        } else if (degrees >= 157.5 && degrees < 202.5) {
            direction = "S";
        } else if (degrees >= 202.5 && degrees < 247.5) {
            direction = "SW";
        } else if (degrees >= 247.5 && degrees < 292.5) {
            direction = "W";
        } else if (degrees >= 292.5 && degrees < 337.5) {
            direction = "NW";
        }

        return String.format(context.getString(windFormat), windSpeed, direction);
    }

    /**
     * Helper method to provide the string according to the centz
     * condition id returned by the OpenCentzMap call.
     *
     * @param context   Android context
     * @param centzId from OpenCentzMap API response
     *                  See http://opencentzmap.org/centz-conditions for a list of all IDs
     *
     * @return String for the centz condition, null if no relation is found.
     */
    public static String getStringForCentzCondition(Context context, int centzId) {
        int stringId;
        if (centzId >= 200 && centzId <= 232) {
            stringId = R.string.condition_2xx;
        } else if (centzId >= 300 && centzId <= 321) {
            stringId = R.string.condition_3xx;
        } else switch (centzId) {
            case 500:
                stringId = R.string.condition_500;
                break;
            case 501:
                stringId = R.string.condition_501;
                break;
            case 502:
                stringId = R.string.condition_502;
                break;
            case 503:
                stringId = R.string.condition_503;
                break;
            case 504:
                stringId = R.string.condition_504;
                break;
            case 511:
                stringId = R.string.condition_511;
                break;
            case 520:
                stringId = R.string.condition_520;
                break;
            case 531:
                stringId = R.string.condition_531;
                break;
            case 600:
                stringId = R.string.condition_600;
                break;
            case 601:
                stringId = R.string.condition_601;
                break;
            case 602:
                stringId = R.string.condition_602;
                break;
            case 611:
                stringId = R.string.condition_611;
                break;
            case 612:
                stringId = R.string.condition_612;
                break;
            case 615:
                stringId = R.string.condition_615;
                break;
            case 616:
                stringId = R.string.condition_616;
                break;
            case 620:
                stringId = R.string.condition_620;
                break;
            case 621:
                stringId = R.string.condition_621;
                break;
            case 622:
                stringId = R.string.condition_622;
                break;
            case 701:
                stringId = R.string.condition_701;
                break;
            case 711:
                stringId = R.string.condition_711;
                break;
            case 721:
                stringId = R.string.condition_721;
                break;
            case 731:
                stringId = R.string.condition_731;
                break;
            case 741:
                stringId = R.string.condition_741;
                break;
            case 751:
                stringId = R.string.condition_751;
                break;
            case 761:
                stringId = R.string.condition_761;
                break;
            case 762:
                stringId = R.string.condition_762;
                break;
            case 771:
                stringId = R.string.condition_771;
                break;
            case 781:
                stringId = R.string.condition_781;
                break;
            case 800:
                stringId = R.string.condition_800;
                break;
            case 801:
                stringId = R.string.condition_801;
                break;
            case 802:
                stringId = R.string.condition_802;
                break;
            case 803:
                stringId = R.string.condition_803;
                break;
            case 804:
                stringId = R.string.condition_804;
                break;
            case 900:
                stringId = R.string.condition_900;
                break;
            case 901:
                stringId = R.string.condition_901;
                break;
            case 902:
                stringId = R.string.condition_902;
                break;
            case 903:
                stringId = R.string.condition_903;
                break;
            case 904:
                stringId = R.string.condition_904;
                break;
            case 905:
                stringId = R.string.condition_905;
                break;
            case 906:
                stringId = R.string.condition_906;
                break;
            case 951:
                stringId = R.string.condition_951;
                break;
            case 952:
                stringId = R.string.condition_952;
                break;
            case 953:
                stringId = R.string.condition_953;
                break;
            case 954:
                stringId = R.string.condition_954;
                break;
            case 955:
                stringId = R.string.condition_955;
                break;
            case 956:
                stringId = R.string.condition_956;
                break;
            case 957:
                stringId = R.string.condition_957;
                break;
            case 958:
                stringId = R.string.condition_958;
                break;
            case 959:
                stringId = R.string.condition_959;
                break;
            case 960:
                stringId = R.string.condition_960;
                break;
            case 961:
                stringId = R.string.condition_961;
                break;
            case 962:
                stringId = R.string.condition_962;
                break;
            default:
                return context.getString(R.string.condition_unknown, centzId);
        }

        return context.getString(stringId);
    }

    /**
     * Helper method to provide the icon resource id according to the centz condition id returned
     * by the OpenCentzMap call. This method is very similar to
     *
     *   {@link #getLargeArtResourceIdForCentzCondition(int)}.
     *
     * The difference between these two methods is that this method provides smaller assets, used
     * in the list item layout for a "future day", as well as
     *
     * @param centzId from OpenCentzMap API response
     *                  See http://opencentzmap.org/centz-conditions for a list of all IDs
     *
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    public static int getSmallArtResourceIdForCentzCondition(int centzId) {

        /*
         * Based on centz code data for Open Centz Map.
         */
        if (centzId >= 200 && centzId <= 232) {
            return R.drawable.ic_storm;
        } else if (centzId >= 300 && centzId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (centzId >= 500 && centzId <= 504) {
            return R.drawable.ic_rain;
        } else if (centzId == 511) {
            return R.drawable.ic_snow;
        } else if (centzId >= 520 && centzId <= 531) {
            return R.drawable.ic_rain;
        } else if (centzId >= 600 && centzId <= 622) {
            return R.drawable.ic_snow;
        } else if (centzId >= 701 && centzId <= 761) {
            return R.drawable.ic_fog;
        } else if (centzId == 761 || centzId == 771 || centzId == 781) {
            return R.drawable.ic_storm;
        } else if (centzId == 800) {
            return R.drawable.ic_clear;
        } else if (centzId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (centzId >= 802 && centzId <= 804) {
            return R.drawable.ic_cloudy;
        } else if (centzId >= 900 && centzId <= 906) {
            return R.drawable.ic_storm;
        } else if (centzId >= 958 && centzId <= 962) {
            return R.drawable.ic_storm;
        } else if (centzId >= 951 && centzId <= 957) {
            return R.drawable.ic_clear;
        }

        Log.e(LOG_TAG, "Unknown Centz: " + centzId);
        return R.drawable.ic_storm;
    }

    /**
     * Helper method to provide the art resource ID according to the centz condition ID returned
     * by the OpenCentzMap call. This method is very similar to
     *
     *   {@link #getSmallArtResourceIdForCentzCondition(int)}.
     *
     * The difference between these two methods is that this method provides larger assets, used
     * in the "today view" of the list, as well as in the DetailActivity.
     *
     * @param centzId from OpenCentzMap API response
     *                  See http://opencentzmap.org/centz-conditions for a list of all IDs
     *
     * @return resource ID for the corresponding icon. -1 if no relation is found.
     */
    public static int getLargeArtResourceIdForCentzCondition(int centzId) {

        /*
         * Based on centz code data for Open Centz Map.
         */
        if (centzId >= 200 && centzId <= 232) {
            return R.drawable.art_storm;
        } else if (centzId >= 300 && centzId <= 321) {
            return R.drawable.art_light_rain;
        } else if (centzId >= 500 && centzId <= 504) {
            return R.drawable.art_rain;
        } else if (centzId == 511) {
            return R.drawable.art_snow;
        } else if (centzId >= 520 && centzId <= 531) {
            return R.drawable.art_rain;
        } else if (centzId >= 600 && centzId <= 622) {
            return R.drawable.art_snow;
        } else if (centzId >= 701 && centzId <= 761) {
            return R.drawable.art_fog;
        } else if (centzId == 761 || centzId == 771 || centzId == 781) {
            return R.drawable.art_storm;
        } else if (centzId == 800) {
            return R.drawable.art_clear;
        } else if (centzId == 801) {
            return R.drawable.art_light_clouds;
        } else if (centzId >= 802 && centzId <= 804) {
            return R.drawable.art_clouds;
        } else if (centzId >= 900 && centzId <= 906) {
            return R.drawable.art_storm;
        } else if (centzId >= 958 && centzId <= 962) {
            return R.drawable.art_storm;
        } else if (centzId >= 951 && centzId <= 957) {
            return R.drawable.art_clear;
        }

        Log.e(LOG_TAG, "Unknown Centz: " + centzId);
        return R.drawable.art_storm;
    }
}