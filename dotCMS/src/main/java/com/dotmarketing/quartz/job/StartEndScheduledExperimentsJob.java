package com.dotmarketing.quartz.job;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.quartz.DotStatefulJob;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.control.Try;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Stateful job used to start and end scheduled {@link com.dotcms.experiments.model.Experiment}s
 */
public class StartEndScheduledExperimentsJob extends DotStatefulJob {

    final ExperimentsAPI experimentsAPI;
    public StartEndScheduledExperimentsJob() {
        this(APILocator.getExperimentsAPI());
    }

    @VisibleForTesting
    public StartEndScheduledExperimentsJob(final ExperimentsAPI experimentsAPI) {
        this.experimentsAPI = experimentsAPI;
    }

    @Override
    @WrapInTransaction
    public void run(final JobExecutionContext jobContext)
            throws JobExecutionException {

        if (LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level) {
            Try.run(() -> experimentsAPI.startScheduledToStartExperiments(APILocator.systemUser()))
                    .getOrElseThrow((e) -> new JobExecutionException(e));

            Try.run(() -> experimentsAPI.endFinalizedExperiments(APILocator.systemUser()))
                    .getOrElseThrow((e) -> new JobExecutionException(e));
        }
    }

}