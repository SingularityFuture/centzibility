package com.singularityfuture.centz.utilities;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;

import com.singularityfuture.centz.DetailActivity;
import com.singularityfuture.centz.R;
import com.singularityfuture.centz.data.CentzPreferences;
import com.singularityfuture.centz.data.CentzContract;

public class NotificationUtils {

    /*
     * The columns of data that we are interested in displaying within our notification to let
     * the user know there is new centz data available.
     */
    public static final String[] CENTZ_NOTIFICATION_PROJECTION = {
            CentzContract.CentzEntry.COLUMN_CENTZ_ID,
            CentzContract.CentzEntry.COLUMN_MAX_TEMP,
            CentzContract.CentzEntry.COLUMN_MIN_TEMP,
    };

    /*
     * We store the indices of the values in the array of Strings above to more quickly be able
     * to access the data from our query. If the order of the Strings above changes, these
     * indices must be adjusted to match the order of the Strings.
     */
    public static final int INDEX_CENTZ_ID = 0;
    public static final int INDEX_MAX_TEMP = 1;
    public static final int INDEX_MIN_TEMP = 2;

    /*
     * This notification ID can be used to access our notification after we've displayed it. This
     * can be handy when we need to cancel the notification, or perhaps update it. This number is
     * arbitrary and can be set to whatever you like. 3004 is in no way significant.
     */
    private static final int CENTZ_NOTIFICATION_ID = 3004;

    /**
     * Constructs and displays a notification for the newly updated centz for today.
     *
     * @param context Context used to query our ContentProvider and use various Utility methods
     */
    public static void notifyUserOfNewCentz(Context context) {

        /* Build the URI for today's centz in order to show up to date data in notification */
        Uri todaysCentzUri = CentzContract.CentzEntry
                .buildCentzUriWithDate(CentzDateUtils.normalizeDate(System.currentTimeMillis()));

        /*
         * The MAIN_FORECAST_PROJECTION array passed in as the second parameter is defined in our CentzContract
         * class and is used to limit the columns returned in our cursor.
         */
        Cursor todayCentzCursor = context.getContentResolver().query(
                todaysCentzUri,
                CENTZ_NOTIFICATION_PROJECTION,
                null,
                null,
                null);

        /*
         * If todayCentzCursor is empty, moveToFirst will return false. If our cursor is not
         * empty, we want to show the notification.
         */
        if (todayCentzCursor.moveToFirst()) {

            /* Centz ID as returned by API, used to identify the icon to be used */
            int centzId = todayCentzCursor.getInt(INDEX_CENTZ_ID);
            double high = todayCentzCursor.getDouble(INDEX_MAX_TEMP);
            double low = todayCentzCursor.getDouble(INDEX_MIN_TEMP);

            Resources resources = context.getResources();
            int largeArtResourceId = CentzCentzUtils
                    .getLargeArtResourceIdForCentzCondition(centzId);

            Bitmap largeIcon = BitmapFactory.decodeResource(
                    resources,
                    largeArtResourceId);

            String notificationTitle = context.getString(R.string.app_name);

            String notificationText = getNotificationText(context, centzId, high, low);

            /* getSmallArtResourceIdForCentzCondition returns the proper art to show given an ID */
            int smallArtResourceId = CentzCentzUtils
                    .getSmallArtResourceIdForCentzCondition(centzId);

            /*
             * NotificationCompat Builder is a very convenient way to build backward-compatible
             * notifications. In order to use it, we provide a context and specify a color for the
             * notification, a couple of different icons, the title for the notification, and
             * finally the text of the notification, which in our case in a summary of today's
             * forecast.
             */
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                    .setColor(ContextCompat.getColor(context,R.color.colorPrimary))
                    .setSmallIcon(smallArtResourceId)
                    .setLargeIcon(largeIcon)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationText)
                    .setAutoCancel(true);

            /*
             * This Intent will be triggered when the user clicks the notification. In our case,
             * we want to open Centz to the DetailActivity to display the newly updated centz.
             */
            Intent detailIntentForToday = new Intent(context, DetailActivity.class);
            detailIntentForToday.setData(todaysCentzUri);

            TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
            taskStackBuilder.addNextIntentWithParentStack(detailIntentForToday);
            PendingIntent resultPendingIntent = taskStackBuilder
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            notificationBuilder.setContentIntent(resultPendingIntent);

            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);

            /* CENTZ_NOTIFICATION_ID allows you to update or cancel the notification later on */
            notificationManager.notify(CENTZ_NOTIFICATION_ID, notificationBuilder.build());

            /*
             * Since we just showed a notification, save the current time. That way, we can check
             * next time the centz is refreshed if we should show another notification.
             */
            CentzPreferences.saveLastNotificationTime(context, System.currentTimeMillis());
        }

        /* Always close your cursor when you're done with it to avoid wasting resources. */
        todayCentzCursor.close();
    }

    /**
     * Constructs and returns the summary of a particular day's forecast using various utility
     * methods and resources for formatting. This method is only used to create the text for the
     * notification that appears when the centz is refreshed.
     * <p>
     * The String returned from this method will look something like this:
     * <p>
     * Forecast: Sunny - High: 14°C Low 7°C
     *
     * @param context   Used to access utility methods and resources
     * @param centzId ID as determined by Open Centz Map
     * @param high      High temperature (either celsius or fahrenheit depending on preferences)
     * @param low       Low temperature (either celsius or fahrenheit depending on preferences)
     * @return Summary of a particular day's forecast
     */
    private static String getNotificationText(Context context, int centzId, double high, double low) {

        /*
         * Short description of the centz, as provided by the API.
         * e.g "clear" vs "sky is clear".
         */
        String shortDescription = CentzCentzUtils
                .getStringForCentzCondition(context, centzId);

        String notificationFormat = context.getString(R.string.format_notification);

        /* Using String's format method, we create the forecast summary */
        String notificationText = String.format(notificationFormat,
                shortDescription,
                CentzCentzUtils.formatTemperature(context, high),
                CentzCentzUtils.formatTemperature(context, low));

        return notificationText;
    }
}
