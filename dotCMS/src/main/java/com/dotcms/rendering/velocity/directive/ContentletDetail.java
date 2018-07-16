package com.dotcms.rendering.velocity.directive;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.directive.DotDirective;
import com.dotcms.rendering.velocity.directive.RenderParams;
import com.dotcms.rendering.velocity.services.VelocityType;

import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.util.Config;

import com.dotmarketing.util.PageMode;
import com.liferay.util.StringPool;
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
          String inode = (params.mode.showLive) ? cv.getLiveInode() : cv.getWorkingInode();
          Contentlet test = APILocator.getContentletAPI().find(inode, params.user, params.mode.respectAnonPerms);
          ContentType type = APILocator.getContentTypeAPI(APILocator.systemUser()).find(test.getContentTypeId());
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

    // If the _show_working_ comes in the context, means that is Time Machine we need
    // to show the working. If not we only need to show the live

    final boolean showWorking = (context.get("_show_working_") != null && (boolean)context.get("_show_working_")) ? true : false;

    final StringBuilder path=new StringBuilder();

    path.append(File.separator).append(!showWorking ? params.mode.name() : PageMode.PREVIEW_MODE.name()).append(File.separator)
            .append(argument).append(StringPool.UNDERLINE).append(cv.getLang()).append(StringPool.PERIOD).append(EXTENSION);

      return path.toString();
  }
}

