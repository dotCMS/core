package com.dotcms.rest.api.v1.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.BulkResultView;
import com.dotcms.rest.exception.ConflictException;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@code GET /api/v1/containers/dependencies}
 * ({@link ContainerResource#getContainerDependencies}), the lookup the legacy Containers portlet
 * runs before deleting a container.
 */
public class ContainerResourceDependenciesIntegrationTest {

    private static final String PASSWORD = "admin";

    private static ContainerResource resource;
    private static HttpServletResponse response;
    private static User adminUser;
    private static final List<Template> templates = new ArrayList<>();
    private static final List<Container> containers = new ArrayList<>();
    private static final List<User> users = new ArrayList<>();
    private static final List<Host> sites = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new ContainerResource();
        adminUser = TestUserUtils.getAdminUser();
        response = new MockHttpResponse();
    }

    @AfterClass
    public static void cleanUp() {
        templates.forEach(template -> Try.run(() -> TemplateDataGen.remove(template))
                .onFailure(e -> Logger.warn(ContainerResourceDependenciesIntegrationTest.class,
                        "Could not remove template " + template.getIdentifier(), e)));
        containers.forEach(container -> Try.run(() -> ContainerDataGen.remove(container))
                .onFailure(e -> Logger.warn(ContainerResourceDependenciesIntegrationTest.class,
                        "Could not remove container " + container.getIdentifier(), e)));
        users.forEach(user -> Try.run(() -> UserDataGen.remove(user))
                .onFailure(e -> Logger.warn(ContainerResourceDependenciesIntegrationTest.class,
                        "Could not remove user " + user.getUserId(), e)));
        sites.forEach(site -> Try.run(() -> {
            APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
            APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
        }).onFailure(e -> Logger.warn(ContainerResourceDependenciesIntegrationTest.class,
                "Could not remove site " + site.getIdentifier(), e)));
    }

    private static Host defaultSite() throws Exception {
        return APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
    }

    private static Host freshSite() {
        final Host site = new SiteDataGen().nextPersisted();
        sites.add(site);
        return site;
    }

    /**
     * Method to test: {@link ContainerResource#getContainerDependencies}
     * Given Scenario: a container used by one drawed template, and a second template on the same
     * site that uses another container
     * ExpectedResult: 200 with the container's data and only the template that uses it, with the
     * site's host name; the same answer whether the container is looked up by inode or identifier
     */
    @Test
    public void listsTheTemplateThatUsesTheContainer() throws Exception {
        final Host site = defaultSite();
        final Container container = persistContainer(site, "Container " + System.currentTimeMillis());
        final Container other = persistContainer(site, "Other " + System.currentTimeMillis());
        final Template using = persistTemplate(site, "Using " + System.currentTimeMillis(), container);
        persistTemplate(site, "Unrelated " + System.currentTimeMillis(), other);

        for (final String containerId : List.of(container.getInode(), container.getIdentifier())) {
            final Response result = resource.getContainerDependencies(
                    request(adminUser.getEmailAddress()), response, List.of(containerId));

            assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
            final ContainerDependenciesView view = entity(result);
            assertEquals(container.getInode(), view.inode());
            assertEquals(container.getIdentifier(), view.identifier());
            assertEquals(container.getTitle(), view.title());
            assertEquals(1, view.templates().size());
            assertEquals(using.getIdentifier(), view.templates().get(0).identifier());
            assertEquals(using.getTitle(), view.templates().get(0).title());
            assertEquals(site.getHostname(), view.templates().get(0).siteName());
        }
    }

    /**
     * Method to test: {@link ContainerResource#getContainerDependencies}
     * Given Scenario: a container that no template uses
     * ExpectedResult: 200 with an empty templates list
     */
    @Test
    public void returnsAnEmptyListForAnUnusedContainer() throws Exception {
        final Host site = defaultSite();
        final Container container = persistContainer(site, "Unused " + System.currentTimeMillis());

        final Response result = resource.getContainerDependencies(
                request(adminUser.getEmailAddress()), response, List.of(container.getInode()));

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        assertTrue(entity(result).templates().isEmpty());
    }

    /**
     * Method to test: {@link ContainerResource#getContainerDependencies}
     * Given Scenario: a back-end user without READ permission on the container
     * ExpectedResult: the lookup is refused with a {@link DotSecurityException}
     */
    @Test
    public void requiresReadPermissionOnTheContainer() throws Exception {
        final Host site = defaultSite();
        final Container container = persistContainer(site, "Private " + System.currentTimeMillis());
        final User limitedUser = user(TestUserUtils.getBackendRole());

        assertThrows(DotSecurityException.class, () -> resource.getContainerDependencies(
                request(limitedUser.getEmailAddress()), response, List.of(container.getInode())));
    }

    /**
     * Method to test: {@link ContainerResource#getContainerDependencies}
     * Given Scenario: a back-end user who can read the container and one of the two templates that
     * use it, but not the other template nor the site
     * ExpectedResult: 200 listing only the readable template, with a null site name
     */
    @Test
    public void listsOnlyTheTemplatesTheUserCanRead() throws Exception {
        final Host site = freshSite();
        final Container container = persistContainer(site, "Shared " + System.currentTimeMillis());
        persistTemplate(site, "Hidden " + System.currentTimeMillis(), container);
        final Template visible = persistTemplate(site, "Visible " + System.currentTimeMillis(), container);
        final User limitedUser = user(TestUserUtils.getBackendRole());
        grantRead(limitedUser, container);
        grantRead(limitedUser, visible);

        final Response result = resource.getContainerDependencies(
                request(limitedUser.getEmailAddress()), response, List.of(container.getInode()));

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        final ContainerDependenciesView view = entity(result);
        assertEquals(1, view.templates().size());
        assertEquals(visible.getIdentifier(), view.templates().get(0).identifier());
        assertNull(view.templates().get(0).siteName());
    }

    /**
     * Method to test: {@link ContainerResource#getContainerDependencies}
     * Given Scenario: a back-end user who can read the container and a template that uses it,
     * while that template also uses a second container the user cannot read
     * ExpectedResult: 200 listing the template; the other container does not fail the lookup
     */
    @Test
    public void listsATemplateThatAlsoUsesAContainerTheUserCannotRead() throws Exception {
        final Host site = freshSite();
        final Container container = persistContainer(site, "Readable " + System.currentTimeMillis());
        final Container other = persistContainer(site, "Hidden " + System.currentTimeMillis());
        final Template template = new TemplateDataGen().site(site).title("Both " + System.currentTimeMillis())
                .drawed(true).withContainer(container, "1").withContainer(other, "2").nextPersisted();
        templates.add(template);
        final User limitedUser = user(TestUserUtils.getBackendRole());
        grantRead(limitedUser, container);
        grantRead(limitedUser, template);

        final Response result = resource.getContainerDependencies(
                request(limitedUser.getEmailAddress()), response, List.of(container.getInode()));

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        final ContainerDependenciesView view = entity(result);
        assertEquals(1, view.templates().size());
        assertEquals(template.getIdentifier(), view.templates().get(0).identifier());
    }

    /**
     * Method to test: {@link ContainerResource#delete}
     * Given Scenario: a container that a template uses, and one that no template uses
     * ExpectedResult: the first is refused with a conflict and still exists; the second is deleted
     */
    @Test
    public void deleteRefusesAContainerATemplateUses() throws Exception {
        final Host site = defaultSite();
        final Container used = persistContainer(site, "Used " + System.currentTimeMillis());
        final Container free = persistContainer(site, "Free " + System.currentTimeMillis());
        persistTemplate(site, "Uses " + System.currentTimeMillis(), used);

        assertThrows(ConflictException.class, () -> resource.delete(
                request(adminUser.getEmailAddress()), response, used.getIdentifier()));
        assertNotNull(APILocator.getContainerAPI()
                .getWorkingContainerById(used.getIdentifier(), adminUser, false));

        final Response result = resource.delete(
                request(adminUser.getEmailAddress()), response, free.getIdentifier());

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        assertEquals(true, ((ResponseEntityView<?>) result.getEntity()).getEntity());
    }

    /**
     * Method to test: {@link ContainerResource#delete}
     * Given Scenario: a back-end user who can read a container but not write it, and the container is
     * used by templates the user cannot see
     * ExpectedResult: the delete is refused with a permission error, not with the 409 that would
     * disclose how many templates use it
     */
    @Test
    public void deleteWithoutWritePermissionIsForbiddenNotAConflict() throws Exception {
        final Host site = freshSite();
        final Container container = persistContainer(site, "ReadOnly " + System.currentTimeMillis());
        persistTemplate(site, "Hidden " + System.currentTimeMillis(), container);
        final User readOnly = user(TestUserUtils.getBackendRole());
        grantRead(readOnly, container);

        assertThrows(DotSecurityException.class, () -> resource.delete(
                request(readOnly.getEmailAddress()), response, container.getIdentifier()));
        assertNotNull(APILocator.getContainerAPI()
                .getWorkingContainerById(container.getIdentifier(), adminUser, false));
    }

    /**
     * Method to test: {@link ContainerResource#bulkDelete}
     * Given Scenario: a bulk delete of a container a template uses and one no template uses
     * ExpectedResult: the free container is deleted and the used one is reported as a failure
     */
    @Test
    public void bulkDeleteReportsAContainerATemplateUsesAsAFailure() throws Exception {
        final Host site = defaultSite();
        final Container used = persistContainer(site, "BulkUsed " + System.currentTimeMillis());
        final Container free = persistContainer(site, "BulkFree " + System.currentTimeMillis());
        persistTemplate(site, "BulkUses " + System.currentTimeMillis(), used);

        final Response result = resource.bulkDelete(request(adminUser.getEmailAddress()), response,
                List.of(used.getIdentifier(), free.getIdentifier()));

        final BulkResultView view = (BulkResultView) ((ResponseEntityView<?>) result.getEntity()).getEntity();
        assertEquals(Long.valueOf(1L), view.getSuccessCount());
        assertEquals(1, view.getFails().size());
        assertEquals(used.getIdentifier(), view.getFails().get(0).getElement());
    }

    /**
     * Method to test: {@link ContainerResource#getContainerDependencies}
     * Given Scenario: three containers asked for at once: one used by a template, one used only by
     * a page custom layout, and one unused
     * ExpectedResult: 200 with one view per container, in the order asked, each with its own templates
     */
    @Test
    public void answersSeveralContainersInOneRequest() throws Exception {
        final Host site = defaultSite();
        final Container usedByTemplate = persistContainer(site, "ByTemplate " + System.currentTimeMillis());
        final Container usedByLayout = persistContainer(site, "ByLayout " + System.currentTimeMillis());
        final Container unused = persistContainer(site, "Unused " + System.currentTimeMillis());
        final Template template = persistTemplate(site, "Uses " + System.currentTimeMillis(), usedByTemplate);
        final Template layout = persistPageCustomLayout(site, usedByLayout);

        final Response result = resource.getContainerDependencies(request(adminUser.getEmailAddress()), response,
                List.of(unused.getIdentifier(), usedByLayout.getIdentifier(), usedByTemplate.getInode()));

        final List<ContainerDependenciesView> views = entities(result);
        assertEquals(List.of(unused.getIdentifier(), usedByLayout.getIdentifier(), usedByTemplate.getIdentifier()),
                views.stream().map(ContainerDependenciesView::identifier).collect(Collectors.toList()));
        assertTrue(views.get(0).templates().isEmpty());
        assertEquals(layout.getIdentifier(), views.get(1).templates().get(0).identifier());
        assertEquals(template.getIdentifier(), views.get(2).templates().get(0).identifier());
    }

    /**
     * Method to test: {@link ContainerResource#getContainerDependencies}
     * Given Scenario: two containers asked for at once, the second one without READ permission
     * ExpectedResult: the whole request is refused with a {@link DotSecurityException}
     */
    @Test
    public void refusesTheWholeRequestWhenOneContainerCannotBeRead() throws Exception {
        final Host site = freshSite();
        final Container readable = persistContainer(site, "Readable " + System.currentTimeMillis());
        final Container private1 = persistContainer(site, "Private " + System.currentTimeMillis());
        final User limitedUser = user(TestUserUtils.getBackendRole());
        grantRead(limitedUser, readable);

        assertThrows(DotSecurityException.class, () -> resource.getContainerDependencies(
                request(limitedUser.getEmailAddress()), response,
                List.of(readable.getInode(), private1.getInode())));
    }

    /**
     * Method to test: {@link ContainerResource#getContainerDependencies}
     * Given Scenario: more container ids than the endpoint accepts
     * ExpectedResult: the request is rejected as a bad request
     */
    @Test
    public void rejectsMoreContainerIdsThanTheLimit() {
        final List<String> tooMany = IntStream.rangeClosed(1, ContainerResource.MAX_DEPENDENCY_LOOKUP + 1)
                .mapToObj(i -> "id-" + i).collect(Collectors.toList());

        assertThrows(IllegalArgumentException.class, () -> resource.getContainerDependencies(
                request(adminUser.getEmailAddress()), response, tooMany));
    }

    /**
     * Method to test: {@link ContainerResource#delete}
     * Given Scenario: a container used only by a page custom layout (a hidden
     * {@code anonymous_layout_} template)
     * ExpectedResult: the delete is refused with a conflict whose message is the one of
     * {@code Language.properties}, and the container still exists
     */
    @Test
    public void deleteRefusesAContainerOnlyAPageCustomLayoutUses() throws Exception {
        final Host site = defaultSite();
        final Container container = persistContainer(site, "Layout " + System.currentTimeMillis());
        persistPageCustomLayout(site, container);

        final ConflictException conflict = assertThrows(ConflictException.class, () -> resource.delete(
                request(adminUser.getEmailAddress()), response, container.getIdentifier()));

        assertEquals("The container is used by 1 template(s). Remove it from them before deleting it.",
                conflict.getMessage());
        assertNotNull(APILocator.getContainerAPI()
                .getWorkingContainerById(container.getIdentifier(), adminUser, false));
    }

    /**
     * Method to test: {@link ContainerResource#bulkDelete}
     * Given Scenario: a bulk delete of a container only a page custom layout uses and a free one
     * ExpectedResult: the free container is deleted and the other one is reported as a failure
     */
    @Test
    public void bulkDeleteReportsAContainerOnlyAPageCustomLayoutUsesAsAFailure() throws Exception {
        final Host site = defaultSite();
        final Container used = persistContainer(site, "BulkLayout " + System.currentTimeMillis());
        final Container free = persistContainer(site, "BulkFree " + System.currentTimeMillis());
        persistPageCustomLayout(site, used);

        final Response result = resource.bulkDelete(request(adminUser.getEmailAddress()), response,
                List.of(used.getIdentifier(), free.getIdentifier()));

        final BulkResultView view = (BulkResultView) ((ResponseEntityView<?>) result.getEntity()).getEntity();
        assertEquals(Long.valueOf(1L), view.getSuccessCount());
        assertEquals(used.getIdentifier(), view.getFails().get(0).getElement());
    }

    /**
     * Method to test: {@link ContainerResource#getContainerDependencies}
     * Given Scenario: a user holding only the front-end role, with READ on the container
     * ExpectedResult: the request is rejected because the endpoint requires a back-end user
     */
    @Test
    public void requiresABackendUser() throws Exception {
        final Host site = defaultSite();
        final Container container = persistContainer(site, "Front " + System.currentTimeMillis());
        final User frontendUser = user(TestUserUtils.getFrontendRole());
        grantRead(frontendUser, container);

        assertThrows(SecurityException.class, () -> resource.getContainerDependencies(
                request(frontendUser.getEmailAddress()), response, List.of(container.getInode())));
    }

    /**
     * Method to test: {@link ContainerResource#getContainerDependencies}
     * Given Scenario: a request without credentials, for an id that matches nothing
     * ExpectedResult: the request is rejected by the authentication gate before any lookup
     */
    @Test
    public void rejectsAnonymousRequests() {
        assertThrows(SecurityException.class, () -> resource.getContainerDependencies(
                request(null), response, List.of("no-such-container")));
    }

    private static Container persistContainer(final Host site, final String title) {
        final Container container = new ContainerDataGen().site(site).title(title).nextPersisted();
        containers.add(container);
        return container;
    }

    private static Template persistTemplate(final Host site, final String title, final Container container) {
        final Template template = new TemplateDataGen().site(site).title(title).drawed(true)
                .withContainer(container, "1").nextPersisted();
        templates.add(template);
        return template;
    }

    private static Template persistPageCustomLayout(final Host site, final Container container) {
        final Template layout = new TemplateDataGen().site(site)
                .title(Template.ANONYMOUS_PREFIX + System.nanoTime()).showOnMenu(false).drawed(true)
                .withContainer(container, "1").nextPersisted();
        templates.add(layout);
        return layout;
    }

    private static User user(final Role role) throws Exception {
        final User user = new UserDataGen().roles(role).nextPersisted();
        user.setPassword(PASSWORD);
        APILocator.getUserAPI().save(user, APILocator.systemUser(), false);
        users.add(user);
        return user;
    }

    private static void grantRead(final User user, final Permissionable asset) throws Exception {
        final Permission permission = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                asset.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(user.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ, true);
        APILocator.getPermissionAPI().save(permission, asset, APILocator.systemUser(), false);
    }

    @SuppressWarnings("unchecked")
    private static List<ContainerDependenciesView> entities(final Response result) {
        return (List<ContainerDependenciesView>) ((ResponseEntityView<?>) result.getEntity()).getEntity();
    }

    private static ContainerDependenciesView entity(final Response result) {
        final List<ContainerDependenciesView> views = entities(result);
        assertEquals(1, views.size());
        return views.get(0);
    }

    private static HttpServletRequest request(final String userEmail) {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());
        if (null != userEmail) {
            request.setHeader("Authorization",
                    "Basic " + new String(Base64.encode((userEmail + ":" + PASSWORD).getBytes())));
        }
        return request;
    }
}
