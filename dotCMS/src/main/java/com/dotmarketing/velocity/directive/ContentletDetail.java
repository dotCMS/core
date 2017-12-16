package com.dotmarketing.velocity.directive;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.util.Config;

import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;

import java.io.Writer;

public class ContentletDetail extends DotDirective {



  private static final long serialVersionUID = 1L;
  final static String EXTENSION = Config.getStringProperty("VELOCITY_CONTENT_EXTENSION", "content");


  @Override
  public String getName() {
    return "contentDetail";
  }



  @Override
  String resolveTemplatePath(final Context context, final Writer writer, final RenderParams params, final String argument) {

    ContentletVersionInfo cv;

    try {
      cv = APILocator.getVersionableAPI().getContentletVersionInfo(argument, params.language.getId());
      if (cv == null) {
        long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        if (defaultLang != params.language.getId()) {
          cv = APILocator.getVersionableAPI().getContentletVersionInfo(argument, defaultLang);
          String inode = (params.live) ? cv.getLiveInode() : cv.getWorkingInode();
          Contentlet test = APILocator.getContentletAPI().find(inode, params.user, !params.editMode);
          ContentType type = APILocator.getContentTypeAPI(params.user).find(test.getContentTypeId());
          if(type.baseType() == BaseContentType.CONTENT  && !Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false)){
            throw new ResourceNotFoundException("cannnot find contentlet id " +  argument + " lang:" + params.language);
          }
          else if(type.baseType() == BaseContentType.WIDGET  && !Config.getBooleanProperty("DEFAULT_WIDGET_TO_DEFAULT_LANGUAGE", true)){
            throw new ResourceNotFoundException("cannnot find widget id " +  argument + " lang:" + params.language);
          }
        }
      }
    } catch (Exception e1) {
      throw new ResourceNotFoundException("cannnot find contentlet id " +  argument + " lang:" + params.language);
    }

      // _show_working_ context variable is used on Container Services. If the time machine date is after the Publish
      // date of the Contentlet (identifier data) we need to show the working. If not we only show the live.
      //
      // #if($UtilMethods.isSet($_ident.sysPublishDate) && $_tmdate.after($_ident.sysPublishDate))
      //    #set($_show_working_=true)
      //
      boolean showWorking = false;

      if (context.get("_show_working_") != null && (boolean)context.get("_show_working_")) {
          showWorking = true;
      }

      return  (params.live && !showWorking)
          ? "/live/"      + argument + "_" + cv.getLang() + "." + EXTENSION
          : "/working/"   + argument + "_" + cv.getLang() + "." + EXTENSION;
  }
}

