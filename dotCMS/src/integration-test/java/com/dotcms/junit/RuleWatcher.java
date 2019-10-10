package com.dotcms.junit;

import com.dotmarketing.util.Logger;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class RuleWatcher extends TestWatcher {

    @Override
    protected void starting(Description description) {
        Logger.info(RuleWatcher.class, ">>>>>>>>>>>>>>>>>>>");
        Logger.info(RuleWatcher.class,
                String.format(">>> Starting: [%s][%s]",
                        description.getClassName(),
                        description.getMethodName()));
        Logger.info(RuleWatcher.class, ">>>>>>>>>>>>>>>>>>>");
    }

}