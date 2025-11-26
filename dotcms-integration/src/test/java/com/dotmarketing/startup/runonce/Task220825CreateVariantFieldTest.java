package com.dotmarketing.startup.runonce;

import static graphql.Assert.assertNull;
import static graphql.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task220825CreateVariantFieldTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

       checkIfVariantColumnExist();
    }

    @After
    public void createPrimaryKeyWithTwoFields() throws Exception {
        new DotConnect().executeStatement("ALTER TABLE contentlet_version_info "
                + " ADD CONSTRAINT contentlet_version_info_pkey PRIMARY KEY (identifier, lang)");
    }

    @AfterClass
    public static void createPrimaryKeyWithThreeFields() throws Exception {

        final Optional<String> mssqlPrimaryKeyName = DbConnectionFactory.isMsSql() ?
                getMSSQLPrimaryKeyName() : Optional.of("contentlet_version_info_pkey");

        if (mssqlPrimaryKeyName.isPresent()) {
            new DotConnect().executeStatement("ALTER TABLE contentlet_version_info DROP CONSTRAINT "
                    + mssqlPrimaryKeyName.get());
        }

        new DotConnect().executeStatement("ALTER TABLE contentlet_version_info "
                + " ADD CONSTRAINT contentlet_version_info_pkey PRIMARY KEY (identifier, lang, variant_id)");
    }

    private static void checkIfVariantColumnExist() throws SQLException {
        final Connection connection = DbConnectionFactory.getConnection();

        final ResultSet columnsMetaData = DotDatabaseMetaData
                .getColumnsMetaData(connection, "contentlet_version_info");

        boolean variantIdFound = false;
        while(columnsMetaData.next()){
            final String columnName = columnsMetaData.getString("COLUMN_NAME");
            final String columnType = columnsMetaData.getString(6);

            if ("variant_id".equals(columnName)) {
                variantIdFound = true;

                if (DbConnectionFactory.isPostgres()) {
                    assertEquals("varchar", columnType);
                } else {
                    assertEquals("nvarchar", columnType);
                }

                assertEquals("The column sice should be 255", "255", columnsMetaData.getString(7));
            }
        }

        assertTrue( variantIdFound, ()->"Should exists de variant field in contentlet_version_info");
    }

    /**
     * Method to test: {@link Task220824CreateDefaultVariant#executeUpgrade()}
     * when: the UT run
     * Should: Create the default variant and add a new field in the contentlet_version_info
     */
    @Test
    public void runningTU() throws DotDataException, SQLException {

        cleanAllBefore();

        final Task220825CreateVariantField upgradeTask = new Task220825CreateVariantField();
        upgradeTask.executeUpgrade();

        checkIfVariantColumnExist();
    }

    /**
     *  Method to test: {@link Task220824CreateDefaultVariant#executeUpgrade()}
     *  When: Create a Contentlet before the {@link Task220824CreateDefaultVariant} run
     *  Should: not going to have variant_id
     *  When: Create a Contentlet after the {@link Task220824CreateDefaultVariant} run
     *  Should:  have a variant_id equals to 1
     *
     * @throws DotDataException
     */
    @Test
    public void conntentletVersionInfoWithVariant() throws DotDataException {
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentletBefore = new ContentletDataGen(contentType).nextPersisted();

        cleanAllBefore();

        if (!DbConnectionFactory.isMsSql()) {
            checkNotExistVariant(contentletBefore);
        }

        final Task220825CreateVariantField upgradeTask = new Task220825CreateVariantField();
        upgradeTask.executeUpgrade();

        final Contentlet contentletAfter = new ContentletDataGen(contentType).nextPersisted();

        checkContentletVersionInfo(contentletBefore);
        checkContentletVersionInfo(contentletAfter);
    }

    private void checkNotExistVariant(final Contentlet contentlet) throws DotDataException {
        final ArrayList results = new DotConnect().setSQL(
                        "SELECT * FROM contentlet_version_info WHERE identifier = ?")
                .addParam(contentlet.getIdentifier())
                .loadResults();

        assertEquals(1, results.size());
        assertNull(((Map) results.get(0)).get("variant_id"));
    }

    private void checkContentletVersionInfo(final Contentlet contentlet) throws DotDataException {
        final ArrayList results = new DotConnect().setSQL(
                        "SELECT * FROM contentlet_version_info WHERE identifier = ?")
                .addParam(contentlet.getIdentifier())
                .loadResults();

        assertEquals(1, results.size());
        assertEquals("DEFAULT", ((Map) results.get(0)).get("variant_id").toString());
    }

    @Test
    public void runningTwice() throws DotDataException {
        cleanAllBefore();

        final Task220825CreateVariantField upgradeTask = new Task220825CreateVariantField();
        upgradeTask.executeUpgrade();
        upgradeTask.executeUpgrade();
    }

    /**
     *  Method to test: {@link Task220824CreateDefaultVariant#forceRun()}
     *  When: the Default variant exists
     *  Should return false
     *  When: the Default variant does not  exists
     *  Should return true
     */
    @Test
    public void forceRun() throws DotDataException {
        cleanAllBefore();

        final Task220825CreateVariantField upgradeTask = new Task220825CreateVariantField();
        assertTrue(upgradeTask.forceRun());

        upgradeTask.executeUpgrade();

        assertFalse(upgradeTask.forceRun());
    }

    private void cleanAllBefore()  {

        if (hasVariantIdColumn()) {

            final DotConnect dotConnect = new DotConnect();

            try {

                cleanAnyConstraint();

                dotConnect.executeStatement(
                        "ALTER TABLE contentlet_version_info DROP COLUMN variant_id");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean hasVariantIdColumn() {
        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();
        try {
            return databaseMetaData.hasColumn("contentlet_version_info", "variant_id");
        } catch (SQLException e) {
            return false;
        }
    }

    private void cleanAnyConstraint()  {
        final DotConnect dotConnect = new DotConnect();

        final Optional<String> constraintName = DbConnectionFactory.isPostgres() ?
                Optional.of("contentlet_version_info_pkey") : getMSSQLPrimaryKeyName();

        try {
            if (constraintName.isPresent()) {
                dotConnect.executeStatement(
                        String.format("ALTER TABLE contentlet_version_info "
                                + "DROP CONSTRAINT %s", constraintName.get()));
            }

            if (DbConnectionFactory.isMsSql()) {
                final ArrayList<Map> loadResults = dotConnect.setSQL("SELECT name "
                                + "FROM sysobjects so JOIN sysconstraints sc ON so.id = sc.constid "
                                + "WHERE object_name(so.parent_obj) = 'contentlet_version_info' AND "
                                + "sc.colid in  (select colid from syscolumns where name = 'variant_id')")
                        .addParam(VariantAPI.DEFAULT_VARIANT.name())
                        .loadResults();

                if (!loadResults.isEmpty()) {
                    dotConnect.setSQL("ALTER TABLE contentlet_version_info DROP CONSTRAINT "
                                    + loadResults.get(0).get("name").toString())
                            .loadResult();
                }
            }
        } catch (Exception e) {
            //ignore
        }
    }

    private static Optional<String> getMSSQLPrimaryKeyName()  {
        try {
            final ArrayList arrayList = new DotConnect()
                    .setSQL("SELECT * FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS T "
                            + "WHERE table_name = 'contentlet_version_info' "
                            + "AND constraint_type = 'PRIMARY KEY'")
                    .loadResults();

            return arrayList.isEmpty() ? Optional.empty() :
                    Optional.of(((Map) arrayList.get(0)).get("constraint_name").toString());
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }
}
