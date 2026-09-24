package com.dotcms.rest.api.v1.layout;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Body of {@code PUT /v1/layouts/_reorder}: every section id exactly once, in the new
 * navigation order.
 *
 * @author hassandotcms
 */
@JsonDeserialize(builder = SectionOrderForm.Builder.class)
@Schema(description = "Every navigation section id exactly once, in the new order")
public class SectionOrderForm extends Validated {

    @NotNull
    @Schema(description = "Section ids in the new navigation order; must contain every existing section exactly once", requiredMode = Schema.RequiredMode.REQUIRED)
    private final List<String> layoutIds;

    private SectionOrderForm(final Builder builder) {
        super();
        this.layoutIds = builder.layoutIds;
        checkValid();
    }

    public List<String> getLayoutIds() {
        return layoutIds;
    }

    /** Jackson builder. */
    public static final class Builder {

        @JsonProperty(required = true)
        private List<String> layoutIds;

        public Builder layoutIds(final List<String> layoutIds) {
            this.layoutIds = layoutIds;
            return this;
        }

        public SectionOrderForm build() {
            return new SectionOrderForm(this);
        }
    }
}
