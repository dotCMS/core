package com.dotcms.util;

import com.dotcms.UnitTestBase;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * CollectionsUtils unit test.
 * @author jsanca
 */
public class ConversionUtilsTest extends UnitTestBase {


    /**
     * Testing the new Instance
     *
     */
    @Test
    public void toLongTest()  {


        final Double        aDouble    = 9.0;
        final Integer       integer    = 10;
        final Float         aFloat     = 11.1f;
        final Long          aLong      = 12l;
        final BigInteger    bigInteger = BigInteger.valueOf(13l);
        final BigDecimal    bigDecimal = new BigDecimal(14);
        final String        string     = "15";
        final CharSequence  sequence   = "16";
        final StringBuilder builder    = new StringBuilder("17");
        final StringBuffer  buffer     = new StringBuffer("18");


        Assert.assertEquals(0l, ConversionUtils.toLong(null, 0l));
        Assert.assertEquals(0l, ConversionUtils.toLong("null", 0l));
        Assert.assertEquals(0l, ConversionUtils.toLong(new Object(), 0l));
        Assert.assertEquals(9l, ConversionUtils.toLong(aDouble, 0l));
        Assert.assertEquals(10l, ConversionUtils.toLong(integer, 0l));
        Assert.assertEquals(11l, ConversionUtils.toLong(aFloat, 0l));
        Assert.assertEquals(12l, ConversionUtils.toLong(aLong, 0l));
        Assert.assertEquals(13l, ConversionUtils.toLong(bigInteger, 0l));
        Assert.assertEquals(14l, ConversionUtils.toLong(bigDecimal, 0l));
        Assert.assertEquals(15l, ConversionUtils.toLong(string, 0l));
        Assert.assertEquals(16l, ConversionUtils.toLong(sequence, 0l));
        Assert.assertEquals(17l, ConversionUtils.toLong(builder, 0l));
        Assert.assertEquals(18l, ConversionUtils.toLong(buffer, 0l));

    }
}