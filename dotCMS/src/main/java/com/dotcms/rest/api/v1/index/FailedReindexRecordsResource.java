package com.dotcms.rest.api.v1.index;

import com.dotcms.content.index.IndexDocumentConstraints;
import com.dotcms.content.index.IndexDocumentViolation;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.exception.ConflictException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.common.reindex.ParkedRecordsRetry;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.common.reindex.ReindexQueueFactory;
import com.dotmarketing.exception.DotDataException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.server.JSONP;

/**
 * Vendor-neutral listing of the reindex journal entries that failed at least once, parked or
 * still being retried (#37270).
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
     * @return the failed journal entries, parked or still being retried, identified but without
     *         content values, with the document limits and retry policy they are measured against
     * @throws DotDataException if the journal cannot be read
     */
    @Operation(
            operationId = "listFailedReindexRecords",
            summary = "List failed reindex records",
            description = "Returns every reindex journal entry that failed at least once, whether it"
                    + " gave up (parked) or will be tried again (retrying), with the"
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
            @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated, or the"
                    + " user lacks the CMS Administrator role or access to the Maintenance portlet",
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
        final List<ReindexEntry> rows = APILocator.getReindexQueueAPI().getFailedReindexRecords();
        // A document-limit failure names the version it withheld: show that exact version. Any
        // other failure only names the identifier: show a version of it. Column-only queries,
        // batched, instead of loading every failed contentlet (review of #37924).
        // Identity, not equals: ReindexEntry equality ignores the row id, and one identifier can
        // have several failed rows.
        final Map<ReindexEntry, String> withheldInodes = new IdentityHashMap<>();
        rows.forEach(row -> IndexDocumentViolation.violationOf(row.getLastResult())
                .ifPresent(withheld -> withheldInodes.put(row, withheld.inode())));
        final DisplayVersionFinder finder = new DisplayVersionFinder();
        final Map<String, DisplayVersion> byInode = finder.findByInodes(withheldInodes.values());
        final Map<String, DisplayVersion> byIdentifier = finder.find(rows.stream()
                .filter(row -> !withheldInodes.containsKey(row))
                .map(ReindexEntry::getIdentToIndex).collect(Collectors.toList()));
        final List<FailedReindexRecordView> records = rows.stream()
                .map(row -> FailedReindexRecordView.from(row, withheldInodes.containsKey(row)
                        ? byInode.get(withheldInodes.get(row))
                        : byIdentifier.get(row.getIdentToIndex())))
                .collect(Collectors.toList());
        return new ResponseEntityFailedReindexRecordsView(FailedReindexRecordsView.of(records,
                DOCUMENT_CONSTRAINTS.maxStringLength(), DOCUMENT_CONSTRAINTS.maxNestingDepth(),
                ReindexQueueFactory.REINDEX_MAX_FAILURE_ATTEMPTS));
    }

    /**
     * Sends the parked reindex records back to the queue, so content that gave up is indexed again
     * once the cause has been fixed (#37270). Refused while a full reindex runs: requeued reindex
     * records would land in the band the reindex waits on before switching over, and "Stop
     * Reindex" deletes that band. Every retry is refused then, not only the reindex ones, so the
     * rule is the same whatever was parked.
     *
     * @param request  the HTTP request
     * @param response the HTTP response
     * @return how many records were requeued, and how many of them as removals
     * @throws DotDataException if the journal cannot be updated
     */
    @Operation(
            operationId = "retryFailedReindexRecords",
            summary = "Retry parked reindex records",
            description = "Sends every parked reindex record (one whose retries ran out) back to the"
                    + " queue at the priority it was originally queued at, with a fresh retry"
                    + " budget. A record whose content no longer exists is requeued as a removal,"
                    + " so its stale documents leave the index. Records still being retried are"
                    + " not touched. Refused with 409 while a full reindex is running. Requires"
                    + " the CMS Administrator role and access to the Maintenance portlet."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Parked records requeued",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ResponseEntityRetriedFailedRecordsView.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - not authenticated, or the"
                    + " user lacks the CMS Administrator role or access to the Maintenance portlet",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "409", description = "A full reindex is running; nothing"
                    + " was requeued", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @CloseDBIfOpened
    @POST
    @Path("/retry")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityRetriedFailedRecordsView retryFailedRecords(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response)
            throws DotDataException {
        final InitDataObject init = new WebResource.InitBuilder(request, response)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requiredPortlet("maintenance")
                .init();
        if (APILocator.getContentletIndexAPI().isInFullReindex()) {
            throw new ConflictException(
                    "A full reindex is running; retry failed records after it finishes");
        }
        final ParkedRecordsRetry retried =
                APILocator.getReindexQueueAPI().retryParkedRecords(init.getUser());
        return new ResponseEntityRetriedFailedRecordsView(RetriedFailedRecordsView.of(retried));
    }
}
