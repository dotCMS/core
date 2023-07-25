package com.dotmarketing.startup.runonce;


import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for the {@link Task230707CreateSystemTable}
 * @author jsanca
 */
public class Task230707CreateSystemTableTest extends IntegrationTestBase {


    @BeforeClass
    public static  void sytemTableExists() throws Exception {

        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        LocalTransaction.wrap(Task230707CreateSystemTableTest::checkIfSystemTableExits);
    }

    private static void checkIfSystemTableExits()  {
        try {
            final Connection connection = DbConnectionFactory.getConnection();
            final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();
            final boolean tableExistsSystemTable = databaseMetaData.tableExists(connection, "system_table");
            if (tableExistsSystemTable) {
                databaseMetaData.dropTable(connection, "system_table");
            }
        }catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Method to test: {@link Task230707CreateSystemTable#executeUpgrade()} and
     * {@link Task230707CreateSystemTable#forceRun()}
     * when: the table does not exist
     * should: the table will be created after run the upgrade
     * @throws SQLException
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void executeTaskUpgrade() throws SQLException, DotDataException, DotSecurityException {

        final Task230707CreateSystemTable task230707CreateSystemTable = new Task230707CreateSystemTable();
        final Connection connection = DbConnectionFactory.getConnection();
        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

        final boolean tableExistsSystemTable = databaseMetaData.tableExists(connection, "system_table");
        assertFalse(tableExistsSystemTable);

        Assert.assertTrue(task230707CreateSystemTable.forceRun());
        LocalTransaction.wrap(()-> {
            try {
                task230707CreateSystemTable.executeUpgrade();
            }catch (Exception e) {
                throw new DotRuntimeException(e);
            }
        });

        final boolean tableExistsSystemTableAfterUpgrade = databaseMetaData.tableExists(connection, "system_table");
        assertTrue(tableExistsSystemTableAfterUpgrade);
    }

}
