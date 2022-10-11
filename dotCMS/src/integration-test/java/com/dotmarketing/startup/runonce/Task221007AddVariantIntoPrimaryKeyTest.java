package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        final List<String> primaryKeysFields = getPrimaryKeysFields();
        assertEquals(3, primaryKeysFields.size());
        assertTrue(primaryKeysFields.contains("identifier"));
        assertTrue(primaryKeysFields.contains("lang"));
        assertTrue(primaryKeysFields.contains("variant_id"));
    }

    private static void checkOldPrimaryKey() throws SQLException {
        final List<String> primaryKeysFields = getPrimaryKeysFields();
        assertEquals(2, primaryKeysFields.size());
        assertTrue(primaryKeysFields.contains("identifier"));
        assertTrue(primaryKeysFields.contains("lang"));
    }

    private static List<String> getPrimaryKeysFields() throws SQLException {
        final Connection connection = DbConnectionFactory.getConnection();
        final ResultSet pkColumns = connection.getMetaData()
                .getPrimaryKeys(null, null, "contentlet_version_info");

        List<String> primaryKeysFields = new ArrayList();

        while(pkColumns.next()) {
            primaryKeysFields.add(pkColumns.getString("COLUMN_NAME"));
        }

        return primaryKeysFields;
    }

    private void cleanUp() throws SQLException {

        final String constraintName =
                DbConnectionFactory.isPostgres() ? "contentlet_version_info_pkey"
                        : getMSSQLPrimaryKeyName();

        try {
            new DotConnect().executeStatement(String.format("ALTER TABLE contentlet_version_info "
                    + "DROP CONSTRAINT %s", constraintName));

        } catch (Exception e) {
            //ignore
        }

        new DotConnect().executeStatement(String.format("ALTER TABLE contentlet_version_info "
                + " ADD CONSTRAINT %s PRIMARY KEY (identifier, lang)", constraintName));
    }

    private String getMSSQLPrimaryKeyName()  {
        try {
            final ArrayList arrayList = new DotConnect()
                    .setSQL("SELECT * FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS T "
                            + "WHERE table_name = 'contentlet_version_info' "
                            + "AND constraint_type = 'PRIMARY KEY'")
                    .loadResults();

            return (((Map) arrayList.get(0))).get("constraint_name").toString();
        } catch (Exception e) {
            return "contentlet_version_info_pkey";
        }
    }
}
