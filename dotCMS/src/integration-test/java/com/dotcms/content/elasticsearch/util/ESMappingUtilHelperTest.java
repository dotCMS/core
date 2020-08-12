package com.dotcms.content.elasticsearch.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import com.tngtech.junit.dataprovider.format.DataProviderTestNameFormatter;
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
                {"permissions", new String[]{"permissions"}, "text"},
                {"hostname", new String[]{"host.hostname_text"}, "text"}
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

    /*Data provider used to verify that the ES mapping is applied correctly and considers exclusions in the `es-content-mapping` file
        For each scenario we define:
            1) testCase name
            2) Field(s) type
            3) Field(s) variable name(s)
            4) Expected ES mapping
     */
    @DataProvider(formatter = PlusTestNameFormatter.class)
    public static Object[][] dataProviderAddMappingForFields() {
        return new Object[][] {
                {  "strings_as_dates", DateField.class, DataTypes.DATE,
                        new String[] {"originalstartdate", "recurrencestart", "recurrenceend"},  "date" },

                {  "strings_as_date_times", DateTimeField.class, DataTypes.DATE,
                        new String[] {"originalstartdate", "recurrencestart", "recurrenceend"},  "date" },

                {  "dates_as_text", TextField.class, DataTypes.TEXT,
                        new String[] {"originalstartdate", "recurrencestart", "recurrenceend"},  "text" },

                {  "keywordmapping", TextField.class, DataTypes.TEXT,
                        new String[] {"categories", "tags", "conhost",
                                "wfstep", "structurename", "contenttype", "parentpath",
                                "path", "urlmap", "moduser", "owner"},  "text" },

                {  "geomapping", TextField.class, DataTypes.TEXT,
                        new String[] {"mylatlong", "mylatlon"},  null },

                {  "permissions", TextField.class, DataTypes.TEXT, new String[] {"permissions"},  "text" },

                {  "radio_as_boolean", RadioField.class, DataTypes.BOOL,
                        new String[] {"MyRadioAsBoolean"},  "boolean" },

                {  "radio_as_float", RadioField.class, DataTypes.FLOAT,
                        new String[] {"MyRadioAsFloat"},  "double" },

                {  "radio_as_integer", RadioField.class, DataTypes.INTEGER,
                        new String[] {"MyRadioAsInteger"},  "long" },

                {  "select_as_boolean", SelectField.class, DataTypes.BOOL,
                        new String[] {"MySelectAsBoolean"},  "boolean" },

                {  "select_as_float", SelectField.class, DataTypes.FLOAT,
                        new String[] {"MySelectAsFloat"},  "double" },

                {  "select_as_integer", SelectField.class, DataTypes.INTEGER,
                        new String[] {"MySelectAsInteger"},  "long" },

                {  "text_as_float", TextField.class, DataTypes.FLOAT,
                        new String[] {"MyTextAsFloat"},  "double" },

                {  "text_as_integer", TextField.class, DataTypes.INTEGER,
                        new String[] {"MyTextAsInteger"},  "long" }
        };
    }

    /**
     * <b>Method to test:</b> Internally, it tests the method ESMappingUtilHelper.addMappingForFieldIfNeeded<p></p>
     * <b>Test Case:</b> Given a field or an array of fields, the right ES mapping should be set, considering exclusions in the `es-content-mapping` file<p></p>
     * <b>Expected Results:</b> Each test case should match the `expectedResult` value, which is a string with the correct datatype in ES
     * @param testCase
     * @param fieldType
     * @param type
     * @param fields
     * @param expectedResult
     * @throws IOException
     * @throws DotIndexException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @UseDataProvider
    @Test
    public void testAddMappingForFields(final String testCase, final Class fieldType,
            final DataTypes type,
            final String[] fields, final String expectedResult)
            throws IOException, DotIndexException, DotSecurityException, DotDataException {

        Logger.info(ESMappingUtilHelperTest.class,
                String.format("Testing Add Mapping for fields defined in %s template", testCase));

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        String workingIndex = null;
        //Build the index name
        String timestamp = String.valueOf(new Date().getTime());
        try {
            //Adding fields
            for (final String field : fields) {
                final Field newField = FieldBuilder.builder(fieldType)
                        .name(field).variable(field).dataType(type).contentTypeId(contentType.id())
                        .indexed(true).build();
                fieldAPI.save(newField, user);
            }

            workingIndex = new ESIndexAPI().getNameWithClusterIDPrefix(
                    IndexType.WORKING.getPrefix() + "_" + timestamp);

            //Create a working index
            boolean result = contentletIndexAPI.createContentIndex(workingIndex);
            //Validate
            assertTrue(result);

            for (final String field : fields) {

                if (expectedResult == null) {
                    assertFalse(UtilMethods.isSet(esMappingAPI.getFieldMappingAsMap(workingIndex,
                            (contentType.variable() + StringPool.PERIOD + field).toLowerCase())));
                } else {
                    Map<String, String> mapping = (Map<String, String>) esMappingAPI
                            .getFieldMappingAsMap(workingIndex,
                                    (contentType.variable() + StringPool.PERIOD + field)
                                            .toLowerCase()).get(field.toLowerCase());
                    assertTrue(UtilMethods.isSet(mapping.get("type")));
                    assertEquals(expectedResult, mapping.get("type"));
                }
            }

        } finally {
            if (workingIndex != null) {
                contentletIndexAPI.delete(workingIndex);
            }

            if (contentType != null) {
                contentTypeAPI.delete(contentType);
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
                    .getFieldMappingAsMap(APILocator.getIndiciesAPI().loadIndicies().getWorking(),
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
                    .getFieldMappingAsMap(APILocator.getIndiciesAPI().loadIndicies().getWorking(),
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
