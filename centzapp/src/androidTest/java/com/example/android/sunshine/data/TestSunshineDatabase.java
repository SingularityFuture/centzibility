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
package com.example.android.centz.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static com.example.android.centz.data.TestUtilities.getConstantNameByStringValue;
import static com.example.android.centz.data.TestUtilities.getStaticIntegerField;
import static com.example.android.centz.data.TestUtilities.getStaticStringField;
import static com.example.android.centz.data.TestUtilities.studentReadableClassNotFound;
import static com.example.android.centz.data.TestUtilities.studentReadableNoSuchField;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Used to test the database we use in Centz to cache centz data. Within these tests, we
 * test the following:
 * <p>
 * <p>
 * 1) Creation of the database with proper table(s)
 * 2) Insertion of single record into our centz table
 * 3) When a record is already stored in the centz table with a particular date, a new record
 * with the same date will overwrite that record.
 * 4) Verify that NON NULL constraints are working properly on record inserts
 * 5) Verify auto increment is working with the ID
 * 6) Test the onUpgrade functionality of the CentzDbHelper
 */
@RunWith(AndroidJUnit4.class)
public class TestCentzDatabase {

    /*
     * Context used to perform operations on the database and create CentzDbHelpers.
     */
    private final Context context = InstrumentationRegistry.getTargetContext();

    /*
     * In order to verify that you have set up your classes properly and followed our TODOs, we
     * need to create what's called a Change Detector Test. In almost any other situation, these
     * tests are discouraged, as they provide no real value in a production setting. However, using
     * reflection to verify that you have set your classes up correctly will help provide more
     * useful errors if you've missed a step in our instructions.
     *
     * Additionally, using reflection for these tests allows you to run the tests when they
     * normally wouldn't compile, as they depend on pieces of your classes that you might not
     * have created when you initially run the tests.
     */
    private static final String packageName = "com.example.android.centz";
    private static final String dataPackageName = packageName + ".data";

    private Class centzEntryClass;
    private Class centzDbHelperClass;
    private static final String centzContractName = ".CentzContract";
    private static final String centzEntryName = centzContractName + "$CentzEntry";
    private static final String centzDbHelperName = ".CentzDbHelper";

    private static final String databaseNameVariableName = "DATABASE_NAME";
    private static String REFLECTED_DATABASE_NAME;

    private static final String databaseVersionVariableName = "DATABASE_VERSION";
    private static int REFLECTED_DATABASE_VERSION;

    private static final String tableNameVariableName = "TABLE_NAME";
    private static String REFLECTED_TABLE_NAME;

    private static final String columnDateVariableName = "COLUMN_DATE";
    static String REFLECTED_COLUMN_DATE;

    private static final String columnCentzIdVariableName = "COLUMN_CENTZ_ID";
    static String REFLECTED_COLUMN_CENTZ_ID;

    private static final String columnMinVariableName = "COLUMN_MIN_TEMP";
    static String REFLECTED_COLUMN_MIN;

    private static final String columnMaxVariableName = "COLUMN_MAX_TEMP";
    static String REFLECTED_COLUMN_MAX;

    private static final String columnHumidityVariableName = "COLUMN_HUMIDITY";
    static String REFLECTED_COLUMN_HUMIDITY;

    private static final String columnPressureVariableName = "COLUMN_PRESSURE";
    static String REFLECTED_COLUMN_PRESSURE;

    private static final String columnWindSpeedVariableName = "COLUMN_WIND_SPEED";
    static String REFLECTED_COLUMN_WIND_SPEED;

    private static final String columnWindDirVariableName = "COLUMN_DEGREES";
    static String REFLECTED_COLUMN_WIND_DIR;

    private SQLiteDatabase database;
    private SQLiteOpenHelper dbHelper;

    @Before
    public void before() {
        try {

            centzEntryClass = Class.forName(dataPackageName + centzEntryName);
            if (!BaseColumns.class.isAssignableFrom(centzEntryClass)) {
                String centzEntryDoesNotImplementBaseColumns = "CentzEntry class needs to " +
                        "implement the interface BaseColumns, but does not.";
                fail(centzEntryDoesNotImplementBaseColumns);
            }

            REFLECTED_TABLE_NAME = getStaticStringField(centzEntryClass, tableNameVariableName);
            REFLECTED_COLUMN_DATE = getStaticStringField(centzEntryClass, columnDateVariableName);
            REFLECTED_COLUMN_CENTZ_ID = getStaticStringField(centzEntryClass, columnCentzIdVariableName);
            REFLECTED_COLUMN_MIN = getStaticStringField(centzEntryClass, columnMinVariableName);
            REFLECTED_COLUMN_MAX = getStaticStringField(centzEntryClass, columnMaxVariableName);
            REFLECTED_COLUMN_HUMIDITY = getStaticStringField(centzEntryClass, columnHumidityVariableName);
            REFLECTED_COLUMN_PRESSURE = getStaticStringField(centzEntryClass, columnPressureVariableName);
            REFLECTED_COLUMN_WIND_SPEED = getStaticStringField(centzEntryClass, columnWindSpeedVariableName);
            REFLECTED_COLUMN_WIND_DIR = getStaticStringField(centzEntryClass, columnWindDirVariableName);

            centzDbHelperClass = Class.forName(dataPackageName + centzDbHelperName);

            Class centzDbHelperSuperclass = centzDbHelperClass.getSuperclass();

            if (centzDbHelperSuperclass == null || centzDbHelperSuperclass.equals(Object.class)) {
                String noExplicitSuperclass =
                        "CentzDbHelper needs to extend SQLiteOpenHelper, but yours currently doesn't extend a class at all.";
                fail(noExplicitSuperclass);
            } else if (centzDbHelperSuperclass != null) {
                String centzDbHelperSuperclassName = centzDbHelperSuperclass.getSimpleName();
                String doesNotExtendOpenHelper =
                        "CentzDbHelper needs to extend SQLiteOpenHelper but yours extends "
                                + centzDbHelperSuperclassName;

                assertTrue(doesNotExtendOpenHelper,
                        SQLiteOpenHelper.class.isAssignableFrom(centzDbHelperSuperclass));
            }

            REFLECTED_DATABASE_NAME = getStaticStringField(
                    centzDbHelperClass, databaseNameVariableName);

            REFLECTED_DATABASE_VERSION = getStaticIntegerField(
                    centzDbHelperClass, databaseVersionVariableName);

            Constructor centzDbHelperCtor = centzDbHelperClass.getConstructor(Context.class);

            dbHelper = (SQLiteOpenHelper) centzDbHelperCtor.newInstance(context);

            context.deleteDatabase(REFLECTED_DATABASE_NAME);

            Method getWritableDatabase = SQLiteOpenHelper.class.getDeclaredMethod("getWritableDatabase");
            database = (SQLiteDatabase) getWritableDatabase.invoke(dbHelper);

        } catch (ClassNotFoundException e) {
            fail(studentReadableClassNotFound(e));
        } catch (NoSuchFieldException e) {
            fail(studentReadableNoSuchField(e));
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
        } catch (NoSuchMethodException e) {
            fail(e.getMessage());
        } catch (InstantiationException e) {
            fail(e.getMessage());
        } catch (InvocationTargetException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testDatabaseVersionWasIncremented() {
        int expectedDatabaseVersion = 3;
        String databaseVersionShouldBe1 = "Database version should be "
                + expectedDatabaseVersion + " but isn't."
                + "\n Database version: ";

        assertEquals(databaseVersionShouldBe1,
                expectedDatabaseVersion,
                REFLECTED_DATABASE_VERSION);
    }

    /**
     * Tests to ensure that inserts into your database results in automatically incrementing row
     * IDs and that row IDs are not reused.
     * <p>
     * If the INTEGER PRIMARY KEY column is not explicitly given a value, then it will be filled
     * automatically with an unused integer, usually one more than the largest _ID currently in
     * use. This is true regardless of whether or not the AUTOINCREMENT keyword is used.
     * <p>
     * If the AUTOINCREMENT keyword appears after INTEGER PRIMARY KEY, that changes the automatic
     * _ID assignment algorithm to prevent the reuse of _IDs over the lifetime of the database.
     * In other words, the purpose of AUTOINCREMENT is to prevent the reuse of _IDs from previously
     * deleted rows.
     * <p>
     * To test this, we first insert a row into the database and get its _ID. Then, we'll delete
     * that row, change the data that we're going to insert, and insert the changed data into the
     * database again. If AUTOINCREMENT isn't set up properly in the CentzDbHelper's table
     * create statement, then the _ID of the first insert will be reused. However, if AUTOINCREMENT
     * is setup properly, that older ID will NOT be reused, and the test will pass.
     */
    @Test
    public void testDuplicateDateInsertBehaviorShouldReplace() {

        /* Obtain centz values from TestUtilities */
        ContentValues testCentzValues = TestUtilities.createTestCentzContentValues();

        /*
         * Get the original centz ID of the testCentzValues to ensure we use a different
         * centz ID for our next insert.
         */
        long originalCentzId = testCentzValues.getAsLong(REFLECTED_COLUMN_CENTZ_ID);

        /* Insert the ContentValues with old centz ID into database */
        database.insert(
                CentzContract.CentzEntry.TABLE_NAME,
                null,
                testCentzValues);

        /*
         * We don't really care what this ID is, just that it is different than the original and
         * that we can use it to verify our "new" centz entry has been made.
         */
        long newCentzId = originalCentzId + 1;

        testCentzValues.put(REFLECTED_COLUMN_CENTZ_ID, newCentzId);

        /* Insert the ContentValues with new centz ID into database */
        database.insert(
                CentzContract.CentzEntry.TABLE_NAME,
                null,
                testCentzValues);

        /* Query for a centz record with our new centz ID */
        Cursor newCentzIdCursor = database.query(
                REFLECTED_TABLE_NAME,
                new String[]{REFLECTED_COLUMN_DATE},
                null,
                null,
                null,
                null,
                null);

        String recordWithNewIdNotFound =
                "New record did not overwrite the previous record for the same date.";
        assertTrue(recordWithNewIdNotFound,
                newCentzIdCursor.getCount() == 1);

        /* Always close the cursor after you're done with it */
        newCentzIdCursor.close();
    }

    /**
>>>>>>> a6840f1... S07.03-Exercise-ConflictResolutionPolicy
     * Tests the columns with null values cannot be inserted into the database.
     */
    @Test
    public void testNullColumnConstraints() {
        /* Use a CentzDbHelper to get access to a writable database */

        /* We need a cursor from a centz table query to access the column names */
        Cursor centzTableCursor = database.query(
                REFLECTED_TABLE_NAME,
                /* We don't care about specifications, we just want the column names */
                null, null, null, null, null, null);

        /* Store the column names and close the cursor */
        String[] centzTableColumnNames = centzTableCursor.getColumnNames();
        centzTableCursor.close();

        /* Obtain centz values from TestUtilities and make a copy to avoid altering singleton */
        ContentValues testValues = TestUtilities.createTestCentzContentValues();
        /* Create a copy of the testValues to save as a reference point to restore values */
        ContentValues testValuesReferenceCopy = new ContentValues(testValues);

        for (String columnName : centzTableColumnNames) {

            /* We don't need to verify the _ID column value is not null, the system does */
            if (columnName.equals(CentzContract.CentzEntry._ID)) continue;

            /* Set the value to null */
            testValues.putNull(columnName);

            /* Insert ContentValues into database and get a row ID back */
            long shouldFailRowId = database.insert(
                    REFLECTED_TABLE_NAME,
                    null,
                    testValues);

            String variableName = getConstantNameByStringValue(
                    CentzContract.CentzEntry.class,
                    columnName);

            /* If the insert fails, which it should in this case, database.insert returns -1 */
            String nullRowInsertShouldFail =
                    "Insert should have failed due to a null value for column: '" + columnName + "'"
                            + ", but didn't."
                            + "\n Check that you've added NOT NULL to " + variableName
                            + " in your create table statement in the CentzEntry class."
                            + "\n Row ID: ";
            assertEquals(nullRowInsertShouldFail,
                    -1,
                    shouldFailRowId);

            /* "Restore" the original value in testValues */
            testValues.put(columnName, testValuesReferenceCopy.getAsDouble(columnName));
        }

        /* Close database */
        dbHelper.close();
    }

    /**
     * Tests to ensure that inserts into your database results in automatically
     * incrementing row IDs.
>>>>>>> 4174cf2... S07.02-Exercise-PreventInvalidInserts
     */
    @Test
    public void testIntegerAutoincrement() {

        /* First, let's ensure we have some values in our table initially */
        testInsertSingleRecordIntoCentzTable();

        /* Obtain centz values from TestUtilities */
        ContentValues testCentzValues = TestUtilities.createTestCentzContentValues();

        /* Get the date of the testCentzValues to ensure we use a different date later */
        long originalDate = testCentzValues.getAsLong(REFLECTED_COLUMN_DATE);

        /* Insert ContentValues into database and get a row ID back */
        long firstRowId = database.insert(
                REFLECTED_TABLE_NAME,
                null,
                testCentzValues);

        /* Delete the row we just inserted to see if the database will reuse the rowID */
        database.delete(
                REFLECTED_TABLE_NAME,
                "_ID == " + firstRowId,
                null);

        /*
         * Now we need to change the date associated with our test content values because the
         * database policy is to replace identical dates on conflict.
         */
        long dayAfterOriginalDate = originalDate + TimeUnit.DAYS.toMillis(1);
        testCentzValues.put(REFLECTED_COLUMN_DATE, dayAfterOriginalDate);

        /* Insert ContentValues into database and get another row ID back */
        long secondRowId = database.insert(
                REFLECTED_TABLE_NAME,
                null,
                testCentzValues);

        String sequentialInsertsDoNotAutoIncrementId =
                "IDs were reused and shouldn't be if autoincrement is setup properly.";
        assertNotSame(sequentialInsertsDoNotAutoIncrementId,
                firstRowId, secondRowId);
    }

    /**
     * This method tests the {@link CentzDbHelper#onUpgrade(SQLiteDatabase, int, int)}. The proper
     * behavior for this method in our case is to simply DROP (or delete) the centz table from
     * the database and then have the table recreated.
     */
    @Test
    public void testOnUpgradeBehavesCorrectly() {

        testInsertSingleRecordIntoCentzTable();

        dbHelper.onUpgrade(database, 13, 14);

        /*
         * This Cursor will contain the names of each table in our database and we will use it to
         * make sure that our centz table is still in the database after upgrading.
         */
        Cursor tableNameCursor = database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='" + REFLECTED_TABLE_NAME + "'",
                null);

        /*
         * Our database should only contain one table, and so the above query should have one
         * record in the cursor that queried for our table names.
         */
        int expectedTableCount = 1;
        String shouldHaveSingleTable = "There should only be one table returned from this query.";
        assertEquals(shouldHaveSingleTable,
                expectedTableCount,
                tableNameCursor.getCount());

        /* We are done verifying our table names, so we can close this cursor */
        tableNameCursor.close();

        Cursor shouldBeEmptyCentzCursor = database.query(
                REFLECTED_TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null);

        int expectedRecordCountAfterUpgrade = 0;
        /* We will finally verify that our centz table is empty after */
        String centzTableShouldBeEmpty =
                "Centz table should be empty after upgrade, but wasn't."
                        + "\nNumber of records: ";
        assertEquals(centzTableShouldBeEmpty,
                expectedRecordCountAfterUpgrade,
                shouldBeEmptyCentzCursor.getCount());

        /* Test is over, close the cursor */
        database.close();
    }

    /**
     * This method tests that our database contains all of the tables that we think it should
     * contain. Although in our case, we just have one table that we expect should be added
     * <p>
     * {@link com.example.android.centz.data.CentzContract.CentzEntry#TABLE_NAME}.
     * <p>
     * Despite only needing to check one table name in Centz, we set this method up so that
     * you can use it in other apps to test databases with more than one table.
     */
    @Test
    public void testCreateDb() {
        /*
         * Will contain the name of every table in our database. Even though in our case, we only
         * have only table, in many cases, there are multiple tables. Because of that, we are
         * showing you how to test that a database with multiple tables was created properly.
         */
        final HashSet<String> tableNameHashSet = new HashSet<>();

        /* Here, we add the name of our only table in this particular database */
        tableNameHashSet.add(REFLECTED_TABLE_NAME);
        /* Students, here is where you would add any other table names if you had them */
//        tableNameHashSet.add(MyAwesomeSuperCoolTableName);
//        tableNameHashSet.add(MyOtherCoolTableNameThatContainsOtherCoolData);

        /* We think the database is open, let's verify that here */
        String databaseIsNotOpen = "The database should be open and isn't";
        assertEquals(databaseIsNotOpen,
                true,
                database.isOpen());

        /* This Cursor will contain the names of each table in our database */
        Cursor tableNameCursor = database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'",
                null);

        /*
         * If tableNameCursor.moveToFirst returns false from this query, it means the database
         * wasn't created properly. In actuality, it means that your database contains no tables.
         */
        String errorInCreatingDatabase =
                "Error: This means that the database has not been created correctly";
        assertTrue(errorInCreatingDatabase,
                tableNameCursor.moveToFirst());

        /*
         * tableNameCursor contains the name of each table in this database. Here, we loop over
         * each table that was ACTUALLY created in the database and remove it from the
         * tableNameHashSet to keep track of the fact that was added. At the end of this loop, we
         * should have removed every table name that we thought we should have in our database.
         * If the tableNameHashSet isn't empty after this loop, there was a table that wasn't
         * created properly.
         */
        do {
            tableNameHashSet.remove(tableNameCursor.getString(0));
        } while (tableNameCursor.moveToNext());

        /* If this fails, it means that your database doesn't contain the expected table(s) */
        assertTrue("Error: Your database was created without the expected tables.",
                tableNameHashSet.isEmpty());

        /* Always close the cursor when you are finished with it */
        tableNameCursor.close();
    }

    /**
     * This method tests inserting a single record into an empty table from a brand new database.
     * It will fail for the following reasons:
     * <p>
     * 1) Problem creating the database
     * 2) A value of -1 for the ID of a single, inserted record
     * 3) An empty cursor returned from query on the centz table
     * 4) Actual values of centz data not matching the values from TestUtilities
     */
    @Test
    public void testInsertSingleRecordIntoCentzTable() {

        /* Obtain centz values from TestUtilities */
        ContentValues testCentzValues = TestUtilities.createTestCentzContentValues();

        /* Insert ContentValues into database and get a row ID back */
        long centzRowId = database.insert(
                REFLECTED_TABLE_NAME,
                null,
                testCentzValues);

        /* If the insert fails, database.insert returns -1 */
        int valueOfIdIfInsertFails = -1;
        String insertFailed = "Unable to insert into the database";
        assertNotSame(insertFailed,
                valueOfIdIfInsertFails,
                centzRowId);

        /*
         * Query the database and receive a Cursor. A Cursor is the primary way to interact with
         * a database in Android.
         */
        Cursor centzCursor = database.query(
                /* Name of table on which to perform the query */
                REFLECTED_TABLE_NAME,
                /* Columns; leaving this null returns every column in the table */
                null,
                /* Optional specification for columns in the "where" clause above */
                null,
                /* Values for "where" clause */
                null,
                /* Columns to group by */
                null,
                /* Columns to filter by row groups */
                null,
                /* Sort order to return in Cursor */
                null);

        /* Cursor.moveToFirst will return false if there are no records returned from your query */
        String emptyQueryError = "Error: No Records returned from centz query";
        assertTrue(emptyQueryError,
                centzCursor.moveToFirst());

        /* Verify that the returned results match the expected results */
        String expectedCentzDidntMatchActual =
                "Expected centz values didn't match actual values.";
        TestUtilities.validateCurrentRecord(expectedCentzDidntMatchActual,
                centzCursor,
                testCentzValues);

        /*
         * Since before every method annotated with the @Test annotation, the database is
         * deleted, we can assume in this method that there should only be one record in our
         * Centz table because we inserted it. If there is more than one record, an issue has
         * occurred.
         */
        assertFalse("Error: More than one record returned from centz query",
                centzCursor.moveToNext());

        /* Close cursor */
        centzCursor.close();
    }
}