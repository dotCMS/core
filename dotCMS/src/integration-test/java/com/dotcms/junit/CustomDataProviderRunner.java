package com.dotcms.junit;

import com.dotcms.business.bytebuddy.ByteBuddyFactory;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.util.Arrays;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import org.junit.Ignore;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

public class CustomDataProviderRunner extends DataProviderRunner {

    public CustomDataProviderRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
        ByteBuddyFactory.init();
    }

    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
        Description description = describeChild(method);
        if (method.getAnnotation(Ignore.class) != null) {
            notifier.fireTestIgnored(description);
        } else {
            RunRules runRules = new RunRules(methodBlock(method),
                    Arrays.asList(new TestRule[]{new RuleWatcher()}), description);
            runLeaf(runRules, description, notifier);
            if (DbConnectionFactory.connectionExists())
            {
                Logger.error(CustomDataProviderRunner.class,"Connection remains after test = "+description+":"+DbConnectionFactory.connectionExists());
            }

        }
    }

}