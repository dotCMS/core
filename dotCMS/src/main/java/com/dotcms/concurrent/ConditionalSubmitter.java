package com.dotcms.concurrent;


import java.util.function.Supplier;

/**
 * A conditional submitter allows the execution of a process if they are available slots, otherwise
 * may runs a default one.
 * @author jsanca
 */
public interface ConditionalSubmitter {

    /**
     * Submit the onAvailableSupplier if has available slot, otherwise will submit the onDefaultSupplier
     * @param onAvailableSupplier Supplier
     * @param onDefaultSupplier   Supplier
     * @param <T>
     * @return T
     */
    <T> T submit(final Supplier<T> onAvailableSupplier, final Supplier<T> onDefaultSupplier);

    /**
     * Returns the max number slots for this submitter
     * they are not the current available slot, are just the max number of them.
     * @return int
     */
    int slotsNumber();
}
