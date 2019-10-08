package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task05200WorkflowTaskUniqueKeyTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private void removeConstraintIfAny() throws DotDataException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        //Certain db engines store unique constraints as indices
        final DotConnect dotConnect = new DotConnect();
        try {
           dotConnect.setSQL("alter table workflow_task drop index unique_workflow_task");
           dotConnect.loadResult();
       }catch (DotDataException e){
           //Nah.
       }
        try {
            dotConnect.setSQL("alter table workflow_task drop constraint unique_workflow_task");
            dotConnect.loadResult();
        }catch (DotDataException e){
            //Nah.
        }
    }

    @Test
    public void Test_Upgrade_Task() throws DotDataException {
        removeConstraintIfAny();
        final Task05200WorkflowTaskUniqueKey task =  new Task05200WorkflowTaskUniqueKey();
        assertTrue(task.forceRun());
        task.executeUpgrade();
    }

}
