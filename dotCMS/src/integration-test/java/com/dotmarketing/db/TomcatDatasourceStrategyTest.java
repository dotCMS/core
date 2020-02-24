package com.dotmarketing.db;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for {@link TomcatDataSourceStrategy}
 * @author nollymar
 */
public class TomcatDatasourceStrategyTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testApply() throws SQLException {
        final DataSource dataSource = TomcatDataSourceStrategy.getInstance().apply();

        assertNotNull(dataSource);
        assertTrue(dataSource instanceof HikariDataSource);
        assertNotNull(dataSource.getConnection());
    }
}
