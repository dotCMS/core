package com.dotmarketing.velocity.directive;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.Writer;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;


public class DotParse extends DotDirective {

  private static final long serialVersionUID = 1L;

  private final String hostIndicator = "//";
  private final String EDIT_ICON =
      "<div class='dot_parseIcon'><a href='javascript:window.top.document.getElementById(\"detailFrame\").contentWindow.editFile(\"${_dotParseInode}\");' title='$_dotParsePath'><span class='editIcon'></span></a></div>";


  @Override
  public final String getName() {

    return "dotParse";
  }






  @Override
  String resolveTemplatePath(final Context context, final Writer writer, final RenderParams params,final String argument) {
    String templatePath = argument;
    boolean live = params.live;
    Host host = params.currentHost;;
    User user = params.user;
    HttpServletRequest request = (HttpServletRequest) context.get("request");
    
    try {

      // if we have a host
      if (templatePath.startsWith(hostIndicator)) {
        templatePath = templatePath.substring(hostIndicator.length(), templatePath.length());
        String hostName = templatePath.substring(0, templatePath.indexOf('/'));
        templatePath = templatePath.substring(templatePath.indexOf('/'), templatePath.length());
        host = APILocator.getHostAPI().resolveHostName(hostName, user, live);
      }

      long lang = params.language.getId();
      Identifier id = APILocator.getIdentifierAPI().find(host, templatePath);

      //Verify if we found a resource with the given path
      if ( null == id || !UtilMethods.isSet(id.getId()) ) {

        String errorMessage = String.format("No resource found for [%s]", templatePath);

        /*
        In Edit mode we are allow to fail and be noisy, but on Preview and Live mode we just want to
        continue with the render of the page, on the DotDirective.render we are catching ResourceNotFoundException's
        and on the catch we continue with the render.
         */
        if ( params.editMode ) {
          throw new DotStateException(errorMessage);
        } else {
          throw new ResourceNotFoundException(errorMessage);
        }
      }

      ContentletVersionInfo cv = APILocator.getVersionableAPI().getContentletVersionInfo(id.getId(), lang);

      if (cv == null) {
        long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        if (defaultLang != lang) {
          cv = APILocator.getVersionableAPI().getContentletVersionInfo(id.getId(), defaultLang);
        }
      }
      String inode = ((live) ? cv.getLiveInode() : cv.getWorkingInode());

      //We found the resource but not the version we are looking for
      if ( null == inode ) {
        String errorMessage = String.format("Not found %s version of [%s]", (live) ? "Live" : "Working", templatePath);
        throw new ResourceNotFoundException(errorMessage);
      }
      boolean respectFrontEndRolesForVTL = (!params.live) ? Config.getBooleanProperty("RESPECT_FRONTEND_ROLES_FOR_DOTPARSE", true) : params.live;

      Contentlet c = APILocator.getContentletAPI().find(inode, params.user, respectFrontEndRolesForVTL);
      FileAsset asset = APILocator.getFileAssetAPI().fromContentlet(c);
      
      
      // add the edit control if we have run through a page render
      if (!context.containsKey("dontShowIcon") && params.editMode &&  (request.getAttribute(
              Constants.CMS_FILTER_URI_OVERRIDE)!=null)) {
        if (APILocator.getPermissionAPI().doesUserHavePermission(c, PermissionAPI.PERMISSION_READ, user)) {
          String editIcon = new String(EDIT_ICON).replace("${_dotParseInode}", c.getInode()).replace("${_dotParsePath}",
              id.getParentPath());
          writer.append(editIcon);
        }
      }


      return (null != asset.getFileAsset())?asset.getFileAsset().getAbsolutePath():null;
    } 
    catch (ResourceNotFoundException e) {
        Logger.warn(this.getClass(), " - unable to resolve " + templatePath + " getting this: "+ e.getMessage() );
        if(e.getStackTrace().length>0){
          Logger.warn(this.getClass(), " - at " + e.getStackTrace()[0]);
        }
        throw e;
    }
    catch (DotSecurityException  e) {
        Logger.warn(this.getClass(), " - unable to resolve " + templatePath + " getting this: "+ e.getMessage() );
        if(e.getStackTrace().length>0){
            Logger.warn(this.getClass(), " - at " + e.getStackTrace()[0]);
        }
        throw new ResourceNotFoundException(e);
    }
    catch (Exception e) {
        Logger.warn(this.getClass(), " - unable to resolve " + templatePath + " getting this: "+ e.getMessage() );
        if(e.getStackTrace().length>0){
            Logger.warn(this.getClass(), " - at " + e.getStackTrace()[0]);
        }
        throw new DotStateException(e);
    }
  }


}

