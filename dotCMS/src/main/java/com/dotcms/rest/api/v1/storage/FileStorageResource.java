package com.dotcms.rest.api.v1.storage;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.storage.FileStorageAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Path("/v1/storage")
public class FileStorageResource {

    private final WebResource    webResource;
    private final FileStorageAPI fileStorageAPI;
    private final MultiPartUtils multiPartUtils;

    @VisibleForTesting
    protected FileStorageResource(final FileStorageAPI fileStorageAPI,
                               final WebResource webResource,
                               final MultiPartUtils multiPartUtils) {

        this.fileStorageAPI = fileStorageAPI;
        this.webResource    = webResource;
        this.multiPartUtils = multiPartUtils;
    }

    public FileStorageResource() {
        this(APILocator.getFileStorageAPI(), new WebResource(), new MultiPartUtils());
    }

    @PUT
    @Path("/metadata")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response getMetadata(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                               final FormDataMultiPart multipart) throws IOException {

        final List<File> files = this.multiPartUtils.getBinariesFromMultipart(multipart);
        final ImmutableMap.Builder<String, Object> bodyResultBuilder = new ImmutableMap.Builder<>();
        if (!UtilMethods.isSet(files)) {

            throw new BadRequestException("Must send files to generate the metadata");
        }

        for (final File file : files) {

            bodyResultBuilder.put(file.getName(), this.fileStorageAPI.generateRawBasicMetaData(file));
        }

        return Response.ok(new ResponseEntityView(bodyResultBuilder.build())).build();
    }

    @PUT
    @Path("/fullmetadata")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response getFullMetadata(@Context final HttpServletRequest request,
                                          @Context final HttpServletResponse response,
                                                   final FormDataMultiPart multipart) throws IOException {

        final List<File> files = this.multiPartUtils.getBinariesFromMultipart(multipart);
        final ImmutableMap.Builder<String, Object> bodyResultBuilder = new ImmutableMap.Builder<>();
        if (!UtilMethods.isSet(files)) {

            throw new BadRequestException("Must send files to generate the metadata");
        }

        for (final File file : files) {

            bodyResultBuilder.put(file.getName(), this.fileStorageAPI.generateRawFullMetaData(file, FileStorageAPI.configuredMaxLength()));
        }

        return Response.ok(new ResponseEntityView(bodyResultBuilder.build())).build();
    }

}
