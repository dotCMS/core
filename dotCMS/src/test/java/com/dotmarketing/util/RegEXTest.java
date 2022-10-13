package com.dotmarketing.util;

import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.util.Optional;

public class RegEXTest {

    @Test
    public void testCaseUnSensitiveAndCaseSensitiveLowerRegexWildcardEnd () throws ParseException {

        final String text1  = "/dotAdmin/#/c/maintenance";
        final String text2  = "/dotadmin/#/c/configuration";
        final String text3  = "/DotAdmin/#/apps/test.gif";
        final String text4  = "/DotAdmin";
        final String text5  = "/c/maintenance";
        final String text6  = "/DotAdmin/#/apps/test.gif";
        final String regex = ".[.]gif";

        Assert.assertFalse(RegEX.containsCaseInsensitive(text1, regex));
        Assert.assertFalse(RegEX.containsCaseInsensitive(text2, regex));
        Assert.assertTrue(RegEX.containsCaseInsensitive(text3, regex));
        Assert.assertFalse(RegEX.containsCaseInsensitive(text4, regex));
        Assert.assertFalse(RegEX.containsCaseInsensitive(text5, regex));
        Assert.assertTrue(RegEX.containsCaseInsensitive(text6, regex));



    }

    @Test
    public void testCaseUnSensitiveAndCaseSensitiveLowerRegexWildcard () throws ParseException {

        final String text1  = "/dotAdmin/#/c/maintenance";
        final String text2  = "/dotadmin/#/c/configuration";
        final String text3  = "/DotAdmin/#/apps";
        final String text4  = "/DotAdmin";
        final String text5  = "/c/maintenance";
        final String regex = "/dotadmin*";

        Assert.assertTrue(RegEX.containsCaseInsensitive(text1, regex));
        Assert.assertTrue(RegEX.containsCaseInsensitive(text2, regex));
        Assert.assertTrue(RegEX.containsCaseInsensitive(text3, regex));
        Assert.assertTrue(RegEX.containsCaseInsensitive(text4, regex));
        Assert.assertFalse(RegEX.containsCaseInsensitive(text5, regex));


        final String regex2  = "/dotAdmin/*.gif$";
    }

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

    /**
     * Method to test: {@link RegEX#replace(String, String, String)}
     * Given Scenario: Do a replace by white space
     * ExpectedResult: The token is replaced with white space
     *
     */
    @Test
    public void test_replace_token_by_white_space () throws ParseException {

        final String replace      = "_sepsep_";
        final String replacement  = " ";
        final String token        = "CMS_sepsep_Administrator";
        final String expected     = "CMS Administrator";

        final String result = RegEX.replace(token, replacement, replace);

        Assert.assertEquals(expected, result);
    }

    /**
     * Method to test: {@link RegEX#replace(String, String, String)}
     * Given Scenario: Do a parsing and replace
     * ExpectedResult: The role will be clean up
     *
     */
    @Test
    public void test_replace_token_by_white_space_on_pattern () throws ParseException {

        final String pattern      = "/_sepsep_/ /";
        Optional<Tuple2<String, String>> substitutionTokenOpt = Optional.empty();

        if (UtilMethods.isSet(pattern) && pattern.startsWith(StringPool.FORWARD_SLASH)
                && pattern.endsWith(StringPool.FORWARD_SLASH)) {

            final String [] substitutionTokens = pattern.substring(1, pattern.length()-1).split(StringPool.FORWARD_SLASH);
            substitutionTokenOpt = substitutionTokens.length == 2? Optional.ofNullable(Tuple.of(substitutionTokens[0], substitutionTokens[1])): Optional.empty();
        }

        Assert.assertTrue(substitutionTokenOpt.isPresent());

        final String replace      = substitutionTokenOpt.get()._1();
        final String replacement  = substitutionTokenOpt.get()._2();
        final String token        = "CMS_sepsep_Administrator";
        final String expected     = "CMS Administrator";

        final String result = RegEX.replace(token, replacement, replace);

        Assert.assertEquals(expected, result);
    }
}
