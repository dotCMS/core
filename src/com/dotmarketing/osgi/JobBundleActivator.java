package com.dotmarketing.osgi;

import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.util.CollectionsUtils;

import java.util.List;

/**
 * Bundle for Jobs activation.
 * @author jsanca
 */
public class JobBundleActivator  extends BaseBundleActivator  {

    private final List<ScheduledTask> jobs;

    public JobBundleActivator() {
        super();
        this.jobs =
                CollectionsUtils.getNewList();
    }

    public JobBundleActivator(final List<ScheduledTask> jobs) {
        super();
        this.jobs =
                jobs;
    }

    protected JobBundleActivator(final GenericBundleActivator bundleActivator) {
        super(bundleActivator);
        this.jobs =
                CollectionsUtils.getNewList();
    }

    protected JobBundleActivator(final GenericBundleActivator bundleActivator,
                                 final List<ScheduledTask> jobs) {
        super(bundleActivator);
        this.jobs =
                jobs;
    }

    @Override
    public void start(final BundleContext bundleContext) throws Exception {

        super.start(bundleContext);

        final List<ScheduledTask> jobs =
                this.getJobs();

        if (null != jobs) {

            for (ScheduledTask scheduledTask : jobs) {

                this.scheduleQuartzJob(scheduledTask);
            }
        }
    }

    /**
     * Register a given Quartz Job scheduled task
     *
     * @param scheduledTask
     * @throws Exception
     */
    protected final void scheduleQuartzJob ( final ScheduledTask scheduledTask ) throws Exception {

        this.getBundleActivator().scheduleQuartzJob(scheduledTask);
    } // scheduleQuartzJob.

    protected List<ScheduledTask> getJobs() {

        return this.jobs;
    }
} // E:O:F:ActionLetBundleActivator.
