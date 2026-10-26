package com.dotcms.rest.api.v1.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the helper behind {@code GET /v1/containers/dependencies} of
 * {@link ContainerResource}. Which Templates use a Container is decided by
 * {@link TemplateAPI#findTemplatesUsingContainers(java.util.Collection, User)}; these tests cover what
 * the helper adds: the READ check, the Site names, the user scoping and a single search for however
 * many Containers are asked about.
 */
class ContainerDependenciesHelperTest {

    private static final String CONTAINER_ID = "container-id";
    private static final String SITE_ID = "site-id";

    private TemplateAPI templateAPI;
    private HostAPI hostAPI;
    private IdentifierAPI identifierAPI;
    private PermissionAPI permissionAPI;
    private User user;
    private User systemUser;
    private Container container;
    private ContainerDependenciesHelper helper;

    @BeforeEach
    void setUp() throws Exception {
        templateAPI = mock(TemplateAPI.class);
        hostAPI = mock(HostAPI.class);
        identifierAPI = mock(IdentifierAPI.class);
        permissionAPI = mock(PermissionAPI.class);
        user = mock(User.class);
        systemUser = mock(User.class);
        container = container("container-inode", CONTAINER_ID, "Main container");
        when(permissionAPI.doesUserHavePermission(any(Container.class), eq(PermissionAPI.PERMISSION_READ),
                eq(user), eq(false))).thenReturn(true);
        helper = new ContainerDependenciesHelper(templateAPI, hostAPI, identifierAPI, permissionAPI, systemUser);
    }

    /**
     * Given templates use the container, when the dependencies are requested, then each one is
     * returned with its inode, identifier, title and site name, and titles with markup characters
     * come back unchanged.
     */
    @Test
    void listsTheTemplatesThatUseTheContainer() throws Exception {
        final String containerTitle = "Tom & Jerry's <main> \"box\"";
        final String templateTitle = "<b>Landing</b> & 'friends'";
        final Container titled = container("container-inode", CONTAINER_ID, containerTitle);
        final Template using = template("t1-inode", "t1-id", templateTitle);
        templatesUsing(user, using);
        templateLivesOn(using, "demo.dotcms.com");

        final ContainerDependenciesView view = dependenciesOf(titled);

        assertEquals(containerTitle, view.title());
        assertEquals(List.of(new TemplateDependencyView("t1-inode", "t1-id", templateTitle, "demo.dotcms.com")),
                view.templates());
    }

    /**
     * Given no template uses the container, when the dependencies are requested, then the view
     * carries the container's inode, identifier and title and an empty list.
     */
    @Test
    void returnsAnEmptyListWhenNoTemplateUsesTheContainer() throws Exception {
        final ContainerDependenciesView view = dependenciesOf(container);

        assertEquals("container-inode", view.inode());
        assertEquals(CONTAINER_ID, view.identifier());
        assertEquals("Main container", view.title());
        assertTrue(view.templates().isEmpty());
    }

    /**
     * Given the user cannot read the container, when the dependencies are requested, then the
     * lookup is refused before any template is searched.
     */
    @Test
    void refusesAContainerTheUserCannotRead() throws Exception {
        when(permissionAPI.doesUserHavePermission(container, PermissionAPI.PERMISSION_READ, user, false))
                .thenReturn(false);

        assertThrows(DotSecurityException.class, () -> dependenciesOf(container));

        verify(templateAPI, never()).findTemplatesUsingContainers(any(), any());
    }

    /**
     * Given the template's identifier cannot be loaded, when the dependencies are requested, then
     * the template is still listed and its site name is null.
     */
    @Test
    void siteNameIsNullWhenTheTemplateIdentifierIsMissing() throws Exception {
        templatesUsing(user, template("t1-inode", "t1-id", "Landing"));
        when(identifierAPI.find("t1-id")).thenReturn(null);

        final ContainerDependenciesView view = dependenciesOf(container);

        assertEquals(1, view.templates().size());
        assertNull(view.templates().get(0).siteName());
    }

    /**
     * Given the site lookup returns nothing for the requesting user, when the dependencies are
     * requested, then the template is still listed and its site name is null.
     */
    @Test
    void siteNameIsNullWhenTheSiteIsNotFoundForTheUser() throws Exception {
        final Template using = template("t1-inode", "t1-id", "Landing");
        templatesUsing(user, using);
        templateIdentifierOn(using, SITE_ID);
        when(hostAPI.find(SITE_ID, user, false)).thenReturn(null);

        final ContainerDependenciesView view = dependenciesOf(container);

        assertEquals(1, view.templates().size());
        assertNull(view.templates().get(0).siteName());
    }

    /**
     * Given the user is not allowed to read the template's site, when the dependencies are
     * requested, then the template is still listed and its site name is null.
     */
    @Test
    void siteNameIsNullWhenTheUserCannotReadTheSite() throws Exception {
        final Template using = template("t1-inode", "t1-id", "Landing");
        templatesUsing(user, using);
        templateIdentifierOn(using, SITE_ID);
        when(hostAPI.find(SITE_ID, user, false)).thenThrow(new DotSecurityException("not readable"));

        final ContainerDependenciesView view = dependenciesOf(container);

        assertEquals(1, view.templates().size());
        assertNull(view.templates().get(0).siteName());
    }

    /**
     * Given a requesting user, when the dependencies are requested, then the READ check, the
     * template search and the site lookup run as that user, without front-end roles.
     */
    @Test
    void runsTheLookupsAsTheRequestingUser() throws Exception {
        final Template using = template("t1-inode", "t1-id", "Landing");
        templatesUsing(user, using);
        templateLivesOn(using, "demo.dotcms.com");

        dependenciesOf(container);

        verify(permissionAPI).doesUserHavePermission(container, PermissionAPI.PERMISSION_READ, user, false);
        verify(templateAPI).findTemplatesUsingContainers(List.of(CONTAINER_ID), user);
        verify(hostAPI).find(SITE_ID, user, false);
    }

    /**
     * Given the template search fails with a data error, when the dependencies are requested,
     * then the error propagates instead of an empty list.
     */
    @Test
    void propagatesATemplateSearchFailureInsteadOfAnsweringEmpty() throws Exception {
        when(templateAPI.findTemplatesUsingContainers(List.of(CONTAINER_ID), user)).thenThrow(new DotDataException("read failed"));

        assertThrows(DotDataException.class, () -> dependenciesOf(container));
    }

    /**
     * Given the identifier lookup of a template that uses the container fails with a data error,
     * when the dependencies are requested, then the error propagates instead of a null site name.
     */
    @Test
    void propagatesAnIdentifierLookupFailureInsteadOfAnsweringEmpty() throws Exception {
        templatesUsing(user, template("t1-inode", "t1-id", "Landing"));
        when(identifierAPI.find("t1-id")).thenThrow(new DotDataException("read failed"));

        assertThrows(DotDataException.class, () -> dependenciesOf(container));
    }

    /**
     * Given two templates on the same site use the container, when the dependencies are
     * requested, then the site is looked up once and both templates carry its name.
     */
    @Test
    void looksUpEachSiteOnce() throws Exception {
        final Template first = template("t1-inode", "t1-id", "Landing");
        final Template second = template("t2-inode", "t2-id", "Blog");
        templatesUsing(user, first, second);
        templateLivesOn(first, "demo.dotcms.com");
        templateIdentifierOn(second, SITE_ID);

        final ContainerDependenciesView view = dependenciesOf(container);

        assertEquals(2, view.templates().size());
        assertEquals("demo.dotcms.com", view.templates().get(1).siteName());
        verify(hostAPI, times(1)).find(SITE_ID, user, false);
    }

    /**
     * Given templates the requesting user cannot read also use the container, when the usage is
     * counted for a delete, then every template counts: the search runs as the system user.
     */
    @Test
    void countsEveryTemplateThatUsesTheContainerWhoeverCanReadIt() throws Exception {
        templatesUsing(systemUser, template("t1-inode", "t1-id", "Landing"), template("t2-inode", "t2-id", "Hidden"));

        assertEquals(Map.of(CONTAINER_ID, 2), helper.countTemplatesUsing(List.of(container)));

        verify(templateAPI, never()).findTemplatesUsingContainers(any(), eq(user));
    }

    /**
     * Given no template uses the container, when the usage is counted, then it is zero.
     */
    @Test
    void countsNoTemplatesWhenNoneUsesTheContainer() throws Exception {
        assertEquals(Map.of(CONTAINER_ID, 0), helper.countTemplatesUsing(List.of(container)));
    }

    /**
     * Given three containers, when their dependencies are requested, then the templates are
     * searched once for all of them and each view carries its own templates, in the order asked.
     */
    @Test
    void searchesOnceForSeveralContainers() throws Exception {
        final Container second = container("second-inode", "second-id", "Second");
        final Container third = container("third-inode", "third-id", "Third");
        final Template usingSecond = template("t1-inode", "t1-id", "Landing");
        when(permissionAPI.doesUserHavePermission(any(Container.class), eq(PermissionAPI.PERMISSION_READ),
                eq(user), eq(false))).thenReturn(true);
        when(templateAPI.findTemplatesUsingContainers(List.of(CONTAINER_ID, "second-id", "third-id"), user))
                .thenReturn(Map.of("second-id", List.of(usingSecond)));
        templateLivesOn(usingSecond, "demo.dotcms.com");

        final List<ContainerDependenciesView> views = helper.dependenciesOf(List.of(container, second, third), user);

        assertEquals(List.of(CONTAINER_ID, "second-id", "third-id"),
                views.stream().map(ContainerDependenciesView::identifier).collect(Collectors.toList()));
        assertTrue(views.get(0).templates().isEmpty());
        assertEquals(1, views.get(1).templates().size());
        assertTrue(views.get(2).templates().isEmpty());
        verify(templateAPI, times(1)).findTemplatesUsingContainers(any(), any());
    }

    /**
     * Given the user cannot read the second of two containers, when the dependencies are
     * requested, then the whole lookup is refused before any template is searched.
     */
    @Test
    void refusesTheWholeLookupWhenOneContainerCannotBeRead() throws Exception {
        final Container unreadable = container("second-inode", "second-id", "Second");
        when(permissionAPI.doesUserHavePermission(unreadable, PermissionAPI.PERMISSION_READ, user, false))
                .thenReturn(false);

        assertThrows(DotSecurityException.class,
                () -> helper.dependenciesOf(List.of(container, unreadable), user));

        verify(templateAPI, never()).findTemplatesUsingContainers(any(), any());
    }

    /**
     * Given three containers, when their usage is counted for a delete, then the templates are
     * searched once, as the system user, and each container has its own count.
     */
    @Test
    void countsSeveralContainersWithOneSearch() throws Exception {
        final Container second = container("second-inode", "second-id", "Second");
        when(templateAPI.findTemplatesUsingContainers(List.of(CONTAINER_ID, "second-id"), systemUser))
                .thenReturn(Map.of(CONTAINER_ID, List.of(template("t1-inode", "t1-id", "A"), template("t2-inode", "t2-id", "B")),
                        "second-id", List.of()));

        assertEquals(Map.of(CONTAINER_ID, 2, "second-id", 0), helper.countTemplatesUsing(List.of(container, second)));

        verify(templateAPI, times(1)).findTemplatesUsingContainers(any(), any());
    }

    private void templatesUsing(final User searchingUser, final Template... templates) throws Exception {
        when(templateAPI.findTemplatesUsingContainers(List.of(CONTAINER_ID), searchingUser))
                .thenReturn(Map.of(CONTAINER_ID, List.of(templates)));
    }

    private ContainerDependenciesView dependenciesOf(final Container requested) throws Exception {
        return helper.dependenciesOf(List.of(requested), user).get(0);
    }

    private void templateIdentifierOn(final Template template, final String hostId) throws Exception {
        final Identifier identifier = mock(Identifier.class);
        when(identifier.getId()).thenReturn(template.getIdentifier());
        when(identifier.getHostId()).thenReturn(hostId);
        when(identifierAPI.find(template.getIdentifier())).thenReturn(identifier);
    }

    private void templateLivesOn(final Template template, final String hostname) throws Exception {
        templateIdentifierOn(template, SITE_ID);
        final Host site = mock(Host.class);
        when(site.getIdentifier()).thenReturn(SITE_ID);
        when(site.getHostname()).thenReturn(hostname);
        when(hostAPI.find(SITE_ID, user, false)).thenReturn(site);
    }

    private static Container container(final String inode, final String identifier, final String title) {
        final Container container = new Container();
        container.setInode(inode);
        container.setIdentifier(identifier);
        container.setTitle(title);
        return container;
    }

    private static Template template(final String inode, final String identifier, final String title) {
        final Template template = new Template();
        template.setInode(inode);
        template.setIdentifier(identifier);
        template.setTitle(title);
        return template;
    }
}
