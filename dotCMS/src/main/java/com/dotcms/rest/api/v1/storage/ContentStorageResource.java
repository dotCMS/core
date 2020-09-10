package com.dotcms.rest.api.v1.storage;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.storage.ContentletMetadata;
import com.dotcms.storage.ContentletMetadataAPI;
import com.dotcms.storage.FileStorageAPI;
import com.dotcms.storage.StoragePersistenceProvider;
import com.dotcms.storage.StorageType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import io.vavr.Tuple2;
import javax.validation.Valid;
import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Resource to expose the storage and metadata
 * @author jsanca
 */
@Path("/v1/storage")
public class ContentStorageResource {

    private final WebResource webResource;
    private final FileStorageAPI fileStorageAPI;
    private final MultiPartUtils multiPartUtils;
    private final ContentStorageHelper helper;
    private final ContentletMetadataAPI contentletMetadataAPI;
    private final LanguageWebAPI languageWebAPI;

    @VisibleForTesting
    ContentStorageResource(final FileStorageAPI fileStorageAPI,
            final WebResource webResource,
            final MultiPartUtils multiPartUtils,
            final ContentStorageHelper helper,
            final ContentletMetadataAPI contentletMetadataAPI, final LanguageWebAPI languageWebAPI) {

        this.fileStorageAPI = fileStorageAPI;
        this.webResource    = webResource;
        this.multiPartUtils = multiPartUtils;
        this.helper = helper;
        this.contentletMetadataAPI = contentletMetadataAPI;
        this.languageWebAPI = languageWebAPI;
    }

    public ContentStorageResource() {
        this(APILocator.getFileStorageAPI(), new WebResource(),
                new MultiPartUtils(), new ContentStorageHelper(),
                APILocator.getContentletMetadataAPI(), WebAPILocator.getLanguageWebAPI());
    }

    @GET
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public final Response getFileFromStorage(@Context final HttpServletRequest request,
                                           @Context final HttpServletResponse response,
                                           @QueryParam("path") final String path) {

        final File   file        = StoragePersistenceProvider.INSTANCE.get().getStorage().pullFile("files", path);
        final String fileName    = FilenameUtils.getName(path);

        return Response.ok(
                new StreamingOutput() {
                    @Override
                    public void write(OutputStream output)  {
                        try (ZipOutputStream zoutput = new ZipOutputStream(output);
                                InputStream fileInput = Files.newInputStream(file.toPath())) {

                            int   bytesRead      = 0;
                            final byte[] buffer  = new byte[512];
                            zoutput.putNextEntry(new ZipEntry(path));

                            while ((bytesRead = fileInput.read(buffer)) > 0) {

                                zoutput.write(buffer, 0, bytesRead);
                            }

                            zoutput.closeEntry();
                        } catch (Exception e) {

                            Logger.warn(this.getClass(), e.getMessage(), e);
                            throw new DotRuntimeException(e.getMessage());
                        }
                    }
                },
                "application/zip")
                .header("content-disposition", "attachment; filename=" + fileName + ".zip")
                .build();
    }

    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response addFileToStorage(@Context final HttpServletRequest request,
                                           @Context final HttpServletResponse response,
                                           final FormDataMultiPart multipart) throws IOException, JSONException {

        final Tuple2<Map<String,Object>, List<File>> bodyAndBinaries =
                multiPartUtils.getBodyMapAndBinariesFromMultipart(multipart);
        final List<File> files = bodyAndBinaries._2();
        if (!UtilMethods.isSet(files)) {
            throw new BadRequestException("No files found on the multi-part request.");
        }
        return Response.ok(new ResponseEntityView(helper.push(files))).build();
    }

    @PUT
    @Path("/metadata/content/_generate")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response generateContentletMetadata(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("inode") final String inode,
            @QueryParam("identifier") final String identifier,
            @DefaultValue("-1") @QueryParam("language") final String language)
            throws IOException, DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredBackendUser(true).init();

        if (!UtilMethods.isSet(identifier) && !UtilMethods.isSet(inode)) {

            throw new BadRequestException("Must send identifier or inode");
        }

        final long languageId = UtilMethods.isSet(language) && !"-1".equals(language) ?
                LanguageUtil.getLanguageId(language) : -1;

        Logger.debug(this,
                () -> "Generating the metadata for: " + (null != inode ? inode : identifier));

        final ContentletMetadata metadata = helper
                .generateContentletMetadata(inode, identifier, languageId,
                        () -> WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                        initDataObject.getUser(), PageMode.get(request));

        return Response.ok(new ResponseEntityView(metadata)).build();
    }

    @PUT
    @Path("/metadata/content/_get")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getContentMetadata(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("inode") final String inode,
            @QueryParam("identifier") final String identifier,
            @DefaultValue("-1") @QueryParam("language") final String language,
            @Valid final MetadataForm metadataForm)
            throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredBackendUser(true).init();

        if (!UtilMethods.isSet(identifier) && !UtilMethods.isSet(inode)) {
            throw new BadRequestException("Must send identifier or inode");
        }

        final long languageId = UtilMethods.isSet(language) && !"-1".equals(language)?
                LanguageUtil.getLanguageId(language):-1;

        final Map<String, Object> metadata = helper
                .getMetadata(inode, identifier, languageId,
                        () -> languageWebAPI.getLanguage(request).getId(), initDataObject.getUser(),
                        PageMode.get(request), metadataForm);
        return Response.ok(new ResponseEntityView(metadata)).build();
    }

}
