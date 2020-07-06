package com.dotcms.content.elasticsearch.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
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


@RunWith(DataProviderRunner.class)
public class ESMappingUtilHelperTest {

    private static User user = APILocator.systemUser();
    private static ContentletIndexAPI contentletIndexAPI = APILocator.getContentletIndexAPI();
    private static ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
    private static ESMappingAPIImpl esMappingAPI = new ESMappingAPIImpl();
    private static FieldAPI fieldAPI = APILocator.getContentTypeFieldAPI();

    public static class PlusTestNameFormatter implements DataProviderTestNameFormatter {
        @Override
        public String format(Method testMethod, int invocationIndex, List<Object> arguments) {
            return String.format("Test Name: %s. Mapping Name: %s", testMethod.getName(), arguments.get(0));
        }
    }

    @DataProvider(formatter = PlusTestNameFormatter.class)
    public static Object[][] dataProviderAddMappingForFields() {
        return new Object[][] {
                {  "strings_as_dates", DateField.class, DataTypes.DATE,
                        new String[] {"originalstartdate", "recurrencestart", "recurrenceend"},  "date" },

                {  "strings_as_date_times", DateTimeField.class, DataTypes.DATE,
                        new String[] {"originalstartdate", "recurrencestart", "recurrenceend"},  "date" },

                {  "dates_as_text", TextField.class, DataTypes.TEXT,
                        new String[] {"originalstartdate", "recurrencestart", "recurrenceend"},  null },

                {  "keywordmapping", TextField.class, DataTypes.TEXT,
                        new String[] {"categories", "tags", "conhost",
                                "wfstep", "structurename", "contenttype", "parentpath",
                                "path", "urlmap", "moduser", "owner"},  null },

                {  "geomapping", TextField.class, DataTypes.TEXT,
                        new String[] {"mylatlong", "mylatlon"},  null },

                {  "permissions", TextField.class, DataTypes.TEXT, new String[] {"permissions"},  null },

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

}
