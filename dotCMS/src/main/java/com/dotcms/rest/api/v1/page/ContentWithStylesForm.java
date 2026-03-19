package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.api.Validated;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Form for updating contentlet styles within a container on a page. Accepts JSON where contentlet
 * IDs are dynamic keys with their style properties as values.
 * <p>
 * Example JSON:
 * <pre>
 * {
 *   "containerId": "SYSTEM_CONTAINER",
 *   "uuid": "1",
 *   "contentlet-id-1": { "width": "100px", "color": "#FF0000" },
 *   "contentlet-id-2": { "margin": "10px" }
 * }
 * </pre>
 */
@Schema(description = "Container with contentlet style properties")
public class ContentWithStylesForm extends Validated {

    @JsonProperty("identifier")
    @Schema(
            description = "Container identifier",
            example = "//demo.dotcms.com/application/containers/default/",
            requiredMode = RequiredMode.REQUIRED
    )
    private final String containerId;

    @JsonProperty("uuid")
    @Schema(
            description = "Container unique identifier (UUID)",
            example = "1",
            requiredMode = RequiredMode.REQUIRED
    )
    private final String uuid;

    @JsonIgnore
    @Schema(hidden = true)
    private final Map<String, Map<String, Object>> contentletStyles;

    /**
     * Constructor for Jackson deserialization. Validation is performed in {@link #validate()}
     * which should be called after deserialization.
     */
    @JsonCreator
    public ContentWithStylesForm(
            @JsonProperty("identifier") final String containerId,
            @JsonProperty("uuid") final String uuid) {

        this.containerId = containerId;
        this.uuid = uuid;
        this.contentletStyles = new HashMap<>();
    }

    /**
     * Captures dynamic properties (contentlet IDs with their styles). Called by Jackson for any
     * JSON property not explicitly mapped.
     *
     * @param contentletId    The contentlet identifier (JSON key)
     * @param styleProperties The style properties for this contentlet (JSON value)
     */
    @JsonAnySetter
    public void addContentletStyle(final String contentletId, final Object styleProperties) {
        // Only process if it's not one of the known fields and is a Map
        if (!"containerId".equals(contentletId) && !"uuid".equals(contentletId)
                && styleProperties instanceof Map) {

            final Map<String, Object> styles = new HashMap<>((Map<String, Object>) styleProperties);
            this.contentletStyles.put(contentletId, styles);
        }
    }

    public String getContainerId() {
        return containerId;
    }

    public String getUuid() {
        return uuid;
    }

    /**
     * Returns a map of contentlet IDs to their style properties. Key: contentlet identifier Value:
     * Map of style property names to values
     *
     * @return Unmodifiable map of contentlet styles
     */
    @JsonIgnore
    public Map<String, Map<String, Object>> getContentletStyles() {
        return Collections.unmodifiableMap(contentletStyles);
    }

    /**
     * Validates the form and returns a list of validation errors. This allows collecting multiple
     * errors before throwing, providing better user experience.
     *
     * @return List of ContentletStylingErrorEntity objects, empty if validation passes
     */
    public List<ContentletStylingErrorEntity> validate() {
        final List<ContentletStylingErrorEntity> errors = new ArrayList<>();

        if (!UtilMethods.isSet(containerId)) {
            errors.add(new ContentletStylingErrorEntity(
                    "CONTAINER_REQUIRED",
                    "containerId cannot be empty",
                    "identifier",
                    null,
                    containerId,
                    uuid
            ));
        }
        if (!UtilMethods.isSet(uuid)) {
            errors.add(new ContentletStylingErrorEntity(
                    "CONTAINER_UUID_REQUIRED",
                    "uuid cannot be empty",
                    "uuid",
                    null,
                    containerId,
                    uuid
            ));
        }
        if (contentletStyles.isEmpty()) {
            errors.add(new ContentletStylingErrorEntity(
                    "CONTENTLET_STYLES_REQUIRED",
                    "styles must be set for at least one contentlet, it cannot be empty",
                    "\"contentlet-id\": { \"margin\": \"10px\" }"
            ));
        }

        return errors;
    }
}
