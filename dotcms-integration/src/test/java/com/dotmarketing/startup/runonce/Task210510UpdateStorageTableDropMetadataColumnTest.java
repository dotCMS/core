package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task210510UpdateStorageTableDropMetadataColumnTest {
    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }


    private String getPostgresScript() {
        return "ALTER TABLE storage ADD COLUMN metadata text;";
    }

    private String getMyScript() {
        return "ALTER TABLE storage ADD COLUMN metadata text;";
    }

    private String getMSSQLScript() {
        return "ALTER TABLE storage ADD metadata text;";
    }

    private String getOracleScript() {
        return "ALTER TABLE storage ADD metadata NCLOB ";
    }

    /**
     * Returns the SQL Script depending on the db 
     * @return
     */
    private String getScript() {
        if(DbConnectionFactory.isPostgres()){
            return getPostgresScript();
        }
        if(DbConnectionFactory.isMySql()){
            return getMyScript();
        }
        if(DbConnectionFactory.isMsSql()){
            return getMSSQLScript();
        }
        if(DbConnectionFactory.isOracle()){
            return getOracleScript();
        }
        throw new DotRuntimeException("Oh snap! dunno what database I'm running on.");
    }

    /**
     * Given scenario: We intend to test the upgrade task that drops the column if it exist
     * Expected result: If the column does not exist it gets added. Once added we test it can be removed and the task wont run again.
     * @throws DotDataException
     * @throws SQLException
     */
    @Test
    public void testExecuteUpgrade() throws DotDataException, SQLException {
        //First Recreate the old column situation
        //add column if it does not exist
        final Connection connection = DbConnectionFactory.getConnection();
        final boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(true);
        try{
          final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
          if (!dotDatabaseMetaData.hasColumn("storage", "metadata")) {
            new DotConnect().executeStatement(getScript(), connection);
          }

         final Task210510UpdateStorageTableDropMetadataColumn upgradeTask = new Task210510UpdateStorageTableDropMetadataColumn();
         assertTrue(upgradeTask.forceRun());
         upgradeTask.executeUpgrade();
         assertFalse(upgradeTask.forceRun());
        }finally {
            connection.setAutoCommit(autoCommit);
        }
    }
}
