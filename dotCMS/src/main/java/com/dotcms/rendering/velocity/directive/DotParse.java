package com.dotcms.rendering.velocity.directive;

import com.dotcms.util.ConversionUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;

import javax.servlet.http.HttpServletRequest;
import java.io.Writer;


public class DotParse extends DotDirective {

  private static final long serialVersionUID = 1L;

  private final String hostIndicator = "//";
  private final String EDIT_ICON =
          "<div data-dot-object='vtl-file' data-dot-inode='%s' data-dot-url='%s' data-dot-can-read='%s' data-dot-can-edit='%s'></div>";

  @Override
  public final String getName() {

    return "dotParse";
  }


  private static final String CONTENT_LANGUAGE_ID = "_contentlanguageid";
    /**
     * In case than an upper component needs to overrides the language to get the version of the content, you can use this method to do it in advance.
     * note: the change will just affect the calls to dotParse at request scope.
     * @param request {@link HttpServletRequest}
     * @param identifier {@link String} contentlet identifier
     * @param languageId {@link Long} language identifier
     */
  public static void setContentLanguageId (final HttpServletRequest request, final String identifier, final long languageId) {

      final String contentLanguageId  = identifier + CONTENT_LANGUAGE_ID;
      request.setAttribute(contentLanguageId, languageId);
  }

  private static long getContentLanguageId (final HttpServletRequest request, final String identifier,  final long defaultLanguageId) {

      final String contentLanguageId  = identifier + CONTENT_LANGUAGE_ID;
      return null != request &&
              null != request.getAttribute(contentLanguageId)?
                ConversionUtils.toLong(request.getAttribute(contentLanguageId), defaultLanguageId):
                defaultLanguageId;
  }


  @Override
  String resolveTemplatePath(final Context context, final Writer writer, final RenderParams params,final String[] arguments) {
      
    final String argument = arguments[0];
    String templatePath   = argument;
    Host host             = params.currentHost;
    final User user       = params.user;
    final HttpServletRequest request =
            (HttpServletRequest) context.get("request");
    
    try {

      // if we have a host
      if (templatePath.startsWith(hostIndicator)) {
        templatePath = templatePath.substring(hostIndicator.length(), templatePath.length());
        String hostName = templatePath.substring(0, templatePath.indexOf('/'));
        templatePath = templatePath.substring(templatePath.indexOf('/'), templatePath.length());
        host = APILocator.getHostAPI().resolveHostName(hostName, user, params.mode.respectAnonPerms);
      }

      final Identifier identifier = APILocator.getIdentifierAPI().find(host, templatePath);
      final long languageId = getContentLanguageId(request, identifier.getId(), params.language.getId());

      //Verify if we found a resource with the given path
      if ( null == identifier || !UtilMethods.isSet(identifier.getId()) ) {

        String errorMessage = String.format("No resource found for [%s]",  arguments[0]);

        throw new ResourceNotFoundException(errorMessage);
      }

      ContentletVersionInfo contentletVersionInfo = APILocator.getVersionableAPI()
              .getContentletVersionInfo(identifier.getId(), languageId);

      if (contentletVersionInfo == null  ||  UtilMethods.isNotSet(contentletVersionInfo.getIdentifier())  || contentletVersionInfo.isDeleted()) {

        final long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        if (defaultLang != languageId) {
          contentletVersionInfo = APILocator.getVersionableAPI().getContentletVersionInfo(identifier.getId(), defaultLang);
        }
      }

      if ( null == contentletVersionInfo || UtilMethods.isNotSet(contentletVersionInfo.getIdentifier()) ) {
          throwNotResourceNotFoundException(params, templatePath);
      }

      final String inode = params.mode.showLive ?
              contentletVersionInfo.getLiveInode() : contentletVersionInfo.getWorkingInode();

      //We found the resource but not the version we are looking for
      if ( null == inode ) {
          throwNotResourceNotFoundException(params, templatePath);
      }

      final boolean respectFrontEndRolesForVTL = params.mode.respectAnonPerms?
              Config.getBooleanProperty("RESPECT_FRONTEND_ROLES_FOR_DOTPARSE", true) : params.mode.respectAnonPerms;
      final Contentlet contentlet = APILocator.getContentletAPI().find(inode, APILocator.getUserAPI().getSystemUser(), respectFrontEndRolesForVTL);
      final FileAsset asset = APILocator.getFileAssetAPI().fromContentlet(contentlet);

      // add the edit control if we have run through a page render
      if (!context.containsKey("dontShowIcon") &&
              PageMode.EDIT_MODE == params.mode ) {
          final String editIcon = String.format(EDIT_ICON, contentlet.getInode(), identifier.getURI(),
                  APILocator.getPermissionAPI().doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user),
                  APILocator.getPermissionAPI().doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user));
          writer.append(editIcon);
      }

      return null != asset.getFileAsset()? asset.getFileAsset().getAbsolutePath():null;
    } catch (ResourceNotFoundException e) {
        Logger.warn(this.getClass(), " - unable to resolve " + templatePath + " getting this: "+ e.getMessage() );
        if(e.getStackTrace().length>0){
          Logger.warn(this.getClass(), " - at " + e.getStackTrace()[0]);
        }
        throw e;
    } catch (DotSecurityException  e) {
        Logger.warn(this.getClass(), " - unable to resolve " + templatePath + " getting this: "+ e.getMessage() );
        if(e.getStackTrace().length>0){
            Logger.warn(this.getClass(), " - at " + e.getStackTrace()[0]);
        }
        throw new ResourceNotFoundException(e);
    } catch (Exception e) {
        Logger.warn(this.getClass(), " - unable to resolve " + templatePath + " getting this: "+ e.getMessage() );
        if(e.getStackTrace().length>0){
            Logger.warn(this.getClass(), " - at " + e.getStackTrace()[0]);
        }
        throw new DotStateException(e);
    }
  }

    private void throwNotResourceNotFoundException(final RenderParams params, final String templatePath) {
        final String errorMessage = String.format("Not found %s version of [%s]", params.mode, templatePath);
        throw new ResourceNotFoundException(errorMessage);
    }
}

