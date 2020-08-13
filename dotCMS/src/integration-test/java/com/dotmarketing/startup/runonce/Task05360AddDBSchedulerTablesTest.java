package com.dotmarketing.startup.runonce;

import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;

public class Task05360AddDBSchedulerTablesTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }
    
    @Test
    public void test() throws Exception {
        if(new DotDatabaseMetaData().tableExists(
                    DbConnectionFactory.getConnection(), "scheduled_tasks")){
            new DotConnect().executeStatement("drop table scheduled_tasks");
            
            
        }
        
        assert(!new DotDatabaseMetaData().tableExists(
                    DbConnectionFactory.getConnection(), "scheduled_tasks"));
        
        
        
        Task05360AddDBSchedulerTables task = new Task05360AddDBSchedulerTables();
        
        //task should run
        assert(task.forceRun());

        task.executeUpgrade();
        
        // assert table exists
        assert(new DotDatabaseMetaData().tableExists(
                        DbConnectionFactory.getConnection(), "scheduled_tasks"));
        
    }

}
