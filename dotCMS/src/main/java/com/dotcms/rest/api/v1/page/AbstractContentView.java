package com.dotcms.rest.api.v1.page;

import com.dotcms.annotations.Nullable;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.Map;
import org.immutables.value.Value;

@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = ContentView.class)
@JsonDeserialize(as = ContentView.class)
@Schema(description = "Content within a Page and Styles info")
public interface AbstractContentView {

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

    @JsonProperty(Contentlet.STYLE_PROPERTIES_KEY)
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(
            description = "Styles defined for the Contentlet",
            example = "{\"color\": \"#FF0000\", \"margin\": \"10px\"}",
            requiredMode = RequiredMode.NOT_REQUIRED
    )
    Map<String, Object> styleProperties();
}