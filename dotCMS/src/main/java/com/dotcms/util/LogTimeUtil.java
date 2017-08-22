package com.dotcms.util;

import com.dotcms.repackage.org.apache.commons.lang.time.StopWatch;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;

import java.util.function.Supplier;

/**
 * This util logs the total time for a calling delegate method.
 * @author jsanca
 */
public class LogTimeUtil {

    public static final LogTimeUtil INSTANCE = new LogTimeUtil();

    protected LogTimeUtil () {}

    /**
     * Call's the delegate and logs the "message" plus "duration {0} seconds
     *
     * @param delegate ReturnableDelegate delegate to call
     * @param messageSupplier String message to print if the log is able
     * @param <T>
     * @return
     */
    public <T> T logTime (final ReturnableDelegate<T> delegate, final Supplier<String> messageSupplier) throws Throwable {

        T methodReturn = null;

        if (Logger.isDebugEnabled(this.getClass())) {
            final StopWatch stopWatch = new StopWatch();

            stopWatch.start();

            methodReturn = delegate.execute();

            stopWatch.stop();

            Logger.debug(this, messageSupplier.get() +
                    ", duration:" +
                    DateUtil.millisToSeconds(stopWatch.getTime()) + " seconds");
        } else {

            methodReturn = delegate.execute();
        }

        return methodReturn;
    } // logTime.

} // E:O:F:LogTimeUtil.
