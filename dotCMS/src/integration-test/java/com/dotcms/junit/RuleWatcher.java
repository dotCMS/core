package com.dotcms.junit;

import com.dotmarketing.util.Logger;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class RuleWatcher extends TestWatcher {

    @Override
    protected void starting(Description description) {
        Logger.info(RuleWatcher.class, ">>>>>>>>>>>>>>>>>>>>>>>>>>");
        Logger.info(RuleWatcher.class,
                String.format(">>> %s - %s",
                        description,
                        "starting..."));
        Logger.info(RuleWatcher.class, ">>>>>>>>>>>>>>>>>>>>>>>>>>");
    }

}