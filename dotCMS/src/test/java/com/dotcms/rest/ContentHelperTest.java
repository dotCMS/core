package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import org.junit.Test;

/**
 * Unit test for {@link ContentHelper}
 */
public class ContentHelperTest extends UnitTestBase {

    @Test
    public void testGetNullUrl() {

        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE);
        final String identifier = null;

        final String url = contentHelper.getUrl(identifier);

        assertNull(url);
    }

    @Test
    public void testGetNotFoundUrl() throws DotDataException {

        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE);
        final String identifier = null;

        when(identifierAPI.find(identifier)).thenReturn(null);

        final String url = contentHelper.getUrl(identifier);

        assertNull(url);
    }

    @Test
    public void testGetFoundUrl() throws DotDataException {

        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE);
        final String identifier = "1234";
        Identifier identifierObject = new Identifier();
        String urlExpected = "home_page";
        identifierObject.setAssetName(urlExpected);
        identifierObject.setId(identifier);

        when(identifierAPI.find(identifier)).thenReturn(identifierObject);

        final String url = contentHelper.getUrl(identifier);

        assertNotNull(url);
        assertEquals(urlExpected, url);
    }

    /**
     * Method to test: {@link ContentHelper#extractLanguageIdFromQuery(String, SearchForm)}
     * When:
     * - Test various query strings with languageId in different positions (start, middle, end)
     * - Test query without languageId field
     * - Test query with multiple spaces around languageId
     * - Test complex queries with multiple fields and operators
     * - Test empty query
     * Should:
     * - Extract the languageId value from the query string when present
     * - Return the SearchForm's languageId as fallback when languageId is not found in query
     * - Handle edge cases like end-of-string and multiple spaces correctly
     */
    @Test
    public void testExtractLanguageIdFromQuery() {
        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE);

        // Test 1: languageId in the middle of the query
        SearchForm searchForm1 = new SearchForm.Builder().languageId(1).build();
        String query1 = "+contentType:Blog +languageId:2 +live:true";
        Long result1 = contentHelper.extractLanguageIdFromQuery(query1, searchForm1);
        assertEquals("Should extract languageId from middle of query", Long.valueOf(2L), result1);

        // Test 2: languageId at the end of the query
        SearchForm searchForm2 = new SearchForm.Builder().languageId(1).build();
        String query2 = "+contentType:Blog +live:true +languageId:3";
        Long result2 = contentHelper.extractLanguageIdFromQuery(query2, searchForm2);
        assertEquals("Should extract languageId from end of query", Long.valueOf(3L), result2);

        // Test 3: languageId at the start of the query
        SearchForm searchForm3 = new SearchForm.Builder().languageId(1).build();
        String query3 = "+languageId:5 +contentType:Blog +live:true";
        Long result3 = contentHelper.extractLanguageIdFromQuery(query3, searchForm3);
        assertEquals("Should extract languageId from start of query", Long.valueOf(5L), result3);

        // Test 4: Query without languageId
        SearchForm searchForm4 = new SearchForm.Builder().languageId(10).build();
        String query4 = "+contentType:Blog +live:true";
        Long result4 = contentHelper.extractLanguageIdFromQuery(query4, searchForm4);
        assertEquals("Should return SearchForm's languageId when not in query", Long.valueOf(10L), result4);

        // Test 5: Query with multiple spaces around languageId
        SearchForm searchForm5 = new SearchForm.Builder().languageId(1).build();
        String query5 = "+contentType:Blog   +languageId:7   +live:true";
        Long result5 = contentHelper.extractLanguageIdFromQuery(query5, searchForm5);
        assertEquals("Should handle multiple spaces correctly", Long.valueOf(7L), result5);

        // Test 6: Complex query with multiple fields
        SearchForm searchForm6 = new SearchForm.Builder().languageId(1).build();
        String query6 = "+contentType:BlogPost +Blog.title:test* +languageId:4 +live:true +deleted:false";
        Long result6 = contentHelper.extractLanguageIdFromQuery(query6, searchForm6);
        assertEquals("Should extract languageId from complex query", Long.valueOf(4L), result6);

        // Test 7: Query with parentheses and OR operator
        SearchForm searchForm7 = new SearchForm.Builder().languageId(1).build();
        String query7 = "+(contentType:Blog OR contentType:News) +languageId:8 +live:true";
        Long result7 = contentHelper.extractLanguageIdFromQuery(query7, searchForm7);
        assertEquals("Should handle queries with parentheses and operators", Long.valueOf(8L), result7);

        // Test 8: Empty query
        SearchForm searchForm8 = new SearchForm.Builder().languageId(99).build();
        String query8 = "";
        Long result8 = contentHelper.extractLanguageIdFromQuery(query8, searchForm8);
        assertEquals("Should return SearchForm's languageId for empty query", Long.valueOf(99L), result8);

        // Test 9: Null query
        SearchForm searchForm9 = new SearchForm.Builder().languageId(50).build();
        String query9 = null;
        Long result9 = contentHelper.extractLanguageIdFromQuery(query9, searchForm9);
        assertEquals("Should return SearchForm's languageId for null query", Long.valueOf(50L), result9);
    }

}
