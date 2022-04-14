package com.dotcms.concurrent;


import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class ConditionalSubmitterImpl implements ConditionalSubmitter {

    private final int slotsNumber;
    private final Semaphore semaphore;
    private final long timeout;
    private final TimeUnit timeUnit;

    ConditionalSubmitterImpl(final int slotsNumber) {
        this(slotsNumber, 1, TimeUnit.SECONDS);
    }

    ConditionalSubmitterImpl(final int slotsNumber, final long timeout, final TimeUnit timeUnit) {

        this.slotsNumber = slotsNumber;
        this.timeout     = timeout;
        this.timeUnit    = timeUnit;
        this.semaphore   =  new Semaphore(slotsNumber);
    }

    private boolean tryAcquire () {

        return Try.of(()->this.semaphore.tryAcquire(this.timeout, this.timeUnit)).getOrElse(false);
    }

    @Override
    public <T> T submit(final Supplier<T> onAvailableSupplier, final Supplier<T> onDefaultSupplier) {

        T result = null;
        final int availableSlots = this.semaphore.availablePermits();

        if (this.tryAcquire()) {

            try {

                Logger.debug(this, ()-> "Available slot taken, slot available: " + availableSlots);
                result = onAvailableSupplier.get();
            } finally {

                this.semaphore.release();
                Logger.debug(this, ()-> "Slot released");
            }
        } else {

            Logger.debug(this, ()-> "Not Available slots");
            result = onDefaultSupplier.get();
        }

        return result;
    }

    @Override
    public int slotsNumber() {
        return this.slotsNumber;
    }
}
