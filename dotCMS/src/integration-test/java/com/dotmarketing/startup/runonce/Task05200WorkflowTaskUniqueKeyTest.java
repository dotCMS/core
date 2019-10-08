package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task05200WorkflowTaskUniqueKeyTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void Test_Upgrade_Task()
            throws DotDataException {
        final Task05200WorkflowTaskUniqueKey task =  new Task05200WorkflowTaskUniqueKey();
        if(task.forceRun()){
           task.executeUpgrade();
        }
    }

}
