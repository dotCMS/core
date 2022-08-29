package com.dotmarketing.startup.runonce;

import static com.dotcms.util.CollectionsUtils.map;
import static graphql.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDGenerator;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task220822CreateVariantTableTest {

    private final Map<String, String> POSTGRES_EXPECTED = map(
            "id", "varchar",
            "name", "varchar",
            "archived", "bool"
    );

    private final Map<String, String> MSSQL_EXPECTED = map(
            "id", "nvarchar",
            "name", "nvarchar",
            "archived", "tinyint"
    );


    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        assertTrue(new DotDatabaseMetaData().tableExists(
                DbConnectionFactory.getConnection(), "variant"));

        checkNotAllowDuplicatedName();
    }

    /**
     * Method to test: {@link Task220822CreateVariantTable#executeUpgrade()}
     * When: the method is run
     * Should: create the variant table
     *
     * @throws DotDataException
     * @throws SQLException
     */
    @Test
    public void createVariantTable() throws DotDataException, SQLException {
        final Connection connection = DbConnectionFactory.getConnection();
        clean();

        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();

        assertFalse(dotDatabaseMetaData.tableExists(connection, "variant"));

        final Task220822CreateVariantTable task222208CreateVariantTable = new Task220822CreateVariantTable();
        task222208CreateVariantTable.executeUpgrade();

        assertTrue(dotDatabaseMetaData.tableExists(connection, "variant"));

        final ResultSet columnsMetaData = DotDatabaseMetaData
                .getColumnsMetaData(connection, "variant");

        final Map<String, String> expected = DbConnectionFactory.isMsSql() ?  MSSQL_EXPECTED :
            POSTGRES_EXPECTED;

        int count = 0;

        while(columnsMetaData.next()){
            final String columnName = columnsMetaData.getString("COLUMN_NAME");
            final String columnType = columnsMetaData.getString(6);

            assertTrue(expected.keySet().contains(columnName));
            assertEquals(expected.get(columnName), columnType);
            count++;
        }
        assertEquals(3, count);
    }

    private void clean() throws DotDataException {
        new DotConnect().setSQL("DROP TABLE variant").loadResult();
    }

    /**
     * Method to test: {@link Task220822CreateVariantTable#executeUpgrade()}
     * When: the method is run twice
     * Should: not throw any exception
     *
     * @throws DotDataException
     * @throws SQLException
     */
    @Test
    public void runTUTwice() throws DotDataException, SQLException {
        clean();
        final Task220822CreateVariantTable task222208CreateVariantTable = new Task220822CreateVariantTable();
        task222208CreateVariantTable.executeUpgrade();
        task222208CreateVariantTable.executeUpgrade();

        assertTrue(new DotDatabaseMetaData().tableExists(DbConnectionFactory.getConnection(), "variant"));
    }

    /**
     * Method to test: {@link Task220822CreateVariantTable#executeUpgrade()}
     * When: try to insert to variant with the same name after running the executeUpgrade
     * Should: throw
     *
     * @throws DotDataException
     * @throws SQLException
     */
    @Test
    public void nameDuplicated() throws DotDataException {
        clean();

        final Task220822CreateVariantTable task222208CreateVariantTable = new Task220822CreateVariantTable();
        task222208CreateVariantTable.executeUpgrade();
        checkNotAllowDuplicatedName();
    }

    private static void checkNotAllowDuplicatedName() throws DotDataException {
        new DotConnect().setSQL("INSERT INTO variant (id, name, archived) VALUES (?, ?, ?)")
                .addParam(UUIDGenerator.generateUuid())
                .addParam("Same Name")
                .addParam(false)
                .loadResult();

        try {
            new DotConnect().setSQL("INSERT INTO variant (id, name, archived) VALUES (?, ?, ?)")
                    .addParam(UUIDGenerator.generateUuid())
                    .addParam("Same Name")
                    .addParam(false)
                    .loadResult();
            throw new AssertionError("Exceeption expected because the same name is duplicated");
        } catch (DotDataException e) {
            //expected
        }
    }
}
