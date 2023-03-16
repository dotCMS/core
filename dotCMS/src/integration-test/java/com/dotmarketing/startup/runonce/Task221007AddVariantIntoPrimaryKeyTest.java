package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotDataException;
import java.sql.SQLException;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task221007AddVariantIntoPrimaryKeyTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link Task221007AddVariantIntoPrimaryKey#executeUpgrade()}
     * When: RUn the Task221007AddVariantIntoPrimaryKey
     * Should: Update the contentlet_version_info primary key to contain the variant_id
     *
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    public void executeUpgrade() throws SQLException, DotDataException {
        cleanUp();

        Task221007AddVariantIntoPrimaryKey task221007AddVariantIntoPrimaryKey =
                new Task221007AddVariantIntoPrimaryKey();
        assertTrue(task221007AddVariantIntoPrimaryKey.forceRun());

        checkOldPrimaryKey();
        task221007AddVariantIntoPrimaryKey.executeUpgrade();

        assertFalse(task221007AddVariantIntoPrimaryKey.forceRun());
        checkPrimaryKey();
    }

    private static void checkPrimaryKey() throws SQLException {
        final List<String> primaryKeysFields = DotDatabaseMetaData.getPrimaryKeysFields("contentlet_version_info");
        assertEquals(3, primaryKeysFields.size());
        assertTrue(primaryKeysFields.contains("identifier"));
        assertTrue(primaryKeysFields.contains("lang"));
        assertTrue(primaryKeysFields.contains("variant_id"));
    }

    private static void checkOldPrimaryKey() throws SQLException {
        final List<String> primaryKeysFields =  DotDatabaseMetaData.getPrimaryKeysFields("contentlet_version_info");
        assertEquals(2, primaryKeysFields.size());
        assertTrue(primaryKeysFields.contains("identifier"));
        assertTrue(primaryKeysFields.contains("lang"));
    }

    private void cleanUp() throws SQLException {

        final String constraintName = DotDatabaseMetaData.getPrimaryKeyName(
                        "contentlet_version_info")
                .orElse("contentlet_version_info_pkey");

        cleanUp(constraintName);
    }

    private void cleanUp(final String newName) throws SQLException {
        try {
            DotDatabaseMetaData.dropPrimaryKey("contentlet_version_info");
        } catch (Exception e) {
            //ignore
        }

        new DotConnect().executeStatement(String.format("ALTER TABLE contentlet_version_info "
                + " ADD CONSTRAINT %s PRIMARY KEY (identifier, lang)", newName));
    }

    private void removePrimaryKey() throws SQLException {

        try {
            DotDatabaseMetaData.dropPrimaryKey("contentlet_version_info");
        } catch (Exception e) {
            //ignore
        }
    }

    /**
     * Method to test: {@link Task221007AddVariantIntoPrimaryKey#executeUpgrade()}
     * When: Run the Task221007AddVariantIntoPrimaryKey and the Primary key does not exist
     * Should: Create the contentlet_version_info primary key to contain the variant_id
     *
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    public void whenThePrimaryKeyDoesNotExists() throws SQLException, DotDataException {
        removePrimaryKey();

        Task221007AddVariantIntoPrimaryKey task221007AddVariantIntoPrimaryKey =
                new Task221007AddVariantIntoPrimaryKey();
        assertTrue(task221007AddVariantIntoPrimaryKey.forceRun());

        task221007AddVariantIntoPrimaryKey.executeUpgrade();

        assertFalse(task221007AddVariantIntoPrimaryKey.forceRun());
        checkPrimaryKey();
    }

    /**
     * Method to test: {@link Task221007AddVariantIntoPrimaryKey#executeUpgrade()}
     * When: Run the Task221007AddVariantIntoPrimaryKey and the Primary key has a name different that contentlet_version_info_pkey
     * Should: Update the contentlet_version_info primary key to contain the variant_id
     *
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    public void primaryKeyWithNoDefaultName() throws SQLException, DotDataException {
        cleanUp("whatever_name");

        Task221007AddVariantIntoPrimaryKey task221007AddVariantIntoPrimaryKey =
                new Task221007AddVariantIntoPrimaryKey();
        assertTrue(task221007AddVariantIntoPrimaryKey.forceRun());

        task221007AddVariantIntoPrimaryKey.executeUpgrade();

        assertFalse(task221007AddVariantIntoPrimaryKey.forceRun());
        checkPrimaryKey();
    }
}
