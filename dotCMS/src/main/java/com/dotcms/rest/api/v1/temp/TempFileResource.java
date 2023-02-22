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
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

@Path("/v1/temp")
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
    @Produces("application/octet-stream")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response uploadTempResourceMulti(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response, 
            @DefaultValue("-1") @QueryParam(MAX_FILE_LENGTH_PARAM) final String maxFileLengthString, // this is being used later
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

                    return this.tempApi.createTempFile(fileName, statelessRequest, in);
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

    /**
     *
     * @param request
     * @param response
     * @param tempFileId
     * @param body
     * @return
     */
    @PUT
    @Path("/id/{tempFileId: .*}")
    @JSONP
    @NoCache
    @Produces("application/octet-stream")
    @Consumes(MediaType.APPLICATION_JSON)
    public final Response updateTempResource(@Context final HttpServletRequest request,
                                             @Context final HttpServletResponse response,
                                             @PathParam("tempFileId") final String tempFileId,
                                             final PlainTextFileForm body) {
        this.verifyTempResourceEnabled();
        new WebResource.InitBuilder(request, response).requiredAnonAccess(AnonymousAccess.WRITE).rejectWhenNoUser(true).init();
        if (!new SecurityUtils().validateReferer(request)) {
            throw new BadRequestException("Invalid Origin or referer");
        }

        final StreamingOutput streamingOutput = output -> {

            final ObjectMapper objectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
            TempFileResource.this.savePlainTextFile(tempFileId, body, request, output, objectMapper);

        };

        return Response.ok(streamingOutput).header(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_JSON).build();
    }

    protected class TempFileStreamingOutput implements StreamingOutput {

        private final String tempFileId;
        private final PlainTextFileForm body;
        private final HttpServletRequest request;

        private TempFileStreamingOutput(final String tempFileId, final PlainTextFileForm body, final HttpServletRequest request) {
            this.tempFileId = tempFileId;
            this.body    = body;
            this.request = request;
        }

        @Override
        public void write(final OutputStream output) throws IOException, WebApplicationException {
            final ObjectMapper objectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
            TempFileResource.this.savePlainTextFile(tempFileId, body, request, output, objectMapper);
        }
    }

    private void savePlainTextFile(final String tempFileId, final PlainTextFileForm body, final HttpServletRequest request,
                                    final OutputStream outputStream, final ObjectMapper objectMapper) {
        final CompletionService<Object> completionService = this.createCompletionService(2, 5, 100000);
        final HttpServletRequest statelessRequest = RequestUtil.INSTANCE.createStatelessRequest(request);
        // this triggers the save
        final InputStream in = new ByteArrayInputStream(body.fileContent().getBytes());
        final Future<Object> future = this.updateTempFileFuture(completionService, statelessRequest, 1,
                body.fileName(), in, tempFileId);
        /*final Future<Object> future = completionService.submit(() -> {

            try {
                final InputStream in = new ByteArrayInputStream(body.fileContent().getBytes());
                if (in == null) {
                    return new ErrorEntity(String.valueOf(HttpServletResponse.SC_BAD_REQUEST), "Invalid Binary Part, index: " + futureIndex,
                            String.valueOf(futureIndex));
                }
                final String fileName = body.fileName();
                if (fileName == null || fileName.startsWith(".") || fileName.contains("/.")) {

                    return new ErrorEntity(String.valueOf(HttpServletResponse.SC_BAD_REQUEST),
                            "Invalid Binary Part, Name: " + fileName + ", index: " + futureIndex,
                            String.valueOf(futureIndex));
                }
                return this.tempApi.updateTempFile(tempFileId, fileName, statelessRequest, in);
            } catch (final Exception e) {
                Logger.error(this, e.getMessage(), e);
                return new ErrorEntity(String.valueOf(HttpServletResponse.SC_BAD_REQUEST),
                        "Invalid Binary Part, Message: " + e.getMessage() + ", index: " + futureIndex,
                        String.valueOf(futureIndex));
            }

        });*/
        this.printResponseEntityViewResult(outputStream, objectMapper, completionService, List.of(future));
    }

    private Future<Object> updateTempFileFuture(final CompletionService<Object> completionService,
                                                final HttpServletRequest statelessRequest, final int futureId,
                                                final String fileName, final InputStream in, final String tempFileId) {
        return completionService.submit(() -> {

            try {
                if (fileName == null || fileName.startsWith(".") || fileName.contains("/.")) {
                    return new ErrorEntity(String.valueOf(HttpServletResponse.SC_BAD_REQUEST), "Invalid File Name, " +
                                                                                                       "Name: " + fileName + ", index: " + futureId, String.valueOf(futureId));
                }
                if (in == null) {
                    return new ErrorEntity(String.valueOf(HttpServletResponse.SC_BAD_REQUEST), "Invalid Binary Stream, " +
                                                                                                       "index: " + futureId, fileName);
                }
                return this.tempApi.updateTempFile(tempFileId, fileName, statelessRequest, in);
            } catch (final Exception e) {
                Logger.error(this, e.getMessage(), e);
                return new ErrorEntity(String.valueOf(HttpServletResponse.SC_BAD_REQUEST), "Invalid Binary Part, " +
                                                                                                   "Message: " + e.getMessage() + ", index: " + futureId, String.valueOf(futureId));
            }

        });
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
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON})
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

            final List<DotTempFile> tempFiles = new ArrayList<DotTempFile>();
            tempFiles.add(tempApi
                    .createTempFileFromUrl(form.fileName, request, new URL(form.remoteUrl),
                            form.urlTimeoutSeconds, form.maxFileLength));

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

    /**
     *
     * @return
     */
    private CompletionService<Object> createCompletionService(final int poolSize, final int maxPoolSize, final int queueCapacity) {
        final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance().getSubmitter("TEMP_API_SUBMITTER",
                new DotConcurrentFactory.SubmitterConfigBuilder().poolSize(poolSize).maxPoolSize(maxPoolSize).queueCapacity(queueCapacity).build());
        return new ExecutorCompletionService<>(dotSubmitter);
    }

}
