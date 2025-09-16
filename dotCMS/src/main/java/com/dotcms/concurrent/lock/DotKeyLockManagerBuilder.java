package com.dotcms.concurrent.lock;

import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.util.Config;
import com.liferay.util.StringPool;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class DotKeyLockManagerBuilder {

    private static final String DOTCMS_CONCURRENT_LOCK_STRIPES = "dotcms.concurrent.locks.stripes";
    private static final String DOTCMS_CONCURRENT_LOCK_STRIPES_TIME = "dotcms.concurrent.locks.stripes.time";
    private static final String DOTCMS_CONCURRENT_LOCK_STRIPES_TIMEUNIT = "dotcms.concurrent.locks.stripes.timeunit";
    private static final String DOTCMS_CONCURRENT_LOCK_FACTORY_CLASS = "dotcms.concurrent.locks.lockFactoryClass";
    private static final String DEFAULT = "default";

    private static final DotKeyLockManagerFactory dotKeyLockManagerFactory = getDotKeyLockManagerFactory();

    private static DotKeyLockManagerFactory getDotKeyLockManagerFactory() {

        final String lockFactoryClassName = Config.getStringProperty( DOTCMS_CONCURRENT_LOCK_FACTORY_CLASS, DEFAULT);
        return (DEFAULT.equals(lockFactoryClassName) ? null : (DotKeyLockManagerFactory)ReflectionUtils.newInstance(lockFactoryClassName));
    }

    public static <R> DotKeyLockManager <R> newLockManager() {
        return newLockManager(StringPool.BLANK);
    }


    public static <R> DotKeyLockManager <R> newLockManager(final String lockManagerName) {

        if(null !=  dotKeyLockManagerFactory){
            return dotKeyLockManagerFactory.create(lockManagerName);
        }

        final int stripes = Config.getIntProperty(lockManagerName + DOTCMS_CONCURRENT_LOCK_STRIPES, 
                        Config.getIntProperty(DOTCMS_CONCURRENT_LOCK_STRIPES,StripedLockImpl.DEFAULT_STRIPES));
        final int time = Config.getIntProperty(lockManagerName + DOTCMS_CONCURRENT_LOCK_STRIPES_TIME, StripedLockImpl.DEFAULT_TIME);
        final TimeUnit timeUnit = TimeUnit.valueOf(Config.getStringProperty(lockManagerName + DOTCMS_CONCURRENT_LOCK_STRIPES_TIMEUNIT, StripedLockImpl.DEFAULT_TU.name()));
        return new StripedLockImpl<>(stripes, time, timeUnit);
    }

    public static <R> DotKeyLockManager <R> newLockManager(final String lockManagerName, final int stripes, final Duration timeout) {

        if(null !=  dotKeyLockManagerFactory){
            return dotKeyLockManagerFactory.create(lockManagerName);
        }

        final int time = (int) timeout.toMillis();
        final TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        return new StripedLockImpl<>(stripes, time, timeUnit);
    }



}
