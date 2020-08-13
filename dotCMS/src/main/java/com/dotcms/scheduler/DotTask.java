package com.dotcms.scheduler;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public abstract class DotTask implements Serializable {


    private static final long serialVersionUID = 1L;

    final String name() {
        return this.getClass().getName();
    }

    public final Map<String, Serializable> map = new HashMap<>();

    DotTask task;
    String instanceId;

    public void runTask(String instanceId, DotTask task) {
        this.instanceId = instanceId;
        this.task = task;
        this.execute();
    }

    public abstract void execute();

    public Duration initialDelay() {
        return Duration.ofMillis(0);
    }


}
