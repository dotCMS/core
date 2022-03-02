package com.dotcms.concurrent;


import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

class ConditionalSubmitterImpl implements ConditionalSubmitter {

    private final Semaphore semaphore;

    ConditionalSubmitterImpl(final int slotsNumber) {

        this.semaphore =  new Semaphore(slotsNumber);
    }

    @Override
    public <T> T submit(final Supplier<T> onAvailableSupplier, final Supplier<T> onDefaultSupplier) {

        T result = null;
        if (this.semaphore.availablePermits() > 0 && this.semaphore.tryAcquire()) {

            try {
                result = onAvailableSupplier.get();
            } finally {

                this.semaphore.release();
            }
        } else {

            result = onDefaultSupplier.get();
        }

        return result;
    }
}
