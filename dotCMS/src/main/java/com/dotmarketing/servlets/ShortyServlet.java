package com.dotmarketing.servlets;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageRequestModeUtil;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

public class ShortyServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  final static String NOT_FOUND = "fileAsset";


  public final static String SHORTY_SERVLET_FORWARD_PATH = "shorty.servlet.forward.path";

  private static final String JPEG_DEFAULT_COMPRESSION_QUALITY = "jpeg.compression.quality";
  private static int jpegDefaultCompression = 85;

  public void init(ServletConfig config)
          throws ServletException {

    super.init(config);
    // Evaluate the default compression quality configured
    String jpegDefaultCompressionStr = config.getInitParameter(JPEG_DEFAULT_COMPRESSION_QUALITY);

    if (jpegDefaultCompressionStr != null) {
      try {
        jpegDefaultCompression = Integer.parseInt(jpegDefaultCompressionStr);
        Logger.info(this, "Default JPEG compression set to " + Integer.toString(jpegDefaultCompression) + " from servlet init-param");
      } catch (NumberFormatException nfe) {
        Logger.info(this, "Default JPEG compression set to " + Integer.toString(jpegDefaultCompression) + " due to misconfiguration in the init-param");
      }
    }
    Logger.info(this, "Default JPEG compression set to " + Integer.toString(jpegDefaultCompression));
  }

  protected void service(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    try {
      _serve(request, response);
    } catch (Throwable t) {
      throw new ServletException(t);
    } finally {
      DbConnectionFactory.closeSilently();
    }
  }
  public final String SHORTY_SERVLET_FINAL_PATH = "shorty.servlet.final.path";

  private final static Pattern dimw = Pattern.compile("/\\d+[w]");
  private final static Pattern dimh = Pattern.compile("/\\d+[h]");
  private void _serve(final HttpServletRequest request, final HttpServletResponse response) throws Exception {

    Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);


    HttpSession session = request.getSession(false);
    boolean ADMIN_MODE = PageRequestModeUtil.isAdminMode(session);
    boolean PREVIEW_MODE = PageRequestModeUtil.isPreviewMode(session);
    boolean EDIT_MODE = PageRequestModeUtil.isEditMode(session);


    // Checking if host is active
    if (!ADMIN_MODE && !APILocator.getVersionableAPI().hasLiveVersion(host)) {
      response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
              LanguageUtil.get("server-unavailable-error-message"));
      return;
    }

    String uri = request.getRequestURI();


    StringTokenizer tokens = new StringTokenizer(uri, "/");
    if (tokens.countTokens() < 2) {
      response.sendError(404);
      return;
    }

    tokens.nextToken();
    String id = tokens.nextToken();
    String fieldName = tokens.hasMoreTokens() ? tokens.nextToken() : NOT_FOUND;
    uri=uri.toLowerCase();

    int w = 0;
    int h = 0;
    try{
      Matcher m = dimw.matcher(uri);
      w = m.find() ? Integer.parseInt(m.group().substring(1).replace("w", "")) : w;
      m = dimh.matcher(uri);
      h = m.find() ? Integer.parseInt(m.group().substring(1).replace("h", "")) : h;
    }
    catch(Exception e){
      Logger.debug(this, e.getMessage());
      // let this one die''
    }

    boolean jpeg = uri.contains("jpeg");
    boolean jpegp = jpeg && uri.contains("jpegp");
    boolean isImage = jpeg || w+h>0;






    Optional<ShortyId> shortOpt = APILocator.getShortyAPI().getShorty(id);
    User user = WebAPILocator.getUserWebAPI().getLoggedInFrontendUser(request);
    boolean live = (user != null) ? (EDIT_MODE || PREVIEW_MODE) ? false : true : true;
    if (!live) {
      response.setHeader("Pragma", "no-cache");
      response.setHeader("Cache-Control", "no-cache");
      response.setDateHeader("Expires", 0);
    }

    if (!shortOpt.isPresent()) {
      response.sendError(404);
      return;
    }
    ShortyId shorty = shortOpt.get();
    String path = (isImage) ? "/contentAsset/image" : "/contentAsset/raw-data";


    if (shorty.type == ShortType.IDENTIFIER) {
      Contentlet con = APILocator.getContentletAPI().findContentletByIdentifier(shorty.longId, false, -1,
              APILocator.getUserAPI().getSystemUser(), false);

      String field = resolveField(con, fieldName);

      path += "/" + shorty.longId + "/" + field;

    } else {
      Contentlet con =
              APILocator.getContentletAPI().find(shorty.longId, APILocator.getUserAPI().getSystemUser(), false);

      String field = resolveField(con, fieldName);

      path += "/" + shorty.longId + "/" + field + "/byInode/true";
    }

    if(isImage){
      path += "/filter/";
      path += (w+h > 0) ? "Resize," : "";
      path += (jpeg) ? "Jpeg/jpeg_q/" + jpegDefaultCompression : "";
      path += (jpeg && jpegp) ? "/jpeg_p/1" : "";
      path += (w > 0) ? "/resize_w/" + w : "";
      path += (h > 0) ? "/resize_h/" + h : "";
    }

    request.setAttribute(SHORTY_SERVLET_FORWARD_PATH, path);



    RequestDispatcher dispatch = request.getRequestDispatcher(path);
    if(dispatch!=null){
      dispatch.forward(request, response);
    }
  }



  private String resolveField(Contentlet con, final String tryField) {
    if (!NOT_FOUND.equals(tryField)) {
      Object obj = con.getMap().get(tryField);
      if (obj instanceof File) {
        return tryField;
      }

    }


    for (Field f : FieldsCache.getFieldsByStructureInode(con.getStructureInode())) {
      if ("binary".equals(f.getFieldType())) {
        return f.getVelocityVarName();
      }
    }

    return NOT_FOUND;
  }

}