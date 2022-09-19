package com.dotcms.variant.business.web;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.languagesmanager.model.Language;
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

        final String currentVarintId = currentVariantId();
        Optional<ContentletVersionInfo> contentletVersionInfo = APILocator.getVersionableAPI()
                .getContentletVersionInfo(identifier, tryingLang, currentVarintId);

        if (contentletVersionInfo.isPresent()) {
            return new RenderContext(currentVarintId, tryingLang);
        }

        contentletVersionInfo = APILocator.getVersionableAPI()
                .getContentletVersionInfo(identifier, tryingLang, VariantAPI.DEFAULT_VARIANT.identifier());


        if (contentletVersionInfo.isPresent()) {
            return new RenderContext(VariantAPI.DEFAULT_VARIANT.identifier(), tryingLang);
        }

        try {
            final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
            contentletVersionInfo = APILocator.getVersionableAPI()
                    .getContentletVersionInfo(identifier, defaultLanguage.getId(), currentVarintId);

            if (contentletVersionInfo.isPresent() && shouldFallbackByLang(
                    contentletVersionInfo.get(), pageMode, user)) {
                return new RenderContext(currentVarintId, defaultLanguage.getId());
            }

            contentletVersionInfo = APILocator.getVersionableAPI()
                    .getContentletVersionInfo(identifier, defaultLanguage.getId(),
                            VariantAPI.DEFAULT_VARIANT.identifier());

            if (contentletVersionInfo.isPresent() && shouldFallbackByLang(
                    contentletVersionInfo.get(), pageMode, user)) {
                return new RenderContext(VariantAPI.DEFAULT_VARIANT.identifier(),
                        defaultLanguage.getId());
            }

            throw new ResourceNotFoundException("cannnot find contentlet id " + identifier + " lang:" + tryingLang);
        } catch (DotDataException | DotSecurityException e) {
            throw new ResourceNotFoundException("cannnot find contentlet id " + identifier + " lang:" + tryingLang, e);
        }
    }

    private boolean shouldFallbackByLang(final ContentletVersionInfo contentletVersionInfo, PageMode pageMode, User user)
            throws DotDataException, DotSecurityException {
        final String inode = pageMode.showLive ? contentletVersionInfo.getLiveInode() :
                contentletVersionInfo.getWorkingInode();

        final Contentlet contentlet = APILocator.getContentletAPI().find(inode, user, pageMode.respectAnonPerms);
        final ContentType type = contentlet.getContentType();

        if (type.baseType() == BaseContentType.FORM || type.baseType() == BaseContentType.PERSONA
                || "Host".equalsIgnoreCase(type.variable())) {
            return true;
        } else if (type.baseType() == BaseContentType.CONTENT
                && APILocator.getLanguageAPI().canDefaultContentToDefaultLanguage()) {
            return true;
        } else if (type.baseType() == BaseContentType.WIDGET
                && APILocator.getLanguageAPI().canDefaultWidgetToDefaultLanguage()) {
            return true;
        }

        return false;
    }
}
