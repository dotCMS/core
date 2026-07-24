package com.dotmarketing.util;

import static com.dotmarketing.util.LuceneQueryUtils.isLuceneQuery;

import com.dotcms.UnitTestBase;
import org.junit.Assert;
import org.junit.Test;

public class LuceneQueryUtilsTest extends UnitTestBase {

    @Test
    public void testRetrieveAllActions() throws Exception {
        final String retrieveAllActionsQuery = "+contentType:FileAsset +(conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 conhost:SYSTEM_HOST) +languageId:1 +deleted:false +working:true";
        final String preparedRetrieveAllActionsQuery = "+contentType:FileAsset +(conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 conhost:SYSTEM_HOST) +languageId:1 +deleted:false +working:true -contentType:host";
        final String sanitizedQuery = LuceneQueryUtils.sanitizeBulkActionsQuery(retrieveAllActionsQuery);
        Assert.assertEquals(preparedRetrieveAllActionsQuery , sanitizedQuery);
    }

    @Test
    public void testRetrieveAllActionsQueryWithPrefix() throws Exception {
        final String retrieveAllActionsQueryWithPrefix = "query_-contentType:Host -baseType:3 +(conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 conhost:SYSTEM_HOST) +languageId:1 +deleted:false +working:true";
        final String preparedRetrieveAllActionsQueryWithPrefix = "-contentType:Host -baseType:3 +(conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 conhost:SYSTEM_HOST) +languageId:1 +deleted:false +working:true -contentType:host";
        final String sanitizedQuery = LuceneQueryUtils.sanitizeBulkActionsQuery(retrieveAllActionsQueryWithPrefix);
        Assert.assertEquals(preparedRetrieveAllActionsQueryWithPrefix , sanitizedQuery);
    }

    @Test
    public void testRetrieveAllActionsQueryWithPrefixAndExcludeInodes() throws Exception {
        final String retrieveAllActionsQueryWithPrefixAndExcludeInodes = "query_-contentType:Host -baseType:3 +(conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 conhost:SYSTEM_HOST) +languageId:1 +deleted:false +working:true -(inode:4eb3c99f-e999-44aa-acdc-75371deb8776 inode:2cd36499-beac-4c5a-9f8e-65c28ce62b8f)";
        final String preparedRetrieveAllActionsQueryWithPrefixAndExcludeInodes = "-contentType:Host -baseType:3 +(conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 conhost:SYSTEM_HOST) +languageId:1 +deleted:false +working:true -(inode:4eb3c99f-e999-44aa-acdc-75371deb8776 inode:2cd36499-beac-4c5a-9f8e-65c28ce62b8f) -contentType:host";
        final String sanitizedQuery = LuceneQueryUtils.sanitizeBulkActionsQuery(retrieveAllActionsQueryWithPrefixAndExcludeInodes);
        Assert.assertEquals(preparedRetrieveAllActionsQueryWithPrefixAndExcludeInodes , sanitizedQuery);
    }

    @Test
    public void tstIsLuceneQuery() throws Exception {

        Assert.assertTrue(isLuceneQuery("+title:hello"));    // true - field query
        Assert.assertTrue(isLuceneQuery("hello AND world"));  // true - boolean operator
        Assert.assertTrue(isLuceneQuery("test*"));            // true - wildcard
        Assert.assertTrue(isLuceneQuery("\"hello world\""));
        Assert.assertTrue(isLuceneQuery("+id:a1b2c3d4-e5f6-7890-abcd-ef1234567890 -inode:550e8400-e29b-41d4-a716-446655440000"));
        Assert.assertTrue(isLuceneQuery("+contentType:Blog OR Activity +conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 OR cool-site +languageId:1 AND 2 +deleted:false +working:true +variant:default +title:Snowboard -summary:'Swiss Alps' +authors:'John Doe' NOT 'Jane Doe'"));

        // FALSE -  simple identifier
        Assert.assertFalse(isLuceneQuery("lol-223"));         // false - simple hyphenated identifier
        Assert.assertFalse(isLuceneQuery("badExample"));       // false - single term
        Assert.assertFalse(isLuceneQuery("user_123"));         // false - simple underscore identifier
        Assert.assertFalse(isLuceneQuery("550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    public void testEscapeLeavesCleanTermUnchanged() {
        Assert.assertEquals("angular", LuceneQueryUtils.escape("angular"));
        Assert.assertEquals("", LuceneQueryUtils.escape(""));
    }

    @Test
    public void testEscapeBackslashesLuceneSpecialCharacters() {
        Assert.assertEquals("quarterly\\-report", LuceneQueryUtils.escape("quarterly-report"));
        Assert.assertEquals("12\\:30", LuceneQueryUtils.escape("12:30"));
        Assert.assertEquals("a\\/\\(b\\)\\:c", LuceneQueryUtils.escape("a/(b):c"));
        // Every character in the query_string special set is escaped.
        Assert.assertEquals("\\\\\\+\\-\\!\\(\\)\\:\\^\\[\\]\\\"\\{\\}\\~\\*\\?\\|\\&\\/",
                LuceneQueryUtils.escape("\\+-!():^[]\"{}~*?|&/"));
    }

    /**
     * The neutral reimplementation must produce byte-for-byte the same output as the original
     * {@code QueryParser.escape} it replaced, so the ES→OS migration that drops the Lucene artifact
     * cannot change query behavior. {@code QueryParser} is still on the test classpath (via the ES
     * client), so we can assert parity directly.
     */
    @Test
    public void testEscapeMatchesLuceneQueryParserExactly() {
        final String[] samples = {
                "angular", "quarterly-report", "12:30", "a/(b):c", "x\")OR",
                "doc-dev-blue-cold.pdf", "color_blue-green", "", "no+special&chars|here",
                "path/to/asset", "a-b-c-d", "weird ~ ? * [ ] { } ^ chars"
        };
        for (final String s : samples) {
            Assert.assertEquals("escape parity mismatch for: " + s,
                    org.apache.lucene.queryparser.classic.QueryParser.escape(s),
                    LuceneQueryUtils.escape(s));
        }
    }

}
