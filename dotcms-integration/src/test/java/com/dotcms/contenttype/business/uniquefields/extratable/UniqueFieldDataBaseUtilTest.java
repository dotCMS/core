package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotcms.JUnit4WeldRunner;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FieldVariableDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.JsonUtil;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UUIDGenerator;
import graphql.AssertException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.Builder;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.CONTENTLET_IDS_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.CONTENT_TYPE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.FIELD_VALUE_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.FIELD_VARIABLE_NAME_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.LANGUAGE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.LIVE_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.SITE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.UNIQUE_PER_SITE_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.VARIANT_ATTR;
import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This Integration Test verifies that the {@link UniqueFieldDataBaseUtil} class is working as
 * expected.
 *
 * @author Freddy Rodriguez
 * @since Oct 30th, 2024
 */
@ApplicationScoped
@RunWith(JUnit4WeldRunner.class)
public class UniqueFieldDataBaseUtilTest {

    @BeforeClass
    public static void init () throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b>
     *     {@link UniqueFieldDataBaseUtil#createUniqueFieldsValidationTable()}</li>
     *     <li><b>Given Scenario:</b> Create the unique_fields table which, by default, has no
     *     primary key set. This is because there might be situations in which several Contentlets
     *     may have the same unique value, and that's not correct. Temporarily allowing duplicates
     *     allows us to find them and fix them appropriately.</li>
     *     <li><b>Expected Result:</b> The unique_fields table must exist, with the right columns
     *     .</li>
     * </ul>
     *
     * @throws SQLException     An error occurred when checking the DBMS metadata.
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @Test
    public void createUniqueFieldsTable() throws SQLException, DotDataException {
        final Connection connection = DbConnectionFactory.getConnection();
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        dotDatabaseMetaData.dropTable(connection, "unique_fields");

        assertFalse("The 'unique_fields' table was just dropped, and should NOT exist",
                dotDatabaseMetaData.tableExists(connection, "unique_fields"));

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();

        assertTrue("The 'unique_fields' table must exist now",
                dotDatabaseMetaData.tableExists(connection, "unique_fields"));

        final ResultSet uniqueFieldsColumns = DotDatabaseMetaData.getColumnsMetaData(connection,
                "unique_fields");
        while (uniqueFieldsColumns.next()) {
            final String columnName = uniqueFieldsColumns.getString("COLUMN_NAME");
            final String columnType = uniqueFieldsColumns.getString("TYPE_NAME");
            final String columnSize = uniqueFieldsColumns.getString("COLUMN_SIZE");

            if (columnName.equals("unique_key_val")) {
                assertEquals("varchar", columnType);
                assertEquals("64", columnSize);
            } else if (columnName.equals("supporting_values")) {
                assertEquals("jsonb", columnType);
            } else {
                throw new AssertException("Unexpected column name: " + columnName);
            }
        }
    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#dropUniqueFieldsValidationTable()}
     * When: Add some register in the table and call this method
     * Should: drop the unique_fields table with the right columns
     *
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    public void dropUniqueFieldsTable() throws SQLException, DotDataException {
        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();

        try {
            final Connection connection = DbConnectionFactory.getConnection();
            final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
            dotDatabaseMetaData.dropTable(connection, "unique_fields");
            assertFalse(dotDatabaseMetaData.tableExists(connection, "unique_fields"));

            uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();
            uniqueFieldDataBaseUtil.addPrimaryKeyConstraintsBack();

            assertTrue(dotDatabaseMetaData.tableExists(connection, "unique_fields"));

            new DotConnect().setSQL("INSERT INTO unique_fields (unique_key_val, supporting_values) VALUES (encode(sha256(?::bytea), 'hex'), ?)")
                    .addParam("Testing")
                    .addJSONParam("{\"test\": \"this is just a test\"}")
                    .loadObjectResults();

            uniqueFieldDataBaseUtil.dropUniqueFieldsValidationTable();

            assertFalse(dotDatabaseMetaData.tableExists(connection, "unique_fields"));
        } finally {
            uniqueFieldDataBaseUtil.createTableAndPopulate();
        }
    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#populateUniqueFieldsTable()}
     * When:
     * - Create a {@link ContentType} with a Unique {@link Field}
     * - Create a {@link Contentlet} with WORKING and LIVE version, each one with different value.
     * - Populate the unique_fields table.
     * Should: Create a couple of register with one with the LIVE value and the other one with the WORKING value
     * and uniquePerSite equals to false.
     *
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    public void populateUniqueFieldsTable() throws SQLException, DotDataException, IOException {
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        final Connection connection = DbConnectionFactory.getConnection();

        final String liveUniqueValue = "live_unique_value";
        final String workingUniqueValue =  "working_unique_value";
        final String anotherUniqueValue =  "another_unique_value";

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Language language = new LanguageDataGen().nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                .nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final Contentlet liveContentlet = new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), liveUniqueValue)
                .nextPersistedAndPublish();

        Contentlet workingContentlet = ContentletDataGen.checkout(liveContentlet);
        workingContentlet.setProperty(uniqueTextField.variable(), workingUniqueValue);
        ContentletDataGen.checkin(workingContentlet);

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), anotherUniqueValue)
                .nextPersistedAndPublish();

        dotDatabaseMetaData.dropTable(connection, "unique_fields");

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();
        uniqueFieldDataBaseUtil.addPrimaryKeyConstraintsBack();

        assertTrue(getUniqueFieldRecords(contentType).isEmpty());

        uniqueFieldDataBaseUtil.populateUniqueFieldsTable();

        final List<Map<String, Object>> uniqueFieldsRegisters = getUniqueFieldRecords(contentType);
        assertEquals(3, uniqueFieldsRegisters.size());

        for (Map<String, Object> uniqueFieldsRegister : uniqueFieldsRegisters) {
            final Map<String, Object> supportingValues = getSupportingValues(uniqueFieldsRegister);

            final List<String> contentletIds = (List<String>) supportingValues.get(CONTENTLET_IDS_ATTR);
            final Boolean live = Boolean.valueOf(supportingValues.get(LIVE_ATTR) != null ?
                    supportingValues.get(LIVE_ATTR).toString() : "false");

            assertEquals(1, contentletIds.size());

            final String contentId = contentletIds.get(0);

            if (liveContentlet.getIdentifier().equals(contentId) && live) {
                final String hash = calculateHash(liveContentlet, language, uniqueTextField, host, liveUniqueValue);
                assertEquals(hash, uniqueFieldsRegister.get("unique_key_val") );
            } else if (liveContentlet.getIdentifier().equals(contentId) && !live) {
                final String hash = calculateHash(workingContentlet, language, uniqueTextField, host, workingUniqueValue);
                assertEquals(hash, uniqueFieldsRegister.get("unique_key_val") );
            } else if (contentlet_2.getIdentifier().equals(contentId) && live) {
                final String hash = calculateHash(contentlet_2, language, uniqueTextField, host, anotherUniqueValue);
                assertEquals(hash, uniqueFieldsRegister.get("unique_key_val") );
            } else {
                throw new AssertException("Contentlet don't expected");
            }

            assertEquals(false, supportingValues.get("uniquePerSite") );
        }

    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#populateUniqueFieldsTable()}
     * When:
     * - Create a {@link ContentType} with a Unique {@link Field}
     * - Create a {@link Contentlet} with WORKING and LIVE version both with the same value.
     * - Populate the unique_fields table.
     * Should: Create just one register with one with the LIVE value set to false
     *
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    public void populateUniqueFieldsTableWithMultiVersionSameValue() throws SQLException, DotDataException, IOException {
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        final Connection connection = DbConnectionFactory.getConnection();
        final String uniqueValue =  "unique_value";

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Language language = new LanguageDataGen().nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                .nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final Contentlet liveContentlet = new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), uniqueValue)
                .nextPersistedAndPublish();

        Contentlet workingContentlet = ContentletDataGen.checkout(liveContentlet);
        workingContentlet.setProperty(uniqueTextField.variable(), uniqueValue);
        ContentletDataGen.checkin(workingContentlet);

        dotDatabaseMetaData.dropTable(connection, "unique_fields");

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();
        uniqueFieldDataBaseUtil.addPrimaryKeyConstraintsBack();

        assertTrue(getUniqueFieldRecords(contentType).isEmpty());

        uniqueFieldDataBaseUtil.populateUniqueFieldsTable();

        final List<Map<String, Object>> uniqueFieldsRegisters = getUniqueFieldRecords(contentType);
        assertEquals(1, uniqueFieldsRegisters.size());

        final Map<String, Object> uniqueFieldsRegister = uniqueFieldsRegisters.get(0);
        final Map<String, Object> supportingValues = getSupportingValues(uniqueFieldsRegister);

        assertEquals(false, Boolean.valueOf(supportingValues.get(LIVE_ATTR) != null ?
                supportingValues.get(LIVE_ATTR).toString() : "false"));

        final List<String> contentletIds = (List<String>) supportingValues.get(CONTENTLET_IDS_ATTR);
        assertEquals(1, contentletIds.size());

        final String contentId = contentletIds.get(0);
        assertEquals(liveContentlet.getIdentifier(), contentId);

        final String hash = calculateHash(liveContentlet, language, uniqueTextField, host, uniqueValue);
        assertEquals(hash, uniqueFieldsRegister.get("unique_key_val") );

    }

    private static String calculateHash(Contentlet liveContentlet, Language language, Field uniqueTextField, Host host, String liveUniqueValue) throws DotDataException {
        final UniqueFieldCriteria uniqueFieldCriteria = new Builder().setVariantName(liveContentlet.getVariantId())
                .setLanguage(language)
                .setContentType(liveContentlet.getContentType())
                .setField(uniqueTextField)
                .setSite(host)
                .setLive(true)
                .setValue(liveUniqueValue)
                .build();

        final String hash = new DotConnect().setSQL("SELECT encode(sha256(convert_to(?::text, 'UTF8' )), 'hex') as hash")
                 .addParam(uniqueFieldCriteria.criteria())
                 .loadObjectResults().get(0).get("hash").toString();
        return hash;
    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#populateUniqueFieldsTable()}
     * When:
     * - Create a {@link ContentType} with a Unique {@link Field} with the uniquePerSIte set to TRUE
     * - Create a {@link Contentlet} with WORKING and LIVE version, each one with different value.
     * - Populate the unique_fields table.
     * Should: Create a couple of register with one with the LIVE value and the other one with the WORKING value
     * and uniquePerSite equals to TRUE.
     *
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    public void populateUniqueFieldsTableWithUniquePerSIteEqualsTrue() throws SQLException, DotDataException, IOException {
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        final Connection connection = DbConnectionFactory.getConnection();

        final String liveUniqueValue = "live_unique_value";
        final String workingUniqueValue =  "working_unique_value";

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Language language = new LanguageDataGen().nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                .nextPersisted();

        new FieldVariableDataGen()
                .key(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                .value("true")
                .field(uniqueTextField)
                .nextPersisted();

        final Host host_1 = new SiteDataGen().nextPersisted();
        final Host host_2 = new SiteDataGen().nextPersisted();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                .host(host_1)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), liveUniqueValue)
                .nextPersistedAndPublish();

        Contentlet workingContentlet = ContentletDataGen.checkout(contentlet_1);
        workingContentlet.setProperty(uniqueTextField.variable(), workingUniqueValue);
        ContentletDataGen.checkin(workingContentlet);

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .host(host_2)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), liveUniqueValue)
                .nextPersistedAndPublish();

        dotDatabaseMetaData.dropTable(connection, "unique_fields");

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();
        uniqueFieldDataBaseUtil.addPrimaryKeyConstraintsBack();

        assertTrue(getUniqueFieldRecords(contentType).isEmpty());

        uniqueFieldDataBaseUtil.populateUniqueFieldsTable();

        final List<Map<String, Object>> uniqueFieldsRegisters = getUniqueFieldRecords(contentType);
        assertEquals(3, uniqueFieldsRegisters.size());

        for (Map<String, Object> uniqueFieldsRegister : uniqueFieldsRegisters) {
            final Map<String, Object> supportingValues = getSupportingValues(uniqueFieldsRegister);

            final List<String> contentletIds = (List<String>) supportingValues.get(CONTENTLET_IDS_ATTR);
            final Boolean live = Boolean.valueOf(supportingValues.get(LIVE_ATTR) != null ?
                    supportingValues.get(LIVE_ATTR).toString() : "false");

            assertEquals(1, contentletIds.size());

            final String contentId = contentletIds.get(0);

            if (contentlet_1.getIdentifier().equals(contentId) && live) {
                final String hash = calculateHash(contentlet_1, language, uniqueTextField, host_1, liveUniqueValue);
                assertEquals(hash, uniqueFieldsRegister.get("unique_key_val") );
                assertEquals(host_1.getIdentifier(), supportingValues.get("siteId") );
            } else if (contentlet_1.getIdentifier().equals(contentId) && !live) {
                final String hash = calculateHash(workingContentlet, language, uniqueTextField, host_1, workingUniqueValue);
                assertEquals(hash, uniqueFieldsRegister.get("unique_key_val") );
                assertEquals(host_1.getIdentifier(), supportingValues.get("siteId") );
            } else if (contentlet_2.getIdentifier().equals(contentId) && live) {
                final String hash = calculateHash(contentlet_2, language, uniqueTextField, host_2, liveUniqueValue);
                assertEquals(hash, uniqueFieldsRegister.get("unique_key_val") );
                assertEquals(host_2.getIdentifier(), supportingValues.get("siteId") );
            } else {
                throw new AssertException("Contentlet don't expected");
            }

            assertEquals(true, supportingValues.get("uniquePerSite") );
        }
    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#populateUniqueFieldsTable()}
     * When:
     * - Create a {@link ContentType} with a field called unique but for now it's not going to be unique
     * - Create a couple of {@link Contentlet} with the same value for the unique field.
     * - Update the unique Field and set it as unique
     * - Run the populate method
     * Should: Create a register with 2 contentlets
     *
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    public void multiContentletWithTheSameValue() throws SQLException, DotDataException, IOException, DotSecurityException {
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        final Connection connection = DbConnectionFactory.getConnection();

        final String uniqueValue =  "nique_value";

        final Language language = new LanguageDataGen().nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .type(TextField.class)
                .next();

        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen()
                .host(host)
                .fields(list(uniqueTextField))
                .nextPersisted();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), uniqueValue)
                .nextPersistedAndPublish();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), uniqueValue)
                .nextPersistedAndPublish();

        final Field uniqueTextFieldFromDB = APILocator.getContentTypeFieldAPI()
                .byContentTypeAndVar(contentType, uniqueTextField.variable());

        final ImmutableTextField uniqueFieldUpdated = ImmutableTextField.builder()
                .from(uniqueTextField)
                .contentTypeId(contentType.id())
                .unique(true)
                .build();

        APILocator.getContentTypeFieldAPI().save(uniqueFieldUpdated, APILocator.systemUser());

        dotDatabaseMetaData.dropTable(connection, "unique_fields");

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();
        uniqueFieldDataBaseUtil.addPrimaryKeyConstraintsBack();

        assertTrue(getUniqueFieldRecords(contentType).isEmpty());

        uniqueFieldDataBaseUtil.populateUniqueFieldsTable();

        final List<Map<String, Object>> uniqueFieldsRegisters = getUniqueFieldRecords(contentType);
        assertEquals(1, uniqueFieldsRegisters.size());

        final Map<String, Object> uniqueFieldsRegister = uniqueFieldsRegisters.get(0);
        final Map<String, Object> supportingValues = getSupportingValues(uniqueFieldsRegister);

        final List<String> contentletIds = (List<String>) supportingValues.get(CONTENTLET_IDS_ATTR);
        assertEquals(2, contentletIds.size());
        assertTrue(contentletIds.contains(contentlet_1.getIdentifier()));
        assertTrue(contentletIds.contains(contentlet_2.getIdentifier()));
    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#populateUniqueFieldsTable()}
     * When:
     * - Create a {@link ContentType} with a field called unique but for now it's not going to be unique
     * - Create a couple of {@link Contentlet} with the same value for the unique field in different sites.
     * - Update the unique Field and set it as unique with the UNIQUE_PER_SITES variables set to false
     * - Run the populate method
     * Should: Create a register with 2 contentlets
     *
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    public void multiContentletWithTheSameValueInDifferentSites() throws SQLException, DotDataException, IOException, DotSecurityException {
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        final Connection connection = DbConnectionFactory.getConnection();

        final String uniqueValue =  "unique_value";

        final Language language = new LanguageDataGen().nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .type(TextField.class)
                .next();

        Host systemHost = APILocator.getHostAPI().findSystemHost();
        final Host host_1 = new SiteDataGen().nextPersisted();
        final Host host_2 = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen()
                .host(systemHost)
                .fields(list(uniqueTextField))
                .nextPersisted();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                .host(host_1)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), uniqueValue)
                .nextPersistedAndPublish();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .host(host_2)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), uniqueValue)
                .nextPersistedAndPublish();

        final Field uniqueTextFieldFromDB = APILocator.getContentTypeFieldAPI()
                .byContentTypeAndVar(contentType, uniqueTextField.variable());

        final ImmutableTextField uniqueFieldUpdated = ImmutableTextField.builder()
                .from(uniqueTextField)
                .contentTypeId(contentType.id())
                .unique(true)
                .build();

        APILocator.getContentTypeFieldAPI().save(uniqueFieldUpdated, APILocator.systemUser());

        dotDatabaseMetaData.dropTable(connection, "unique_fields");

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();
        uniqueFieldDataBaseUtil.addPrimaryKeyConstraintsBack();

        assertTrue(getUniqueFieldRecords(contentType).isEmpty());

        uniqueFieldDataBaseUtil.populateUniqueFieldsTable();

        final List<Map<String, Object>> uniqueFieldsRegisters = getUniqueFieldRecords(contentType);
        assertEquals(1, uniqueFieldsRegisters.size());

        final Map<String, Object> uniqueFieldsRegister = uniqueFieldsRegisters.get(0);
        final Map<String, Object> supportingValues = getSupportingValues(uniqueFieldsRegister);

        final List<String> contentletIds = (List<String>) supportingValues.get(CONTENTLET_IDS_ATTR);
        assertEquals(2, contentletIds.size());
        assertTrue(contentletIds.contains(contentlet_1.getIdentifier()));
        assertTrue(contentletIds.contains(contentlet_2.getIdentifier()));
    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#populateUniqueFieldsTable()}
     * When:
     * - Create a {@link ContentType} with a field called unique but for now it's not going to be unique
     * - Create a couple of {@link Contentlet} with the same value for the unique field in different sites.
     * - Update the unique Field and set it as unique with the UNIQUE_PER_SITES variables set to true
     * - Run the populate method
     * Should: Create a register with 2 contentlets
     *
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    @SuppressWarnings("unchecked")
    public void multiContentletWithTheSameValueInDifferentSitesAndUniquePerSiteTrue()
            throws SQLException, DotDataException, DotSecurityException {
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        final Connection connection = DbConnectionFactory.getConnection();

        final String uniqueValue =  "unique_value";

        final Language language = new LanguageDataGen().nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .type(TextField.class)
                .next();

        Host systemHost = APILocator.getHostAPI().findSystemHost();
        final Host site_1 = new SiteDataGen().nextPersisted();
        final Host site_2 = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen()
                .host(systemHost)
                .fields(list(uniqueTextField))
                .nextPersisted();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                .host(site_1)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), uniqueValue)
                .nextPersistedAndPublish();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .host(site_2)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), uniqueValue)
                .nextPersistedAndPublish();

        final ImmutableTextField uniqueFieldUpdated = ImmutableTextField.builder()
                .from(uniqueTextField)
                .contentTypeId(contentType.id())
                .unique(true)
                .build();

        APILocator.getContentTypeFieldAPI().save(uniqueFieldUpdated, APILocator.systemUser());

        final Field uniqueTextFieldFromDB = APILocator.getContentTypeFieldAPI()
                .byContentTypeAndVar(contentType, uniqueTextField.variable());

        new FieldVariableDataGen()
                .key(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                .value("true")
                .field(uniqueTextFieldFromDB)
                .nextPersisted();

        dotDatabaseMetaData.dropTable(connection, "unique_fields");
        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();
        uniqueFieldDataBaseUtil.addPrimaryKeyConstraintsBack();
        assertTrue("There must be NO records after dropping the unique fields table", getUniqueFieldRecords(contentType).isEmpty());

        uniqueFieldDataBaseUtil.populateUniqueFieldsTable();
        final List<Map<String, Object>> uniqueFieldRecords = getUniqueFieldRecords(contentType);
        assertEquals("There must be exactly 2 records after populating the unique fields table", 2, uniqueFieldRecords.size());

        final List<String> contentletIds = uniqueFieldRecords.stream()
                .map(UniqueFieldDataBaseUtilTest::getSupportingValues)
                .map(supportingValues ->  (List<String>) supportingValues.get("contentletIds"))
                .map(supportingValuesContentletIds -> {
                    assertEquals(1, supportingValuesContentletIds.size());
                    return supportingValuesContentletIds.get(0);
                })
                .collect(Collectors.toList());

        assertEquals("The returned list must have exactly two records", 2, contentletIds.size());
        assertTrue("The returned list must contain the first test Contentlet ID", contentletIds.contains(contentlet_1.getIdentifier()));
        assertTrue("The returned list must contain the second test Contentlet ID", contentletIds.contains(contentlet_2.getIdentifier()));
    }

    private static Map<String, Object> getSupportingValues(Map<String, Object> uniqueFieldRegister) {
        try {
            return JsonUtil.getJsonFromString(uniqueFieldRegister.get("supporting_values").toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#populateUniqueFieldsTable()}
     * When:
     * - Create a {@link ContentType} with a Unique {@link Field} with the uniquePerSite set to FALSE
     * - Create a {@link Contentlet} with version in a specific Variant and DEFAULT Variant, each one with the same value.
     * - Populate the unique_fields table.
     * Should: Create a couple of records with one with the DEFAULT Variant and the other one to the Specific Variant
     * and uniquePerSite equals FALSE.
     *
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    @SuppressWarnings("unchecked")
    //TODO: WE'RE TEMPORARILY IGNORING THIS TEST BECAUSE IT DEPENDS ON THE CODE FIX
    // FOM TICKET https://github.com/dotCMS/core/issues/32309
    public void populateUniqueFieldsTableWithVariantWithSameValues() throws SQLException, DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();

        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        final Connection connection = DbConnectionFactory.getConnection();

        final String uniqueValue = "unique_value";

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Language language = new LanguageDataGen().nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                .nextPersisted();

        final Host site = new SiteDataGen().nextPersisted();

        final Contentlet defaultContentlet = new ContentletDataGen(contentType)
                .host(site)
                .languageId(language.getId())
                .variant(VariantAPI.DEFAULT_VARIANT)
                .setProperty(uniqueTextField.variable(), uniqueValue)
                .nextPersisted();

        Contentlet specificVariantContentlet = ContentletDataGen.checkout(defaultContentlet);
        specificVariantContentlet.setProperty(uniqueTextField.variable(), uniqueValue);
        specificVariantContentlet.setVariantId(variant.name());
        ContentletDataGen.checkin(specificVariantContentlet);

        dotDatabaseMetaData.dropTable(connection, "unique_fields");
        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();
        uniqueFieldDataBaseUtil.addPrimaryKeyConstraintsBack();
        assertTrue("There must be NO records after dropping the unique fields table", getUniqueFieldRecords(contentType).isEmpty());

        uniqueFieldDataBaseUtil.populateUniqueFieldsTable();

        final List<Map<String, Object>> uniqueFieldRecords = getUniqueFieldRecords(contentType);
        assertEquals("There must be exactly 1 record after populating the unique fields table", 1, uniqueFieldRecords.size());

        final Map<String, Object> uniqueFieldRecord = uniqueFieldRecords.get(0);
        final Map<String, Object> supportingValues = getSupportingValues(uniqueFieldRecord);
        assertEquals("The existing unique field record must have its 'live' attribute set to 'false'", false, Boolean.valueOf(supportingValues.get(LIVE_ATTR) != null ?
                supportingValues.get(LIVE_ATTR).toString() : "false"));

        final List<String> contentletIds = (List<String>) supportingValues.get(CONTENTLET_IDS_ATTR);
        assertEquals("There must be only 1 associated ID in the unique field record", 1, contentletIds.size());

        final String contentId = contentletIds.get(0);
        assertEquals("The associated Contentlet ID in the unique field record must match the test Contentlet ID", defaultContentlet.getIdentifier(), contentId);

        final String hash = calculateHash(defaultContentlet, language, uniqueTextField, site, uniqueValue);
        assertEquals("The hash of the test Contentlet must match the hash in the unique field record", hash, uniqueFieldRecord.get("unique_key_val") );
    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#populateUniqueFieldsTable()}
     * When:
     * - Create a {@link ContentType} with a Unique {@link Field} with the uniquePerSite set to FALSE
     * - Create a {@link Contentlet} with version in a specific Variant and DEFAULT Variant, each one with different value.
     * - Populate the unique_fields table.
     * Should: Create a couple of register with one with the LIVE value and the other one with the WORKING value
     * and uniquePerSite equals to TRUE.
     *
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    public void populateUniqueFieldsTableWithVariantWithDifferentValues() throws SQLException, DotDataException, IOException, DotSecurityException {
        final Variant variant = new VariantDataGen().nextPersisted();

        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        final Connection connection = DbConnectionFactory.getConnection();

        final String defaultUniqueValue = "default_variant_unique_value";
        final String specificUniqueValue = "specific_variant_unique_value";

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Language language = new LanguageDataGen().nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                .nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final Contentlet defaultContentlet = new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), defaultUniqueValue)
                .nextPersisted();

        final Contentlet newVersion = ContentletDataGen.createNewVersion(defaultContentlet, variant, language,
                Map.of(uniqueTextField.variable(), specificUniqueValue));

        dotDatabaseMetaData.dropTable(connection, "unique_fields");

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();
        uniqueFieldDataBaseUtil.addPrimaryKeyConstraintsBack();

        assertTrue(getUniqueFieldRecords(contentType).isEmpty());

        uniqueFieldDataBaseUtil.populateUniqueFieldsTable();

        final List<Map<String, Object>> uniqueFieldsRegisters = getUniqueFieldRecords(contentType);
        assertEquals(2, uniqueFieldsRegisters.size());

        for (Map<String, Object> uniqueFieldsRegister : uniqueFieldsRegisters) {
            final Map<String, Object> supportingValues = getSupportingValues(uniqueFieldsRegister);

            final List<String> contentletIds = (List<String>) supportingValues.get(CONTENTLET_IDS_ATTR);

            assertEquals(1, contentletIds.size());

            final String contentId = contentletIds.get(0);
            assertEquals(defaultContentlet.getIdentifier(), contentId);
            assertEquals(false, supportingValues.get("live"));

            if (VariantAPI.DEFAULT_VARIANT.name().equals(supportingValues.get("variant"))) {
                final String hash = calculateHash(defaultContentlet, language, uniqueTextField, host, defaultUniqueValue);
                assertEquals(hash, uniqueFieldsRegister.get("unique_key_val") );
            } else if (variant.name().equals(supportingValues.get("variant"))) {
                final String hash = calculateHash(newVersion, language, uniqueTextField, host, specificUniqueValue);
                assertEquals(hash, uniqueFieldsRegister.get("unique_key_val") );
            } else {
                throw new AssertException("Contentlet don't expected");
            }

            assertEquals(false, supportingValues.get("uniquePerSite") );
        }
    }

    /**
     *
     * @param contentType
     * @return
     * @throws DotDataException
     */
    private List<Map<String, Object>> getUniqueFieldRecords(final ContentType contentType) throws DotDataException {
        return new DotConnect().setSQL("SELECT * FROM unique_fields WHERE supporting_values->>'contentTypeId' = ?")
                .addParam(contentType.id()).loadObjectResults();
    }

    private List<Map<String, Object>> getUniqueFieldRecordsWithSameHash() throws DotDataException {
        return new DotConnect().setSQL(SqlQueries.GET_RECORDS_WITH_SAME_HASH).loadObjectResults();
    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#get(String, long)}
     * when: Try to get the Unique value using a too long language ID
     * should: get the register and not throw any exception
     *
     * @throws IOException
     * @throws DotDataException
     */
    @Test
    public void getUniqueValueWithLanguageWithTooLongId() throws IOException, DotDataException {
        final String defaultUniqueValue = "default_variant_unique_value";

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Language language = new LanguageDataGen().nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                .nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final Contentlet defaultContentlet = new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), defaultUniqueValue)
                .nextPersisted();

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        final String supportingValueJSON = "{\"live\": true, \"siteId\": \"SYSTEM_HOST\", \"variant\": \"DEFAULT\", \"fieldValue\": \"System Host\", \"languageId\": 1565640883097, \"contentTypeId\": \"855a2d72-f2f3-4169-8b04-ac5157c4380c\", \"contentletIds\": [\"" + defaultContentlet.getIdentifier() + "\"], \"uniquePerSite\": false, \"fieldVariableName\": \"hostName\"}";
        final Map<String, Object> jsonFromString = JsonUtil.getJsonFromString(supportingValueJSON);

        uniqueFieldDataBaseUtil.insert("1", jsonFromString);

        List<Map<String, Object>> maps = uniqueFieldDataBaseUtil.get(defaultContentlet.getIdentifier(), language.getId());
        assertNotNull(maps);
    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#get(Contentlet, Field)}
     * when: Try to get register from unique_fields table when exists a register with a too long languege_id
     * should: get the register and not throw any exception
     *
     * @throws IOException
     * @throws DotDataException
     */
    @Test
    public void getUniqueValueWithFieldWithTooLongId() throws IOException, DotDataException {
        final String defaultUniqueValue = "default_variant_unique_value";

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Language language = new LanguageDataGen().nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                .nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final Contentlet defaultContentlet = new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), defaultUniqueValue)
                .nextPersisted();

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        final String supportingValueJSON = "{\"live\": true, \"siteId\": \"SYSTEM_HOST\", \"variant\": \"DEFAULT\", \"fieldValue\": \"System Host\", \"languageId\": 1565640883097, \"contentTypeId\": \"855a2d72-f2f3-4169-8b04-ac5157c4380c\", \"contentletIds\": [\"" + defaultContentlet.getIdentifier() + "\"], \"uniquePerSite\": false, \"fieldVariableName\": \"" + uniqueTextField.variable() + "\"}";
        final Map<String, Object> jsonFromString = JsonUtil.getJsonFromString(supportingValueJSON);

        uniqueFieldDataBaseUtil.insert(String.valueOf(System.currentTimeMillis()), jsonFromString);

        List<Map<String, Object>> maps = uniqueFieldDataBaseUtil.get(defaultContentlet, uniqueTextField);
        assertNotNull(maps);
    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#setLive(Contentlet, boolean)}
     * when: Try to set live attribute to a register in the unique_fields table when exists a register with a too long languege_id
     * should: get the register and not throw any exception
     *
     * @throws IOException
     * @throws DotDataException
     */
    @Test
    public void setLiveUniqueValueWithTooLongId() throws IOException, DotDataException {
        final String defaultUniqueValue = "default_variant_unique_value";

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Language language = new LanguageDataGen().nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                .nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final Contentlet defaultContentlet = new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), defaultUniqueValue)
                .nextPersisted();

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        final String supportingValueJSON = "{\"live\": false, \"siteId\": \"SYSTEM_HOST\", \"variant\": \"DEFAULT\", \"fieldValue\": \"System Host\", \"languageId\": 1565640883097, \"contentTypeId\": \"855a2d72-f2f3-4169-8b04-ac5157c4380c\", \"contentletIds\": [\"" + defaultContentlet.getIdentifier() + "\"], \"uniquePerSite\": false, \"fieldVariableName\": \"hostName\"}";
        final Map<String, Object> jsonFromString = JsonUtil.getJsonFromString(supportingValueJSON);

        uniqueFieldDataBaseUtil.insert(String.valueOf(System.currentTimeMillis()), jsonFromString);

        uniqueFieldDataBaseUtil.setLive(defaultContentlet, true);
    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#setLive(Contentlet, boolean)}
     * when: Try to remove live attribute to a register in the unique_fields table when exists a register with a too long languege_id
     * should: get the register and not throw any exception
     *
     * @throws IOException
     * @throws DotDataException
     */
    @Test
    public void removeLiveUniqueValueWithTooLongId() throws IOException, DotDataException {
        final String defaultUniqueValue = "default_variant_unique_value";

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Language language = new LanguageDataGen().nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                .nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final Contentlet defaultContentlet = new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), defaultUniqueValue)
                .nextPersisted();

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        final String supportingValueJSON = "{\"live\": true, \"siteId\": \"SYSTEM_HOST\", \"variant\": \"DEFAULT\", \"fieldValue\": \"System Host\", \"languageId\": 1565640883097, \"contentTypeId\": \"855a2d72-f2f3-4169-8b04-ac5157c4380c\", \"contentletIds\": [\"" + defaultContentlet.getIdentifier() + "\"], \"uniquePerSite\": false, \"fieldVariableName\": \"hostName\"}";
        final Map<String, Object> jsonFromString = JsonUtil.getJsonFromString(supportingValueJSON);

        uniqueFieldDataBaseUtil.insert(String.valueOf(System.currentTimeMillis()), jsonFromString);

        uniqueFieldDataBaseUtil.removeLive(defaultContentlet);
    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#get(UniqueFieldCriteria)}
     * when: Save a Unique fields value
     * Should: be able to retrieve it from the hash
     */
    @Test
    public void getFromHash() throws DotDataException {
        final String uniqueValue = "unique_value";

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Language language = new LanguageDataGen().nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                .nextPersisted();

        final Host site = new SiteDataGen().nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .host(site)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), uniqueValue)
                .nextPersisted();

        final UniqueFieldCriteria uniqueFieldCriteria = new Builder().setVariantName(contentlet.getVariantId())
                .setLanguage(language)
                .setContentType(contentlet.getContentType())
                .setField(uniqueTextField)
                .setSite(site)
                .setLive(false)
                .setValue(uniqueValue)
                .build();

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();

        final UniqueFieldDataBaseUtil.UniqueFieldValue uniqueFieldValue = uniqueFieldDataBaseUtil.get(uniqueFieldCriteria)
                .orElseThrow();

        final Map<String, Object> supportingValuesFromDB = uniqueFieldValue.getSupportingValues();

        assertEquals("The unique value does not match the one in the database", uniqueValue, supportingValuesFromDB.get(FIELD_VALUE_ATTR));
        assertEquals("The language ID does not match the one in the database", language.getId(), Long.parseLong(supportingValuesFromDB.get(LANGUAGE_ID_ATTR).toString()));
        assertEquals("The content type ID does not match the one in the database", contentType.id(), supportingValuesFromDB.get(CONTENT_TYPE_ID_ATTR));
        assertEquals("The field variable does not match the one in the database", uniqueTextField.variable(), supportingValuesFromDB.get(FIELD_VARIABLE_NAME_ATTR));
        assertEquals("The site ID does not match the one in the database", site.getIdentifier(), supportingValuesFromDB.get(SITE_ID_ATTR));
        assertEquals("The live status does not match the one in the database", false, supportingValuesFromDB.get(LIVE_ATTR));

        assertEquals("There must be only 1 associated Contentlet ID", 1, ((Collection<?>) supportingValuesFromDB.get(CONTENTLET_IDS_ATTR)).size());
        assertTrue("The associated Contentlet Id must be in the list of related IDs", ((Collection<?>) supportingValuesFromDB.get(CONTENTLET_IDS_ATTR)).contains(contentlet.getIdentifier()));
    }


    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#delete(Field)}
     * when: try to delete a unique_fields register using the hash
     * Should: delete it
     */
    @Test
    public void deleteByHash() throws DotDataException {
        final String uniqueValue = "unique_value";

        final ContentType contentType = new ContentTypeDataGen()
                .nextPersisted();

        final Language language = new LanguageDataGen().nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .contentTypeId(contentType.id())
                .unique(true)
                .type(TextField.class)
                .nextPersisted();

        final Host site = new SiteDataGen().nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .host(site)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), uniqueValue)
                .nextPersisted();

        final UniqueFieldCriteria uniqueFieldCriteria = new Builder().setVariantName(contentlet.getVariantId())
                .setLanguage(language)
                .setContentType(contentlet.getContentType())
                .setField(uniqueTextField)
                .setSite(site)
                .setLive(false)
                .setValue(uniqueValue)
                .build();

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();

        assertTrue("The unique field record must be present", uniqueFieldDataBaseUtil.get(uniqueFieldCriteria).isPresent());

        uniqueFieldDataBaseUtil.delete(StringUtils.hashText(uniqueFieldCriteria.criteria()));

        assertTrue("The unique field record must have been deleted by now", uniqueFieldDataBaseUtil.get(uniqueFieldCriteria).isEmpty());
    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#populateUniqueFieldsTable()}
     * When:
     * - Create a {@link ContentType} with a UNIQUE field
     * - Create a couple of {@link Contentlet} with a value of A
     * - Drop the unique_fields table, and run the populate method
     * - Try to create a Contentlet with unique equals to 'a'
     * Should: throw a DuplicationException
     *
     * @throws SQLException
     * @throws DotDataException
     */
    @Test()
    public void populatedUniqueDataMustBeCaseInsensitive() throws  DotDataException {
        final String uniqueValue =  "A";

        final Language language = new LanguageDataGen().nextPersisted();

        final Field uniqueTextField = new FieldDataGen()
                .type(TextField.class)
                .unique(true)
                .next();

        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen()
                .host(host)
                .fields(list(uniqueTextField))
                .nextPersisted();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), uniqueValue)
                .nextPersistedAndPublish();

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.dropUniqueFieldsValidationTable();
        uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();
        uniqueFieldDataBaseUtil.addPrimaryKeyConstraintsBack();
        uniqueFieldDataBaseUtil.populateUniqueFieldsTable();

        try {
            final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                    .host(host)
                    .languageId(language.getId())
                    .setProperty(uniqueTextField.variable(), uniqueValue.toLowerCase())
                    .nextPersistedAndPublish();

            throw new AssertionError("The unique field record must be present");
        } catch(RuntimeException e) {
            if (!(e.getCause() instanceof DotContentletValidationException)) {
                throw new AssertionError("DotContentletValidationException expected");
            }
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UniqueFieldDataBaseUtil#handleDuplicateRecords()}</li>
     *     <li><b>Given Scenario:</b> Manually insert four records in the {@code unique_fields}
     *     table having the exact same hash, but point to different Contentlet Identifiers. This
     *     simulates the scenario in which different contents have the same unique value, which
     *     should NEVER happen but was being allowed by race conditions with Elasticsearch.</li>
     *     <li><b>Expected Result:</b> The method to test will detect the duplicates and manually
     *     fixes the conflicts. It leaves only one of the duplicates, updates the unique value of
     *     the remaining ones, and re-generates their hashes so they can be inserted in the table
     *     without problems. This error is NOT supposed to happen anymore.</li>
     * </ul>
     *
     * @throws SQLException     An error occurred when checking the DBMS metadata.
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @Test
    public void fixDuplicateEntriesWithDifferentContentIdentifiers() throws DotDataException, DotSecurityException, SQLException {
        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        final Connection connection = DbConnectionFactory.getConnection();
        final String uniqueValue = "duplicate_unique_value";
        final String siteId = UUIDGenerator.generateUuid();
        final String testContentIdOne = UUIDGenerator.generateUuid();
        final String testContentIdTwo = UUIDGenerator.generateUuid();
        final String testContentIdThree = UUIDGenerator.generateUuid();
        final String testContentIdFour = UUIDGenerator.generateUuid();
        final long languageId = 1;

        // ╔════════════════════════╗
        // ║  Generating Test data  ║
        // ╚════════════════════════╝
        final Host site = new SiteDataGen().nextPersisted();
        final Field uniqueTextField = new FieldDataGen()
                .type(TextField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .host(site)
                .fields(list(uniqueTextField))
                .nextPersisted();

        final ImmutableTextField uniqueFieldUpdated = ImmutableTextField.builder()
                .from(uniqueTextField)
                .contentTypeId(contentType.id())
                .unique(true)
                .build();
        APILocator.getContentTypeFieldAPI().save(uniqueFieldUpdated, APILocator.systemUser());
        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        try {
            dotDatabaseMetaData.dropTable(connection, "unique_fields");
            uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();

            // Generating the duplicate hash key
            final String uniqueKeyValue = contentType.id() + uniqueTextField.variable() + languageId + uniqueValue;

            // Generating the duplicate record for the first Contentlet
            assertNotNull(VariantAPI.DEFAULT_VARIANT.name());
            final Map<String, Object> supportingValues = new HashMap<>(Map.of(
                    CONTENTLET_IDS_ATTR, List.of(testContentIdOne),
                    CONTENT_TYPE_ID_ATTR, Objects.requireNonNull(contentType.id()),
                    FIELD_VARIABLE_NAME_ATTR, Objects.requireNonNull(uniqueTextField.variable()),
                    FIELD_VALUE_ATTR, uniqueValue,
                    LANGUAGE_ID_ATTR, languageId,
                    UNIQUE_PER_SITE_ATTR, false,
                    VARIANT_ATTR, VariantAPI.DEFAULT_VARIANT.name(),
                    LIVE_ATTR, true,
                    SITE_ID_ATTR, siteId
            ));
            uniqueFieldDataBaseUtil.insert(uniqueKeyValue, supportingValues);

            // Generating the duplicate unique value record for the second Contentlet
            supportingValues.put(CONTENTLET_IDS_ATTR, List.of(testContentIdTwo));
            uniqueFieldDataBaseUtil.insert(uniqueKeyValue, supportingValues);
            // Generating the duplicate unique value record for the third Contentlet
            supportingValues.put(CONTENTLET_IDS_ATTR, List.of(testContentIdThree));
            uniqueFieldDataBaseUtil.insert(uniqueKeyValue, supportingValues);
            // Generating the duplicate unique value record for the fourth Contentlet
            supportingValues.put(CONTENTLET_IDS_ATTR, List.of(testContentIdFour));
            uniqueFieldDataBaseUtil.insert(uniqueKeyValue, supportingValues);

            // ╔══════════════╗
            // ║  Assertions  ║
            // ╚══════════════╝
            List<Map<String, Object>> recordCountWithSameHash = this.getUniqueFieldRecordsWithSameHash();
            assertEquals("There must be exactly 4 duplicate records", 4,
                    Integer.parseInt(recordCountWithSameHash.get(0).get("count").toString()));

            uniqueFieldDataBaseUtil.handleDuplicateRecords();

            recordCountWithSameHash = this.getUniqueFieldRecordsWithSameHash();
            assertEquals("There must be NO duplicate records as they all should've been fixed",
                    0, recordCountWithSameHash.size());
        } finally {
            dotDatabaseMetaData.dropTable(connection, "unique_fields");
            uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();
            uniqueFieldDataBaseUtil.addPrimaryKeyConstraintsBack();
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UniqueFieldDataBaseUtil#handleDuplicateRecords()}</li>
     *     <li><b>Given Scenario:</b> Manually insert two records in the {@code unique_fields} table
     *     having the exact same hash because a single Contentlet can have both a live and a working
     *     version. This duplicate record handling just applies the first time the table is being
     *     populated. The API already handles this situation correctly.</li>
     *     <li><b>Expected Result:</b> The method to test will detect the duplicates and manually
     *     fixes the conflicts. It leaves only one of the duplicates, updates the unique value of
     *     the remaining ones, and re-generates their hashes so they can be inserted in the table
     *     without problems. This error is NOT supposed to happen anymore</li>
     * </ul>
     *
     * @throws SQLException     An error occurred when checking the DBMS metadata.
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @Test
    public void fixDuplicateEntriesWithSameContentInLiveAndWorking() throws DotDataException, DotSecurityException, SQLException {
        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        final Connection connection = DbConnectionFactory.getConnection();
        final String uniqueValue = "duplicate_unique_value";
        final String siteId = UUIDGenerator.generateUuid();
        final String testContentIdOne = UUIDGenerator.generateUuid();
        final long languageId = 1;

        // ╔════════════════════════╗
        // ║  Generating Test data  ║
        // ╚════════════════════════╝
        final Host site = new SiteDataGen().nextPersisted();
        final Field uniqueTextField = new FieldDataGen()
                .type(TextField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .host(site)
                .fields(list(uniqueTextField))
                .nextPersisted();

        final ImmutableTextField uniqueFieldUpdated = ImmutableTextField.builder()
                .from(uniqueTextField)
                .contentTypeId(contentType.id())
                .unique(true)
                .build();
        APILocator.getContentTypeFieldAPI().save(uniqueFieldUpdated, APILocator.systemUser());
        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        try {
            dotDatabaseMetaData.dropTable(connection, "unique_fields");
            uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();

            // Generating the duplicate hash key
            final String uniqueKeyValue = contentType.id() + uniqueTextField.variable() + languageId + uniqueValue;

            // Generating the duplicate record for the first Contentlet
            assertNotNull(VariantAPI.DEFAULT_VARIANT.name());
            final Map<String, Object> supportingValues = new HashMap<>(Map.of(
                    CONTENTLET_IDS_ATTR, List.of(testContentIdOne),
                    CONTENT_TYPE_ID_ATTR, Objects.requireNonNull(contentType.id()),
                    FIELD_VARIABLE_NAME_ATTR, Objects.requireNonNull(uniqueTextField.variable()),
                    FIELD_VALUE_ATTR, uniqueValue,
                    LANGUAGE_ID_ATTR, languageId,
                    UNIQUE_PER_SITE_ATTR, false,
                    VARIANT_ATTR, VariantAPI.DEFAULT_VARIANT.name(),
                    LIVE_ATTR, true,
                    SITE_ID_ATTR, siteId
            ));
            uniqueFieldDataBaseUtil.insert(uniqueKeyValue, supportingValues);

            // Generating the duplicate record for the same Contentlet as a working version
            supportingValues.put(LIVE_ATTR, false);
            uniqueFieldDataBaseUtil.insert(uniqueKeyValue, supportingValues);

            // ╔══════════════╗
            // ║  Assertions  ║
            // ╚══════════════╝
            List<Map<String, Object>> recordCountWithSameHash = this.getUniqueFieldRecordsWithSameHash();
            assertEquals("There must be exactly 2 duplicate records", 2,
                    Integer.parseInt(recordCountWithSameHash.get(0).get("count").toString()));

            uniqueFieldDataBaseUtil.handleDuplicateRecords();

            recordCountWithSameHash = this.getUniqueFieldRecordsWithSameHash();
            assertEquals("There must be NO duplicate records as they all should've been fixed",
                    0, recordCountWithSameHash.size());
        } finally {
            dotDatabaseMetaData.dropTable(connection, "unique_fields");
            uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();
            uniqueFieldDataBaseUtil.addPrimaryKeyConstraintsBack();
        }
    }

}
