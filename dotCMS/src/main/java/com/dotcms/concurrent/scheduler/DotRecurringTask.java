package com.dotcms.concurrent.scheduler;

import java.io.Serializable;
import java.time.Duration;

/**
 * DotTask is a recurring task such as CRON Job, an expression must be provided
 * The instanceId must be unique and it will be the reference next operations
 * @author jsanca
 * @param <T>
 */
public class DotRecurringTask<T extends Runnable & Serializable> extends DotTask<T> {

    private final String cronExpression;

    public DotRecurringTask(final T runnable, final String instanceId, final String cronExpression) {
        super(runnable, instanceId);
        this.cronExpression = cronExpression;
    }

    public DotRecurringTask(T runnable, String instanceId, final String cronExpression,
                            final Duration initialDelay) {
        super(runnable, instanceId, initialDelay);
        this.cronExpression = cronExpression;
    }

    public String getCronExpression() {
        return cronExpression;
    }
}
