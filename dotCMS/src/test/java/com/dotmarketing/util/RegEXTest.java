package com.dotmarketing.util;

import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;

public class RegEXTest {

    @Test
    public void testCaseUnSensitiveAndCaseSensitiveLowerRegex () throws ParseException {

        final String text1  = "/dotAdmin";
        final String text2  = "/dotadmin";
        final String text3  = "/DotAdmin";
        final String regex = "/dotadmin";

        Assert.assertFalse(RegEX.contains(text1, regex));
        Assert.assertTrue(RegEX.contains(text2, regex));
        Assert.assertFalse(RegEX.contains(text3, regex));

        Assert.assertTrue(RegEX.containsCaseInsensitive(text1, regex));
        Assert.assertTrue(RegEX.containsCaseInsensitive(text2, regex));
        Assert.assertTrue(RegEX.containsCaseInsensitive(text3, regex));
    }

    @Test
    public void testCaseUnSensitiveAndCaseSensitiveMixRegex () throws ParseException {

        final String text1  = "/dotAdmin";
        final String text2  = "/dotadmin";
        final String text3  = "/DotAdmin";
        final String regex = "/dotADMIN";

        Assert.assertFalse(RegEX.contains(text1, regex));
        Assert.assertFalse(RegEX.contains(text2, regex));
        Assert.assertFalse(RegEX.contains(text3, regex));

        Assert.assertTrue(RegEX.containsCaseInsensitive(text1, regex));
        Assert.assertTrue(RegEX.containsCaseInsensitive(text2, regex));
        Assert.assertTrue(RegEX.containsCaseInsensitive(text3, regex));
    }
}
