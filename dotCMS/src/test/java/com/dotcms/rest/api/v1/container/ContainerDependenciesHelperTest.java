package com.dotcms.rest.api.v1.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the helper that lists the templates that use a container, consumed by the
 * {@code GET /v1/containers/dependencies} endpoint of {@link ContainerResource}.
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
     * Given a user who can list two templates, one of which uses the container and one which uses
     * another container, when the dependencies are requested, then only the template that uses
     * the container is returned, with its inode, identifier, title and site name, and titles with
     * markup characters come back unchanged.
     */
    @Test
    void listsTheTemplatesThatUseTheContainer() throws Exception {
        final String containerTitle = "Tom & Jerry's <main> \"box\"";
        final String templateTitle = "<b>Landing</b> & 'friends'";
        final Container titled = container("container-inode", CONTAINER_ID, containerTitle);
        final Template using = template("t1-inode", "t1-id", templateTitle);
        final Template other = template("t2-inode", "t2-id", "Blog");
        userCanList(using, other);
        templateUses(using, titled);
        templateUses(other, container("other-inode", "other-id", "Other"));
        templateLivesOn(using, "demo.dotcms.com");

        final ContainerDependenciesView view = helper.dependenciesOf(titled, user);

        assertEquals(containerTitle, view.title());
        assertEquals(List.of(new TemplateDependencyView("t1-inode", "t1-id", templateTitle, "demo.dotcms.com")),
                view.templates());
    }

    /**
     * Given the template's layout references another version of the container (same identifier,
     * different inode), when the dependencies are requested, then the template is listed.
     */
    @Test
    void matchesTheContainerByIdentifierAcrossVersions() throws Exception {
        final Template using = template("t1-inode", "t1-id", "Landing");
        userCanList(using);
        templateUses(using, container("older-inode", CONTAINER_ID, "Main container"));
        templateLivesOn(using, "demo.dotcms.com");

        final ContainerDependenciesView view = helper.dependenciesOf(container, user);

        assertEquals(1, view.templates().size());
        assertEquals("t1-id", view.templates().get(0).identifier());
    }

    /**
     * Given no template uses the container, when the dependencies are requested, then the view
     * carries the container's inode, identifier and title and an empty list.
     */
    @Test
    void returnsAnEmptyListWhenNoTemplateUsesTheContainer() throws Exception {
        final Template other = template("t2-inode", "t2-id", "Blog");
        userCanList(other);
        templateUses(other, container("other-inode", "other-id", "Other"));

        final ContainerDependenciesView view = helper.dependenciesOf(container, user);

        assertEquals("container-inode", view.inode());
        assertEquals(CONTAINER_ID, view.identifier());
        assertEquals("Main container", view.title());
        assertTrue(view.templates().isEmpty());
    }

    /**
     * Given the user cannot read the container, when the dependencies are requested, then the
     * lookup is refused before any template is listed.
     */
    @Test
    void refusesAContainerTheUserCannotRead() throws Exception {
        when(permissionAPI.doesUserHavePermission(container, PermissionAPI.PERMISSION_READ, user, false))
                .thenReturn(false);

        assertThrows(DotSecurityException.class, () -> helper.dependenciesOf(container, user));

        verify(templateAPI, never()).findTemplates(any(), anyBoolean(), any(), any(), any(), any(), any(),
                anyInt(), anyInt(), any());
    }

    /**
     * Given the template's identifier cannot be loaded, when the dependencies are requested, then
     * the template is still listed and its site name is null.
     */
    @Test
    void siteNameIsNullWhenTheTemplateIdentifierIsMissing() throws Exception {
        final Template using = template("t1-inode", "t1-id", "Landing");
        userCanList(using);
        templateUses(using, container);
        when(identifierAPI.find("t1-id")).thenReturn(null);

        final ContainerDependenciesView view = helper.dependenciesOf(container, user);

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
        userCanList(using);
        templateUses(using, container);
        templateIdentifierOn(using, SITE_ID);
        when(hostAPI.find(SITE_ID, user, false)).thenReturn(null);

        final ContainerDependenciesView view = helper.dependenciesOf(container, user);

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
        userCanList(using);
        templateUses(using, container);
        templateIdentifierOn(using, SITE_ID);
        when(hostAPI.find(SITE_ID, user, false)).thenThrow(new DotSecurityException("not readable"));

        final ContainerDependenciesView view = helper.dependenciesOf(container, user);

        assertEquals(1, view.templates().size());
        assertNull(view.templates().get(0).siteName());
    }

    /**
     * Given a requesting user, when the dependencies are requested, then the READ check, the
     * template listing and the site lookup run as that user without front-end roles, while the
     * containers of each template are resolved as the system user.
     */
    @Test
    void runsTheLookupsAsTheRequestingUserExceptContainerResolution() throws Exception {
        final Template using = template("t1-inode", "t1-id", "Landing");
        userCanList(using);
        templateUses(using, container);
        templateLivesOn(using, "demo.dotcms.com");

        helper.dependenciesOf(container, user);

        verify(permissionAPI).doesUserHavePermission(container, PermissionAPI.PERMISSION_READ, user, false);
        verify(templateAPI).findTemplates(eq(user), eq(true), isNull(), isNull(), isNull(), isNull(),
                isNull(), eq(0), eq(ContainerDependenciesHelper.TEMPLATE_PAGE_SIZE), isNull());
        verify(templateAPI).getContainersInTemplate(using, systemUser, false);
        verify(templateAPI, never()).getContainersInTemplate(any(), eq(user), anyBoolean());
        verify(hostAPI).find(SITE_ID, user, false);
    }

    /**
     * Given the user can read more templates than one page holds, when the dependencies are
     * requested, then every page is inspected and a template on the last page is listed.
     */
    @Test
    void inspectsEveryPageOfTemplates() throws Exception {
        final int pageSize = ContainerDependenciesHelper.TEMPLATE_PAGE_SIZE;
        final List<Template> firstPage = new ArrayList<>();
        for (int i = 0; i < pageSize; i++) {
            final Template other = template("p1-" + i, "p1-id-" + i, "Other " + i);
            templateUses(other, container("other-inode", "other-id", "Other"));
            firstPage.add(other);
        }
        final Template last = template("p2-inode", "p2-id", "Last page");
        templateUses(last, container);
        templateLivesOn(last, "demo.dotcms.com");
        when(templateAPI.findTemplates(eq(user), eq(true), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(pageSize), isNull())).thenReturn(firstPage);
        when(templateAPI.findTemplates(eq(user), eq(true), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(pageSize), eq(pageSize), isNull())).thenReturn(List.of(last));

        final ContainerDependenciesView view = helper.dependenciesOf(container, user);

        assertEquals(1, view.templates().size());
        assertEquals("p2-id", view.templates().get(0).identifier());
    }

    /**
     * Given the user can read exactly one page of templates and the listing serves the first
     * page again for an offset past the end, when the dependencies are requested, then the
     * lookup stops after the second page and reports each template once.
     */
    @Test
    void stopsPagingWhenAPageBringsNothingNew() throws Exception {
        final int pageSize = ContainerDependenciesHelper.TEMPLATE_PAGE_SIZE;
        final List<Template> onlyPage = new ArrayList<>();
        for (int i = 0; i < pageSize; i++) {
            final Template using = template("p1-" + i, "p1-id-" + i, "Using " + i);
            templateUses(using, container);
            templateLivesOn(using, "demo.dotcms.com");
            onlyPage.add(using);
        }
        when(templateAPI.findTemplates(eq(user), eq(true), isNull(), isNull(), isNull(), isNull(), isNull(),
                anyInt(), eq(pageSize), isNull())).thenReturn(onlyPage);

        final ContainerDependenciesView view = assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> helper.dependenciesOf(container, user));

        assertEquals(pageSize, view.templates().size());
        verify(templateAPI, times(2)).findTemplates(eq(user), eq(true), isNull(), isNull(), isNull(),
                isNull(), isNull(), anyInt(), eq(pageSize), isNull());
    }

    /**
     * Given two templates on the same site use the container, when the dependencies are
     * requested, then the site is looked up once and both templates carry its name.
     */
    @Test
    void looksUpEachSiteOnce() throws Exception {
        final Template first = template("t1-inode", "t1-id", "Landing");
        final Template second = template("t2-inode", "t2-id", "Blog");
        userCanList(first, second);
        templateUses(first, container);
        templateUses(second, container);
        templateLivesOn(first, "demo.dotcms.com");
        templateIdentifierOn(second, SITE_ID);

        final ContainerDependenciesView view = helper.dependenciesOf(container, user);

        assertEquals(2, view.templates().size());
        assertEquals("demo.dotcms.com", view.templates().get(1).siteName());
        verify(hostAPI, times(1)).find(SITE_ID, user, false);
    }

    /**
     * Given the container resolution of one template fails with a data error, when the
     * dependencies are requested, then the error propagates instead of an empty or partial list.
     */
    @Test
    void propagatesATemplateLookupFailureInsteadOfAnsweringEmpty() throws Exception {
        final Template using = template("t1-inode", "t1-id", "Landing");
        userCanList(using);
        when(templateAPI.getContainersInTemplate(using, systemUser, false)).thenThrow(new DotDataException("read failed"));

        assertThrows(DotDataException.class, () -> helper.dependenciesOf(container, user));
    }

    /**
     * Given the identifier lookup of a template that uses the container fails with a data error,
     * when the dependencies are requested, then the error propagates instead of a null site name.
     */
    @Test
    void propagatesAnIdentifierLookupFailureInsteadOfAnsweringEmpty() throws Exception {
        final Template using = template("t1-inode", "t1-id", "Landing");
        userCanList(using);
        templateUses(using, container);
        when(identifierAPI.find("t1-id")).thenThrow(new DotDataException("read failed"));

        assertThrows(DotDataException.class, () -> helper.dependenciesOf(container, user));
    }

    /**
     * Given templates the requesting user cannot read also use the container, when the usage is
     * counted for a delete, then every template counts: the count is taken as the system user.
     */
    @Test
    void countsEveryTemplateThatUsesTheContainerWhoeverCanReadIt() throws Exception {
        final Template first = template("t1-inode", "t1-id", "Landing");
        final Template second = template("t2-inode", "t2-id", "Hidden");
        final Template other = template("t3-inode", "t3-id", "Blog");
        when(templateAPI.findTemplates(eq(systemUser), eq(true), any(), any(), any(), any(), any(),
                eq(0), eq(ContainerDependenciesHelper.TEMPLATE_PAGE_SIZE), any()))
                .thenReturn(List.of(first, second, other));
        when(templateAPI.getContainersInTemplate(first, systemUser, false)).thenReturn(List.of(container));
        when(templateAPI.getContainersInTemplate(second, systemUser, false)).thenReturn(List.of(container));
        when(templateAPI.getContainersInTemplate(other, systemUser, false))
                .thenReturn(List.of(container("other-inode", "other-id", "Other")));

        assertEquals(2, helper.countTemplatesUsing(container));

        verify(templateAPI, never()).findTemplates(eq(user), anyBoolean(), any(), any(), any(), any(), any(),
                anyInt(), anyInt(), any());
    }

    /**
     * Given a container without identifier, when the usage is counted, then it is zero and no
     * template is listed.
     */
    @Test
    void countsNoTemplatesForAContainerWithoutIdentifier() throws Exception {
        assertEquals(0, helper.countTemplatesUsing(container("new-inode", null, "Unsaved")));

        verify(templateAPI, never()).findTemplates(any(), anyBoolean(), any(), any(), any(), any(), any(),
                anyInt(), anyInt(), any());
    }

    private void userCanList(final Template... templates) throws Exception {
        when(templateAPI.findTemplates(eq(user), anyBoolean(), any(), any(), any(), any(), any(),
                eq(0), eq(ContainerDependenciesHelper.TEMPLATE_PAGE_SIZE), any())).thenReturn(List.of(templates));
    }

    private void templateUses(final Template template, final Container used) throws Exception {
        when(templateAPI.getContainersInTemplate(template, systemUser, false)).thenReturn(List.of(used));
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
