package com.dotcms.util;

import org.junit.Test;
import org.junit.Assert;

public class SizeUtilTest {

    @Test
    public void testNullOrEmpty() {
        Assert.assertEquals(0, SizeUtil.convertToBytes(null));
        Assert.assertEquals(0, SizeUtil.convertToBytes(""));
    }

    @Test
    public void testNumberOnly() {
        Assert.assertEquals(123L, SizeUtil.convertToBytes("123"));
    }

    @Test
    public void testKb() {
        Assert.assertEquals(10L * 1024L, SizeUtil.convertToBytes("10kb"));
    }

    @Test
    public void testMb() {
        Assert.assertEquals(5L * 1024L * 1024L, SizeUtil.convertToBytes("5MB"));
    }

    @Test
    public void testGb() {
        Assert.assertEquals(2L * 1024L * 1024L * 1024L, SizeUtil.convertToBytes("2GB"));
    }

    @Test
    public void testGbWithSpaces() {
        Assert.assertEquals(2L * 1024L * 1024L * 1024L, SizeUtil.convertToBytes("2 GB"));
    }



    @Test
    public void testB() {
        Assert.assertEquals(256L, SizeUtil.convertToBytes("256b"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidInput() {
        SizeUtil.convertToBytes("invalid");
    }
}
