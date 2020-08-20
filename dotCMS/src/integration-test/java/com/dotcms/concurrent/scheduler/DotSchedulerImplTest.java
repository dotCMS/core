package com.dotcms.concurrent.scheduler;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;

public class DotSchedulerImplTest {

    private final DotScheduler dotScheduler = DotConcurrentFactory.getInstance().getScheduler();

    private final OneTimeTask<DotTask> oneTime = Tasks
            .oneTime(
                    "one-time-task",
                    DotTask.class)
            .execute(dotScheduler::executeOnce);

    @CloseDBIfOpened
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        new DotConnect().setSQL("delete from scheduled_tasks").loadResult();
    }

    /**
     * This method starts another scheduler (like one running on another server)- along with the
     * scheduler embedded in the SchedulerAPI to ensure that Dot Synchronized tasks run in series, one at
     * a time
     *
     * @throws InterruptedException
     */
    @Test
    public void test_sync_task() throws Exception {
        Scheduler scheduler = Scheduler.create(DbConnectionFactory.getDataSource(), oneTime)
                .threads(Config.getIntProperty("SCHEDULER_NUMBER_OF_THREADS", 10))
                .schedulerName(() -> "testing:" + UUIDGenerator.shorty())
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

        final int expecting = rand1 + rand2 + rand3 + rand4;

        // if all four threads run, then we are expecting to see
        // the sum of the 4 random numbers
        dotScheduler.start();

        dotScheduler.fire(new DotTask(new TestSynchronizedRunnable(rand1), "TestSynchronizedTask1"));
        dotScheduler.fire(new DotTask(new TestSynchronizedRunnable(rand2), "TestSynchronizedTask2"));
        dotScheduler.fire(new DotTask(new TestSynchronizedRunnable(rand3), "TestSynchronizedTask3"));
        dotScheduler.fire(new DotTask(new TestSynchronizedRunnable(rand4), "TestSynchronizedTask4"));

        int i = 0;
        while(TestSynchronizedRunnable.getTotalIncrement() < expecting) {
            Thread.sleep(1000);
            if (++i > 100) {
                break;
            }
        }

        // this took more than 30 and less than 100 seconds to run
        //assert(i >30 && i< 100);
        // we got what we were expecting
        assert(TestSynchronizedRunnable.getTotalIncrement() == expecting);
    }

}
