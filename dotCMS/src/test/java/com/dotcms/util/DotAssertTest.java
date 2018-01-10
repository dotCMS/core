package com.dotcms.util;

import com.dotcms.UnitTestBase;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * CollectionsUtils unit test.
 * @author jsanca
 */
public class DotAssertTest extends UnitTestBase {


    /**
     * Testing the isTrue
     *
     */
    @Test
    public void isTrueTest() {

        try {

            DotAssert.isTrue(false, "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotAssert.isTrue(true, "Right");

        //
        try {

            DotAssert.isTrue(false, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotAssert.isTrue(true, () -> "Right");

        //
        try {

            DotAssert.isTrue(false, () -> "Uppss", TestException.class);
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotAssert.isTrue(true, () -> "Right", TestException.class);
        } catch (Throwable throwable) {
            Assert.fail("It must not throws an Throwable");
        }

        //
        try {

            DotAssert.isTrue(false, TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        DotAssert.isTrue(true, TestException.class, () -> "Right");
    }

    @Test
    public void isNullTest() {

        try {

            DotAssert.isNull("Not null", "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotAssert.isNull(null, "Right");

        //
        try {

            DotAssert.isNull("Not null", () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotAssert.isNull(null, () -> "Right");

        //
        try {

            DotAssert.isNull("Not null", () -> "Uppss", TestException.class);
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotAssert.isNull(null, () -> "Right", TestException.class);
        } catch (Throwable throwable) {
            Assert.fail("It must not throws an Throwable");
        }

        //
        try {

            DotAssert.isNull("Not null", TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        DotAssert.isNull(null, TestException.class, () -> "Right");
    }

    /// Not Null
    @Test
    public void notNullTest() {

        try {

            DotAssert.notNull(null, "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotAssert.notNull("not null", "Right");

        //
        try {

            DotAssert.notNull(null, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotAssert.notNull(" not null", () -> "Right");

        //
        try {

            DotAssert.notNull(null, () -> "Uppss", TestException.class);
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotAssert.notNull("not null", () -> "Right", TestException.class);
        } catch (Throwable throwable) {
            Assert.fail("It must not throws an Throwable");
        }

        //
        try {

            DotAssert.notNull(null, TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        DotAssert.notNull("not null", TestException.class, () -> "Right");
    }

    /// Not Empty
    @Test
    public void notEmptyTest() {

        try {

            Object [] nullArray = null;
            DotAssert.notEmpty(nullArray, "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        try {

            DotAssert.notEmpty(new Object[] {}, "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotAssert.notEmpty(new Object[] {"not null"}, "Right");

        try {

            Object [] nullArray = null;
            DotAssert.notEmpty(nullArray, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        try {

            DotAssert.notEmpty(new Object[] {}, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotAssert.notEmpty(new Object[] {"not null"}, () -> "Right");

        try {

            Object [] nullArray = null;
            DotAssert.notEmpty(nullArray, () -> "Uppss", TestException.class);
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotAssert.notEmpty(new Object[] {"not null"}, () -> "Right", TestException.class);
        } catch (Throwable throwable) {
            Assert.fail("It must not throws an Throwable");
        }

        //
        try {

            Object [] nullArray = null;
            DotAssert.notEmpty(nullArray, TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotAssert.notEmpty(new Object[] {}, TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        DotAssert.notEmpty(new Object[] {"not null"}, TestException.class, () -> "Right");
    }

    /// Not Empty
    @Test
    public void notEmptyCollectionTest() {

        try {

            Collection nullArray = null;
            DotAssert.notEmpty(nullArray, "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        try {

            DotAssert.notEmpty(Arrays.asList(new Object[] {}), "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotAssert.notEmpty(Arrays.asList(new Object[] {"not null"}), "Right");

        try {

            Collection nullArray = null;
            DotAssert.notEmpty(nullArray, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        try {

            DotAssert.notEmpty(Arrays.asList(new Object[] {}), () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotAssert.notEmpty(Arrays.asList(new Object[] {"not null"}), () -> "Right");

        try {

            Collection nullArray = null;
            DotAssert.notEmpty(nullArray, () -> "Uppss", TestException.class);
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotAssert.notEmpty(Arrays.asList(new Object[] {"not null"}), () -> "Right", TestException.class);
        } catch (Throwable throwable) {
            Assert.fail("It must not throws an Throwable");
        }

        //
        try {

            Object [] nullArray = null;
            DotAssert.notEmpty(nullArray, TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotAssert.notEmpty(Arrays.asList(new Object[] {}), TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        DotAssert.notEmpty(Arrays.asList(new Object[] {"not null"}), TestException.class, () -> "Right");
    }

    /// Not Empty
    @Test
    public void notEmptyMapTest() {

        try {

            Map nullArray = null;
            DotAssert.notEmpty(nullArray, "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        try {

            DotAssert.notEmpty(new HashMap(), "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        Map map = new HashMap();
        map.put("not null", "not null");
        DotAssert.notEmpty(map, "Right");

        try {

            Map nullArray = null;
            DotAssert.notEmpty(nullArray, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        try {

            DotAssert.notEmpty(new HashMap(), () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotAssert.notEmpty(map, () -> "Right");

        try {

            Map nullArray = null;
            DotAssert.notEmpty(nullArray, () -> "Uppss", TestException.class);
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotAssert.notEmpty(map, () -> "Right", TestException.class);
        } catch (Throwable throwable) {
            Assert.fail("It must not throws an Throwable");
        }

        //
        try {

            Map nullArray = null;
            DotAssert.notEmpty(nullArray, TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotAssert.notEmpty(new HashMap(), TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        DotAssert.notEmpty(map, TestException.class, () -> "Right");
    }

    public static class TestException extends RuntimeException {
        public TestException(String message) {
            super(message);
        }
    }


}