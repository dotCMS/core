package com.dotcms.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

@JsonDeserialize(builder = DeleteBundlesByIdentifierForm.Builder.class)
public class DeleteBundlesByIdentifierForm {

    private final List<String> identifiers;

    private DeleteBundlesByIdentifierForm(final Builder builder) {

        this.identifiers = builder.identifiers;
    }

    public List<String> getIdentifiers() {

        return this.identifiers;
    }

    public static final class Builder {
        @JsonProperty(required = true) private List<String> identifiers;

        public DeleteBundlesByIdentifierForm.Builder identifiers(final List<String> identifiers) {
            this.identifiers = identifiers;
            return this;
        }

        public DeleteBundlesByIdentifierForm build() {
            return new DeleteBundlesByIdentifierForm(this);
        }
    }
}
