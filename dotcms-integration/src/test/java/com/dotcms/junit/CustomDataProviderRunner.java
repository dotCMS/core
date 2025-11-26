package com.dotcms.junit;

import static com.dotcms.util.IntegrationTestInitService.CONTAINER;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.JUnit4WeldRunner;
import com.dotmarketing.util.Logger;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.internal.DataConverter;
import com.tngtech.java.junit.dataprovider.internal.TestGenerator;
import com.tngtech.java.junit.dataprovider.internal.TestValidator;
import java.util.List;
import java.util.Optional;
import org.junit.Ignore;
import org.junit.rules.RunRules;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

public class CustomDataProviderRunner extends DataProviderRunner {

    // We assume that any test annotated with any of the following runners is meant to be run with Weld
    static final List<Class<?>> weldRunners = List.of(JUnit4WeldRunner.class, DataProviderWeldRunner.class);

    /**
     * Check if the given class is annotated with any of the Weld runners
     * @param clazz the class to check
     * @return true if the class is annotated with any of the Weld runners
     */
    static boolean isWeldRunnerPresent(Class<?> clazz) {
        return Optional.ofNullable(clazz.getAnnotation(RunWith.class))
                .map(RunWith::value)
                .map(runnerClass -> weldRunners.stream()
                        .anyMatch(weldRunner -> weldRunner.equals(runnerClass)))
                .orElse(false);
    }

    private final boolean instantiateWithWeld;

    public CustomDataProviderRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
        instantiateWithWeld = isWeldRunnerPresent(clazz);
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

    @Override
    protected Object createTest() throws Exception {
        if (instantiateWithWeld) {
            final Class<?> javaClass = getTestClass().getJavaClass();
            Logger.debug(this, String.format("Instantiating [%s] with Weld", javaClass));
            return CONTAINER.select(javaClass).get();
        }
        return super.createTest();
    }
}
