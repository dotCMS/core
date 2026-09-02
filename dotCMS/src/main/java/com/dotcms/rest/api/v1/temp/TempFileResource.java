package com.dotcms.rest.api.v1.temp;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.mock.request.DotCMSMockRequest;
import com.dotcms.mock.request.DotCMSMockRequestWithSession;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.authentication.RequestUtil;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.api.v1.workflow.WorkflowResource;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.SecurityUtils;
import com.dotcms.workflow.form.FireMultipleActionForm;
import com.dotmarketing.beans.Request;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.HttpHeaders;
import com.liferay.util.StringPool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Try;
import org.apache.commons.lang.time.StopWatch;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.server.JSONP;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

@Path("/v1/temp")
@Tag(name = "Temporary Files", description = "Temporary file upload and management for content creation")
public class TempFileResource {

    public final static String MAX_FILE_LENGTH_PARAM ="maxFileLength";
    private final TempFileAPI tempApi;

    /**
     * Default constructor.
     */
    public TempFileResource() {
        this( APILocator.getTempFileAPI());
    }

    @VisibleForTesting
    TempFileResource(final TempFileAPI tempApi) {

        this.tempApi = tempApi;
    }

    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(
            operationId = "uploadTempFileMultipart",
            summary = "Upload temporary files via multipart form",
            description = "Uploads one or more files as temporary resources via multipart form data. " +
                    "Files are stored temporarily and can be referenced when creating content. " +
                    "The response streams back a JSON object with the created temporary file references. " +
                    "Anonymous access can be allowed via the TEMP_RESOURCE_ALLOW_ANONYMOUS configuration property.\n\n" +
                    "**Use this endpoint to supply files for binary and image fields** (`ImmutableBinaryField`, `ImmutableImageField`) " +
                    "when creating or updating contentlets. After uploading, pass `tempFiles[0].id` " +
                    "(e.g. `\"temp_5311313004\"`) as the field value in the workflow fire endpoint. " +
                    "See `PUT /api/v1/workflow/actions/default/fire/{systemAction}` for the full pattern.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Temporary files created successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = TempFilesView.class),
                                    examples = @ExampleObject(
                                            value = "{\n" +
                                                    "  \"tempFiles\": [\n" +
                                                    "    {\n" +
                                                    "      \"id\": \"temp_5311313004\",\n" +
                                                    "      \"fileName\": \"hero.jpg\",\n" +
                                                    "      \"length\": 84471,\n" +
                                                    "      \"mimeType\": \"image/jpeg\",\n" +
                                                    "      \"image\": true,\n" +
                                                    "      \"referenceUrl\": \"/dA/temp_5311313004/tmp/hero.jpg\",\n" +
                                                    "      \"metadata\": {\n" +
                                                    "        \"contentType\": \"image/jpeg\",\n" +
                                                    "        \"height\": 522,\n" +
                                                    "        \"width\": 900,\n" +
                                                    "        \"fileSize\": 84471,\n" +
                                                    "        \"isImage\": true\n" +
                                                    "      }\n" +
                                                    "    }\n" +
                                                    "  ]\n" +
                                                    "}"))),
                    @ApiResponse(responseCode = "400", description = "Invalid file, origin, or referer"),
                    @ApiResponse(responseCode = "401", description = "Authentication required (when anonymous access is disabled)"),
                    @ApiResponse(responseCode = "404", description = "Temp file resource is not enabled")
            }
    )
    public final Response uploadTempResourceMulti(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Maximum file length in bytes (-1 for default)")
            @DefaultValue("-1") @QueryParam(MAX_FILE_LENGTH_PARAM) final String maxFileLengthString, // this is being used later
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Multipart form data with one or more files to upload temporarily. Files are stored for a limited time and can be referenced when creating content.",
                    required = true,
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(type = "object",
                                    description = "One or more file parts to upload"))
            )
            final FormDataMultiPart body) {

        verifyTempResourceEnabled();

        final boolean allowAnonToUseTempFiles = Config
                .getBooleanProperty(TempFileAPI.TEMP_RESOURCE_ALLOW_ANONYMOUS, true);

        new WebResource.InitBuilder(request, response)
          .requiredAnonAccess(AnonymousAccess.WRITE)
          .rejectWhenNoUser(!allowAnonToUseTempFiles)
          .init();

        if (!new SecurityUtils().validateReferer(request)) {

            throw new BadRequestException("Invalid Origin or referer");
        }

        return Response.ok(new MultipleBinaryStreamingOutput(body, request))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
    }

    protected class MultipleBinaryStreamingOutput implements StreamingOutput {

        private final FormDataMultiPart body;
        private final HttpServletRequest request;

        private MultipleBinaryStreamingOutput(final FormDataMultiPart body,
                                                  final HttpServletRequest request) {

            this.body    = body;
            this.request = request;
        }

        @Override
        public void write(final OutputStream output) throws IOException, WebApplicationException {

            final ObjectMapper objectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
            TempFileResource.this.saveMultipleBinary(body, request, output, objectMapper);
        }
    }

    private void saveMultipleBinary(final FormDataMultiPart body, final HttpServletRequest request,
                                    final OutputStream outputStream, final ObjectMapper objectMapper) {

        final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance().getSubmitter("TEMP_API_SUBMITTER",
                new DotConcurrentFactory.SubmitterConfigBuilder().poolSize(2).maxPoolSize(5).queueCapacity(100000).build());
        final CompletionService<Object> completionService = new ExecutorCompletionService<>(dotSubmitter);
        final List<Future<Object>> futures = new ArrayList<>();
        final HttpServletRequest statelessRequest = RequestUtil.INSTANCE.createStatelessRequest(request);

        int index = 1;
        for (final BodyPart part : body.getBodyParts()) {

            // this triggers the save
            final int futureIndex = index;
            final Future<Object> future = completionService.submit(() -> {

                try {
                    final InputStream in = (part.getEntity() instanceof InputStream) ?
                            InputStream.class.cast(part.getEntity())
                            : Try.of(() -> part.getEntityAs(InputStream.class)).getOrNull();

                    if (in == null) {

                        return new ErrorEntity(String.valueOf(HttpServletResponse.SC_BAD_REQUEST), "Invalid Binary Part, index: " + futureIndex,
                                String.valueOf(futureIndex));
                    }

                    final ContentDisposition meta = part.getContentDisposition();
                    if (meta == null) {

                        return new ErrorEntity(String.valueOf(HttpServletResponse.SC_BAD_REQUEST), "Invalid Binary Part, index: " + futureIndex,
                                String.valueOf(futureIndex));
                    }

                    final String fileName = meta.getFileName();
                    if (fileName == null || fileName.startsWith(".") || fileName.contains("/.")) {

                        return new ErrorEntity(String.valueOf(HttpServletResponse.SC_BAD_REQUEST),
                                "Invalid Binary Part, Name: " + fileName + ", index: " + futureIndex,
                                String.valueOf(futureIndex));
                    }

                    final String sanitize = sanitizeFileName(meta);

                    return this.tempApi.createTempFile(sanitize, statelessRequest, in);
                } catch (Exception e) {

                    Logger.error(this, e.getMessage(), e);
                    return new ErrorEntity(String.valueOf(HttpServletResponse.SC_BAD_REQUEST),
                            "Invalid Binary Part, Message: " + e.getMessage() + ", index: " + futureIndex,
                            String.valueOf(futureIndex));
                }
            });

            ++index;
            futures.add(future);
        }

        printResponseEntityViewResult(outputStream, objectMapper, completionService, futures);
    }

    private static @NotNull String sanitizeFileName(ContentDisposition meta) {
        // Jersey decodes multipart Content-Disposition filenames as ISO-8859-1.
        // Re-interpret those bytes as UTF-8 to recover the original filename,
        // then normalize to NFC for consistent Unicode representation.
        // ASSUMPTION: modern browsers (HTML5 / RFC 6266) send UTF-8 bytes in
        // Content-Disposition filenames. This round-trip silently drops high bytes
        // from genuine ISO-8859-1 filenames sent by legacy or non-browser clients.
        final String raw = meta.getFileName();
        final String utf8Name = new String(raw.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)
                .replace("\uFFFD", "");
        final String nfcName = Normalizer.normalize(utf8Name, Normalizer.Form.NFC);
        return FileUtil.sanitizeFileName(nfcName);
    }

    private void printResponseEntityViewResult(final OutputStream outputStream,
                                               final ObjectMapper objectMapper,
                                               final CompletionService<Object> completionService,
                                               final List<Future<Object>> futures) {

        try {

            outputStream.write(StringPool.OPEN_CURLY_BRACE.getBytes(StandardCharsets.UTF_8));
            ResponseUtil.beginWrapProperty(outputStream, "tempFiles", false);
            outputStream.write(StringPool.OPEN_BRACKET.getBytes(StandardCharsets.UTF_8));
            // now recover the N results
            for (int i = 0; i < futures.size(); i++) {

                try {

                    Logger.info(this, "Recovering the result " + (i + 1) + " of " + futures.size());
                    objectMapper.writeValue(outputStream, completionService.take().get());

                    if (i < futures.size()-1) {
                        outputStream.write(StringPool.COMMA.getBytes(StandardCharsets.UTF_8));
                    }
                } catch (InterruptedException | ExecutionException | IOException e) {

                    Logger.error(this, e.getMessage(), e);
                }
            }

            outputStream.write(StringPool.CLOSE_BRACKET.getBytes(StandardCharsets.UTF_8));
            ResponseUtil.endWrapProperty(outputStream);
        } catch (IOException e) {

            Logger.error(this, e.getMessage(), e);
        }
    }



    @POST
    @Path("/byUrl")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(
            operationId = "createTempFileFromUrl",
            summary = "Create temporary file from a remote URL",
            description = "Downloads a file from the specified remote URL and stores it as a temporary resource. " +
                    "The temporary file can then be referenced when creating content. " +
                    "The URL must pass validation to prevent SSRF attacks. " +
                    "Anonymous access can be allowed via the TEMP_RESOURCE_ALLOW_ANONYMOUS configuration property.\n\n" +
                    "**Use this endpoint to supply files for binary and image fields** (`ImmutableBinaryField`, `ImmutableImageField`) " +
                    "when a file is already accessible by URL. Send `{\"remoteUrl\": \"https://example.com/image.jpg\"}` " +
                    "and pass `tempFiles[0].id` (e.g. `\"temp_5311313004\"`) as the field value in the workflow fire endpoint. " +
                    "See `PUT /api/v1/workflow/actions/default/fire/{systemAction}` for the full pattern.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Temporary file created from URL successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = TempFilesView.class),
                                    examples = @ExampleObject(
                                            value = "{\n" +
                                                    "  \"tempFiles\": [\n" +
                                                    "    {\n" +
                                                    "      \"id\": \"temp_5311313004\",\n" +
                                                    "      \"fileName\": \"hero.jpg\",\n" +
                                                    "      \"length\": 84471,\n" +
                                                    "      \"mimeType\": \"image/jpeg\",\n" +
                                                    "      \"image\": true,\n" +
                                                    "      \"referenceUrl\": \"/dA/temp_5311313004/tmp/hero.jpg\",\n" +
                                                    "      \"metadata\": {\n" +
                                                    "        \"contentType\": \"image/jpeg\",\n" +
                                                    "        \"height\": 522,\n" +
                                                    "        \"width\": 900,\n" +
                                                    "        \"fileSize\": 84471,\n" +
                                                    "        \"isImage\": true\n" +
                                                    "      }\n" +
                                                    "    }\n" +
                                                    "  ]\n" +
                                                    "}"))),
                    @ApiResponse(responseCode = "400", description = "Invalid URL, missing URL, or invalid origin/referer"),
                    @ApiResponse(responseCode = "401", description = "Authentication required (when anonymous access is disabled)"),
                    @ApiResponse(responseCode = "404", description = "Temp file resource is not enabled")
            }
    )
    public final Response copyTempFromUrl(@Context final HttpServletRequest request,@Context final HttpServletResponse response,
            final RemoteUrlForm form) {

        try {

            verifyTempResourceEnabled();

            final boolean allowAnonToUseTempFiles = Config
                .getBooleanProperty(TempFileAPI.TEMP_RESOURCE_ALLOW_ANONYMOUS, true);

              new WebResource.InitBuilder(request, response)
                .requiredAnonAccess(AnonymousAccess.WRITE)
                .rejectWhenNoUser(!allowAnonToUseTempFiles)
                .init();

            if (!new SecurityUtils().validateReferer(request)) {
                throw new BadRequestException("Invalid Origin or referer");
            }
            if(!UtilMethods.isSet(form.remoteUrl)){
                throw new BadRequestException("No Url passed");
            }
            if (!tempApi.validUrl(form.remoteUrl)) {
                throw new BadRequestException("Invalid url attempted for tempFile : " + form.remoteUrl);
            }

            final List<DotTempFile> tempFiles = new ArrayList<>();
            tempFiles.add(tempApi
                    .createTempFileFromUrl(form.fileName, request, new URL(form.remoteUrl),
                            form.urlTimeoutSeconds));

            return Response.ok(ImmutableMap.of("tempFiles", tempFiles)).build();

        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    private void verifyTempResourceEnabled(){
        if (!Config.getBooleanProperty(TempFileAPI.TEMP_RESOURCE_ENABLED, true)) {
            final String message = "Temp Files Resource is not enabled, please change the TEMP_RESOURCE_ENABLED to true in your properties file";
            Logger.error(this, message);
            throw new DoesNotExistException(message);
        }
    }

}
