package com.dotcms.rest.api.v1.drive;

import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.drive.export.ContentDriveExportHelper;
import com.dotcms.rest.api.v1.drive.export.DriveExportRequestForm;
import com.dotcms.rest.api.v1.drive.export.ExportFile;
import com.dotcms.rest.api.v1.drive.export.ExportFileBuilder;
import com.dotcms.rest.api.v1.drive.export.ExportFileNames;
import com.dotcms.rest.exception.HttpStatusCodeException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.file.Files;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;

/**
 * Content Drive Resource
 * <p>This resource provides drive-like functionality for browsing and searching content assets.</p>
 * <p>Enables navigation through content structures with filtering and search capabilities.</p>
 */
@Path("/v1/drive")
public class ContentDriveResource {

    /** The number of data rows in an export file, over every CSV. */
    static final String EXPORT_CONTENTLETS_HEADER = "X-Export-Contentlets";
    /** The number of CSVs in an export file. */
    static final String EXPORT_FILES_HEADER = "X-Export-Files";
    private static final String EXPORT_FAILED_MESSAGE =
            "The export failed. Try again, and contact your administrator if it keeps failing.";

    private final ContentDriveHelper helper = ContentDriveHelper.newInstance();
    private final ContentDriveExportHelper exportHelper = new ContentDriveExportHelper();

    /**
     * Search content assets using drive functionality
     */
    @Operation(
        summary = "Search content assets with drive functionality (Internal API)",
        description = "⚠️ **INTERNAL API - NOT FOR EXTERNAL USE** ⚠️\n\n" +
                     "This endpoint is designed exclusively for dotCMS internal operations and UI components. " +
                     "It is not intended for external integrations or third-party applications.\n\n" +
                     "**Important Notice:**\n" +
                     "• This API may change without notice in future versions\n" +
                     "• No backward compatibility guarantees\n" +
                     "• dotCMS does not provide support tickets for this endpoint\n" +
                     "• Use at your own risk for custom implementations\n\n" +
                     "**Functionality:** Search and browse content assets using drive-like functionality " +
                     "with filtering, navigation, and content type filtering capabilities.\n\n" +
                     "**Status filter:** the optional `status` array narrows by content state — " +
                     "`ARCHIVED`, `UNPUBLISHED` or `LOCKED`. Entries combine with **OR**, so " +
                     "selecting more statuses returns more content and adding one never shrinks " +
                     "the result set. Archived content is excluded by default and only `ARCHIVED` " +
                     "admits it, so `[\"UNPUBLISHED\"]` means \"unpublished and not archived\". " +
                     "An empty or omitted array applies no status filtering at all; an " +
                     "unrecognized value is rejected with a 400. It has no side effects on other " +
                     "fields \u2014 `showFolders` is honoured as sent.",
        tags = {"Internal APIs"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "Drive search results retrieved successfully",
                    content = @Content(mediaType = "application/json",
                        schema = @Schema(type = "object",
                        description = "Drive search response containing filtered assets, folders, and navigation metadata with content type filtering. WYSIWYG/TextArea/Story Block field values on each listing row are a <=150-character extracted plain-text preview, not the full stored value (issue #37185).")
                    )
        ),
        @ApiResponse(responseCode = "401",
                    description = "Unauthorized access",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404",
                    description = "Not found. Site or folder not found at the specified path",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400",
                    description = "Invalid request parameters or malformed drive request",
                    content = @Content(mediaType = "application/json"))
    })
    @Hidden
    @Path("/search")
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response search(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            DriveRequestForm form
    ) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        final User user = initDataObject.getUser();
        Logger.debug(this,
                String.format("User [%s] is requesting content drive search for path [%s]",
                        user.getUserId(), form.assetPath()));
        return Response.ok(new ResponseEntityView<>(helper.driveSearch(form, user))).build();
    }

    /**
     * Exports what the Drive shows, or what is selected in it, as a CSV or a zip of CSVs. The
     * file is built completely before the first byte is sent, so a failure is an error status and
     * never a truncated download.
     *
     * @param request  the HTTP request
     * @param response the HTTP response
     * @param form     the export request
     * @return 200 with the file, 204 when nothing readable matched, or 400, 404 or 500 with the
     *         reason in {@code errors[0].message}
     */
    @Operation(
        operationId = "postDriveExport",
        summary = "Export Content Drive content to CSV",
        description = "Exports content from Content Drive in the CSV format the Content Search "
                + "export writes and the content import reads.\n\n"
                + "**Filter mode** (no `selection`): send `query`, the same body the Drive listing "
                + "sends to `/api/v1/drive/search`, filtered to exactly one content type. Every "
                + "contentlet the listing matches, across all pages, is returned as one CSV. "
                + "Paging, sorting, `showFolders`, `showLinks` and `live` are ignored.\n\n"
                + "**Selection mode** (`selection` with folders or contentlets, at most 60 in all, "
                + "plus `siteId`): the selected contentlets in their selected language and every "
                + "contentlet under the selected folders, in every language, as a zip with one CSV "
                + "per content type.\n\n"
                + "Only the working version of each contentlet is exported, and only what the user "
                + "can read. Forms and sites are never exported. More than "
                + "`CONTENT_DRIVE_EXPORT_MAX_ROWS` rows (default 100,000) is refused with a 400. "
                + "The response headers `X-Export-Contentlets` and `X-Export-Files` give the rows "
                + "and the CSVs written.",
        tags = {"Content Drive"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                description = "The export file: one CSV in filter mode, a zip of CSVs in selection mode",
                content = {
                    @Content(mediaType = "text/csv",
                            schema = @Schema(type = "string", format = "binary")),
                    @Content(mediaType = "application/zip",
                            schema = @Schema(type = "string", format = "binary"))
                }),
        @ApiResponse(responseCode = "204",
                description = "Nothing readable matched the request; no file"),
        @ApiResponse(responseCode = "400",
                description = "Invalid request: nothing to export, not exactly one readable content "
                        + "type, a Form or Host type, too many selected items, an invalid entry, "
                        + "or more rows than the limit",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(type = "object",
                                description = "Error body: errors[0].message holds the reason"))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404",
                description = "The site or a selected folder was not found",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(type = "object",
                                description = "Error body: errors[0].message holds the reason"))),
        @ApiResponse(responseCode = "500",
                description = "The export failed",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(type = "object",
                                description = "Error body: errors[0].message holds a generic message")))
    })
    @Path("/export")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({"text/csv", "application/zip", MediaType.APPLICATION_JSON})
    public Response export(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "What to export: a listing query (filter mode) or a "
                    + "selection of folders and contentlets",
                    required = true,
                    content = @Content(schema = @Schema(implementation = DriveExportRequestForm.class)))
            final DriveExportRequestForm form) {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();
        final User user = initDataObject.getUser();

        try {
            final DriveExportRequestForm body =
                    form == null ? DriveExportRequestForm.builder().build() : form;
            final ExportFile file = exportHelper.export(body, user, request);
            if (file.contentlets() == 0) {
                return Response.noContent().build();
            }
            return Response.ok(ExportFileBuilder.toStreamingOutput(file), file.mediaType())
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ExportFileNames.contentDisposition(file.fileName()))
                    .header(HttpHeaders.CONTENT_LENGTH, Files.size(file.path()))
                    .header(EXPORT_CONTENTLETS_HEADER, file.contentlets())
                    .header(EXPORT_FILES_HEADER, file.files())
                    .build();
        } catch (final HttpStatusCodeException e) {
            return errorResponse(e.getResponse().getStatus(),
                    e.getResponse().getHeaderString("error-message"));
        } catch (final Exception e) {
            Logger.error(this, String.format("Content Drive export failed for user '%s': %s",
                    user.getUserId(), e.getMessage()), e);
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    EXPORT_FAILED_MESSAGE);
        }
    }

    /**
     * Builds an export error response in the one shape the Drive reads:
     * {@code {"errors":[{"errorCode","message","fieldName":null}],"entity":null}}.
     *
     * @param status  the HTTP status
     * @param message the message for the user
     * @return the response
     */
    private static Response errorResponse(final int status, final String message) {
        final Response.Status known = Response.Status.fromStatusCode(status);
        final String errorCode = known == null ? String.valueOf(status) : known.name();
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ResponseEntityView<>(List.of(new ErrorEntity(errorCode, message, null))))
                .build();
    }
}
