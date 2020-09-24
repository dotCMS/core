package com.dotcms.rendering.velocity.directive;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.services.VelocityType;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.util.PageMode;

import java.io.File;
import java.io.Writer;

import java.util.Optional;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;

import com.liferay.util.StringPool;

public class ContentletDetail extends DotDirective {



    private static final long serialVersionUID = 1L;
    final static String EXTENSION = VelocityType.CONTENT.fileExtension;


    @Override
    public String getName() {
        return "contentDetail";
    }


    private long resolveLang(final String identifier, final RenderParams params) {
        Optional<ContentletVersionInfo> cv;
        long tryingLang = params.language.getId();
        long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        if(tryingLang==defaultLang) {
            return tryingLang;
        }
        try {
            cv = APILocator.getVersionableAPI().getContentletVersionInfo(identifier, tryingLang);
            if (cv.isPresent()) {
                return tryingLang;
            }
            else {
                cv = APILocator.getVersionableAPI().getContentletVersionInfo(identifier, defaultLang);

                if(!cv.isPresent()) {
                    throw new ResourceNotFoundException("cannnot find contentlet id " + identifier + " lang:" + defaultLang);
                }

                String inode = (params.mode.showLive) ? cv.get().getLiveInode() : cv.get().getWorkingInode();
                Contentlet test = APILocator.getContentletAPI().find(inode, params.user, params.mode.respectAnonPerms);
                ContentType type = test.getContentType();
                if (type.baseType() == BaseContentType.FORM || type.baseType() == BaseContentType.PERSONA
                        || "Host".equalsIgnoreCase(type.variable())) {
                    return defaultLang;
                } else if (type.baseType() == BaseContentType.CONTENT
                        && APILocator.getLanguageAPI().canDefaultContentToDefaultLanguage()) {
                    return defaultLang;
                } else if (type.baseType() == BaseContentType.WIDGET
                        && APILocator.getLanguageAPI().canDefaultWidgetToDefaultLanguage()) {
                    return defaultLang;
                }

            }
        } catch (Exception e) {

            throw new ResourceNotFoundException("cannnot find contentlet id " + identifier + " lang:" + tryingLang, e);
        }
        throw new ResourceNotFoundException("cannnot find contentlet id " + identifier + " lang:" + tryingLang);

    }

    @Override
    String resolveTemplatePath(final Context context, final Writer writer, final RenderParams params, final String[] arguments) {

        final String identifier = arguments[0];
        long lang = resolveLang(identifier, params);



        final boolean showWorking = (context.get("_show_working_") != null && (boolean) context.get("_show_working_"));

        final StringBuilder path = new StringBuilder();

        path.append(StringPool.FORWARD_SLASH)
            .append(!showWorking ? params.mode.name() : PageMode.PREVIEW_MODE.name())
            .append(StringPool.FORWARD_SLASH)
            .append(identifier)
            .append(StringPool.UNDERLINE)
            .append(lang)
            .append(StringPool.PERIOD)
            .append(EXTENSION);

        return path.toString();
    }
}

