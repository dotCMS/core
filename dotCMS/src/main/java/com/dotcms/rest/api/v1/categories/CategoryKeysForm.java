package com.dotcms.rest.api.v1.categories;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

/**
 * Category Input Form
 */
@JsonDeserialize(builder = CategoryKeysForm.Builder.class)
public class CategoryKeysForm extends Validated {

    private List<String> keys;

    private CategoryKeysForm(final Builder builder) {

        this.keys = builder.keys;
    }

    public List<String> getKeys() {
        return keys;
    }

    public static final class Builder {

        @JsonProperty
        private List<String> keys;

        public void setKeys(List<String> keys) {
            this.keys = keys;
        }

        public CategoryKeysForm build() {

            return new CategoryKeysForm(this);
        }
    }
}
