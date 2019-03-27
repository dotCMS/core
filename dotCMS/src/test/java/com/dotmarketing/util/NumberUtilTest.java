package com.dotmarketing.util;

import com.dotcms.UnitTestBase;
import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for {@link DateUtil}
 * @author jsanca
 */
public class NumberUtilTest extends UnitTestBase {

    @Test
    public void testToIntNull () throws ParseException {

        final int i = NumberUtil.toInt(null, ()-> -1);
        assertEquals(-1, i);
    }

    @Test
    public void testToIntEmpty () throws ParseException {

        final int i = NumberUtil.toInt("", ()-> -1);
        assertEquals(-1, i);
    }

    @Test
    public void testToIntEmpty2 () throws ParseException {

        final int i = NumberUtil.toInt("        ", ()-> -1);
        assertEquals(-1, i);
    }

    @Test
    public void testToIntInvalid () throws ParseException {

        final int i = NumberUtil.toInt("123abc", ()-> -1);
        assertEquals(-1, i);
    }

    @Test
    public void testToInt () throws ParseException {

        final int i = NumberUtil.toInt("123", ()-> -1);
        assertEquals(123, i);
    }

    /////
    @Test
    public void testToLongNull () throws ParseException {

        final long i = NumberUtil.toLong(null, ()-> -1l);
        assertEquals(-1l, i);
    }

    @Test
    public void testToLongEmpty () throws ParseException {

        final long i = NumberUtil.toLong("", ()-> -1l);
        assertEquals(-1l, i);
    }

    @Test
    public void testToLongEmpty2 () throws ParseException {

        final long i = NumberUtil.toLong("        ", ()-> -1l);
        assertEquals(-1l, i);
    }

    @Test
    public void testToLongInvalid () throws ParseException {

        final long i = NumberUtil.toLong("123abc", ()-> -1l);
        assertEquals(-1l, i);
    }

    @Test
    public void testLongInt () throws ParseException {

        final long i = NumberUtil.toLong("123", ()-> -1l);
        assertEquals(123l, i);
    }

}
