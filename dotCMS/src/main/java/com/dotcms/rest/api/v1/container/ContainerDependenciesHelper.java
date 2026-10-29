package com.dotcms.rest.api.v1.container;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Finds the Templates that use a Container, for the {@code GET /api/v1/containers/dependencies}
 * endpoint of {@link ContainerResource}.
 *
 * <p>The requesting {@link User} must be able to read the Container, and only the Templates that
 * user can read are reported. Which Containers a Template uses is resolved as the system user, so
 * a Container the user cannot read inside some Template does not make the whole lookup fail;
 * nothing about such Containers is returned. A Site the user cannot read is reported as an absent
 * site name. Front-end roles are never considered, because the endpoint is for back-end users.
 *
 * <p>Which Templates use the Containers is decided by
 * {@link TemplateAPI#findTemplatesUsingContainers(Collection, User)}, which looks for all the
 * Containers of a request in a single pass over the Templates.
 */
public class ContainerDependenciesHelper {

    private final TemplateAPI templateAPI;
    private final HostAPI hostAPI;
    private final IdentifierAPI identifierAPI;
    private final PermissionAPI permissionAPI;
    private final User systemUser;

    /**
     * Builds the helper on the APIs registered in {@link APILocator}.
     */
    public ContainerDependenciesHelper() {
        this(APILocator.getTemplateAPI(), APILocator.getHostAPI(), APILocator.getIdentifierAPI(),
                APILocator.getPermissionAPI(), APILocator.systemUser());
    }

    /**
     * Builds the helper with explicit collaborators, so tests can pass mocks.
     *
     * @param templateAPI   the {@link TemplateAPI} that lists Templates and their containers
     * @param hostAPI       the {@link HostAPI} that resolves the Templates' Sites
     * @param identifierAPI the {@link IdentifierAPI} that resolves a Template's Site id
     * @param permissionAPI the {@link PermissionAPI} that checks READ on the Container
     * @param systemUser    the system {@link User}, used to count the Templates whoever can read them
     */
    @VisibleForTesting
    ContainerDependenciesHelper(final TemplateAPI templateAPI, final HostAPI hostAPI,
                                final IdentifierAPI identifierAPI, final PermissionAPI permissionAPI,
                                final User systemUser) {
        this.templateAPI = templateAPI;
        this.hostAPI = hostAPI;
        this.identifierAPI = identifierAPI;
        this.permissionAPI = permissionAPI;
        this.systemUser = systemUser;
    }

    /**
     * Lists, for each Container, the Templates among those the user can read that use it. The
     * Templates are searched once for all the Containers.
     *
     * @param containers the {@link Container}s to look up; the user needs READ permission on each
     * @param user       the {@link User} requesting the list; Template visibility and Site names
     *                   follow this user's permissions
     * @return one view per Container, in the order given, with its inode, identifier and title plus
     * one {@link TemplateDependencyView} per Template that uses it; the list is empty when no
     * Template does
     * @throws DotDataException     if the Containers, the Templates or their sites cannot be read
     *                              from the data source
     * @throws DotSecurityException if the user cannot read one of the Containers or list Templates;
     *                              nothing is searched in that case
     */
    public List<ContainerDependenciesView> dependenciesOf(final List<Container> containers, final User user)
            throws DotDataException, DotSecurityException {

        for (final Container container : containers) {
            this.requireRead(container, user);
        }
        final Map<String, List<Template>> templatesByContainer =
                this.templateAPI.findTemplatesUsingContainers(identifiersOf(containers), user);

        final Map<String, String> siteNames = new HashMap<>();
        final List<ContainerDependenciesView> views = new ArrayList<>();
        for (final Container container : containers) {
            final List<TemplateDependencyView> usages = new ArrayList<>();
            for (final Template template : templatesByContainer.getOrDefault(container.getIdentifier(), List.of())) {
                usages.add(new TemplateDependencyView(template.getInode(), template.getIdentifier(),
                        template.getTitle(), this.siteNameOf(template, user, siteNames)));
            }
            views.add(new ContainerDependenciesView(container.getInode(), container.getIdentifier(),
                    container.getTitle(), usages));
        }
        return views;
    }

    /**
     * Counts every Template that uses each Container, whoever can read it. It is meant to guard a
     * delete, so it does not depend on the permissions of the user asking: a Template the user
     * cannot see still blocks the delete. Only the counts are returned, so nothing about those
     * Templates is disclosed. The Templates are searched once for all the Containers.
     *
     * @param containers the {@link Container}s about to be deleted
     * @return how many Templates, archived ones included, use each Container, by Container
     * identifier; a Container no Template uses has a zero
     * @throws DotDataException     if the Templates or their containers cannot be read from the
     *                              data source
     * @throws DotSecurityException if the Templates cannot be listed
     */
    public Map<String, Integer> countTemplatesUsing(final Collection<Container> containers)
            throws DotDataException, DotSecurityException {

        final List<Container> asked = new ArrayList<>(containers);
        final Map<String, List<Template>> templatesByContainer =
                this.templateAPI.findTemplatesUsingContainers(identifiersOf(asked), this.systemUser);

        final Map<String, Integer> counts = new LinkedHashMap<>();
        for (final Container container : asked) {
            counts.put(container.getIdentifier(),
                    templatesByContainer.getOrDefault(container.getIdentifier(), List.of()).size());
        }
        return counts;
    }

    /**
     * The distinct identifiers of the Containers, in the order given.
     */
    private static List<String> identifiersOf(final List<Container> containers) {
        return containers.stream().map(Container::getIdentifier).distinct().collect(Collectors.toList());
    }

    /**
     * Refuses the request when the user cannot write the Container. A delete asks for it before it
     * looks at the Templates, so a user who may not delete the Container gets a permission error
     * rather than a count of Templates they may not even be able to see.
     *
     * @param container            the {@link Container} about to be deleted
     * @param user                 the {@link User} deleting it
     * @param respectFrontendRoles whether front-end roles count towards the permission
     * @throws DotDataException     if the permission cannot be read from the data source
     * @throws DotSecurityException if the user does not have WRITE permission on the Container
     */
    public void requireWrite(final Container container, final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

        if (!this.permissionAPI.doesUserHavePermission(container, PermissionAPI.PERMISSION_WRITE, user,
                respectFrontendRoles)) {
            SecurityLogger.logInfo(ContainerDependenciesHelper.class, String.format(
                    "User '%s' does not have WRITE permission on Container '%s'",
                    user.getUserId(), container.getIdentifier()));
            throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
        }
    }

    /**
     * Refuses the lookup when the user cannot read the Container.
     */
    private void requireRead(final Container container, final User user)
            throws DotDataException, DotSecurityException {

        if (!this.permissionAPI.doesUserHavePermission(container, PermissionAPI.PERMISSION_READ, user, false)) {
            SecurityLogger.logInfo(ContainerDependenciesHelper.class, String.format(
                    "User '%s' does not have READ permission on Container '%s'",
                    user.getUserId(), container.getIdentifier()));
            throw new DotSecurityException("User does not have READ permission on the Container");
        }
    }

    /**
     * Resolves the host name of the Site the Template lives in, remembering the answer per Site
     * for the rest of the call. Returns {@code null} when the Template's identifier has no Site or
     * when the user cannot read that Site; a data error propagates.
     */
    private String siteNameOf(final Template template, final User user, final Map<String, String> siteNames)
            throws DotDataException {

        final Identifier identifier = this.identifierAPI.find(template.getIdentifier());
        if (null == identifier || !UtilMethods.isSet(identifier.getId())) {
            return null;
        }
        final String hostId = identifier.getHostId();
        if (siteNames.containsKey(hostId)) {
            return siteNames.get(hostId);
        }
        String siteName = null;
        try {
            final Host site = this.hostAPI.find(hostId, user, false);
            siteName = null == site || !UtilMethods.isSet(site.getIdentifier()) ? null : site.getHostname();
        } catch (final DotSecurityException e) {
            Logger.debug(this, () -> String.format("User '%s' cannot read Site '%s'", user.getUserId(), hostId));
        }
        siteNames.put(hostId, siteName);
        return siteName;
    }
}
