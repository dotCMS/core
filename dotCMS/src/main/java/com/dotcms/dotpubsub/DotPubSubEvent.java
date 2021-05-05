package com.dotcms.dotpubsub;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.Lazy;
import io.vavr.control.Try;



public final class DotPubSubEvent implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String ORIGIN = "o";
    private static final String MESSAGE = "m";
    private static final String TYPE = "t";
    private static final String TIMESTAMP = "ts";
    private static final String TOPIC = "top";
    private final Map<String, Serializable> payload;

    // cannot use DotObjectMapperProvider.getInstance().getDefaultObjectMapper() b/c it does not work in
    // unit tests
    private final static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Construct a DotPubSubEvent from a Json String
     * 
     * @param payloadJson
     */
    public DotPubSubEvent(String payloadJson) {
        this(Try.of(() -> objectMapper.readValue(payloadJson, Map.class)).getOrElseThrow(e -> {
            throw new DotRuntimeException(e);
        }));


    }

    /**
     * builder constructor
     * 
     * @param builder
     */
    private DotPubSubEvent(Builder builder) {
        this(builder.payload);
    }



    /**
     * Construct an DotPubSubEvent from a map
     * 
     * @param payloadJson
     */
    public DotPubSubEvent(Map<String, Serializable> map) {


        this.payload =Collections.unmodifiableMap(map);
    }


    /**
     * get whole payload as a map
     * 
     * @return
     */
    public Map<String, Serializable> getPayload() {
        return this.payload;
    }

    /**
     * Returns the type of DotPubSubEvent
     * 
     * @return
     */
    public String getType() {
        return payload != null ? (String) payload.get(TYPE) : null;
    }


    /**
     * Returns the sending server of DotPubSubEvent
     * 
     * @return
     */

    public String getOrigin() {
        return payload != null ? (String) payload.get(ORIGIN) : null;
    }

    /**
     * Returns the meat of the DotPubSubEvent
     */
    public String getMessage() {
        return payload != null ? (String) payload.get(MESSAGE) : null;
    }

    /**
     * Returns the meat of the DotPubSubEvent
     */
    public String getTopic() {
        return payload != null ? (String) payload.get(TOPIC) : null;
    }

    /**
     * converts an event to a String - because events are immutable once constructed, we should only do
     * this work once
     * 
     */
    private final Lazy<String> payloadAsString = Lazy.of(() -> {
        final Map<String, Serializable> payload = getPayload();
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            Logger.warn(this.getClass(), "unable to write payload as String:" + e.getMessage() + " " + payload);
            return null;
        }
    });



    @Override
    public String toString() {

        return payloadAsString.get();

    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((payload == null) ? 0 : payload.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DotPubSubEvent other = (DotPubSubEvent) obj;
        if (payload == null) {
            if (other.payload != null)
                return false;
        } else if (!payload.equals(other.payload)) {
            return false;
        }
        return true;
    }



    /**
     * Creates a builder to build {@link DotPubSubEvent} and initialize it with the given object.
     * 
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

        private Map<String, Serializable> payload = new HashMap<>();


        public Builder() {}

        public Builder(final DotPubSubEvent dotPubSubEvent) {

            this.payload.putAll(dotPubSubEvent.payload);

        }

        public Builder withPayload(String payloadJson) {
            HashMap<String, Serializable> map =
                            Try.of(() -> objectMapper.readValue(payloadJson, HashMap.class)).getOrElseThrow(e -> {
                                throw new DotRuntimeException(e);
                            });

            this.payload = map;


            return this;
        }

        public Builder withOrigin(final String origin) {
            return addPayload(DotPubSubEvent.ORIGIN, StringUtils.shortify(origin, 10));
        }


        public Builder withPayload(final Map<String, Serializable> payloadIncoming) {
            if (payloadIncoming != null) {
                payload.putAll(payloadIncoming);
            }
            return this;
        }

        public Builder addPayload(String key, Serializable value) {
            this.payload.put(key, value);

            return this;
        }

        public Builder withMessage(Serializable message) {

            return addPayload(MESSAGE, message);
        }

        public Builder withTopic(final DotPubSubTopic topic) {
            return withTopic(String.valueOf(topic.getKey()));
        }

        public Builder withTopic(final String topicKey) {
            final String topicStr = topicKey != null ? topicKey.toLowerCase() : null;
            payload.put(TOPIC, topicStr);
            return this;
        }

        public Builder withType(final String type) {

            return addPayload(DotPubSubEvent.TYPE, type);

        }

        public DotPubSubEvent build() {
            return new DotPubSubEvent(this);
        }
    }



}
