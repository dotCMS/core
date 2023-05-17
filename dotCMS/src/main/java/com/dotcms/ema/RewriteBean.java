package com.dotcms.ema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Encapsulates a rewrite for the EMA
 * @author jsanca
 *
 * @deprecated The ability to map incoming URLs with specific EMA Proxy URLs has been implemented via the new JSON
 * configuration format. This class is not necessary anymore and should be deleted in the future.
 */
@Deprecated()
public class RewriteBean {
    private final String source;
    private final String destination;

    @JsonCreator
    public RewriteBean(@JsonProperty("source") final String source,
                       @JsonProperty("destination") final String destination) {
        this.source = source;
        this.destination = destination;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }
}
