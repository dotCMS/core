package com.dotcms.rest.api.v1.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests {@link ContainerResource#getDependencies(HttpServletRequest, HttpServletResponse, List)}
 * against real Containers, Templates and permissions.
 */
public class ContainerResourceDependenciesIntegrationTest {

    private static final String PASSWORD = "admin";

    private static ContainerResource resource;
    private static HttpServletResponse response;
    private static User adminUser;
    private static Host site;

    private static final List<Template> createdTemplates = new ArrayList<>();
    private static final List<Container> createdContainers = new ArrayList<>();
    private static final List<User> createdUsers = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        resource = new ContainerResource();
        response = new MockHttpResponse();
        adminUser = TestUserUtils.getAdminUser();
        site = new SiteDataGen().nextPersisted();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        createdTemplates.forEach(TemplateDataGen::remove);
        createdContainers.forEach(ContainerDataGen::remove);
        createdUsers.forEach(UserDataGen::remove);
        if (null != site) {
            APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
            APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
        }
    }

    private static HttpServletRequest getHttpRequest(final User user) {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());

        final String userEmailAndPassword = user.getEmailAddress() + ":" + PASSWORD;
        request.setHeader("Authorization",
                "Basic " + new String(Base64.encode(userEmailAndPassword.getBytes())));
        return request;
    }

    private static Container newContainer(final ContainerDataGen dataGen) {
        final Container container = dataGen.site(site).nextPersisted();
        createdContainers.add(container);
        return container;
    }

    private static Template newTemplateUsing(final Container container, final String title) {
        final Template template = new TemplateDataGen().site(site).title(title).drawed(true)
                .withContainer(container.getIdentifier()).nextPersisted();
        createdTemplates.add(template);
        return template;
    }

    private static User newBackendUser() throws DotDataException, DotSecurityException {
        final User user = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        user.setPassword(PASSWORD);
        APILocator.getUserAPI().save(user, APILocator.systemUser(), false);
        createdUsers.add(user);
        return user;
    }

    private static void grantRead(final User user, final Permissionable permissionable)
            throws DotDataException, DotSecurityException {
        final Permission permission = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                permissionable.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(user.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ, true);
        APILocator.getPermissionAPI().save(permission, permissionable, APILocator.systemUser(), false);
    }

    private static boolean canRead(final User user, final Permissionable permissionable)
            throws DotDataException {
        return APILocator.getPermissionAPI().doesUserHavePermission(permissionable,
                PermissionAPI.PERMISSION_READ, user, false);
    }

    private static List<ContainerDependenciesView> getDependencies(final User user, final String... inodes)
            throws DotDataException, DotSecurityException {
        return resource.getDependencies(getHttpRequest(user), response, List.of(inodes)).getEntity();
    }

    /**
     * Given scenario: A Container is used by a Template.
     * Expected result: The Container comes back with the Template's identifier, title and Site name.
     */
    @Test
    public void test_getDependencies_containerUsedByTemplate_returnsTheTemplate() throws Exception {
        final Container container = newContainer(new ContainerDataGen());
        final Template template = newTemplateUsing(container, "Template" + System.currentTimeMillis());

        final List<ContainerDependenciesView> dependencies = getDependencies(adminUser, container.getInode());

        assertEquals(1, dependencies.size());
        final ContainerDependenciesView dependency = dependencies.get(0);
        assertEquals(container.getIdentifier(), dependency.getIdentifier());
        assertEquals(container.getInode(), dependency.getInode());
        assertEquals(container.getTitle(), dependency.getTitle());
        assertEquals(1, dependency.getTemplates().size());
        final DependentTemplateView dependentTemplate = dependency.getTemplates().get(0);
        assertEquals(template.getIdentifier(), dependentTemplate.getIdentifier());
        assertEquals(template.getTitle(), dependentTemplate.getTitle());
        assertEquals(site.getHostname(), dependentTemplate.getSiteName());
    }

    /**
     * Given scenario: Two Containers are checked at once and only one of them is used by a Template.
     * Expected result: Only the Container in use is returned.
     */
    @Test
    public void test_getDependencies_severalContainers_returnsOnlyTheOnesInUse() throws Exception {
        final Container usedContainer = newContainer(new ContainerDataGen());
        final Container unusedContainer = newContainer(new ContainerDataGen());
        newTemplateUsing(usedContainer, "Template" + System.currentTimeMillis());

        final List<ContainerDependenciesView> dependencies = getDependencies(adminUser,
                unusedContainer.getInode(), usedContainer.getInode());

        assertEquals(1, dependencies.size());
        assertEquals(usedContainer.getIdentifier(), dependencies.get(0).getIdentifier());
    }

    /**
     * Given scenario: The Container and Template titles contain markup.
     * Expected result: The titles come back unchanged in their own JSON string fields, and nothing
     * else in the response carries markup, so the caller decides how they are displayed.
     */
    @Test
    public void test_getDependencies_titlesContainingMarkup_areReturnedAsPlainValues() throws Exception {
        final String containerTitle = "<b>Container</b> <img src=\"x.png\"> & co";
        final String templateTitle = "<i>Template</i> \"" + System.currentTimeMillis() + "\"";
        final Container container = newContainer(new ContainerDataGen().title(containerTitle));
        newTemplateUsing(container, templateTitle);

        final ResponseEntityContainerDependenciesView responseView = resource.getDependencies(
                getHttpRequest(adminUser), response, List.of(container.getInode()));

        final String json = DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
                .writeValueAsString(responseView);
        final Map<String, Object> parsed = DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
                .readValue(json, Map.class);
        final Map<String, Object> dependency = ((List<Map<String, Object>>) parsed.get("entity")).get(0);
        final Map<String, Object> template = ((List<Map<String, Object>>) dependency.get("templates")).get(0);
        assertEquals(containerTitle, dependency.get("title"));
        assertEquals(templateTitle, template.get("title"));
        assertEquals(site.getHostname(), template.get("siteName"));
        assertEquals("Only the title itself may contain markup", json.indexOf("<b>"), json.lastIndexOf("<b>"));
        assertFalse("No markup must be assembled on the server",
                json.contains("<li>") || json.contains("<ul>") || json.contains("<br"));
    }

    /**
     * Given scenario: A limited user can read the Container but not the Template that uses it.
     * Expected result: The Template is not disclosed, so no dependency is reported.
     */
    @Test
    public void test_getDependencies_userCannotReadTheTemplate_templateIsNotListed() throws Exception {
        final Container container = newContainer(new ContainerDataGen());
        final Template template = newTemplateUsing(container, "Template" + System.currentTimeMillis());
        final User limitedUser = newBackendUser();
        grantRead(limitedUser, container);
        assertFalse("Precondition: the user must not be able to read the Template", canRead(limitedUser, template));

        assertTrue(getDependencies(limitedUser, container.getInode()).isEmpty());
        assertEquals(1, getDependencies(adminUser, container.getInode()).size());
    }

    /**
     * Given scenario: A limited user can read both the Container and the Template that uses it.
     * Expected result: The Template is listed, and since the user cannot read the Template's Site,
     * its Site name is not disclosed.
     */
    @Test
    public void test_getDependencies_userCanReadTheTemplate_templateIsListed() throws Exception {
        final Container container = newContainer(new ContainerDataGen());
        final Template template = newTemplateUsing(container, "Template" + System.currentTimeMillis());
        final User limitedUser = newBackendUser();
        grantRead(limitedUser, container);
        grantRead(limitedUser, template);

        final List<ContainerDependenciesView> dependencies = getDependencies(limitedUser, container.getInode());

        assertEquals(1, dependencies.size());
        final DependentTemplateView dependentTemplate = dependencies.get(0).getTemplates().get(0);
        assertEquals(template.getIdentifier(), dependentTemplate.getIdentifier());
        assertFalse("Precondition: the user must not be able to read the Site", canRead(limitedUser, site));
        assertNull(dependentTemplate.getSiteName());
    }

    /**
     * Given scenario: A limited user without Read permission on the Container asks for its
     * dependencies.
     * Expected result: The request is rejected.
     */
    @Test(expected = DotSecurityException.class)
    public void test_getDependencies_userCannotReadTheContainer_isRejected() throws Exception {
        final Container container = newContainer(new ContainerDataGen());
        newTemplateUsing(container, "Template" + System.currentTimeMillis());
        final User limitedUser = newBackendUser();
        assertFalse("Precondition: the user must not be able to read the Container", canRead(limitedUser, container));

        getDependencies(limitedUser, container.getInode());
    }

    /**
     * Given scenario: The inode sent does not belong to any Container.
     * Expected result: The request fails as not found, rather than reporting no dependencies,
     * which would let the caller go ahead with the delete.
     */
    @Test(expected = DoesNotExistException.class)
    public void test_getDependencies_unknownInode_isNotFound() throws Exception {
        getDependencies(adminUser, UUIDGenerator.generateUuid());
    }

    /**
     * Given scenario: No Container inode is sent.
     * Expected result: The request is rejected as invalid.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_getDependencies_noInode_isRejected() throws Exception {
        getDependencies(adminUser);
    }
}
