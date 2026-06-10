package com.dotcms.rest.api.v2.asset;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rest.InitDataObject;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.api.v1.asset.AssetsRequestForm;
import com.dotcms.rest.api.v1.asset.FileUploadData;
import com.dotcms.rest.api.v1.asset.FileUploadDetail;
import com.dotcms.rest.api.v1.asset.WebAssetHelper;
import com.dotcms.rest.api.v1.asset.view.AssetView;
import com.dotcms.rest.api.v1.asset.view.WebAssetView;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Try;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import com.dotcms.rest.exception.NotFoundException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URLConnection;
import java.util.Map;
import java.util.Optional;

/**
 * v2 REST endpoint for reading and writing dotCMS file assets in place.
 *
 * <p>Key design decisions vs v1:
 * <ul>
 *   <li>GET returns raw file bytes (streaming) instead of metadata JSON.</li>
 *   <li>Write intent (working vs published) is expressed via separate sub-paths
 *       ({@code /save} and {@code /publish}) rather than a flag in a JSON detail blob.</li>
 *   <li>Multipart form fields are flat ({@code file}, {@code path}, {@code language}) — no
 *       nested JSON {@code detail} blob.</li>
 *   <li>Identifier-addressed reads enforce READ permission (403 on violation).</li>
 * </ul>
 */
@SwaggerCompliant(value = "File Assets v2", batch = 3)
@Path("/v2/assets")
@Tag(name = "File Assets", description = "Read and write dotCMS file assets by path or identifier")
public class WebAssetResourceV2 {

    private final WebResource webResource;
    private final WebAssetHelper helper;
    private final ContentletAPI contentletAPI;
    private final FileAssetAPI fileAssetAPI;
    private final LanguageAPI languageAPI;
    private final PermissionAPI permissionAPI;

    /** Production constructor — all dependencies resolved from APILocator. */
    @SuppressWarnings("unused")
    public WebAssetResourceV2() {
        this(
                new WebResource(),
                WebAssetHelper.newInstance(),
                APILocator.getContentletAPI(),
                APILocator.getFileAssetAPI(),
                APILocator.getLanguageAPI(),
                APILocator.getPermissionAPI()
        );
    }

    @VisibleForTesting
    WebAssetResourceV2(
            final WebResource webResource,
            final WebAssetHelper helper,
            final ContentletAPI contentletAPI,
            final FileAssetAPI fileAssetAPI,
            final LanguageAPI languageAPI,
            final PermissionAPI permissionAPI) {
        this.webResource    = webResource;
        this.helper         = helper;
        this.contentletAPI  = contentletAPI;
        this.fileAssetAPI   = fileAssetAPI;
        this.languageAPI    = languageAPI;
        this.permissionAPI  = permissionAPI;
    }

    // -----------------------------------------------------------------------
    // GET  /api/v2/assets?path=//host/...
    //      Returns raw file bytes for the working version (default) or live
    //      version when version=live is specified.
    // -----------------------------------------------------------------------

    /**
     * Read raw file-asset bytes by host-qualified path.
     *
     * <p>The {@code path} parameter must be a host-qualified path of the form
     * {@code //hostname/folder/file.ext}.  The {@code language} parameter is
     * optional; when omitted the site default language is used.  The
     * {@code version} parameter is optional; accepted values are
     * {@code working} (default) and {@code live}.
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     * @param path     host-qualified asset path, e.g. {@code //demo.dotcms.com/application/foo.vtl}
     * @param language optional language tag (e.g. {@code en-US}); defaults to site default
     * @param version  {@code working} (default) or {@code live}
     * @return raw bytes of the file with the resolved MIME type as content type
     */
    @Operation(
            operationId = "getFileAssetByPath",
            summary     = "Get raw file-asset content by path",
            description = "Streams the raw bytes of a file asset addressed by a host-qualified path " +
                          "(//hostname/path/file.ext).  Returns the working version by default; " +
                          "use version=live for the published version.  " +
                          "Responds with 404 when the asset or language version does not exist, " +
                          "400 when the path points at a folder."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                     description = "Raw file bytes streamed with resolved MIME type and Content-Disposition header",
                     content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)),
        @ApiResponse(responseCode = "400",
                     description = "Bad Request — path is missing, ambiguous, or points at a folder",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON)),
        @ApiResponse(responseCode = "401",
                     description = "Unauthorized — authentication required",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON)),
        @ApiResponse(responseCode = "403",
                     description = "Forbidden — insufficient read permissions",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON)),
        @ApiResponse(responseCode = "404",
                     description = "Not Found — host, path, or language version does not exist",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @GET
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getByPath(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Host-qualified asset path, e.g. //demo.dotcms.com/application/foo.vtl",
                       required = true, example = "//demo.dotcms.com/application/containers/default/banner.vtl")
            @QueryParam("path") final String path,
            @Parameter(description = "Language tag (e.g. en-US). Defaults to site default language.",
                       example = "en-US")
            @QueryParam("language") final String language,
            @Parameter(description = "Asset version to retrieve. Accepted values: working (default), live.",
                       example = "working",
                       schema = @Schema(allowableValues = {"working", "live"}))
            @QueryParam("version") @DefaultValue("working") final String version
    ) throws DotDataException, DotSecurityException {

        if (!UtilMethods.isSet(path)) {
            throw new BadRequestException("Query parameter 'path' is required.");
        }

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final User user = initData.getUser();
        Logger.debug(this, () -> String.format(
                "User [%s] reading asset by path [%s] lang=[%s] version=[%s]",
                user.getUserId(), path, language, version));

        final boolean live = "live".equalsIgnoreCase(version);
        final String resolvedLang = resolveLanguageParam(language);
        final FileAsset fileAsset;
        try {
            fileAsset = helper.getAsset(buildAssetsRequestForm(path, resolvedLang, live), user);
        } catch (NotFoundInDbException e) {
            throw new NotFoundException(
                    String.format("Asset not found for path [%s] lang=[%s] version=[%s].",
                            path, resolvedLang, version));
        }

        return buildFileResponse(fileAsset);
    }

    // -----------------------------------------------------------------------
    // GET  /api/v2/assets/{identifier}
    //      Returns raw file bytes for an asset addressed by its identifier.
    //      Defaults: working version, site default language.
    // -----------------------------------------------------------------------

    /**
     * Read raw file-asset bytes by identifier.
     *
     * <p>Resolves the working version by default using the site default language.
     * The caller must have READ permission on the contentlet; otherwise a 403 is
     * returned (never the bytes).
     *
     * @param request    current HTTP request
     * @param response   current HTTP response
     * @param identifier asset identifier (UUID)
     * @param language   optional language tag; defaults to site default
     * @param version    {@code working} (default) or {@code live}
     * @return raw bytes of the file
     */
    @Operation(
            operationId = "getFileAssetById",
            summary     = "Get raw file-asset content by identifier",
            description = "Streams the raw bytes of a file asset addressed by its dotCMS identifier (UUID). " +
                          "Returns the working version in the site default language by default. " +
                          "READ permission is enforced — 403 is returned when the user lacks permission."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                     description = "Raw file bytes streamed with resolved MIME type and Content-Disposition header",
                     content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)),
        @ApiResponse(responseCode = "400",
                     description = "Bad Request — unknown language tag",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON)),
        @ApiResponse(responseCode = "401",
                     description = "Unauthorized — authentication required",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON)),
        @ApiResponse(responseCode = "403",
                     description = "Forbidden — user does not have READ permission on this asset",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON)),
        @ApiResponse(responseCode = "404",
                     description = "Not Found — identifier not found, not a file asset, or language version unavailable",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @GET
    @JSONP
    @NoCache
    @Path("/{identifier}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getById(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Asset identifier (UUID)", required = true,
                       example = "48190c8c-42c4-46af-8d1a-0cd5db894797")
            @PathParam("identifier") final String identifier,
            @Parameter(description = "Language tag (e.g. en-US). Defaults to site default language.",
                       example = "en-US")
            @QueryParam("language") final String language,
            @Parameter(description = "Asset version to retrieve. Accepted values: working (default), live.",
                       example = "working",
                       schema = @Schema(allowableValues = {"working", "live"}))
            @QueryParam("version") @DefaultValue("working") final String version
    ) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final User user = initData.getUser();
        Logger.debug(this, () -> String.format(
                "User [%s] reading asset by identifier [%s] lang=[%s] version=[%s]",
                user.getUserId(), identifier, language, version));

        final boolean live = "live".equalsIgnoreCase(version);
        final long languageId = resolveLanguageId(language);

        // Resolve the contentlet — throws NotFoundInDbException (→ 404) when absent.
        final Contentlet contentlet = findContentlet(identifier, live, languageId, user);

        // Enforce READ permission explicitly — never serve bytes to unauthorized callers.
        if (!permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user, false)) {
            throw new DotSecurityException(
                    String.format("User [%s] does not have READ permission on asset [%s]",
                            user.getUserId(), identifier));
        }

        if (!contentlet.isFileAsset()) {
            throw new NotFoundInDbException(
                    String.format("Content [%s] is not a file asset.", identifier));
        }

        final FileAsset fileAsset = fileAssetAPI.fromContentlet(contentlet);
        return buildFileResponse(fileAsset);
    }

    // -----------------------------------------------------------------------
    // PUT  /api/v2/assets/save
    //      Create or update — working version only.
    // -----------------------------------------------------------------------

    /**
     * Save (working version only) a file asset via multipart upload.
     *
     * <p>Flat form fields: {@code file} (binary), {@code path} (host-qualified path including
     * filename), {@code language} (optional — defaults to site default language).
     *
     * @param request             current HTTP request
     * @param response            current HTTP response
     * @param fileInputStream     binary file content
     * @param contentDisposition  file disposition (carries original filename)
     * @param path                host-qualified target path including filename
     * @param language            optional language tag
     * @return {@link ResponseEntityFileAssetView} with persisted asset metadata
     */
    @Operation(
            operationId = "saveFileAsset",
            summary     = "Save file asset (working version)",
            description = "Creates or updates a file asset at the given host-qualified path. " +
                          "Only the working version is affected — the live version is NOT changed. " +
                          "Submit via multipart/form-data with fields: file (binary), " +
                          "path (//host/folder/file.ext), language (optional)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                     description = "Asset saved successfully as working version",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                        schema = @Schema(implementation = ResponseEntityFileAssetView.class))),
        @ApiResponse(responseCode = "400",
                     description = "Bad Request — missing file part, zero-byte file, unknown language, or invalid path",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON)),
        @ApiResponse(responseCode = "401",
                     description = "Unauthorized — authentication required",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON)),
        @ApiResponse(responseCode = "403",
                     description = "Forbidden — insufficient permissions on target folder",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON)),
        @ApiResponse(responseCode = "404",
                     description = "Not Found — unknown host in path",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @PUT
    @JSONP
    @NoCache
    @Path("/save")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityFileAssetView save(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Binary file content", required = true)
            @FormDataParam("file") final InputStream fileInputStream,
            @Parameter(description = "File content disposition (carries filename)")
            @FormDataParam("file") final FormDataContentDisposition contentDisposition,
            @Parameter(description = "Host-qualified asset path including filename, " +
                                     "e.g. //demo.dotcms.com/application/foo.vtl",
                       required = true,
                       example = "//demo.dotcms.com/application/containers/default/banner.vtl")
            @FormDataParam("path") final String path,
            @Parameter(description = "Language tag (e.g. en-US). Defaults to site default language.",
                       example = "en-US")
            @FormDataParam("language") final String language
    ) throws DotDataException, DotSecurityException, IOException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final User user = initData.getUser();
        Logger.debug(this, () -> String.format(
                "User [%s] saving asset (working) at path [%s] lang=[%s]",
                user.getUserId(), path, language));

        return writeAsset(request, user, fileInputStream, contentDisposition, path, language, false);
    }

    // -----------------------------------------------------------------------
    // PUT  /api/v2/assets/publish
    //      Create or update — promotes to live immediately.
    // -----------------------------------------------------------------------

    /**
     * Publish a file asset (working + live) via multipart upload.
     *
     * <p>Flat form fields: {@code file} (binary), {@code path} (host-qualified path including
     * filename), {@code language} (optional — defaults to site default language).
     *
     * @param request             current HTTP request
     * @param response            current HTTP response
     * @param fileInputStream     binary file content
     * @param contentDisposition  file disposition (carries original filename)
     * @param path                host-qualified target path including filename
     * @param language            optional language tag
     * @return {@link ResponseEntityFileAssetView} with persisted asset metadata
     */
    @Operation(
            operationId = "publishFileAsset",
            summary     = "Publish file asset (working + live)",
            description = "Creates or updates a file asset at the given host-qualified path and " +
                          "immediately publishes it (promotes to live). " +
                          "Submit via multipart/form-data with fields: file (binary), " +
                          "path (//host/folder/file.ext), language (optional)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                     description = "Asset saved and published successfully",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                        schema = @Schema(implementation = ResponseEntityFileAssetView.class))),
        @ApiResponse(responseCode = "400",
                     description = "Bad Request — missing file part, zero-byte file, unknown language, or invalid path",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON)),
        @ApiResponse(responseCode = "401",
                     description = "Unauthorized — authentication required",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON)),
        @ApiResponse(responseCode = "403",
                     description = "Forbidden — insufficient permissions on target folder",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON)),
        @ApiResponse(responseCode = "404",
                     description = "Not Found — unknown host in path",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @PUT
    @JSONP
    @NoCache
    @Path("/publish")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityFileAssetView publish(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Binary file content", required = true)
            @FormDataParam("file") final InputStream fileInputStream,
            @Parameter(description = "File content disposition (carries filename)")
            @FormDataParam("file") final FormDataContentDisposition contentDisposition,
            @Parameter(description = "Host-qualified asset path including filename, " +
                                     "e.g. //demo.dotcms.com/application/foo.vtl",
                       required = true,
                       example = "//demo.dotcms.com/application/containers/default/banner.vtl")
            @FormDataParam("path") final String path,
            @Parameter(description = "Language tag (e.g. en-US). Defaults to site default language.",
                       example = "en-US")
            @FormDataParam("language") final String language
    ) throws DotDataException, DotSecurityException, IOException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final User user = initData.getUser();
        Logger.debug(this, () -> String.format(
                "User [%s] publishing asset at path [%s] lang=[%s]",
                user.getUserId(), path, language));

        return writeAsset(request, user, fileInputStream, contentDisposition, path, language, true);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Core write logic shared by save and publish endpoints.
     *
     * <p>Performs zero-byte and missing-file-part validation before delegating to
     * {@link WebAssetHelper#saveUpdateAsset(HttpServletRequest, FileUploadData, User)}.
     * The language is resolved to site default when blank.
     */
    private ResponseEntityFileAssetView writeAsset(
            final HttpServletRequest request,
            final User user,
            final InputStream fileInputStream,
            final FormDataContentDisposition contentDisposition,
            final String path,
            final String language,
            final boolean live) throws DotDataException, DotSecurityException, IOException {

        // Validate required form fields.
        if (!UtilMethods.isSet(path)) {
            throw new BadRequestException("Form parameter 'path' is required.");
        }
        if (fileInputStream == null) {
            throw new BadRequestException(
                    "Missing 'file' part — a multipart field named 'file' with binary content is required.");
        }

        // Zero-byte guard: wrap in a BufferedInputStream so we can peek without consuming.
        // mark(1) / read(1) / reset() tells us whether the stream has any bytes.
        final BufferedInputStream buffered = (fileInputStream instanceof BufferedInputStream)
                ? (BufferedInputStream) fileInputStream
                : new BufferedInputStream(fileInputStream);
        buffered.mark(1);
        final boolean isEmpty = buffered.read() == -1;
        buffered.reset();
        if (isEmpty) {
            throw new BadRequestException(
                    "File part is present but contains zero bytes — a non-empty file is required.");
        }

        // Validate language up-front: blank → site default (no error), non-blank unknown → 400.
        final String resolvedLang = resolveLanguageParam(language);

        // Adapt flat v2 form fields into the v1 FileUploadData/FileUploadDetail model so we can
        // reuse WebAssetHelper.saveUpdateAsset unchanged.
        final FileUploadDetail detail = new FileUploadDetail(path, resolvedLang, live);
        final FileUploadData uploadData = new FileUploadData();
        uploadData.setFileInputStream(buffered);
        uploadData.setContentDisposition(contentDisposition);
        uploadData.setDetail(detail);

        final WebAssetView saved = helper.saveUpdateAsset(request, uploadData, user);

        return buildFileAssetResponse(saved);
    }

    /**
     * Converts a {@link WebAssetView} (returned by the helper after save) into a
     * {@link ResponseEntityFileAssetView} including the persisted file size.
     */
    private ResponseEntityFileAssetView buildFileAssetResponse(final WebAssetView view) {
        if (!(view instanceof AssetView)) {
            // Should not happen for a successful file-asset write.
            throw new IllegalStateException(
                    "Unexpected view type returned by saveUpdateAsset: " + view.getClass().getName());
        }
        final AssetView assetView = (AssetView) view;
        final Map<String, Object> metadata = assetView.metadata();

        final long fileSize = extractFileSize(metadata);
        final String path   = extractString(metadata, "path", "");

        final FileAssetView fileAssetView = new FileAssetView(
                assetView.identifier(),
                assetView.inode(),
                assetView.name(),
                path,
                assetView.lang(),
                assetView.live(),
                assetView.working(),
                fileSize);

        return new ResponseEntityFileAssetView(fileAssetView);
    }

    /**
     * Safely extracts the file size from the contentlet metadata map.
     * The v1 helper stores the size under the key {@code size} (set in
     * {@link WebAssetHelper#toAsset(FileAsset)} as {@code fileAsset.getFileSize()}).
     * Falls back to 0 when the key is absent or not numeric.
     */
    private long extractFileSize(final Map<String, Object> metadata) {
        if (metadata == null) {
            return 0L;
        }
        final Object sizeObj = metadata.get("size");
        if (sizeObj instanceof Number) {
            return ((Number) sizeObj).longValue();
        }
        if (sizeObj != null) {
            return Try.of(() -> Long.parseLong(sizeObj.toString())).getOrElse(0L);
        }
        return 0L;
    }

    /** Safely reads a String from a metadata map, with a default fallback. */
    private String extractString(final Map<String, Object> metadata, final String key, final String defaultValue) {
        if (metadata == null) {
            return defaultValue;
        }
        final Object value = metadata.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Resolves a language query/form parameter to a language tag string.
     * <ul>
     *   <li>Blank/null → site default language tag (no error).</li>
     *   <li>Non-blank but unrecognised → {@link BadRequestException} (400).</li>
     * </ul>
     */
    private String resolveLanguageParam(final String language) {
        if (!UtilMethods.isSet(language)) {
            // Blank → site default; WebAssetHelper.parseLang handles the actual resolution.
            return languageAPI.getDefaultLanguage().toString();
        }
        // Non-blank: validate existence.
        final Optional<Language> resolved = helper.parseLang(language, false);
        if (resolved.isEmpty() || resolved.get().getId() == 0) {
            throw new BadRequestException(
                    String.format("Unknown language tag [%s]. " +
                            "Provide a valid language code such as 'en-US' or leave blank for the site default.",
                            language));
        }
        return language;
    }

    /**
     * Resolves a language parameter to a language ID for identifier-based lookups.
     * Blank → site default language ID; unknown → 400.
     */
    private long resolveLanguageId(final String language) {
        if (!UtilMethods.isSet(language)) {
            return languageAPI.getDefaultLanguage().getId();
        }
        final Optional<Language> resolved = helper.parseLang(language, false);
        if (resolved.isEmpty() || resolved.get().getId() == 0) {
            throw new BadRequestException(
                    String.format("Unknown language tag [%s].", language));
        }
        return resolved.get().getId();
    }

    /**
     * Looks up a contentlet by identifier, version, and language.
     * Throws {@link NotFoundInDbException} (→ 404) when not found.
     */
    private Contentlet findContentlet(
            final String identifier,
            final boolean live,
            final long languageId,
            final User user) throws DotDataException, DotSecurityException {

        // Use system user for the lookup — permission check is explicit below.
        Contentlet contentlet = Try.of(() ->
                contentletAPI.findContentletByIdentifier(
                        identifier, live, languageId, APILocator.systemUser(), false))
                .getOrNull();

        if (contentlet == null) {
            throw new NotFoundInDbException(
                    String.format("Asset with identifier [%s] not found for language id [%d] and " +
                                  "version [%s].", identifier, languageId,
                            live ? "live" : "working"));
        }
        return contentlet;
    }

    /**
     * Streams a {@link FileAsset}'s underlying file as a JAX-RS {@link Response} with:
     * <ul>
     *   <li>Resolved MIME type (falls back to {@code application/octet-stream}).</li>
     *   <li>{@code Content-Disposition: attachment; filename="…"} header.</li>
     * </ul>
     */
    private Response buildFileResponse(final FileAsset fileAsset) {
        final File file = fileAsset.getFileAsset();
        if (file == null || !file.exists()) {
            throw new DoesNotExistException(
                    String.format("Binary for asset [%s] not found on disk.", fileAsset.getIdentifier()));
        }

        final String mimeType = resolveMimeType(file.getName(), fileAsset.getMimeType());
        final StreamingOutput streamingOutput = outputStream -> {
            try (final java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                final byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
            }
        };

        return Response.ok(streamingOutput, mimeType)
                .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
                .header("Content-Length", file.length())
                .build();
    }

    /**
     * Resolves the MIME type for a file, preferring the stored value and falling back to
     * {@link URLConnection#guessContentTypeFromName(String)} then
     * {@code application/octet-stream}.
     */
    private String resolveMimeType(final String fileName, final String storedMimeType) {
        if (UtilMethods.isSet(storedMimeType)) {
            return storedMimeType;
        }
        final String guessed = URLConnection.guessContentTypeFromName(fileName);
        return UtilMethods.isSet(guessed) ? guessed : MediaType.APPLICATION_OCTET_STREAM;
    }

    /**
     * Builds an {@link AssetsRequestForm} (the immutable generated from
     * {@link com.dotcms.rest.api.v1.asset.AbstractAssetsRequestForm}) for path-based reads.
     */
    private AssetsRequestForm buildAssetsRequestForm(
            final String path, final String language, final boolean live) {
        return AssetsRequestForm.builder()
                .assetPath(path)
                .language(language)
                .live(live)
                .build();
    }
}
