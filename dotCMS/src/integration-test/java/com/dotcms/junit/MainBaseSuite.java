package com.dotcms.junit;

import com.dotcms.util.StdOutErrLog;
import java.util.LinkedList;
import java.util.List;
import org.junit.runner.Runner;
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

        List<Runner> runners = new LinkedList<>();

        for (Class<?> klazz : classes) {
            runners.add(new CustomDataProviderRunner(klazz));
        }

        return runners;
    }

}