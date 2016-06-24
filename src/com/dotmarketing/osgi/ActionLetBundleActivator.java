package com.dotmarketing.osgi;

import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.util.CollectionsUtils;

import java.util.List;

/**
 * Bundle for Actions Let activation.
 * @author jsanca
 */
public class ActionLetBundleActivator extends BaseBundleActivator {

    private final List<WorkFlowActionlet> workFlowActionlets;

    public ActionLetBundleActivator() {

        super();
        this.workFlowActionlets =
                CollectionsUtils.getNewList();
    }

    public ActionLetBundleActivator(final List<WorkFlowActionlet> workFlowActionlets) {

        super();
        this.workFlowActionlets =
                workFlowActionlets;
    }

    protected ActionLetBundleActivator(final GenericBundleActivator bundleActivator) {
        super(bundleActivator);
        this.workFlowActionlets =
                CollectionsUtils.getNewList();
    }

    protected ActionLetBundleActivator(final GenericBundleActivator bundleActivator, final List<WorkFlowActionlet> workFlowActionlets) {
        super(bundleActivator);
        this.workFlowActionlets =
                workFlowActionlets;
    }

    @Override
    public void start(BundleContext bundleContext) throws Exception {

        super.start(bundleContext);

        final List<WorkFlowActionlet> actionlets =
                this.getWorkFlowActionlets();

        if (null != actionlets) {

            for (WorkFlowActionlet workFlowActionlet : actionlets) {

                this.registerActionlet(bundleContext, workFlowActionlet);
            }
        }
    }

    /**
     * Register a WorkFlowActionlet service
     *
     * @param context
     * @param actionlet
     */
    protected final void registerActionlet ( final BundleContext context,
                                             final WorkFlowActionlet actionlet ) {

        if (null != actionlet) {

            this.getBundleActivator().registerActionlet(context, actionlet);
        }
    } // registerActionlet.

    protected  List<WorkFlowActionlet> getWorkFlowActionlets () {

        return this.workFlowActionlets;
    }
} // E:O:F:ActionLetBundleActivator.
