package com.dotcms.rest.tag;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.rest.api.Validated;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Rest Tag representation
 */
@JsonDeserialize(builder = RestTag.Builder.class)
public final class RestTag extends Validated  {

    @JsonIgnore
    public final String key;
    public final String label;
    public final String siteId;
    public final String siteName;
    public final boolean persona;
    public final String id;

    /**
     * constructor
     * @param builder
     */
    private RestTag(Builder builder) {
        key = builder.key;
        label = builder.label;
        siteId = builder.siteId;
        siteName = builder.siteName;
        persona = builder.persona;
        id = builder.id;
        checkValid();
    }

    public String toString() {
        return ToStringBuilder.reflectionToString( this );
    }

    /**
     * json builder
     */
    public static final class Builder {
        @JsonProperty private String key;
        @JsonProperty private String label;
        @JsonProperty private String siteId;
        @JsonProperty private String siteName;
        @JsonProperty private boolean persona;
        @JsonProperty private String id;

        /**
         * default constructor
         */
        public Builder() {
        }

        /**
         * key setter
         * @param key
         * @return
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * key getter
         * @return
         */
        public String key(){
            return key;
        }

        public Builder from(RestTag copy) {
            key = copy.key;
            return this;
        }

        /**
         * builder method
         * @return
         */
        public RestTag build() {
            return new RestTag(this);
        }

        /**
         * label setter
         * @param label
         * @return
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * site setter
         * @param siteId
         * @return
         */
        public Builder siteId(final String siteId) {
            this.siteId = siteId;
            return this;
        }

        /**
         * site name setter
         * @param siteName
         * @return
         */
        public Builder siteName(String siteName) {
            this.siteName = siteName;
            return this;
        }

        /**
         * Persona setter
         * @param persona
         * @return
         */
        public Builder persona(boolean persona) {
            this.persona = persona;
            return this;
        }

        /**
         * id setter
         * @param id
         * @return
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

    }
}

