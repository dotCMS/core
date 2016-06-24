package com.dotmarketing.osgi;

import com.dotcms.repackage.org.osgi.framework.BundleActivator;
import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.util.CollectionsUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ReflectionUtils;
import org.apache.velocity.tools.view.ToolInfo;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * In case you need to add two or more bundle activator to the bundle context, you might want to use
 * @author jsanca
 */
public class CompositeBundleActivator implements BundleActivator, Serializable {

    private final GenericBundleActivator genericBundleActivator =
            new ConcreteGenericBundleActivator();

    private final List<BundleActivator> bundleActivators;

    private final boolean callTnitializeServices;

    private final boolean callUnregisterServices;

    public CompositeBundleActivator(final boolean callTnitializeServices,
                                    final boolean callUnregisterServices) {

        this.bundleActivators = CollectionsUtils.getNewList();
        this.callTnitializeServices = callTnitializeServices;
        this.callUnregisterServices = callUnregisterServices;
    }

    public CompositeBundleActivator(final List<BundleActivator> bundleActivators,
                                    final boolean callTnitializeServices,
                                    final boolean callUnregisterServices) {

        this.bundleActivators = bundleActivators;
        this.callTnitializeServices = callTnitializeServices;
        this.callUnregisterServices = callUnregisterServices;
    }

    @Override
    public void start(final BundleContext bundleContext) throws Exception {

        if (this.callTnitializeServices) {

            this.genericBundleActivator.initializeServices(bundleContext);
        }

        for (BundleActivator bundleActivator: this.bundleActivators) {

            bundleActivator.start(bundleContext);
        }
    }

    @Override
    public void stop(final BundleContext bundleContext) throws Exception {

        Collections.reverse(this.bundleActivators);
        for (BundleActivator bundleActivator: this.bundleActivators) {

            bundleActivator.stop(bundleContext);
        }

        if (this.callUnregisterServices) {

            this.genericBundleActivator.unregisterServices(bundleContext);
        }
    }

    /**
     * Adds a custom activator.
     * The class needs a constructor with a {@link GenericBundleActivator} as parameter, that will be the component share between all the activators.
     * @param activatorClass {@link BundleActivator}
     * @return CompositeBundleActivator
     */
    protected CompositeBundleActivator addCustomActivator (final Class<BundleActivator> activatorClass) {

        final BundleActivator bundleActivator = ReflectionUtils.newInstance(activatorClass,
                this.genericBundleActivator);

        if (null != bundleActivator) {

            this.bundleActivators.add(bundleActivator);
        } else {

            if (Logger.isWarnEnabled(CompositeBundleActivator.class)) {

                Logger.warn(CompositeBundleActivator.class, "Couldn't load the bundle activator: " + activatorClass.getName());
            }
        }

        return this;
    }

    /**
     * Adds a Portlet Action Bundle activator
     * @param portletActions {@link List}
     * @return CompositeBundleActivator
     */
    protected CompositeBundleActivator addPortletActionBundleActivator (final List<PortletActionBean> portletActions) {

        final BundleActivator bundleActivator =
                new PortletActionBundleActivator(this.genericBundleActivator,
                        portletActions);

        this.bundleActivators.add(bundleActivator);

        return this;
    }

    /**
     * Adds an Action Let Bundle activator
     * Bundle activator
     * @param workFlowActionlets {@link List}
     * @return CompositeBundleActivator
     */
    protected CompositeBundleActivator addActionLetBundleActivator (final List<WorkFlowActionlet> workFlowActionlets) {

        final BundleActivator bundleActivator =
                new ActionLetBundleActivator(this.genericBundleActivator,
                        workFlowActionlets);

        this.bundleActivators.add(bundleActivator);

        return this;
    }

    /**
     * Adds an Filter Bundle activator
     * Bundle activator
     * @param filters {@link List}
     * @return CompositeBundleActivator
     */
    public CompositeBundleActivator addFilterBundleActivator (final List<FilterBean> filters) {

        final BundleActivator bundleActivator =
                new FilterBundleActivator(this.genericBundleActivator,
                        filters);

        this.bundleActivators.add(bundleActivator);

        return this;
    }

    /**
     * Adds a Hooks bundle activator
     * Bundle activator
     * @param hooks {@link List}
     * @return CompositeBundleActivator
     */
    public CompositeBundleActivator addHooksBundleActivator (final List<HooksBean> hooks) {

        final BundleActivator bundleActivator =
                new HooksBundleActivator(this.genericBundleActivator,
                        hooks);

        this.bundleActivators.add(bundleActivator);

        return this;
    }


    /**
     * Adds a job Bundle Activator
     * Bundle activator
     * @param jobs {@link List}
     * @return CompositeBundleActivator
     */
    public CompositeBundleActivator addJobBundleActivator (final List<ScheduledTask> jobs) {

        final BundleActivator bundleActivator =
                new JobBundleActivator(this.genericBundleActivator,
                        jobs);

        this.bundleActivators.add(bundleActivator);

        return this;
    }

    /**
     * Adds a Servlet Bundle Activator
     * @param servlets {@link List}
     * @return CompositeBundleActivator
     */
    public CompositeBundleActivator addServletPublishBundleActivator (final List<ServletBean> servlets) {

        final BundleActivator bundleActivator =
                new ServletBundleActivator(this.genericBundleActivator,
                        servlets);

        this.bundleActivators.add(bundleActivator);

        return this;
    }

    /**
     * Adds a Dwr Servlet Bundle Activator
     * @param dwrServlets {@link List}
     * @return CompositeBundleActivator
     */
    public CompositeBundleActivator addDwrPublishBundleActivator (final List<DwrServletBean> dwrServlets) {

        final BundleActivator bundleActivator =
                new DwrPublishBundleActivator(this.genericBundleActivator,
                        dwrServlets);

        this.bundleActivators.add(bundleActivator);

        return this;
    }

    /**
     * Adds a Spring Servlet Bundle Activator
     * @param springServlets {@link List}
     * @return CompositeBundleActivator
     */
    public CompositeBundleActivator addSpringPublishBundleActivator (final List<SpringServletBean> springServlets) {

        final BundleActivator bundleActivator =
                new SpringPublishBundleActivator(this.genericBundleActivator,
                        springServlets);

        this.bundleActivators.add(bundleActivator);

        return this;
    }

    /**
     * Adds a rest bundle activator
     * @param resourcesClass {@link List}
     * @return CompositeBundleActivator
     */
    public CompositeBundleActivator addRestBundleActivator (final List<Class> resourcesClass) {

        final BundleActivator bundleActivator =
                new RestBundleActivator(this.genericBundleActivator,
                        resourcesClass);

        this.bundleActivators.add(bundleActivator);

        return this;
    }

    /**
     * Adds a rules bundle activator
     * @param ruleActionLets {@link List}
     * @param conditionLets {@link List}
     * @return CompositeBundleActivator
     */
    public CompositeBundleActivator addRestBundleActivator (final List<RuleActionlet> ruleActionLets,
                                           final List<Conditionlet>  conditionLets) {

        this.bundleActivators.add(new RuleBundleActivator(this.genericBundleActivator,
                ruleActionLets, conditionLets));

        return this;
    }

    /**
     * Adds a services bundle activator
     * @param services {@link List}
     * @return CompositeBundleActivator
     */
    public CompositeBundleActivator addServicesBundleActivator (final List<ServiceBean<?>> services) {

        this.bundleActivators.add(new ServiceBundleActivator(this.genericBundleActivator,
                services));

        return this;
    }


    /**
     * Adds a services bundle activator
     * @param tools {@link List}
     * @return CompositeBundleActivator
     */
    public CompositeBundleActivator addToolInfoBundleActivator (final List<ToolInfo> tools) {

        this.bundleActivators.add(new ToolInfoBundleActivator(this.genericBundleActivator,
                tools));

        return this;
    }

    /**
     * Adds a services bundle activator
     * @param tuckeys {@link List}
     * @return CompositeBundleActivator
     */
    public CompositeBundleActivator addTuckeyBundleActivator (final List<TuckeyBean> tuckeys) {

        this.bundleActivators.add(new TuckeyBundleActivator(this.genericBundleActivator,
                tuckeys));

        return this;
    }

} // E:O:F:CompositeBundleActivator.
