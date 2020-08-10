package com.dotcms.scheduler;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import com.github.kagkarlsson.scheduler.task.TaskInstance;

public abstract class DotTask implements Serializable {


    private static final long serialVersionUID = 1L;

    final String name() {
        return this.getClass().getName();
    }

    public final Map<String, Serializable> map = new HashMap<>();

    TaskInstance<DotTask> task;

    public void runTask(TaskInstance<DotTask> task) {
        this.task = task;
        this.execute();
    }

    public abstract void execute();

    public Duration initialDelay() {
        return Duration.ofMillis(0);
    }


}
