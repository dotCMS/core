package com.dotcms.junit;

import com.dotcms.business.bytebuddy.ByteBuddyFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.StdOutErrLog;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import java.util.LinkedList;
import java.util.List;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

public class MainBaseSuite extends Suite {

    public MainBaseSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
        super(klass, getRunners(getAnnotatedClasses(klass)));
    }

    // copied from Suite
    private static Class<?>[] getAnnotatedClasses(Class<?> klass) throws InitializationError {
        SuiteClasses annotation = klass.getAnnotation(SuiteClasses.class);
        if (annotation == null) {
            throw new InitializationError(
                    String.format("class '%s' must have a SuiteClasses annotation",
                            klass.getName()));
        }
        return annotation.value();
    }

    private static List<Runner> getRunners(Class<?>[] classes) throws InitializationError {
        StdOutErrLog.tieSystemOutAndErrToLog();
        ByteBuddyFactory.init();
        try {
            Logger.info(MainBaseSuite.class, "START IntegrationTestInit *****************************");
            IntegrationTestInitService.getInstance().init();
            Logger.info(MainBaseSuite.class, "EMD  IntegrationTestInit *****************************");

        } catch (Exception e) {
            throw new DotRuntimeException("Failed to initialize Integration tests", e);
        }

        List<Runner> runners = new LinkedList<>();

        for (Class<?> klazz : classes) {
            runners.add(new DotRunner(new CustomDataProviderRunner(klazz)));
        }

        return runners;
    }


    /**
     * Thin wrapper around the per-class runner. It must implement {@link Filterable}: JUnit's
     * {@code Filter.apply} silently skips any child runner that is not Filterable, so without it
     * failsafe's {@code rerunFailingTestsCount} (which filters the suite by the failed
     * descriptions) re-ran every method of a failing class instead of only the failed ones.
     */
    private static class DotRunner extends Runner implements Filterable {

        private final Runner runner;

        DotRunner(Runner runner) {
            this.runner = runner;
        }

        @Override
        public Description getDescription() {
            return this.runner.getDescription();
        }

        @Override
        public void run(RunNotifier notifier) {
            this.runner.run(notifier);
        }

        /**
         * Applies the filter to the wrapped runner so only the matching test methods remain.
         *
         * @param filter the filter to apply
         * @throws NoTestsRemainException if the wrapped runner has no matching test left
         */
        @Override
        public void filter(Filter filter) throws NoTestsRemainException {
            filter.apply(this.runner);
        }
    }
}
