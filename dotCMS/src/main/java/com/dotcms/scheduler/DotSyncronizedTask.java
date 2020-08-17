package com.dotcms.scheduler;

import java.time.Duration;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

/**
 * classes that extend this class checks to see if another 
 * instance of the class is running and if
 * so, reschedules this task instance 15 seconds in the future
 */
public abstract class DotSyncronizedTask extends DotTask {
    
    private static final long serialVersionUID = 1L;


    final int rescheduleDelay = Config.getIntProperty("DOTSYNCRONIZEDTASK_RESCHEDULE_DELAY_SEC", 15);
    
    @Override
    public final void runTask(String instanceId, DotTask task) {


        if (SchedulerAPI.getInstance().shouldIRun(instanceId, task)) {
            Logger.info(DotSyncronizedTask.class, "Running: " + task.getClass());
            super.runTask(instanceId, task);
            return;
        }

        Logger.info(DotSyncronizedTask.class, "Rescheduling: " + task.getClass() + " for " + rescheduleDelay  + "s in the future");
        SchedulerAPI.getInstance().scheduleOneTimeTask(task, Duration.ofSeconds(rescheduleDelay));

    }

}
