package com.dotmarketing.portlets.rules.conditionlet;

import com.beust.jcommander.internal.Sets;
import com.dotcms.repackage.junit.framework.Assert;
import com.dotcms.repackage.org.junit.Test;
import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.osgi.HostActivator;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.ValidationResult;
import com.dotmarketing.portlets.rules.ValidationResults;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.util.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;
import java.util.Set;


public class ConditionletOSGIFTest {

    @Test
    public void registerRuleConditionlet_validConditionlet_success() throws Exception{
        BundleContext context = HostActivator.instance().getBundleContext();

        ConditionletActivator conditionletActivator = new ConditionletActivator();

        try {

            conditionletActivator.start(context);

            Assert.assertNotNull(APILocator.getRulesAPI().findConditionlet(UsersContinentConditionlet.class.getSimpleName()));

            conditionletActivator.stop(context);

            Assert.assertNull(APILocator.getRulesAPI().findConditionlet(UsersContinentConditionlet.class.getSimpleName()));

        } catch(Exception e) {
            Logger.error(ConditionletOSGIFTest.class, "Error starting/stoping ConditionletActivator", e);
            throw e;
        }

    }

    private static class ConditionletActivator extends GenericBundleActivator {

        @Override
        public void start(BundleContext bundleContext) throws Exception {

            //Initializing services...
            initializeServices(bundleContext);

            //Registering the Conditionlet
            registerRuleConditionlet(bundleContext, new UsersContinentConditionlet());
        }

        @Override
        public void stop(BundleContext bundleContext) throws Exception {
            unregisterConditionlets();
        }
    }


    public static class UsersContinentConditionlet extends Conditionlet<Instance> {

        private static final long serialVersionUID = 1L;

        public UsersContinentConditionlet() {
            super("User's Continent", Sets.newHashSet());
        }

        @Override
        public Collection<ConditionletInput> getInputs(String comparisonId) {
            return null;
        }

        @Override
        public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
            return false;
        }

        @Override
        public Instance instanceFrom(Comparison comparison, List<ParameterModel> values) {
            return null;
        }

    }

    protected static class Instance implements RuleComponentInstance{}

}
