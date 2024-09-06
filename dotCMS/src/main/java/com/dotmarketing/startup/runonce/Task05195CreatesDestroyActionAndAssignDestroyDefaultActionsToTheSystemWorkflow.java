package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DbType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

import java.util.Map;

/**
 * This upgrade task set to the system workflow the {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction#DESTROY} default actions
 * @author jsanca
 */
public class Task05195CreatesDestroyActionAndAssignDestroyDefaultActionsToTheSystemWorkflow implements StartupTask {

    protected static final String INSERT_ACTION            = "insert into workflow_action (id, scheme_id, name, condition_to_progress, next_step_id, next_assign, my_order, assignable, commentable, icon, use_role_hierarchy_assign, requires_checkout, show_on) values (?, ?, ?, ?, ?, ?, ?,?, ?, ?,?,?,?)";
    protected static final String INSERT_ACTION_FOR_STEP   = "insert into workflow_action_step(action_id, step_id, action_order) values (?,?,?)";
    protected static final String INSERT_ACTION_CLASS      = "insert into workflow_action_class (id, action_id, name, my_order, clazz) values (?,?, ?, ?, ?)";
    protected static final Map<DbType, String> insertPermissionMap   = Map.of(
            DbType.POSTGRESQL,   "insert into permission(id, permission_type, inode_id, roleid, permission) values (nextval('permission_seq'), ?, ?, ?, ?)",
            DbType.ORACLE,       "insert into permission(id, permission_type, inode_id, roleid, permission) values (permission_seq.NEXTVAL,    ?, ?, ?, ?)",
            DbType.MYSQL,        "insert into permission(permission_type, inode_id, roleid, permission) values (?, ?, ?, ?)",
            DbType.MSSQL,        "insert into permission(permission_type, inode_id, roleid, permission) values (?, ?, ?, ?)"
    );

    @Override
    public boolean forceRun() {

        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException {

        this.checkInsertDestroyAction();
        this.checkInsertSystemAction(SystemWorkflowConstants.WORKFLOW_DESTROY_ACTION_ID,      SystemAction.DESTROY);
    } // executeUpgrade.

    private void checkInsertDestroyAction () throws DotDataException {

        if (!UtilMethods.isSet(new DotConnect().setSQL("select * from workflow_action where id = ? and scheme_id = ?")
                .addParam(SystemWorkflowConstants.WORKFLOW_DESTROY_ACTION_ID).addParam(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                .loadObjectResults())) {

            this.createAction();
            this.createActionStep();
            this.createActionClass();
            this.addPermissions();
        }
    }

    private void createAction() throws DotDataException {

        new DotConnect().setSQL(INSERT_ACTION)
                .addParam(SystemWorkflowConstants.WORKFLOW_DESTROY_ACTION_ID)
                .addParam(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                .addParam("Destroy")
                .addParam(StringPool.BLANK)
                .addParam("d6b095b6-b65f-4bdb-bbfd-701d663dfee2")
                .addParam(this.getRoleId("CMS Anonymous", "654b0931-1027-41f7-ad4d-173115ed8ec1"))
                .addParam(0)
                .addParam(false)
                .addParam(false)
                .addParam("workflowIcon")
                .addParam(false)
                .addParam(false)
                .addParam("UNLOCKED,ARCHIVED,LOCKED,LISTING,EDITING")
                .loadResult();
    }

    private void createActionStep() throws DotDataException {

        new DotConnect().setSQL(INSERT_ACTION_FOR_STEP)
                .addParam(SystemWorkflowConstants.WORKFLOW_DESTROY_ACTION_ID)
                .addParam(SystemWorkflowConstants.WORKFLOW_ARCHIVE_STEP_ID)
                .addParam(2)
                .loadResult();
    }

    private void createActionClass() throws DotDataException {

        new DotConnect().setSQL(INSERT_ACTION_CLASS)
                .addParam("74f42846-86b6-4660-bd00-789446fd67c8")
                .addParam(SystemWorkflowConstants.WORKFLOW_DESTROY_ACTION_ID)
                .addParam("Destroy content")
                .addParam(0)
                .addParam("com.dotmarketing.portlets.workflows.actionlet.DestroyContentActionlet")
                .loadResult();
    }

    private void addPermissions() throws DotDataException {

            final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());

            new DotConnect().setSQL(insertPermissionMap.get(dbType))
                    .addParam("individual")
                    .addParam(SystemWorkflowConstants.WORKFLOW_DESTROY_ACTION_ID)
                    .addParam("c3eb4526-6d96-48d8-9540-e5fa560cfc0f")
                    .addParam(1)
                    .loadResult();
    }

    private String getRoleId(final String roleKey, final String defaultValue) throws DotDataException {
        return (String)new DotConnect()
                .setSQL("select id,role_name,role_key from cms_role where role_key = ?")
                .addParam(roleKey)
                .loadObjectResults()
                .stream().findFirst().orElse(Map.of("id", defaultValue))
                .get("id");
    }

    private void checkInsertSystemAction (final String workflowActionId, final WorkflowAPI.SystemAction... systemActions) throws DotDataException {

        if (UtilMethods.isSet(new DotConnect().setSQL("select * from workflow_action where id = ? and scheme_id = ?")
                .addParam(workflowActionId).addParam(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                .loadObjectResults())) {

            for (final WorkflowAPI.SystemAction systemAction : systemActions) {
                insert(systemAction, workflowActionId);
            }
        }
    }

    private void insert (final WorkflowAPI.SystemAction systemAction, final String workflowActionId) throws DotDataException {

        if (!UtilMethods.isSet(new DotConnect()
                .setSQL("select * from workflow_action_mappings where action = ? and workflow_action = ? and scheme_or_content_type = ?")
                .addParam(systemAction.name()).addParam(workflowActionId)
                .addParam(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                .loadObjectResults())) {

            new DotConnect()
                    .setSQL("insert into workflow_action_mappings(id, action, workflow_action, scheme_or_content_type) values (?,?,?,?)")
                    .addParam(UUIDUtil.uuid()).addParam(systemAction.name())
                    .addParam(workflowActionId).addParam(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                    .loadObjectResults();
        }

    }
}
