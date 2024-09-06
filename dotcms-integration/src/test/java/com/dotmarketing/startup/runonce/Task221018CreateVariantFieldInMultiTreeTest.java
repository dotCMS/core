package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.MultiTreeDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import graphql.Assert;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task221018CreateVariantFieldInMultiTreeTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        checkIfFieldExist();
        checkIfFieldIsIntoPrimaryKey();
    }

    /**
     * Method to test: {@link Task221018CreateVariantFieldInMultiTree#executeUpgrade()} and {@link Task221018CreateVariantFieldInMultiTree#forceRun()}
     * When: Run the {@link Task221018CreateVariantFieldInMultiTree#executeUpgrade()}
     * Should: Create a new variantId field in the multi_tree table.
     * Also should add the new field into the primarykey
     *
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    public void createField() throws SQLException, DotDataException {
        cleanUp();

        final DotConnect dotConnect = new DotConnect();
        dotConnect.executeStatement("INSERT INTO multi_tree (parent1, parent2, child, relation_type) VALUES('htmlPage', 'container', 'content', '1')");

        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

        assertEquals(5, DotDatabaseMetaData.getPrimaryKeysFields("multi_tree").size());

        final Task221018CreateVariantFieldInMultiTree task221018CreateMultiTreeField = new Task221018CreateVariantFieldInMultiTree();
        assertTrue(task221018CreateMultiTreeField.forceRun());

        task221018CreateMultiTreeField.executeUpgrade();

        checkIfFieldExist();
        checkIfFieldIsIntoPrimaryKey();
        assertFalse(task221018CreateMultiTreeField.forceRun());

        dotConnect.executeStatement("INSERT INTO multi_tree (parent1, parent2, child, relation_type) VALUES('htmlPage_2', 'container', 'content', '1')");

        final ArrayList multiTrees = dotConnect.setSQL(
                "SELECT variant_id FROM multi_tree WHERE parent1 = 'htmlPage_2'").loadResults();

        assertEquals(1, multiTrees.size());
        assertEquals("DEFAULT", ((Map) multiTrees.get(0)).get("variant_id"));
    }

    /**
     * Method to test: {@link Task221018CreateVariantFieldInMultiTree#executeUpgrade()}
     * When: Run the {@link Task221018CreateVariantFieldInMultiTree#executeUpgrade()} twice
     * Should: Not throw any exception
     *
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    public void runTwice() throws SQLException, DotDataException {
        cleanUp();
        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

        assertFalse(databaseMetaData
                .hasColumn("multi_tree", "variant_id"));
        assertEquals(5, DotDatabaseMetaData.getPrimaryKeysFields("multi_tree").size());

        final Task221018CreateVariantFieldInMultiTree task221018CreateMultiTreeField = new Task221018CreateVariantFieldInMultiTree();
        assertTrue(task221018CreateMultiTreeField.forceRun());

        task221018CreateMultiTreeField.executeUpgrade();
        task221018CreateMultiTreeField.executeUpgrade();

        checkIfFieldExist();
        checkIfFieldIsIntoPrimaryKey();
        assertFalse(task221018CreateMultiTreeField.forceRun());
    }

    private static void checkIfFieldIsIntoPrimaryKey() {
        final List<String> multiTreePrimaryKey = DotDatabaseMetaData.getPrimaryKeysFields("multi_tree");
        assertEquals(6, multiTreePrimaryKey.size());
    }

    private static void cleanUp () throws SQLException {

        final String constraintName = DotDatabaseMetaData.getPrimaryKeyName("multi_tree")
                .orElse("multi_tree_pkey");

        try {
            DotDatabaseMetaData.dropPrimaryKey("multi_tree");
        } catch (Exception e) {
            //ignore
        }

        try {
            new DotConnect().executeStatement(
                    "ALTER TABLE multi_tree DROP COLUMN variant_id");
        } catch (SQLException e) {
            //ignore
        }

        new DotConnect().executeStatement(String.format("ALTER TABLE multi_tree "
                + " ADD CONSTRAINT %s PRIMARY KEY (child, parent1, parent2, relation_type, personalization)", constraintName));
    }

    private static void checkIfFieldExist() throws SQLException {
        final Connection connection = DbConnectionFactory.getConnection();

        final ResultSet columnsMetaData = DotDatabaseMetaData
                .getColumnsMetaData(connection, "multi_tree");

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

        Assert.assertTrue( variantIdFound, ()->"Should exists de variant_id field in multi_tree");
    }
}
