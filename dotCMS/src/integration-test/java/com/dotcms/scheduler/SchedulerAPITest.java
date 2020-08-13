package com.dotcms.scheduler;

import java.util.Random;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerName;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;

public class SchedulerAPITest {
    
    @CloseDBIfOpened
    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        new DotConnect().setSQL("delete from scheduled_tasks").loadResult();

    }

    private final OneTimeTask<DotTask> oneTime =
                    Tasks.oneTime("one-time-task", DotTask.class).execute((inst, ctx) -> {
                        inst.getData().runTask(inst.getId(), inst.getData());
                    });
    

    
    /**
     * This method starts another scheduler (like one running on another server)- along with the
     * scheduler embedded in the SchedulerAPI to ensure that DotSyncronized tasks run in series, one at
     * a time
     * 
     * @throws InterruptedException
     */
    @Test
    public void test_sync_task() throws Exception {

        

        Scheduler scheduler = Scheduler.create(DbConnectionFactory.getDataSource(), oneTime)
                        .threads(Config.getIntProperty("SCHEDULER_NUMBER_OF_THREADS", 10))
                        .schedulerName(new SchedulerName() {
                            @Override
                            public String getName() {
                                return "testing:" + UUIDGenerator.shorty();
                            }
                        })
                        .enableImmediateExecution()
                        .build();

        scheduler.start();


        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Logger.info(this.getClass(), "Shutting down scheduler");
                scheduler.stop();
            }
        });

        
        
        
        
        int rand1 = new Random().nextInt(100);
        int rand2 = new Random().nextInt(100);
        int rand3 = new Random().nextInt(100);
        int rand4 = new Random().nextInt(100);
        
        final int expecting =rand1 + rand2 +rand3 + rand4;
        
        // if all four threads run, then we are expecting to see
        // the sum of the 4 random numbers
        APILocator.getSchedulerAPI().scheduleOneTimeTask(new TestSyncronizedTask(rand1));
        APILocator.getSchedulerAPI().scheduleOneTimeTask(new TestSyncronizedTask(rand2));
        APILocator.getSchedulerAPI().scheduleOneTimeTask(new TestSyncronizedTask(rand3));
        APILocator.getSchedulerAPI().scheduleOneTimeTask(new TestSyncronizedTask(rand4));
        
        int i=0;
        while(TestSyncronizedTask.getTotalIncrement()<expecting) {
            Thread.sleep(1000);
            if(++i>100) {
               break;
            }
        }
        
        // this took more than 30 and less than 100 seconds to run
        assert(i >30 && i< 100);
        // we got what we were expecting
        assert(TestSyncronizedTask.getTotalIncrement()==expecting);

    }
    

}
