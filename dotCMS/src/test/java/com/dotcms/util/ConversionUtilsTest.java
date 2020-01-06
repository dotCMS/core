package com.dotcms.util;

import com.dotcms.UnitTestBase;
import org.junit.Assert;
import org.junit.Test;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * CollectionsUtils unit test.
 * @author jsanca
 */
public class ConversionUtilsTest extends UnitTestBase {

    @Test
    public void toLongFromByteCountHumanDisplaySizeTest () {

        long value = ConversionUtils.toLongFromByteCountHumanDisplaySize("0", -1l);
        Assert.assertEquals(0l, value);

        value = ConversionUtils.toLongFromByteCountHumanDisplaySize("10", -1l);
        Assert.assertEquals(10l, value);

        value = ConversionUtils.toLongFromByteCountHumanDisplaySize("900", -1l);
        Assert.assertEquals(900l, value);

        value = ConversionUtils.toLongFromByteCountHumanDisplaySize("9000", -1l);
        Assert.assertEquals(9000l, value);

        value = ConversionUtils.toLongFromByteCountHumanDisplaySize("1kb", -1l);
        Assert.assertEquals(1024l, value);

        value = ConversionUtils.toLongFromByteCountHumanDisplaySize("112kb", -1l);
        Assert.assertEquals(112l * 1024l, value);

        value = ConversionUtils.toLongFromByteCountHumanDisplaySize("3kb", -1l);
        Assert.assertEquals( (3l * 1024l), value);

        value = ConversionUtils.toLongFromByteCountHumanDisplaySize("2mb", -1l);
        Assert.assertEquals( (2l * 1024l * 1024l), value);

        value = ConversionUtils.toLongFromByteCountHumanDisplaySize("4gb", -1l);
        Assert.assertEquals( (4l * 1024l * 1024l * 1024l), value);

        value = ConversionUtils.toLongFromByteCountHumanDisplaySize("4xxx", -1l);
        Assert.assertEquals( -1l, value);
    }


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

    /**
     * Testing the new Instance
     *
     */
    @Test
    public void toIntTest()  {


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


        Assert.assertEquals(0, ConversionUtils.toInt(null, 0));
        Assert.assertEquals(0, ConversionUtils.toInt("null", 0));
        Assert.assertEquals(0, ConversionUtils.toInt(new Object(), 0));
        Assert.assertEquals(9, ConversionUtils.toInt(aDouble, 0));
        Assert.assertEquals(10, ConversionUtils.toInt(integer, 0));
        Assert.assertEquals(11, ConversionUtils.toInt(aFloat, 0));
        Assert.assertEquals(12, ConversionUtils.toInt(aLong, 0));
        Assert.assertEquals(13, ConversionUtils.toInt(bigInteger, 0));
        Assert.assertEquals(14, ConversionUtils.toInt(bigDecimal, 0));
        Assert.assertEquals(15, ConversionUtils.toInt(string, 0));
        Assert.assertEquals(16, ConversionUtils.toInt(sequence, 0));
        Assert.assertEquals(17, ConversionUtils.toInt(builder, 0));
        Assert.assertEquals(18, ConversionUtils.toInt(buffer, 0));

    }
}