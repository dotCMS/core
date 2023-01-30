package com.dotmarketing.quartz.job;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.experiments.model.Scheduling;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.quartz.DotStatefulJob;
import io.vavr.control.Try;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Stateful job used to end finalized {@link com.dotcms.experiments.model.Experiment}s
 * <p>
 *     A finalized Experiment is an Experiment that is in the {@link com.dotcms.experiments.model.Experiment.Status#RUNNING}
 *     state and whose {@link  Scheduling#endDate()} is in the past
 */
public class EndFinalizedExperimentsJob extends DotStatefulJob {

    @Override
    @WrapInTransaction
    public void run(final JobExecutionContext jobContext)
            throws JobExecutionException {
        Try.run(()->APILocator.getExperimentsAPI().endFinalizedExperiments(APILocator.systemUser()))
                .getOrElseThrow((e)->new JobExecutionException(e));
    }

}
