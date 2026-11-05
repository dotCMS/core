package com.dotcms.publishing.sitesearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResult;
import com.dotcms.content.index.domain.SearchHit;
import java.util.List;
import java.util.Map;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.core.search.Hit;
import org.junit.Test;

/**
 * @author nollymar
 */
public class SiteSearchResultTest {

    /** Source-disabled search results retain metadata and a one-element keyword list. */
    @Test
    public void storedFieldsRetainResultMetadataAndKeywordList() {
        final Hit<Object> hit = Hit.of(builder -> builder.index("sitesearch_1").id("doc")
                .fields(Map.of(
                        "title", JsonData.of(List.of("Stored title")),
                        "url", JsonData.of(List.of("https://example.test/page")),
                        "content", JsonData.of(List.of("Searchable content")),
                        "keywords", JsonData.of(List.of("single")),
                        "language", JsonData.of(List.of(1)))));
        final SiteSearchResult result = new SiteSearchResult(SearchHit.from(hit).getSourceAsMap());

        assertEquals("Stored title", result.getTitle());
        assertEquals("https://example.test/page", result.getUrl());
        assertEquals("Searchable content", result.getContent());
        assertEquals(List.of("single"), result.getKeywords());
        assertEquals(1L, result.getLanguage());
    }

    /** Empty keyword arrays are not returned as stored fields, but the result still needs a list. */
    @Test
    public void missingStoredKeywordsRemainAnEmptyList() {
        assertEquals(List.of(), new SiteSearchResult(Map.of("title", "No keywords")).getKeywords());
    }

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
