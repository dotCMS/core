package com.dotcms.rest.api.v1.content.search;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.JSONField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.content.ContentSearchForm;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
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

import java.util.Map;

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
    private static Host defaultSite;

    private final ObjectMapper jsonMapper =
            DotObjectMapperProvider.getInstance().getDefaultObjectMapper();

    private static final String TEST_CONTENT_TYPE_NAME = "my_test_ct";

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
        defaultSite = APILocator.getHostAPI().findDefaultHost(adminUser, false);
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
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true"),

                new TestCase("Global Search",
                        "{\n" +
                        "    \"globalSearch\": \"dummy search\"\n" +
                        "}",
                        "+systemType:false -contentType:forms -contentType:Host title:'dummy search'^15title:dummy^5 title:search^5 title_dotraw:*dummy search*^5 +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true"),

                new TestCase("Published content",
                        "{\n" +
                                "    \"unpublishedContent\": false\n" +
                                "}",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +variant:default +deleted:false +live:true +working:true"),

                new TestCase("Unpublished content",
                        "{\n" +
                        "    \"unpublishedContent\": true\n" +
                        "}",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +variant:default +deleted:false +live:false +working:true"),

                new TestCase("Locked content",
                        "{\n" +
                                "    \"lockedContent\": true\n" +
                                "}",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +variant:default +deleted:false +locked:true +working:true"),

                new TestCase("Unlocked content",
                        "{\n" +
                                "    \"lockedContent\": false\n" +
                                "}",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +variant:default +deleted:false +locked:false +working:true"),

                new TestCase("Archived content",
                        "{\n" +
                                "    \"archivedContent\": true\n" +
                                "}",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +variant:default +deleted:true +working:true"),

                new TestCase("With all attributes present",
                        "{\n" +
                                "    \"unpublishedContent\": true,\n" +
                                "    \"lockedContent\": true,\n" +
                                "    \"archivedContent\": true,\n" +
                                "    \"orderBy\": \"modDate\",\n" +
                                "    \"page\": 0,\n" +
                                "    \"perPage\": 40\n" +
                                "}",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +variant:default +deleted:true +locked:true +live:false +working:true")
        };
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link }</li>
     *     <li><b>Given Scenario: </b></li>
     *     <li><b>Expected Result: </b></li>
     * </ul>
     *
     * @param testCase
     * @throws JsonProcessingException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    @UseDataProvider("contentStatusTestCases")
    public void runContentStatusTestCases(final TestCase testCase) throws JsonProcessingException, DotDataException, DotSecurityException {
        final ContentSearchForm contentSearchForm = jsonMapper.readValue(testCase.jsonBody, ContentSearchForm.class);
        final LuceneQueryBuilder luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, adminUser);
        final String luceneQuery = luceneQueryBuilder.build();

        assertNotNull(String.format("Generated query cannot be null for Test Case: '%s'", testCase.title), luceneQuery);
        assertFalse(String.format("Generated query cannot be an empty String for Test Case: '%s'", testCase.title), luceneQuery.isEmpty());
        assertEquals(String.format("The generated query is different than expected for Test Case: '%s'", testCase.title), testCase.expectedQuery, luceneQuery);
    }

    @DataProvider
    public static Object[] systemSearchableFieldsTestCases() {
        return new TestCase[]{
                new TestCase("With Site ID",
                        "{\n" +
                                "    \"systemSearchableFields\": {\n" +
                                "        \"siteId\": \"48190c8c-42c4-46af-8d1a-0cd5db894797\"\n" +
                                "    }\n" +
                                "}",
                        "+systemType:false -contentType:forms -contentType:Host +(conHost:48190c8c-42c4-46af-8d1a-0cd5db894797 conHost:SYSTEM_HOST) +variant:default +deleted:false +working:true"),

                new TestCase("Without Site ID",
                        "{\n" +
                                "    \"systemSearchableFields\": {\n" +
                                "        \"siteId\": \"\"\n" +
                                "    }\n" +
                                "}",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true"),

                new TestCase("With Language ID",
                        "{\n" +
                                "    \"systemSearchableFields\": {\n" +
                                "        \"languageId\": 1\n" +
                                "    }\n" +
                                "}",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +languageId:1 +variant:default +deleted:false +working:true"),

                new TestCase("With Workflow Scheme ID",
                        "{\n" +
                                "    \"systemSearchableFields\": {\n" +
                                "        \"workflowSchemeId\": \"d61a59e1-a49c-46f2-a929-db2b4bfa88b2\"\n" +
                                "    }\n" +
                                "}",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +(wfscheme:d61a59e1-a49c-46f2-a929-db2b4bfa88b2*) +variant:default +deleted:false +working:true"),

                new TestCase("With Workflow Step ID",
                        "{\n" +
                                "    \"systemSearchableFields\": {\n" +
                                "        \"workflowStepId\": \"dc3c9cd0-8467-404b-bf95-cb7df3fbc293\"\n" +
                                "    }\n" +
                                "}",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +(wfstep:dc3c9cd0-8467-404b-bf95-cb7df3fbc293*) +variant:default +deleted:false +working:true"),

                new TestCase("With Variant Name",
                        "{\n" +
                                "    \"systemSearchableFields\": {\n" +
                                "        \"variantName\": \"test-variant-name\"\n" +
                                "    }\n" +
                                "}",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +(variant:test-variant-name OR variant:default) +deleted:false +working:true"),

                new TestCase("With System Host Content",
                        "{\n" +
                                "    \"systemSearchableFields\": {\n" +
                                "        \"systemHostContent\": false\n" +
                                "    }\n" +
                                "}",
                        "+systemType:false -contentType:forms -contentType:Host +variant:default +deleted:false +working:true")
        };
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link }</li>
     *     <li><b>Given Scenario: </b></li>
     *     <li><b>Expected Result: </b></li>
     * </ul>
     *
     * @param testCase
     * @throws JsonProcessingException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    @UseDataProvider("systemSearchableFieldsTestCases")
    public void runSystemSearchableFieldsTestCases(final TestCase testCase) throws JsonProcessingException, DotDataException, DotSecurityException {
        final ContentSearchForm contentSearchForm = jsonMapper.readValue(testCase.jsonBody, ContentSearchForm.class);
        final LuceneQueryBuilder luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, adminUser);
        final String luceneQuery = luceneQueryBuilder.build();

        assertNotNull(String.format("Generated query cannot be null for Test Case: '%s'", testCase.title), luceneQuery);
        assertFalse(String.format("Generated query cannot be an empty String for Test Case: '%s'", testCase.title), luceneQuery.isEmpty());
        assertEquals(String.format("The generated query is different than expected for Test Case: '%s'", testCase.title), testCase.expectedQuery, luceneQuery);
    }

    @DataProvider
    public static Object[] contentTypeSearchableFieldsTestCases() {
        return new TestCase[]{
                new TestCase("With No Content Type",
                        "{ }",
                        "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true"),
        };
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link }</li>
     *     <li><b>Given Scenario: </b></li>
     *     <li><b>Expected Result: </b></li>
     * </ul>
     *
     * @param testCase
     * @throws JsonProcessingException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    @UseDataProvider("contentTypeSearchableFieldsTestCases")
    public void runContentTypeSearchableFieldsTestCases(final TestCase testCase) throws JsonProcessingException, DotDataException, DotSecurityException {
        final ContentSearchForm contentSearchForm = jsonMapper.readValue(testCase.jsonBody, ContentSearchForm.class);
        final LuceneQueryBuilder luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, adminUser);
        final String luceneQuery = luceneQueryBuilder.build();

        assertNotNull(String.format("Generated query cannot be null for Test Case: '%s'", testCase.title), luceneQuery);
        assertFalse(String.format("Generated query cannot be an empty String for Test Case: '%s'", testCase.title), luceneQuery.isEmpty());
        assertEquals(String.format("The generated query is different than expected for Test Case: '%s'", testCase.title), testCase.expectedQuery, luceneQuery);
    }

    @Test
    public void testSearchableFieldsByContentTypeAttribute() throws JsonProcessingException, DotDataException, DotSecurityException {
        final String testContentTypeOneName = "my_test_ct_one";
        final ContentType testContentTypeOne = new ContentTypeDataGen()
                .host(defaultSite)
                .name(testContentTypeOneName)
                .velocityVarName(testContentTypeOneName)
                .workflowId("d61a59e1-a49c-46f2-a929-db2b4bfa88b2").nextPersisted();
        try {
            String jsonBody = "{ }";
            String expectedQuery = "+systemType:false -contentType:forms -contentType:Host +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true";

            ContentSearchForm contentSearchForm = jsonMapper.readValue(jsonBody, ContentSearchForm.class);
            LuceneQueryBuilder luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, adminUser);
            String luceneQuery = luceneQueryBuilder.build();

            assertNotNull("Generated query using Velocity Var Name cannot be null", luceneQuery);
            assertFalse("Generated query using Velocity Var Name cannot be an empty String", luceneQuery.isEmpty());
            assertEquals("The generated query using Velocity Var Name is different than expected", expectedQuery, luceneQuery);

            jsonBody = "{\n" +
                    "    \"searchableFieldsByContentType\": {\n" +
                    "        \"" + testContentTypeOneName + "\": {\n" +
                    "            \n" +
                    "        }\n" +
                    "    }\n" +
                    "}";
            expectedQuery = "+contentType:(" + testContentTypeOneName + ") +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true";

            contentSearchForm = jsonMapper.readValue(jsonBody, ContentSearchForm.class);
            luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, adminUser);
            luceneQuery = luceneQueryBuilder.build();

            assertNotNull("Generated query using Velocity Var Name cannot be null", luceneQuery);
            assertFalse("Generated query using Velocity Var Name cannot be an empty String", luceneQuery.isEmpty());
            assertEquals("The generated query using Velocity Var Name is different than expected", expectedQuery, luceneQuery);

            jsonBody = "{\n" +
                    "    \"searchableFieldsByContentType\": {\n" +
                    "        \"" + testContentTypeOne.id() + "\": {\n" +
                    "            \n" +
                    "        }\n" +
                    "    }\n" +
                    "}";

            contentSearchForm = jsonMapper.readValue(jsonBody, ContentSearchForm.class);
            luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, adminUser);
            luceneQuery = luceneQueryBuilder.build();

            assertNotNull("Generated query using Content Type ID cannot be null", luceneQuery);
            assertFalse("Generated query using Content Type ID cannot be an empty String", luceneQuery.isEmpty());
            assertEquals("The generated query using Content Type ID is different than expected", expectedQuery, luceneQuery);
        } finally {
            ContentTypeDataGen.remove(testContentTypeOne, true);
        }
    }

    @DataProvider
    public static Object[] contentTypeSearchableFieldsWithFieldTestCases() {
        return new TestCase[]{
                new TestCase("With Text Field",
                        "text",
                        "{\n" +
                                "    \"searchableFieldsByContentType\": {\n" +
                                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                                "            \"text\": \"value\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}",
                        "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +(SSS.text:*value* SSS.text_dotraw:*value*)"
                                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME)),

                new TestCase("With Binary Field",
                        "binary",
                        "{\n" +
                                "    \"searchableFieldsByContentType\": {\n" +
                                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                                "            \"binary\": \"value\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}",
                        "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +SSS.binary:*value*"
                                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME)),

                new TestCase("With Checkbox Field",
                        "checkbox",
                        "{\n" +
                                "    \"searchableFieldsByContentType\": {\n" +
                                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                                "            \"checkbox\": \"value1,value2\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}",
                        "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +(SSS.checkbox:*value1* SSS.checkbox_dotraw:*value1*) +(SSS.checkbox:*value2* SSS.checkbox_dotraw:*value2*)"
                                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME)),

                new TestCase("With Custom Field",
                        "custom",
                        "{\n" +
                                "    \"searchableFieldsByContentType\": {\n" +
                                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                                "            \"custom\": \"value\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}",
                        "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +(SSS.custom:*value* SSS.custom_dotraw:*value*)"
                                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME)),

                new TestCase("With Date Field",
                        "date",
                        "{\n" +
                                "    \"searchableFieldsByContentType\": {\n" +
                                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                                "            \"date\": \"02/03/2025\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}",
                        "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +SSS.date:[02/03/2025 TO 02/03/2025]"
                                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME)),

                new TestCase("With Date and Time Field",
                        "dateAndTime",
                        "{\n" +
                                "    \"searchableFieldsByContentType\": {\n" +
                                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                                "            \"dateAndTime\": \"01/07/2025 13:50:00\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}",
                        "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +SSS.dateAndTime:[01/07/2025 13:50:00 TO 01/07/2025 13:50:00]"
                                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME)),

                new TestCase("With JSON Field",
                        "json",
                        "{\n" +
                                "    \"searchableFieldsByContentType\": {\n" +
                                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                                "            \"json\": \"value\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}",
                        "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +(SSS.json:*value* SSS.json_dotraw:*value*)"
                                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME)),

                new TestCase("With Key/Value Field",
                        "keyValue",
                        "{\n" +
                                "    \"searchableFieldsByContentType\": {\n" +
                                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                                "            \"keyValue\": \"value\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}",
                        "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +SSS.keyValue.key_value:*value*"
                                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME))
        };
    }

    @Test
    @UseDataProvider("contentTypeSearchableFieldsWithFieldTestCases")
    public void cosa(final TestCase testCase) throws JsonProcessingException, DotDataException, DotSecurityException {
        final ContentType testContentType = new ContentTypeDataGen()
                .host(defaultSite)
                .name(TEST_CONTENT_TYPE_NAME)
                .velocityVarName(TEST_CONTENT_TYPE_NAME)
                .field(getFields().get(testCase.field))
                .workflowId("d61a59e1-a49c-46f2-a929-db2b4bfa88b2").nextPersisted();
        try {
            final ContentSearchForm contentSearchForm = jsonMapper.readValue(testCase.jsonBody, ContentSearchForm.class);
            final LuceneQueryBuilder luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, adminUser);
            final String luceneQuery = luceneQueryBuilder.build();

            assertNotNull("Generated query using Content Type ID cannot be null", luceneQuery);
            assertFalse("Generated query using Content Type ID cannot be an empty String", luceneQuery.isEmpty());
            assertEquals("The generated query using Content Type ID is different than expected", testCase.expectedQuery, luceneQuery);
        } finally {
            ContentTypeDataGen.remove(testContentType, true);
        }
    }

    private Map<String, Field> getFields() {
        return Map.of(
                "text", new FieldDataGen()
                        .type(TextField.class)
                        .name("text")
                        .velocityVarName("text")
                        .values("")
                        .searchable(true).next(),
                "binary", new FieldDataGen()
                        .type(BinaryField.class)
                        .name("binary")
                        .velocityVarName("binary")
                        .values("")
                        .searchable(true).next(),
                "checkbox", new FieldDataGen()
                        .type(CheckboxField.class)
                        .name("checkbox")
                        .velocityVarName("checkbox")
                        .values("")
                        .searchable(true).next(),
                "custom", new FieldDataGen()
                        .type(CustomField.class)
                        .name("custom")
                        .velocityVarName("custom")
                        .values("")
                        .searchable(true).next(),
                "date", new FieldDataGen()
                        .type(DateField.class)
                        .name("date")
                        .velocityVarName("date")
                        .values("")
                        .defaultValue("")
                        .searchable(true).next()
                , "dateAndTime", new FieldDataGen()
                        .type(DateTimeField.class)
                        .name("dateAndTime")
                        .velocityVarName("dateAndTime")
                        .values("")
                        .defaultValue("")
                        .searchable(true).next()
                , "json", new FieldDataGen()
                        .type(JSONField.class)
                        .name("json")
                        .velocityVarName("json")
                        .values("")
                        .defaultValue("")
                        .searchable(true).next()
                , "keyValue", new FieldDataGen()
                        .type(KeyValueField.class)
                        .name("keyValue")
                        .velocityVarName("keyValue")
                        .values("")
                        .defaultValue("")
                        .searchable(true).next()
        );
    }

    @Test
    public void testCategoryField() {

    }














    @Test
    public void testSearchableFieldsByContentTypeWithText() throws JsonProcessingException, DotDataException, DotSecurityException {
        final Field textField = new FieldDataGen()
                .type(TextField.class)
                .name("text")
                .velocityVarName("text")
                .searchable(true).next();
        String jsonBody = "{\n" +
                "    \"searchableFieldsByContentType\": {\n" +
                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                "            \"text\": \"value\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        final String expectedQuery = "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +(SSS.text:*value* SSS.text_dotraw:*value*)"
                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME);
        this.validateSearchableFieldByContentType(textField, jsonBody, expectedQuery);
        /*final ContentType testContentTypeOne = new ContentTypeDataGen()
                .host(defaultSite)
                .name(TEST_CONTENT_TYPE_NAME)
                .velocityVarName(TEST_CONTENT_TYPE_NAME)
                .field(new FieldDataGen()
                        .type(TextField.class)
                        .name("text")
                        .velocityVarName("text")
                        .searchable(true).next())
                .workflowId("d61a59e1-a49c-46f2-a929-db2b4bfa88b2").nextPersisted();
        try {
            String jsonBody = "{\n" +
                    "    \"searchableFieldsByContentType\": {\n" +
                    "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                    "            \"text\": \"value\"\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";
            final String expectedQuery = String.format("+contentType:(%s) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +(%s.text:*value* %s.text_dotraw:*value*)",
                    TEST_CONTENT_TYPE_NAME, TEST_CONTENT_TYPE_NAME, TEST_CONTENT_TYPE_NAME);

            final ContentSearchForm contentSearchForm = jsonMapper.readValue(jsonBody, ContentSearchForm.class);
            final LuceneQueryBuilder luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, adminUser);
            final String luceneQuery = luceneQueryBuilder.build();

            assertNotNull("Generated query using Content Type ID cannot be null", luceneQuery);
            assertFalse("Generated query using Content Type ID cannot be an empty String", luceneQuery.isEmpty());
            assertEquals("The generated query using Content Type ID is different than expected", expectedQuery, luceneQuery);
        } finally {
            ContentTypeDataGen.remove(testContentTypeOne, true);
        }*/
    }

    @Test
    public void testSearchableFieldsByContentTypeWithBinary() throws JsonProcessingException, DotDataException, DotSecurityException {
        final Field textField = new FieldDataGen()
                .type(BinaryField.class)
                .name("binary")
                .velocityVarName("binary")
                .searchable(true).next();
        String jsonBody = "{\n" +
                "    \"searchableFieldsByContentType\": {\n" +
                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                "            \"binary\": \"value\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        final String expectedQuery = "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +SSS.binary:*value*"
                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME);
        this.validateSearchableFieldByContentType(textField, jsonBody, expectedQuery);
    }

    @Test
    public void testSearchableFieldsByContentTypeWithBlockEditor() throws JsonProcessingException, DotDataException, DotSecurityException {
        final Field textField = new FieldDataGen()
                .type(StoryBlockField.class)
                .name("blockEditor")
                .velocityVarName("blockEditor")
                .searchable(true).next();
        String jsonBody = "{\n" +
                "    \"searchableFieldsByContentType\": {\n" +
                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                "            \"blockEditor\": \"value\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        final String expectedQuery = "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +(SSS.blockEditor:*value* SSS.blockEditor_dotraw:*value*)"
                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME);
        this.validateSearchableFieldByContentType(textField, jsonBody, expectedQuery);
    }

    /**
     *
     * @param field
     * @param jsonBody
     * @param expectedQuery
     * @throws JsonProcessingException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private void validateSearchableFieldByContentType(final Field field, final String jsonBody, final String expectedQuery) throws JsonProcessingException, DotDataException, DotSecurityException {
        final ContentType testContentType = new ContentTypeDataGen()
                .host(defaultSite)
                .name(TEST_CONTENT_TYPE_NAME)
                .velocityVarName(TEST_CONTENT_TYPE_NAME)
                .field(field)
                .workflowId("d61a59e1-a49c-46f2-a929-db2b4bfa88b2").nextPersisted();
        try {
            final ContentSearchForm contentSearchForm = jsonMapper.readValue(jsonBody, ContentSearchForm.class);
            final LuceneQueryBuilder luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, adminUser);
            final String luceneQuery = luceneQueryBuilder.build();

            assertNotNull("Generated query using Content Type ID cannot be null", luceneQuery);
            assertFalse("Generated query using Content Type ID cannot be an empty String", luceneQuery.isEmpty());
            assertEquals("The generated query using Content Type ID is different than expected", expectedQuery, luceneQuery);
        } finally {
            ContentTypeDataGen.remove(testContentType, true);
        }
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

        String title;
        String field;
        String jsonBody;
        String expectedQuery;

        public TestCase(final String title, final String jsonBody, final String expectedQuery) {
            this.title = title;
            this.jsonBody = jsonBody;
            this.expectedQuery = expectedQuery;
        }

        public TestCase(final String title, final String field, final String jsonBody, final String expectedQuery) {
            this.title = title;
            this.field = field;
            this.jsonBody = jsonBody;
            this.expectedQuery = expectedQuery;
        }

    }

}
