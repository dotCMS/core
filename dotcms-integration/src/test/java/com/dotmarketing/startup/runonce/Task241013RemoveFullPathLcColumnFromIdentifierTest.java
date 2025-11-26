package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;

public class Task241013RemoveFullPathLcColumnFromIdentifierTest {

    private static final String ADD_FULL_PATH_LC_COLUMN = "ALTER TABLE identifier ADD COLUMN full_path_lc varchar(255)";
    private static final String DROP_FUNCTION = "DROP FUNCTION IF EXISTS full_path_lc(identifier)";
    private static final String CHECK_INDEX_QUERY =
            "SELECT 1 FROM pg_indexes WHERE tablename = ? AND indexname = ?";
    private static final String CHECK_FUNCTION_QUERY =
            "SELECT 1 FROM information_schema.routines WHERE routine_name = ? AND routine_schema = ?";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }


    @Test
    public void test_upgradeTask_indexNorFunctionExists_success() throws DotDataException, SQLException {
        final DotConnect dc = new DotConnect();
        //Add the column to the table
        dc.executeStatement(ADD_FULL_PATH_LC_COLUMN);
        //Remove the index
        dc.executeStatement(Task241013RemoveFullPathLcColumnFromIdentifier.DROP_INDEX);
        //Remove the function
        dc.executeStatement(DROP_FUNCTION);

        //Run UT
        final Task241013RemoveFullPathLcColumnFromIdentifier upgradeTask = new Task241013RemoveFullPathLcColumnFromIdentifier();
        Assert.assertTrue("Column Not Exists and it should exists",upgradeTask.forceRun());
        if(upgradeTask.forceRun()) {
            upgradeTask.executeUpgrade();
        }

        //Check that the column was removed
        Assert.assertFalse("Column Exists and it shouldn't exists",upgradeTask.forceRun());
        //Check that the index was created
        dc.setSQL(CHECK_INDEX_QUERY).addParam("identifier").addParam("idx_ident_uniq_asset_name");
        Assert.assertFalse(dc.loadResults().isEmpty());
        //Check that the function was created
        dc.setSQL(CHECK_FUNCTION_QUERY).addParam("full_path_lc").addParam("public");
        Assert.assertFalse(dc.loadResults().isEmpty());
    }

    @Test
    public void test_upgradeTask_indexDoesNotExists_success() throws DotDataException, SQLException {
        final DotConnect dc = new DotConnect();
        //Add the column to the table
        dc.executeStatement(ADD_FULL_PATH_LC_COLUMN);
        //Remove the index
        dc.executeStatement(Task241013RemoveFullPathLcColumnFromIdentifier.DROP_INDEX);

        //Run UT
        final Task241013RemoveFullPathLcColumnFromIdentifier upgradeTask = new Task241013RemoveFullPathLcColumnFromIdentifier();
        Assert.assertTrue("Column Not Exists and it should exists",upgradeTask.forceRun());
        if(upgradeTask.forceRun()) {
            upgradeTask.executeUpgrade();
        }

        //Check that the column was removed
        Assert.assertFalse("Column Exists and it shouldn't exists",upgradeTask.forceRun());
        //Check that the index was created
        dc.setSQL(CHECK_INDEX_QUERY).addParam("identifier").addParam("idx_ident_uniq_asset_name");
        Assert.assertFalse(dc.loadResults().isEmpty());
        //Check that the function was created
        dc.setSQL(CHECK_FUNCTION_QUERY).addParam("full_path_lc").addParam("public");
        Assert.assertFalse(dc.loadResults().isEmpty());
    }

    @Test
    public void test_upgradeTask_columnNotPresent_success() throws DotDataException, SQLException {
        //Run UT
        final Task241013RemoveFullPathLcColumnFromIdentifier upgradeTask = new Task241013RemoveFullPathLcColumnFromIdentifier();
        Assert.assertFalse("Column Exists and it shouldn't exists",upgradeTask.forceRun());
    }

}
