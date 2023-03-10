package com.dotmarketing.quartz.job;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.quartz.DotStatefulJob;
import io.vavr.control.Try;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Stateful job used to start and end scheduled {@link com.dotcms.experiments.model.Experiment}s
 */
public class StartEndScheduledExperimentsJob extends DotStatefulJob {

    @Override
    @WrapInTransaction
    public void run(final JobExecutionContext jobContext)
            throws JobExecutionException {

        Try.run(()->APILocator.getExperimentsAPI().startScheduledToStartExperiments(APILocator.systemUser()))
                .getOrElseThrow((e)->new JobExecutionException(e));

        Try.run(()->APILocator.getExperimentsAPI().endFinalizedExperiments(APILocator.systemUser()))
                .getOrElseThrow((e)->new JobExecutionException(e));
    }

}