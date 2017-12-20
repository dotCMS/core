package com.dotcms.rendering.velocity.directive;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.directive.DotDirective;
import com.dotcms.rendering.velocity.directive.RenderParams;
import com.dotcms.rendering.velocity.services.VelocityType;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.util.Config;

import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;

import java.io.File;
import java.io.Writer;

public class ContentletDetail extends DotDirective {



  private static final long serialVersionUID = 1L;
  final static String EXTENSION = VelocityType.CONTENT.fileExtension;


  @Override
  public String getName() {
    return "contentDetail";
  }



  @Override
  String resolveTemplatePath(final Context context, final Writer writer, final RenderParams params, final String[] arguments) {

    ContentletVersionInfo cv;
    final String argument = arguments[0];
    try {
      cv = APILocator.getVersionableAPI().getContentletVersionInfo(argument, params.language.getId());
      if (cv == null) {
        long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        if (defaultLang != params.language.getId()) {
          cv = APILocator.getVersionableAPI().getContentletVersionInfo(argument, defaultLang);
          String inode = (params.live) ? cv.getLiveInode() : cv.getWorkingInode();
          Contentlet test = APILocator.getContentletAPI().find(inode, params.user, params.mode.showLive);
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




      return  File.separator +  params.mode.name() + File.separator   + argument + "_" + cv.getLang() + "." + EXTENSION;
  }
}

