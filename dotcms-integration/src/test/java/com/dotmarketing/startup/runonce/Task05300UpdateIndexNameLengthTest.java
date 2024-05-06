package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertEquals;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import java.sql.SQLException;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task05300UpdateIndexNameLengthTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private Map<String, String> getColumnProperties() throws DotDataException {
        String query;

        if (!DbConnectionFactory.getDBType().equals("Oracle")){
            query = "select character_maximum_length as field_length, is_nullable as nullable_value from "
                    + "information_schema.columns where table_name = 'indicies' "
                    + "and column_name='index_name'";
        } else {
            query = "SELECT DATA_LENGTH as field_length, NULLABLE as nullable_value "
                    + "FROM user_tab_columns WHERE table_name = 'INDICIES' "
                    + "AND column_name = 'INDEX_NAME'";
        }
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(query);
        return (Map<String, String>)dotConnect.loadResults().get(0);

    }

    private void setStateBeforeUpgrade() throws SQLException {
        String query = null;

        switch(DbConnectionFactory.getDBType()){
            case "PostgreSQL":
                query = "alter table indicies alter column index_name type varchar(30);";
                break;

            case "MySQL":
                query = "alter table indicies modify index_name varchar(30);";
                break;

            case "Oracle":
                query = "alter table indicies modify index_name varchar2(30);";
                break;

            case "Microsoft SQL Server":
                query = "DECLARE @SQL VARCHAR(4000)\n"
                        + "SET @SQL = 'ALTER TABLE dbo.Indicies DROP CONSTRAINT |ConstraintName| '\n"
                        + "\n"
                        + "SET @SQL = REPLACE(@SQL, '|ConstraintName|', ( SELECT   name\n"
                        + "                                               FROM     sysobjects\n"
                        + "                                               WHERE    xtype = 'PK'\n"
                        + "                                                        AND parent_obj =        OBJECT_ID('Indicies')))\n"
                        + "\n"
                        + "EXEC (@SQL)\n"
                        + "alter table indicies alter column index_name nvarchar(100) not null;\n"
                        + "alter table Indicies add primary key (index_name);";
                break;
        }

        final DotConnect dotConnect = new DotConnect();
        dotConnect.executeStatement(query);
    }

    @Test
    public void testUpgradeTaskShouldPass() throws DotDataException, SQLException {

        setStateBeforeUpgrade();

        //Check values before upgrade task
        Map<String, String> result = getColumnProperties();
        assertEquals("30", result.get("field_length"));
        assertEquals(DbConnectionFactory.getDBType().equals("Oracle") ? "N" : "NO",
                result.get("nullable_value"));

        final Task05300UpdateIndexNameLength task = new Task05300UpdateIndexNameLength();

        task.executeUpgrade();

        //check values after upgrade task
        result = getColumnProperties();
        assertEquals("100", result.get("field_length"));
        assertEquals(DbConnectionFactory.getDBType().equals("Oracle") ? "N" : "NO",
                result.get("nullable_value"));
    }

}
