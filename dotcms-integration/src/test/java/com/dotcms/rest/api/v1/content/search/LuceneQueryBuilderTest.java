package com.dotcms.rest.api.v1.content.search;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.content.ContentSearchForm;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * This Integration Test verifies that the {@link LuceneQueryBuilder} class works as expected.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class LuceneQueryBuilderTest extends IntegrationTestBase {

    private static User adminUser;
    private final ObjectMapper jsonMapper =
            DotObjectMapperProvider.getInstance().getDefaultObjectMapper();

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
        initialize();
    }

    private static void initialize() throws DotDataException, DotSecurityException {
        adminUser = APILocator.getUserAPI().loadByUserByEmail("admin@dotcms.com",
                APILocator.systemUser(), false);
    }

    /**
     * This DataProvider provides the test cases for the different content status filters:
     * <ul>
     *     <li>Global Search.</li>
     *     <li>Unpublished content.</li>
     *     <li>Locked content.</li>
     *     <li>Deleted content.</li>
     *     <li>Working content.</li>
     *     <li>Live content.</li>
     * </ul>
     *
     * @return An array of {@link TestCase} objects.
     */
    @DataProvider
    public static Object[] contentStatusTestCases() {
        return new TestCase[]{
                new TestCase("Empty Global Search",
                        "{\n" +
                                "    " +
                                "}",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +variant:default +deleted:false +locked:false +live:true +working:true"),
                new TestCase("Global Search",
                        "{\n" +
                        "    \"globalSearch\": \"dummy search\"\n" +
                        "}",
                        "+systemType:false -contentType:forms -contentType:Host title:'dummy search'^15title:dummy^5 title:search^5 title_dotraw:*dummy search*^5 +conHost:SYSTEM_HOST +variant:default +deleted:false +locked:false +live:true +working:true"),
                new TestCase("Published content",
                        "{\n" +
                                "    \"unpublishedContent\": false\n" +
                                "}",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +variant:default +deleted:false +locked:false +live:true +working:true"),
                new TestCase("Unpublished content",
                        "{\n" +
                        "    \"unpublishedContent\": true\n" +
                        "}",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +variant:default +deleted:false +locked:false +live:false +working:true"),
                new TestCase("Locked content",
                        "{\n" +
                                "    \"lockedContent\": true\n" +
                                "}",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +variant:default +deleted:false +locked:true +live:true +working:true"),
                new TestCase("Unlocked content",
                        "{\n" +
                                "    \"lockedContent\": false\n" +
                                "}",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +variant:default +deleted:false +locked:false +live:true +working:true"),
                new TestCase("Archived content",
                        "{\n" +
                                "    \"archivedContent\": true\n" +
                                "}",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +variant:default +deleted:true +locked:false +live:true +working:true")
        };
    }

    @Test
    @UseDataProvider("contentStatusTestCases")
    public void runContentStatusTestCases(final TestCase testCase) throws JsonProcessingException, DotDataException, DotSecurityException {
        final ContentSearchForm contentSearchForm = jsonMapper.readValue(testCase.jsonBody, ContentSearchForm.class);
        final LuceneQueryBuilder luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, adminUser);
        final String luceneQuery = luceneQueryBuilder.build();

        assertNotNull("Generated query cannot be null", luceneQuery);
        assertFalse("Generated query cannot be an empty String", luceneQuery.isEmpty());
        assertEquals("The generated query is different than expected", testCase.generatedQuery, luceneQuery);
    }

    /**
     * Defines a test case for the {@link LuceneQueryBuilderTest} class. It's composed of:
     * <ol>
     *     <li>A description of the test case.</li>
     *     <li>The JSON body that will be used to create the {@link ContentSearchForm} object.</li>
     *     <li>The expected Lucene query that will be generated.</li>
     * </ol>
     */
    public static class TestCase {

        String description;
        String jsonBody;
        String generatedQuery;

        public TestCase(final String description, final String jsonBody, final String generatedQuery) {
            this.description = description;
            this.jsonBody = jsonBody;
            this.generatedQuery = generatedQuery;
        }

    }

}
