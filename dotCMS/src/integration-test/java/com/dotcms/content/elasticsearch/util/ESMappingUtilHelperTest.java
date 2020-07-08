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
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Relationship;
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
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Class used to validate ES mapping for fields is applied correctly considering fields types and dynamic mapping defined in `es-content-mapping.json`
 * @author nollymar
 */
@RunWith(DataProviderRunner.class)
public class ESMappingUtilHelperTest {

    private static User user = APILocator.systemUser();
    private static ContentletIndexAPI contentletIndexAPI = APILocator.getContentletIndexAPI();
    private static ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
    private static ESMappingAPIImpl esMappingAPI = new ESMappingAPIImpl();
    private static FieldAPI fieldAPI = APILocator.getContentTypeFieldAPI();
    private static RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();

    public static class PlusTestNameFormatter implements DataProviderTestNameFormatter {
        @Override
        public String format(Method testMethod, int invocationIndex, List<Object> arguments) {
            return String.format("Test Name: %s. Mapping Name: %s", testMethod.getName(), arguments.get(0));
        }
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
                        new String[] {"MyRadioAsFloat"},  "float" },

                {  "radio_as_integer", RadioField.class, DataTypes.INTEGER,
                        new String[] {"MyRadioAsInteger"},  "integer" },

                {  "select_as_boolean", SelectField.class, DataTypes.BOOL,
                        new String[] {"MySelectAsBoolean"},  "boolean" },

                {  "select_as_float", SelectField.class, DataTypes.FLOAT,
                        new String[] {"MySelectAsFloat"},  "float" },

                {  "select_as_integer", SelectField.class, DataTypes.INTEGER,
                        new String[] {"MySelectAsInteger"},  "integer" },

                {  "text_as_float", TextField.class, DataTypes.FLOAT,
                        new String[] {"MyTextAsFloat"},  "float" },

                {  "text_as_integer", TextField.class, DataTypes.INTEGER,
                        new String[] {"MyTextAsInteger"},  "integer" }
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
                    .name("age").contentTypeId(parentContentType.id()).indexed(false).build();
            ageField = fieldAPI.save(ageField, user);

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
