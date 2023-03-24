package com.dotcms.junit;

import com.dotcms.business.bytebuddy.ByteBuddyFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.util.List;
import org.junit.Ignore;
import org.junit.rules.RunRules;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

public class CustomDataProviderRunner extends DataProviderRunner {

    public CustomDataProviderRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
        ByteBuddyFactory.init();
        // Make sure integration test init service is initialized for Data Providers
        // these run when the tests to run are determined, not when the tests are run
        // so @BeforeClass methods are not run before this.
        // It is better to have DataProviders not do any database work, but if they do
        try {
            IntegrationTestInitService.getInstance().init();
        } catch (Exception e) {
            throw new InitializationError(e);
        }
    }

    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
        Description description = describeChild(method);
        if (method.getAnnotation(Ignore.class) != null) {
            notifier.fireTestIgnored(description);
        } else {
            RunRules runRules = new RunRules(methodBlock(method),
                    List.of(new RuleWatcher()), description);
            runLeaf(runRules, description, notifier);
        }
    }

}