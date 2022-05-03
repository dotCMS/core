package com.dotcms.junit;

import com.dotcms.business.bytebuddy.ByteBuddyFactory;
import com.dotcms.util.StdOutErrLog;
import com.dotmarketing.db.DbConnectionFactory;
import java.util.LinkedList;
import java.util.List;

import com.dotmarketing.util.Logger;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

public class MainBaseSuite extends Suite {

    public MainBaseSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
        super(klass, getRunners(getAnnotatedClasses(klass)));
        StdOutErrLog.tieSystemOutAndErrToLog();
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

        System.out.println("Register ByteBuddy");
        ByteBuddyFactory.init();
        List<Runner> runners = new LinkedList<>();

        for (Class<?> klazz : classes) {
            runners.add(new DotRunner(new CustomDataProviderRunner(klazz)));
        }

        return runners;
    }


    private static class DotRunner extends Runner {

        private Runner runner;

        DotRunner(Runner runner) {
            this.runner = runner;
        }

        @Override
        public Description getDescription() {
            return this.runner.getDescription();
        }

        @Override
        public void run(RunNotifier notifier) {
            try {
                this.runner.run(notifier);
            } finally {
                if (DbConnectionFactory.inTransaction())
                    Logger.error(DotRunner.class,"Test "+this.getDescription()+" has open transaction after");

                if (DbConnectionFactory.connectionExists())
                    Logger.error(DotRunner.class,"Test "+this.getDescription()+" has open connection after");
                DbConnectionFactory.closeSilently();
            }
        }
    }
}