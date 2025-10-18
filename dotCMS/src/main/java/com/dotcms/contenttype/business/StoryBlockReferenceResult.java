package com.dotcms.contenttype.business;

import com.dotcms.cost.RequestCost;
import com.dotcms.cost.RequestPrices.Price;

/**
 * Holds the result of updating any Contentlet references in a Story Block field so that it can be handled correctly in
 * upper layers of the system.
 *
 * @author Jonathan Sanchez
 * @since Oct 19th, 2022
 */
public class StoryBlockReferenceResult {

    private final boolean refreshed;
    private final Object value;

    /**
     * Creates an instance of this class.
     *
     * @param refreshed The final status of the update process. If the Contentlet was refreshed, this must be set to
     *                  {@code true}
     * @param value     The Contentlet object with the updated properties. If it DID NOT have to be updated, then the
     *                  original Contentlet must be set.
     */
    @RequestCost(Price.CONTENT_GET_RELATED)
    public StoryBlockReferenceResult(final boolean refreshed, final Object value) {
        this.refreshed = refreshed;
        this.value = value;
    }

    /**
     * Returns the final status of the update process for a given referenced Contentlet.
     *
     * @return If the referenced Contentlet was refreshed, returns {@code true}.
     */
    public boolean isRefreshed() {
        return refreshed;
    }

    /**
     * Returns the Contentlet object with the updated properties.
     *
     * @return The updated Contentlet. If it DID NOT have to be updated, then the original Contentlet is returned.
     */
    public Object getValue() {
        return value;
    }

}
