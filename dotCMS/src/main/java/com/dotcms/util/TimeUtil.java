package com.dotcms.util;

import com.dotmarketing.util.Config;

import java.util.function.Supplier;

public class TimeUtil {

    public static void waitFor (final long waitTime, final long maxTime, final Supplier<Boolean> breakCondition)
        throws InterruptedException {

        int addedTime = 0;
        while (addedTime<maxTime) {

            if (breakCondition.get()) {
                break;
            }

            Thread.sleep(waitTime);
            addedTime+=waitTime;

        }
    }
}
