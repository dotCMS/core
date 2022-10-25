package com.dotcms.rendering.velocity.directive;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.services.VelocityType;

import com.dotcms.variant.business.web.VariantWebAPI.RenderContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
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

    @Override
    String resolveTemplatePath(final Context context, final Writer writer, final RenderParams params, final String[] arguments) {

        final String identifier = arguments[0];
        final RenderContext renderContext = WebAPILocator.getVariantWebAPI()
                .getRenderContext(params.language.getId(), identifier, params.mode, params.user);

        final boolean showWorking = (context.get("_show_working_") != null && (boolean) context.get("_show_working_"));

        final StringBuilder path = new StringBuilder();

        path.append(StringPool.FORWARD_SLASH)
            .append(!showWorking ? params.mode.name() : PageMode.PREVIEW_MODE.name())
            .append(StringPool.FORWARD_SLASH)
            .append(identifier)
            .append(StringPool.UNDERLINE)
            .append(renderContext.getCurrentLanguageId())
            .append(StringPool.UNDERLINE)
            .append(renderContext.getCurrentVariantKey())
            .append(StringPool.PERIOD)
            .append(EXTENSION);

        return path.toString();
    }
}

