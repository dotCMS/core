package com.dotmarketing.startup.runonce;

import static graphql.Assert.assertNull;
import static graphql.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.variant.business.VariantAPI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task222408CreateDefaultVariantTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        checkIfVariantColumnExist();
    }

    private static void checkIfVariantDefaultExists() throws DotDataException {

        final ArrayList results = new DotConnect().setSQL("SELECT * FROM variant WHERE id = '1'")
                .loadResults();

        assertEquals("The DEFAULT Variant should exists", 1, results.size());
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

        assertTrue( variantIdFound, "Should exists de variant field in contentlet_version_info");
    }

    /**
     * Method to test: {@link Task222408CreateDefaultVariant#executeUpgrade()}
     * when: the UT run
     * Should: Create the default variant and add a new fiel in the contentlet_version_info
     */
    @Test
    public void runningTU() throws DotDataException, SQLException {

        cleanAllBefore();

        final Task222408CreateDefaultVariant upgradeTask = new Task222408CreateDefaultVariant();
        upgradeTask.executeUpgrade();

        checkIfVariantDefaultExists();
        checkIfVariantColumnExist();
    }

    /**
     *  Method to test: {@link Task222408CreateDefaultVariant#executeUpgrade()}
     *  When: Create a Contentlet before the {@link Task222408CreateDefaultVariant} run
     *  Should: not going to have variant_id
     *  When: Create a Contentlet after the {@link Task222408CreateDefaultVariant} run
     *  Should:  have a variant_id equals to 1
     *
     * @throws DotDataException
     */
    @Test
    public void conntentletVerionInfoWithVariant() throws DotDataException {
        cleanAllBefore();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentletBefore = new ContentletDataGen(contentType).nextPersisted();

        checkNotExistVariant(contentletBefore);

        final Task222408CreateDefaultVariant upgradeTask = new Task222408CreateDefaultVariant();
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
        assertEquals("1", ((Map) results.get(0)).get("variant_id").toString());
    }

    @Test
    public void runningTwice() throws DotDataException {
        cleanAllBefore();

        final Task222408CreateDefaultVariant upgradeTask = new Task222408CreateDefaultVariant();
        upgradeTask.executeUpgrade();
        upgradeTask.executeUpgrade();
    }

    /**
     *  Method to test: {@link Task222408CreateDefaultVariant#forceRun()}
     *  When: the Default variant exists
     *  Should return false
     *  When: the Default variant does not  exists
     *  Should return true
     */
    @Test
    public void forceRun() throws DotDataException {
        cleanAllBefore();

        final Task222408CreateDefaultVariant upgradeTask = new Task222408CreateDefaultVariant();
        assertTrue(upgradeTask.forceRun());

        upgradeTask.executeUpgrade();

        assertFalse(upgradeTask.forceRun());
    }

    private void cleanAllBefore()  {
        final DotConnect dotConnect = new DotConnect();

        try {
            dotConnect.setSQL("DELETE FROM variant WHERE id = ?")
                    .addParam(VariantAPI.DEFAULT_VARIANT.getIdentifier())
                    .loadResult();
        } catch (Exception e) {

        }

        try {
            dotConnect
                    .setSQL("ALTER TABLE contentlet_version_info DROP COLUMN variant_id")
                    .loadResult();
        }catch (Exception e){

        }
    }
}
