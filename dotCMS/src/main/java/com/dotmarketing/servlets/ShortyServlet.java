package com.dotmarketing.servlets;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Resolves a shorty or long id, image or assets.
 * if the path has an jpeg or jpegp would be taken as a image and can resize
 */
public class ShortyServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  private final HostWebAPI     hostWebAPI     = WebAPILocator.getHostWebAPI();
  private final VersionableAPI versionableAPI = APILocator.getVersionableAPI();
  private final ShortyIdAPI    shortyIdAPI    = APILocator.getShortyAPI();


  private static final String  JPEG                        = "jpeg";
  private static final String  JPEGP                       = "jpegp";
  private static final String  WEBP                       = "webp";
  private static final String  FILE_ASSET_DEFAULT          = FileAssetAPI.BINARY_FIELD;
  public  static final String  SHORTY_SERVLET_FORWARD_PATH = "shorty.servlet.forward.path";
  private static final Pattern widthPattern                = Pattern.compile("/\\d+[w]");
  private static final Pattern heightPattern               = Pattern.compile("/\\d+[h]");
  private static final Pattern qualityPattern               = Pattern.compile("/\\d+[q]");
  
  @CloseDBIfOpened
  protected void service(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    try {

        serve(request, response);
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }


  private int getWidth(final String uri, final int defaultWidth) {

    int width = 0;

    try {
      final Matcher widthMatcher = widthPattern.matcher(uri);
      width = widthMatcher.find()?
              Integer.parseInt(widthMatcher.group().substring(1).replace("w", StringPool.BLANK)):
              defaultWidth;
    } catch(Exception e){
      Logger.debug(this, e.getMessage());
    }

    return width;
  }

  private int getHeight (final String uri, final int defaultHeight) {

    int height = 0;

    try {
      final Matcher heightMatcher = heightPattern.matcher(uri);
      height = heightMatcher.find() ?
              Integer.parseInt(heightMatcher.group().substring(1).replace("h", StringPool.BLANK)) :
              defaultHeight;
    } catch(Exception e){
      Logger.debug(this, e.getMessage());
    }

    return height;
  }
  
  private int getQuality (final String uri, final int defaultQuality) {

    int quality = 0;

    try {
      final Matcher qualityMatcher = qualityPattern.matcher(uri);
      quality = qualityMatcher.find() ?
              Integer.parseInt(qualityMatcher.group().substring(1).replace("q", StringPool.BLANK)) :
                defaultQuality;
    } catch(Exception e){
      Logger.debug(this, e.getMessage());
    }
    quality= (quality<0) ? 0 : (quality>100) ? 100 : quality;
    return quality;
  }
  
  private void serve(final HttpServletRequest request,
                     final HttpServletResponse response) throws Exception {


    final PageMode mode = PageMode.get(request);

    if (!this.isValidRequest(request, response, mode)) {
      return;
    }

    final String uri    = request.getRequestURI();
    final StringTokenizer tokens = new StringTokenizer(uri, StringPool.FORWARD_SLASH);

    if (tokens.countTokens() < 2) {
      response.sendError(404);
      return;
    }

    tokens.nextToken();
    final String inodeOrIdentifier    = tokens.nextToken();
    final String fieldName            = tokens.hasMoreTokens() ? tokens.nextToken() : FILE_ASSET_DEFAULT;
    final String lowerUri             = uri.toLowerCase();
    final boolean live                = mode.showLive;
    final Optional<ShortyId> shortOpt = this.shortyIdAPI.getShorty(inodeOrIdentifier);

    this.addHeaders(response, live);
    if (!shortOpt.isPresent()) {
      response.sendError(404);
      return;
    }

    this.doForward(request, response, fieldName, lowerUri, live, shortOpt);
  }

  private void doForward(final HttpServletRequest request,
                         final HttpServletResponse response,
                         final String fieldName,
                         final String lowerUri,
                         final boolean live,
                         final Optional<ShortyId> shortOpt)
          throws DotDataException, DotSecurityException, ServletException, IOException {

    final int      width   = this.getWidth(lowerUri, 0);
    final int      height  = this.getHeight(lowerUri, 0);
    final int      quality  = this.getQuality(lowerUri, 0);
    final boolean  jpeg    = lowerUri.contains(JPEG);
    final boolean  jpegp   = jpeg && lowerUri.contains(JPEGP);
    final boolean  webp    = lowerUri.contains(WEBP);
    final boolean  isImage = webp || jpeg || width+height > 0 || quality>0;
    final ShortyId shorty  = shortOpt.get();
    final String   path    = isImage? "/contentAsset/image" : "/contentAsset/raw-data";
    final User systemUser  = APILocator.systemUser();
    final Language language =WebAPILocator.getLanguageWebAPI().getLanguage(request);
    try {

        
        String id = null;
        if(shorty.type!= ShortType.TEMP_FILE) {
          final Optional<Contentlet> conOpt = (shorty.type == ShortType.IDENTIFIER)
                      ? APILocator.getContentletAPI().findContentletByIdentifierOrFallback(shorty.longId, live, language.getId(), APILocator.systemUser(), false)
                      : Optional.ofNullable(APILocator.getContentletAPI().find(shorty.longId, systemUser, false));
                      
          if(!conOpt.isPresent()) {
              response.sendError(HttpServletResponse.SC_NOT_FOUND);
              return;
          }
          id=this.inodePath(conOpt.get(), fieldName, live);
        }else {
          id="/" + shorty.longId + "/temp";
        }
        
        

      final StringBuilder pathBuilder = new StringBuilder(path)
              .append(id).append("/byInode/true");

      this.addImagePath(width, height, quality, jpeg, jpegp,webp, isImage, pathBuilder);
      this.doForward(request, response, pathBuilder.toString());
    } catch (DotContentletStateException e) {

      Logger.error(this, e.getMessage(), e);
      response.sendError(404);
    }
  }



  private void doForward(final HttpServletRequest request,
                         final HttpServletResponse response,
                         final String path) throws ServletException, IOException {

      request.setAttribute(SHORTY_SERVLET_FORWARD_PATH, path);

      final RequestDispatcher dispatch = request.getRequestDispatcher(path);
      if(dispatch!=null) {

          dispatch.forward(request, response);
      }
  }

  private void addImagePath(final int weight,
                            final int height,
                            final int quality,
                            final boolean jpeg,
                            final boolean jpegp,
                            final boolean webp,
                            final boolean isImage,
                            final StringBuilder pathBuilder) {
      if(isImage) {

          pathBuilder.append("/filter/");
          pathBuilder.append(weight+height > 0? "Resize,"      : StringPool.BLANK);
          if(quality>0) {
            pathBuilder.append("Quality/quality_q/" + quality);
          }else {
            pathBuilder.append(jpeg ? "Jpeg/jpeg_q/75"            : StringPool.BLANK);
            pathBuilder.append(webp ? "WebP/webp_q/75"        : StringPool.BLANK);
            pathBuilder.append(jpeg && jpegp ? "/jpeg_p/1"        : StringPool.BLANK);
          }
          pathBuilder.append(weight > 0? "/resize_w/" + weight : StringPool.BLANK);
          pathBuilder.append(height > 0? "/resize_h/" + height : StringPool.BLANK);
      }
  }

  private void addHeaders(final HttpServletResponse response, final boolean live) {

    if (!live) {
      response.setHeader("Pragma", "no-cache");
      response.setHeader("Cache-Control", "no-cache");
      response.setDateHeader("Expires", 0);
    }
  }

  private boolean isValidRequest(final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 final PageMode mode) throws DotDataException, DotSecurityException, PortalException, SystemException, IOException {

    final Host host     = this.hostWebAPI.getCurrentHost(request);
    // Checking if host is active
    if (!mode.isAdmin && !this.versionableAPI.hasLiveVersion(host)) {

      response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
          LanguageUtil.get("server-unavailable-error-message"));
      return false;
    }

    return true;
  }


  protected final String inodePath(final Contentlet contentlet,
                                   final String tryField,
                                   final boolean live)
            throws DotStateException, DotDataException {

        final Optional<Field> fieldOpt = resolveField(contentlet, tryField);

        if (!fieldOpt.isPresent()) {
            return "/" + contentlet.getInode() + "/" + FILE_ASSET_DEFAULT;
        }

        final Field field = fieldOpt.get();
        if (field instanceof ImageField || field instanceof FileField) {

            final String relatedImageId = contentlet.getStringProperty(field.variable());
            final ContentletVersionInfo contentletVersionInfo =
                    this.versionableAPI.getContentletVersionInfo(relatedImageId, contentlet.getLanguageId());

            if (contentletVersionInfo != null) {

                final String inode = live ? contentletVersionInfo.getLiveInode() : contentletVersionInfo.getWorkingInode();
                return new StringBuilder(StringPool.FORWARD_SLASH).append(inode)
                        .append(StringPool.FORWARD_SLASH).append(FILE_ASSET_DEFAULT).toString();
            }
        }

        return new StringBuilder(StringPool.FORWARD_SLASH).append(contentlet.getInode())
                .append(StringPool.FORWARD_SLASH).append(field.variable()).toString();
    }


    protected final Optional<Field> resolveField(final Contentlet contentlet, final String tryField) {


        return Contentlet.TITLE_IMAGE_KEY.equals(tryField) ?
                contentlet.getTitleImage():
                contentlet.getContentType().fieldMap().containsKey(tryField) ?
                        Optional.of(contentlet.getContentType().fieldMap().get(tryField)):
                        contentlet.getContentType().fields().stream()
                                .filter(f -> (f instanceof BinaryField || f instanceof ImageField || f instanceof FileField)).findFirst();
    }
    
}
