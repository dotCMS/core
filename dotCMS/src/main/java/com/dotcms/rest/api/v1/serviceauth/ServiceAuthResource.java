package com.dotcms.rest.api.v1.serviceauth;

import com.dotcms.auth.providers.jwt.services.serviceauth.ServiceJwtService;
import com.dotcms.auth.providers.jwt.services.serviceauth.ServiceTokenClaims;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityBooleanView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;

/**
 * REST API for service-to-service authentication.
 *
 * <p>This endpoint allows external microservices to validate JWTs issued by dotCMS
 * and check the status of the service authentication system.</p>
 *
 * <h2>Usage by External Services:</h2>
 * <pre>
 * # Validate a token received from dotCMS
 * curl -X POST https://dotcms.example.com/api/v1/service-auth/validate \
 *   -H "Content-Type: application/json" \
 *   -d '{"token": "eyJhbGc...", "expectedAudience": "wa11y-checker"}'
 * </pre>
 *
 * @author dotCMS
 */
@Path("/v1/service-auth")
@Tag(name = "Service Authentication",
        description = "Endpoints for service-to-service JWT authentication")
public class ServiceAuthResource implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ServiceJwtService jwtService;
    private final WebResource webResource;

    public ServiceAuthResource() {
        this(ServiceJwtService.getInstance(), new WebResource());
    }

    @VisibleForTesting
    protected ServiceAuthResource(final ServiceJwtService jwtService,
                                  final WebResource webResource) {
        this.jwtService = jwtService;
        this.webResource = webResource;
    }

    /**
     * Validate a service-to-service JWT token.
     *
     * <p>External services can call this endpoint to verify that a JWT token
     * was issued by this dotCMS instance and is still valid.</p>
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param form Token validation request
     * @return Validation result with claims if valid
     */
    @POST
    @Path("/validate")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "validateServiceToken",
            summary = "Validate a service JWT token",
            description = "Validates a JWT token issued for service-to-service authentication. " +
                    "Returns the token claims if valid, or an error if invalid/expired. " +
                    "External microservices should call this endpoint to verify tokens " +
                    "received from dotCMS before processing requests."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token validation result",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ResponseEntityServiceTokenValidationView.class),
                            examples = @ExampleObject(value = "{\n" +
                                    "  \"entity\": {\n" +
                                    "    \"valid\": true,\n" +
                                    "    \"claims\": {\n" +
                                    "      \"tokenId\": \"550e8400-e29b-41d4-a716-446655440000\",\n" +
                                    "      \"serviceId\": \"wa11y-checker\",\n" +
                                    "      \"issuer\": \"dotcms-cluster-1\",\n" +
                                    "      \"audience\": \"wa11y-checker\",\n" +
                                    "      \"sourceCluster\": \"dotcms-cluster-1\",\n" +
                                    "      \"issuedAt\": \"2024-01-15T10:30:00Z\",\n" +
                                    "      \"expiresAt\": \"2024-01-15T10:35:00Z\"\n" +
                                    "    }\n" +
                                    "  },\n" +
                                    "  \"errors\": [],\n" +
                                    "  \"messages\": []\n" +
                                    "}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - missing token or malformed request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token validation failed - expired, invalid signature, or wrong audience",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service authentication is not enabled",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            )
    })
    public Response validateToken(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(
                    description = "Token validation request",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ServiceTokenValidationForm.class))
            )
            final ServiceTokenValidationForm form) {

        // Check if service auth is enabled
        if (!jwtService.isEnabled()) {
            Logger.warn(this, "Service auth validation attempted but SERVICE_AUTH_ENABLED=false");
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ServiceTokenValidationResult(false, null,
                            "Service authentication is not enabled"))
                    .build();
        }

        // Validate input
        if (form == null || !UtilMethods.isSet(form.getToken())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ServiceTokenValidationResult(false, null,
                            "Token is required"))
                    .build();
        }

        try {
            final ServiceTokenClaims claims = jwtService.validateToken(
                    form.getToken(),
                    form.getExpectedAudience()
            );

            SecurityLogger.logInfo(this.getClass(),
                    String.format("Service token validated: serviceId=%s, audience=%s, sourceIP=%s",
                            claims.serviceId(),
                            claims.audience(),
                            request.getRemoteAddr()));

            return Response.ok(
                    new ResponseEntityServiceTokenValidationView(
                            new ServiceTokenValidationResult(true, claims, null)
                    )
            ).build();

        } catch (DotSecurityException e) {
            SecurityLogger.logInfo(this.getClass(),
                    String.format("Service token validation failed: %s, sourceIP=%s",
                            e.getMessage(), request.getRemoteAddr()));

            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ServiceTokenValidationResult(false, null, e.getMessage()))
                    .build();
        } catch (Exception e) {
            Logger.error(this, "Unexpected error validating service token", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ServiceTokenValidationResult(false, null,
                            "Internal error during token validation"))
                    .build();
        }
    }

    /**
     * Check if service authentication is enabled and healthy.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @return Status of service authentication
     */
    @GET
    @Path("/status")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getServiceAuthStatus",
            summary = "Check service authentication status",
            description = "Returns whether service-to-service authentication is enabled " +
                    "and the system is ready to issue/validate tokens."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service authentication status",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ResponseEntityServiceAuthStatusView.class),
                            examples = @ExampleObject(value = "{\n" +
                                    "  \"entity\": {\n" +
                                    "    \"enabled\": true,\n" +
                                    "    \"ready\": true,\n" +
                                    "    \"message\": \"Service authentication is enabled and ready\"\n" +
                                    "  },\n" +
                                    "  \"errors\": [],\n" +
                                    "  \"messages\": []\n" +
                                    "}")
                    )
            )
    })
    public Response getStatus(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {

        final boolean enabled = jwtService.isEnabled();
        final String message = enabled
                ? "Service authentication is enabled and ready"
                : "Service authentication is disabled. Set SERVICE_AUTH_ENABLED=true to enable.";

        return Response.ok(
                new ResponseEntityServiceAuthStatusView(
                        new ServiceAuthStatus(enabled, enabled, message)
                )
        ).build();
    }

    /**
     * Health check endpoint for Kubernetes probes.
     * Returns 200 if service auth is enabled and working, 503 if disabled.
     */
    @GET
    @Path("/health")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "serviceAuthHealthCheck",
            summary = "Health check for service authentication",
            description = "Simple health check endpoint for Kubernetes liveness/readiness probes. " +
                    "Returns 200 if enabled, 503 if disabled."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service authentication is healthy",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ResponseEntityBooleanView.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service authentication is not available",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            )
    })
    public Response healthCheck(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {

        if (jwtService.isEnabled()) {
            return Response.ok(new ResponseEntityBooleanView(true)).build();
        }

        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(new ResponseEntityBooleanView(false))
                .build();
    }
}
