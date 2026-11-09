package com.dotmarketing.factories;

import com.dotcms.variant.VariantAPI;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.Table;
import com.liferay.util.StringPool;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides which contentlets placed on a page's tree a user can read. A contentlet is looked up by
 * identifier in any language, working version first: in the variant of the tree when it has a
 * version there, otherwise in the default variant (archived versions included), otherwise, except
 * for the render checks ({@link #forRender}, {@link #forViewAs}), in any other variant it exists in
 * (archived versions excluded). READ is checked on that version, so the
 * categories it is filed under count. READ is checked without front-end roles by default, the same
 * way the page REST endpoints check the page. Each contentlet is checked once per variant and
 * instance, however many tree rows place it. CMS administrators can read every row, including rows
 * whose contentlet cannot be loaded, and no lookup is made for them.
 */
public class PageTreeReadFilter {

    private final User user;
    private final boolean respectFrontendRoles;
    private final boolean anonymousFallback;
    private final boolean renderedContentOnly;
    private final Map<String, Boolean> readableByVariantAndContentId = new HashMap<>();

    /**
     * Checks READ without front-end roles, as the page REST endpoints check the page.
     *
     * @param user the user the rows are filtered for
     */
    public PageTreeReadFilter(final User user) {
        this(user, false, false, false);
    }

    /**
     * @param user                 the user the rows are filtered for; {@code null} is checked as
     *                             the anonymous user
     * @param respectFrontendRoles whether front-end roles count, as for the page mode being
     *                             rendered ({@code PageMode#respectAnonPerms})
     * @param anonymousFallback    whether content the anonymous user can read in the default
     *                             variant also counts, as the render falls back to it in edit and
     *                             preview mode
     * @param renderedContentOnly  whether only versions the render can show count: an archived
     *                             version counts as not readable, and versions of variants other
     *                             than the current and the default one are not looked up
     */
    private PageTreeReadFilter(final User user, final boolean respectFrontendRoles,
            final boolean anonymousFallback, final boolean renderedContentOnly) {
        this.user = user;
        this.respectFrontendRoles = respectFrontendRoles;
        this.anonymousFallback = anonymousFallback;
        this.renderedContentOnly = renderedContentOnly;
    }

    /**
     * Tells whether a page render in the given mode needs this filter to know what the user sees.
     * Only edit and preview mode leave out content the user has no READ on and render the rest; in
     * every other mode the render refuses a page holding such content, so a page that renders
     * shows the user all of its content.
     *
     * @param mode the mode the page is rendered in
     * @return {@code true} for edit and preview mode
     */
    public static boolean appliesTo(final PageMode mode) {
        return mode == PageMode.EDIT_MODE || mode == PageMode.PREVIEW_MODE;
    }

    /**
     * Builds a filter that follows what a page render shows the user in the given mode: READ on the
     * version the render shows (current variant, then default variant; archived versions not
     * counted), with front-end roles when the mode counts them or when content falls back to the
     * default language ({@code DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE}), and, in edit and preview mode,
     * also content whose default-variant version the anonymous user can read, which the render
     * falls back to there.
     *
     * @param user the user the page is rendered for
     * @param mode the mode the page is rendered in
     * @return the filter for that render
     */
    public static PageTreeReadFilter forRender(final User user, final PageMode mode) {
        // The render's lookup with default-language fallback counts front-end roles in every mode
        final boolean respectFrontendRoles = mode.respectAnonPerms
                || Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false);
        return new PageTreeReadFilter(user, respectFrontendRoles, appliesTo(mode), true);
    }

    /**
     * Builds a filter for the view-as status of a page render: READ with the mode's front-end roles
     * setting, counting only content the render can show (no archived versions), without the
     * render's anonymous fallback, the same as the page personas endpoint.
     *
     * @param user the user the page is rendered for
     * @param mode the mode the page is rendered in
     * @return the filter for that view-as status
     */
    public static PageTreeReadFilter forViewAs(final User user, final PageMode mode) {
        return new PageTreeReadFilter(user, mode.respectAnonPerms, false, true);
    }

    /**
     * Keeps the tree rows whose contentlet the user can read, in their original order.
     *
     * @param multiTrees the page's tree rows, may be {@code null}
     * @return the rows the user can see, or {@code null} when {@code multiTrees} is {@code null}
     * @throws DotDataException if the permissions of a contentlet cannot be loaded
     */
    public List<MultiTree> filter(final List<MultiTree> multiTrees) throws DotDataException {

        if (null == multiTrees || isAdmin()) {
            return multiTrees;
        }

        final List<MultiTree> readableRows = new ArrayList<>();
        for (final MultiTree multiTree : multiTrees) {
            if (canRead(multiTree.getContentlet(), multiTree.getVariantId())) {
                readableRows.add(multiTree);
            }
        }
        return readableRows;
    }

    /**
     * Tells whether the user can read the contentlet a tree row points to.
     *
     * @param contentId   identifier of the contentlet placed in the row
     * @param variantName the variant of the tree the row belongs to
     * @return {@code true} if the user is a CMS administrator or has READ on the contentlet;
     *         {@code false} otherwise, including when the contentlet cannot be loaded
     * @throws DotDataException if the permissions of the contentlet cannot be loaded
     */
    public boolean canRead(final String contentId, final String variantName) throws DotDataException {

        if (isAdmin()) {
            return true;
        }

        final String key = variantName + StringPool.COLON + contentId;
        Boolean readable = readableByVariantAndContentId.get(key);
        if (null == readable) {
            readable = loadAndCheck(contentId, variantName);
            readableByVariantAndContentId.put(key, readable);
        }
        return readable;
    }

    /**
     * Collects the personalizations of a page's tree whose contentlet the user can read, so a
     * persona only counts as used on the page when the user can see some of its content.
     *
     * @param pageContents the page's tree, as returned by {@code MultiTreeAPI#getPageMultiTrees}
     * @param variantName  the variant the tree was loaded for
     * @return the personalization tags of the rows the user can read
     * @throws DotDataException if the permissions of a contentlet cannot be loaded
     */
    public Set<String> readablePersonalizations(
            final Table<String, String, Set<PersonalizedContentlet>> pageContents,
            final String variantName) throws DotDataException {

        final Set<String> personalizations = new HashSet<>();
        for (final Set<PersonalizedContentlet> contentlets : pageContents.values()) {
            for (final PersonalizedContentlet contentlet : contentlets) {
                if (canRead(contentlet.getContentletId(), variantName)) {
                    personalizations.add(contentlet.getPersonalization());
                }
            }
        }
        return personalizations;
    }

    /**
     * Tells whether the user can read at least one contentlet placed on the page for the given
     * personalization.
     *
     * @param pageContents    the page's tree, as returned by {@code MultiTreeAPI#getPageMultiTrees}
     * @param personalization the personalization tag, e.g. {@code dot:persona:<keyTag>}
     * @param variantName     the variant the tree was loaded for
     * @return {@code true} if some row of that personalization holds content the user can read
     * @throws DotDataException if the permissions of a contentlet cannot be loaded
     */
    public boolean hasReadableContent(final Table<String, String, Set<PersonalizedContentlet>> pageContents,
            final String personalization, final String variantName) throws DotDataException {

        for (final Set<PersonalizedContentlet> contentlets : pageContents.values()) {
            for (final PersonalizedContentlet contentlet : contentlets) {
                if (personalization.equals(contentlet.getPersonalization())
                        && canRead(contentlet.getContentletId(), variantName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAdmin() {
        return null != user && user.isAdmin();
    }

    private boolean loadAndCheck(final String contentId, final String variantName) throws DotDataException {

        if (!UtilMethods.isSet(contentId)) {
            return false;
        }

        final Contentlet contentlet;
        try {
            contentlet = load(contentId, variantName);
        } catch (final DotContentletStateException e) {
            Logger.warn(this, "Contentlet " + contentId + " placed on a page could not be loaded; "
                    + "it is treated as not readable", e);
            return false;
        }
        if (null == contentlet) {
            return false;
        }

        // Same order as the render: a version the user can read is shown unless it is archived;
        // otherwise the render falls back to the default-variant version the anonymous user can read
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        if (permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user,
                respectFrontendRoles)) {
            return !(renderedContentOnly && isArchived(contentlet));
        }
        if (!anonymousFallback) {
            return false;
        }
        final Contentlet defaultVersion = loadDefaultVariantVersion(contentId);
        return null != defaultVersion && !isArchived(defaultVersion)
                && permissionAPI.doesUserHavePermission(defaultVersion, PermissionAPI.PERMISSION_READ,
                        APILocator.getUserAPI().getAnonymousUser(), true);
    }

    /**
     * Loads the default-variant version the render falls back to for the anonymous user, or
     * {@code null} when the contentlet has none or it cannot be loaded.
     */
    private Contentlet loadDefaultVariantVersion(final String contentId) throws DotDataException {
        try {
            return APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(contentId,
                    VariantAPI.DEFAULT_VARIANT.name(), false);
        } catch (final DotContentletStateException e) {
            Logger.warn(this, "Default version of contentlet " + contentId + " placed on a page could not "
                    + "be loaded; it is treated as not readable", e);
            return null;
        }
    }

    private boolean isArchived(final Contentlet contentlet) throws DotDataException {
        try {
            return contentlet.isArchived();
        } catch (final DotSecurityException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    private Contentlet load(final String contentId, final String variantName) throws DotDataException {

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        if (UtilMethods.isSet(variantName) && !VariantAPI.DEFAULT_VARIANT.name().equals(variantName)) {
            final Contentlet variantVersion = contentletAPI.findContentletByIdentifierAnyLanguage(contentId,
                    variantName, true);
            if (null != variantVersion) {
                return variantVersion;
            }
        }
        final Contentlet contentlet = contentletAPI.findContentletByIdentifierAnyLanguage(contentId, true);
        // The render only loads versions of the current and the default variant
        return null != contentlet || renderedContentOnly ? contentlet
                : contentletAPI.findContentletByIdentifierAnyLanguageAnyVariant(contentId);
    }
}
