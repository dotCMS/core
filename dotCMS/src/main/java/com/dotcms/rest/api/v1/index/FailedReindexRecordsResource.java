package com.dotcms.rest.api.v1.index;

import com.dotcms.content.index.IndexDocumentConstraints;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.common.reindex.ReindexQueueFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Try;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.server.JSONP;

/**
 * Vendor-neutral listing of the reindex journal entries that exhausted their retries.
 *
 * <p>Replaces {@code GET /api/v1/esindex/failed}, which embeds each entry's full contentlet and
 * so grows with the size of the content that failed (#37269).</p>
 */
@Path("/v1/index/failed")
@SwaggerCompliant(value = "Search index management APIs", batch = 2)
@Tag(name = "Search Index", description = "Search index management and operations")
public class FailedReindexRecordsResource {

    /** Reports the same limits the mapping step enforces. */
    private static final IndexDocumentConstraints DOCUMENT_CONSTRAINTS =
            new IndexDocumentConstraints();

    /**
     * Lists the failed reindex records.
     *
     * @param request  the HTTP request
     * @param response the HTTP response
     * @return the failed journal entries, identified but without content values, with the
     *         document limits and retry policy they are measured against
     * @throws DotDataException if the journal cannot be read
     */
    @Operation(
            operationId = "listFailedReindexRecords",
            summary = "List failed reindex records",
            description = "Returns every reindex journal entry that exhausted its retries, with the"
                    + " index-document limits and retry policy they are measured against. Each"
                    + " record identifies the content (identifier, inode, title, content type"
                    + " variable, language), what the index still owes for it (reindex or delete),"
                    + " how many attempts failed, the last failure reason and, when the document"
                    + " exceeded a document limit, which limit, field, actual value and allowed"
                    + " value. Unknown values are explicit nulls. Content field values are never"
                    + " included, so the response size does not depend on the size of the failed"
                    + " content. Requires the CMS Administrator role and access to the Maintenance"
                    + " portlet. Replaces the deprecated GET /api/v1/esindex/failed."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Failed records listed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ResponseEntityFailedReindexRecordsView.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "403", description = "Forbidden - CMS Administrator role and Maintenance portlet access required",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @CloseDBIfOpened
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityFailedReindexRecordsView listFailedRecords(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response)
            throws DotDataException {
        new WebResource.InitBuilder(request, response)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requiredPortlet("maintenance")
                .init();
        final List<FailedReindexRecordView> records = APILocator.getReindexQueueAPI()
                .getFailedReindexRecords().stream()
                .map(row -> FailedReindexRecordView.from(row, findForDisplay(row.getIdentToIndex())))
                .collect(Collectors.toList());
        return new ResponseEntityFailedReindexRecordsView(FailedReindexRecordsView.of(records,
                DOCUMENT_CONSTRAINTS.maxStringLength(), DOCUMENT_CONSTRAINTS.maxNestingDepth(),
                ReindexQueueFactory.REINDEX_MAX_FAILURE_ATTEMPTS));
    }

    /**
     * Resolves a version of the contentlet to show its title, type and language.
     *
     * @param identifier the contentlet identifier
     * @return any language's version, or {@code null} when the content no longer exists
     */
    private static Contentlet findForDisplay(final String identifier) {
        return Try.of(() -> APILocator.getContentletAPI()
                        .findContentletByIdentifierAnyLanguage(identifier))
                .onFailure(e -> Logger.debug(FailedReindexRecordsResource.class,
                        "Unable to resolve contentlet " + identifier + ": " + e.getMessage()))
                .getOrNull();
    }
}
