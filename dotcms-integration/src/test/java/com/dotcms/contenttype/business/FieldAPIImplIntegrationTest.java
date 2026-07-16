package com.dotcms.contenttype.business;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.job.CleanUpFieldReferencesJob;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.quartz.SchedulerException;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.*;

@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class FieldAPIImplIntegrationTest {

    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        //TODO: Remove this when the whole change is done
        try {
            new DotConnect().setSQL("CREATE TABLE IF NOT EXISTS unique_fields (" +
                    "unique_key_val VARCHAR(64) PRIMARY KEY," +
                    "supporting_values JSONB" +
                    " )").loadObjectResults();
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * when: A content type has fields with wrong sort_order and save multiple fields to fix the sort order and move a field
     * Should: save all of them with the right sort order value
     * Method to test: {@link FieldAPIImpl#saveFields(List, User)}
     */
    @Test
    public void shouldSaveFields() throws DotDataException, DotSecurityException {

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final long current = System.currentTimeMillis();

        final Field aliases = new FieldDataGen()
                .name("Aliases")
                .sortOrder(1)
                .contentTypeId(contentType.id())
                .velocityVarName("Aliases" + current)
                .nextPersisted();

        final Field row = new FieldDataGen()
                .name("fields-0")
                .type(RowField.class)
                .sortOrder(1)
                .contentTypeId(contentType.id())
                .velocityVarName("fields0" + current)
                .nextPersisted();

        final Field column = new FieldDataGen()
                .name("fields-1").
                type(ColumnField.class)
                .sortOrder(1)
                .contentTypeId(contentType.id())
                .velocityVarName("fields1" + current)
                .nextPersisted();

        final Field hostName = new FieldDataGen()
                .name("Host Name")
                .sortOrder(1)
                .contentTypeId(contentType.id())
                .velocityVarName("hostname" + current)
                .nextPersisted();

        final Field tagStorage = new FieldDataGen()
                .name("Tag Storage")
                .sortOrder(2)
                .contentTypeId(contentType.id())
                .velocityVarName("tagstorage" + current)
                .nextPersisted();

        final Field isDefault = new FieldDataGen()
                .name("Is Default")
                .sortOrder(3)
                .contentTypeId(contentType.id())
                .velocityVarName("default" + current)
                .nextPersisted();

        //
        final Field newRow_1 = new FieldDataGen()
                .name("fields-2")
                .type(RowField.class)
                .sortOrder(0)
                .contentTypeId(contentType.id())
                .velocityVarName("field2" + current)
                .next();

        final Field newColumn_1 = new FieldDataGen()
                .name("fields-3")
                .type(ColumnField.class)
                .sortOrder(1)
                .contentTypeId(contentType.id())
                .velocityVarName("field3" + current)
                .next();

        final Field newRow_2 = new FieldDataGen()
                .id(row.id())
                .name("fields-0")
                .type(RowField.class)
                .sortOrder(2)
                .contentTypeId(contentType.id())
                .velocityVarName(row.variable())
                .next();

        final Field newColumn_2 = new FieldDataGen()
                .id(column.id())
                .name("fields-1")
                .type(ColumnField.class)
                .sortOrder(3)
                .contentTypeId(contentType.id())
                .velocityVarName(column.variable())
                .next();

        final Field newAliases = new FieldDataGen()
                .id(aliases.id())
                .name("Aliases")
                .sortOrder(4)
                .contentTypeId(contentType.id())
                .velocityVarName(aliases.variable())
                .next();

        final Field newHostName = new FieldDataGen()
                .id(hostName.id())
                .name("Host Name")
                .sortOrder(5)
                .contentTypeId(contentType.id())
                .velocityVarName(hostName.variable())
                .next();

        final Field newTagStorage = new FieldDataGen()
                .id(tagStorage.id())
                .name("Tag Storage")
                .sortOrder(6)
                .contentTypeId(contentType.id())
                .velocityVarName(tagStorage.variable())
                .next();

        final Field newIsDefault = new FieldDataGen()
                .id(isDefault.id())
                .name("Is Default")
                .sortOrder(7)
                .contentTypeId(contentType.id())
                .velocityVarName(isDefault.variable())
                .next();

        final List<Field> newFieldList = list(
                newRow_1, newColumn_1, newRow_2, newColumn_2, newAliases, newHostName, newTagStorage, newIsDefault);
        APILocator.getContentTypeFieldAPI().saveFields(newFieldList, APILocator.systemUser());

        final List<Field> fields = APILocator.getContentTypeAPI(APILocator.systemUser()).find(contentType.id()).fields();

        assertEquals(8, fields.size());

        final List<String> expectedNames = newFieldList.stream().map(field -> field.name()).collect(Collectors.toList());

        for (int i = 0; i < expectedNames.size(); i++) {
            assertEquals(expectedNames.get(i), fields.get(i).name());
            assertEquals(i, fields.get(i).sortOrder());
        }

    }


    /**
     * Method to test: {@link FieldAPIImpl#delete(Field, User)}
     * When: Create a COntentType with a unique fields and later remove the unique Field
     * Should: Clean the unique_fields extra table
     *
     * @throws DotDataException
     */
    @Test
    public void cleanUpUniqueFieldTableAfterDeleteField() throws DotDataException {
        final boolean oldEnabledDataBaseValidation = ESContentletAPIImpl.getFeatureFlagDbUniqueFieldValidation();

        try {
            ESContentletAPIImpl.setFeatureFlagDbUniqueFieldValidation(true);
            final ContentType contentType = new ContentTypeDataGen()
                    .nextPersisted();

            final Language language = new LanguageDataGen().nextPersisted();

            final Field uniqueTextField = new FieldDataGen()
                    .name("unique")
                    .contentTypeId(contentType.id())
                    .unique(true)
                    .type(TextField.class)
                    .nextPersisted();

            final Host host = new SiteDataGen().nextPersisted();

            checkExtraTableCount(contentType, 0);

            new ContentletDataGen(contentType)
                    .host(host)
                    .languageId(language.getId())
                    .setProperty(uniqueTextField.variable(), "unique-value")
                    .nextPersistedAndPublish();

            checkExtraTableCount(contentType, 1);

            APILocator.getContentTypeFieldAPI().delete(uniqueTextField);

            checkExtraTableCount(contentType, 0);
        } finally {
            ESContentletAPIImpl.setFeatureFlagDbUniqueFieldValidation(oldEnabledDataBaseValidation);
        }
    }

    /**
     * Method to test: {@link FieldAPIImpl#delete(Field, User)}
     * Given Scenario: Big amount of contents, delete a field of the contents type to trigger CleanUpFieldReferencesJob, and while it is running try to create a new field with the same name
     * ExpectedResult: Throw an exception indicating that the field cannot be created while the job is still running
     *
     */
    @Test
    public void createSameFieldNameWhileCleaningUpField(){
        try {
            final ContentType contentType = new ContentTypeDataGen()
                    .nextPersisted();
            final Language language = new LanguageDataGen().nextPersisted();

            final Field field = new FieldDataGen()
                    .name("creationDate")
                    .velocityVarName("creationDate")
                    .contentTypeId(contentType.id())
                    .type(TextField.class)
                    .nextPersisted();

            final Host host = new SiteDataGen().nextPersisted();

            final long langId = language.getId();

            for (int i = 0; i < 1000; i++) {
            new ContentletDataGen(contentType)
                    .host(host)
                    .languageId(langId)
                    .nextPersisted();
            }


            Thread deleteThread = new Thread(() -> {
                try {
                    APILocator.getContentTypeFieldAPI().delete(field);
                    QuartzUtils.startSchedulers();
                } catch (SchedulerException | DotDataException e) {
                    throw new RuntimeException(e);
                }
            });

            deleteThread.start();

            Thread.sleep(500);


            Exception e = Assertions.assertThrows(Exception.class, () -> {
                Field newField = new FieldDataGen()
                        .name("creationDate")
                        .velocityVarName("creationDate")
                        .contentTypeId(contentType.id())
                        .type(TextField.class)
                        .next();
                APILocator.getContentTypeFieldAPI().save(newField, APILocator.systemUser(), false);
            });

            Assert.assertEquals("Field variable 'creationDate' cannot be recreated while the CleanUpFieldReferencesJob is running. Please wait until finish", e.getMessage());

            deleteThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkExtraTableCount(final ContentType contentType, final int countExpected)
            throws DotDataException {
        final List<Map<String, Object>> results = new DotConnect().setSQL("SELECT * FROM unique_fields WHERE supporting_values->>'contentTypeId' = ?")
                .addParam(contentType.id())
                .loadObjectResults();

        assertEquals(countExpected, results.size());
    }
}