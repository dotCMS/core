package com.dotcms.ai.v2.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Migration Form for google docs
 * @author jsanca
 */
@JsonDeserialize(builder = MigrationRequestForm.Builder.class)
public class MigrationRequestForm {

    private final String googleDocUrl;
    private final String contentTypeVarname;
    private final String fieldVariableName;

    private MigrationRequestForm(final Builder builder) {

        this.googleDocUrl = builder.googleDocUrl;
        this.contentTypeVarname = builder.contentTypeVarname;
        this.fieldVariableName = builder.fieldVariableName;
    }

    public String getGoogleDocUrl() {
        return googleDocUrl;
    }

    public String getContentTypeVarname() {
        return contentTypeVarname;
    }

    public String getFieldVariableName() {
        return fieldVariableName;
    }

    public static final class Builder {
        @JsonProperty(required = true) private String googleDocUrl;
        @JsonProperty(required = true) private String contentTypeVarname;
        @JsonProperty() private String fieldVariableName;

        public Builder googleDocUrl(String googleDocUrl) {
            this.googleDocUrl = googleDocUrl;
            return this;
        }

        public Builder contentTypeVarname(String contentTypeVarname) {
            this.contentTypeVarname = contentTypeVarname;
            return this;
        }

        public Builder fieldVariableName(String fieldVariableName) {
            this.fieldVariableName = fieldVariableName;
            return this;
        }

        public MigrationRequestForm build() {
            return new MigrationRequestForm(this);
        }
    }
}
