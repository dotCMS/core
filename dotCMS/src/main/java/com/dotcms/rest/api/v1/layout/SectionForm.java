package com.dotcms.rest.api.v1.layout;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Body of {@code POST /v1/layouts} and {@code PUT /v1/layouts/{layoutId}}: the name and icon of
 * a navigation section. On create only, the position and the ordered tools may be sent too, so
 * the section is saved complete in one call. Trimming, length and uniqueness rules are applied
 * by {@link LayoutHelper}.
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

    @Schema(description = "Create only: position of the section in the navigation; defaults to after every existing section", example = "3")
    private final Integer tabOrder;

    @Schema(description = "Create only: tool (portlet) ids the section holds, in order; defaults to none")
    private final List<String> portletIds;

    private SectionForm(final Builder builder) {
        super();
        this.name = builder.name;
        this.icon = builder.icon;
        this.tabOrder = builder.tabOrder;
        this.portletIds = builder.portletIds;
        checkValid();
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    /** @return the requested position on create, or {@code null} for after every existing section */
    public Integer getTabOrder() {
        return tabOrder;
    }

    /** @return the ordered tool ids on create, or {@code null} for none */
    public List<String> getPortletIds() {
        return portletIds;
    }

    /** Jackson builder. */
    public static final class Builder {

        @JsonProperty(required = true)
        private String name;

        @JsonProperty
        private String icon;

        @JsonProperty
        private Integer tabOrder;

        @JsonProperty
        private List<String> portletIds;

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder icon(final String icon) {
            this.icon = icon;
            return this;
        }

        public Builder tabOrder(final Integer tabOrder) {
            this.tabOrder = tabOrder;
            return this;
        }

        public Builder portletIds(final List<String> portletIds) {
            this.portletIds = portletIds;
            return this;
        }

        public SectionForm build() {
            return new SectionForm(this);
        }
    }
}
