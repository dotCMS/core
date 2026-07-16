package com.dotcms.rest.api.v1.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;

import java.util.List;

/**
 * Encapsulates the DeleteForm to delete one or more notification
 * @author jsanca 
 */
@JsonDeserialize(builder = DeleteForm.Builder.class)
public class DeleteForm extends Validated {


    @NotNull
    private final List<String> items;

    private DeleteForm(DeleteForm.Builder builder) {

        this.items = builder.items;
        checkValid();
    }

    public List<String> getItems() {
        return items;
    }

    public static final class Builder {
        @JsonProperty(required = true)
        private List<String> items;

        public Builder items(final List<String> items) {
            this.items = items;
            return this;
        }

        public DeleteForm build() {
            return new DeleteForm(this);
        }
    }
}
