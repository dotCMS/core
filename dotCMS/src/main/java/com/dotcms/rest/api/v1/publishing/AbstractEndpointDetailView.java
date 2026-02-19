package com.dotcms.rest.api.v1.publishing;

import com.dotcms.annotations.Nullable;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * Represents a publishing endpoint with its status and error details.
 * Contains the endpoint configuration (server name, address, port, protocol)
 * and the result of the publishing operation (status, message, stack trace).
 *
 * @since Jan 2026
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = EndpointDetailView.class)
@JsonDeserialize(as = EndpointDetailView.class)
@Schema(description = "Publishing endpoint with status and error details")
public interface AbstractEndpointDetailView {

    /**
     * Unique endpoint identifier.
     *
     * @return Endpoint ID
     */
    @Schema(
            description = "Unique endpoint identifier",
            example = "endpoint-1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String id();

    /**
     * Human-readable server name.
     *
     * @return Server name
     */
    @Schema(
            description = "Human-readable server name",
            example = "Staging Server 1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String serverName();

    /**
     * Server address (hostname or IP).
     *
     * @return Server address
     */
    @Schema(
            description = "Server address (hostname or IP)",
            example = "staging1.example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String address();

    /**
     * Server port number.
     *
     * @return Port number
     */
    @Schema(
            description = "Server port number",
            example = "443",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String port();

    /**
     * Communication protocol (http or https).
     *
     * @return Protocol
     */
    @Schema(
            description = "Communication protocol",
            example = "https",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String protocol();

    /**
     * Publishing status for this endpoint.
     * Will be null if endpoint has not been processed yet.
     *
     * @return Status enum (e.g., SUCCESS, FAILED_TO_SENT) or null
     */
    @Schema(
            description = "Publishing status for this endpoint (e.g., SUCCESS, FAILED_TO_SENT)",
            example = "SUCCESS"
    )
    @Nullable
    PublishAuditStatus.Status status();

    /**
     * Status message with additional details.
     *
     * @return Status message or null
     */
    @Schema(
            description = "Status message with additional details",
            example = "Everything ok"
    )
    @Nullable
    String statusMessage();

    /**
     * Stack trace for failed endpoints.
     * Only included when the endpoint status indicates a failure.
     *
     * @return Stack trace or null
     */
    @Schema(
            description = "Stack trace for failed endpoints (only included for failures)"
    )
    @Nullable
    String stackTrace();

}
