package com.dotmarketing.startup.runonce;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.common.db.PrimaryKey;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DbType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import java.sql.Connection;
import java.sql.SQLException;

public class Task05160MultiTreeAddPersonalizationColumnAndChangingPKTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    
    @Ignore
    @Test
    public void testExecuteUpgrade() {
        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());

        System.out.println("Running with the db: " + dbType);

        try {
            final Task05160MultiTreeAddPersonalizationColumnAndChangingPK treeAddPersonalizationColumnAndChangingPK =
                            new Task05160MultiTreeAddPersonalizationColumnAndChangingPK();

            if (!treeAddPersonalizationColumnAndChangingPK.forceRun()) {

                this.removePersonalizationColumnAndPK();
            }

            if (treeAddPersonalizationColumnAndChangingPK.forceRun()) {
                treeAddPersonalizationColumnAndChangingPK.executeUpgrade();
            }
        } catch (Exception e) {
            final String errMessage = "Could not modify multi tree table on db of type: " + dbType + " Err: " + e.toString();
            Logger.info(getClass(), errMessage + "\n" + ExceptionUtil.exceptionAsString(e, 30));
            Logger.error(getClass(), errMessage, e);
            Assert.fail(errMessage);
        }

        int count = new DotConnect().setSQL("select count(*) as test from multi_tree where personalization='thisworked'")
                        .getInt("test");

        Assert.assertTrue("db worked", count == 0);
    }

    @WrapInTransaction
    private void removePersonalizationColumnAndPK() throws SQLException, DotDataException {
        Connection connection = DbConnectionFactory.getConnection();


        final PrimaryKey primaryKey = new DotDatabaseMetaData().dropPrimaryKey(connection, "multi_tree");
        System.out.println("Removed the primaryKey :  " + primaryKey);

        // setting the old key
        new DotConnect().executeStatement(
                        "ALTER TABLE multi_tree ADD CONSTRAINT idx_multitree_index1 PRIMARY KEY (parent1, parent2, child, relation_type)");

        new DotDatabaseMetaData().dropColumn(connection, "multi_tree", "personalization");
        System.out.println("Removed personalization column ");

    }

}
