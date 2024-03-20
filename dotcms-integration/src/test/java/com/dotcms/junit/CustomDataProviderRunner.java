package com.dotcms.junit;

import com.dotmarketing.util.Logger;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.internal.DataConverter;
import com.tngtech.java.junit.dataprovider.internal.TestGenerator;
import com.tngtech.java.junit.dataprovider.internal.TestValidator;
import org.junit.Ignore;
import org.junit.rules.RunRules;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import java.util.List;

public class CustomDataProviderRunner extends DataProviderRunner {

    public CustomDataProviderRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
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

    @Override
    protected void initializeHelpers() {
        dataConverter = new DataConverter();
        testGenerator = new TestGenerator(dataConverter) {
            @Override
            public List<FrameworkMethod> generateExplodedTestMethodsFor(FrameworkMethod testMethod,
                    FrameworkMethod dataProviderMethod) {

                if (dataProviderMethod!=null) {
                    FrameworkMethod loggedDataProviderMethod = new FrameworkMethod(dataProviderMethod.getMethod()) {
                        @Override
                        public Object invokeExplosively(Object target, Object... params) throws Throwable {
                            Logger.info(this, "START Data Provider Explode for " + this.getMethod() );
                            Object result = super.invokeExplosively(target, params);
                            Logger.info(this, "END Data Provider Explode for " + this.getMethod() );
                            return result;
                        }
                    };

                    return  super.generateExplodedTestMethodsFor(testMethod, loggedDataProviderMethod);

                }
                return super.generateExplodedTestMethodsFor(testMethod, null);

            }
        };
        testValidator = new TestValidator(dataConverter);
    }

}
