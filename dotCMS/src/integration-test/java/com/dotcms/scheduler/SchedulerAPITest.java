package com.dotcms.scheduler;

import java.time.Duration;
import java.util.Random;
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

        int rand1 = new Random().nextInt(100);
        int rand2 = new Random().nextInt(100);
        int rand3 = new Random().nextInt(100);
        
        final int expecting =rand1 + rand2 +rand3;
        
        
        TestSyncronizedTask task = new TestSyncronizedTask(rand1);
        TestSyncronizedTask task2 = new TestSyncronizedTask(rand2);
        TestSyncronizedTask task3 = new TestSyncronizedTask(rand3);

        
        APILocator.getSchedulerAPI().scheduleOneTimeTask(task);
        APILocator.getSchedulerAPI().scheduleOneTimeTask(task2);
        APILocator.getSchedulerAPI().scheduleOneTimeTask(task3);

        
        int i=0;
        while(TestSyncronizedTask.getTotalIncrement()<expecting) {
            Thread.sleep(1000);
            i++;
            if(i>70) {
                assert(false);
            }
        }
        
        // this took less than 70 seconds to run
        assert(i >15 && i< 70);
        // we got what we were expecting
        assert(TestSyncronizedTask.getTotalIncrement()==expecting);

    }
    
    

    @Test
    public void test_recurring_task() throws InterruptedException {
        TestRecurringTask task = new TestRecurringTask();
        APILocator.getSchedulerAPI().scheduleRecurringTask(task, Duration.ofSeconds(5));
        Thread.sleep(60*1000);
        assert(TestRecurringTask.getNumberOfRuns()>3);
        
        
        
    
    }
}
