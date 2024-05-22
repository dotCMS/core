package com.liferay.util;

import com.dotcms.UnitTestBase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.liferay.util.StringUtil.replaceAll;
import static com.liferay.util.StringUtil.replaceAllGroups;
import static com.liferay.util.StringUtil.replaceOnce;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StringUtilTest extends UnitTestBase {

    @Nested
    @DisplayName("Tests for replaceOnce")
    class ReplaceOnceTests {

        @Test
        @DisplayName("Test replaceOnce with null values")
        void testReplaceOnceNull() {
            StringBuilder builder = new StringBuilder("somethingtochange$1");

            replaceOnce(builder, "$1", null);
            assertEquals("somethingtochange$1", builder.toString(), "Replace once with null newSub should not change the builder");

            replaceOnce(builder, null, "this");
            assertEquals("somethingtochange$1", builder.toString(), "Replace once with null oldSub should not change the builder");

            replaceOnce(null, "$1", "this");
            assertEquals("somethingtochange$1", builder.toString(), "Replace once with null builder should not change the builder");
        }

        @ParameterizedTest(name = "{index} => builder={0}, oldSub={1}, newSub={2}, expected={3}")
        @CsvSource({
                "'somethingtochange$1', '$1', 'this', 'somethingtochangethis'",
                "'somethingtochange$2$1', '$1', 'this', 'somethingtochange$2this'",
                "'somethingtochange$2$1', '$2', 'these', 'somethingtochangethese$1'",
                "'somethingtochange', '$1', 'this', 'somethingtochange'"
        })
        @DisplayName("Test replaceOnce with various inputs")
        void testReplaceOnceParameterized(String builderStr, String oldSub, String newSub, String expected) {
            StringBuilder builder = new StringBuilder(builderStr);
            replaceOnce(builder, oldSub, newSub);
            assertEquals(expected, builder.toString());
        }
    }

    @Nested
    @DisplayName("Tests for replaceAll")
    class ReplaceAllTests {

        @Test
        @DisplayName("Test replaceAll with null values")
        void testReplaceAllNull() {
            StringBuilder builder = new StringBuilder("somethingtochange$1");

            replaceAll(builder, new String[]{"$1"}, null);
            assertEquals("somethingtochange$1", builder.toString(), "Replace all with null newSubs should not change the builder");

            replaceAll(builder, null, new String[]{"this"});
            assertEquals("somethingtochange$1", builder.toString(), "Replace all with null oldSubs should not change the builder");

            replaceAll(null, new String[]{"$1"}, new String[]{"this"});
            assertEquals("somethingtochange$1", builder.toString(), "Replace all with null builder should not change the builder");
        }

        @ParameterizedTest(name = "{index} => builder={0}, oldSubs={1}, newSubs={2}, expected={3}")
        @CsvSource({
                "'somethingtochange$1', '$1', 'this', 'somethingtochangethis'",
                "'somethingtochange$2$1', '$1,$2', 'this,these', 'somethingtochangethesethis'",
                "'somethingtochange', '$1,$2', 'this,these', 'somethingtochange'",
                "'somethingtochange$1$2', '$1,$2', 'this,these,those', 'somethingtochange$1$2'",
                "'somethingtochange$1$2', '$1,$2,$3', 'this,these', 'somethingtochange$1$2'"
        })
        @DisplayName("Test replaceAll with various inputs")
        void testReplaceAllParameterized(String builderStr, String oldSubs, String newSubs, String expected) {
            StringBuilder builder = new StringBuilder(builderStr);
            replaceAll(builder, oldSubs.split(","), newSubs.split(","));
            assertEquals(expected, builder.toString());
        }
    }

    @Nested
    @DisplayName("Tests for replaceAllGroups")
    class ReplaceAllGroupsTests {

        @ParameterizedTest(name = "{index} => builder={0}, newSubs={1}, expected={2}")
        @CsvSource({
                "'$1 is a test, and $1 is another test, $2 are great, are not $2', 'this,these', 'this is a test, and this is another test, these are great, are not these'",
                "'$1$1 is a test, and $1 is another test, $1 this is a great test', 'this', 'thisthis is a test, and this is another test, this this is a great test'"
        })
        @DisplayName("Test replaceAllGroups with various inputs")
        void testReplaceAllGroupsParameterized(String builderStr, String newSubs, String expected) {
            StringBuilder builder = new StringBuilder(builderStr);
            replaceAllGroups(builder, newSubs.split(","));
            assertEquals(expected, builder.toString());
        }
    }
}