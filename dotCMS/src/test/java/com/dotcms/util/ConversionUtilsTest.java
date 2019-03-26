package com.dotcms.util;

import com.dotcms.UnitTestBase;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.Assert;
import org.junit.Test;

/**
 * CollectionsUtils unit test.
 *
 * @author jsanca
 */
public class ConversionUtilsTest extends UnitTestBase {

  /** Testing the new Instance */
  @Test
  public void toLongTest() {

    final Double aDouble = 9.0;
    final Integer integer = 10;
    final Float aFloat = 11.1f;
    final Long aLong = 12L;
    final BigInteger bigInteger = BigInteger.valueOf(13L);
    final BigDecimal bigDecimal = new BigDecimal(14);
    final String string = "15";
    final CharSequence sequence = "16";
    final StringBuilder builder = new StringBuilder("17");
    final StringBuffer buffer = new StringBuffer("18");

    Assert.assertEquals(0L, ConversionUtils.toLong(null, 0L));
    Assert.assertEquals(0L, ConversionUtils.toLong("null", 0L));
    Assert.assertEquals(0L, ConversionUtils.toLong(new Object(), 0L));
    Assert.assertEquals(9L, ConversionUtils.toLong(aDouble, 0L));
    Assert.assertEquals(10L, ConversionUtils.toLong(integer, 0L));
    Assert.assertEquals(11L, ConversionUtils.toLong(aFloat, 0L));
    Assert.assertEquals(12L, ConversionUtils.toLong(aLong, 0L));
    Assert.assertEquals(13L, ConversionUtils.toLong(bigInteger, 0L));
    Assert.assertEquals(14L, ConversionUtils.toLong(bigDecimal, 0L));
    Assert.assertEquals(15L, ConversionUtils.toLong(string, 0L));
    Assert.assertEquals(16L, ConversionUtils.toLong(sequence, 0L));
    Assert.assertEquals(17L, ConversionUtils.toLong(builder, 0L));
    Assert.assertEquals(18L, ConversionUtils.toLong(buffer, 0L));
  }

  /** Testing the new Instance */
  @Test
  public void toIntTest() {

    final Double aDouble = 9.0;
    final Integer integer = 10;
    final Float aFloat = 11.1f;
    final Long aLong = 12L;
    final BigInteger bigInteger = BigInteger.valueOf(13L);
    final BigDecimal bigDecimal = new BigDecimal(14);
    final String string = "15";
    final CharSequence sequence = "16";
    final StringBuilder builder = new StringBuilder("17");
    final StringBuffer buffer = new StringBuffer("18");

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
