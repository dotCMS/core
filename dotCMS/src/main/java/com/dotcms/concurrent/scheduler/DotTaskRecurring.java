package com.dotcms.concurrent.scheduler;

import java.io.Serializable;
import java.time.Duration;

/**
 * DotTask is a recurring task such as CRON Job, an expression must be provided
 * The instanceId must be unique and it will be the reference next operations
 * @author jsanca
 * @param <T>
 */
public class DotTaskRecurring<T extends Runnable & Serializable>  extends DotTask {

    private final String cronExpression;

    public DotTaskRecurring(T runnable, String instanceId, final String cronExpression) {
        super(runnable, instanceId);
        this.cronExpression = cronExpression;
    }

    public DotTaskRecurring(T runnable, String instanceId, final String cronExpression, Duration initialDelay) {
        super(runnable, instanceId, initialDelay);
        this.cronExpression = cronExpression;
    }

    public String getCronExpression() {
        return cronExpression;
    }
}
