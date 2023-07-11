package com.dotcms.util;

import com.dotcms.UnitTestBase;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.Stream;

/**
 * WhileBlackList unit test.
 * @author jsanca
 */
public class WhileBlackListTest extends UnitTestBase {

    /**
     * Method to test: {@link WhiteBlackList#isAllowed(String)}
     * Given Scenario: Creates a white list with a white patterns and a black patterns
     * ExpectedResult: The allowed patterns are allowed (js and css) and the not allowed patterns are not allowed (jsp and html)
     */
    @Test
    public void white_and_black_is_allowed_pattern_success() {

        final WhiteBlackList whiteBlackList = new WhiteBlackList.Builder()
                .addWhitePatterns(".*\\.js", ".*\\.css")
                 .addBlackPatterns(".*\\.jsp", ".*\\.html")
                .build();

        Assert.assertFalse(whiteBlackList.isAllowed(null));
        Assert.assertFalse(whiteBlackList.isAllowed(""));
        Assert.assertFalse(whiteBlackList.isAllowed("nomatch"));
        Assert.assertFalse(whiteBlackList.isAllowed("jsp-are-not-allowed.jsp"));
        Assert.assertFalse(whiteBlackList.isAllowed("html-are-not-allowed.html"));
        Assert.assertTrue(whiteBlackList.isAllowed("js-are-allowed.js"));
        Assert.assertTrue(whiteBlackList.isAllowed("css-are-allowed.css"));
    }

    /**
     * Method to test: {@link WhiteBlackList#isAllowed(String)}
     * Given Scenario: Creates a white list with a white patterns and a black patterns
     * ExpectedResult: The allowed patterns are allowed (js and css) and the not allowed patterns are not allowed (jsp and html)
     */
    @Test
    public void white_and_black_filter_pattern_success() {

        final WhiteBlackList whiteBlackList = new WhiteBlackList.Builder()
                .addWhitePatterns(".*\\.js", ".*\\.css")
                .addBlackPatterns(".*\\.jsp", ".*\\.html")
                .build();

        final Stream filteredStream = whiteBlackList.filter(Stream.of(null,"","js-are-allowed.js", "css-are-allowed.css", "jsp-are-not-allowed.jsp", "html-are-not-allowed.html"));

        Assert.assertEquals(2, filteredStream.count());
        Assert.assertTrue(filteredStream.allMatch(s -> s.equals("js-are-allowed.js") || s.equals("css-are-allowed.css")));
        Assert.assertTrue(filteredStream.noneMatch(s -> s.equals("jsp-are-not-allowed.jsp") || s.equals("html-are-not-allowed.html")));
    }

}
