package com.dotcms.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

@JsonDeserialize(builder = DeletePPQueueElementsByIdentifierForm.Builder.class)
public class DeletePPQueueElementsByIdentifierForm {

    private final List<String> identifiers;

    private DeletePPQueueElementsByIdentifierForm(final Builder builder) {

        this.identifiers = builder.identifiers;
    }

    public List<String> getIdentifiers() {

        return this.identifiers;
    }

    public static final class Builder {
        @JsonProperty(required = true) private List<String> identifiers;

        public DeletePPQueueElementsByIdentifierForm.Builder identifiers(final List<String> identifiers) {
            this.identifiers = identifiers;
            return this;
        }

        public DeletePPQueueElementsByIdentifierForm build() {
            return new DeletePPQueueElementsByIdentifierForm(this);
        }
    }
}
