package com.dotcms.rest.api.v1.categories;

import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

/**
 * Category Input Form
 *
 * @author Hassan Mustafa Baig
 */
@JsonDeserialize(builder = CategoryInodesForm.Builder.class)
public class CategoryInodesForm extends Validated {

    private List<String> inodes;

    private CategoryInodesForm(final Builder builder) {

        this.inodes = builder.inodes;
    }

    public List<String> getInodes() {
        return inodes;
    }

    public static final class Builder {

        @JsonProperty
        private List<String> inodes;

        public void setInodes(List<String> inodes) {
            this.inodes = inodes;
        }

        public CategoryInodesForm build() {

            return new CategoryInodesForm(this);
        }
    }
}
