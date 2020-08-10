package com.dotcms.scheduler;

import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

public class TestRecurringTask extends DotTask {


    public final static int getNumberOfRuns() {
        return Try.of(() -> Integer.parseInt(System.getProperty("TestRecurringTask", "0"))).getOrElse(0);
    }


    @Override
    public void execute() {

        try {


            int runs = getNumberOfRuns();
            Logger.warn(this.getClass(), "TestRecurringTask runs:" + runs);


            System.setProperty("TestRecurringTask", String.valueOf(++runs));

        } finally {
            System.clearProperty("TestRecurringTask");

        }
    }

}
