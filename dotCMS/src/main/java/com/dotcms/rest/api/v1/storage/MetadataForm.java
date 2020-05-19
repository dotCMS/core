package com.dotcms.rest.api.v1.storage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = MetadataForm.Builder.class)
public class MetadataForm {

    private final String identifier;
    private final String inode;
    private final String language;
    private final String field;
    private final boolean nocache;

    public MetadataForm(final Builder builder) {

        this.identifier = builder.identifier;
        this.inode      = builder.inode;
        this.language   = builder.language;
        this.field      = builder.field;
        this.nocache    = builder.nocache;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getInode() {
        return inode;
    }

    public String getLanguage() {
        return language;
    }

    public String getField() {
        return field;
    }

    public boolean isNocache() {
        return nocache;
    }

    @Override
    public String toString() {
        return "MetadataForm{" +
                "identifier='" + identifier + '\'' +
                ", inode='" + inode + '\'' +
                ", language='" + language + '\'' +
                '}';
    }

    public static final class Builder {

        @JsonProperty private String identifier;
        @JsonProperty private String inode;
        @JsonProperty private String language;
        @JsonProperty private String field;
        @JsonProperty private boolean nocache = false;

        public Builder nocache(final boolean nocache) {
            this.nocache = nocache;
            return this;
        }

        public Builder identifier(final String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder inode(final String inode) {
            this.inode = inode;
            return this;
        }

        public Builder language(final String language) {
            this.language = language;
            return this;
        }

        public Builder field(final String field) {
            this.field = field;
            return this;
        }

        public MetadataForm build() {
            return new MetadataForm(this);
        }
    }
}
