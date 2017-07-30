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

import android.content.UriMatcher;
import android.net.Uri;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.example.android.centz.data.TestUtilities.getStaticIntegerField;
import static com.example.android.centz.data.TestUtilities.studentReadableNoSuchField;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class TestUriMatcher {

    private static final Uri TEST_CENTZ_DIR = CentzContract.CentzEntry.CONTENT_URI;
    private static final Uri TEST_CENTZ_WITH_DATE_DIR = CentzContract.CentzEntry
            .buildCentzUriWithDate(TestUtilities.DATE_NORMALIZED);

    private static final String centzCodeVariableName = "CODE_CENTZ";
    private static int REFLECTED_CENTZ_CODE;

    private static final String centzCodeWithDateVariableName = "CODE_CENTZ_WITH_DATE";
    private static int REFLECTED_CENTZ_WITH_DATE_CODE;

    private UriMatcher testMatcher;

    @Before
    public void before() {
        try {

            Method buildUriMatcher = CentzProvider.class.getDeclaredMethod("buildUriMatcher");
            testMatcher = (UriMatcher) buildUriMatcher.invoke(CentzProvider.class);

            REFLECTED_CENTZ_CODE = getStaticIntegerField(
                    CentzProvider.class,
                    centzCodeVariableName);

            REFLECTED_CENTZ_WITH_DATE_CODE = getStaticIntegerField(
                    CentzProvider.class,
                    centzCodeWithDateVariableName);

        } catch (NoSuchFieldException e) {
            fail(studentReadableNoSuchField(e));
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
        } catch (NoSuchMethodException e) {
            String noBuildUriMatcherMethodFound =
                    "It doesn't appear that you have created a method called buildUriMatcher in " +
                            "the CentzProvider class.";
            fail(noBuildUriMatcherMethodFound);
        } catch (InvocationTargetException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Students: This function tests that your UriMatcher returns the correct integer value for
     * each of the Uri types that our ContentProvider can handle. Uncomment this when you are
     * ready to test your UriMatcher.
     */
    @Test
    public void testUriMatcher() {

        /* Test that the code returned from our matcher matches the expected centz code */
        String centzUriDoesNotMatch = "Error: The CODE_CENTZ URI was matched incorrectly.";
        int actualCentzCode = testMatcher.match(TEST_CENTZ_DIR);
        int expectedCentzCode = REFLECTED_CENTZ_CODE;
        assertEquals(centzUriDoesNotMatch,
                expectedCentzCode,
                actualCentzCode);

        /*
         * Test that the code returned from our matcher matches the expected centz with date code
         */
        String centzWithDateUriCodeDoesNotMatch =
                "Error: The CODE_CENTZ WITH DATE URI was matched incorrectly.";
        int actualCentzWithDateCode = testMatcher.match(TEST_CENTZ_WITH_DATE_DIR);
        int expectedCentzWithDateCode = REFLECTED_CENTZ_WITH_DATE_CODE;
        assertEquals(centzWithDateUriCodeDoesNotMatch,
                expectedCentzWithDateCode,
                actualCentzWithDateCode);
    }
}
