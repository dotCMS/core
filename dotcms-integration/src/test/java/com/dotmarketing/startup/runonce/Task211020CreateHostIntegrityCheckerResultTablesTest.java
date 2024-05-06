package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.SQLException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Task211020CreateHostIntegrityCheckerResultTablesTest {
    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        dropTableIfExists();
    }

    @Test
    public void testUpgradeTask() throws DotDataException {
        final Task211020CreateHostIntegrityCheckerResultTables task =
                new Task211020CreateHostIntegrityCheckerResultTables();
        assertTrue(task.forceRun());
        task.executeUpgrade();
        assertFalse(task.forceRun());
    }

    private static void dropTableIfExists() throws SQLException, DotDataException {
        DotConnect dc = new DotConnect();
        boolean dropIt = false;
        if (DbConnectionFactory.isOracle()) {
            dc.setSQL("SELECT COUNT(*) as exist FROM user_tables WHERE table_name = 'hosts_ir'");
            BigDecimal existTable = (BigDecimal) dc.loadObjectResults().get(0).get("exist");
            if (existTable.longValue() == 1) {
                dropIt = true;
            }
        } else if (DbConnectionFactory.isPostgres() || DbConnectionFactory.isMySql()) {
            dc.setSQL("SELECT COUNT(table_name) AS exist FROM information_schema.tables WHERE table_name = 'hosts_ir'");
            dropIt = (Long) dc.loadObjectResults().get(0).get("exist") > 0;
        } else if (DbConnectionFactory.isMsSql()) {
            dc.setSQL("SELECT COUNT(*) AS exist FROM sysobjects WHERE name = 'hosts_ir'");
            dropIt = (Integer) dc.loadObjectResults().get(0).get("exist") > 0;
        }

        if (dropIt) {
            dc.executeStatement("DROP TABLE hosts_ir");
        }
    }
}
