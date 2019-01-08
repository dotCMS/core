package com.dotmarketing.servlets;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.language.LanguageUtil;

public class ShortyServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  final static String FILE_ASSEST_DEFAULT = FileAssetAPI.BINARY_FIELD;

  
  public final static String SHORTY_SERVLET_FORWARD_PATH = "shorty.servlet.forward.path";
  
  
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


    PageMode mode = PageMode.get(request);



    // Checking if host is active
    if (!mode.isAdmin && !APILocator.getVersionableAPI().hasLiveVersion(host)) {
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
    String fieldName = tokens.hasMoreTokens() ? tokens.nextToken() : FILE_ASSEST_DEFAULT;
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
    boolean live = mode.showLive;
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

    Contentlet con = (shorty.type == ShortType.IDENTIFIER)
                ? APILocator.getContentletAPI().findContentletByIdentifier(shorty.longId, false, -1,
                        APILocator.systemUser(), false)
                : APILocator.getContentletAPI().find(shorty.longId, APILocator.systemUser(), false);




    path += inodePath(con, fieldName, live) +  "/byInode/true";


    if(isImage){
      path += "/filter/";
      path += (w+h > 0) ? "Resize," : "";
      path += (jpeg) ? "Jpeg/jpeg_q/75" : "";
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



    private final String inodePath(final Contentlet con, final String tryField, final boolean live) throws DotStateException, DotDataException {

        final Optional<Field> fieldOpt = resolveField(con, tryField);

        if (!fieldOpt.isPresent()) {
            return "/" + con.getInode() + "/" + FILE_ASSEST_DEFAULT ;
        }
        
        final Field field = fieldOpt.get();
        if (field instanceof ImageField || field instanceof FileField) {
            String id = con.getStringProperty(field.variable());
            ContentletVersionInfo cvi = APILocator.getVersionableAPI().getContentletVersionInfo(id, con.getLanguageId());
            String inode = (live) ? cvi.getLiveInode() : cvi.getWorkingInode();
            return "/" + inode + "/" + FILE_ASSEST_DEFAULT ;
        } else {
            return "/" + con.getInode() + "/" + field.variable() ;
        }

    }


    private final Optional<Field> resolveField(final Contentlet con, final String tryField) {

        
        return Contentlet.TITLE_IMAGE_KEY.equals(tryField)
            ? con.getTitleImage() 
            : con.getContentType().fieldMap().containsKey(tryField)  
                ? Optional.of(con.getContentType().fieldMap().get(tryField))
                : con.getContentType().fields().stream().filter(f -> (f instanceof BinaryField || f instanceof ImageField || f instanceof FileField) ).findFirst();
        
    }
    
}
