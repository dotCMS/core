package com.dotcms.ai.v2.rest;

import com.dotcms.ai.v2.api.CompletionRequest;
import com.dotcms.ai.v2.api.CompletionResponse;
import com.dotcms.ai.v2.api.CompletionSpec;
import com.dotcms.ai.v2.api.CompletionsAPI;
import com.dotcms.ai.v2.api.SummarizeRequest;
import com.dotcms.rest.WebResource;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Jersey resource exposing the Completions endpoints.
 *
 * Paths are versioned and separate from conversation endpoints.
 * @author jsanca
 */
@Path("/v2/ai/completions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "AI", description = "AI-powered completions")
public class CompletionsResource {

    private final CompletionsAPI completions;

    @Inject
    public CompletionsResource(final CompletionsAPI completions) {
        this.completions = completions;
    }

    /**
     * Synchronous one-shot completion.
     */
    @POST
    @Path("/complete")
    @Operation(
            operationId = "complete",
            summary = "The user does a prompt and gets an answer one-shot",
            description = "Creates AI-powered content complete.",
            tags = {"AI"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Completion generated successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Object.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Missing or invalid prompt"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public CompletionResponse complete(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final CompletionRequest completionRequest) {

        // get user if we have one (this is allow anon)
        final User user = new WebResource
                .InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init()
                .getUser();

        validate(completionRequest);

        final long startTime = System.currentTimeMillis();

        final CompletionResponse completionResponse = completions.complete(completionRequest);
        final Map<String, Object> meta = Objects.nonNull(completionResponse.meta)?
                completionResponse.meta:new HashMap<>();
        meta.put("totalTime", System.currentTimeMillis() - startTime + "ms");

        return completionResponse;
    }

    /**
     * Synchronous summarization shortcut.
     */
    @POST
    @Path("/summarize")
    @Operation(
            operationId = "summarize",
            summary = "The user does a prompt and gets an answer one-shot",
            description = "Creates AI-powered content complete.",
            tags = {"AI"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Completion generated successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Object.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Missing or invalid prompt"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public CompletionResponse summarize(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final SummarizeRequest completionRequest) {

        validate(completionRequest);
        return completions.summarize(completionRequest);
    }

    /**
     * Streaming completion (chunked).
     * Note: If you prefer SSE, switch to text/event-stream and emit data: lines.
     */
    @POST
    @Path("/stream")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(
            operationId = "stream",
            summary = "The user does a prompt and gets an answer one-shot",
            description = "Creates AI-powered content complete.",
            tags = {"AI"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Completion generated successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Object.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Missing or invalid prompt"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response stream(@Context final HttpServletRequest request,
                           @Context final HttpServletResponse response,
                           final CompletionRequest completionRequest) {

        validate(completionRequest);
        return Response.ok((StreamingOutput) (OutputStream output) -> {
            completions.completeStream(completionRequest, output);
            output.flush();
        }).build();
    }

    /** Minimal validation to prevent empty prompts. Enhance as needed. */
    private static void validate(final CompletionSpec req) {

        if (req == null || req.getPrompt() == null || req.getPrompt().trim().isEmpty()) {

            throw new BadRequestException("Prompt is required");
        }
    }

    // Small functional interface for Java 11 compatibility javax.ws.rs.
    private interface StreamingOutput extends javax.ws.rs.core.StreamingOutput {}
}
