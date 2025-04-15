package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotcms.JUnit4WeldRunner;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
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
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import graphql.AssertException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.*;
import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.*;

@ApplicationScoped
@RunWith(JUnit4WeldRunner.class)
public class UniqueFieldDataBaseUtilTest {

    @BeforeClass
    public static void init () throws Exception {
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
     * Method to test: {@link UniqueFieldDataBaseUtil#createUniqueFieldsValidationTable()}
     * When: call this method
     * Should: create the unique_fields table with the right columns
     *
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    public void createUniqueFieldsTable() throws SQLException, DotDataException {
        final Connection connection = DbConnectionFactory.getConnection();
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        dotDatabaseMetaData.dropTable(connection, "unique_fields");
        assertFalse(dotDatabaseMetaData.tableExists(connection, "unique_fields"));

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();

        assertTrue(dotDatabaseMetaData.tableExists(connection, "unique_fields"));

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
                throw new AssertException("Column no valid");
            }
        }


        final List<String> primaryKeysFields = DotDatabaseMetaData.getPrimaryKeysFields("unique_fields");
        assertEquals(1, primaryKeysFields.size());
        assertTrue(primaryKeysFields.contains("unique_key_val"));

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

        assertTrue(getUniqueFieldsRegisters(contentType).isEmpty());

        uniqueFieldDataBaseUtil.populateUniqueFieldsTable();

        final List<Map<String, Object>> uniqueFieldsRegisters = getUniqueFieldsRegisters(contentType);
        assertEquals(3, uniqueFieldsRegisters.size());

        for (Map<String, Object> uniqueFieldsRegister : uniqueFieldsRegisters) {
            final Map<String, Object> supportingValues = JsonUtil.getJsonFromString(uniqueFieldsRegister.get("supporting_values").toString());

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

        assertTrue(getUniqueFieldsRegisters(contentType).isEmpty());

        uniqueFieldDataBaseUtil.populateUniqueFieldsTable();

        final List<Map<String, Object>> uniqueFieldsRegisters = getUniqueFieldsRegisters(contentType);
        assertEquals(1, uniqueFieldsRegisters.size());

        final Map<String, Object> uniqueFieldsRegister = uniqueFieldsRegisters.get(0);
        final Map<String, Object> supportingValues = JsonUtil.getJsonFromString(uniqueFieldsRegister.get("supporting_values").toString());

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

        assertTrue(getUniqueFieldsRegisters(contentType).isEmpty());

        uniqueFieldDataBaseUtil.populateUniqueFieldsTable();

        final List<Map<String, Object>> uniqueFieldsRegisters = getUniqueFieldsRegisters(contentType);
        assertEquals(3, uniqueFieldsRegisters.size());

        for (Map<String, Object> uniqueFieldsRegister : uniqueFieldsRegisters) {
            final Map<String, Object> supportingValues = JsonUtil.getJsonFromString(uniqueFieldsRegister.get("supporting_values").toString());

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

        assertTrue(getUniqueFieldsRegisters(contentType).isEmpty());

        uniqueFieldDataBaseUtil.populateUniqueFieldsTable();

        final List<Map<String, Object>> uniqueFieldsRegisters = getUniqueFieldsRegisters(contentType);
        assertEquals(1, uniqueFieldsRegisters.size());

        final Map<String, Object> uniqueFieldsRegister = uniqueFieldsRegisters.get(0);
        final Map<String, Object> supportingValues = JsonUtil.getJsonFromString(uniqueFieldsRegister.get("supporting_values").toString());

        final List<String> contentletIds = (List<String>) supportingValues.get(CONTENTLET_IDS_ATTR);
        assertEquals(2, contentletIds.size());
        assertTrue(contentletIds.contains(contentlet_1.getIdentifier()));
        assertTrue(contentletIds.contains(contentlet_2.getIdentifier()));
    }

    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#populateUniqueFieldsTable()}
     * When:
     * - Create a {@link ContentType} with a Unique {@link Field} with the uniquePerSite set to FALSE
     * - Create a {@link Contentlet} with version in a specific Variant and DEFAULT Variant, each one with different value.
     * - Populate the unique_fields table.
     * Should: Create a couple of register with one with the DEFAULT Variant and the other one to the Specific Variant
     * and uniquePerSite equals to TRUE.
     *
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    public void populateUniqueFieldsTableWithVariantWithSameValues() throws SQLException, DotDataException, IOException {
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

        final Host host = new SiteDataGen().nextPersisted();

        final Contentlet defaultContentlet = new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), uniqueValue)
                .nextPersisted();

        Contentlet specificVariantContentlet = ContentletDataGen.checkout(defaultContentlet);
        specificVariantContentlet.setProperty(uniqueTextField.variable(), uniqueValue);
        specificVariantContentlet.setVariantId(variant.name());
        ContentletDataGen.checkin(specificVariantContentlet);

        dotDatabaseMetaData.dropTable(connection, "unique_fields");

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();

        assertTrue(getUniqueFieldsRegisters(contentType).isEmpty());

        uniqueFieldDataBaseUtil.populateUniqueFieldsTable();

        final List<Map<String, Object>> uniqueFieldsRegisters = getUniqueFieldsRegisters(contentType);
        assertEquals(1, uniqueFieldsRegisters.size());

        final Map<String, Object> uniqueFieldsRegister = uniqueFieldsRegisters.get(0);
        final Map<String, Object> supportingValues = JsonUtil.getJsonFromString(uniqueFieldsRegister.get("supporting_values").toString());

        assertEquals(false, Boolean.valueOf(supportingValues.get(LIVE_ATTR) != null ?
                supportingValues.get(LIVE_ATTR).toString() : "false"));

        final List<String> contentletIds = (List<String>) supportingValues.get(CONTENTLET_IDS_ATTR);
        assertEquals(1, contentletIds.size());

        final String contentId = contentletIds.get(0);
        assertEquals(defaultContentlet.getIdentifier(), contentId);

        final String hash = calculateHash(defaultContentlet, language, uniqueTextField, host, uniqueValue);
        assertEquals(hash, uniqueFieldsRegister.get("unique_key_val") );
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

        assertTrue(getUniqueFieldsRegisters(contentType).isEmpty());

        uniqueFieldDataBaseUtil.populateUniqueFieldsTable();

        final List<Map<String, Object>> uniqueFieldsRegisters = getUniqueFieldsRegisters(contentType);
        assertEquals(2, uniqueFieldsRegisters.size());

        for (Map<String, Object> uniqueFieldsRegister : uniqueFieldsRegisters) {
            final Map<String, Object> supportingValues = JsonUtil.getJsonFromString(uniqueFieldsRegister.get("supporting_values").toString());

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


    private List<Map<String, Object>> getUniqueFieldsRegisters(ContentType contentType) throws DotDataException {
        return new DotConnect().setSQL("SELECT * FROM unique_fields WHERE supporting_values->>'contentTypeId' = ?")
                .addParam(contentType.id()).loadObjectResults();
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

        final Host host = new SiteDataGen().nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), uniqueValue)
                .nextPersisted();

        final UniqueFieldCriteria uniqueFieldCriteria = new Builder().setVariantName(contentlet.getVariantId())
                .setLanguage(language)
                .setContentType(contentlet.getContentType())
                .setField(uniqueTextField)
                .setSite(host)
                .setLive(false)
                .setValue(uniqueValue)
                .build();

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();

        final Map<String, Object> supportingValues =  new HashMap<>(uniqueFieldCriteria.toMap());
        supportingValues.put(CONTENTLET_IDS_ATTR, List.of(contentlet.getIdentifier()));
        supportingValues.put(UNIQUE_PER_SITE_ATTR, false);

        uniqueFieldDataBaseUtil.insert(uniqueFieldCriteria.criteria(), supportingValues);

        UniqueFieldDataBaseUtil.UniqueFieldValue uniqueFieldValue = uniqueFieldDataBaseUtil.get(uniqueFieldCriteria)
                .orElseThrow();

        Map<String, Object> supportingValuesFromDB = uniqueFieldValue.getSupportingValues();

        assertEquals(uniqueValue, supportingValuesFromDB.get(FIELD_VALUE_ATTR));
        assertEquals(language.getId(), Long.parseLong(supportingValuesFromDB.get(LANGUAGE_ID_ATTR).toString()));
        assertEquals(contentType.id(), supportingValuesFromDB.get(CONTENT_TYPE_ID_ATTR));
        assertEquals(uniqueTextField.variable(), supportingValuesFromDB.get(FIELD_VARIABLE_NAME_ATTR));
        assertEquals(host.getIdentifier(), supportingValuesFromDB.get(SITE_ID_ATTR));
        assertEquals(false, supportingValuesFromDB.get(LIVE_ATTR));

        assertEquals(1, ((Collection) supportingValuesFromDB.get(CONTENTLET_IDS_ATTR)).size());
        assertTrue(((Collection) supportingValuesFromDB.get(CONTENTLET_IDS_ATTR)).contains(contentlet.getIdentifier()));
    }


    /**
     * Method to test: {@link UniqueFieldDataBaseUtil#updateContentListWithHash(String, List)}
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

        final Host host = new SiteDataGen().nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .setProperty(uniqueTextField.variable(), uniqueValue)
                .nextPersisted();

        final UniqueFieldCriteria uniqueFieldCriteria = new Builder().setVariantName(contentlet.getVariantId())
                .setLanguage(language)
                .setContentType(contentlet.getContentType())
                .setField(uniqueTextField)
                .setSite(host)
                .setLive(false)
                .setValue(uniqueValue)
                .build();

        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();

        final Map<String, Object> supportingValues =  new HashMap<>(uniqueFieldCriteria.toMap());
        supportingValues.put(CONTENTLET_IDS_ATTR, List.of(contentlet.getIdentifier()));
        supportingValues.put(UNIQUE_PER_SITE_ATTR, false);

        uniqueFieldDataBaseUtil.insert(uniqueFieldCriteria.criteria(), supportingValues);

        uniqueFieldDataBaseUtil.delete(uniqueFieldCriteria.criteria());

        UniqueFieldDataBaseUtil.UniqueFieldValue uniqueFieldValue = uniqueFieldDataBaseUtil.get(uniqueFieldCriteria)
                .orElseThrow();

        assertFalse(uniqueFieldValue.getSupportingValues().isEmpty());
    }
}
