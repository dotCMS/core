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
import javax.servlet.http.HttpSession;

/**
 * Default implementation for {@link VariantWebAPI}
 */
public class VariantWebAPIImpl implements VariantWebAPI{

    /**
     * Return the current variant following this steps:
     *
     * - Get the current {@link HttpServletRequest} from {@link HttpServletRequestThreadLocal#getRequest()},
     * If the method {@link HttpServletRequestThreadLocal#getRequest()} return null then return {@link VariantAPI#DEFAULT_VARIANT}..
     * - if the request exists get the parameter name <code>variantName</code>, if the parameter not exists
     * return {@link VariantAPI#DEFAULT_VARIANT}.
     * - If the <code>variantName</code> parameter exists but it not a existing variant then return
     * {@link VariantAPI#DEFAULT_VARIANT}.
     *
     * @return
     */
    public String currentVariantId() {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();

        if (!UtilMethods.isSet(request)) {
            return VariantAPI.DEFAULT_VARIANT.name();
        }

        String currentVariantName = getCurrentVariantNameFromParameter(request)
                .or(() -> getCurrentVariantNameFromAttribute(request))
                .orElse(null);

        if (!UtilMethods.isSet(currentVariantName)) {
            currentVariantName = VariantAPI.DEFAULT_VARIANT.name();
        } else {
            try {
                final Optional<Variant> byName = APILocator.getVariantAPI()
                        .get(currentVariantName);
                currentVariantName = byName.isPresent() ? byName.get().name() : VariantAPI.DEFAULT_VARIANT.name();
            } catch (DotDataException e) {
                Logger.error(VariantWebAPIImpl.class,
                        String.format("It is not possible get variant y name %s: %s",
                                currentVariantName, e.getMessage()));
                currentVariantName = VariantAPI.DEFAULT_VARIANT.name();
            }
        }

        setSessionAttribute(request, currentVariantName);
        return currentVariantName;
    }

    private static void setSessionAttribute(final HttpServletRequest request,
            final String currentVariantName) {

        final HttpSession session = request.getSession(true);

        if (!UtilMethods.isSet(session)) {
            return;
        }
        
        final Object attribute = session.getAttribute(VariantAPI.VARIANT_KEY);

        if (mustOverwrite(attribute, currentVariantName)) {
            final CurrentVariantSessionItem currentVariantSessionItem = new
                    CurrentVariantSessionItem(currentVariantName);
            session.setAttribute(VariantAPI.VARIANT_KEY, currentVariantSessionItem);
        }
    }

    private static boolean mustOverwrite(final Object currentVariantSessionItemAsObject, final String currentVariantName) {

        if (!UtilMethods.isSet(currentVariantSessionItemAsObject)) {
            return true;
        }

        final CurrentVariantSessionItem currentVariantSessionItem = (CurrentVariantSessionItem) currentVariantSessionItemAsObject;
        return currentVariantSessionItem instanceof CurrentVariantSessionItem &&
                (!currentVariantSessionItem.getVariantName().equals(currentVariantName) || currentVariantSessionItem.isExpired());
    }

    private Optional<String> getCurrentVariantNameFromAttribute(
            HttpServletRequest request) {
        return Optional.ofNullable(request.getAttribute(VariantAPI.VARIANT_KEY))
                .map(Object::toString);
    }

    private static Optional<String> getCurrentVariantNameFromParameter(final HttpServletRequest request) {
        return Optional.ofNullable(request.getParameter(VariantAPI.VARIANT_KEY));
    }

    /**
     * Return the specific {@link Variant} and {@link Language} with a {@link Contentlet} should be render.
     * this method follow this rules:
     *
     * - First look for a version of the {@link Contentlet} in the current variant and <code>tryingLang</code>
     * if exists then return the Current Variant and <code>tryingLang</code> .
     * - If it does not exist and the current variant is different that the {@link VariantAPI#DEFAULT_VARIANT}
     * then look for a version of the {@link Contentlet} in the {@link VariantAPI#DEFAULT_VARIANT}
     * and <code>tryingLang</code> if it exists then return the {@link VariantAPI#DEFAULT_VARIANT}  and <code>tryingLang</code>.
     * - If it does not exist then look for a version of the {@link Contentlet} in the Current Variant
     * and the Default Language if it exists then return the Current Variant and Default Language.
     * - If it does not exist then look for a version of the {@link Contentlet} in the {@link VariantAPI#DEFAULT_VARIANT}
     * and the Default Language if it exists then return the {@link VariantAPI#DEFAULT_VARIANT}  and Default Language.
     *
     * @param tryingLang Language to try if not exists any version for this lang try with default
     * @param identifier {@link com.dotcms.content.model.Contentlet}'s identifier
     * @param pageMode page mode to render
     * @param user to check {@link com.dotmarketing.beans.Permission}
     * @return
     */
    @Override
    public RenderContext getRenderContext(final long tryingLang, final String identifier,
            final PageMode pageMode, final User user) {

        final ContentletVersionInfo contentletVersionInfoByFallback = getContentletVersionInfoByFallback(
                tryingLang, identifier, pageMode, user);

        return new RenderContext(contentletVersionInfoByFallback.getVariant(),
                contentletVersionInfoByFallback.getLang());
    }



    @Override
    public ContentletVersionInfo getContentletVersionInfoByFallback(final long tryingLang, final String identifier,
            final PageMode pageMode, final User user) {

        final String currentVariantName = currentVariantId();
        Optional<ContentletVersionInfo> contentletVersionInfo = APILocator.getVersionableAPI()
                .getContentletVersionInfo(identifier, tryingLang, currentVariantName);

        if (contentletVersionInfo.isPresent()) {
            return contentletVersionInfo.get();
        }

        if (!VariantAPI.DEFAULT_VARIANT.equals(currentVariantName)) {
            contentletVersionInfo = APILocator.getVersionableAPI()
                    .getContentletVersionInfo(identifier, tryingLang,
                            VariantAPI.DEFAULT_VARIANT.name());

            if (contentletVersionInfo.isPresent()) {
                return contentletVersionInfo.get();
            }
        }

        try {
            final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

            if (defaultLanguage.getId() != tryingLang) {

                contentletVersionInfo = APILocator.getVersionableAPI()
                        .getContentletVersionInfo(identifier, defaultLanguage.getId(),
                                currentVariantName);

                if (contentletVersionInfo.isPresent() && shouldFallbackByLang(
                        contentletVersionInfo.get(), pageMode, user)) {
                    return contentletVersionInfo.get();
                }

                if (!VariantAPI.DEFAULT_VARIANT.equals(currentVariantName)) {

                    contentletVersionInfo = APILocator.getVersionableAPI()
                            .getContentletVersionInfo(identifier, defaultLanguage.getId(),
                                    VariantAPI.DEFAULT_VARIANT.name());

                    if (contentletVersionInfo.isPresent() && shouldFallbackByLang(
                            contentletVersionInfo.get(), pageMode, user)) {
                        return contentletVersionInfo.get();
                    }
                }
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
