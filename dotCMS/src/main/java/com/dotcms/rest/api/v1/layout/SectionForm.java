package com.dotcms.rest.api.v1.layout;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;

/**
 * Body of {@code POST /v1/layouts} and {@code PUT /v1/layouts/{layoutId}}: the name and icon of
 * a navigation section. Trimming, length and uniqueness rules are applied by {@link LayoutHelper}.
 *
 * @author hassandotcms
 */
@JsonDeserialize(builder = SectionForm.Builder.class)
@Schema(description = "Name and icon of a navigation section")
public class SectionForm extends Validated {

    @NotNull
    @Schema(description = "Section name, unique among sections, at most 255 characters", example = "Marketing", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String name;

    @Schema(description = "Icon name, at most 255 characters; may be empty", example = "campaign")
    private final String icon;

    private SectionForm(final Builder builder) {
        super();
        this.name = builder.name;
        this.icon = builder.icon;
        checkValid();
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    /** Jackson builder. */
    public static final class Builder {

        @JsonProperty(required = true)
        private String name;

        @JsonProperty
        private String icon;

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder icon(final String icon) {
            this.icon = icon;
            return this;
        }

        public SectionForm build() {
            return new SectionForm(this);
        }
    }
}
