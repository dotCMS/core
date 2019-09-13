package com.dotcms.publishing.sitesearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResult;
import org.junit.Test;

/**
 * @author nollymar
 */
public class SiteSearchResultTest {

    @Test
    public void testSetKeywordsReturnsListOfTrimmedStrings(){
        SiteSearchResult result = new SiteSearchResult();
        result.setKeywords(" firstKey, second key, thirdKey ");

        assertTrue(!result.getKeywords().isEmpty());
        assertEquals("firstKey", result.getKeywords().get(0));
        assertEquals("second key", result.getKeywords().get(1));
        assertEquals("thirdKey", result.getKeywords().get(2));
    }

    @Test
    public void testSetKeywordsReturnsEmptyListWhenNoKeywordSet(){
        SiteSearchResult result = new SiteSearchResult();
        result.setKeywords(null);
        assertTrue(result.getKeywords().isEmpty());

        result.setKeywords("");
        assertTrue(result.getKeywords().isEmpty());
    }

}
