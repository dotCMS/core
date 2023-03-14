package com.dotcms.rest.api.v1.temp;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.authentication.RequestUtil;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.HttpHeaders;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

/**
 * This REST Endpoint allows Users to interact with the Temp File API in dotCMS. Temporary Files will live in the system
 * for a specified amount of time -- 30 minutes by default. After that they won't be accessible anymore..
 *
 * @author Will Ezell
 * @since Jul 8th, 2019
 */
@Path("/v1/temp")
public class TempFileResource {

    public static final String MAX_FILE_LENGTH_PARAM ="maxFileLength";
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

    /**
     * Uploads a binary file to dotCMS via the Temp File API.
     *
     * @param request             The current instance of the {@link HttpServletRequest}.
     * @param response            The current instance of the {@link HttpServletResponse}.
     * @param maxFileLengthString The maximum allowed size for the uploaded file. If not specified, the dotCMS default
     *                            value will be used instead.
     * @param body                The {@link FormDataMultiPart} object containing the file.
     *
     * @return A JSON response including important information related to the recently uploaded Temporary File.
     */
    @POST
    @JSONP
    @NoCache
    @Produces("application/octet-stream")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response uploadTempResourceMulti(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response, 
            @DefaultValue("-1") @QueryParam(MAX_FILE_LENGTH_PARAM) final String maxFileLengthString, // this is being used later
            final FormDataMultiPart body) {
        this.checkEndpointAccess(request, response);
        return Response.ok(new MultipleBinaryStreamingOutput(body, request))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Streaming Output class used for saving one or more binary files as Temporary Files.
     */
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
            saveMultipleBinaryFiles(body, request, output, objectMapper);
        }

        /**
         * Saves the binary file or files that are being submitted by the user. A Completion Service is used to improve the
         * time it takes to save every Temporary File.
         *
         * @param body          The {@link FormDataMultiPart} containing the file or files that will be saved.
         * @param request       The current instance of the {@link HttpServletRequest}.
         * @param outputStream  The streaming output of the response.
         * @param objectMapper  The {@link ObjectMapper} that transforms the response into a JSON object.
         */
        private void saveMultipleBinaryFiles(final FormDataMultiPart body, final HttpServletRequest request,
                                             final OutputStream outputStream, final ObjectMapper objectMapper) {
            final CompletionService<Object> completionService = createCompletionService(2, 5, 100000);
            final List<Future<Object>> futures = new ArrayList<>();
            final HttpServletRequest statelessRequest = RequestUtil.INSTANCE.createStatelessRequest(request);
            int index = 1;
            for (final BodyPart part : body.getBodyParts()) {
                // this triggers the save
                final int futureIndex = index;
                final Future<Object> future = completionService.submit(() -> {

                    try {
                        final InputStream in = (part.getEntity() instanceof InputStream) ? (InputStream) part.getEntity() :
                                                       Try.of(() -> part.getEntityAs(InputStream.class)).getOrNull();
                        final ContentDisposition meta = part.getContentDisposition();
                        final Optional<ErrorEntity> errorEntity = validateFileData(in, meta, futureIndex);
                        return errorEntity.isPresent() ? errorEntity.get() :
                                       tempApi.createTempFile(meta.getFileName(), statelessRequest, in);
                    } catch (final Exception e) {
                        final String errorMsg =
                                "Invalid Binary Part, Message: " + e.getMessage() + ", index: " + futureIndex;
                        Logger.error(this, errorMsg, e);
                        return new ErrorEntity(String.valueOf(HttpServletResponse.SC_BAD_REQUEST), errorMsg,
                                String.valueOf(futureIndex));
                    }

                });
                ++index;
                futures.add(future);
            }
            printResponseEntityViewResult(outputStream, objectMapper, completionService, futures);
        }

        /**
         * Verifies that the information retrieved for the uploaded binary file is correct and readable.
         *
         * @param inputStream The content of the binary file as an {@link InputStream} object.
         * @param meta        The {@link ContentDisposition} containing the file's metadata.
         * @param futureIndex The index representing the order in which this file is being processed.
         *
         * @return An {@link Optional} with the result of the validation. An empty optional means that no errors were
         * found.
         */
        private Optional<ErrorEntity> validateFileData(final InputStream inputStream, final ContentDisposition meta,
                                                       final int futureIndex) {
            if (null == inputStream) {
                return Optional.of(new ErrorEntity(String.valueOf(HttpServletResponse.SC_BAD_REQUEST), "Invalid inout" +
                                                                                                               " stream Binary Part, index: " + futureIndex, String.valueOf(futureIndex)));
            }
            if (null == meta) {
                return Optional.of(new ErrorEntity(String.valueOf(HttpServletResponse.SC_BAD_REQUEST), "Invalid metadata Binary Part, index: " + futureIndex, String.valueOf(futureIndex)));
            }
            final String fileName = meta.getFileName();
            if (UtilMethods.isNotSet(fileName) || fileName.startsWith(StringPool.PERIOD) || fileName.contains("/.")) {
                return Optional.of(new ErrorEntity(String.valueOf(HttpServletResponse.SC_BAD_REQUEST), "Invalid Binary Part, Name: " + fileName + ", index: " + futureIndex, String.valueOf(futureIndex)));
            }
            return Optional.empty();
        }

    }

    /**
     * Updates a specific Temporary File with the provided content as a String. If such a file doesn't exist or has
     * expired, a brand new Temporary File with the specified content will be generated instead.
     *
     * @param request       The current instance of the {@link HttpServletRequest}.
     * @param response      The current instance of the {@link HttpServletResponse}.
     * @param tempFileId    The ID of the Temporary File that will be updated.
     * @param form          The {@link PlainTextFileForm} with the required file information.
     *
     * @return A JSON response including important information related to the recently updated Temporary File.
     */
    @PUT
    @Path("/id/{tempFileId: .*}")
    @JSONP
    @NoCache
    @Produces("application/octet-stream")
    @Consumes(MediaType.APPLICATION_JSON)
    public final Response upsertTempResource(@Context final HttpServletRequest request,
                                             @Context final HttpServletResponse response,
                                             @PathParam("tempFileId") final String tempFileId,
                                             final PlainTextFileForm form) {
        this.checkEndpointAccess(request, response, false);
        final StreamingOutput streamingOutput = output -> {

            final ObjectMapper objectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
            TempFileResource.this.savePlainTextFile(request, objectMapper, output, tempFileId, form);

        };
        return Response.ok(streamingOutput).header(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_JSON).build();
    }

    /**
     * Overwrites a specific Temporary File with new content.
     *
     * @param request       The current instance of the {@link HttpServletRequest}.
     * @param objectMapper  The {@link ObjectMapper} that transforms the response into a JSON object.
     * @param outputStream  The streaming output for the response.
     * @param tempFileId    The ID of the Temporary File that is being overwritten.
     * @param form          The {@link PlainTextFileForm} object with the information of the submitted file.
     */
    private void savePlainTextFile(final HttpServletRequest request, final ObjectMapper objectMapper, final OutputStream outputStream, final String tempFileId, final PlainTextFileForm form) {
        final CompletionService<Object> completionService = this.createCompletionService(1, 1, 10);
        final Future<Object> future = this.updateTempFile(completionService,
                RequestUtil.INSTANCE.createStatelessRequest(request), tempFileId, form.fileName(),
                new ByteArrayInputStream(form.fileContent().getBytes()));
        this.printResponseEntityViewResult(outputStream, objectMapper, completionService, List.of(future));
    }

    /**
     * Saves the content of the specified Temporary File. In order to improve the performance, a
     * {@link CompletionService} task care of creating a Future task that calls the Temp File API that actually saves
     * the new content of the Temporary File.
     *
     * @param completionService The {@link CompletionService} instance that will update the Temporary File.
     * @param statelessRequest  A stateless {@link HttpServletRequest} taht is used to call the Temp File API.
     * @param tempFileId        The ID of the Temporary File that is being overwritten.
     * @param fileName          The name of the Temporary File. If the file doesn't exist or has expired, this value
     *                          will be used to create the new Temporary File.
     * @param in                The new content of the file as an {@link InputStream} object.
     *
     * @return The {@link Future} task that will save the Temporary File.
     */
    private Future<Object> updateTempFile(final CompletionService<Object> completionService,
                                          final HttpServletRequest statelessRequest,
                                          final String tempFileId, final String fileName, final InputStream in) {
        return completionService.submit(() -> {

            try {
                if (in == null) {
                    return new ErrorEntity(String.valueOf(HttpServletResponse.SC_BAD_REQUEST), "Invalid Binary Stream", fileName);
                }
                final String sanitizedFileName = FileUtil.sanitizeFileName(fileName);
                return this.tempApi.upsertTempFile(statelessRequest, tempFileId, sanitizedFileName, in);
            } catch (final Exception e) {
                Logger.error(this, e.getMessage(), e);
                return new ErrorEntity(String.valueOf(HttpServletResponse.SC_BAD_REQUEST), "Invalid Binary Part, " +
                                                                                                   "Message: " + e.getMessage(), fileName);
            }

        });
    }

    /**
     * Returns the basic information of the created Temporary File as a JSON object. The data provided here is very
     * useful for the service or user that called this endpoint in order to get a summary of the Temporary File that was
     * created.
     *
     * @param outputStream      The streaming output for the response.
     * @param objectMapper      The {@link ObjectMapper} that transforms the response into a JSON object.
     * @param completionService The {@link CompletionService} instance containing the task that saved/updated the
     *                          Temporary File.
     * @param futures           The list of {@link Future} tasks that were created when saving one or more files.
     */
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
                    Logger.debug(this, "Recovering result " + (i + 1) + " of " + futures.size());
                    objectMapper.writeValue(outputStream, completionService.take().get());
                    if (i < futures.size()-1) {
                        outputStream.write(StringPool.COMMA.getBytes(StandardCharsets.UTF_8));
                    }
                } catch (final InterruptedException e) {
                    Logger.error(this, "Thread has been interrupted: " + e.getMessage(), e);
                    Thread.currentThread().interrupt();
                } catch (final ExecutionException | IOException e) {
                    Logger.error(this, e.getMessage(), e);
                }
            }
            outputStream.write(StringPool.CLOSE_BRACKET.getBytes(StandardCharsets.UTF_8));
            ResponseUtil.endWrapProperty(outputStream);
        } catch (final IOException e) {
            Logger.error(this, e.getMessage(), e);
        }
    }

    @POST
    @Path("/byUrl")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON})
    public final Response copyTempFromUrl(@Context final HttpServletRequest request,@Context final HttpServletResponse response,
            final RemoteUrlForm form) {
        try {
            this.checkEndpointAccess(request, response);
            if(!UtilMethods.isSet(form.remoteUrl)){
                throw new BadRequestException("No Url passed");
            }
            if (!tempApi.validUrl(form.remoteUrl)) {
                throw new BadRequestException("Invalid url attempted for tempFile : " + form.remoteUrl);
            }

            final List<DotTempFile> tempFiles = new ArrayList<>();
            tempFiles.add(tempApi
                    .createTempFileFromUrl(form.fileName, request, new URL(form.remoteUrl),
                            form.urlTimeoutSeconds, form.maxFileLength));

            return Response.ok(ImmutableMap.of("tempFiles", tempFiles)).build();
        } catch (final Exception e) {
            Logger.warnAndDebug(this.getClass(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * Utility method that checks that this REST Endpoint can be safely accessed under given circumstances. For
     * instance:
     * <ul>
     *     <li>The Temp File Resources is enabled.</li>
     *     <li>Whether Anonymous Users can submit Temporary Files or not.</li>
     *     <li>The origin or referer must be valid for the current HTTP Request.</li>
     * </ul>
     *
     * @param request       The current instance of the {@link HttpServletRequest}.
     * @param response      The current instance of the {@link HttpServletResponse}.
     */
    private void checkEndpointAccess(final HttpServletRequest request, final HttpServletResponse response) {
        this.checkEndpointAccess(request, response, true);
    }

    /**
     * Utility method that checks that this REST Endpoint can be safely accessed under given circumstances. For
     * instance:
     * <ul>
     *     <li>The Temp File Resources is enabled.</li>
     *     <li>The origin or referer must be valid for the current HTTP Request.</li>
     * </ul>
     * You can explicitly restrict Temp File API access for Anonymous Users as well.
     *
     * @param request              The current instance of the {@link HttpServletRequest}.
     * @param response             The current instance of the {@link HttpServletResponse}.
     * @param allowAnonymousAccess If Anonymous Users are NOT supposed to access a method in this REST Endpoint, set
     *                             this to {@code false}. Otherwise, this method will access the current dotCMS
     *                             configuration to determine if Anonymous Users are able to call a given endpoint
     *                             action or not -- see {@link TempFileAPI#TEMP_RESOURCE_ALLOW_ANONYMOUS}.
     */
    private void checkEndpointAccess(final HttpServletRequest request, final HttpServletResponse response,
                                     final boolean allowAnonymousAccess) {
        if (!Config.getBooleanProperty(TempFileAPI.TEMP_RESOURCE_ENABLED, true)) {
            final String message = "Temp Files Resource is not enabled, please change the TEMP_RESOURCE_ENABLED to " +
                                           "true in your properties file";
            Logger.error(this, message);
            throw new DoesNotExistException(message);
        }
        final boolean allowAnonToUseTempFiles =
                allowAnonymousAccess && Config.getBooleanProperty(TempFileAPI.TEMP_RESOURCE_ALLOW_ANONYMOUS, true);
        new WebResource.InitBuilder(request, response).requiredAnonAccess(AnonymousAccess.WRITE).rejectWhenNoUser(!allowAnonToUseTempFiles).init();
        if (!new SecurityUtils().validateReferer(request)) {
            throw new BadRequestException("Invalid Origin or referer");
        }
    }

    /**
     * Creates a Completion Service with the specified configuration parameters.
     *
     * @param poolSize      The initial size of the thread pool.
     * @param maxPoolSize   The maximum number of threads in the pool.
     * @param queueCapacity The maximum capacity of the queue that will be processed.
     *
     * @return The {@link CompletionService} instance.
     */
    private CompletionService<Object> createCompletionService(final int poolSize, final int maxPoolSize, final int queueCapacity) {
        final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance().getSubmitter("TEMP_API_SUBMITTER",
                new DotConcurrentFactory.SubmitterConfigBuilder().poolSize(poolSize).maxPoolSize(maxPoolSize).queueCapacity(queueCapacity).build());
        return new ExecutorCompletionService<>(dotSubmitter);
    }

}
