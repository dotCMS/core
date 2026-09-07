package com.dotcms.rest.api.v1.system.role;

import com.dotmarketing.exception.DoesNotExistException;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.WorkflowActionDataGen;
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.datagen.WorkflowStepDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ConflictException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.ejb.UserTestUtil;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
 * - DELETE /api/v1/roles/{roleId} (delete role, cascading) — issue #36939
 * - POST /api/v1/roles/{roleId}/users/{userId} (grant user to role) — issue #36937
 * - DELETE /api/v1/roles/{roleId}/users (bulk remove members) — issue #36938
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

    private static HttpServletRequest anonymousRequest() {
        return new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest(testHost.getHostname(), "/").request())
                                .request())
                        .request());
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
        // pre-warm the role cache OUTSIDE the update's transaction — models production, where
        // the edited role is already cached; entries created inside the failing transaction
        // are rollback-evicted (CommitListenerCacheWrapper.put) and would mask the poisoning
        roleAPI.loadRoleById(parent.getId());

        final String originalParentOfParent = parent.getParent();
        final String originalName = parent.getName();
        final String originalKey = parent.getRoleKey();
        final String originalDescription = parent.getDescription();

        // the form also renames, so the post-rejection assertions can detect any partial mutation
        final RoleForm form = formFrom(parent)
                .roleName("cycle-rename-" + uniq())
                .parentRoleId(child.getId())
                .build();

        try {
            resource.updateRole(adminRequest(), new MockHttpResponse().response(), parent.getId(), form);
            fail("Should have thrown BadRequestException for a hierarchy cycle");
        } catch (final BadRequestException e) {
            // expected
        }

        final Role reloaded = roleAPI.loadRoleById(parent.getId());
        assertEquals(originalName, reloaded.getName());
        assertEquals(originalKey, reloaded.getRoleKey());
        assertEquals(originalDescription, reloaded.getDescription());
        assertEquals(originalParentOfParent, reloaded.getParent());
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
    @Test
    public void testUpdateRole_duplicateKey_conflict() throws Exception {
        final Role roleA = new RoleDataGen().key("key-a-" + uniq()).nextPersisted();
        final Role roleB = new RoleDataGen().key("key-b-" + uniq()).nextPersisted();

        // pre-warm the role cache OUTSIDE the update's transaction — models production, where
        // the edited role is already cached; entries created inside the failing transaction
        // are rollback-evicted (CommitListenerCacheWrapper.put) and would mask the poisoning
        roleAPI.loadRoleById(roleB.getId());

        final String originalName = roleB.getName();
        final String originalKey = roleB.getRoleKey();
        final String originalDescription = roleB.getDescription();

        final RoleForm form = formFrom(roleB)
                .roleName("dup-key-rename-" + uniq())
                .roleKey(roleA.getRoleKey())
                .build();

        try {
            resource.updateRole(adminRequest(), new MockHttpResponse().response(), roleB.getId(), form);
            fail("Should have thrown ConflictException for a duplicate role key");
        } catch (final ConflictException e) {
            // expected
        }

        // a rejected update must leave no trace — not in the DB and not in the role cache
        final Role reloaded = roleAPI.loadRoleById(roleB.getId());
        assertEquals(originalName, reloaded.getName());
        assertEquals(originalKey, reloaded.getRoleKey());
        assertEquals(originalDescription, reloaded.getDescription());
    }

    /**
     * Given Scenario: Two sibling roles under the same parent; one is renamed to the other's name.
     * Expected Result: 409 ConflictException (DuplicateRoleException from RoleAPIImpl.save).
     */
    @Test
    public void testUpdateRole_duplicateNameUnderSameParent_conflict() throws Exception {
        final Role parent = new RoleDataGen().nextPersisted();
        final Role roleA = new RoleDataGen().parent(parent.getId()).nextPersisted();
        final Role roleB = new RoleDataGen().parent(parent.getId()).nextPersisted();

        // pre-warm the role cache OUTSIDE the update's transaction — models production, where
        // the edited role is already cached; entries created inside the failing transaction
        // are rollback-evicted (CommitListenerCacheWrapper.put) and would mask the poisoning
        roleAPI.loadRoleById(roleB.getId());

        final String originalName = roleB.getName();
        final String originalKey = roleB.getRoleKey();
        final String originalDescription = roleB.getDescription();

        final RoleForm form = formFrom(roleB)
                .roleName(roleA.getName())
                .parentRoleId(parent.getId())
                .build();

        try {
            resource.updateRole(adminRequest(), new MockHttpResponse().response(), roleB.getId(), form);
            fail("Should have thrown ConflictException for a duplicate role name");
        } catch (final ConflictException e) {
            // expected
        }

        final Role reloaded = roleAPI.loadRoleById(roleB.getId());
        assertEquals(originalName, reloaded.getName());
        assertEquals(originalKey, reloaded.getRoleKey());
        assertEquals(originalDescription, reloaded.getDescription());
    }

    /**
     * Given Scenario: A role is renamed to an invalid name (over the 100-char limit enforced by
     * RoleAPIImpl.save's RoleNameException).
     * Expected Result: 400 BadRequestException — a validation error, not a conflict.
     */
    @Test
    public void testUpdateRole_invalidName_badRequest() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();

        final StringBuilder longName = new StringBuilder();
        for (int i = 0; i <= 100; i++) {
            longName.append('a');
        }
        // pre-warm the role cache OUTSIDE the update's transaction — models production, where
        // the edited role is already cached; entries created inside the failing transaction
        // are rollback-evicted (CommitListenerCacheWrapper.put) and would mask the poisoning
        roleAPI.loadRoleById(role.getId());

        final String originalName = role.getName();
        final String originalKey = role.getRoleKey();
        final String originalDescription = role.getDescription();

        final RoleForm form = formFrom(role).roleName(longName.toString()).build();

        try {
            resource.updateRole(adminRequest(), new MockHttpResponse().response(), role.getId(), form);
            fail("Should have thrown BadRequestException for an invalid role name");
        } catch (final BadRequestException e) {
            // expected
        }

        final Role reloaded = roleAPI.loadRoleById(role.getId());
        assertEquals(originalName, reloaded.getName());
        assertEquals(originalKey, reloaded.getRoleKey());
        assertEquals(originalDescription, reloaded.getDescription());
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
    @Test
    public void testUpdateRole_missingParent_notFound() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();

        // pre-warm the role cache OUTSIDE the update's transaction — models production, where
        // the edited role is already cached; entries created inside the failing transaction
        // are rollback-evicted (CommitListenerCacheWrapper.put) and would mask the poisoning
        roleAPI.loadRoleById(role.getId());

        final String originalName = role.getName();
        final String originalKey = role.getRoleKey();
        final String originalDescription = role.getDescription();
        final String originalParent = role.getParent();

        final RoleForm form = formFrom(role)
                .roleName("missing-parent-rename-" + uniq())
                .parentRoleId(UUID.randomUUID().toString())
                .build();

        try {
            resource.updateRole(adminRequest(), new MockHttpResponse().response(), role.getId(), form);
            fail("Should have thrown DoesNotExistException for a missing parent role");
        } catch (final DoesNotExistException e) {
            // expected
        }

        final Role reloaded = roleAPI.loadRoleById(role.getId());
        assertEquals(originalName, reloaded.getName());
        assertEquals(originalKey, reloaded.getRoleKey());
        assertEquals(originalDescription, reloaded.getDescription());
        assertEquals(originalParent, reloaded.getParent());
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
     * Given Scenario: PUT is a full replace — a minimal form (only the required roleName) is
     * sent for a role that has key, description, can-grant flags, and a parent.
     * Expected Result: every omitted field is overwritten: flags reset to false, roleKey and
     * description become null, and the role is reparented to root. This pins the documented
     * full-replace contract (clients must send the complete role representation) so any future
     * drift to merge/PATCH semantics is a deliberate, test-breaking change.
     */
    @Test
    public void testUpdateRole_fullReplace_omittedFieldsAreReset() throws Exception {
        final Role parent = new RoleDataGen().nextPersisted();
        final Role role = new RoleDataGen()
                .parent(parent.getId())
                .key("full-replace-key-" + uniq())
                .description("full replace description")
                .editUsers(true)
                .editPermissions(true)
                .editLayouts(true)
                .nextPersisted();

        final RoleForm minimalForm = new RoleForm.Builder()
                .roleName(role.getName())
                .build();

        resource.updateRole(adminRequest(), new MockHttpResponse().response(), role.getId(), minimalForm);

        final Role reloaded = roleAPI.loadRoleById(role.getId());
        assertFalse(reloaded.isEditUsers());
        assertFalse(reloaded.isEditPermissions());
        assertFalse(reloaded.isEditLayouts());
        // the persistence layer normalizes omitted (null) values to empty strings
        assertFalse(UtilMethods.isSet(reloaded.getRoleKey()));
        assertFalse(UtilMethods.isSet(reloaded.getDescription()));
        assertEquals(role.getId(), reloaded.getParent());
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

    // ==================== DELETE /v1/roles/{roleId} — #36939 ====================

    /**
     * Method to test: {@link RoleResource#deleteRole(HttpServletRequest, HttpServletResponse, String)}
     * Given Scenario: An admin deletes a leaf role with no users, permissions, layouts, children,
     * or workflow-action references.
     * Expected Result: 200 with {deleted: true, roleId, usersAffected: 0}; the role no longer exists.
     */
    @Test
    public void testDeleteRole_success_leafRole() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();

        final ResponseEntityRoleDeletionView view = resource.deleteRole(
                adminRequest(), new MockHttpResponse().response(), role.getId());

        final RoleDeletionView entity = view.getEntity();
        assertNotNull(entity);
        assertTrue(entity.deleted());
        assertEquals(role.getId(), entity.roleId());
        assertEquals(0, entity.usersAffected());

        final Role reloaded = roleAPI.loadRoleById(role.getId());
        assertTrue(null == reloaded || !UtilMethods.isSet(reloaded.getId()));
    }

    /**
     * Given Scenario: The role being deleted is assigned to a user, grants a permission on a host,
     * and has a layout attached. This mirrors what legacy RoleAPIImpl.delete has always done: it
     * CASCADES — removes the role from every user, strips its permissions, detaches its layouts,
     * then deletes.
     * Expected Result: 200 (NOT 409 — deleting a role means revoking that access, legacy parity),
     * usersAffected reports the blast radius, and every dependent row is gone.
     *
     * ⚠️ This test intentionally pins the cascade. If it starts failing because the endpoint began
     * blocking on assigned users, that is removed functionality, not a fix — see #36939 decisions.
     */
    @Test
    public void testDeleteRole_withUsersAssigned_cascades() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();

        final User member = UserTestUtil.getUser("cascadeuser" + uniq(), false, true);
        roleAPI.addRoleToUser(role, member);
        assertTrue(roleAPI.doesUserHaveRole(member, role));

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        permissionAPI.save(
                new Permission(testHost.getPermissionId(), role.getId(), PermissionAPI.PERMISSION_READ, true),
                testHost, APILocator.systemUser(), false);
        assertFalse(permissionAPI.getPermissionsByRole(role, false).isEmpty());

        final Layout layout = new LayoutDataGen().nextPersisted();
        roleAPI.addLayoutToRole(layout, role);
        assertFalse(APILocator.getLayoutAPI().loadLayoutsForRole(role).isEmpty());

        final ResponseEntityRoleDeletionView view = resource.deleteRole(
                adminRequest(), new MockHttpResponse().response(), role.getId());

        final RoleDeletionView entity = view.getEntity();
        assertTrue(entity.deleted());
        assertEquals(1, entity.usersAffected());

        final Role reloaded = roleAPI.loadRoleById(role.getId());
        assertTrue(null == reloaded || !UtilMethods.isSet(reloaded.getId()));
        assertFalse(roleAPI.doesUserHaveRole(member, role.getId()));
        assertTrue(permissionAPI.getPermissionsByRole(role, false).isEmpty());
        assertTrue(APILocator.getLayoutAPI().loadLayoutsForRole(role).isEmpty());
    }

    /**
     * Given Scenario: The role has a child role.
     * Expected Result: 409 ConflictException reporting the child count; neither role is modified.
     * Legacy parity: DWR RoleAjax#deleteRole silently returns false for roles with children —
     * the pre-flight surfaces the same block as a structured conflict.
     */
    @Test
    public void testDeleteRole_withChildren_conflict() throws Exception {
        final Role parent = new RoleDataGen().nextPersisted();
        final Role child = new RoleDataGen().parent(parent.getId()).nextPersisted();

        try {
            resource.deleteRole(adminRequest(), new MockHttpResponse().response(), parent.getId());
            fail("Should have thrown ConflictException for a role with children");
        } catch (final ConflictException e) {
            assertTrue("message should mention children: " + e.getMessage(),
                    e.getMessage().toLowerCase().contains("child"));
        }

        assertNotNull(roleAPI.loadRoleById(parent.getId()));
        assertEquals(parent.getId(), roleAPI.loadRoleById(child.getId()).getParent());
    }

    /**
     * Given Scenario: A workflow action's "Assign To" (nextAssign) references the role.
     * Expected Result: 409 ConflictException naming the workflow scheme and action; the role
     * still exists. This dependency is enforced by RoleAPIImpl#findDependentWorkflowActions, but
     * that check runs inside delete()'s catch(Exception) which re-wraps it into a generic
     * DotDataException — the endpoint must pre-check it to produce this structured 409.
     */
    @Test
    public void testDeleteRole_referencedByWorkflowAction_conflict() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();

        final WorkflowScheme scheme = new WorkflowDataGen()
                .name("delete-role-scheme-" + uniq()).nextPersisted();
        final WorkflowStep step = new WorkflowStepDataGen(scheme.getId()).nextPersisted();
        final WorkflowAction action = new WorkflowActionDataGen(scheme.getId(), step.getId())
                .nextAssign(role.getId())
                .nextPersisted();

        try {
            resource.deleteRole(adminRequest(), new MockHttpResponse().response(), role.getId());
            fail("Should have thrown ConflictException for a role referenced by a workflow action");
        } catch (final ConflictException e) {
            assertTrue("message should name the scheme: " + e.getMessage(),
                    e.getMessage().contains(scheme.getName()));
            assertTrue("message should name the action: " + e.getMessage(),
                    e.getMessage().contains(action.getName()));
        }

        assertNotNull(roleAPI.loadRoleById(role.getId()));
    }

    /**
     * Given Scenario: An admin attempts to delete a system role (a user's individual role is
     * flagged system=true on creation).
     * Expected Result: DotSecurityException (403); the role still exists.
     */
    @Test
    public void testDeleteRole_systemRole_forbidden() throws Exception {
        final Role systemRole = roleAPI.getUserRole(limitedUser);
        assertTrue(systemRole.isSystem());

        try {
            resource.deleteRole(adminRequest(), new MockHttpResponse().response(), systemRole.getId());
            fail("Should have thrown DotSecurityException for a system role");
        } catch (final DotSecurityException e) {
            // expected
        }

        assertNotNull(roleAPI.loadRoleById(systemRole.getId()));
    }

    /**
     * Given Scenario: An admin attempts to delete a locked role.
     * Expected Result: DotSecurityException (403); the role still exists. Legacy
     * RoleAPIImpl.delete blocks locked roles with a DotStateException (~500); the pre-flight
     * surfaces it as a clean 403.
     */
    @Test
    public void testDeleteRole_lockedRole_forbidden() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        roleAPI.lock(role);

        try {
            resource.deleteRole(adminRequest(), new MockHttpResponse().response(), role.getId());
            fail("Should have thrown DotSecurityException for a locked role");
        } catch (final DotSecurityException e) {
            // expected
        } finally {
            roleAPI.unLock(role);
        }

        assertNotNull(roleAPI.loadRoleById(role.getId()));
    }

    /**
     * Given Scenario: The roleId path parameter does not match any role.
     * Expected Result: 404 DoesNotExistException (resource-wide convention).
     */
    @Test(expected = DoesNotExistException.class)
    public void testDeleteRole_missingRole_notFound() throws Exception {
        resource.deleteRole(adminRequest(), new MockHttpResponse().response(),
                UUID.randomUUID().toString());
    }

    /**
     * Given Scenario: An anonymous caller (no session user, no Authorization header) calls the
     * endpoint.
     * Expected Result: rejected by the InitBuilder's rejectWhenNoUser gate (401); the role
     * still exists.
     */
    @Test
    public void testDeleteRole_anonymous_unauthorized() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();

        try {
            resource.deleteRole(anonymousRequest(),
                    new MockHttpResponse().response(), role.getId());
            fail("Should have thrown a security exception for an anonymous caller");
        } catch (final com.dotcms.rest.exception.SecurityException e) {
            // expected: rejectWhenNoUser → 401
        }

        assertNotNull(roleAPI.loadRoleById(role.getId()));
    }

    /**
     * Given Scenario: A backend user without the roles portlet and without the CMS admin role
     * calls the endpoint.
     * Expected Result: rejected with a security exception (403); the role still exists.
     */
    @Test
    public void testDeleteRole_nonAdmin_forbidden() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();

        try {
            resource.deleteRole(requestFor(limitedUser),
                    new MockHttpResponse().response(), role.getId());
            fail("Should have thrown a security exception");
        } catch (final DotSecurityException | com.dotcms.rest.exception.SecurityException e) {
            // expected: the InitBuilder portlet gate throws the REST SecurityException (→ 403),
            // the CMS-admin check throws DotSecurityException (→ 403)
        }

        assertNotNull(roleAPI.loadRoleById(role.getId()));
    }

    /**
     * Given Scenario: A backend user WITH access to the roles portlet but WITHOUT the CMS admin
     * role calls the endpoint — exercises the CMS-admin gate specifically.
     * Expected Result: rejected with a security exception (403); the role still exists.
     */
    @Test
    public void testDeleteRole_rolesPortletUserWithoutAdmin_forbidden() throws Exception {
        final Layout rolesLayout = new LayoutDataGen().portletIds("roles").nextPersisted();
        final Role portletRole = new RoleDataGen().layout(rolesLayout).nextPersisted();
        final User portletUser = UserTestUtil.getUser("rolesdeleteuser" + uniq(), false, true);
        roleAPI.addRoleToUser(roleAPI.loadBackEndUserRole(), portletUser);
        roleAPI.addRoleToUser(portletRole, portletUser);

        final Role role = new RoleDataGen().nextPersisted();

        try {
            resource.deleteRole(requestFor(portletUser),
                    new MockHttpResponse().response(), role.getId());
            fail("Should have thrown a security exception for a non-admin caller");
        } catch (final DotSecurityException | com.dotcms.rest.exception.SecurityException e) {
            // expected: the CMS-admin check
        }

        assertNotNull(roleAPI.loadRoleById(role.getId()));
    }

    // ==================== POST /v1/roles/{roleId}/users/{userId} — #36937 ====================

    private static boolean isDirectMember(final Role role, final User user) throws Exception {
        return roleAPI.findUsersForRole(role, false).stream()
                .anyMatch(u -> u.getUserId().equals(user.getUserId()));
    }

    /**
     * Method to test: {@link RoleResource#addUserToRole(HttpServletRequest, HttpServletResponse, String, String)}
     * Given Scenario: An admin grants a grantable role (editUsers=true) to a user who does not
     * hold it.
     * Expected Result: 200 with {granted: true, roleId, user: {userId, email, fullName}}; the
     * user becomes a DIRECT member of the role.
     */
    @Test
    public void testAddUserToRole_success_grantsDirectMembership() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final User target = UserTestUtil.getUser("grantuser" + uniq(), false, true);

        final ResponseEntityRoleUserGrantView view = resource.addUserToRole(
                adminRequest(), new MockHttpResponse().response(), role.getId(), target.getUserId());

        final RoleUserGrantView entity = view.getEntity();
        assertNotNull(entity);
        assertTrue(entity.granted());
        assertEquals(role.getId(), entity.roleId());
        assertEquals(target.getUserId(), entity.user().userId());
        assertEquals(target.getEmailAddress(), entity.user().email());
        assertEquals(target.getFullName(), entity.user().fullName());

        assertTrue("user must be a DIRECT member after grant", isDirectMember(role, target));
    }

    /**
     * Given Scenario: The same grant is issued twice (user is already a direct member).
     * Expected Result: 200 both times — the endpoint is idempotent (legacy
     * RoleAPIImpl.addRoleToUser silently no-ops when doesUserHaveRole is true). No duplicate
     * membership row is created.
     */
    @Test
    public void testAddUserToRole_alreadyDirectMember_idempotent() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final User target = UserTestUtil.getUser("regrantuser" + uniq(), false, true);

        resource.addUserToRole(
                adminRequest(), new MockHttpResponse().response(), role.getId(), target.getUserId());
        final ResponseEntityRoleUserGrantView view = resource.addUserToRole(
                adminRequest(), new MockHttpResponse().response(), role.getId(), target.getUserId());

        assertTrue(view.getEntity().granted());

        final long memberships = roleAPI.findUsersForRole(role, false).stream()
                .filter(u -> u.getUserId().equals(target.getUserId()))
                .count();
        assertEquals("grant must not create a duplicate membership row", 1, memberships);
    }

    /**
     * Given Scenario: The user holds a PARENT role, which makes every CHILD role an implicit
     * (inherited) role for that user — dotCMS role inheritance flows DOWN the tree
     * (RoleFactoryImpl#loadRolesForUser expands getRoleChildren()). An admin grants the CHILD
     * role to that user.
     * Expected Result: 200 — but NO direct membership is created, because legacy
     * addRoleToUser's doesUserHaveRole check counts inherited roles and silently no-ops.
     * This test PINS the quirk (documented in the OpenAPI description): the user will NOT
     * appear in the child role's direct-users list after this call.
     */
    @Test
    public void testAddUserToRole_inheritedMembership_noOpButOk() throws Exception {
        final Role parent = new RoleDataGen().nextPersisted();
        final Role child = new RoleDataGen().parent(parent.getId()).nextPersisted();
        final User target = UserTestUtil.getUser("inherituser" + uniq(), false, true);

        roleAPI.addRoleToUser(parent, target);
        assertTrue("precondition: the child role must be inherited via the parent",
                roleAPI.doesUserHaveRole(target, child));
        assertFalse("precondition: the child role must not be direct", isDirectMember(child, target));

        final ResponseEntityRoleUserGrantView view = resource.addUserToRole(
                adminRequest(), new MockHttpResponse().response(), child.getId(), target.getUserId());

        assertTrue(view.getEntity().granted());
        assertFalse("granting an inherited role must remain a no-op (legacy parity)",
                isDirectMember(child, target));
    }

    /**
     * Given Scenario: The user already holds the role, and the role's editUsers flag is later
     * turned off (membership frozen). The same grant is issued again.
     * Expected Result: 200 — legacy RoleAPIImpl.addRoleToUser checks doesUserHaveRole BEFORE
     * the editUsers gate, so a re-grant of an already-held role is a silent no-op regardless
     * of the flag. This pins the documented "retries are safe" idempotency for frozen roles.
     */
    @Test
    public void testAddUserToRole_alreadyHeldOnEditUsersFalseRole_idempotent() throws Exception {
        final Role role = new RoleDataGen().editUsers(true).nextPersisted();
        final User target = UserTestUtil.getUser("regrantfrozen" + uniq(), false, true);
        roleAPI.addRoleToUser(role, target);
        assertTrue(isDirectMember(role, target));

        role.setEditUsers(false);
        roleAPI.save(role);

        final ResponseEntityRoleUserGrantView view = resource.addUserToRole(
                adminRequest(), new MockHttpResponse().response(), role.getId(), target.getUserId());

        assertTrue(view.getEntity().granted());
        assertTrue("membership must be unchanged", isDirectMember(role, target));
    }

    /**
     * Given Scenario: The role has editUsers=false — the single gate legacy has on grants
     * (RoleAPIImpl.addRoleToUser throws DotStateException "Cannot alter users on this role").
     * Workflow/system roles are non-grantable precisely because this flag is false on them.
     * Expected Result: DotSecurityException (403); the user is not granted the role.
     */
    @Test
    public void testAddUserToRole_editUsersFalse_forbidden() throws Exception {
        final Role role = new RoleDataGen().editUsers(false).nextPersisted();
        final User target = UserTestUtil.getUser("nogrmuseruser" + uniq(), false, true);

        try {
            resource.addUserToRole(
                    adminRequest(), new MockHttpResponse().response(), role.getId(), target.getUserId());
            fail("Should have thrown DotSecurityException for an editUsers=false role");
        } catch (final DotSecurityException e) {
            // expected
        }

        assertFalse(roleAPI.doesUserHaveRole(target, role));
    }

    /**
     * Given Scenario: The roleId path parameter does not match any role.
     * Expected Result: 404 DoesNotExistException (resource-wide convention).
     */
    @Test(expected = DoesNotExistException.class)
    public void testAddUserToRole_missingRole_notFound() throws Exception {
        final User target = UserTestUtil.getUser("missingroleuser" + uniq(), false, true);

        resource.addUserToRole(adminRequest(), new MockHttpResponse().response(),
                UUID.randomUUID().toString(), target.getUserId());
    }

    /**
     * Given Scenario: The userId path parameter does not match any user.
     * Expected Result: 404 DoesNotExistException (NoSuchUserException mapped to the same
     * resource-wide convention).
     */
    @Test(expected = DoesNotExistException.class)
    public void testAddUserToRole_missingUser_notFound() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();

        resource.addUserToRole(adminRequest(), new MockHttpResponse().response(),
                role.getId(), "no-such-user-" + uniq());
    }

    /**
     * Given Scenario: An anonymous caller (no session user, no Authorization header).
     * Expected Result: rejected by the InitBuilder's rejectWhenNoUser gate (401).
     */
    @Test
    public void testAddUserToRole_anonymous_unauthorized() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final User target = UserTestUtil.getUser("anongrantuser" + uniq(), false, true);

        try {
            resource.addUserToRole(anonymousRequest(),
                    new MockHttpResponse().response(), role.getId(), target.getUserId());
            fail("Should have thrown a security exception for an anonymous caller");
        } catch (final com.dotcms.rest.exception.SecurityException e) {
            // expected: rejectWhenNoUser → 401
        }

        assertFalse(roleAPI.doesUserHaveRole(target, role));
    }

    /**
     * Given Scenario: A backend user without the roles portlet and without the CMS admin role
     * calls the endpoint.
     * Expected Result: rejected with a security exception (403); no membership is created.
     */
    @Test
    public void testAddUserToRole_nonAdmin_forbidden() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final User target = UserTestUtil.getUser("nonadmingrant" + uniq(), false, true);

        try {
            resource.addUserToRole(requestFor(limitedUser),
                    new MockHttpResponse().response(), role.getId(), target.getUserId());
            fail("Should have thrown a security exception");
        } catch (final DotSecurityException | com.dotcms.rest.exception.SecurityException e) {
            // expected: the InitBuilder portlet gate throws the REST SecurityException (→ 403),
            // the CMS-admin check throws DotSecurityException (→ 403)
        }

        assertFalse(roleAPI.doesUserHaveRole(target, role));
    }

    /**
     * Given Scenario: A backend user WITH access to the roles portlet but WITHOUT the CMS admin
     * role calls the endpoint — exercises the CMS-admin gate specifically.
     * Expected Result: rejected with a security exception (403); no membership is created.
     */
    @Test
    public void testAddUserToRole_rolesPortletUserWithoutAdmin_forbidden() throws Exception {
        final Layout rolesLayout = new LayoutDataGen().portletIds("roles").nextPersisted();
        final Role portletRole = new RoleDataGen().layout(rolesLayout).nextPersisted();
        final User portletUser = UserTestUtil.getUser("rolesgrantuser" + uniq(), false, true);
        roleAPI.addRoleToUser(roleAPI.loadBackEndUserRole(), portletUser);
        roleAPI.addRoleToUser(portletRole, portletUser);

        final Role role = new RoleDataGen().nextPersisted();
        final User target = UserTestUtil.getUser("portletgrant" + uniq(), false, true);

        try {
            resource.addUserToRole(requestFor(portletUser),
                    new MockHttpResponse().response(), role.getId(), target.getUserId());
            fail("Should have thrown a security exception for a non-admin caller");
        } catch (final DotSecurityException | com.dotcms.rest.exception.SecurityException e) {
            // expected: the CMS-admin check
        }

        assertFalse(roleAPI.doesUserHaveRole(target, role));
    }

    // ==================== DELETE /v1/roles/{roleId}/users — #36938 ====================

    /**
     * Method to test: {@link RoleResource#removeUsersFromRole(HttpServletRequest, HttpServletResponse, String, RoleUsersForm)}
     * Given Scenario: An admin removes a single direct member from a role.
     * Expected Result: 200 with removedUserIds=[userId], skipped empty; the membership row
     * is gone.
     */
    @Test
    public void testRemoveUsersFromRole_singleUser_removed() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final User member = UserTestUtil.getUser("removeuser" + uniq(), false, true);
        roleAPI.addRoleToUser(role, member);
        assertTrue(isDirectMember(role, member));

        final ResponseEntityRoleUsersRemovalView view = resource.removeUsersFromRole(
                adminRequest(), new MockHttpResponse().response(), role.getId(),
                new RoleUsersForm(Set.of(member.getUserId())));

        final RoleUsersRemovalView entity = view.getEntity();
        assertNotNull(entity);
        assertEquals(List.of(member.getUserId()), entity.removedUserIds());
        assertTrue(entity.skipped().isEmpty());

        assertFalse("membership row must be gone", isDirectMember(role, member));
        assertFalse(roleAPI.doesUserHaveRole(member, role));
    }

    /**
     * Given Scenario: An admin bulk-removes three direct members in one call (bulk is legacy
     * functionality — DWR RoleAjax#removeUsersFromRole takes an array).
     * Expected Result: 200; all three userIds in removedUserIds, all three memberships gone.
     */
    @Test
    public void testRemoveUsersFromRole_bulk_allRemoved() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final User memberA = UserTestUtil.getUser("bulkusera" + uniq(), false, true);
        final User memberB = UserTestUtil.getUser("bulkuserb" + uniq(), false, true);
        final User memberC = UserTestUtil.getUser("bulkuserc" + uniq(), false, true);
        roleAPI.addRoleToUser(role, memberA);
        roleAPI.addRoleToUser(role, memberB);
        roleAPI.addRoleToUser(role, memberC);

        final ResponseEntityRoleUsersRemovalView view = resource.removeUsersFromRole(
                adminRequest(), new MockHttpResponse().response(), role.getId(),
                new RoleUsersForm(Set.of(
                        memberA.getUserId(), memberB.getUserId(), memberC.getUserId())));

        final RoleUsersRemovalView entity = view.getEntity();
        assertEquals(Set.of(memberA.getUserId(), memberB.getUserId(), memberC.getUserId()),
                Set.copyOf(entity.removedUserIds()));
        assertTrue(entity.skipped().isEmpty());
        assertTrue(roleAPI.findUsersForRole(role, false).isEmpty());
    }

    /**
     * Given Scenario: A mixed batch — one valid direct member, one nonexistent userId, and one
     * user whose membership is inherited-only (holds the PARENT role; the target role is a child).
     * Expected Result: 200 with PARTIAL SUCCESS — this is the pin for the batch contract:
     * removedUserIds contains only the valid member (who IS actually removed), skipped reports
     * the nonexistent id with reason "not_found" and the inherited-only user with reason
     * "inherited". The batch never fails as a whole (improvement over legacy DWR's fail-fast
     * mid-loop with partial effects — same outcomes, but reported).
     */
    @Test
    public void testRemoveUsersFromRole_mixedBatch_partialSuccess() throws Exception {
        final Role parent = new RoleDataGen().nextPersisted();
        final Role role = new RoleDataGen().parent(parent.getId()).nextPersisted();

        final User directMember = UserTestUtil.getUser("mixdirect" + uniq(), false, true);
        roleAPI.addRoleToUser(role, directMember);

        final User inheritedOnly = UserTestUtil.getUser("mixinherit" + uniq(), false, true);
        roleAPI.addRoleToUser(parent, inheritedOnly);
        assertTrue(roleAPI.doesUserHaveRole(inheritedOnly, role));
        assertFalse(isDirectMember(role, inheritedOnly));

        final String missingUserId = "no-such-user-" + uniq();

        final ResponseEntityRoleUsersRemovalView view = resource.removeUsersFromRole(
                adminRequest(), new MockHttpResponse().response(), role.getId(),
                new RoleUsersForm(Set.of(
                        directMember.getUserId(), missingUserId, inheritedOnly.getUserId())));

        final RoleUsersRemovalView entity = view.getEntity();
        assertEquals(List.of(directMember.getUserId()), entity.removedUserIds());

        final Map<String, String> skippedByUser = entity.skipped().stream()
                .collect(Collectors.toMap(SkippedUserView::userId, SkippedUserView::reason));
        assertEquals(2, skippedByUser.size());
        assertEquals("not_found", skippedByUser.get(missingUserId));
        assertEquals("inherited", skippedByUser.get(inheritedOnly.getUserId()));

        assertFalse("the valid member must actually be removed", isDirectMember(role, directMember));
        assertTrue("the inherited membership must be intact",
                roleAPI.doesUserHaveRole(inheritedOnly, role));
    }

    /**
     * Given Scenario: The only requested removal is for a user whose membership is
     * inherited-only. Legacy silently no-ops here (the raw DELETE matches 0 rows,
     * RoleFactoryImpl#removeRoleFromUser); this endpoint does the same thing but REPORTS it.
     * Expected Result: 200 with removedUserIds empty and the user skipped with reason
     * "inherited"; the inherited membership (via the parent role) is intact.
     */
    @Test
    public void testRemoveUsersFromRole_inheritedOnly_skippedAndIntact() throws Exception {
        final Role parent = new RoleDataGen().nextPersisted();
        final Role child = new RoleDataGen().parent(parent.getId()).nextPersisted();
        final User target = UserTestUtil.getUser("inheritonly" + uniq(), false, true);
        roleAPI.addRoleToUser(parent, target);
        assertTrue(roleAPI.doesUserHaveRole(target, child));

        final ResponseEntityRoleUsersRemovalView view = resource.removeUsersFromRole(
                adminRequest(), new MockHttpResponse().response(), child.getId(),
                new RoleUsersForm(Set.of(target.getUserId())));

        final RoleUsersRemovalView entity = view.getEntity();
        assertTrue(entity.removedUserIds().isEmpty());
        assertEquals(1, entity.skipped().size());
        assertEquals(target.getUserId(), entity.skipped().get(0).userId());
        assertEquals("inherited", entity.skipped().get(0).reason());

        assertTrue("inheritance must be intact", roleAPI.doesUserHaveRole(target, child));
        assertTrue("direct parent membership must be intact", isDirectMember(parent, target));
    }

    /**
     * Given Scenario: The request body carries an empty userIds set.
     * Expected Result: 400 BadRequestException.
     */
    @Test(expected = BadRequestException.class)
    public void testRemoveUsersFromRole_emptyUserIds_badRequest() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();

        resource.removeUsersFromRole(adminRequest(), new MockHttpResponse().response(),
                role.getId(), new RoleUsersForm(Set.of()));
    }

    /**
     * Given Scenario: The request body contains a null element inside userIds (Jackson accepts
     * null elements in a JSON array bound to Set&lt;String&gt;).
     * Expected Result: 400 BadRequestException — NOT an NPE-shaped 500 mid-batch, which would
     * break the partial-success contract after earlier removals already committed.
     */
    @Test(expected = BadRequestException.class)
    public void testRemoveUsersFromRole_nullEntry_badRequest() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();

        resource.removeUsersFromRole(adminRequest(), new MockHttpResponse().response(),
                role.getId(), new RoleUsersForm(new HashSet<>(Arrays.asList("some-id", null))));
    }

    /**
     * Given Scenario: The request body contains a blank (whitespace-only) userId entry.
     * Expected Result: 400 BadRequestException — a trivially-invalid input must be rejected up
     * front, not misreported as a per-user "error" (which is documented as a server failure).
     */
    @Test(expected = BadRequestException.class)
    public void testRemoveUsersFromRole_blankEntry_badRequest() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();

        resource.removeUsersFromRole(adminRequest(), new MockHttpResponse().response(),
                role.getId(), new RoleUsersForm(Set.of("  ")));
    }

    /**
     * Given Scenario: The DELETE request carries no body at all (form is null).
     * Expected Result: 400 BadRequestException — not an NPE-shaped 500.
     */
    @Test(expected = BadRequestException.class)
    public void testRemoveUsersFromRole_missingBody_badRequest() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();

        resource.removeUsersFromRole(adminRequest(), new MockHttpResponse().response(),
                role.getId(), null);
    }

    /**
     * Given Scenario: The roleId path parameter does not match any role.
     * Expected Result: 404 DoesNotExistException (resource-wide convention).
     */
    @Test(expected = DoesNotExistException.class)
    public void testRemoveUsersFromRole_missingRole_notFound() throws Exception {
        final User member = UserTestUtil.getUser("missingrolerm" + uniq(), false, true);

        resource.removeUsersFromRole(adminRequest(), new MockHttpResponse().response(),
                UUID.randomUUID().toString(), new RoleUsersForm(Set.of(member.getUserId())));
    }

    /**
     * Given Scenario: The role's editUsers flag is false — its memberships are system-managed
     * (a user's individual role, system roles). The legacy Roles portlet never allowed this
     * removal (it renders no selection checkboxes and hides the Remove button for such roles),
     * and the grant endpoint already rejects these roles with 403. See #37109.
     * Expected Result: DotSecurityException (403) for the whole request, mirroring the grant
     * endpoint; the membership is intact.
     */
    @Test
    public void testRemoveUsersFromRole_editUsersFalse_forbidden() throws Exception {
        final Role role = new RoleDataGen().editUsers(true).nextPersisted();
        final User member = UserTestUtil.getUser("frozenrm" + uniq(), false, true);
        roleAPI.addRoleToUser(role, member);
        assertTrue(isDirectMember(role, member));

        role.setEditUsers(false);
        roleAPI.save(role);

        try {
            resource.removeUsersFromRole(adminRequest(), new MockHttpResponse().response(),
                    role.getId(), new RoleUsersForm(Set.of(member.getUserId())));
            fail("Should have thrown DotSecurityException for an editUsers=false role");
        } catch (final DotSecurityException e) {
            // expected
        }

        assertTrue("membership must be intact", isDirectMember(role, member));
    }

    /**
     * Given Scenario: An anonymous caller (no session user, no Authorization header).
     * Expected Result: rejected by the InitBuilder's rejectWhenNoUser gate (401); the
     * membership is intact.
     */
    @Test
    public void testRemoveUsersFromRole_anonymous_unauthorized() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final User member = UserTestUtil.getUser("anonremove" + uniq(), false, true);
        roleAPI.addRoleToUser(role, member);

        try {
            resource.removeUsersFromRole(anonymousRequest(), new MockHttpResponse().response(),
                    role.getId(), new RoleUsersForm(Set.of(member.getUserId())));
            fail("Should have thrown a security exception for an anonymous caller");
        } catch (final com.dotcms.rest.exception.SecurityException e) {
            // expected: rejectWhenNoUser → 401
        }

        assertTrue(isDirectMember(role, member));
    }

    /**
     * Given Scenario: A backend user without the roles portlet and without the CMS admin role
     * calls the endpoint.
     * Expected Result: rejected with a security exception (403); the membership is intact.
     */
    @Test
    public void testRemoveUsersFromRole_nonAdmin_forbidden() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final User member = UserTestUtil.getUser("nonadminrm" + uniq(), false, true);
        roleAPI.addRoleToUser(role, member);

        try {
            resource.removeUsersFromRole(requestFor(limitedUser), new MockHttpResponse().response(),
                    role.getId(), new RoleUsersForm(Set.of(member.getUserId())));
            fail("Should have thrown a security exception");
        } catch (final DotSecurityException | com.dotcms.rest.exception.SecurityException e) {
            // expected: the InitBuilder portlet gate throws the REST SecurityException (→ 403),
            // the CMS-admin check throws DotSecurityException (→ 403)
        }

        assertTrue(isDirectMember(role, member));
    }

    /**
     * Given Scenario: A backend user WITH access to the roles portlet but WITHOUT the CMS admin
     * role calls the endpoint — exercises the CMS-admin gate specifically.
     * Expected Result: rejected with a security exception (403); the membership is intact.
     */
    @Test
    public void testRemoveUsersFromRole_rolesPortletUserWithoutAdmin_forbidden() throws Exception {
        final Layout rolesLayout = new LayoutDataGen().portletIds("roles").nextPersisted();
        final Role portletRole = new RoleDataGen().layout(rolesLayout).nextPersisted();
        final User portletUser = UserTestUtil.getUser("rolesrmuser" + uniq(), false, true);
        roleAPI.addRoleToUser(roleAPI.loadBackEndUserRole(), portletUser);
        roleAPI.addRoleToUser(portletRole, portletUser);

        final Role role = new RoleDataGen().nextPersisted();
        final User member = UserTestUtil.getUser("portletrm" + uniq(), false, true);
        roleAPI.addRoleToUser(role, member);

        try {
            resource.removeUsersFromRole(requestFor(portletUser), new MockHttpResponse().response(),
                    role.getId(), new RoleUsersForm(Set.of(member.getUserId())));
            fail("Should have thrown a security exception for a non-admin caller");
        } catch (final DotSecurityException | com.dotcms.rest.exception.SecurityException e) {
            // expected: the CMS-admin check
        }

        assertTrue(isDirectMember(role, member));
    }

    // ==================== GET /v1/roles/layouts ====================

    /**
     * Given Scenario: An anonymous caller (no session, no credentials) requests the layout
     * catalog. Before #37259 this endpoint had no authentication gate and answered 200 to anyone.
     * Expected Result: rejected by the InitBuilder gate with the REST SecurityException (401);
     * no layout data is returned.
     */
    @Test
    public void testGetAllLayouts_anonymous_unauthorized() throws Exception {
        try {
            resource.getAllLayouts(anonymousRequest(), new MockHttpResponse().response());
            fail("Anonymous caller should have been rejected");
        } catch (final com.dotcms.rest.exception.SecurityException e) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), e.getResponse().getStatus());
        }
    }

    /**
     * Given Scenario: An authenticated backend user WITHOUT access to the roles portlet and
     * WITHOUT the CMS admin role requests the layout catalog.
     * Expected Result: rejected by the portlet gate with the REST SecurityException, exactly as
     * every sibling endpoint on this resource behaves; no layout data is returned.
     */
    @Test
    public void testGetAllLayouts_backendUserWithoutRolesPortlet_rejected() throws Exception {
        try {
            resource.getAllLayouts(requestFor(limitedUser), new MockHttpResponse().response());
            fail("Backend user without the roles portlet should have been rejected");
        } catch (final com.dotcms.rest.exception.SecurityException e) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), e.getResponse().getStatus());
        }
    }

    /**
     * Given Scenario: A non-admin backend user WITH access to the roles portlet requests the
     * layout catalog — the Roles portlet (legacy and Angular) runs in this context.
     * Expected Result: 200; the payload keeps its shape — every layout in the system, each with a
     * {@code portletTitles} list parallel to its {@code portletIds} — and includes the layout
     * just created for this user.
     */
    @Test
    public void testGetAllLayouts_rolesPortletUser_ok() throws Exception {
        final Layout rolesLayout = new LayoutDataGen().portletIds("roles").nextPersisted();
        final Role portletRole = new RoleDataGen().layout(rolesLayout).nextPersisted();
        final User portletUser = UserTestUtil.getUser("layoutsportletuser" + uniq(), false, true);
        roleAPI.addRoleToUser(roleAPI.loadBackEndUserRole(), portletUser);
        roleAPI.addRoleToUser(portletRole, portletUser);

        final Response response = resource.getAllLayouts(requestFor(portletUser),
                new MockHttpResponse().response());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final List<Map<String, Object>> layouts = layoutsFrom(response);
        assertFalse(layouts.isEmpty());
        assertEquals(APILocator.getLayoutAPI().findAllLayouts().size(), layouts.size());
        assertLayoutsShape(layouts);
        assertTrue("Freshly created layout must be in the catalog",
                layouts.stream().anyMatch(l -> rolesLayout.getId().equals(l.get("id"))));
    }

    /**
     * Given Scenario: The admin user (Basic auth, as the Postman DotFavoritePage collection does)
     * requests the layout catalog.
     * Expected Result: 200 with the unchanged payload shape — the existing consumers keep working.
     */
    @Test
    public void testGetAllLayouts_admin_ok() throws Exception {
        final Response response = resource.getAllLayouts(adminRequest(),
                new MockHttpResponse().response());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final List<Map<String, Object>> layouts = layoutsFrom(response);
        assertFalse(layouts.isEmpty());
        assertEquals(APILocator.getLayoutAPI().findAllLayouts().size(), layouts.size());
        assertLayoutsShape(layouts);
    }

    private static List<Map<String, Object>> layoutsFrom(final Response response) {
        final LayoutMapResponseEntityView view = (LayoutMapResponseEntityView) response.getEntity();
        assertNotNull(view);
        return view.getEntity();
    }

    @SuppressWarnings("unchecked")
    private static void assertLayoutsShape(final List<Map<String, Object>> layouts) {
        for (final Map<String, Object> layout : layouts) {
            assertNotNull("id", layout.get("id"));
            assertNotNull("name", layout.get("name"));
            assertTrue("portletIds must be a list", layout.get("portletIds") instanceof List);
            assertTrue("portletTitles must be a list", layout.get("portletTitles") instanceof List);
            assertEquals("one title per portlet id",
                    ((List<String>) layout.get("portletIds")).size(),
                    ((List<String>) layout.get("portletTitles")).size());
        }
    }
}
