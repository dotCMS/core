package com.dotcms.ema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Encapsulates a rewrite for the EMA
 * @author jsanca
 */
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
