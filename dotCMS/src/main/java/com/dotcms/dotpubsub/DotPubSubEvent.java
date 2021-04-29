package com.dotcms.dotpubsub;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.vavr.Lazy;
import io.vavr.control.Try;



public final class DotPubSubEvent implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String ORIGIN = "o";
    private static final String MESSAGE = "m";
    private static final String TYPE = "t";
    private static final String TIMESTAMP = "ts";
    private final Map<String, Serializable> payload;
    private final static ObjectMapper objectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
    
    /**
     * Construct a DotPubSubEvent from a Json String
     * 
     * @param payloadJson
     */
    public DotPubSubEvent(String payloadJson) {
        this(Try.of(() -> objectMapper.readValue(payloadJson, Map.class))
                        .onFailure(e->Logger.warnAndDebug(DotPubSubEvent.class,e.getMessage(),e))
                        .getOrElse(new HashMap<>()));


    }

    /**
     * Construct an DotPubSubEvent from a map
     * 
     * @param payloadJson
     */
    public DotPubSubEvent(Map<String, Serializable> map) {

        ImmutableMap.Builder<String, Serializable> builder = ImmutableMap.<String, Serializable>builder().putAll(map);
        /*
        if(!map.containsKey(TIMESTAMP)) {
            builder.put(TIMESTAMP,System.currentTimeMillis());
        }
        */
        this.payload= builder.build();
    }


    /**
     * get whole payload as a map
     * 
     * @return
     */
    public Map<String, Serializable> getPayload() {
        return ImmutableMap.copyOf(payload);
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
     * converts an event to a String - because events are immutable once constructed, we should only do
     * this work once
     * 
     */
    private final Lazy<String> payloadAsString = Lazy.of(() -> {
        final Map<String, Serializable> payload = getPayload();
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            Logger.warn(this.getClass(), "unable to write payload as String:" + e.getMessage() + " " + payload );
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


    private DotPubSubEvent(Builder builder) {
        this(builder.payload);


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
            HashMap<String, Serializable> map = Try.of(() -> DotObjectMapperProvider.getInstance()
                            .getDefaultObjectMapper().readValue(payloadJson, HashMap.class)).getOrElse(new HashMap<>());

            this.payload = map;


            return this;
        }

        public Builder withOrigin(final String origin) {
            return addPayload(DotPubSubEvent.ORIGIN, APILocator.getShortyAPI().shortify(origin));
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


        public Builder withType(final String type) {

            return addPayload(DotPubSubEvent.TYPE, type);

        }

        public DotPubSubEvent build() {
            return new DotPubSubEvent(this);
        }
    }



}
