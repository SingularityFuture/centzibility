package com.singularityfuture.centz.utilities;

import android.content.ContentValues;
import android.content.Context;

import com.singularityfuture.centz.data.CentzContract;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.singularityfuture.centz.data.CentzContract.CentzEntry;

public class FakeDataUtils {

    private static int [] centzIDs = {200,300,500,711,900,962};

    /**
     * Creates a single ContentValues object with random centz data for the provided date
     * @param date a normalized date
     * @return ContentValues object filled with random centz data
     */
    private static ContentValues createTestCentzContentValues(long date) {
        ContentValues testCentzValues = new ContentValues();
        testCentzValues.put(CentzEntry.COLUMN_DATE, date);
        testCentzValues.put(CentzEntry.COLUMN_DEGREES, Math.random()*2);
        testCentzValues.put(CentzEntry.COLUMN_HUMIDITY, Math.random()*100);
        testCentzValues.put(CentzEntry.COLUMN_PRESSURE, 870 + Math.random()*100);
        int maxTemp = (int)(Math.random()*100);
        testCentzValues.put(CentzEntry.COLUMN_MAX_TEMP, maxTemp);
        testCentzValues.put(CentzEntry.COLUMN_MIN_TEMP, maxTemp - (int) (Math.random()*10));
        testCentzValues.put(CentzEntry.COLUMN_WIND_SPEED, Math.random()*10);
        testCentzValues.put(CentzEntry.COLUMN_CENTZ_ID, centzIDs[(int)(Math.random()*10)%5]);
        return testCentzValues;
    }

    /**
     * Creates random centz data for 7 days starting today
     * @param context
     */
    public static void insertFakeData(Context context) {
        //Get today's normalized date
        long today = CentzDateUtils.normalizeDate(System.currentTimeMillis());
        List<ContentValues> fakeValues = new ArrayList<ContentValues>();
        //loop over 7 days starting today onwards
        for(int i=0; i<7; i++) {
            fakeValues.add(FakeDataUtils.createTestCentzContentValues(today + TimeUnit.DAYS.toMillis(i)));
        }
        // Bulk Insert our new centz data into Centz's Database
        context.getContentResolver().bulkInsert(
                CentzContract.CentzEntry.CONTENT_URI,
                fakeValues.toArray(new ContentValues[7]));
    }
}
