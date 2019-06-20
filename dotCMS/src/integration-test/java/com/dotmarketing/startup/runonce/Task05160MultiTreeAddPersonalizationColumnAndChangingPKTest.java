package com.dotmarketing.startup.runonce;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.common.db.PrimaryKey;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DbType;
import com.dotmarketing.util.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

public class Task05160MultiTreeAddPersonalizationColumnAndChangingPKTest {

    @BeforeClass
    public static void prepare() throws Exception{
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade()  {
        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());
        try{
            final Task05160MultiTreeAddPersonalizationColumnAndChangingPK treeAddPersonalizationColumnAndChangingPK =
                    new Task05160MultiTreeAddPersonalizationColumnAndChangingPK();

            if (!treeAddPersonalizationColumnAndChangingPK.forceRun()) {

                this.removePersonalizationColumnAndPK();
            }

            if (treeAddPersonalizationColumnAndChangingPK.forceRun()) {
                treeAddPersonalizationColumnAndChangingPK.executeUpgrade();
            }
        } catch (Exception e) {
            final String  errMessage = "Could not modify multi tree table on db of type: " + dbType + " Err: " +  e.toString() ;
            Logger.error(getClass(),errMessage, e);
            Assert.fail(errMessage);
        }
    }

    @WrapInTransaction
    private void removePersonalizationColumnAndPK() throws SQLException {

        try (final Connection connection = DbConnectionFactory.getDataSource().getConnection()) {
            final PrimaryKey primaryKey = new DotDatabaseMetaData().dropPrimaryKey(connection, "multi_tree");
            System.out.println("Removed primaryKey :  " + primaryKey);
        }
        // setting the old key
        new DotConnect().executeStatement("ALTER TABLE multi_tree ADD CONSTRAINT idx_multitree_index1 PRIMARY KEY (parent1, parent2, child, relation_type)");
    }

}
