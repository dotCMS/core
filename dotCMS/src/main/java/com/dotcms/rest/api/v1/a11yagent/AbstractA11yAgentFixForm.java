package com.dotcms.rest.api.v1.a11yagent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Request body for {@code POST /api/v1/agents/a11y/fix} and {@code POST /api/v1/agents/a11y/fix/stream}.
 *
 * <p>The proxy resolves the identifier to a live URL, URI, and hostId before forwarding to the
 * agent service — the agent receives a fully-resolved payload (plan §8.2) and never performs
 * its own page resolution.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*",
        additionalJsonAnnotations = JsonIgnoreProperties.class)
@Value.Immutable
@JsonSerialize(as = A11yAgentFixForm.class)
@JsonDeserialize(as = A11yAgentFixForm.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request body for the a11y-fix agent proxy")
public interface AbstractA11yAgentFixForm {

    /**
     * Identifier of the page to fix. Declared nullable so a missing value surfaces as the
     * resource's {@code MISSING_IDENTIFIER} 400 rather than a deserialization failure.
     *
     * @return the dotCMS content identifier of the page
     */
    @Nullable
    @Schema(
            description = "dotCMS content identifier of the page to fix",
            example = "a9f30020-54ef-494e-92ed-645e757171c2",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String identifier();

    /**
     * Language the page is fixed in.
     *
     * @return the language id, defaults to 1
     */
    @Value.Default
    @Schema(
            description = "Language id of the page version to fix",
            example = "1",
            defaultValue = "1"
    )
    default int languageId() {
        return 1;
    }

    /**
     * When true the agent fixes only VTL and reports CSS contrast issues without changing
     * stylesheets (plan §3).
     *
     * @return true to skip CSS fixes, defaults to false
     */
    @Value.Default
    @Schema(
            description = "When true the agent fixes only VTL and reports CSS contrast issues "
                    + "instead of editing stylesheets",
            example = "false",
            defaultValue = "false"
    )
    default boolean skipCss() {
        return false;
    }
}
