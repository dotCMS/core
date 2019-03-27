package com.dotcms.concurrent;

import com.dotcms.util.VoidDelegate;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates the delay and the payload
 * @author jsanca
 */
public class DelayedDelegate implements Delayed {

    private final long     delay;
    private final VoidDelegate delegate;

    public DelayedDelegate(final VoidDelegate delegate,
                           final long delay,
                           final TimeUnit timeUnit) {


        this.delay    = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(delay, timeUnit);
        this.delegate = delegate;
    }

    @Override
    public long getDelay(final TimeUnit unit) {
        final long diff = this.delay - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (this.delay < ((DelayedDelegate) o).delay) {
            return -1;
        }
        if (this.delay > ((DelayedDelegate) o).delay) {
            return 1;
        }
        return 0;
    }

    public void executeDelegate () throws DotSecurityException, DotDataException {

        this.delegate.execute();
    }
} // DelayedDelegate.
