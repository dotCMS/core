package com.dotcms.rest.api.v1.temp;

import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.exception.DoesNotExistException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.server.JSONP;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;

import io.vavr.control.Try;

@Path("/v1/temp")
public class TempFileResource {

    private final WebResource webResource;
    private final TempFileAPI tempApi;

    /**
     * Default constructor.
     */
    public TempFileResource() {
        this(new WebResource(), APILocator.getTempFileAPI());
    }

    @VisibleForTesting
    TempFileResource(final WebResource webResource, final TempFileAPI tempApi) {
        this.webResource = webResource;
        this.tempApi = tempApi;
    }

    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response uploadTempResourceMulti(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response, final FormDataMultiPart body) {

        try {
            verifyTempResourceEnabled();

            final boolean allowAnonToUseTempFiles = !Config
                    .getBooleanProperty(TempFileAPI.TEMP_RESOURCE_ALLOW_ANONYMOUS, true);

            this.webResource
                    .init(false, request, allowAnonToUseTempFiles);



            if (!new SecurityUtils().validateReferer(request)) {
                throw new BadRequestException("Invalid Origin or referer");
            }

            final List<DotTempFile> tempFiles = new ArrayList<DotTempFile>();

            for (final BodyPart part : body.getBodyParts()) {

                InputStream in = (part.getEntity() instanceof InputStream) ? InputStream.class
                        .cast(part.getEntity())
                        : Try.of(() -> part.getEntityAs(InputStream.class)).getOrNull();

                if (in == null) {
                    continue;
                }
                final ContentDisposition meta = part.getContentDisposition();
                if (meta == null) {
                    continue;
                }
                final String fileName = meta.getFileName();
                if (fileName == null || fileName.startsWith(".") || fileName.contains("/.")) {
                    continue;
                }
                tempFiles.add(tempApi.createTempFile(fileName, request, in));
            }

            return Response.ok(ImmutableMap.of("tempFiles", tempFiles)).build();

        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

    }

    @POST
    @Path("/byUrl")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON})
    public final Response copyTempFromUrl(@Context final HttpServletRequest request,
            final RemoteUrlForm form) {

        try {

            verifyTempResourceEnabled();

            final boolean allowAnonToUseTempFiles = !Config
                    .getBooleanProperty(TempFileAPI.TEMP_RESOURCE_ALLOW_ANONYMOUS, true);
            final InitDataObject initDataObject = this.webResource
                    .init(false, request, allowAnonToUseTempFiles);

            final User user = initDataObject.getUser();
            final String uniqueKey = request.getSession().getId();

            if (!new SecurityUtils().validateReferer(request)) {
                throw new BadRequestException("Invalid Origin or referer");
            }

            final List<DotTempFile> tempFiles = new ArrayList<DotTempFile>();
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
