package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.validation.constraints.NotNull;

/**
 * This form encapsulates the info for contentlet copy
 * @author jsanca
 */
@JsonDeserialize(builder = CopyContentTypeForm.Builder.class)
public class CopyContentTypeForm extends Validated {

    @NotNull
    private final String name;

    private final String variable;

    private final String folder;

    private final String host;

    private final String icon;

    private CopyContentTypeForm(final Builder builder) {

        this.name     = builder.name;
        this.variable = builder.variable;
        this.folder   = builder.folder;
        this.host     = builder.host;
        this.icon     = builder.icon;
        checkValid();
    }

    public String getName() {
        return name;
    }

    public String getVariable() {
        return variable;
    }

    public String getFolder() {
        return folder;
    }

    public String getHost() {
        return host;
    }

    public String getIcon() {
        return icon;
    }

    public static final class Builder {

        @JsonProperty(required = true)
        private String name;

        @JsonProperty()
        private String variable;

        @JsonProperty()
        private String folder;

        @JsonProperty()
        private String host;

        @JsonProperty()
        private String icon;


            public Builder name(String name) {

                this.name = name;
                return this;
            }

            public Builder variable(String variable) {

                this.variable = variable;
                return this;
            }

            public Builder folder(String folder) {

                this.folder = folder;
                return this;
            }

            public Builder host(String host) {

                this.host = host;
                return this;
            }

            public Builder icon(String icon) {

                this.icon = icon;
                return this;
            }

        public CopyContentTypeForm build() {

            return new CopyContentTypeForm(this);
        }

    }
}
