package com.dotcms.variant.business.web;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.exception.ResourceNotFoundException;

public class VariantWebAPIImpl implements VariantWebAPI{

    @Override
    public String currentVariantId() {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();

        if (!UtilMethods.isSet(request)) {
            return VariantAPI.DEFAULT_VARIANT.identifier();
        }

        final String requestParameter = request.getParameter(VariantAPI.VARIANT_KEY);

        if (UtilMethods.isSet(requestParameter)) {
            try {
                final Optional<Variant> byName = APILocator.getVariantAPI()
                        .getByName(requestParameter);
                return byName.isPresent() ? byName.get().identifier() : VariantAPI.DEFAULT_VARIANT.identifier();
            } catch (DotDataException e) {
                Logger.error(VariantWebAPIImpl.class,
                        String.format("It is not possible get variant y name %s: %s",
                                requestParameter, e.getMessage()));
                return VariantAPI.DEFAULT_VARIANT.identifier();
            }
        } else {
            return VariantAPI.DEFAULT_VARIANT.identifier();
        }
    }

    @Override
    public RenderContext getRenderContext(final long tryingLang, final String identifier,
            final PageMode pageMode, final User user) {

        //final long languageId = getLanguageId(tryingLang, identifier, pageMode, user);
        return new RenderContext(currentVariantId(), tryingLang);
    }

    private long getLanguageId(final long tryingLang, final String identifier, final PageMode pageMode, final User user){
        long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();

        if(tryingLang == defaultLang) {
            return tryingLang;
        }

        final Optional<ContentletVersionInfo> contentletVersionInfo = APILocator.getVersionableAPI()
                .getContentletVersionInfo(identifier, tryingLang);

        if (contentletVersionInfo.isPresent()) {
            return tryingLang;
        }
        try {
            ContentletVersionInfo defaultLangVersionInfo = APILocator.getVersionableAPI()
                    .getContentletVersionInfo(identifier, defaultLang)
                    .orElseThrow(() -> new ResourceNotFoundException("cannnot find contentlet id " + identifier + " lang:" + defaultLang));

            final String inode = pageMode.showLive ? defaultLangVersionInfo.getLiveInode() :
                    defaultLangVersionInfo.getWorkingInode();

            final Contentlet contentlet = APILocator.getContentletAPI().find(inode, user, pageMode.respectAnonPerms);
            final ContentType type = contentlet.getContentType();

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

            throw new ResourceNotFoundException("cannnot find contentlet id " + identifier + " lang:" + tryingLang);
        } catch (Exception e) {

            throw new ResourceNotFoundException("cannnot find contentlet id " + identifier + " lang:" + tryingLang, e);
        }
    }
}
