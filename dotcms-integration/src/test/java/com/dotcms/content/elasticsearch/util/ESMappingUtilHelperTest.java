package com.dotcms.content.elasticsearch.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.DotIndexException;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.content.elasticsearch.business.IndexType;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import com.tngtech.junit.dataprovider.format.DataProviderTestNameFormatter;
import io.vavr.control.Try;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Class used to validate ES mapping for fields is applied correctly considering fields types and dynamic mapping defined in `es-content-mapping.json`
 * @author nollymar
 */
@RunWith(DataProviderRunner.class)
public class ESMappingUtilHelperTest {

    private static User user;
    private static CategoryAPI categoryAPI;
    private static ContentletAPI contentletAPI;
    private static ContentletIndexAPI contentletIndexAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static ESMappingAPIImpl esMappingAPI;
    private static FieldAPI fieldAPI;
    private static RelationshipAPI relationshipAPI;
    private static Language language;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        user = APILocator.systemUser();
        categoryAPI = APILocator.getCategoryAPI();
        contentletAPI = APILocator.getContentletAPI();
        contentletIndexAPI = APILocator.getContentletIndexAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(user);
        esMappingAPI = new ESMappingAPIImpl();
        fieldAPI = APILocator.getContentTypeFieldAPI();
        relationshipAPI = APILocator.getRelationshipAPI();
        language = APILocator.getLanguageAPI().getDefaultLanguage();

        final ContentType currentCalendarEventType = Try.of(()->contentTypeAPI.find("calendarEvent")).getOrNull();
        if (null != currentCalendarEventType) {

            contentTypeAPI.delete(currentCalendarEventType);
        }
    }

    public static class PlusTestNameFormatter implements DataProviderTestNameFormatter {
        @Override
        public String format(Method testMethod, int invocationIndex, List<Object> arguments) {
            return String.format("Test Name: %s. Mapping Name: %s", testMethod.getName(), arguments.get(0));
        }
    }

    @DataProvider(formatter = PlusTestNameFormatter.class)
    public static Object[][] dataProviderValidateEventMapping(){
        return new Object[][] {
                {"template_1", new String[]{"host.hostname_dotraw"}, "keyword"},
                {"textmapping", new String[]{"host.hostname"}, "keyword"},
                {"strings_as_dates", new String[]{"calendarevent.originalstartdate",
                        "calendarevent.recurrencestart", "calendarevent.recurrenceend"}, "date"},
                {"permissions", new String[]{"permissions"}, "text"}
        };
    }

    @DataProvider(formatter = PlusTestNameFormatter.class)
    public static Object[][] dataProviderValidateFileAssetMapping(){
        return new Object[][] {
                {"longmapping", new String[]{ "metadata.height", "metadata.width"}, "long"},
                {"keywordmapping", new String[]{"metadata.contenttype"}, "keyword"}
        };
    }

    @DataProvider(formatter = PlusTestNameFormatter.class)
    public static Object[][] dataProviderValidateNewsLikeMapping() {
        return new Object[][]{
                {"geomapping", new String[]{"mylatlon"}, "geo_point"},
                {"geomapping_2", new String[]{"latlong"}, "geo_point"},
                {"keywordmapping", new String[]{"categories", "tags", "conhost", "conhostname",
                        "wfstep", "structurename", "contenttype", "parentpath", "path",
                        "moduser", "owner"}, "keyword"}
        };
    }

    /**
     * Data entity used to verify that the ES mapping is applied correctly and considers exclusions in the `es-content-mapping` file
     * For each scenario we define:
     * 1) TestCase name
     * 2) A content type where the mapping will be applied
     * 3) Field type and Datatype
     * 4) Field(s) variable name(s)
     * 5) Expected ES mapping
     * 6) Boolean indicating if the field is unique
     */
    private class AddMappingTestCase{
        String testCaseName;
        ContentType contentType;
        Class fieldType;
        DataTypes type;
        String[] fieldsVarNames;
        String expectedResult;
        boolean isUnique;

        public AddMappingTestCase(final String testCaseName, final ContentType contentType,
                final Class fieldType, final DataTypes type, final String[] fieldsVarNames,
                final String expectedResult, final boolean isUnique){
            this.testCaseName   = testCaseName;
            this.contentType    = contentType;
            this.fieldType      = fieldType;
            this.type           = type;
            this.fieldsVarNames = fieldsVarNames;
            this.expectedResult = expectedResult;
            this.isUnique       = isUnique;
        }
    }

    private List<AddMappingTestCase> getAddMappingForFieldsTestCases() {
        return CollectionsUtils.list(
                getStringsAsDatesMappingTestCase(),
                getStringsAsDatesTimeMappingTestCase(),
                getDatesAsTextMappingTestCase(),
                getKeywordMappingTestCase(),
                getGeoMappingTestCase(),
                getPermissionsMappingTestCase(),
                getRadioAsBooleanMappingTestCase(),
                getRadioAsFloatMappingTestCase(),
                getRadioAsIntegerMappingTestCase(),
                getSelectAsBooleanMappingTestCase(),
                getSelectAsFloatMappingTestCase(),
                getSelectAsIntegerMappingTestCase(),
                getTextAsFloatMappingTestCase(),
                getTextAsIntegerMappingTestCase(),
                getTagsMappingTestCase(),
                getUniqueFieldMappingTestCase()
        );
    }

    private AddMappingTestCase getStringsAsDatesMappingTestCase(){
        return new AddMappingTestCase("strings_as_dates", new ContentTypeDataGen().nextPersisted(),
                DateField.class, DataTypes.DATE,
                new String[]{"originalstartdate", "recurrencestart", "recurrenceend"}, "date",
                false);
    }

    private AddMappingTestCase getStringsAsDatesTimeMappingTestCase(){
        return new AddMappingTestCase("strings_as_date_times",
                new ContentTypeDataGen().nextPersisted(), DateTimeField.class, DataTypes.DATE,
                new String[]{"originalstartdate", "recurrencestart", "recurrenceend"}, "date",
                false);
    }

    private AddMappingTestCase getDatesAsTextMappingTestCase(){
        return new AddMappingTestCase("dates_as_text",
                new ContentTypeDataGen().nextPersisted(), TextField.class, DataTypes.TEXT,
                new String[]{"originalstartdate", "recurrencestart", "recurrenceend"}, "text",
                false);
    }

    private AddMappingTestCase getKeywordMappingTestCase(){
        return new AddMappingTestCase("keywordmapping",
                new ContentTypeDataGen().nextPersisted(), TextField.class, DataTypes.TEXT,
                new String[] {"categories", "tags", "conhost",
                        "wfstep", "structurename", "contenttype", "parentpath",
                        "path", "urlmap", "moduser", "owner"},  "text", false);
    }

    private AddMappingTestCase getGeoMappingTestCase(){
        return new AddMappingTestCase("geomapping",
                new ContentTypeDataGen().nextPersisted(), TextField.class, DataTypes.TEXT,
                new String[] {"mylatlong", "mylatlon"},  null, false);
    }

    private AddMappingTestCase getPermissionsMappingTestCase(){
        return new AddMappingTestCase("permissions",
                new ContentTypeDataGen().nextPersisted(), TextField.class, DataTypes.TEXT,
                new String[] {"permissions"},  "text", false);
    }

    private AddMappingTestCase getRadioAsBooleanMappingTestCase(){
        return new AddMappingTestCase("radio_as_boolean",
                new ContentTypeDataGen().nextPersisted(), RadioField.class, DataTypes.BOOL,
                new String[] {"MyRadioAsBoolean"},  "boolean", false);
    }

    private AddMappingTestCase getRadioAsFloatMappingTestCase(){
        return new AddMappingTestCase("radio_as_float",
                new ContentTypeDataGen().nextPersisted(), RadioField.class, DataTypes.FLOAT,
                new String[] {"MyRadioAsFloat"},  "double", false);
    }

    private AddMappingTestCase getRadioAsIntegerMappingTestCase(){
        return new AddMappingTestCase("radio_as_integer",
                new ContentTypeDataGen().nextPersisted(), RadioField.class, DataTypes.INTEGER,
                new String[] {"MyRadioAsInteger"},  "long", false);
    }

    private AddMappingTestCase getSelectAsBooleanMappingTestCase(){
        return new AddMappingTestCase("select_as_boolean",
                new ContentTypeDataGen().nextPersisted(), SelectField.class, DataTypes.BOOL,
                new String[] {"MySelectAsBoolean"},  "boolean", false);
    }

    private AddMappingTestCase getSelectAsFloatMappingTestCase(){
        return new AddMappingTestCase("select_as_float",
                new ContentTypeDataGen().nextPersisted(), SelectField.class, DataTypes.FLOAT,
                new String[] {"MySelectAsFloat"},  "double", false);
    }

    private AddMappingTestCase getSelectAsIntegerMappingTestCase(){
        return new AddMappingTestCase("select_as_integer",
                new ContentTypeDataGen().nextPersisted(), SelectField.class, DataTypes.INTEGER,
                new String[] {"MySelectAsInteger"},  "long", false);
    }

    private AddMappingTestCase getTextAsFloatMappingTestCase(){
        return new AddMappingTestCase("text_as_float",
                new ContentTypeDataGen().nextPersisted(), TextField.class, DataTypes.FLOAT,
                new String[] {"MyTextAsFloat"},  "double", false);
    }

    private AddMappingTestCase getTextAsIntegerMappingTestCase(){
        return new AddMappingTestCase("text_as_integer",
                new ContentTypeDataGen().nextPersisted(), TextField.class, DataTypes.INTEGER,
                new String[] {"MyTextAsInteger"},  "long", false);
    }

    private AddMappingTestCase getTagsMappingTestCase(){
        return new AddMappingTestCase("tags",
                new ContentTypeDataGen().nextPersisted(), TagField.class, DataTypes.TEXT,
                new String[] {"MyTagField"},  "keyword", false);
    }

    private AddMappingTestCase getUniqueFieldMappingTestCase(){
        return new AddMappingTestCase("uniqueField",
                new ContentTypeDataGen().nextPersisted(), TextField.class, DataTypes.TEXT,
                new String[] {"MyUniqueField"},  "keyword", true);
    }

    /**
     * <b>Method to test:</b> Internally, it tests the method ESMappingUtilHelper.addMappingForFieldIfNeeded<p></p>
     * <b>Test Case:</b> Given a field or an array of fields, the right ES mapping should be set, considering exclusions in the `es-content-mapping` file<p></p>
     * <b>Expected Results:</b> Each test case should match the `expectedResult` value, which is a string with the correct datatype in ES
     * @throws IOException
     * @throws DotIndexException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testAddMappingForFields()
            throws IOException, DotIndexException, DotSecurityException, DotDataException {

        final List<ESMappingUtilHelperTest.AddMappingTestCase> testCases = getAddMappingForFieldsTestCases();
        String workingIndex = null;
        //Build the index name
        String timestamp = String.valueOf(new Date().getTime());
        try {
            //Adding fields
            for (ESMappingUtilHelperTest.AddMappingTestCase testCase:testCases) {
                for (final String field : testCase.fieldsVarNames) {
                    final Field newField = FieldBuilder.builder(testCase.fieldType)
                            .name(field).variable(field).dataType(testCase.type)
                            .contentTypeId(testCase.contentType.id())
                            .indexed(true).unique(testCase.isUnique).build();
                    fieldAPI.save(newField, user);
                }
            }

            workingIndex = new ESIndexAPI().getNameWithClusterIDPrefix(
                    IndexType.WORKING.getPrefix() + "_" + timestamp);

            Config.setProperty("CREATE_TEXT_INDEX_FIELD_FOR_NON_TEXT_FIELDS", true);
            //Create a working index
            boolean result = contentletIndexAPI.createContentIndex(workingIndex);
            //Validate
            assertTrue(result);

            for (ESMappingUtilHelperTest.AddMappingTestCase testCase:testCases) {
                for (final String field : testCase.fieldsVarNames) {
                    final String failureMessage = "Assert failed for test case: " + testCase.testCaseName + " and field: " + field;
                    if (testCase.expectedResult == null) {
                        assertFalse(failureMessage,
                                UtilMethods.isSet(esMappingAPI.getFieldMappingAsMap(workingIndex,
                                        (testCase.contentType.variable() + StringPool.PERIOD
                                                + field).toLowerCase())));
                    } else {
                        Map<String, String> mapping = (Map<String, String>) esMappingAPI
                                .getFieldMappingAsMap(workingIndex,
                                        (testCase.contentType.variable() + StringPool.PERIOD + field)
                                                .toLowerCase()).get(field.toLowerCase());
                        assertTrue(failureMessage, UtilMethods.isSet(mapping.get("type")));
                        assertEquals(failureMessage, testCase.expectedResult, mapping.get("type"));

                        //validate _dotraw fields
                        mapping = (Map<String, String>) esMappingAPI
                                .getFieldMappingAsMap(workingIndex,
                                        (testCase.contentType.variable() + StringPool.PERIOD + field)
                                                .toLowerCase() + "_dotraw")
                                .get(field.toLowerCase() + "_dotraw");
                        assertTrue(failureMessage, UtilMethods.isSet(mapping.get("type")));
                        assertEquals(failureMessage, "keyword", mapping.get("type"));

                        //validate _sha256 mapping for unique fields
                        if (testCase.isUnique) {
                            mapping = (Map<String, String>) esMappingAPI
                                    .getFieldMappingAsMap(workingIndex,
                                            (testCase.contentType.variable() + StringPool.PERIOD + field)
                                                    .toLowerCase() + ESUtils.SHA_256)
                                    .get(field.toLowerCase() + ESUtils.SHA_256);
                            assertTrue(failureMessage, UtilMethods.isSet(mapping.get("type")));
                            assertEquals(failureMessage, "keyword", mapping.get("type"));
                        }

                        //validate _text fields
                        mapping = (Map<String, String>) esMappingAPI
                                .getFieldMappingAsMap(workingIndex,
                                        (testCase.contentType.variable() + StringPool.PERIOD + field)
                                                .toLowerCase() + ESMappingAPIImpl.TEXT)
                                .get(field.toLowerCase() + ESMappingAPIImpl.TEXT);
                        assertTrue(failureMessage, UtilMethods.isSet(mapping.get("type")));
                        assertEquals(failureMessage, "text", mapping.get("type"));
                    }
                }
            }

        } finally {
            Config.setProperty("CREATE_TEXT_INDEX_FIELD_FOR_NON_TEXT_FIELDS", false);
            if (workingIndex != null) {
                contentletIndexAPI.delete(workingIndex);
            }
            for (ESMappingUtilHelperTest.AddMappingTestCase testCase:testCases) {
                if (testCase.contentType != null) {
                    contentTypeAPI.delete(testCase.contentType);
                }
            }
        }
    }

    /**
     * <b>Test Case:</b> This test verifies that fields of type {@link DateField} or {@link DateTimeField} are always mapped as dates in ES<p></p>
     * <b>Expected Results:</b> All fields should be mapped as dates with the right format
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     * @throws DotIndexException
     */
    @Test
    public void testMappingForDateFields()
            throws DotSecurityException, DotDataException, IOException, DotIndexException {
        ContentType contentType = null;
        String workingIndex = null;
        String oldWorkingIndex = contentletIndexAPI.getActiveIndexName(IndexType.WORKING.getPrefix());
        try {
            contentType = new ContentTypeDataGen().nextPersisted();

            Field dateField = FieldBuilder.builder(DateField.class).name("myDateField")
                    .contentTypeId(contentType.id()).indexed(true).build();
            Field dateTimeField = FieldBuilder.builder(DateTimeField.class).name("myDateTimeField")
                    .contentTypeId(contentType.id()).indexed(true).build();

            dateField = fieldAPI.save(dateField, user);
            dateTimeField = fieldAPI.save(dateTimeField, user);

            workingIndex = IndexType.WORKING.getPrefix() + "_" + System.currentTimeMillis();

            //Create a working index
            boolean result = contentletIndexAPI.createContentIndex(workingIndex);
            assertTrue(result);

            contentletIndexAPI.activateIndex(workingIndex);

            assertEquals(workingIndex, contentletIndexAPI.getActiveIndexName( IndexType.WORKING.getPrefix()));

            new ContentletDataGen(contentType.id())
                    .setProperty("myDateField", new Date())
                    .setProperty("myDateTimeField", new Date()).nextPersisted();

            final String formatExpected = "yyyy-MM-dd't'HH:mm:ssZ||yyyy-MM-dd't'HH:mm:ss||MMM d, yyyy h:mm:ss a||yyyy-MM-dd HH:mm:ss||yyyy-MM-dd HH:mm:ss.SSS||yyyy-MM-dd||epoch_millis";

            //verifies mapping type for common text fields
            Map<String, String> mapping = (Map<String, String>) esMappingAPI
                    .getFieldMappingAsMap(APILocator.getIndiciesAPI().loadLegacyIndices().getWorking(),
                            contentType.variable().toLowerCase() + "." + dateField.variable()
                                    .toLowerCase()).entrySet()
                    .iterator()
                    .next().getValue();
            assertTrue(UtilMethods.isSet(mapping.get("type")));
            assertEquals("date", mapping.get("type"));
            assertEquals(formatExpected, mapping.get("format"));

            mapping = (Map<String, String>) esMappingAPI
                    .getFieldMappingAsMap(APILocator.getIndiciesAPI().loadLegacyIndices().getWorking(),
                            contentType.variable().toLowerCase() + "." + dateTimeField.variable()
                                    .toLowerCase()).entrySet()
                    .iterator()
                    .next().getValue();
            assertTrue(UtilMethods.isSet(mapping.get("type")));
            assertEquals("date", mapping.get("type"));
            assertEquals(formatExpected, mapping.get("format"));

            mapping = (Map<String, String>) esMappingAPI
                    .getFieldMappingAsMap(APILocator.getIndiciesAPI().loadLegacyIndices().getWorking(),
                            "moddate").entrySet().iterator().next().getValue();
            assertTrue(UtilMethods.isSet(mapping.get("type")));
            assertEquals("date", mapping.get("type"));
            assertEquals(formatExpected, mapping.get("format"));
        } finally {
            if (contentType != null) {
                contentTypeAPI.delete(contentType);
            }

            if (workingIndex != null) {
                contentletIndexAPI.deactivateIndex(workingIndex);
                contentletIndexAPI.activateIndex(oldWorkingIndex);
                contentletIndexAPI.delete(workingIndex);
            }
        }
    }


    /**
     * <b>Method to test:</b> Internally, it tests the methods ESMappingUtilHelper.addCustomMappingForRelationships and </p>
     * ESMappingUtilHelper.addCustomMappingFromFieldVariables<p></p>
     * </p><b>Test Case:</b> This test creates an index with two custom mapped fields: a text field and a relationship field.
     * Additionally, it creates a legacy relationship without a custom mapping in order to verify that
     * relationships in general are mapped as keywords by default, unless a custom mapping is defined for a specific case <p></p>
     * <b>Expected Results:</b> Custom mappings should take precedence always if exists. In case of relationships, </p>
     * they should be mapped as keywords, unless a custom mapping is defined
     * @throws Exception
     */
    @Test
    public void testCreateContentIndexWithCustomMappings() throws Exception {

        String workingIndex = null;
        ContentType parentContentType = null;
        ContentType childContentType = null;
        try {
            parentContentType = new ContentTypeDataGen().nextPersisted();
            childContentType = new ContentTypeDataGen().nextPersisted();

            //Adding fields
            Field ageField = FieldBuilder.builder(TextField.class)
                    .name("age").contentTypeId(parentContentType.id()).indexed(true).build();
            ageField = fieldAPI.save(ageField, user);

            Field nonIndexedField = FieldBuilder.builder(TextField.class)
                    .name("nonIndexedField").contentTypeId(parentContentType.id()).indexed(false).build();
            nonIndexedField = fieldAPI.save(nonIndexedField, user);

            Field relationshipField = FieldBuilder.builder(RelationshipField.class)
                    .name("relationshipField")
                    .contentTypeId(parentContentType.id())
                    .values(String.valueOf(RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()))
                    .relationType(parentContentType.variable()).build();

            relationshipField = fieldAPI.save(relationshipField, user);

            //Create legacy relationship
            final Relationship legacyRelationship = createLegacyRelationship(parentContentType,
                    childContentType);

            //Adding field variables
            final FieldVariable ageVariable = ImmutableFieldVariable.builder()
                    .fieldId(ageField.inode())
                    .name("ageMapping").key(FieldVariable.ES_CUSTOM_MAPPING_KEY).value("{\n"
                            + "                \"type\": \"long\"\n"
                            + "              }").userId(user.getUserId())
                    .build();

            fieldAPI.save(ageVariable, user);

            FieldVariable relVariable = ImmutableFieldVariable.builder()
                    .fieldId(relationshipField.inode())
                    .name("relMapping").key(FieldVariable.ES_CUSTOM_MAPPING_KEY).value("{\n"
                            + "                \"type\": \"text\"\n"
                            + "              }").userId(user.getUserId())
                    .build();

            relVariable = fieldAPI.save(relVariable, user);

            final Field updatedField = fieldAPI.find(relVariable.fieldId());

            //Verify ageField is updated as System Indexed
            assertTrue(updatedField.indexed());

            //Build the index name
            String timestamp = String.valueOf(new Date().getTime());
            workingIndex = new ESIndexAPI().getNameWithClusterIDPrefix(IndexType.WORKING.getPrefix() + "_" + timestamp);

            //Create a working index
            boolean result = contentletIndexAPI.createContentIndex(workingIndex);
            //Validate
            assertTrue(result);

            //verify mapping
            final String mapping = esMappingAPI.getMapping(workingIndex);

            //parse json mapping and validate
            assertNotNull(mapping);

            final JSONObject propertiesJSON = (JSONObject) (new JSONObject(mapping)).get("properties");
            final JSONObject contentTypeJSON = (JSONObject) ((JSONObject) propertiesJSON
                    .get(parentContentType.variable().toLowerCase())).get("properties");

            //validate no mapping is added for non-indexed fields
            assertFalse(contentTypeJSON.has(nonIndexedField.variable().toLowerCase()));

            //validate age mapping results
            final Map ageMapping = ((JSONObject) contentTypeJSON
                    .get(ageField.variable().toLowerCase())).getAsMap();
            assertNotNull(ageMapping);
            assertEquals(1, ageMapping.size());
            assertEquals("long", ageMapping.get("type"));

            //validate relationship field mapping results
            final Map relationshipMapping = ((JSONObject) contentTypeJSON
                    .get(relationshipField.variable().toLowerCase())).getAsMap();
            assertNotNull(relationshipMapping);
            assertEquals(1, relationshipMapping.size());
            assertEquals("text", relationshipMapping.get("type"));

            //validate legacy relationship
            final Map legacyRelationshipMapping = ((JSONObject) propertiesJSON
                    .get(legacyRelationship.getRelationTypeValue().toLowerCase())).getAsMap();
            assertNotNull(ageMapping);
            assertEquals(2, legacyRelationshipMapping.size());
            assertEquals("keyword", legacyRelationshipMapping.get("type"));
            assertEquals(8191, legacyRelationshipMapping.get("ignore_above"));

        } finally {
            if (workingIndex != null) {
                contentletIndexAPI.delete(workingIndex);
            }

            if (parentContentType != null && parentContentType.inode() != null) {
                ContentTypeDataGen.remove(parentContentType);
            }

            if (childContentType != null && childContentType.inode() != null) {
                ContentTypeDataGen.remove(childContentType);
            }
        }
    }

    /**
     * <b>Test Case:</b> Validates dynamic mappings defined in the `es-content-mapping` file are applied correctly<p></p>
     * <b>Expected Results:</b> Each test case should match the `expectedResult` value, which is a string with the correct datatype in ES
     * @param testCase
     * @param fields
     * @param expectedResult
     * @throws DotDataException
     * @throws IOException
     * @throws DotSecurityException
     */
    @UseDataProvider
    @Test
    public void testValidateNewsLikeMapping(final String testCase, final String[] fields, final String expectedResult)
            throws DotDataException, IOException, DotSecurityException {
        final ContentType newsContentType = TestDataUtils.getNewsLikeContentType();
        fieldAPI.save(FieldBuilder.builder(TextField.class).contentTypeId(newsContentType.id())
                .name("mylatlon").variable("mylatlon").indexed(true).build(), user);


        Contentlet newsContent = null;
        try {
            final String categoryName = "myCategory" + System.currentTimeMillis();
            Category category = new Category();
            category.setCategoryName(categoryName);
            category.setKey(categoryName);
            category.setCategoryVelocityVarName(categoryName);
            category.setSortOrder(2);
            category.setKeywords(null);
            categoryAPI.save(null, category, user, false);
            newsContent = new ContentletDataGen(newsContentType.id()).languageId(language.getId())
                    .host(APILocator.getHostAPI()
                            .findDefaultHost(APILocator.systemUser(), false))
                    .setProperty("title", "newsContent Title" + System.currentTimeMillis())
                    .setProperty("urlTitle", "myUrlTitle")
                    .setProperty("urlMap", "myUrlMap")
                    .setProperty("byline", "byline")
                    .setProperty("sysPublishDate", new Date())
                    .setProperty("story", "newsStory")
                    .setProperty("latlong", "[90, -90]")
                    .setProperty("mylatlon", "[90, -90]")
                    .setProperty("tags", "test").next();

            newsContent = contentletAPI.checkin(newsContent, CollectionsUtils.list(category), null, user, false);
            validateMappingForFields(testCase, newsContentType.variable().toLowerCase(), fields, expectedResult);
        }finally{
            if (newsContent != null) {
                ContentletDataGen.destroy(newsContent);
            }
        }

    }

    /**
     * <b>Test Case:</b> Validates dynamic mappings defined in the `es-content-mapping` file are applied correctly<p></p>
     * <b>Expected Results:</b> Each test case should match the `expectedResult` value, which is a string with the correct datatype in ES
     * @param testCase
     * @param fields
     * @param expectedResult
     * @throws DotDataException
     * @throws IOException
     */
    @UseDataProvider
    @Test
    public void testValidateFileAssetMapping(final String testCase, final String[] fields, final String expectedResult)
            throws DotDataException, IOException {
        Contentlet fileAsset = null;

        try{
            fileAsset = TestDataUtils.getFileAssetContent(true, language.getId());
            validateMappingForFields(testCase, null, fields, expectedResult);
        } finally {
            if (fileAsset != null) {
                ContentletDataGen.destroy(fileAsset);
            }
        }
    }

    /**
     * <b>Test Case:</b> Validates dynamic mappings defined in the `es-content-mapping` file are applied correctly<p></p>
     * <b>Expected Results:</b> Each test case should match the `expectedResult` value, which is a string with the correct datatype in ES
     * @param testCase
     * @param fields
     * @param expectedResult
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @UseDataProvider
    @Test
    public void testValidateEventMapping(final String testCase, final String[] fields, final String expectedResult)
            throws DotDataException, DotSecurityException, IOException {

        ContentType eventContentType;
        Contentlet event = null;
        try {
            try {
                eventContentType = contentTypeAPI.find("calendarEvent");

            } catch(NotFoundInDbException e){
                final List<Field> eventFields = new ArrayList<>();
                eventFields
                        .add(new FieldDataGen().type(TextField.class).name("Title").velocityVarName("title").indexed(true)
                                .next());
                eventFields.add(new FieldDataGen().type(DateField.class).name("StartDate").defaultValue(null)
                        .velocityVarName("startDate").indexed(true).next());
                eventFields.add(new FieldDataGen().type(DateField.class).name("EndDate").defaultValue(null)
                        .velocityVarName("endDate").indexed(true).next());
                eventFields.add(new FieldDataGen().type(DateField.class).name("OriginalStartDate")
                        .defaultValue(null).defaultValue(null).velocityVarName("originalStartDate")
                        .indexed(true).next());
                eventFields.add(new FieldDataGen().type(DateField.class).name("RecurrenceStart")
                        .defaultValue(null).velocityVarName("recurrenceStart").indexed(true).next());
                eventFields.add(new FieldDataGen().type(DateField.class).name("RecurrenceEnd")
                        .defaultValue(null).velocityVarName("recurrenceEnd").indexed(true).next());
                eventContentType = new ContentTypeDataGen().velocityVarName("calendarEvent").fields(eventFields).nextPersisted();
            }
            event = new ContentletDataGen(eventContentType.id())
                    .setProperty("title", "MyEvent" + System.currentTimeMillis())
                    .setProperty("startDate", new Date())
                    .setProperty("endDate", new Date())
                    .setProperty("originalStartDate", new Date())
                    .setProperty("recurrenceStart", new Date())
                    .setProperty("recurrenceEnd", new Date()).nextPersisted();

            validateMappingForFields(testCase, null, fields, expectedResult);

            //verifies analyzer for common text fields
            final Map<String, String> mapping = (Map<String, String>) esMappingAPI
                    .getFieldMappingAsMap(APILocator.getIndiciesAPI().loadLegacyIndices().getWorking(),
                            "calendarevent.title").entrySet().iterator()
                    .next().getValue();
            assertTrue(UtilMethods.isSet(mapping.get("type")));
            assertEquals("text", mapping.get("type"));

            assertTrue(UtilMethods.isSet(mapping.get("analyzer")));
            assertEquals("my_analyzer", mapping.get("analyzer"));
        }finally {
            if (event != null){
                ContentletDataGen.destroy(event);
            }
        }
    }

    /**
     * Validates asserts for tests: testValidateNewsLikeMapping, testValidateFileAssetMapping and testValidateEventMapping
     * @param testCase
     * @param contentTypeVarName
     * @param fields
     * @param expectedResult
     * @throws IOException
     * @throws DotDataException
     */
    private void validateMappingForFields(final String testCase, final String contentTypeVarName,  final String[] fields, final String expectedResult)
            throws IOException, DotDataException {
        Map<String, String> mapping;

        for (final String field : fields) {
            Logger.info(this,
                    "Validating mapping for case: " + testCase + ". Field Name: " + field);
            mapping = (Map<String, String>) esMappingAPI
                    .getFieldMappingAsMap(APILocator.getIndiciesAPI().loadLegacyIndices().getWorking(),
                            expectedResult.equals("geo_point") ? contentTypeVarName
                                    + StringPool.PERIOD + field : field).entrySet().iterator()
                    .next().getValue();
            assertTrue(UtilMethods.isSet(mapping.get("type")));
            assertEquals(expectedResult, mapping.get("type"));
            Logger.info(this,
                    "Case: " + testCase + ". Field Name: " + field + " validated successfully");
        }
    }


    /**
     * Creates a new Legacy Relationship object
     * @param parentContentType
     * @param childContentType
     * @return
     */
    private Relationship createLegacyRelationship(final ContentType parentContentType,
            final ContentType childContentType) {
        final String relationTypeValue = parentContentType.name() + "-" + childContentType.name();

        Relationship relationship;
        relationship = relationshipAPI.byTypeValue(relationTypeValue);
        if (null != relationship) {
            return relationship;
        } else {
            relationship = new Relationship();
            relationship.setParentRelationName(parentContentType.name());
            relationship.setChildRelationName(childContentType.name());
            relationship.setCardinality(0);
            relationship.setRelationTypeValue(relationTypeValue);
            relationship.setParentStructureInode(parentContentType.inode());
            relationship.setChildStructureInode(childContentType.id());
            try {
                relationshipAPI.create(relationship);
            } catch (Exception e) {
                throw new DotRuntimeException(e);
            }
        }
        return relationship;
    }

}
