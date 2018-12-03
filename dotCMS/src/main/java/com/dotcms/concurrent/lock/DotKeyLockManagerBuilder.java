package com.dotcms.concurrent.lock;

import com.dotmarketing.util.Config;
import java.util.concurrent.TimeUnit;

public class DotKeyLockManagerBuilder {

    private static final String DOTCMS_CONCURRENT_LOCK_STRIPS = "dotcms.concurrent.locks.strips";
    private static final String DOTCMS_CONCURRENT_LOCK_STRIPS_TIME = "dotcms.concurrent.locks.strips.time";
    private static final String DOTCMS_CONCURRENT_LOCK_STRIPS_TIMEUNIT = "dotcms.concurrent.locks.strips.timeunit";


     static <R,K> DotKeyLockManager <R,K> newDefaultStripedLockManager() {
        final int strips = Config.getIntProperty(DOTCMS_CONCURRENT_LOCK_STRIPS, StripedLockImpl.DEFAULT_STRIPES);
        return new StripedLockImpl<>(strips);
     }

    static <R,K> DotKeyLockManager <R,K> newStripedLockManager() {
        final int strips = Config.getIntProperty(DOTCMS_CONCURRENT_LOCK_STRIPS, StripedLockImpl.DEFAULT_STRIPES);
        final int time = Config.getIntProperty(DOTCMS_CONCURRENT_LOCK_STRIPS_TIME, StripedLockImpl.DEFAULT_TIME);
        final TimeUnit timeUnit = TimeUnit.valueOf(Config.getStringProperty(DOTCMS_CONCURRENT_LOCK_STRIPS_TIMEUNIT, StripedLockImpl.DEFAULT_TU.name()));
        return new StripedLockImpl<>(strips, time, timeUnit);
    }

}
