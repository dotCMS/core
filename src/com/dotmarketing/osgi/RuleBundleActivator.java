package com.dotmarketing.osgi;

import com.dotcms.repackage.org.apache.felix.http.api.ExtHttpService;
import com.dotcms.repackage.org.osgi.framework.BundleActivator;
import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotcms.repackage.org.osgi.framework.ServiceReference;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.util.CollectionsUtils;

import java.io.Serializable;
import java.util.List;

/**
 * Bundle for Rules activation.
 * @author jsanca
 */
public class RuleBundleActivator extends BaseBundleActivator {

    private final List<RuleActionlet> ruleActionLets;
    private final List<Conditionlet>  conditionLets;

    public RuleBundleActivator() {
        super();
        this.ruleActionLets = CollectionsUtils.getNewList();
        this.conditionLets  = CollectionsUtils.getNewList();
    }

    public RuleBundleActivator(final List<RuleActionlet> ruleActionLets,
             final List<Conditionlet>  conditionLets) {
        super();
        this.ruleActionLets = ruleActionLets;
        this.conditionLets  = conditionLets;
    }

    protected RuleBundleActivator(final GenericBundleActivator bundleActivator) {
        super(bundleActivator);
        this.ruleActionLets = CollectionsUtils.getNewList();
        this.conditionLets  = CollectionsUtils.getNewList();
    }

    protected RuleBundleActivator(final GenericBundleActivator bundleActivator,
                                  final List<RuleActionlet> ruleActionLets,
                                  final List<Conditionlet>  conditionLets) {
        super(bundleActivator);
        this.ruleActionLets = ruleActionLets;
        this.conditionLets  = conditionLets;
    }

    @Override
    public void start(final BundleContext bundleContext) throws Exception {

        super.start(bundleContext);

        final List<RuleActionlet> ruleActionLets =
                this.getRuleActionLets();
        final List<Conditionlet>  conditionLets  =
                this.getConditionLets();

        if (null != ruleActionLets) {

            for (RuleActionlet ruleActionlet : ruleActionLets) {
                //Registering the RuleActionlet
                this.registerRuleActionlet(bundleContext,
                        ruleActionlet);
            }
        }

        if (null != conditionLets) {

            for (Conditionlet conditionlet : conditionLets) {
                //Registering the Conditionlet
                this.registerRuleConditionlet(bundleContext,
                        conditionlet);
            }
        }
    }

    /**
     * Register a Rules Engine RuleActionlet service
     */
    @SuppressWarnings("unchecked")
    protected final void registerRuleActionlet(final BundleContext context,
                                               final RuleActionlet actionlet) {

        this.getBundleActivator().registerRuleActionlet(context, actionlet);
    } // registerRuleActionlet.

    /**
     * Register a Rules Engine Conditionlet service
     *
     * @param context
     * @param conditionlet
     */
    @SuppressWarnings ("unchecked")
    protected final void registerRuleConditionlet (final BundleContext context,
                                                   final Conditionlet conditionlet) {

        this.getBundleActivator().registerRuleConditionlet(context, conditionlet);
    } // registerRuleConditionlet.

    public List<RuleActionlet> getRuleActionLets() {
        return ruleActionLets;
    }

    public List<Conditionlet> getConditionLets() {
        return conditionLets;
    }
} // E:O:F:PublishBundleActivator.
