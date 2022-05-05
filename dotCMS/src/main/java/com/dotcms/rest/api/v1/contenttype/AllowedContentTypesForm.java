package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Provides the list of Content Types -- in the form of Velocity Variable Names -- that can be referenced inside a Story
 * Block field. Any other Content Type that is NOT part if this list cannot be referenced t all.
 *
 * @author Jose Castro
 * @since Apr 27th, 2022
 */
@JsonDeserialize(builder = AllowedContentTypesForm.Builder.class)
public class AllowedContentTypesForm extends Validated {

    private final String types;

    /**
     * Internal class constructor meant to be used by the Builder class.
     *
     * @param builder The Builder object for the {@link AllowedContentTypesForm} class.
     */
    private AllowedContentTypesForm(final Builder builder) {
        this.types = builder.types;
    }

    /**
     * The comma-separated list of Velocity Variable Names for each Content Type that can be referenced in a Story Block
     * field.
     *
     * @return The comma-separated list of Velocity Variable Names.
     */
    public String getTypes() {
        return this.types;
    }

    /**
     * Builder class used to create an instance of the {@link AllowedContentTypesForm} class.
     */
    public static final class Builder {

        @JsonProperty(required = true)
        private String types;

        /**
         * The comma-separated list of Velocity Variable Names for each Content Type that can be referenced in a Story
         * Block field.
         *
         * @param types The comma-separated list of Velocity Variable Names.
         *
         * @return An instance of the {@link Builder} class.
         */
        public AllowedContentTypesForm.Builder types(final String types) {
            this.types = types;
            return this;
        }

        /**
         * Creates an instance of the {@link AllowedContentTypesForm} class.
         *
         * @return An instance of the {@link AllowedContentTypesForm} class.
         */
        public AllowedContentTypesForm build() {
            return new AllowedContentTypesForm(this);
        }

    }

}
