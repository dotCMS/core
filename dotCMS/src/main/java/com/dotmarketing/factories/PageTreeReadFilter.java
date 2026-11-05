package com.dotmarketing.factories;

import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides which contentlets placed on a page's tree a user can read. A contentlet is looked up by
 * identifier in any language, working and archived versions included, in the default variant or,
 * when it only exists in another one, in that variant. READ is checked without front-end roles by
 * default, the same way the page REST endpoints check the page. Each contentlet is checked once per
 * instance, however many tree rows place it. CMS administrators can read every row, including rows
 * whose contentlet cannot be loaded, and no lookup is made for them.
 */
public class PageTreeReadFilter {

    private final User user;
    private final boolean respectFrontendRoles;
    private final boolean anonymousFallback;
    private final Map<String, Boolean> readableByContentId = new HashMap<>();

    /**
     * Checks READ without front-end roles, as the page REST endpoints check the page.
     *
     * @param user the user the rows are filtered for
     */
    public PageTreeReadFilter(final User user) {
        this(user, false);
    }

    /**
     * @param user                 the user the rows are filtered for; {@code null} is checked as
     *                             the anonymous user
     * @param respectFrontendRoles whether front-end roles count, as for the page mode being
     *                             rendered ({@code PageMode#respectAnonPerms})
     */
    public PageTreeReadFilter(final User user, final boolean respectFrontendRoles) {
        this(user, respectFrontendRoles, false);
    }

    private PageTreeReadFilter(final User user, final boolean respectFrontendRoles,
            final boolean anonymousFallback) {
        this.user = user;
        this.respectFrontendRoles = respectFrontendRoles;
        this.anonymousFallback = anonymousFallback;
    }

    /**
     * Builds a filter that follows what a page render shows the user in the given mode: READ with
     * the mode's front-end roles setting, and, in edit and preview mode, also content the
     * anonymous user can read, which the render falls back to there.
     *
     * @param user the user the page is rendered for
     * @param mode the mode the page is rendered in
     * @return the filter for that render
     */
    public static PageTreeReadFilter forRender(final User user, final PageMode mode) {
        return new PageTreeReadFilter(user, mode.respectAnonPerms,
                mode == PageMode.EDIT_MODE || mode == PageMode.PREVIEW_MODE);
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
            if (canRead(multiTree.getContentlet())) {
                readableRows.add(multiTree);
            }
        }
        return readableRows;
    }

    /**
     * Tells whether the user can read the contentlet a tree row points to.
     *
     * @param contentId identifier of the contentlet placed in the row
     * @return {@code true} if the user is a CMS administrator or has READ on the contentlet;
     *         {@code false} otherwise, including when the contentlet cannot be loaded
     * @throws DotDataException if the permissions of the contentlet cannot be loaded
     */
    public boolean canRead(final String contentId) throws DotDataException {

        if (isAdmin()) {
            return true;
        }

        Boolean readable = readableByContentId.get(contentId);
        if (null == readable) {
            readable = loadAndCheck(contentId);
            readableByContentId.put(contentId, readable);
        }
        return readable;
    }

    /**
     * Collects the personalizations of a page's tree whose contentlet the user can read, so a
     * persona only counts as used on the page when the user can see some of its content.
     *
     * @param pageContents the page's tree, as returned by {@code MultiTreeAPI#getPageMultiTrees}
     * @return the personalization tags of the rows the user can read
     * @throws DotDataException if the permissions of a contentlet cannot be loaded
     */
    public Set<String> readablePersonalizations(
            final Table<String, String, Set<PersonalizedContentlet>> pageContents) throws DotDataException {

        final Set<String> personalizations = new HashSet<>();
        for (final Set<PersonalizedContentlet> contentlets : pageContents.values()) {
            for (final PersonalizedContentlet contentlet : contentlets) {
                if (canRead(contentlet.getContentletId())) {
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
     * @return {@code true} if some row of that personalization holds content the user can read
     * @throws DotDataException if the permissions of a contentlet cannot be loaded
     */
    public boolean hasReadableContent(final Table<String, String, Set<PersonalizedContentlet>> pageContents,
            final String personalization) throws DotDataException {

        for (final Set<PersonalizedContentlet> contentlets : pageContents.values()) {
            for (final PersonalizedContentlet contentlet : contentlets) {
                if (personalization.equals(contentlet.getPersonalization())
                        && canRead(contentlet.getContentletId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAdmin() {
        return null != user && user.isAdmin();
    }

    private boolean loadAndCheck(final String contentId) throws DotDataException {

        if (!UtilMethods.isSet(contentId)) {
            return false;
        }

        final Contentlet contentlet;
        try {
            contentlet = load(contentId);
        } catch (final DotContentletStateException e) {
            Logger.warn(this, "Contentlet " + contentId + " placed on a page could not be loaded; "
                    + "it is treated as not readable", e);
            return false;
        }
        if (null == contentlet) {
            return false;
        }

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        return permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user,
                respectFrontendRoles)
                || (anonymousFallback && permissionAPI.doesUserHavePermission(contentlet,
                        PermissionAPI.PERMISSION_READ, APILocator.getUserAPI().getAnonymousUser(), true));
    }

    private Contentlet load(final String contentId) throws DotDataException {

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final Contentlet contentlet = contentletAPI.findContentletByIdentifierAnyLanguage(contentId, true);
        return null != contentlet ? contentlet : contentletAPI.findContentletByIdentifierAnyLanguageAnyVariant(contentId);
    }
}
