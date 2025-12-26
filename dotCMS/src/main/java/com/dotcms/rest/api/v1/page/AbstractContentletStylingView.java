package com.dotcms.rest.api.v1.page;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import org.immutables.value.Value;

@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = ContentletStylingView.class)
@JsonDeserialize(as = ContentletStylingView.class)
@Schema(description = "Contentlet with Styles info")
public interface AbstractContentletStylingView {

    @JsonProperty("containerId")
    @Schema(
            description = "Container identifier",
            example = "//demo.dotcms.com/application/containers/default/",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String containerId();

    @JsonProperty("uuid")
    @Schema(
            description = "Container unique identifier (UUID)",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String uuid();

    @JsonProperty("contentletId")
    @Schema(
            description = "Contentlet identifier",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String contentletId();

    @JsonProperty("styleProperties")
    @Schema(
            description = "Styles defined for the Contentlet",
            example = "{\"color\": \"#FF0000\", \"margin\": \"10px\"}",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    Map<String, Object> styleProperties();
}