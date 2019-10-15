package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Add the Workflow Roles
 */
public class Task04310CreateWorkflowRoles extends AbstractJDBCStartupTask {

    private static final String getSystemRole =
            "select id, role_name, description, role_key, db_fqn, parent, edit_permissions, edit_users, edit_layouts, "
                    +
                    "locked, system from cms_role where role_key = 'System' and id = parent";

    private static final String selectSystemRoles =
            "select id, role_name, description, role_key, db_fqn, parent, edit_permissions, edit_users, edit_layouts, "
                    +
                    "locked, system from cms_role where parent = ? and parent <> id";

    private static final String insertRole =
            "insert into cms_role (id, role_name, description, role_key, db_fqn, parent, edit_permissions, "
                    +
                    "edit_users, edit_layouts, locked, system) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String AnyoneWhoCanReadRole = "Anyone who can View Content";
    private static final String AnyoneWhoCanReadRoleId = "0f995057-5c35-4ae7-998a-774dda146c63";
    private static final String AnyoneWhoCanEditRole = "Anyone who can Edit Content";
    private static final String AnyoneWhoCanEditRoleId = "617f7300-5c7b-463f-9554-380b918520bc";
    private static final String AnyoneWhoCanPublishRole = "Anyone who can Publish Content";
    private static final String AnyoneWhoCanPublishRoleId = "c3eb4526-6d96-48d8-9540-e5fa560cfc0f";
    private static final String AnyoneWhoCanEditPermissionsRole = "Anyone who can Edit Permissions Content";
    private static final String AnyoneWhoCanEditPermissionsRoleId = "52181fb6-65c8-4221-8d17-1da8b0e20784";

    private DotConnect dc = null;
    private String systemRootRoleId;

    @Override
    public void executeUpgrade() throws DotDataException {

        dc = new DotConnect();

        try {
            systemRootRoleId = getSystemRootRoleId();
        } catch (DotDataException e) {
            Logger.info(this, "Task not executing seems the roles has not been upgraded yet");
            return;
        }

        final List<Map<String, String>> systemRoles = getSystemRoles();

        if (!containsRole(AnyoneWhoCanReadRole, systemRoles)) {
            insertRole(AnyoneWhoCanReadRole, AnyoneWhoCanReadRoleId,
                    RoleAPI.WORKFLOW_ANY_WHO_CAN_VIEW_ROLE_KEY);
        }

        if (!containsRole(AnyoneWhoCanEditRole, systemRoles)) {
            insertRole(AnyoneWhoCanEditRole, AnyoneWhoCanEditRoleId,
                    RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_ROLE_KEY);
        }

        if (!containsRole(AnyoneWhoCanPublishRole, systemRoles)) {
            insertRole(AnyoneWhoCanPublishRole, AnyoneWhoCanPublishRoleId,
                    RoleAPI.WORKFLOW_ANY_WHO_CAN_PUBLISH_ROLE_KEY);
        }

        if (!containsRole(AnyoneWhoCanEditPermissionsRole, systemRoles)) {
            insertRole(AnyoneWhoCanEditPermissionsRole, AnyoneWhoCanEditPermissionsRoleId,
                    RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_PERMISSIONS_ROLE_KEY);
        }

    }

    /**
     * Get the id of the System Root Role
     *
     * @return the Id of the syste Root role
     */
    private String getSystemRootRoleId() throws DotDataException {
        dc.setSQL(getSystemRole);
        ArrayList<Map<String, String>> results = dc.loadResults();
        return results.get(0).get("id");
    }

    /**
     * Get all the system roles
     *
     * @return List<Map<String, String>>  of the system roles
     */
    private List<Map<String, String>> getSystemRoles() throws DotDataException {
        dc.setSQL(selectSystemRoles);
        dc.addParam(systemRootRoleId);
        return dc.loadResults();
    }

    /**
     * Add the new role
     *
     * @param roleName Name of the role
     * @param roleId Id of the new role
     */
    private void insertRole(final String roleName, final String roleId, final String roleKey) throws DotDataException {
        dc.setSQL(insertRole);
        dc.addParam(roleId);                                    //id
        dc.addParam(roleName.trim());                           //role_name
        dc.addParam(roleName.trim());                           //description
        dc.addParam(roleKey);                                   //key
        dc.addParam(systemRootRoleId + " --> " + roleId);       //db_fqn
        dc.addParam(systemRootRoleId);                          //parent
        dc.addParam(true);                                     //edit_permission
        dc.addParam(true);                                     //edit users
        dc.addParam(false);                                     //edit layouts
        dc.addParam(false);                                     //locked
        dc.addParam(true);                                      //system
        dc.loadResult();
    }

    /**
     * Validate if the role exist in the system
     */
    private boolean containsRole(final String roleName,
            final List<Map<String, String>> systemRoles) {

        for (final Map<String, String> systemRole : systemRoles) {
            if (systemRole.get("role_name").equals(roleName.trim())) {
                return true;
            }
        }
        return false;
    }

    public boolean forceRun() {
        return true;
    }

    @Override
    public String getPostgresScript() {
        return null;
    }

    @Override
    public String getMySQLScript() {
        return null;
    }

    @Override
    public String getOracleScript() {
        return null;
    }

    @Override
    public String getMSSQLScript() {
        return null;
    }

    @Override
    public String getH2Script() {
        return null;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return Collections.emptyList();
    }

}
