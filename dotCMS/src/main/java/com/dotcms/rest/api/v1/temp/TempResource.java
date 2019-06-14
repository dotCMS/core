package com.dotcms.rest.api.v1.temp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.server.JSONP;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

@Path("/v1/temp")
public class TempResource {

  private final WebResource webResource;
  private final TempResourceAPI tempApi;

  /**
   * Default constructor.
   */
  public TempResource() {
    this(new WebResource(), new TempResourceAPI());
  }

  @VisibleForTesting
  TempResource(final WebResource webResource, TempResourceAPI tempApi) {
    this.webResource = webResource;
    this.tempApi = tempApi;
  }

  @PUT
  @Path("/upload")
  @JSONP
  @NoCache
  @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public final Response uploadTempResource(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
      @QueryParam("fieldVar") final String fieldVar, @FormDataParam("file") InputStream fileInputStream,
      @FormDataParam("file") FormDataContentDisposition fileMetaData) {

    return uploadTempResourceImpl(request, response, fieldVar, fileInputStream, fileMetaData);
  }

  
  protected final Response uploadTempResourceImpl(final HttpServletRequest request, final HttpServletResponse response,
      final String fieldVar, final InputStream inputStream, final FormDataContentDisposition fileMetaData) {

    final InitDataObject initDataObject = this.webResource.init(false, request, response, false);

    if (!new SecurityUtils().validateReferer(request)) {
      throw new WebApplicationException("Invalid Origin or referer");
    }

    if (!UtilMethods.isSet(fieldVar)) {
      return ResponseUtil.mapExceptionResponse(new WebApplicationException("Invalid fieldVar passed in"));
    }

    final String tempFileUri =
        File.separator + "tmp" + UUIDGenerator.generateUuid() + File.separator + fieldVar + File.separator + fileMetaData.getFileName();
    final File tempFile = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + tempFileUri);
    final File tempFolder = tempFile.getParentFile();

    if (!tempFolder.mkdirs()) {
      return ResponseUtil.mapExceptionResponse(new WebApplicationException("Error while creating temp directory"));
    }

    String absFilePath = FileUtil.getAbsolutlePath(tempFile.getPath());
    String absTmpPath =FileUtil.getAbsolutlePath(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary());
    if (!absFilePath.startsWith(absTmpPath)) {
      SecurityLogger.logInfo(this.getClass(),
          () -> "Attempted file upload outside of temp folder: " + absFilePath + " from " + request.getRemoteAddr());
      return ResponseUtil.mapExceptionResponse(new DotSecurityException("Invalid file upload"));
    }

    try (final OutputStream out = new FileOutputStream(tempFile)) {
      int read = 0;
      byte[] bytes = new byte[4096];
      while ((read = inputStream.read(bytes)) != -1) {
        out.write(bytes, 0, read);
      }
      tempApi.createWhoCanUseTempFile(request, tempFolder);

      return Response.ok(ImmutableMap.of("tempFile", tempFileUri, "tempFileSize",fileMetaData.getSize(), "tempFileIsImage", UtilMethods.isImage(tempFileUri), "tempFileName",fileMetaData.getFileName() )).build();
    } catch (Exception e) {

      Logger.error(this.getClass(), "unable to save temp file:" + tempFileUri, e);

      return ResponseUtil.mapExceptionResponse(e);
    }
  }

  @PUT
  @Path("/copy")
  @JSONP
  @NoCache
  @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  public final Response copyTempFromUrl(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
      @QueryParam("fieldVar") final String fieldVar, @FormDataParam("file") InputStream fileInputStream,
      @FormDataParam("file") FormDataContentDisposition fileMetaData) {

    final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
    return Response.ok("Cool Tools!").build();

  }

}
