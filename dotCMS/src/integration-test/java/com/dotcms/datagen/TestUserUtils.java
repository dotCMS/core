package com.dotcms.datagen;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;

/**
 * @author Jonathan Gamba 2019-06-11
 */
public class TestUserUtils {

    public static Role getOrCreatePublisherRole() throws DotDataException, DotSecurityException {
        return getOrCreatePublisherRole(APILocator.systemHost());
    }

    public static Role getOrCreatePublisherRole(final Host host)
            throws DotDataException, DotSecurityException {
        final String roleName = "Publisher / Legal";
        final int pagePermissions = (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                | PermissionAPI.PERMISSION_PUBLISH);
        final int contentPermissions = (PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_EDIT | PermissionAPI.PERMISSION_PUBLISH);
        final Map<PermissionableType, Integer> typesAndPermissions = ImmutableMap.of(
                PermissionableType.HTMLPAGES, pagePermissions,
                PermissionableType.CONTENTLETS, contentPermissions
        );
        return getOrCreateRole(host, roleName, null, true, typesAndPermissions);
    }

    public static Role getOrCreateReviewerRole(final Host host)
            throws DotDataException, DotSecurityException {
        final String roleName = "Reviewer";
        final Role publisher = getOrCreatePublisherRole(host);
        final int permissions = (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                | PermissionAPI.PERMISSION_PUBLISH);
        final Map<PermissionableType, Integer> typesAndPermissions = ImmutableMap.of(
                PermissionableType.FOLDERS, permissions, PermissionableType.STRUCTURES, permissions,
                PermissionableType.CONTENTLETS, permissions
        );
        return getOrCreateRole(host, roleName, publisher, true, typesAndPermissions);
    }

    public static Role getOrCreateReviewerRole() throws DotDataException, DotSecurityException {
        return getOrCreateReviewerRole(APILocator.systemHost());
    }

    public static Role getOrCreateContributorRole(final Host host)
            throws DotDataException, DotSecurityException {
        final String roleName = "Contributor";
        final Role reviewer = getOrCreateReviewerRole(host);
        final int folderPermissions = (PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN);
        final int structurePermissions = (PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_USE | PermissionAPI.PERMISSION_EDIT);
        final int contentPermissions = (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_USE
                | PermissionAPI.PERMISSION_EDIT);

        final Map<PermissionableType, Integer> typesAndPermissions = ImmutableMap.of(
                PermissionableType.FOLDERS, folderPermissions,
                PermissionableType.STRUCTURES, structurePermissions,
                PermissionableType.CONTENTLETS, contentPermissions
        );
        return getOrCreateRole(host, roleName, reviewer, true, typesAndPermissions);
    }

    public static Role getOrCreateContributorRole() throws DotDataException, DotSecurityException {
        return getOrCreateContributorRole(APILocator.systemHost());
    }

    public static Role getOrCreateIntranetRole(final Host host)
            throws DotDataException, DotSecurityException {
        final String roleName = "Intranet";
        final int permissions = (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_USE);
        final Map<PermissionableType, Integer> permissionsAndTypes =
                new ImmutableMap.Builder<PermissionableType, Integer>()
                        .put(PermissionableType.FOLDERS, permissions)
                        .put(PermissionableType.HTMLPAGES, permissions)
                        .put(PermissionableType.LINKS, permissions)
                        .put(PermissionableType.STRUCTURES, permissions)
                        .put(PermissionableType.CONTENTLETS, permissions)
                        .build();
        return getOrCreateRole(host, roleName, null, false, permissionsAndTypes);
    }

    public static Role getOrCreateIntranetRole() throws DotDataException, DotSecurityException {
        return getOrCreateIntranetRole(APILocator.systemHost());
    }

    private static Role getOrCreateRole(final Host host, final String roleName,
            final Role parentRole, boolean editLayouts,
            final Map<PermissionableType, Integer> typesAndPermissions)
            throws DotDataException, DotSecurityException {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        Role role = roleAPI.findRoleByName(roleName, parentRole);
        if (role == null) {
            final User user = APILocator.systemUser();
            final String parentId = parentRole != null ? parentRole.getId() : null;
            role = new RoleDataGen().name(roleName).key(roleName).editPermissions(true)
                    .editUsers(true).editLayouts(editLayouts).parent(parentId).nextPersisted();

            for (final PermissionableType type : typesAndPermissions.keySet()) {
                final int permission = typesAndPermissions.get(type);
                final Permission permissions = new Permission(
                        type.getCanonicalName(),
                        host.getPermissionId(),
                        role.getId(),
                        permission);
                APILocator.getPermissionAPI().save(permissions, host, user, false);
            }
        }
        return role;
    }

    public static User getUser(final Role role, final String email,
            final String name,
            final String lastName, final String password)
            throws DotDataException {
        final List<User> users = APILocator.getUserAPI().getUsersByNameOrEmail(email, 0, 1);
        if (UtilMethods.isSet(users)) {
            return users.get(0);
        }
        return new UserDataGen().firstName(name).lastName(lastName).emailAddress(email)
                .password(password).roles(role).nextPersisted();
    }

    public static User getChrisPublisherUser(final Host host)
            throws DotDataException, DotSecurityException {
        final String email = "chris@dotcms.com";
        final List<User> users = APILocator.getUserAPI().getUsersByNameOrEmail(email, 0, 1);
        if (UtilMethods.isSet(users)) {
            return users.get(0);
        }
        return new UserDataGen().firstName("Chris").lastName("Publisher").emailAddress(email)
                .password("chris").roles(getOrCreatePublisherRole(host)).nextPersisted();
    }

    public static User getChrisPublisherUser() throws DotDataException, DotSecurityException {
        return getChrisPublisherUser(APILocator.systemHost());
    }

    public static User getJoeContributorUser(final Host host)
            throws DotDataException, DotSecurityException {
        final String email = "joe@dotcms.com";
        final List<User> users = APILocator.getUserAPI().getUsersByNameOrEmail(email, 0, 1);
        if (UtilMethods.isSet(users)) {
            return users.get(0);
        }
        return new UserDataGen().firstName("Joe").lastName("Contributor").emailAddress(email)
                .password("joe").roles(getOrCreateContributorRole(host)).nextPersisted();
    }

    public static User getJoeContributorUser() throws DotDataException, DotSecurityException {
        return getJoeContributorUser(APILocator.systemHost());
    }

    public static User getBillIntranetUser(final Host host)
            throws DotDataException, DotSecurityException {
        final String email = "bill@dotcms.com";
        final List<User> users = APILocator.getUserAPI().getUsersByNameOrEmail(email, 0, 1);
        if (UtilMethods.isSet(users)) {
            return users.get(0);
        }
        return new UserDataGen().firstName("Bill").lastName("Intranet").emailAddress(email)
                .password("bill").roles(getOrCreateIntranetRole(host), getOrCreateContributorRole())
                .nextPersisted();
    }

    public static User getBillIntranetUser() throws DotDataException, DotSecurityException {
        return getBillIntranetUser(APILocator.systemHost());
    }

    public static User getJaneReviewerUser(final Host host)
            throws DotDataException, DotSecurityException {
        final String email = "jane@dotcms.com";
        final List<User> users = APILocator.getUserAPI().getUsersByNameOrEmail(email, 0, 1);
        if (UtilMethods.isSet(users)) {
            return users.get(0);
        }
        return new UserDataGen().firstName("Jane").lastName("Reviewer").emailAddress(email)
                .password("jane").roles(getOrCreateReviewerRole(host)).nextPersisted();
    }

    public static User getJaneReviewerUser() throws DotDataException, DotSecurityException {
        return getJaneReviewerUser(APILocator.systemHost());
    }

    public static Map<String, Role> getOrCreateWorkflowRoles() throws DotDataException {

        final RoleAPI roleAPI = APILocator.getRoleAPI();

        Role anyWhoView = roleAPI.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_VIEW_ROLE_KEY);
        if (null == anyWhoView) {
            anyWhoView = new RoleDataGen().key(RoleAPI.WORKFLOW_ANY_WHO_CAN_VIEW_ROLE_KEY)
                    .nextPersisted();
        }

        Role anyWhoEdit = roleAPI.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_ROLE_KEY);
        if (null == anyWhoEdit) {
            anyWhoEdit = new RoleDataGen().key(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_ROLE_KEY)
                    .nextPersisted();
        }

        Role anyWhoPublish = roleAPI.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_PUBLISH_ROLE_KEY);
        if (null == anyWhoPublish) {
            anyWhoPublish = new RoleDataGen().key(RoleAPI.WORKFLOW_ANY_WHO_CAN_PUBLISH_ROLE_KEY)
                    .nextPersisted();
        }

        Role anyWhoEditPermissions = roleAPI
                .loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_PERMISSIONS_ROLE_KEY);
        if (null == anyWhoEditPermissions) {
            anyWhoEditPermissions = new RoleDataGen()
                    .key(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_PERMISSIONS_ROLE_KEY).nextPersisted();
        }

        return ImmutableMap.of(
                RoleAPI.WORKFLOW_ANY_WHO_CAN_VIEW_ROLE_KEY, anyWhoView,
                RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_ROLE_KEY, anyWhoEdit,
                RoleAPI.WORKFLOW_ANY_WHO_CAN_PUBLISH_ROLE_KEY, anyWhoPublish,
                RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_PERMISSIONS_ROLE_KEY, anyWhoEditPermissions
        );
    }

}