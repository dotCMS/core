package com.dotmarketing.servlets;

import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.TimeMachineUtil;
import com.dotcms.variant.business.web.VariantWebAPI.RenderContext;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.type.DotAssetContentType;
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
import com.dotmarketing.image.filter.ImageFilterApiImpl;
import com.dotmarketing.image.focalpoint.FocalPoint;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;


/**
 * Resolves a shorty or long id, image or assets.
 * if the path has an jpeg or jpegp would be taken as a image and can resize
 */
public class ShortyServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  private final HostWebAPI     hostWebAPI     = WebAPILocator.getHostWebAPI();
  private final VersionableAPI versionableAPI = APILocator.getVersionableAPI();
  private final ShortyIdAPI    shortyIdAPI    = APILocator.getShortyAPI();
  private final WebResource    webResource    = new WebResource();

  private static final String  JPEG                        = "/jpeg";
  private static final String  JPEGP                       = "/jpegp";
  private static final String  WEBP                        = "/webp";
  private static final String  FILE_ASSET_DEFAULT          = FileAssetAPI.BINARY_FIELD;
  public  static final String  SHORTY_SERVLET_FORWARD_PATH = "shorty.servlet.forward.path";
  private static final Pattern widthPattern                = Pattern.compile("/(\\d+)w\\b");
  private static final Pattern heightPattern               = Pattern.compile("/(\\d+)h\\b");
  private static final Pattern cropWidthPattern                = Pattern.compile("/(\\d+)cw\\b");
  private static final Pattern cropHeightPattern               = Pattern.compile("/(\\d+)ch\\b");
  
  private static final Pattern focalPointPattern               = Pattern.compile("/(\\.\\d+,\\.\\d+)fp\\b");
  
  private static final Pattern qualityPattern               = Pattern.compile("/(\\d+)q\\b");
  
  private static final Pattern resampleOptsPattern               = Pattern.compile("/(\\d+)ro\\b");
  
  private static final Pattern maxWidthPattern                = Pattern.compile("/(\\d+)maxw\\b");
  private static final Pattern maxHeightPattern               = Pattern.compile("/(\\d+)maxh\\b");
  private static final Pattern minWidthPattern                = Pattern.compile("/(\\d+)minw\\b");
  private static final Pattern minHeightPattern               = Pattern.compile("/(\\d+)minh\\b");
  
  
  
  @CloseDBIfOpened
  protected void service(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    try {

        serve(request, response);
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  
    private Optional<String> getParameter(final String uri, final String paramName) {

        int start  = uri.indexOf("/" + paramName + "/") + paramName.length() + 2;
        int end  = uri.indexOf("/", start) >-1 ? uri.indexOf("/", start) : uri.length();
        
        
        return Optional.ofNullable(uri.indexOf("/" + paramName + "/") > 0 
                        ? uri.substring(start,end) 
                                        : null);

    }
  
  

  protected int getWidth(final String uri, final int defaultWidth) {

    int width = 0;

    try {
      final Matcher widthMatcher = widthPattern.matcher(uri);
      width = widthMatcher.find()?
              Integer.parseInt(widthMatcher.group().substring(1).replace("w", StringPool.BLANK)):
              defaultWidth;
      if(width>0) {
          return width;
      }
      width = this.getParameter(uri, "resize_w").isPresent() ? Integer.parseInt(this.getParameter(uri, "resize_w").get()) :0;  
              
    } catch(Exception e){
      Logger.debug(this, e.getMessage());
    }

    return width;
  }

    protected int cropWidth(final String uri) {

        int cropWidth=0;
        try {
            final Matcher widthMatcher = cropWidthPattern.matcher(uri);
            cropWidth= widthMatcher.find() ? Integer.parseInt(widthMatcher.group(1)) : 0;
            if(cropWidth==0) {
                cropWidth = this.getParameter(uri, "crop_w").isPresent() ? Integer.parseInt(this.getParameter(uri, "crop_w").get()) :0;
            }
           

        } catch (Exception e) {
            Logger.debug(this, e.getMessage());
        }

        return cropWidth;
    }
  
    protected int cropHeight(final String uri) {
        int cropHeight=0;
        try {
            final Matcher heightMatcher = cropHeightPattern.matcher(uri);
            cropHeight= heightMatcher.find() ? Integer.parseInt(heightMatcher.group(1)) : 0;
            if(cropHeight>0) {
                return cropHeight;
            }
            cropHeight = this.getParameter(uri, "crop_h").isPresent() ? Integer.parseInt(this.getParameter(uri, "crop_h").get()) :0;

            
        } catch (Exception e) {
            Logger.debug(this, e.getMessage());
        }

        return cropHeight;
    }

    private Optional<FocalPoint> getFocalPoint(final String uri) {

        final Matcher focalPointMatcher = focalPointPattern.matcher(uri);
        Optional<FocalPoint> focalPoint =  focalPointMatcher.find() ? Optional.of(new FocalPoint(focalPointMatcher.group(1))) : Optional.empty();
        if(focalPoint.isEmpty()) {
            focalPoint =  this.getParameter(uri, "fp").isPresent() ? Optional.of(new FocalPoint(this.getParameter(uri, "fp").get())) : Optional.empty();
        }
        return focalPoint;
    }
  
  
  protected int getHeight (final String uri, final int defaultHeight) {

    int height = 0;

    try {
      final Matcher heightMatcher = heightPattern.matcher(uri);
      height = heightMatcher.find() ?
              Integer.parseInt(heightMatcher.group().substring(1).replace("h", StringPool.BLANK)) :
              defaultHeight;
              
      if(height==0) {
          height = this.getParameter(uri, "resize_h").isPresent() ? Integer.parseInt(this.getParameter(uri, "resize_h").get()) :0;
      }
    

    } catch(Exception e){
      Logger.debug(this, e.getMessage());
    }

    return height;
  }
  
  protected int getMaxHeight(final String uri) {
      final Matcher matcher = maxHeightPattern.matcher(uri);
      return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
  }
  
  protected int getMaxWidth(final String uri) {
      final Matcher matcher = maxWidthPattern.matcher(uri);
      return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
  }
  
  protected int getMinHeight(final String uri) {
      final Matcher matcher = minHeightPattern.matcher(uri);
      return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
  }
  
  protected int getMinWidth(final String uri) {
      final Matcher matcher = minWidthPattern.matcher(uri);
      return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
  }
  
  protected int getQuality (final String uri, final int defaultQuality) {

    int quality = 0;

    try {
      final Matcher qualityMatcher = qualityPattern.matcher(uri);
      quality = qualityMatcher.find() ?
              Integer.parseInt(qualityMatcher.group(1)) :
                defaultQuality;
              
      if(quality==0) {
          quality = this.getParameter(uri, "quality_q").isPresent() ? Integer.parseInt(this.getParameter(uri, "quality_q").get()) :0;
      }
    

              
    } catch(Exception e){
      Logger.debug(this, e.getMessage());
    }
    quality= (quality<0) ? 0 : (quality>100) ? 100 : quality;
    return quality;
  }
  
  
  private int getResampleOpt(final String uri) {

      final Matcher resampleOptMatcher = resampleOptsPattern.matcher(uri);

      
      
      return Try.of(() -> resampleOptMatcher.find() ?
                      Integer.parseInt(resampleOptMatcher.group(1)) :
                          ImageFilterApiImpl.DEFAULT_RESAMPLE_OPT).getOrElse(ImageFilterApiImpl.DEFAULT_RESAMPLE_OPT);

  }
  
  
  
  
  
  private void serve(final HttpServletRequest request,
                     final HttpServletResponse response) throws Exception {

    // Check for dotsass=true query parameter to enable SASS compilation
    final String dotsassParam = request.getParameter("dotsass");
    final boolean isDotsassParam = dotsassParam != null && dotsassParam.equalsIgnoreCase("true");
    
    if (isDotsassParam) {
        // Forward to CSSPreProcessServlet for SASS compilation
        Logger.debug(this, "ShortyServlet forwarding request with dotsass=true parameter to CSSPreProcessServlet: " + request.getRequestURI());
        
        // Create a new request attribute to pass the original URI to the CSSPreProcessServlet
        request.setAttribute("originalShortyURI", request.getRequestURI());
        
        // Forward to the CSSPreProcessServlet using the /DOTSASS path
        // We're just using /DOTSASS as a mapping to reach the servlet, not as a real file path
        RequestDispatcher dispatcher = request.getRequestDispatcher("/DOTSASS");
        dispatcher.forward(request, response);
        return;
    }
    
    try {
        final User user = ServletUtils.getUserAndAuthenticateIfRequired(
                this.webResource, request, response);
        Logger.debug(ShortyServlet.class, () -> "User: " + user);
    } catch (SecurityException e) {
        SecurityLogger.logInfo(ShortyServlet.class, e.getMessage());
        Logger.debug(ShortyServlet.class, e,  () -> "Error getting user and authenticating");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return;
    }

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
    final String fieldName            = tokens.hasMoreTokens() ? tokens.nextToken() : null;
    final String lowerUri             = uri.toLowerCase();
    final boolean live                = mode.showLive;
    final Optional<ShortyId> shortOpt = this.shortyIdAPI.getShorty(inodeOrIdentifier);

    this.addHeaders(response, live);
    if (shortOpt.isEmpty()) {
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
    final int      maxWidth   = this.getMaxWidth(lowerUri);
    final int      maxHeight  = this.getMaxHeight(lowerUri);
    final int      minWidth   = this.getMinWidth(lowerUri);
    final int      minHeight  = this.getMinHeight(lowerUri);
    
    final int      quality  = this.getQuality(lowerUri, 0);
    final int      resampleOpt = this.getResampleOpt(lowerUri);
    Optional<FocalPoint> focalPoint = Optional.empty();
    final int      cropWidth  = this.cropWidth(lowerUri);
    final int      cropHeight  = this.cropHeight(lowerUri);
    final boolean  jpeg    = lowerUri.contains(JPEG);
    final boolean  jpegp   = jpeg && lowerUri.contains(JPEGP);
    final boolean  webp    = lowerUri.contains(WEBP);
    final boolean  isImage = webp || jpeg || width+height+maxWidth+maxHeight +minHeight+minWidth> 0 || quality>0 || cropHeight>0 || cropWidth>0;
    final ShortyId shorty  = shortOpt.get();
    final String   path    = isImage? "/contentAsset/image" : "/contentAsset/raw-data";
    final User systemUser  = APILocator.systemUser();
    final Language language =WebAPILocator.getLanguageWebAPI().getLanguage(request);
    try {


        String inodePath = null;
        if(shorty.type!= ShortType.TEMP_FILE) {
          final Optional<Contentlet> conOpt = (shorty.type == ShortType.IDENTIFIER)
                      ? getContentletByIdentifier(live, shorty.longId, language)
                      : Optional.ofNullable(APILocator.getContentletAPI().find(shorty.longId, systemUser, false));
                      
          if(conOpt.isEmpty()) {
              response.sendError(HttpServletResponse.SC_NOT_FOUND);
              return;
          }

          if(cropWidth+cropHeight>0 ) {
              focalPoint = this.getFocalPoint(lowerUri);
              Optional<Tag> focalPointTag = APILocator.getTagAPI().getTagsByInode(conOpt.get().getInode()).stream().filter(t->t.getTagName().startsWith("fp:"+fieldName+":")).findAny();
              if(focalPointTag.isPresent()) {
                  focalPoint = Try.of(()->new FocalPoint(focalPointTag.get().getTagName().replace("fp:", ""))).toJavaOptional();
              }
          }


          inodePath=this.inodePath(conOpt.get(), fieldName, live);
        }else {
            inodePath="/" + shorty.longId + "/temp";
        }
        
        

      final StringBuilder pathBuilder = new StringBuilder(path).append(inodePath).append("/byInode/true");
      

      // This logic is to get the filters to apply in the order were sent
      final String[] filters = lowerUri.split(StringPool.FORWARD_SLASH);

      final String[] subarray = IntStream.range( Math.min(3, filters.length), filters.length).mapToObj(i -> filters[i]).toArray(String[]::new);
        

      if(isImage) {
      this.addImagePath(width, height, maxWidth, maxHeight, minWidth, minHeight, quality, jpeg, jpegp, webp,  pathBuilder,
                      focalPoint, cropWidth, cropHeight, resampleOpt, subarray);
      }
      this.doForward(request, response, pathBuilder.toString());
    } catch (DotContentletStateException e) {

      Logger.error(this, e.getMessage(), e);
      response.sendError(404);
    }
  }

    private static Optional<Contentlet> getContentletByIdentifier(final boolean live,
            final String identifier, final Language language)
            throws DotDataException, DotSecurityException {

        final PageMode pageMode = live ? PageMode.LIVE : PageMode.PREVIEW_MODE;
        final RenderContext renderContext = WebAPILocator.getVariantWebAPI()
                .getRenderContext(language.getId(), identifier, pageMode, APILocator.systemUser());

        // If a Time Machine date is configured, attempt to retrieve the future version of the contentlet.
        // Return if found.
        final Optional<Contentlet> futureContentlet = getTimeMachineContentlet(identifier, renderContext);
        if(futureContentlet.isPresent()){
            return futureContentlet;
        }
        // Retrieve the contentlet based on the specified identifier and language,
        // considering whether the live version or a preview version is required.
        return Optional.ofNullable(
                APILocator.getContentletAPI().findContentletByIdentifier(identifier, live,
                        renderContext.getCurrentLanguageId(), renderContext.getCurrentVariantKey(),
                        APILocator.systemUser(), false)
        );
    }

    /**
     * Retrieves a contentlet for the given identifier and render context using the Time Machine feature,
     * if a Time Machine date is configured.
     *
     * @param identifier      The unique identifier of the contentlet.
     * @param renderContext   The {@link RenderContext} containing language and variant key information.
     * @return An {@link Optional} containing the contentlet if found; otherwise, an empty Optional.
     * @throws DotDataException     If there is an error accessing the contentlet data.
     * @throws DotSecurityException If there is a security-related issue when retrieving the contentlet.
     */
    private static Optional<Contentlet> getTimeMachineContentlet(
            final String identifier, final RenderContext renderContext) throws DotDataException, DotSecurityException {

        final Optional<Date> timeMachineDate = TimeMachineUtil.getTimeMachineDateAsDate();
        if (timeMachineDate.isPresent()) {
            Contentlet future = APILocator.getContentletAPI().findContentletByIdentifier(
                    identifier,
                    renderContext.getCurrentLanguageId(),
                    renderContext.getCurrentVariantKey(),
                    timeMachineDate.get(),
                    APILocator.systemUser(),
                    false
            );
            return Optional.ofNullable(future);
        }
        return Optional.empty();
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

  private void addImagePath(
                            final int width,
                            final int height,
                            final int maxWidth,
                            final int maxHeight,
                            final int minWidth,
                            final int minHeight,
                            final int quality,
                            final boolean jpeg,
                            final boolean jpegp,
                            final boolean webp,
                            final StringBuilder pathBuilder,
                            final Optional<FocalPoint> focalPoint,
                            final int cropWidth,
                            final int cropHeight,
                            final int resampleOpt,
                            final String[] filters) {

        for(String filter : filters){
            filter = StringPool.FORWARD_SLASH + filter;
            if(widthPattern.matcher(filter).find()){
                pathBuilder.append(width > 0 ? "/resize_w/" + width : StringPool.BLANK);
                continue;
            }
            if(heightPattern.matcher(filter).find()){
                pathBuilder.append(height > 0 ? "/resize_h/" + height : StringPool.BLANK);
                continue;
            }
            if(maxWidthPattern.matcher(filter).find()){
                pathBuilder.append("/resize_maxw/" + maxWidth );
                continue;
            }
            if(maxHeightPattern.matcher(filter).find()){
                pathBuilder.append("/resize_maxh/" + maxHeight );
                continue;
            }
            if (minWidthPattern.matcher(filter).find()) {
                pathBuilder.append("/resize_minw/" + minWidth);
                continue;
            }
            if (minHeightPattern.matcher(filter).find()) {
                pathBuilder.append("/resize_minh/" + minHeight);
                continue;
            }
            
            if(cropWidthPattern.matcher(filter).find()){
                pathBuilder.append(cropWidth > 0 ? "/crop_w/" + cropWidth : StringPool.BLANK);
                continue;
            }
            if(cropHeightPattern.matcher(filter).find()){
                pathBuilder.append(cropHeight > 0 ? "/crop_h/" + cropHeight : StringPool.BLANK);
                continue;
            }
            if(filter.contains("fp")){
                pathBuilder.append(focalPoint.isPresent() ? "/fp/" + focalPoint.get() : StringPool.BLANK);
                continue;
            }
            if(resampleOptsPattern.matcher(filter).find()){
                pathBuilder.append(resampleOpt > 0 ? "/resize_ro/" + resampleOpt : StringPool.BLANK);
                continue;
            }
        }

        if (quality > 0) {
            pathBuilder.append("/quality_q/" + quality);
        } else {
            pathBuilder.append(jpeg ? "/jpeg_q/75" : StringPool.BLANK);
            pathBuilder.append(webp ? "/webp_q/75" : StringPool.BLANK);
            pathBuilder.append(jpeg && jpegp ? "/jpeg_p/1" : StringPool.BLANK);
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

  
  /**
   * Resolves and builds the appropriate file path for a contentlet's field.
   * This method handles both regular fields and special cases for image/file fields,
   * including language fallback logic when necessary.
   *
   * @param contentlet The contentlet whose field path needs to be resolved
   * @param tryField The name of the field to try to resolve
   * @param live Whether to use the live version (true) or working version (false)
   * @return A string representing the path to the field's content
   * @throws DotStateException If there's an issue with the contentlet's state
   * @throws DotDataException If there's an error accessing the data
   * @throws DotSecurityException If there's a security violation
   */
  protected final String inodePath(final Contentlet contentlet,
                                   final String tryField,
                                   final boolean live)
          throws DotStateException, DotDataException, DotSecurityException {

        final Optional<Field> fieldOpt = resolveField(contentlet, tryField);

        if (fieldOpt.isEmpty()) {
            return buildFieldPath(contentlet, FILE_ASSET_DEFAULT);
        }

        final Field field = fieldOpt.get();
        if (field instanceof ImageField || field instanceof FileField) {

            final String relatedImageId = contentlet.getStringProperty(field.variable());
            Optional<ContentletVersionInfo> contentletVersionInfo =
                    this.versionableAPI.getContentletVersionInfo(relatedImageId, contentlet.getLanguageId());

            if (contentletVersionInfo.isEmpty() && shouldFallbackToDefaultLanguage(contentlet)) {
                // Try finding the contentlet version with the default language ID
                Logger.info(this, "No contentlet version found for identifier " + relatedImageId + " in language " + contentlet.getLanguageId() + ", trying default language.");
                contentletVersionInfo = this.versionableAPI.getContentletVersionInfo(relatedImageId,APILocator.getLanguageAPI().getDefaultLanguage().getId());
            }

            if (contentletVersionInfo.isPresent()) {
                Logger.debug(this, "Contentlet version found for identifier: " + relatedImageId);
                final String inode = live
                        ? contentletVersionInfo.get().getLiveInode()
                        : contentletVersionInfo.get().getWorkingInode();
                try{
                    final Contentlet imageContentlet = APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false);
                    validateContentlet(imageContentlet, live, inode);
                    final String fieldVar = imageContentlet.isDotAsset() ? DotAssetContentType.ASSET_FIELD_VAR : FILE_ASSET_DEFAULT;
                    return buildFieldPath(imageContentlet, fieldVar);
                }catch (DotDataException e){
                    Logger.debug(this.getClass(), e.getMessage());
                }
            }
            Logger.debug(this, "No contentlet version found for identifier: " + relatedImageId + ", returning path based on original contentlet inode: " + contentlet.getInode());
        }
        return buildFieldPath(contentlet, field.variable());
    }

    /**
     * Determines whether the system should attempt to fallback to the default language
     * for the given contentlet. This is used when content is not found in the
     * contentlet's original language.
     *
     * @param contentlet The contentlet to check for language fallback eligibility
     * @return true if the system should attempt to use the default language, false otherwise
     */
    private boolean shouldFallbackToDefaultLanguage(final Contentlet contentlet) {
        return APILocator.getLanguageAPI().canDefaultFileToDefaultLanguage() &&
                APILocator.getLanguageAPI().getDefaultLanguage().getId() != contentlet.getLanguageId();
    }

    /**
     * Constructs a standardized field path for a contentlet and field variable.
     * The path format is: /[contentlet-inode]/[field-variable]
     *
     * @param contentlet The contentlet for which to build the path
     * @param fieldVar The field variable to append to the path
     * @return A formatted path string
     */
    private String buildFieldPath(final Contentlet contentlet, final String fieldVar) {
        return StringPool.FORWARD_SLASH + contentlet.getInode() + StringPool.FORWARD_SLASH + fieldVar;
    }

    private void validateContentlet(final Contentlet contentlet, final boolean live, final String inode) throws DotDataException {
        if (Objects.isNull(contentlet)) {
            final String versionType = live ? PageMode.LIVE.name() : PageMode.WORKING.name();
            throw new DotDataException(String.format("No contentlet found for %s inode %s", versionType, inode));
        }
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
