package com.dotcms.rest.api.v1.content.search;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.JSONField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FieldRelationshipDataGen;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.content.ContentSearchForm;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    private static final String TEST_CHILD_CONTENT_TYPE_NAME = "my_child_test_ct";

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
        final ContentType testContentType = this.createTestContentType(TEST_CONTENT_TYPE_NAME, TEST_CONTENT_TYPE_NAME);
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
                    "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                    "            \n" +
                    "        }\n" +
                    "    }\n" +
                    "}";
            expectedQuery = "+contentType:(" + TEST_CONTENT_TYPE_NAME + ") +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true";

            contentSearchForm = jsonMapper.readValue(jsonBody, ContentSearchForm.class);
            luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, adminUser);
            luceneQuery = luceneQueryBuilder.build();

            assertNotNull("Generated query using Velocity Var Name cannot be null", luceneQuery);
            assertFalse("Generated query using Velocity Var Name cannot be an empty String", luceneQuery.isEmpty());
            assertEquals("The generated query using Velocity Var Name is different than expected", expectedQuery, luceneQuery);

            jsonBody = "{\n" +
                    "    \"searchableFieldsByContentType\": {\n" +
                    "        \"" + testContentType.id() + "\": {\n" +
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
            ContentTypeDataGen.remove(testContentType, true);
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
                                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME)),

                new TestCase("With Multi-Select Field",
                        "multiSelect",
                        "{\n" +
                                "    \"searchableFieldsByContentType\": {\n" +
                                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                                "            \"multiSelect\": \"multi1, multi2\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}",
                        "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +(SSS.multiSelect:*multi1* SSS.multiSelect_dotraw:*multi1*) +(SSS.multiSelect:*multi2* SSS.multiSelect_dotraw:*multi2*)"
                                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME)),

                new TestCase("With Radio Field",
                        "radio",
                        "{\n" +
                                "    \"searchableFieldsByContentType\": {\n" +
                                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                                "            \"radio\": \"radio1\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}",
                        "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +(SSS.radio:*radio1* SSS.radio_dotraw:*radio1*)"
                                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME)),

                new TestCase("With Select Field",
                        "select",
                        "{\n" +
                                "    \"searchableFieldsByContentType\": {\n" +
                                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                                "            \"select\": \"select1\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}",
                        "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +(SSS.select:*select1* SSS.select_dotraw:*select1*)"
                                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME)),

                new TestCase("With Tag Field",
                        "tag",
                        "{\n" +
                                "    \"searchableFieldsByContentType\": {\n" +
                                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                                "            \"tag\": \"beach, mountain\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}",
                        "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +SSS.tag:\"beach\" +SSS.tag:\"mountain\""
                                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME)),

                new TestCase("With Text Area Field",
                        "textArea",
                        "{\n" +
                                "    \"searchableFieldsByContentType\": {\n" +
                                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                                "            \"textArea\": \"some values\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}",
                        "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +(SSS.textArea:*some* SSS.textArea_dotraw:*some*) +(SSS.textArea:*values* SSS.textArea_dotraw:*values*)"
                                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME)),

                new TestCase("With Time Field",
                        "time",
                        "{\n" +
                                "    \"searchableFieldsByContentType\": {\n" +
                                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                                "            \"time\": \"12:30PM\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}",
                        "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +SSS.time:[12:30PM TO 12:30PM]"
                                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME)),

                new TestCase("With WYSIWYG Field",
                        "wysiwyg",
                        "{\n" +
                                "    \"searchableFieldsByContentType\": {\n" +
                                "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                                "            \"wysiwyg\": \"some values\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}",
                        "+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +(SSS.wysiwyg:*some* SSS.wysiwyg_dotraw:*some*) +(SSS.wysiwyg:*values* SSS.wysiwyg_dotraw:*values*)"
                                .replaceAll("SSS", TEST_CONTENT_TYPE_NAME)),
        };
    }

    @Test
    @UseDataProvider("contentTypeSearchableFieldsWithFieldTestCases")
    public void testSearchableFieldsByContentType(final TestCase testCase) throws JsonProcessingException, DotDataException, DotSecurityException {
        /*final ContentType testContentType = new ContentTypeDataGen()
                .host(defaultSite)
                .name(TEST_CONTENT_TYPE_NAME)
                .velocityVarName(TEST_CONTENT_TYPE_NAME)
                .field(getFields().get(testCase.field))
                .workflowId("d61a59e1-a49c-46f2-a929-db2b4bfa88b2").nextPersisted();*/
        //final ContentType testContentType = this.createTestContentType(this.getFields().get(testCase.field));
        final ContentType testContentType = this.createTestContentType(TEST_CONTENT_TYPE_NAME, TEST_CONTENT_TYPE_NAME, List.of(this.getFields().get(testCase.field)));
        try {
            final ContentSearchForm contentSearchForm = jsonMapper.readValue(testCase.jsonBody, ContentSearchForm.class);
            final LuceneQueryBuilder luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, adminUser);
            final String luceneQuery = luceneQueryBuilder.build();

            assertNotNull(String.format("Generated query cannot be null for Test Case: '%s'", testCase.title), luceneQuery);
            assertFalse(String.format("Generated query cannot be an empty String for Test Case: '%s'", testCase.title), luceneQuery.isEmpty());
            assertEquals(String.format("The generated query is different than expected for Test Case: '%s'", testCase.title), testCase.expectedQuery, luceneQuery);
        } finally {
            ContentTypeDataGen.remove(testContentType, true);
        }
    }

    /**
     * Utility method that adds a specific Field to the test Content Type. This is meant just for
     * passing down test data to the {@link #contentTypeSearchableFieldsWithFieldTestCases()}
     * method.
     *
     * @return
     */
    private Map<String, Field> getFields() {
        Map<String, Field> fields = new java.util.HashMap<>(Map.of(
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
                , "multiSelect", new FieldDataGen()
                        .type(MultiSelectField.class)
                        .name("multiSelect")
                        .velocityVarName("multiSelect")
                        .values("")
                        .defaultValue("")
                        .searchable(true).next()
                , "radio", new FieldDataGen()
                        .type(RadioField.class)
                        .name("radio")
                        .velocityVarName("radio")
                        .values("")
                        .defaultValue("")
                        .searchable(true).next()
        ));
        fields.putAll(Map.of(
                "select", new FieldDataGen()
                        .type(SelectField.class)
                        .name("select")
                        .velocityVarName("select")
                        .values("")
                        .defaultValue("")
                        .searchable(true).next(),
                "tag", new FieldDataGen()
                        .type(TagField.class)
                        .name("tag")
                        .velocityVarName("tag")
                        .values("")
                        .defaultValue("")
                        .searchable(true).next(),
                "textArea", new FieldDataGen()
                        .type(TextAreaField.class)
                        .name("textArea")
                        .velocityVarName("textArea")
                        .values("")
                        .defaultValue("")
                        .searchable(true).next(),
                "time", new FieldDataGen()
                        .type(TimeField.class)
                        .name("time")
                        .velocityVarName("time")
                        .values("")
                        .defaultValue("")
                        .searchable(true).next(),
                "wysiwyg", new FieldDataGen()
                        .type(WysiwygField.class)
                        .name("wysiwyg")
                        .velocityVarName("wysiwyg")
                        .values("")
                        .defaultValue("")
                        .searchable(true).next()));
        return fields;
    }

    @Test
    public void testCategoryField() throws JsonProcessingException, DotDataException, DotSecurityException {
        final Category testCategoryOne = new CategoryDataGen()
                .setCategoryName("rain forest")
                .setKey("rain_forest")
                .setCategoryVelocityVarName("rain_forest")
                .nextPersisted();
        final Category testCategoryTwo = new CategoryDataGen()
                .setCategoryName("sloths")
                .setKey("sloths")
                .setCategoryVelocityVarName("sloths")
                .nextPersisted();
        final ContentType testContentType = this.createTestContentType(TEST_CONTENT_TYPE_NAME, TEST_CONTENT_TYPE_NAME, List.of(new FieldDataGen()
                .type(CategoryField.class)
                .name("category")
                .velocityVarName("category")
                .values("")
                .defaultValue("")
                .searchable(true).next()));
        try {
            String jsonBody = "{\n" +
                    "    \"searchableFieldsByContentType\": {\n" +
                    "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                    "            \"category\": \"" + testCategoryOne.getCategoryId() + ", " + testCategoryTwo.getCategoryId() + "\"\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";
            String expectedQuery = ("+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +(SSS.category:" + testCategoryOne.getKey() + " SSS.category:" + testCategoryTwo.getKey() + ")")
                    .replaceAll("SSS", TEST_CONTENT_TYPE_NAME);

            ContentSearchForm contentSearchForm = jsonMapper.readValue(jsonBody, ContentSearchForm.class);
            LuceneQueryBuilder luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, adminUser);
            String luceneQuery = luceneQueryBuilder.build();

            assertNotNull("Generated query using Velocity Var Name cannot be null", luceneQuery);
            assertFalse("Generated query using Velocity Var Name cannot be an empty String", luceneQuery.isEmpty());
            assertEquals("The generated query using Velocity Var Name is different than expected", expectedQuery, luceneQuery);
        } finally {
            ContentTypeDataGen.remove(testContentType);
            CategoryDataGen.delete(testCategoryOne);
            CategoryDataGen.delete(testCategoryTwo);
        }
    }

    @Test
    public void testRelationshipsFieldWithDifferentContentTypes() throws DotDataException, DotSecurityException, JsonProcessingException {
        ContentType parentCt = null;
        ContentType childCt = null;
        try {
            parentCt = this.createTestContentType(TEST_CONTENT_TYPE_NAME, TEST_CONTENT_TYPE_NAME,
                    List.of(FieldBuilder
                            .builder(TextField.class)
                            .name("title")
                            .variable("title")
                            .indexed(true)
                            .searchable(true).build()));

            childCt = this.createTestContentType(TEST_CHILD_CONTENT_TYPE_NAME, TEST_CHILD_CONTENT_TYPE_NAME,
                    List.of(FieldBuilder
                            .builder(TextField.class)
                            .name("title")
                            .variable("title")
                            .indexed(true)
                            .searchable(true).build()));

            final Field field = FieldBuilder.builder(RelationshipField.class)
                    .name("relationship")
                    .contentTypeId(parentCt.id())
                    .values(String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                    .relationType(childCt.variable())
                    .required(false)
                    .build();
            final Field fieldSaved = APILocator.getContentTypeFieldAPI().save(field, adminUser);
            final Relationship relationship = APILocator.getRelationshipAPI().getRelationshipFromField(fieldSaved, adminUser);

            ContentletDataGen contentletDataGen = new ContentletDataGen(parentCt.id())
                    .languageId(1)
                    .host(defaultSite)
                    .setProperty("hostfolder", defaultSite)
                    .setProperty("title", "Parent content")
                    .setPolicy(IndexPolicy.WAIT_FOR);
            Contentlet parentTestContent = contentletDataGen.nextPersisted();
            ContentletDataGen.publish(parentTestContent);

            contentletDataGen = new ContentletDataGen(childCt.id())
                    .languageId(1)
                    .host(defaultSite)
                    .setProperty("hostfolder", defaultSite)
                    .setProperty("title", "Child content")
                    .setPolicy(IndexPolicy.WAIT_FOR);
            final Contentlet childTestContent = contentletDataGen.nextPersisted();
            ContentletDataGen.publish(childTestContent);

            parentTestContent = ContentletDataGen.checkout(parentTestContent);
            parentTestContent.setProperty(relationship.getChildRelationName(), Collections.singletonList(childTestContent));
            ContentletDataGen.checkin(parentTestContent);

            assertRelatedContents(relationship, parentTestContent, childTestContent);

            String jsonBody = "{\n" +
                    "    \"searchableFieldsByContentType\": {\n" +
                    "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                    "            \"relationship\": \"" + childTestContent.getIdentifier() + "\"" +
                    "        }\n" +
                    "    }\n" +
                    "}";
            String expectedQuery = ("+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +identifier:(" + parentTestContent.getIdentifier() + ")")
                    .replaceAll("SSS", parentCt.variable());

            ContentSearchForm contentSearchForm = jsonMapper.readValue(jsonBody, ContentSearchForm.class);
            LuceneQueryBuilder luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, adminUser);
            String luceneQuery = luceneQueryBuilder.build();

            assertNotNull("Generated query using Velocity Var Name cannot be null", luceneQuery);
            assertFalse("Generated query using Velocity Var Name cannot be an empty String", luceneQuery.isEmpty());
            assertEquals("The generated query using Velocity Var Name is different than expected", expectedQuery, luceneQuery);
        } finally {
            ContentTypeDataGen.remove(parentCt);
            ContentTypeDataGen.remove(childCt);
        }
    }

    @Test
    public void testRelationshipsFieldSelfRelated() throws DotDataException, DotSecurityException, JsonProcessingException {
        ContentType parentCt = null;
        try {
            parentCt = this.createTestContentType(TEST_CONTENT_TYPE_NAME, TEST_CONTENT_TYPE_NAME,
                    List.of(FieldBuilder
                            .builder(TextField.class)
                            .name("title")
                            .variable("title")
                            .indexed(true)
                            .searchable(true).build()));

            final Field field = FieldBuilder.builder(RelationshipField.class)
                    .name("relationship")
                    .contentTypeId(parentCt.id())
                    .values(String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                    .relationType(parentCt.variable())
                    .required(false)
                    .build();
            final Field fieldSaved = APILocator.getContentTypeFieldAPI().save(field, adminUser);
            final Relationship relationship = APILocator.getRelationshipAPI().getRelationshipFromField(fieldSaved, adminUser);

            ContentletDataGen contentletDataGen = new ContentletDataGen(parentCt.id())
                    .languageId(1)
                    .host(defaultSite)
                    .setProperty("hostfolder", defaultSite)
                    .setProperty("title", "Parent content")
                    .setPolicy(IndexPolicy.WAIT_FOR);
            Contentlet parentTestContent = contentletDataGen.nextPersisted();
            ContentletDataGen.publish(parentTestContent);

            contentletDataGen = new ContentletDataGen(parentCt.id())
                    .languageId(1)
                    .host(defaultSite)
                    .setProperty("hostfolder", defaultSite)
                    .setProperty("title", "Child content")
                    .setPolicy(IndexPolicy.WAIT_FOR);
            final Contentlet childTestContent = contentletDataGen.nextPersisted();
            ContentletDataGen.publish(childTestContent);

            parentTestContent = ContentletDataGen.checkout(parentTestContent);
            parentTestContent.setProperty(relationship.getChildRelationName(), Collections.singletonList(childTestContent));
            ContentletDataGen.checkin(parentTestContent);

            assertRelatedContents(relationship, parentTestContent, childTestContent);

            String jsonBody = "{\n" +
                    "    \"searchableFieldsByContentType\": {\n" +
                    "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                    "            \"relationship\": \"" + childTestContent.getIdentifier() + "\"" +
                    "        }\n" +
                    "    }\n" +
                    "}";
            String expectedQuery = ("+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true +my_test_ct.relationship:" + childTestContent.getIdentifier())
                    .replaceAll("SSS", parentCt.variable());

            ContentSearchForm contentSearchForm = jsonMapper.readValue(jsonBody, ContentSearchForm.class);
            LuceneQueryBuilder luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, adminUser);
            String luceneQuery = luceneQueryBuilder.build();

            assertNotNull("Generated query using Velocity Var Name cannot be null", luceneQuery);
            assertFalse("Generated query using Velocity Var Name cannot be an empty String", luceneQuery.isEmpty());
            assertEquals("The generated query using Velocity Var Name is different than expected", expectedQuery, luceneQuery);
        } finally {
            ContentTypeDataGen.remove(parentCt);
        }
    }

    //@Test
    public void testRelationshipsFieldOld() throws DotDataException, JsonProcessingException, DotSecurityException {
        /*final ContentType contentType =  new ContentTypeDataGen()
                .host(defaultSite)
                .nextPersisted();*/
        final ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen()
                .host(defaultSite)
                .name("childCT2")
                .velocityVarName("childCT2")
                .workflowId("d61a59e1-a49c-46f2-a929-db2b4bfa88b2");
        ContentType childCt = contentTypeDataGen.nextPersisted();

        final ContentType contentType = this.createTestContentType(TEST_CONTENT_TYPE_NAME, TEST_CHILD_CONTENT_TYPE_NAME, List.of(new FieldDataGen()
                .type(RelationshipField.class)
                .name("relationship")
                .velocityVarName("relationship")
                .relationType("childCT2")
                .values(String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                .defaultValue("")
                .indexed(true)
                .searchable(true).next()));

        final Relationship relationship = new FieldRelationshipDataGen()
                .child(childCt)
                .parent(contentType)
                .cardinality(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY)
                .nextPersisted();

        /*Contentlet contentletC = new ContentletDataGen(contentType)
                .host(defaultSite)
                .nextPersisted();*/

        try {
            Contentlet contentletA = new ContentletDataGen(childCt)
                    .host(defaultSite)
                    .setProperty("title", "Child 1")
                    .nextPersisted();

            Contentlet contentletB = new ContentletDataGen(contentType)
                    .host(defaultSite)
                    .setProperty("title", "Parent Content")
                    .setProperty(relationship.getChildRelationName(), Arrays.asList(contentletA))
                    .nextPersisted();

            contentletA = relateContent(relationship, contentletA, contentletB);
            //contentletB = relateContent(relationship, contentletB, contentletA);
            //contentletC = relateContent(relationship, contentletC, contentletA, contentletB);

            //contentletA.setProperty("relationship", Collections.singletonList(contentletB));

            //contentletA = ContentletDataGen.checkout(contentletA);
            //ContentletDataGen.checkin(contentletA);

            assertRelatedContents(relationship, contentletA, contentletB);
            //assertRelatedContents(relationship, contentletB, contentletA);

            String jsonBody = "{\n" +
                    "    \"searchableFieldsByContentType\": {\n" +
                    "        \"" + TEST_CONTENT_TYPE_NAME + "\": {\n" +
                    "            \"relationship\": \"" + contentletA.getIdentifier() + "\"" +
                    "        }\n" +
                    "    }\n" +
                    "}";
            String expectedQuery = ("+contentType:(SSS) +conHost:SYSTEM_HOST +variant:default +deleted:false +working:true ")
                    .replaceAll("SSS", contentType.variable());

            ContentSearchForm contentSearchForm = jsonMapper.readValue(jsonBody, ContentSearchForm.class);
            LuceneQueryBuilder luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, adminUser);
            String luceneQuery = luceneQueryBuilder.build();

            assertNotNull("Generated query using Velocity Var Name cannot be null", luceneQuery);
            assertFalse("Generated query using Velocity Var Name cannot be an empty String", luceneQuery.isEmpty());
            assertEquals("The generated query using Velocity Var Name is different than expected", expectedQuery, luceneQuery);
        } finally {
            ContentTypeDataGen.remove(contentType);
            ContentTypeDataGen.remove(childCt);
        }
    }

    private Contentlet relateContent(
            Relationship relationship,
            Contentlet parentContent,
            Contentlet... contentletChilds) {
        final Contentlet parentCheckout = ContentletDataGen.checkout(parentContent);
        parentCheckout.setProperty(relationship.getChildRelationName(), Arrays.asList(contentletChilds));
        return  ContentletDataGen.checkin(parentCheckout);
    }

    private void assertRelatedContents(Relationship relationship, Contentlet contentletParent,
                                       Contentlet... contentsRelated) throws DotDataException {
        final RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        final List<String> contentlets = relationshipAPI
                .dbRelatedContent(relationship, contentletParent, true)
                .stream()
                .map(contentlet -> contentlet.getIdentifier())
                .collect(Collectors.toList());

        assertEquals(contentsRelated.length, contentlets.size());

        for (Contentlet contentletRelated : contentsRelated) {
            assertTrue(contentlets.contains(contentletRelated.getIdentifier()));
        }
    }












/*
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
    }*/

    private ContentType createTestContentType(final String name, final String velocityVarName) {
        return this.createTestContentType(name, velocityVarName, null);
    }

    /*private ContentType createTestContentType(final Field field) {
        final ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen()
                .host(defaultSite)
                .name(TEST_CONTENT_TYPE_NAME)
                .velocityVarName(TEST_CONTENT_TYPE_NAME)
                .workflowId("d61a59e1-a49c-46f2-a929-db2b4bfa88b2");
        if (null != field) {
            contentTypeDataGen.field(field);
        }
        return contentTypeDataGen.nextPersisted();
    }*/
    private ContentType createTestContentType(final String name, final String velocityVarName, final List<Field> fields) {
        final ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen()
                .host(defaultSite)
                .name(name)
                .velocityVarName(velocityVarName)
                .workflowId("d61a59e1-a49c-46f2-a929-db2b4bfa88b2");
        if (UtilMethods.isSet(fields)) {
            contentTypeDataGen.fields(fields);
        }
        return contentTypeDataGen.nextPersisted();
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
