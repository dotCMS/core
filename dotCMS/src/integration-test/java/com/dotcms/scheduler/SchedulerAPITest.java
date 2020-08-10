package com.dotcms.scheduler;

import java.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;

public class SchedulerAPITest {
    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();


    }

    @Test
    public void test_sync_task() throws InterruptedException {

        TestSyncronizedTask task = new TestSyncronizedTask();
        TestSyncronizedTask task2 = new TestSyncronizedTask();
        TestSyncronizedTask task3 = new TestSyncronizedTask();

        
        APILocator.getSchedulerAPI().scheduleOneTimeTask(task);
        APILocator.getSchedulerAPI().scheduleOneTimeTask(task2);
        APILocator.getSchedulerAPI().scheduleOneTimeTask(task3);

        
        int i=0;
        while(TestSyncronizedTask.getNumberOfRuns()<3) {
            Thread.sleep(1000);
            i++;
            if(i>100) {
                // task never ran
                assert(false);
            }
        }
        assert(i >15);
        assert(i < 70);
        assert(TestSyncronizedTask.getNumberOfRuns()==3);

    }
    
    

    @Test
    public void test_recurring_task() throws InterruptedException {
        TestRecurringTask task = new TestRecurringTask();
        APILocator.getSchedulerAPI().scheduleRecurringTask(task, Duration.ofSeconds(5));
        Thread.sleep(60*1000);
        assert(TestRecurringTask.getNumberOfRuns()>3);
        
        
        
    
    }
}
