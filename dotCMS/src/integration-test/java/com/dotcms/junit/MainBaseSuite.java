package com.dotcms.junit;

import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.business.bytebuddy.ByteBuddyFactory;
import com.dotcms.util.StdOutErrLog;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.reindex.ReindexQueueAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import java.util.LinkedList;
import java.util.List;

import com.dotmarketing.util.Logger;
import java.util.Map;
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
            Config.getOverrides().forEach((k,v)->{
                Logger.warn(this.getClass(), ()->"Config overrides before test: " + k + " = " + v);
            });

            Logger.info(MainBaseSuite.class,"Checking indexer status");

            try {
                int waitTime=60;
                ReindexQueueAPI queueAPI = APILocator.getReindexQueueAPI();
                if (queueAPI.areRecordsLeftToIndex()) {
                    Logger.info(MainBaseSuite.class,"Indexer is not idle, waiting for it to finish");
                    boolean queueEmpty = queueAPI.waitForEmptyQueue(waitTime);
                    if (queueEmpty) {
                        Logger.info(MainBaseSuite.class,"Indexer Completed");
                    } else {
                        Logger.info(MainBaseSuite.class,"Indexer did not complete after "+waitTime+" seconds");
                    }
                }
            } catch (DotDataException e) {
                throw new RuntimeException(e);
            }


            Map<String, String> overrideTracker = Config.getOverrides();
            try {

                this.runner.run(notifier);
            } finally {
                if (DbConnectionFactory.inTransaction())
                    Logger.error(DotRunner.class,"Test "+this.getDescription()+" has open transaction after");

                if (DbConnectionFactory.connectionExists())
                    Logger.error(DotRunner.class,"Test "+this.getDescription()+" has open connection after");
                DbConnectionFactory.closeSilently();

                Map<String, String> modifiedOverrides = Config.compareOverrides(overrideTracker);
                if (!modifiedOverrides.isEmpty()) {
                    String mapContents =  modifiedOverrides.entrySet().stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValue()).reduce((a, b) -> a + ", " + b).orElse("empty");
                    Logger.warn(IntegrationTestBase.class, "Modified Config overrides after:" + mapContents);
                }
            }
        }
    }
}