package com.dotcms.contenttype.business.uniquefields;

import com.dotcms.JUnit4WeldRunner;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldDataBaseUtil;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import graphql.AssertException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.CONTENTLET_IDS_ATTR;
import static org.junit.Assert.*;

@ApplicationScoped
@RunWith(JUnit4WeldRunner.class)
public class UniqueFieldsValidationInitializerTest {

    @BeforeClass
    public static void init () throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link UniqueFieldsValidationInitializer#init()}
     * When: The Database Unique Field value validation  is disabled and the table does not exist
     * Should: do nothing
     *
     * @throws DotDataException
     * @throws SQLException
     */
    @Test
    public void tableIsNotThereAndDBValidationIsDisabled() throws DotDataException, SQLException {
        final Connection connection = DbConnectionFactory.getConnection();
        DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.dropUniqueFieldsValidationTable();

        boolean oldFeatureFlagDbUniqueFieldValidation = ESContentletAPIImpl.getFeatureFlagDbUniqueFieldValidation();

        try {
            ESContentletAPIImpl.setFeatureFlagDbUniqueFieldValidation(false);
            final UniqueFieldsValidationInitializer uniqueFieldsValidationInitializer =
                    CDIUtils.getBeanThrows(UniqueFieldsValidationInitializer.class);

            uniqueFieldsValidationInitializer.init();

            assertFalse(dotDatabaseMetaData.tableExists(connection, "unique_fields"));
        } finally {
            ESContentletAPIImpl.setFeatureFlagDbUniqueFieldValidation(oldFeatureFlagDbUniqueFieldValidation);
        }

    }

    /**
     * Method to test: {@link UniqueFieldsValidationInitializer#init()}
     * When: The Database Unique Field value validation  is enabled and the table exists
     * Should: do nothing
     *
     * @throws DotDataException
     * @throws SQLException
     */
    @Test
    public void tableIsThereAndDBValidationIsEnabled() throws DotDataException, SQLException {
        final Connection connection = DbConnectionFactory.getConnection();
        DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.dropUniqueFieldsValidationTable();
        uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();

        boolean oldFeatureFlagDbUniqueFieldValidation = ESContentletAPIImpl.getFeatureFlagDbUniqueFieldValidation();

        try {
            ESContentletAPIImpl.setFeatureFlagDbUniqueFieldValidation(true);

            assertTrue(dotDatabaseMetaData.tableExists(connection, "unique_fields"));
        } finally {
            ESContentletAPIImpl.setFeatureFlagDbUniqueFieldValidation(oldFeatureFlagDbUniqueFieldValidation);
        }
    }

    /**
     * Method to test: {@link UniqueFieldsValidationInitializer#init()}
     * When: The Database Unique Field value validation  is enabled and the table does not exist
     * Should: Create the table and populate it
     *
     * @throws DotDataException
     * @throws SQLException
     */
    @Test
    public void tableIsNotThereAndDBValidationIsEnabled() throws DotDataException, SQLException, IOException {
        final Connection connection = DbConnectionFactory.getConnection();
        DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.dropUniqueFieldsValidationTable();

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Language language = new LanguageDataGen().nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                .nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), "unique_value")
                .nextPersistedAndPublish();

        boolean oldFeatureFlagDbUniqueFieldValidation = ESContentletAPIImpl.getFeatureFlagDbUniqueFieldValidation();

        try {
            ESContentletAPIImpl.setFeatureFlagDbUniqueFieldValidation(true);
            final UniqueFieldsValidationInitializer uniqueFieldsValidationInitializer =
                    CDIUtils.getBeanThrows(UniqueFieldsValidationInitializer.class);

            uniqueFieldsValidationInitializer.init();

            assertTrue(dotDatabaseMetaData.tableExists(connection, "unique_fields"));

            final List<Map<String, Object>> uniqueFieldsRegisters = getUniqueFieldsRegisters(contentType);
            assertEquals(1, uniqueFieldsRegisters.size());

            final Map<String, Object> uniqueFieldsRegister = uniqueFieldsRegisters.get(0);
            final Map<String, Object> supportingValues = JsonUtil.getJsonFromString(uniqueFieldsRegister.get("supporting_values").toString());

            final List<String> contentletIds = (List<String>) supportingValues.get(CONTENTLET_IDS_ATTR);
            assertEquals(1, contentletIds.size());
            assertTrue(contentletIds.contains(contentlet.getIdentifier()));
        } finally {
            ESContentletAPIImpl.setFeatureFlagDbUniqueFieldValidation(oldFeatureFlagDbUniqueFieldValidation);
        }
    }

    /**
     * Method to test: {@link UniqueFieldsValidationInitializer#init()}
     * When: The Database Unique Field value validation  is disabled and the table exists
     * Should: Drop the table even if it has registers
     *
     * @throws DotDataException
     * @throws SQLException
     */
    @Test
    public void tableIsThereAndDBValidationIsDisabled() throws DotDataException, SQLException {
        final Connection connection = DbConnectionFactory.getConnection();
        DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Language language = new LanguageDataGen().nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                .nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), "unique_value")
                .nextPersistedAndPublish();

        boolean oldFeatureFlagDbUniqueFieldValidation = ESContentletAPIImpl.getFeatureFlagDbUniqueFieldValidation();

        try {
            ESContentletAPIImpl.setFeatureFlagDbUniqueFieldValidation(false);
            final UniqueFieldsValidationInitializer uniqueFieldsValidationInitializer =
                    CDIUtils.getBeanThrows(UniqueFieldsValidationInitializer.class);

            uniqueFieldsValidationInitializer.init();

            assertFalse(dotDatabaseMetaData.tableExists(connection, "unique_fields"));
        } finally {
            ESContentletAPIImpl.setFeatureFlagDbUniqueFieldValidation(oldFeatureFlagDbUniqueFieldValidation);
        }
    }

    private List<Map<String, Object>> getUniqueFieldsRegisters(ContentType contentType) throws DotDataException {
        return new DotConnect().setSQL("SELECT * FROM unique_fields WHERE supporting_values->>'contentTypeId' = ?")
                .addParam(contentType.id()).loadObjectResults();
    }
}
