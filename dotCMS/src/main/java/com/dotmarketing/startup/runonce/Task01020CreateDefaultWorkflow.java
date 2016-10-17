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

/**
 * Creates the default workflow (if it does not exist yet) at the end of the 
 * upgrade process.
 * 
 * @author wezell
 * @version 2.0
 * @since 03-22-2013
 *
 */
public class Task01020CreateDefaultWorkflow implements StartupTask {

	/**
	 * Creates the default workflow scheme, if it doesn't exist yet. The basic
	 * structure of a workflow is composed of 4 parts:
	 * <ol>
	 * <li>Workflow scheme.</li>
	 * <li>Workflow steps associated to the scheme.</li>
	 * <li>Workflow actions associated to the steps.</li>
	 * <li>Permissions associated to the workflow actions.</li>
	 * </ol>
	 * 
	 * @param dc
	 *            - Database connection object.
	 * @throws SQLException
	 *             There is something wrong with a SQL statement.
	 * @throws DotDataException
	 *             An error occurred when persisting the data.
	 */
	private void createDefaultScheme(DotConnect dc) throws SQLException, DotDataException {
		// ===== Default Scheme ===== //
		String schemeID = addDefaultWorkflowScheme(dc);
		
		// ===== Workflow Steps ===== //
		String initialID = addWorkflowStep(dc, schemeID, "Initial State", "1",
				DbConnectionFactory.getDBTrue(),
				DbConnectionFactory.getDBFalse());
		String secondID = addWorkflowStep(dc, schemeID, "Content Entry", "2",
				DbConnectionFactory.getDBFalse(),
				DbConnectionFactory.getDBFalse());
		String thirdID = addWorkflowStep(dc, schemeID, "Closed", "3",
				DbConnectionFactory.getDBFalse(),
				DbConnectionFactory.getDBFalse());
		
		// ===== Workflow Actions ===== //
		String workflowIDAssign = addWorkflowAction(dc, "Assign Workflow",
				initialID, secondID, "1", DbConnectionFactory.getDBTrue(),
				DbConnectionFactory.getDBTrue(), "workflowIcon",
				DbConnectionFactory.getDBTrue(),
				DbConnectionFactory.getDBTrue());
		String workflowIDReassign = addWorkflowAction(dc, "Reassign Workflow",
				secondID, secondID, "1", DbConnectionFactory.getDBTrue(),
				DbConnectionFactory.getDBTrue(), "cancelIcon",
				DbConnectionFactory.getDBTrue(),
				DbConnectionFactory.getDBFalse());
		String workflowIDResolve = addWorkflowAction(dc, "Resolve Workflow",
				secondID, thirdID, "2", DbConnectionFactory.getDBFalse(),
				DbConnectionFactory.getDBFalse(), "closeIcon",
				DbConnectionFactory.getDBFalse(),
				DbConnectionFactory.getDBFalse());
		String workflowIDReopen = addWorkflowAction(dc, "Reopen", thirdID,
				secondID, "1", DbConnectionFactory.getDBTrue(),
				DbConnectionFactory.getDBTrue(), "workflowIcon",
				DbConnectionFactory.getDBTrue(),
				DbConnectionFactory.getDBFalse());

		// ===== Permissions for Workflow Actions ===== //
		addPermissionToAction(dc, workflowIDAssign);
		addPermissionToAction(dc, workflowIDReassign);
		addPermissionToAction(dc, workflowIDResolve);
		addPermissionToAction(dc, workflowIDReopen);
	}
	
	/**
	 * Adds a default workflow scheme, if it does not exist yet.
	 * 
	 * @param dc
	 *            - The database connection object.
	 * @return The ID of the created/updated workflow scheme.
	 * @throws DotDataException
	 *             There is something wrong with a SQL statement.
	 * @throws SQLException
	 *             An error occurred when persisting the data.
	 */
	private String addDefaultWorkflowScheme(DotConnect dc)
			throws DotDataException, SQLException {
		String schemeID = UUIDGenerator.generateUuid();
		dc.setSQL("SELECT id FROM workflow_scheme WHERE name = 'Default Scheme'");
		List<Map<String, String>> results = null;
		results = dc.loadResults();
		if (results != null && results.size() > 0) {
			// The scheme already exists, use the existing ID instead
			schemeID = results.get(0).get("id");
		} else {
			dc.executeStatement("insert into workflow_scheme (id, name, description, archived, mandatory, entry_action_id, default_scheme) "
					+ "values ('"
					+ schemeID + "',"// id
					+ "'Default Scheme'," // name
					+ "'This is the default workflow scheme that will be applied to all content'," // description
					+ DbConnectionFactory.getDBFalse() + "," // archived
					+ DbConnectionFactory.getDBFalse() + "," // mandatory
					+ "null,"// entry_action_id
					+ DbConnectionFactory.getDBTrue() + ")"); // default_scheme
		}
		dc.executeStatement("update workflow_scheme set default_scheme = "
				+ DbConnectionFactory.getDBTrue()
				+ " where id = '" + schemeID + "' ");
		return schemeID;
	}
	
	/**
	 * Adds a specific workflow step to the scheme, if it does not exist yet.
	 * 
	 * @param dc
	 *            - The database connection object.
	 * @param schemeID
	 *            - The ID of the scheme.
	 * @param stepName
	 *            - Name of the workflow step.
	 * @param order
	 *            - Step order.
	 * @param resolved
	 * @param escalationEnabled
	 * @return The ID of the existing/created step.
	 * @throws DotDataException
	 *             There is something wrong with a SQL statement.
	 * @throws SQLException
	 *             An error occurred when persisting the data.
	 */
	private String addWorkflowStep(DotConnect dc, String schemeID,
			String stepName, String order, String resolved,
			String escalationEnabled) throws DotDataException, SQLException {
		dc.setSQL("SELECT id FROM workflow_step WHERE scheme_id = ? AND name = '"
				+ stepName + "'");
		dc.addParam(schemeID);
		List<Map<String, String>> results = null;
		results = dc.loadResults();
		String stepID = null;
		if (results != null && results.size() > 0) {
			stepID = results.get(0).get("id");
		} else {
			stepID = UUIDGenerator.generateUuid();
			dc.executeStatement("insert into workflow_step (id, name, scheme_id, my_order, resolved, escalation_enable, escalation_action, escalation_time) "
					+ "values ('" + stepID + "'," // id
					+ " '" + stepName + "'," // name
					+ " '" + schemeID + "'," // scheme_id
					+ " " + order + "," // my_order
					+ resolved + "," // resolved
					+ escalationEnabled + "," // escalation_enable
					+ " null,"// escalation_action
					+ " 0) ");// escalation_time
		}
		return stepID;
	}
	
	/**
	 * Adds a workflow action associated to a specific step, if it does not
	 * exist yet.
	 * 
	 * @param dc
	 *            - The database connection object.
	 * @param actionName
	 *            - Name of the workflow action.
	 * @param stepID
	 *            - The ID of the associated workflow step.
	 * @param nextStepID
	 *            - The ID of the next step to follow.
	 * @param order
	 *            - Action order.
	 * @param assignable
	 * @param commentable
	 * @param icon
	 *            - ID of the workflow icon.
	 * @param useRoleHierarchyAssign
	 * @param requiresCheckout
	 * @return The ID of the existing/created action.
	 * @throws DotDataException
	 *             There is something wrong with a SQL statement.
	 * @throws SQLException
	 *             An error occurred when persisting the data.
	 */
	private String addWorkflowAction(DotConnect dc, String actionName,
			String stepID, String nextStepID, String order, String assignable,
			String commentable, String icon, String useRoleHierarchyAssign,
			String requiresCheckout) throws DotDataException, SQLException {
		dc.setSQL("SELECT id FROM workflow_action WHERE step_id = ? AND name = '"
				+ actionName + "'");
		dc.addParam(stepID);
		List<Map<String, String>> results = null;
		results = dc.loadResults();
		String workflowID = null;
		if (results != null && results.size() > 0) {
			workflowID = results.get(0).get("id");
		} else {
			workflowID = UUIDGenerator.generateUuid();
			dc.executeStatement("insert into workflow_action (id, step_id, name, condition_to_progress, next_step_id, next_assign, my_order, assignable, commentable, icon, use_role_hierarchy_assign, requires_checkout) "
					+ "values ('" + workflowID + "'," // id
					+ " '" + stepID + "'," // step_id
					+ " '" + actionName + "'," // name
					+ " ''," // condition_to_progress
					+ " '" + nextStepID + "'," // next_step_id
					+ " '" + APILocator.getRoleAPI().loadCMSAdminRole().getId() + "'," // next_assign
					+ " " + order + "," // my_order
					+ assignable + "," // assignable
					+ commentable + "," // commentable
					+ " '" + icon + "'," // icon
					+ " " + useRoleHierarchyAssign + "," // use_role_hierarchy_assign
					+ " " + requiresCheckout + ")"); // requires_checkout
		}
		return workflowID;
	}

	/**
	 * Adds the respective permission to a specific action, if it does not exist
	 * yet.
	 * 
	 * @param dc
	 *            - The database connection object.
	 * @param inodeID
	 *            - The ID of the workflow action.
	 * @throws DotDataException
	 *             There is something wrong with a SQL statement.
	 * @throws SQLException
	 *             An error occurred when persisting the data.
	 */
	private void addPermissionToAction(DotConnect dc, String inodeID)
			throws DotDataException, SQLException {
		String query = "insert into permission (%s permission_type, inode_id, roleid, permission) "
				+ "values (%s 'individual', " // permission_type
				+ "'%s', " // inode_id
				+ "'%s', " // roleid
				+ "1)"; // permission
		String formattedQuery = "";
		dc.setSQL("SELECT id FROM permission WHERE inode_id = ? AND permission_type = 'individual'");
		dc.addParam(inodeID);
		List<Map<String, String>> results = null;
		results = dc.loadResults();
		if (results != null && results.size() == 0) {
			if (DbConnectionFactory.isOracle()) {
				formattedQuery = String.format(query, "id,",
						"permission_seq.NEXTVAL,", inodeID, APILocator
								.getRoleAPI().loadCMSAnonymousRole().getId());
			} else if (DbConnectionFactory.isPostgres()) {
				formattedQuery = String.format(query, "id,",
						"nextval('permission_seq'),", inodeID, APILocator
								.getRoleAPI().loadCMSAnonymousRole().getId());
			} else {
				formattedQuery = String.format(query, "", "", inodeID,
						APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
			}
			dc.executeStatement(formattedQuery);
		}
	}
	
	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		try {
			DbConnectionFactory.getConnection().setAutoCommit(true);
		} catch (SQLException e) {
			throw new DotDataException(e.getMessage(), e);
		}
		try {
			DotConnect dc = new DotConnect();
			createDefaultScheme(dc);
		} catch (SQLException e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public boolean forceRun() {
		return true;
	}

}
