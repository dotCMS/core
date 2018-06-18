package com.dotcms.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Counter Set is a thread-safe collection that encapsulates a set of objects with a count; if the appearance of an items is equal or greater than numberOfItems
 * the method getCommonItems will return the items that comply this criteria
 *
 *
 * @author jsanca
 */
public class CounterSet<T> implements Serializable {

    private final int numberOfItems;
    private final ConcurrentHashMap<T, Integer> counterMap =
            new ConcurrentHashMap<>();

    public CounterSet(final int numberOfItems) {
        this.numberOfItems = numberOfItems;
    }

    /**
     * Returns the number of items criteria for the common items.
     * @return int
     */
    public int getNumberOfItems() {
        return numberOfItems;
    }

    /**
     * Adds an element and returns the count for it.
     * @param item T
     * @return int
     */
    public synchronized int add (final T item) {

        int count = this.counterMap.getOrDefault(item, 0);

        count++;
        this.counterMap.put(item, count);

        return count;
    }

    /**
     * Get the common items that comply the numberOfItems criteria.
     * @return Collection
     */
    public Collection<T> getCommonItems () {

        return this.counterMap.entrySet().stream()
                .filter(entry -> entry.getValue() >= this.numberOfItems)
                .map(entry -> entry.getKey())
                .collect(CollectionsUtils.toImmutableList());
    }

    /***
     * Get all items
     * @return Collection
     */
    public Collection<T> getAllItems () {

        return this.counterMap.keySet();
    }

} // E:O:F:CounterSet.
