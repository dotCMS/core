package com.dotcms.rest.api.v1.storage;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.storage.ContentletMetadataAPI;
import com.dotcms.storage.FileStorageAPI;
import com.dotcms.storage.StorageType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.language.LanguageUtil;
import io.vavr.Tuple2;
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
import java.util.Collections;
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

    private final WebResource    webResource;
    private final FileStorageAPI fileStorageAPI;
    private final MultiPartUtils multiPartUtils;
    private final ContentStorageHelper  contentStorageHelper;
    private final ContentletMetadataAPI contentletMetadataAPI;

    @VisibleForTesting
    protected ContentStorageResource(final FileStorageAPI fileStorageAPI,
                                     final WebResource webResource,
                                     final MultiPartUtils multiPartUtils,
                                     final ContentStorageHelper  contentStorageHelper,
                                     final ContentletMetadataAPI contentletMetadataAPI) {

        this.fileStorageAPI = fileStorageAPI;
        this.webResource    = webResource;
        this.multiPartUtils = multiPartUtils;
        this.contentStorageHelper  = contentStorageHelper;
        this.contentletMetadataAPI = contentletMetadataAPI;
    }

    public ContentStorageResource() {
        this(APILocator.getFileStorageAPI(), new WebResource(),
                new MultiPartUtils(), new ContentStorageHelper(), APILocator.getContentletMetadataAPI());
    }

    @GET
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public final Response getFileFromStorage(@Context final HttpServletRequest request,
                                           @Context final HttpServletResponse response,
                                           @QueryParam("path") final String path) {


        final String storageType = Config.getStringProperty("DEFAULT_STORAGE_TYPE", StorageType.FILE_SYSTEM.name());

        Logger.debug(this, ()-> "Getting the file: " + path + " from the storage: " + storageType);

        final File   file        = this.fileStorageAPI.getStorageProvider().getStorage(storageType).pullFile("files", path);
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
                this.multiPartUtils.getBodyMapAndBinariesFromMultipart(multipart);
        final List<File> files            = bodyAndBinaries._2();

        final ImmutableMap.Builder<File, Object> bodyResultBuilder = new ImmutableMap.Builder<>();
        if (!UtilMethods.isSet(files)) {

            throw new BadRequestException("Must send files");
        }

        for (final File file : files) {

            final String storageType = Config.getStringProperty("DEFAULT_STORAGE_TYPE", StorageType.FILE_SYSTEM.name());
            bodyResultBuilder.put(file, this.fileStorageAPI.getStorageProvider().getStorage(storageType)
                    .pushFile("files", File.separator + file.getName(), file, Collections.emptyMap()));
        }

        return Response.ok(new ResponseEntityView(bodyResultBuilder.build())).build();
    }

    @PUT
    @Path("/metadata/content/_generate")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response generateContentletMetadata(@Context final HttpServletRequest request,
                                               @Context final HttpServletResponse response,
                                               @QueryParam("inode")                        final String inode,
                                               @QueryParam("identifier")                   final String identifier,
                                               @DefaultValue("-1") @QueryParam("language") final String language)
            throws IOException, DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource)
                                                        .requestAndResponse(request, response)
                                                        .rejectWhenNoUser(true)
                                                        .requiredBackendUser(true).init();

        if (!UtilMethods.isSet(identifier) && !UtilMethods.isSet(inode)) {

            throw new BadRequestException("Must send identifier or inode");
        }

        final long languageId = UtilMethods.isSet(language) && !"-1".equals(language)?
                LanguageUtil.getLanguageId(language):-1;

        Logger.debug(this, ()-> "Generating the metadata for: " + (null != inode? inode: identifier));

        final Contentlet contentlet = this.contentStorageHelper.getContentlet(inode, identifier, languageId,
                ()-> WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(), initDataObject, PageMode.get(request));

        return Response.ok(new ResponseEntityView(this.contentletMetadataAPI.generateContentletMetadata(contentlet))).build();
    }

    @PUT
    @Path("/metadata/content/_get")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getContentMetadata(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @QueryParam("inode")                        final String inode,
                                     @QueryParam("identifier")                   final String identifier,
                                     @DefaultValue("-1") @QueryParam("language") final String language,
                                     final MetadataForm metadataForm)
            throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredBackendUser(true).init();

        if (!UtilMethods.isSet(identifier) && !UtilMethods.isSet(inode)) {

            throw new BadRequestException("Must send identifier or inode");
        }

        if (!UtilMethods.isSet(metadataForm.getField())) {

            throw new BadRequestException("Must send field");
        }

        final long languageId = UtilMethods.isSet(language) && !"-1".equals(language)?
                LanguageUtil.getLanguageId(language):-1;

        Logger.debug(this, ()-> "Generating the metadata for: " + (null != inode? inode: identifier) + ", " + metadataForm);

        final Contentlet contentlet = this.contentStorageHelper.getContentlet(inode, identifier, languageId,
                ()-> WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(), initDataObject, PageMode.get(request));

        final Map<String, Field> fieldMap = contentlet.getContentType().fieldMap();

        if (!fieldMap.containsKey(metadataForm.getField())) {
            throw new BadRequestException("Field variable sent, is not valid for the contentlet: " + contentlet.getIdentifier());
        }

        return Response.ok(new ResponseEntityView(
                !metadataForm.isCache()?
                        this.contentletMetadataAPI.getMetadataNoCache(contentlet, metadataForm.getField()):
                        this.contentletMetadataAPI.getMetadata(contentlet, metadataForm.getField()))).build();
    }

}
