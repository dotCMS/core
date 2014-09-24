package com.dotmarketing.startup.runonce;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.UUIDGenerator;

public class Task01020CreateDefaultWorkflow implements StartupTask {

	/**
	 * 
	 * @param dc
	 * @throws SQLException
	 * @throws DotDataException
	 */
	private void createDefaultScheme(DotConnect dc) throws SQLException, DotDataException {
		String schemeID = UUIDGenerator.generateUuid();
		String WORKFLOW_DEFAULT_SCHEME_NAME = "Default Scheme";
		dc.setSQL("SELECT id FROM workflow_scheme WHERE name = ?");
		dc.addParam(WORKFLOW_DEFAULT_SCHEME_NAME);
		List<Map<String, String>> results = null;
		results = dc.loadResults();
		if (results != null && results.size() > 0) {
			schemeID = results.get(0).get("id");
		} else {
			// Workflow Scheme
			dc.executeStatement("insert into workflow_scheme (id, name, description, archived, mandatory, entry_action_id, default_scheme) "
					+ "values ('"
					+ schemeID + "',"// id
					+ "'" + WORKFLOW_DEFAULT_SCHEME_NAME + "',"// name
					+ "'This is the default workflow scheme that will be applied to all content',"// description
					+ DbConnectionFactory.getDBFalse() + ","// archived
					+ DbConnectionFactory.getDBFalse() + ","// mandatory
					+ "null,"// entry_action_id
					+ DbConnectionFactory.getDBTrue() + ")");// default_scheme
		}
		dc.executeStatement("update workflow_scheme set default_scheme = "
				+ DbConnectionFactory.getDBTrue()
				+ " where id = '" + schemeID + "' ");
		
		//Workflow Steps
		String initialID = UUIDGenerator.generateUuid();
		dc.executeStatement("insert into workflow_step (id, name, scheme_id, my_order, resolved, escalation_enable, escalation_action, escalation_time) "
				+ "values ('" + initialID + "',"//id
				+ " 'Initial State',"//name
				+ " '" + schemeID + "',"//scheme_id
				+ " 1,"//my_order
				+ DbConnectionFactory.getDBTrue() + ","//resolved
				+ DbConnectionFactory.getDBFalse() + ","//escalation_enable
				+ " null,"//escalation_action
				+ " 0) ");//escalation_time
		
		String secondID = UUIDGenerator.generateUuid();
		dc.executeStatement("insert into workflow_step (id, name, scheme_id, my_order, resolved, escalation_enable, escalation_action, escalation_time) "
				+ "values ('" + secondID + "',"//id
				+ " 'Content Entry',"//name
				+ " '" + schemeID + "',"//scheme_id
				+ " 2,"//my_order
				+ DbConnectionFactory.getDBFalse() + ","//resolved
				+ DbConnectionFactory.getDBFalse() + ","//escalation_enable
				+ " null,"//escalation_action
				+ " 0) ");//escalation_time
		
		String thirdID = UUIDGenerator.generateUuid();
		dc.executeStatement("insert into workflow_step (id, name, scheme_id, my_order, resolved, escalation_enable, escalation_action, escalation_time) "
				+ "values ('" + thirdID + "',"//id
				+ " 'Closed',"//name
				+ " '" + schemeID + "',"//scheme_id
				+ " 3,"//my_order
				+ DbConnectionFactory.getDBTrue() + ","//resolved
				+ DbConnectionFactory.getDBFalse() + ","//escalation_enable
				+ " null,"//escalation_action
				+ " 0) ");//escalation_time
		
		//Workflow Actions
		String workflowIDAssign = UUIDGenerator.generateUuid();
		dc.executeStatement("insert into workflow_action (id, step_id, name, condition_to_progress, next_step_id, next_assign, my_order, assignable, commentable, icon, use_role_hierarchy_assign, requires_checkout) "
				+ "values ('" + workflowIDAssign + "',"//id
				+ " '" + initialID + "',"//step_id
				+ " 'Assign Workflow',"//name
				+ " '',"//condition_to_progress
				+ " '" + secondID + "',"//next_step_id
				+ " '" + APILocator.getRoleAPI().loadCMSAdminRole().getId() + "',"//next_assign
				+ " 1,"//my_order
				+ DbConnectionFactory.getDBTrue() + ","//assignable
				+ DbConnectionFactory.getDBTrue() + ","//commentable
				+ " 'workflowIcon',"//icon
				+ " 1,"//use_role_hierarchy_assign
				+ " 1)");//requires_checkout
		
		String workflowIDReassign = UUIDGenerator.generateUuid();
		dc.executeStatement("insert into workflow_action (id, step_id, name, condition_to_progress, next_step_id, next_assign, my_order, assignable, commentable, icon, use_role_hierarchy_assign, requires_checkout) "
				+ "values ('" + workflowIDReassign + "',"//id
				+ " '" + secondID + "',"//step_id
				+ " 'Reassign Workflow',"//name
				+ " '',"//condition_to_progress
				+ " '" + secondID + "',"//next_step_id
				+ " '" + APILocator.getRoleAPI().loadCMSAdminRole().getId() + "',"//next_assign
				+ " 1,"//my_order
				+ DbConnectionFactory.getDBTrue() + ","//assignable
				+ DbConnectionFactory.getDBTrue() + ","//commentable
				+ " 'cancelIcon',"//icon
				+ " 1,"//use_role_hierarchy_assign
				+ " 0)");//requires_checkout
		
		String workflowIDResolve = UUIDGenerator.generateUuid();
		dc.executeStatement("insert into workflow_action (id, step_id, name, condition_to_progress, next_step_id, next_assign, my_order, assignable, commentable, icon, use_role_hierarchy_assign, requires_checkout) "
				+ "values ('" + workflowIDResolve + "',"//id
				+ " '" + secondID + "',"//step_id
				+ " 'Resolve Workflow',"//name
				+ " '',"//condition_to_progress
				+ " '" + thirdID + "',"//next_step_id
				+ " '" + APILocator.getRoleAPI().loadCMSAdminRole().getId() + "',"//next_assign
				+ " 2,"//my_order
				+ DbConnectionFactory.getDBFalse() + ","//assignable
				+ DbConnectionFactory.getDBFalse() + ","//commentable
				+ " 'closeIcon',"//icon
				+ " 0,"//use_role_hierarchy_assign
				+ " 0)");//requires_checkout
		
		String workflowIDReopen = UUIDGenerator.generateUuid();
		dc.executeStatement("insert into workflow_action (id, step_id, name, condition_to_progress, next_step_id, next_assign, my_order, assignable, commentable, icon, use_role_hierarchy_assign, requires_checkout) "
				+ "values ('" + workflowIDReopen + "',"//id
				+ " '" + thirdID + "',"//step_id
				+ " 'Reopen',"//name
				+ " '',"//condition_to_progress
				+ " '" + secondID + "',"//next_step_id
				+ " '" + APILocator.getRoleAPI().loadCMSAdminRole().getId() + "',"//next_assign
				+ " 1,"//my_order
				+ DbConnectionFactory.getDBTrue() + ","//assignable
				+ DbConnectionFactory.getDBTrue() + ","//commentable
				+ " 'workflowIcon',"//icon
				+ " 1,"//use_role_hierarchy_assign
				+ " 0)");//requires_checkout
		
		//Permission for Workflow Actions
		dc.executeStatement("insert into permission (permission_type, inode_id, roleid, permission) "
				+ "values ('individual', "//permission_type
				+ "'" + workflowIDAssign + "', "//inode_id
				+ "'" + APILocator.getRoleAPI().loadCMSAnonymousRole().getId() + "', "//roleid
				+ "1)");//permission
		
		dc.executeStatement("insert into permission (permission_type, inode_id, roleid, permission) "
				+ "values ('individual', "//permission_type
				+ "'" + workflowIDReassign + "', "//inode_id
				+ "'" + APILocator.getRoleAPI().loadCMSAnonymousRole().getId() + "', "//roleid
				+ "1)");//permission
		
		dc.executeStatement("insert into permission (permission_type, inode_id, roleid, permission) "
				+ "values ('individual', "//permission_type
				+ "'" + workflowIDResolve + "', "//inode_id
				+ "'" + APILocator.getRoleAPI().loadCMSAnonymousRole().getId() + "', "//roleid
				+ "1)");//permission
		
		dc.executeStatement("insert into permission (permission_type, inode_id, roleid, permission) "
				+ "values ('individual', "//permission_type
				+ "'" + workflowIDReopen + "', "//inode_id
				+ "'" + APILocator.getRoleAPI().loadCMSAnonymousRole().getId() + "', "//roleid
				+ "1)");//permission

	}

	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		try {
			DbConnectionFactory.getConnection().setAutoCommit(true);
		} catch (SQLException e) {
			throw new DotDataException(e.getMessage(), e);
		}
		try {
			DotConnect dc=new DotConnect();
			createDefaultScheme(dc);
		} catch (SQLException e) {
			throw new DotRuntimeException(e.getMessage(),e);
		}
	}
	
	@Override
	public boolean forceRun() {
		return true;
	}

}
