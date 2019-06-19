package com.dotcms.rest.api.v1.temp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.server.JSONP;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;

import io.vavr.Tuple2;
import io.vavr.control.Try;

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

  @POST
  @Path("/upload")
  @JSONP
  @NoCache
  @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public final Response uploadTempResource(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
      @FormDataParam("file") InputStream fileInputStream, @FormDataParam("file") FormDataContentDisposition fileMetaData) {

    return uploadTempResourceImpl(request, response, fileInputStream, fileMetaData);
  }

  protected final Response uploadTempResourceImpl(final HttpServletRequest request, final HttpServletResponse response,
      final InputStream inputStream, final FormDataContentDisposition fileMetaData) {

    final InitDataObject initDataObject = this.webResource.init(false, request, false);

    final User user = initDataObject.getUser();
    final String uniqueKey = request.getSession().getId();

    if (!new SecurityUtils().validateReferer(request)) {
      throw new WebApplicationException("Invalid Origin or referer");
    }
    final String fileName = FileUtil.sanitizeFileName(fileMetaData.getFileName());

    DotTempFile dotTempFile = Try.of(() -> tempApi.createTempFile(fileName, user, uniqueKey)).getOrElseThrow(() -> new DotRuntimeException(
        "Attempted file upload outside of temp folder: " + fileMetaData + " from " + request.getRemoteAddr()));
    final String tempFileId = dotTempFile.id;
    final File tempFile = dotTempFile.file;

    try (final OutputStream out = new FileOutputStream(tempFile)) {
      int read = 0;
      byte[] bytes = new byte[4096];
      while ((read = inputStream.read(bytes)) != -1) {
        out.write(bytes, 0, read);
      }

      return Response.ok(dotTempFile).build();
    } catch (Exception e) {

      Logger.warnAndDebug(this.getClass(), "unable to save temp file:" + tempFileId, e);

      return ResponseUtil.mapExceptionResponse(e);
    }
  }

  @POST
  @Path("/byUrl")
  @JSONP
  @NoCache
  @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON})
  public final Response copyTempFromUrl(@Context final HttpServletRequest request, final RemoteUrlForm form) {

    final InitDataObject initDataObject = this.webResource.init(false, request, false);

    final User user = initDataObject.getUser();
    final String uniqueKey = request.getSession().getId();

    String tryFileName =
        UtilMethods.isSet(form.fileName) ? form.fileName : Try.of(() -> new URL(form.remoteUrl).getPath()).getOrElse("uknown");

    tryFileName =
        tryFileName.indexOf("/") > -1 ? tryFileName.substring(tryFileName.lastIndexOf("/") + 1, tryFileName.length()) : tryFileName;

    final String fileName = FileUtil.sanitizeFileName(tryFileName);
    if (!new SecurityUtils().validateReferer(request)) {
      throw new WebApplicationException("Invalid Origin or referer");
    }
    DotTempFile dotTempFile = Try.of(() -> tempApi.createTempFile(fileName, user, uniqueKey)).getOrElseThrow(() -> new DotRuntimeException(
        "Attempted file upload outside of temp folder: " + form.fileName + " from " + request.getRemoteAddr()));
    final String tempFileId = dotTempFile.id;
    final File tempFile = dotTempFile.file;

    try (final OutputStream fileOut = new FileOutputStream(tempFile)) {
      CircuitBreakerUrl
      .builder()
      .setMethod(Method.GET)
      .setUrl(form.remoteUrl)
      .setTimeout(form.urlTimeout)
      .build().doOut(fileOut);
    } catch (Exception e) {
      Logger.warnAndDebug(this.getClass(), "unable to save temp file:" + tempFileId, e);

      return ResponseUtil.mapExceptionResponse(e);
    }
    return Response.ok(dotTempFile).build();

  }

}
