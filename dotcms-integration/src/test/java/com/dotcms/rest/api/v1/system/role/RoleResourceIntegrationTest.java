package com.dotcms.rest.api.v1.system.role;

import com.dotmarketing.exception.DoesNotExistException;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ConflictException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.ejb.UserTestUtil;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Integration tests for the {@link RoleResource} role-mutation endpoints introduced for the
 * Angular Roles &amp; Tools portlet migration (epic #36909).
 *
 * Covered here:
 * - PUT /api/v1/roles/{roleId} (update role + reparent) — issue #36936
 *
 * These tests invoke the resource directly with mock authenticated requests, following the
 * pattern established by {@code PermissionResourceIntegrationTest}.
 *
 * @author hassandotcms
 */
public class RoleResourceIntegrationTest {

    private static RoleResource resource;
    private static RoleAPI roleAPI;
    private static Host testHost;
    private static User limitedUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        resource = new RoleResource();
        roleAPI = APILocator.getRoleAPI();
        testHost = new SiteDataGen().nextPersisted();

        // Backend user WITHOUT roles-portlet access and WITHOUT the CMS admin role,
        // for authorization tests
        limitedUser = UserTestUtil.getUser("limiteduser", false, true);
        final Role backendRole = roleAPI.loadBackEndUserRole();
        if (!roleAPI.doesUserHaveRole(limitedUser, backendRole)) {
            roleAPI.addRoleToUser(backendRole, limitedUser);
        }
    }

    // ==================== Helpers ====================

    private static HttpServletRequest adminRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest(testHost.getHostname(), "/").request())
                                .request())
                        .request());

        request.setHeader("Authorization",
                "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));
        return request;
    }

    private static HttpServletRequest requestFor(final User user) {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest(testHost.getHostname(), "/").request())
                                .request())
                        .request());

        request.getSession().setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
        request.getSession().setAttribute(com.liferay.portal.util.WebKeys.USER, user);
        return request;
    }

    private static RoleForm.Builder formFrom(final Role role) {
        return new RoleForm.Builder()
                .roleName(role.getName())
                .roleKey(role.getRoleKey())
                .description(role.getDescription())
                .canEditUsers(role.isEditUsers())
                .canEditPermissions(role.isEditPermissions())
                .canEditLayouts(role.isEditLayouts());
    }

    private static String uniq() {
        return Long.toString(System.nanoTime());
    }

    // ==================== PUT /v1/roles/{roleId} — #36936 ====================

    /**
     * Method to test: {@link RoleResource#updateRole(HttpServletRequest, HttpServletResponse, String, RoleForm)}
     * Given Scenario: An admin updates every editable field of an existing role.
     * Expected Result: 200; the response carries the updated role map and the changes are persisted.
     */
    @Test
    public void testUpdateRole_success_updatesAllFields() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();

        final String newName = "updated-name-" + uniq();
        final String newKey = "updated-key-" + uniq();
        final String newDescription = "updated description";

        final RoleForm form = new RoleForm.Builder()
                .roleName(newName)
                .roleKey(newKey)
                .description(newDescription)
                .canEditUsers(false)
                .canEditPermissions(false)
                .canEditLayouts(false)
                .build();

        final ResponseEntityRoleDetailView view = resource.updateRole(
                adminRequest(), new MockHttpResponse().response(), role.getId(), form);

        final RoleView entity = view.getEntity();
        assertNotNull(entity);
        assertEquals(newName, entity.getName());
        assertEquals(newKey, entity.getRoleKey());
        assertFalse(entity.isEditUsers());

        final Role reloaded = roleAPI.loadRoleById(role.getId());
        assertEquals(newName, reloaded.getName());
        assertEquals(newKey, reloaded.getRoleKey());
        assertEquals(newDescription, reloaded.getDescription());
        assertFalse(reloaded.isEditUsers());
        assertFalse(reloaded.isEditPermissions());
        assertFalse(reloaded.isEditLayouts());
    }

    /**
     * Given Scenario: A child role is reparented under a different role.
     * Expected Result: 200; the persisted role's parent is the new parent's id.
     */
    @Test
    public void testUpdateRole_reparent_toAnotherRole() throws Exception {
        final Role oldParent = new RoleDataGen().nextPersisted();
        final Role newParent = new RoleDataGen().nextPersisted();
        final Role child = new RoleDataGen().parent(oldParent.getId()).nextPersisted();

        final RoleForm form = formFrom(child).parentRoleId(newParent.getId()).build();

        final ResponseEntityRoleDetailView view = resource.updateRole(
                adminRequest(), new MockHttpResponse().response(), child.getId(), form);

        assertEquals(newParent.getId(), view.getEntity().getParent());
        assertEquals(newParent.getId(), roleAPI.loadRoleById(child.getId()).getParent());
    }

    /**
     * Given Scenario: A child role is updated with a null parentRoleId.
     * Expected Result: 200; the role becomes a root role (parent == own id), matching legacy
     * DWR behavior (RoleAjax#updateRole).
     */
    @Test
    public void testUpdateRole_reparent_toRoot_whenParentNull() throws Exception {
        final Role parent = new RoleDataGen().nextPersisted();
        final Role child = new RoleDataGen().parent(parent.getId()).nextPersisted();

        final RoleForm form = formFrom(child).parentRoleId(null).build();

        final ResponseEntityRoleDetailView view = resource.updateRole(
                adminRequest(), new MockHttpResponse().response(), child.getId(), form);

        assertEquals(child.getId(), view.getEntity().getParent());
        assertEquals(child.getId(), roleAPI.loadRoleById(child.getId()).getParent());
    }

    /**
     * Given Scenario: A parent role is reparented under its own descendant (cycle).
     * Expected Result: 400 BadRequestException and the hierarchy is unchanged. This guard is
     * net-new vs legacy (the Dojo tree simply never offered the bad drop target).
     */
    @Test
    public void testUpdateRole_reparent_cycle_badRequest() throws Exception {
        final Role parent = new RoleDataGen().nextPersisted();
        final Role child = new RoleDataGen().parent(parent.getId()).nextPersisted();
        final String originalParentOfParent = parent.getParent();

        final RoleForm form = formFrom(parent).parentRoleId(child.getId()).build();

        try {
            resource.updateRole(adminRequest(), new MockHttpResponse().response(), parent.getId(), form);
            fail("Should have thrown BadRequestException for a hierarchy cycle");
        } catch (final BadRequestException e) {
            // expected
        }

        assertEquals(originalParentOfParent, roleAPI.loadRoleById(parent.getId()).getParent());
        assertEquals(parent.getId(), roleAPI.loadRoleById(child.getId()).getParent());
    }

    /**
     * Given Scenario: A role is reparented under itself.
     * Expected Result: 400 BadRequestException.
     */
    @Test(expected = BadRequestException.class)
    public void testUpdateRole_reparent_toSelf_badRequest() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();

        final RoleForm form = formFrom(role).parentRoleId(role.getId()).build();

        resource.updateRole(adminRequest(), new MockHttpResponse().response(), role.getId(), form);
    }

    /**
     * Given Scenario: An admin attempts to update a system role.
     * Expected Result: DotSecurityException (403). Legacy RoleAPIImpl.save blocks locked/system
     * roles; the endpoint surfaces it as a clean 403 instead of a 500.
     */
    @Test(expected = DotSecurityException.class)
    public void testUpdateRole_systemRole_forbidden() throws Exception {
        // a user's individual role is flagged system=true on creation (RoleFactoryImpl#addUserRole)
        final Role systemRole = roleAPI.getUserRole(limitedUser);
        assertTrue(systemRole.isSystem());

        final RoleForm form = formFrom(systemRole).roleName("renamed-" + uniq()).build();

        resource.updateRole(adminRequest(), new MockHttpResponse().response(), systemRole.getId(), form);
    }

    /**
     * Given Scenario: An admin attempts to update a locked role.
     * Expected Result: DotSecurityException (403).
     */
    @Test(expected = DotSecurityException.class)
    public void testUpdateRole_lockedRole_forbidden() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        roleAPI.lock(role);

        try {
            final RoleForm form = formFrom(role).roleName("renamed-" + uniq()).build();
            resource.updateRole(adminRequest(), new MockHttpResponse().response(), role.getId(), form);
        } finally {
            roleAPI.unLock(role);
        }
    }

    /**
     * Given Scenario: A role is updated to use another role's roleKey.
     * Expected Result: 409 ConflictException (DuplicateRoleKeyException from RoleAPIImpl.save).
     */
    @Test(expected = ConflictException.class)
    public void testUpdateRole_duplicateKey_conflict() throws Exception {
        final Role roleA = new RoleDataGen().key("key-a-" + uniq()).nextPersisted();
        final Role roleB = new RoleDataGen().key("key-b-" + uniq()).nextPersisted();

        final RoleForm form = formFrom(roleB).roleKey(roleA.getRoleKey()).build();

        resource.updateRole(adminRequest(), new MockHttpResponse().response(), roleB.getId(), form);
    }

    /**
     * Given Scenario: Two sibling roles under the same parent; one is renamed to the other's name.
     * Expected Result: 409 ConflictException (DuplicateRoleException from RoleAPIImpl.save).
     */
    @Test(expected = ConflictException.class)
    public void testUpdateRole_duplicateNameUnderSameParent_conflict() throws Exception {
        final Role parent = new RoleDataGen().nextPersisted();
        final Role roleA = new RoleDataGen().parent(parent.getId()).nextPersisted();
        final Role roleB = new RoleDataGen().parent(parent.getId()).nextPersisted();

        final RoleForm form = formFrom(roleB)
                .roleName(roleA.getName())
                .parentRoleId(parent.getId())
                .build();

        resource.updateRole(adminRequest(), new MockHttpResponse().response(), roleB.getId(), form);
    }

    /**
     * Given Scenario: A role is renamed to an invalid name (over the 100-char limit enforced by
     * RoleAPIImpl.save's RoleNameException).
     * Expected Result: 400 BadRequestException — a validation error, not a conflict.
     */
    @Test(expected = BadRequestException.class)
    public void testUpdateRole_invalidName_badRequest() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();

        final StringBuilder longName = new StringBuilder();
        for (int i = 0; i <= 100; i++) {
            longName.append('a');
        }
        final RoleForm form = formFrom(role).roleName(longName.toString()).build();

        resource.updateRole(adminRequest(), new MockHttpResponse().response(), role.getId(), form);
    }

    /**
     * Given Scenario: The roleId path parameter does not match any role.
     * Expected Result: 404 DoesNotExistException.
     */
    @Test(expected = DoesNotExistException.class)
    public void testUpdateRole_missingRole_notFound() throws Exception {
        final RoleForm form = new RoleForm.Builder().roleName("whatever-" + uniq()).build();

        resource.updateRole(adminRequest(), new MockHttpResponse().response(),
                UUID.randomUUID().toString(), form);
    }

    /**
     * Given Scenario: The parentRoleId in the form does not match any role.
     * Expected Result: 404 DoesNotExistException; the role is not modified.
     */
    @Test(expected = DoesNotExistException.class)
    public void testUpdateRole_missingParent_notFound() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();

        final RoleForm form = formFrom(role).parentRoleId(UUID.randomUUID().toString()).build();

        resource.updateRole(adminRequest(), new MockHttpResponse().response(), role.getId(), form);
    }

    /**
     * Given Scenario: A backend user without the roles portlet and without the CMS admin role
     * calls the endpoint.
     * Expected Result: rejected with a security exception (403); the role is not modified.
     */
    @Test
    public void testUpdateRole_nonAdmin_forbidden() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final String originalName = role.getName();

        final RoleForm form = formFrom(role).roleName("hacked-" + uniq()).build();

        try {
            resource.updateRole(requestFor(limitedUser),
                    new MockHttpResponse().response(), role.getId(), form);
            fail("Should have thrown a security exception");
        } catch (final DotSecurityException | com.dotcms.rest.exception.SecurityException e) {
            // expected: the InitBuilder portlet gate throws the REST SecurityException (→ 403),
            // the CMS-admin check throws DotSecurityException (→ 403)
        }

        assertEquals(originalName, roleAPI.loadRoleById(role.getId()).getName());
    }

    /**
     * Given Scenario: A backend user WITH access to the roles portlet but WITHOUT the CMS admin
     * role calls the endpoint. This exercises the CMS-admin gate specifically, as opposed to the
     * portlet gate covered by {@link #testUpdateRole_nonAdmin_forbidden()}.
     * Expected Result: rejected with a security exception (403); the role is not modified.
     */
    @Test
    public void testUpdateRole_rolesPortletUserWithoutAdmin_forbidden() throws Exception {
        final com.dotmarketing.business.Layout rolesLayout =
                new com.dotcms.datagen.LayoutDataGen().portletIds("roles").nextPersisted();
        final Role portletRole = new RoleDataGen().layout(rolesLayout).nextPersisted();
        final User portletUser = UserTestUtil.getUser("rolesportletuser" + uniq(), false, true);
        roleAPI.addRoleToUser(roleAPI.loadBackEndUserRole(), portletUser);
        roleAPI.addRoleToUser(portletRole, portletUser);

        final Role role = new RoleDataGen().nextPersisted();
        final String originalName = role.getName();
        final RoleForm form = formFrom(role).roleName("hacked-" + uniq()).build();

        try {
            resource.updateRole(requestFor(portletUser),
                    new MockHttpResponse().response(), role.getId(), form);
            fail("Should have thrown a security exception for a non-admin caller");
        } catch (final DotSecurityException | com.dotcms.rest.exception.SecurityException e) {
            // expected: the CMS-admin check
        }

        assertEquals(originalName, roleAPI.loadRoleById(role.getId()).getName());
    }

    /**
     * Given Scenario: Regression guard — PR #36936 extracts the shared auth gate out of
     * {@link RoleResource#addNewRole}. Creating a role through POST must keep working unchanged.
     * Expected Result: 200; the role is persisted with the submitted fields.
     */
    @Test
    public void testAddNewRole_regression_createStillWorks() throws Exception {
        final String name = "created-role-" + uniq();

        final RoleForm form = new RoleForm.Builder()
                .roleName(name)
                .roleKey("created-key-" + uniq())
                .description("created by regression test")
                .canEditUsers(true)
                .canEditPermissions(true)
                .canEditLayouts(true)
                .build();

        final Response restResponse = resource.addNewRole(
                adminRequest(), new MockHttpResponse().response(), form);

        assertEquals(200, restResponse.getStatus());
        final Map<String, Object> entity =
                ((RoleResponseEntityView) restResponse.getEntity()).getEntity();
        assertNotNull(entity.get("id"));
        assertEquals(name, roleAPI.loadRoleById((String) entity.get("id")).getName());
    }
}
