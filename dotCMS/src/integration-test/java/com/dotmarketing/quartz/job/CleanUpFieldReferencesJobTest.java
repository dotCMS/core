package com.dotmarketing.quartz.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Calendar;
import java.util.Date;
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
        IntegrationTestInitService.getInstance().init();
    }

    public static class TestCase {

        String name;
        Object fieldValue;
        String values;
        Object expectedValue;
        Class fieldType;

        public TestCase(final String name, final Object fieldValue, final String values,
                final Object expectedValue, final Class fieldType) {
            this.name = name;
            this.fieldValue = fieldValue;
            this.values = values;
            this.fieldType = fieldType;
            this.expectedValue = expectedValue;
        }
    }

    @DataProvider
    public static Object[] testCases() {
        return new TestCase[]{
                new TestCase("checkboxFieldVarName", "CA",
                        "Canada|CA\r\nMexico|MX\r\nUSA|US","", CheckboxField.class),
                new TestCase("dateTimeFieldVarName", new Date(System.currentTimeMillis()-24*60*60*1000),
                        null, new Date(), DateTimeField.class)

        };
    }

    @UseDataProvider("testCases")
    @Test
    public void testCleanUpFieldJob(TestCase testCase)
            throws DotDataException, DotSecurityException {

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

            // Execute jobs to delete fields
            TestJobExecutor.execute(instance,
                    CollectionsUtils
                            .map("deletionDate",
                                    new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000),
                                    "field", field, "user", systemUser));

            // Make sure the field value is cleaned up
            CacheLocator.getContentletCache().remove(contentlet);
            contentlet = APILocator.getContentletAPI().find(contentlet.getInode(), systemUser, false);

            Object fieldValue = contentlet.get(field.variable());

            if (fieldValue instanceof Date){

                Calendar cal1 = Calendar.getInstance();
                Calendar cal2 = Calendar.getInstance();
                cal1.setTime((Date) fieldValue);
                cal2.setTime((Date) testCase.expectedValue);

                assertTrue(cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
            } else{
                assertEquals(testCase.expectedValue, fieldValue);
            }

        } finally {
            if (contentType != null) {
                contentTypeAPI.delete(contentType);
            }
        }
    }

}
