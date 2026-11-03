package com.dotcms.variant.business.web;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
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
import com.liferay.util.StringPool;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.exception.ResourceNotFoundException;
import javax.servlet.http.HttpSession;

/**
 * Default implementation for {@link VariantWebAPI}
 */
public class VariantWebAPIImpl implements VariantWebAPI{

    private static final String VARIANT_FIELD = ESMappingConstants.VARIANT + ":";

    /**
     * Lowercased with the same call the indexer applies to every String value it writes, because
     * variant is a keyword field with no normalizer.
     */
    private static final String DEFAULT_VARIANT_CLAUSE =
            "+" + VARIANT_FIELD + VariantAPI.DEFAULT_VARIANT.name().toLowerCase() + " ";

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
                return APILocator.getVariantAPI()
                        .get(currentVariantName)
                        .filter((variant -> !variant.archived()))
                        .orElse(VariantAPI.DEFAULT_VARIANT)
                        .name();
            } catch (DotDataException e) {
                Logger.error(VariantWebAPIImpl.class,
                        String.format("It is not possible get variant y name %s: %s",
                                currentVariantName, e.getMessage()));
                currentVariantName = VariantAPI.DEFAULT_VARIANT.name();
            }
        }

        setSessionAttributeIfNeeded(request, currentVariantName);
        return currentVariantName;
    }

    /**
     * A variant is a version axis of a content item, so which variant a request resolves on is the
     * server's to decide. Only a back-end user's request names its own.
     *
     * @param user the {@link User} the request is served for, or null when there is none
     * @return true when the request may name its own variant
     */
    @Override
    public boolean selectsVariant(final User user) {
        return null != user && user.isBackendUser();
    }

    /**
     * Returns the variant the current request resolves on for the given user.
     *
     * @param user the {@link User} the request is served for, or null when there is none
     * @return the variant the request names when {@link #selectsVariant(User)} holds, otherwise
     * {@link VariantAPI#DEFAULT_VARIANT}
     */
    @Override
    public String currentVariantId(final User user) {
        return selectsVariant(user) ? currentVariantId() : VariantAPI.DEFAULT_VARIANT.name();
    }

    /**
     * Returns the query to run for this caller, scoped to the variant the caller may read.
     *
     * @param query the Lucene query as the caller wrote it
     * @param user the {@link User} the search runs for, or null when there is none
     * @return the caller's query untouched when it may name its own variant, otherwise the same
     * query with its {@code variant:} terms dropped and the default one put in front
     */
    @Override
    public String scopeQueryToDefaultVariant(final String query, final User user) {

        if (selectsVariant(user)) {
            return query;
        }

        final String withoutCallerTerms = dropVariantTerms(
                UtilMethods.isSet(query) ? query : StringPool.BLANK);

        // The clause goes before the rest of the query: after it, a query ending in an unbalanced
        // escape would consume the space that separates the two.
        return DEFAULT_VARIANT_CLAUSE + withoutCallerTerms;
    }

    /**
     * Removes every {@code variant:} term the caller wrote from a Lucene query.
     *
     * <p>Only terms the method can remove without changing the meaning of what is left are
     * removed: one sitting at the start of the query or after whitespace, outside quotes and
     * outside parentheses. A term inside a group is left alone, because taking it out would leave
     * the group malformed - it is the clause imposed further down the search path, not this
     * method, that keeps such a query from reading another variant.
     *
     * @param query the Lucene query as the caller wrote it
     * @return the same query without the caller's variant terms
     */
    private static String dropVariantTerms(final String query) {
        final StringBuilder kept = new StringBuilder(query.length());
        boolean insideQuotes = false;
        int depth = 0;
        int index = 0;

        while (index < query.length()) {
            final char current = query.charAt(index);

            if (current == '"') {
                insideQuotes = !insideQuotes;
            } else if (!insideQuotes && current == '(') {
                depth++;
            } else if (!insideQuotes && current == ')' && depth > 0) {
                depth--;
            } else if (!insideQuotes && depth == 0 && startsVariantTerm(query, index, kept)) {
                index = endOfVariantTerm(query, index);
                continue;
            }

            kept.append(current);
            index++;
        }

        return kept.toString().trim();
    }

    /**
     * Tells whether a {@code variant:} term the caller wrote starts at the given position.
     *
     * @param query the query being read
     * @param index the position to look at
     * @param kept what has been kept of the query so far, used to tell a term start from the
     * middle of another term
     * @return true when a variant term starts here
     */
    private static boolean startsVariantTerm(final String query, final int index,
            final CharSequence kept) {

        if (kept.length() > 0 && !Character.isWhitespace(kept.charAt(kept.length() - 1))) {
            return false;
        }

        int at = index;

        if (at < query.length() && (query.charAt(at) == '+' || query.charAt(at) == '-'
                || query.charAt(at) == '!')) {
            at++;
        }

        return query.regionMatches(true, at, VARIANT_FIELD, 0, VARIANT_FIELD.length());
    }

    /**
     * Finds where a variant term ends, so the caller can carry on reading the query past it. The
     * whitespace that followed the term is consumed with it, so dropping a term from the middle of
     * a query does not leave a double space behind.
     *
     * @param query the query being read
     * @param start the position the term starts at
     * @return the position of the first character that is no longer part of the term
     */
    private static int endOfVariantTerm(final String query, final int start) {
        int at = start;

        if (query.charAt(at) == '+' || query.charAt(at) == '-' || query.charAt(at) == '!') {
            at++;
        }

        at += VARIANT_FIELD.length();

        if (at < query.length() && query.charAt(at) == '(') {
            int depth = 0;

            while (at < query.length()) {
                if (query.charAt(at) == '(') {
                    depth++;
                } else if (query.charAt(at) == ')' && --depth == 0) {
                    at++;
                    break;
                }

                at++;
            }
        } else if (at < query.length() && query.charAt(at) == '"') {
            at++;

            while (at < query.length() && query.charAt(at) != '"') {
                at++;
            }

            at = Math.min(at + 1, query.length());
        } else {
            while (at < query.length() && !Character.isWhitespace(query.charAt(at))
                    && query.charAt(at) != ')') {
                at++;
            }
        }

        while (at < query.length() && Character.isWhitespace(query.charAt(at))) {
            at++;
        }

        return at;
    }

    private static void setSessionAttributeIfNeeded(final HttpServletRequest request,
            final String currentVariantName) {

        boolean buildSessionIfNeeded = !"DEFAULT".equals(currentVariantName);

        final HttpSession session = request.getSession(buildSessionIfNeeded);

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
     * and the Default Language if it exists then return the Current Variant and Default Language, if the fallback language is allowed.
     * - If it does not exist then look for a version of the {@link Contentlet} in the {@link VariantAPI#DEFAULT_VARIANT}
     * and the Default Language if it exists then return the {@link VariantAPI#DEFAULT_VARIANT}  and Default Language, if the fallback language is allowed.
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
    public RenderContext getRenderContextForceLangFallback(final long tryingLang, final String identifier,
            final PageMode pageMode, final User user) {

        final ContentletVersionInfo contentletVersionInfoByFallback = getContentletVersionInfoByFallback(
                tryingLang, identifier, pageMode, user, true);

        return new RenderContext(contentletVersionInfoByFallback.getVariant(),
                contentletVersionInfoByFallback.getLang());
    }

    @Override
    public ContentletVersionInfo getContentletVersionInfoByFallback(final long tryingLang, final String identifier,
            final PageMode pageMode, final User user) {
        return getContentletVersionInfoByFallback(tryingLang, identifier, pageMode, user, false);
    }

    public ContentletVersionInfo getContentletVersionInfoByFallback(final long tryingLang, final String identifier,
        final PageMode pageMode, final User user, final boolean forceLangFallback) {

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

                if (contentletVersionInfo.isPresent() &&
                        (forceLangFallback || shouldFallbackByLang(contentletVersionInfo.get(), pageMode, user))) {
                    return contentletVersionInfo.get();
                }

                if (!VariantAPI.DEFAULT_VARIANT.equals(currentVariantName)) {

                    contentletVersionInfo = APILocator.getVersionableAPI()
                            .getContentletVersionInfo(identifier, defaultLanguage.getId(),
                                    VariantAPI.DEFAULT_VARIANT.name());

                    if (contentletVersionInfo.isPresent() &&
                            (forceLangFallback || shouldFallbackByLang(contentletVersionInfo.get(), pageMode, user))) {
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

        return Boolean.TRUE.equals(contentlet.isHTMLPage()) ||
                forceFallbackByContentType(type) ||
                isFileFallback(type) ||
                isContentletFallback(type) ||
                isWidgetFallback(type);
    }

    private static boolean isWidgetFallback(ContentType type) {
        return type.baseType() == BaseContentType.WIDGET
                && APILocator.getLanguageAPI().canDefaultWidgetToDefaultLanguage();
    }

    private static boolean isFileFallback(ContentType type) {
        return (type.baseType() == BaseContentType.FILEASSET || type.baseType() == BaseContentType.DOTASSET)
                && APILocator.getLanguageAPI().canDefaultFileToDefaultLanguage();
    }

    private static boolean isContentletFallback(ContentType type) {
        return type.baseType() == BaseContentType.CONTENT
                && APILocator.getLanguageAPI().canDefaultContentToDefaultLanguage();
    }

    private static boolean forceFallbackByContentType(ContentType type) {
        return type.baseType() == BaseContentType.FORM || type.baseType() == BaseContentType.PERSONA
                || "Host".equalsIgnoreCase(type.variable());
    }
}
