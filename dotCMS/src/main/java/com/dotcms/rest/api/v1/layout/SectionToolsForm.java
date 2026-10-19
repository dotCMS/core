package com.dotcms.rest.api.v1.layout;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Body of {@code PUT /v1/layouts/{layoutId}/portlets}: the full ordered list of tool ids the
 * section must hold after the call.
 *
 * @author hassandotcms
 */
@JsonDeserialize(builder = SectionToolsForm.Builder.class)
@Schema(description = "Full ordered list of the tool (portlet) ids a section holds")
public class SectionToolsForm extends Validated {

    @NotNull
    @Schema(description = "Tool ids in the order they appear in the section; may be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    private final List<String> portletIds;

    private SectionToolsForm(final Builder builder) {
        super();
        this.portletIds = builder.portletIds;
        checkValid();
    }

    public List<String> getPortletIds() {
        return portletIds;
    }

    /** Jackson builder. */
    public static final class Builder {

        @JsonProperty(required = true)
        private List<String> portletIds;

        public Builder portletIds(final List<String> portletIds) {
            this.portletIds = portletIds;
            return this;
        }

        public SectionToolsForm build() {
            return new SectionToolsForm(this);
        }
    }
}
