package com.dotcms.scheduler;

import java.time.Duration;
import com.dotmarketing.util.Logger;
import com.github.kagkarlsson.scheduler.task.TaskInstance;

public abstract class DotSyncronizedTask extends DotTask {


    public final void runTask(final TaskInstance<DotTask> task) {


        if (SchedulerAPI.getInstance().shouldIRun(task.getId(), task.getData())) {
            Logger.info(DotSyncronizedTask.class, "Running: " + task.getData().getClass());
            super.runTask(task);
            return;
        }

        Logger.info(DotSyncronizedTask.class, "Waiting for other task to finish: " + task.getData().getClass());
        SchedulerAPI.getInstance().scheduleOneTimeTask(task.getData(), Duration.ofSeconds(15));

    }

}
