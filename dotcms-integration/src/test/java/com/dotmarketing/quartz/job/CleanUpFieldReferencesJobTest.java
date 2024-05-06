package com.dotmarketing.quartz.job;

import static com.dotcms.content.business.json.ContentletJsonAPI.SAVE_CONTENTLET_AS_JSON;
import static com.dotmarketing.quartz.DotStatefulJob.EXECUTION_DATA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.Config;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author nollymar
 */
@RunWith(DataProviderRunner.class)
public class CleanUpFieldReferencesJobTest extends IntegrationTestBase {

    final CleanUpFieldReferencesJob instance = new CleanUpFieldReferencesJob();

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        APILocator.getContentletIndexAPI().checkAndInitialiazeIndex();
        IntegrationTestInitService.getInstance().init();

    }

    public static class TestCase {

        final String name;
        final Object fieldValue;
        final String values;
        final Class<?> fieldType;
        final boolean isJsonFields;

        public TestCase(final String name, final Object fieldValue, final String values,
                final Class<?> fieldType, final boolean isJsonFields) {
            this.name = name;
            this.fieldValue = fieldValue;
            this.values = values;
            this.fieldType = fieldType;
            this.isJsonFields = isJsonFields;
        }

        @Override
        public String toString() {
            return "TestCase{" +
                    "name='" + name + '\'' +
                    ", fieldValue=" + fieldValue +
                    ", values='" + values + '\'' +
                    ", fieldType=" + fieldType +
                    ", isJsonFields=" + isJsonFields +
                    '}';
        }
    }

    @DataProvider
    public static Object[] testCases() {
        return new TestCase[]{
                new TestCase("checkboxFieldVarName", "CA",
                        "Canada|CA\r\nMexico|MX\r\nUSA|US", CheckboxField.class, false),
                new TestCase("dateTimeFieldVarName",
                        new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000),
                        null, DateTimeField.class, false),

                new TestCase("checkboxFieldVarName", "CA",
                        "Canada|CA\r\nMexico|MX\r\nUSA|US", CheckboxField.class, true),
                new TestCase("dateTimeFieldVarName", null,
                        null, DateTimeField.class, true)
        };
    }

    @UseDataProvider("testCases")
    @Test
    public void testCleanUpFieldJob(TestCase testCase)
            throws DotDataException, DotSecurityException {

        if (!APILocator.getContentletJsonAPI().isPersistContentAsJson() && testCase.isJsonFields) {
            //if we're on a db different from a json supporting db (like Postgres or MS-SQL) and this test is marked for jsonFields. We Skip it.
            return;
        }

        final boolean saveFieldsAsJson = Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);
        Config.setProperty(SAVE_CONTENTLET_AS_JSON, testCase.isJsonFields);

        final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final User systemUser = APILocator.getUserAPI().getSystemUser();

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
        final FieldAPI fieldAPI = APILocator.getContentTypeFieldAPI();

        final String currentTime = String.valueOf(new Date().getTime());
        ContentType contentType = null;

        try {

            // Create content type
            contentType = contentTypeAPI.save(
                    ContentTypeBuilder.builder(SimpleContentType.class).folder(
                                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                            .name("testContentType" + currentTime)
                            .owner(systemUser.getUserId()).build());

            // Adding the test fields
            Field field = FieldBuilder
                    .builder(testCase.fieldType).name(testCase.name).variable(testCase.name)
                    .values(testCase.values).contentTypeId(contentType.id()).build();
            field = fieldAPI.save(field, systemUser);

            // Validate the field was properly saved
            contentType = contentTypeAPI.find(contentType.id());
            assertEquals(1, contentType.fields().size());

            // Create a new content
            ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());
            Contentlet contentlet = contentletDataGen.languageId(langId)
                    .setProperty(field.variable(), testCase.fieldValue).nextPersisted();

            final Map<String, Object> jobProperties = new HashMap<>();
            // Execute jobs to delete fields
            final ImmutableMap<String, Serializable> nextExecutionData = ImmutableMap
                    .of("deletionDate", new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000),
                            "field", field, "user", systemUser);

            jobProperties.put(EXECUTION_DATA, nextExecutionData);

            TestJobExecutor.execute(instance, jobProperties);

            // Make sure the field value is cleaned up
            contentlet = APILocator.getContentletAPI()
                    .find(contentlet.getInode(), systemUser, false);

            Object fieldValue = contentlet.get(field.variable());

            if (testCase.isJsonFields) {
                // For jsonField We're testing that the field was removed from the contentlet
                assertNull(fieldValue);
            } else {
                // For the regular field-column implementation We test that the field was set to their default state before anything was saved on it
                if (fieldValue instanceof Date) {

                    Calendar cal1 = Calendar.getInstance();
                    Calendar cal2 = Calendar.getInstance();
                    cal1.setTime((Date) fieldValue);
                    cal2.setTime((Date) testCase.fieldValue);

                    assertNotEquals(cal1.get(Calendar.DAY_OF_YEAR), cal2.get(Calendar.DAY_OF_YEAR));
                } else {
                    assertEquals(DbConnectionFactory.isOracle() ? null : "", fieldValue);
                }
            }

        } finally {

            Config.setProperty(SAVE_CONTENTLET_AS_JSON, saveFieldsAsJson);

            if (contentType != null) {
                contentTypeAPI.delete(contentType);
            }
        }
    }

}
