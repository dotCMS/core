package com.dotmarketing.quartz;

import com.dotmarketing.exception.DotRuntimeException;
import org.quartz.JobExecutionContext;

/**
 * Stateful job created for testing purpose only
 * @author nollymar
 */
public class DotStatefulTestJob extends DotStatefulJob{
    private static int threadsExecuted;

    @Override
    public void run(JobExecutionContext jobContext)  {
        if (!jobContext.getTrigger().getName().equals(this.getClass().getSimpleName() + "_trigger")){
            throw new DotRuntimeException("Trigger name is not correct for the stateful job");
        }

        threadsExecuted++;
    }

    public static int getThreadsExecuted() {
        return threadsExecuted;
    }
}
