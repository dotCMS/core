package com.dotmarketing.velocity.directive;

import java.io.Writer;

import org.apache.velocity.context.Context;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.util.Config;

public class ContentletDetail extends DotDirective {



  private static final long serialVersionUID = 1L;
  final static String EXTENSION = Config.getStringProperty("VELOCITY_CONTENT_EXTENSION", "content");


  @Override
  public String getName() {
    return "contentDetail";
  }



  @Override
  String resolveTemplatePath(final Context context, final Writer writer, final RenderParams params, final String argument) {

    ContentletVersionInfo cv = null;

    try {
      cv = APILocator.getVersionableAPI().getContentletVersionInfo(argument, params.language.getId());
      if (cv == null) {
        long defualtLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        if (defualtLang != params.language.getId()) {
          cv = APILocator.getVersionableAPI().getContentletVersionInfo(argument, defualtLang);
        }
      }
    } catch (DotStateException | DotDataException e1) {
      throw new DotRuntimeException(e1);
    }


    return  (params.live) 
        ? "/live/"      + argument + "_" + cv.getLang() + "." + EXTENSION
        : "/working/"   + argument + "_" + cv.getLang() + "." + EXTENSION;



  }
}

