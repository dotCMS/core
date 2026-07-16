package com.dotcms.rest.api.v1.publishing;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.util.List;

/**
 * Represents an environment with its endpoints and their publishing statuses.
 * An environment is a group of endpoints that receive published bundles together.
 *
 * @since Jan 2026
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = EnvironmentDetailView.class)
@JsonDeserialize(as = EnvironmentDetailView.class)
@Schema(description = "Environment with its endpoints and their statuses")
public interface AbstractEnvironmentDetailView {

    /**
     * Unique environment identifier.
     *
     * @return Environment ID
     */
    @Schema(
            description = "Unique environment identifier",
            example = "env-staging-123",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String id();

    /**
     * Human-readable environment name.
     *
     * @return Environment name
     */
    @Schema(
            description = "Human-readable environment name",
            example = "Staging Environment",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String name();

    /**
     * List of endpoints within this environment with their individual status.
     *
     * @return List of endpoint details
     */
    @Schema(
            description = "Endpoints within this environment with their status",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<EndpointDetailView> endpoints();

}
