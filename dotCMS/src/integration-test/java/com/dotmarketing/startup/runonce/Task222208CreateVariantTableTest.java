package com.dotmarketing.startup.runonce;

import static com.dotcms.util.CollectionsUtils.map;
import static graphql.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DbConnectionFactory.DataBaseType;
import com.dotmarketing.exception.DotDataException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task222208CreateVariantTableTest {

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
    }

    @Test
    public void createVariantTable() throws DotDataException, SQLException {
        final Connection connection = DbConnectionFactory.getConnection();
        new DotConnect().setSQL("DROP TABLE variant").loadResult();

        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();

        assertFalse(dotDatabaseMetaData.tableExists(connection, "variant"));

        final Task222208CreateVariantTable task222208CreateVariantTable = new Task222208CreateVariantTable();
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


    @Test
    public void runTUTwice() throws DotDataException, SQLException {
        final Task222208CreateVariantTable task222208CreateVariantTable = new Task222208CreateVariantTable();
        task222208CreateVariantTable.executeUpgrade();
        task222208CreateVariantTable.executeUpgrade();

        assertTrue(new DotDatabaseMetaData().tableExists(DbConnectionFactory.getConnection(), "variant"));
    }
}
