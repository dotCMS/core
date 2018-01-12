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

            DotPreconditions.isTrue(false, "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotPreconditions.isTrue(true, "Right");

        //
        try {

            DotPreconditions.isTrue(false, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotPreconditions.isTrue(true, () -> "Right");

        //
        try {

            DotPreconditions.isTrue(false, () -> "Uppss", TestException.class);
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotPreconditions.isTrue(true, () -> "Right", TestException.class);
        } catch (Throwable throwable) {
            Assert.fail("It must not throws an Throwable");
        }

        //
        try {

            DotPreconditions.isTrue(false, TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        DotPreconditions.isTrue(true, TestException.class, () -> "Right");
    }

    @Test
    public void isNullTest() {

        try {

            DotPreconditions.isNull("Not null", "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotPreconditions.isNull(null, "Right");

        //
        try {

            DotPreconditions.isNull("Not null", () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotPreconditions.isNull(null, () -> "Right");

        //
        try {

            DotPreconditions.isNull("Not null", () -> "Uppss", TestException.class);
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotPreconditions.isNull(null, () -> "Right", TestException.class);
        } catch (Throwable throwable) {
            Assert.fail("It must not throws an Throwable");
        }

        //
        try {

            DotPreconditions.isNull("Not null", TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        DotPreconditions.isNull(null, TestException.class, () -> "Right");
    }

    /// Not Null
    @Test
    public void notNullTest() {

        try {

            DotPreconditions.notNull(null, "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotPreconditions.notNull("not null", "Right");

        //
        try {

            DotPreconditions.notNull(null, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotPreconditions.notNull(" not null", () -> "Right");

        //
        try {

            DotPreconditions.notNull(null, () -> "Uppss", TestException.class);
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotPreconditions.notNull("not null", () -> "Right", TestException.class);
        } catch (Throwable throwable) {
            Assert.fail("It must not throws an Throwable");
        }

        //
        try {

            DotPreconditions.notNull(null, TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        DotPreconditions.notNull("not null", TestException.class, () -> "Right");
    }

    /// Not Empty
    @Test
    public void notEmptyTest() {

        try {

            Object [] nullArray = null;
            DotPreconditions.notEmpty(nullArray, "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        try {

            DotPreconditions.notEmpty(new Object[] {}, "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotPreconditions.notEmpty(new Object[] {"not null"}, "Right");

        try {

            Object [] nullArray = null;
            DotPreconditions.notEmpty(nullArray, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        try {

            DotPreconditions.notEmpty(new Object[] {}, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotPreconditions.notEmpty(new Object[] {"not null"}, () -> "Right");

        try {

            Object [] nullArray = null;
            DotPreconditions.notEmpty(nullArray, () -> "Uppss", TestException.class);
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotPreconditions.notEmpty(new Object[] {"not null"}, () -> "Right", TestException.class);
        } catch (Throwable throwable) {
            Assert.fail("It must not throws an Throwable");
        }

        //
        try {

            Object [] nullArray = null;
            DotPreconditions.notEmpty(nullArray, TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotPreconditions.notEmpty(new Object[] {}, TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        DotPreconditions.notEmpty(new Object[] {"not null"}, TestException.class, () -> "Right");
    }

    /// Not Empty
    @Test
    public void notEmptyCollectionTest() {

        try {

            Collection nullArray = null;
            DotPreconditions.notEmpty(nullArray, "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        try {

            DotPreconditions.notEmpty(Arrays.asList(new Object[] {}), "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotPreconditions.notEmpty(Arrays.asList(new Object[] {"not null"}), "Right");

        try {

            Collection nullArray = null;
            DotPreconditions.notEmpty(nullArray, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        try {

            DotPreconditions.notEmpty(Arrays.asList(new Object[] {}), () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotPreconditions.notEmpty(Arrays.asList(new Object[] {"not null"}), () -> "Right");

        try {

            Collection nullArray = null;
            DotPreconditions.notEmpty(nullArray, () -> "Uppss", TestException.class);
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotPreconditions.notEmpty(Arrays.asList(new Object[] {"not null"}), () -> "Right", TestException.class);
        } catch (Throwable throwable) {
            Assert.fail("It must not throws an Throwable");
        }

        //
        try {

            Object [] nullArray = null;
            DotPreconditions.notEmpty(nullArray, TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotPreconditions.notEmpty(Arrays.asList(new Object[] {}), TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        DotPreconditions.notEmpty(Arrays.asList(new Object[] {"not null"}), TestException.class, () -> "Right");
    }

    /// Not Empty
    @Test
    public void notEmptyMapTest() {

        try {

            Map nullArray = null;
            DotPreconditions.notEmpty(nullArray, "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        try {

            DotPreconditions.notEmpty(new HashMap(), "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        Map map = new HashMap();
        map.put("not null", "not null");
        DotPreconditions.notEmpty(map, "Right");

        try {

            Map nullArray = null;
            DotPreconditions.notEmpty(nullArray, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        try {

            DotPreconditions.notEmpty(new HashMap(), () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotPreconditions.notEmpty(map, () -> "Right");

        try {

            Map nullArray = null;
            DotPreconditions.notEmpty(nullArray, () -> "Uppss", TestException.class);
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotPreconditions.notEmpty(map, () -> "Right", TestException.class);
        } catch (Throwable throwable) {
            Assert.fail("It must not throws an Throwable");
        }

        //
        try {

            Map nullArray = null;
            DotPreconditions.notEmpty(nullArray, TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        try {

            DotPreconditions.notEmpty(new HashMap(), TestException.class, () -> "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

            Assert.fail("It must not throws an IllegalArgumentException");
        } catch (TestException e) {

        }

        DotPreconditions.notEmpty(map, TestException.class, () -> "Right");
    }

    public static class TestException extends RuntimeException {
        public TestException(String message) {
            super(message);
        }
    }


}