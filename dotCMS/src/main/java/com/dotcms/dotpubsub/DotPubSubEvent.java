package com.dotcms.dotpubsub;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DotPubSubEvent.Builder.class)
public final class DotPubSubEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    final String origin;

    final Map<String, Object> payload;

    final String topic;

    
    private DotPubSubEvent(Builder builder) {
        this.origin = builder.origin;
        this.payload = builder.payload;
        this.topic = builder.topic;
    }

    /**
     * Creates a builder to build {@link DotPubSubEvent} and initialize it with the given object.
     * @param dotPubSubEvent to initialize the builder with
     * @return created builder
     */
    
    public static Builder from(DotPubSubEvent dotPubSubEvent) {
        return new Builder(dotPubSubEvent);
    }

    /**
     * Builder to build {@link DotPubSubEvent}.
     */
    
    public static final class Builder {
        private String origin;
        private Map<String, Object> payload = Collections.emptyMap();
        private String topic;

        public Builder() {}

        private Builder(DotPubSubEvent dotPubSubEvent) {
            this.origin = dotPubSubEvent.origin;
            this.payload = dotPubSubEvent.payload;
            this.topic = dotPubSubEvent.topic;
        }

        public Builder withOrigin(String origin) {
            this.origin = origin;
            return this;
        }

        public Builder withPayload(Map<String, Object> payload) {
            this.payload = payload;
            return this;
        }

        public Builder withTopic(String topic) {
            this.topic = topic;
            return this;
        }

        public DotPubSubEvent build() {
            return new DotPubSubEvent(this);
        }
    }




}
