package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.dotcms.IntegrationTestBase;

/**
 * Test of {@link Task04345ChangeMultiTreePrimaryKey}
 */
public class Task04345ChangeMultiTreePrimaryKeyTest  {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testCreatePrimaryKey() throws DotDataException, SQLException {
        List primaryKeysColumns = new ArrayList();
        DotConnect dc = new DotConnect();

        Task04345ChangeMultiTreePrimaryKey task04345ChangeMultiTreePrimaryKey = new Task04345ChangeMultiTreePrimaryKey();
        String sql = null;


        if (DbConnectionFactory.isPostgres()) {
            sql = task04345ChangeMultiTreePrimaryKey.getPostgresScript();
        } else if (DbConnectionFactory.isMySql()) {
            sql = task04345ChangeMultiTreePrimaryKey.getMySQLScript();
        } else if (DbConnectionFactory.isOracle()) {
            sql = task04345ChangeMultiTreePrimaryKey.getOracleScript();
        } else if (DbConnectionFactory.isMsSql()) {
            sql = task04345ChangeMultiTreePrimaryKey.getMSSQLScript();
        } else if (DbConnectionFactory.isH2()) {
            sql = task04345ChangeMultiTreePrimaryKey.getH2Script();
        } else {
            assertTrue(false);
        }

        dc.setSQL(sql);
        dc.loadResult();

        DatabaseMetaData metaData = DbConnectionFactory.getConnection().getMetaData();
        ResultSet multi_tree = metaData.getPrimaryKeys(null, null, "multi_tree");

        while(multi_tree.next()){
            String columnName = multi_tree.getString(4);
            primaryKeysColumns.add(columnName);
        }

        assertEquals(4, primaryKeysColumns.size());

        assertTrue(primaryKeysColumns.contains("child"));
        assertTrue(primaryKeysColumns.contains("parent1"));
        assertTrue(primaryKeysColumns.contains("parent2"));
        assertTrue(primaryKeysColumns.contains("relation_type"));
    }
}
