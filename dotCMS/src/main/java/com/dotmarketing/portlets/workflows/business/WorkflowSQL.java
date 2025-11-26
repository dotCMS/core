package com.dotmarketing.portlets.workflows.business;

/**
 * This utility class provides all the SQL statements used to interact with the Workflow tables in
 * dotCMS. Here you can find CRUD queries for Workflow Schemes, Steps, Actions, and the
 * relationships between them.
 *
 * @author root
 * @since Mar 22nd, 2012
 */
public final class WorkflowSQL {

	private WorkflowSQL() {
		// Utility class
	}

	public static final String SELECT_SCHEMES = "select * from workflow_scheme where (archived = ? or archived = ?) order by name";

	public static final String SELECT_SCHEME= "select * from workflow_scheme where id = ?";
	public static final String SELECT_SCHEME_NAME= "select * from workflow_scheme where name = ?";
	public static final String SELECT_SCHEME_BY_VARIABLE_NAME = "SELECT * FROM workflow_scheme WHERE lower(variable_name) LIKE ?";

	public static final String SELECT_TASK_NULL_BY_STRUCT= "select task.* FROM workflow_task task INNER JOIN contentlet con ON con.identifier = task.webasset INNER JOIN contentlet_version_info cvi ON cvi.working_inode = con.inode"
			+ " WHERE task.status is NULL AND con.structure_inode=?";

	public static final String SELECT_COUNT_BY_VARIABLE_NAME =
			"SELECT count(*) AS test FROM workflow_scheme "
					+ "WHERE lower(variable_name) LIKE ?";

	public static final String SELECT_NULL_TASK_CONTENTLET_FOR_WORKFLOW = "select c.identifier from contentlet c join workflow_scheme_x_structure wss on c.structure_inode = wss.structure_id and wss.scheme_id = ? where c.identifier in (select t.webasset from workflow_task t where t.status is null)";
	public static final String SELECT_TASK_STEPS_TO_CLEAN_BY_STRUCT= "select * from  workflow_task where webasset in (select identifier from contentlet where contentlet.structure_inode = ? group by identifier) ";
	public static final String UPDATE_STEPS_BY_STRUCT= "update workflow_task set status = ? where webasset in (select identifier from contentlet where contentlet.structure_inode = ? group by identifier) ";
	public static final String DELETE_SCHEME_FOR_STRUCT= "delete from workflow_scheme_x_structure where structure_id = ?";
	public static final String INSERT_SCHEME_FOR_STRUCT= "insert into workflow_scheme_x_structure (id, scheme_id, structure_id) values ( ?, ?, ?)";
	public static final String SELECT_SCHEME_BY_STRUCT= "select * from workflow_scheme, workflow_scheme_x_structure where workflow_scheme.id = workflow_scheme_x_structure.scheme_id and workflow_scheme_x_structure.structure_id = ?";

	public static final String INSERT_SCHEME = "insert into workflow_scheme (id, name, variable_name, description, archived, mandatory, default_scheme, mod_date) values (?,?,?,?,?,?,?,?)";

	public static final String UPDATE_SCHEME= "update workflow_scheme set name = ?, description =?, archived=?, mandatory=?, mod_date=? where id =? ";

	public static final String SELECT_STEPS_BY_SCHEME= "select * from workflow_step where scheme_id = ? order by  my_order";
	public static final String SELECT_ACTIONS_BY_SCHEME= "select * from workflow_action where scheme_id = ? order by  name";
	public static final String SELECT_ACTIONS_BY_STEP =
		"select workflow_action.* from workflow_action join workflow_action_step on workflow_action.id = workflow_action_step.action_id  where workflow_action_step.step_id = ? order by  action_order";

	public static final String SELECT_STEPS_BY_ACTION =
			"select workflow_step.* from workflow_step join workflow_action_step on workflow_step.id = workflow_action_step.step_id where action_id = ? order by my_order asc";
	public static final String SELECT_ACTION= "select * from workflow_action where id = ? ";

	/**
	 * Select to get the action associated to the step.
	 */
	public static final String SELECT_ACTION_BY_STEP  = "select workflow_action.* from workflow_action join workflow_action_step on workflow_action.id = workflow_action_step.action_id  where workflow_action_step.action_id = ? and workflow_action_step.step_id = ?";

	/**
	 * Select to get the steps ids associated to the action
	 */
	public static final String SELECT_STEPS_ID_BY_ACTION  = "select workflow_action_step.step_id as stepid from workflow_action_step  where workflow_action_step.action_id = ?";

	public static final String DELETE_SYSTEM_ACTION_BY_SCHEME_OR_CONTENT_TYPE = "delete from workflow_action_mappings where scheme_or_content_type=?";
	public static final String DELETE_SYSTEM_ACTION_BY_WORKFLOW_ACTION_ID = "delete from workflow_action_mappings where workflow_action=?";
	public static final String DELETE_SYSTEM_ACTION_BY_IDENTIFIER = "delete from workflow_action_mappings where id=?";
	public static final String SELECT_SYSTEM_ACTION_BY_IDENTIFIER = "select * from workflow_action_mappings where id=?";
	public static final String SELECT_SYSTEM_ACTION_BY_SCHEME_OR_CONTENT_TYPE_MAPPING = "select * from workflow_action_mappings where scheme_or_content_type=?";
	public static final String SELECT_SYSTEM_ACTION_BY_SYSTEM_ACTION_AND_SCHEMES      = "select * from workflow_action_mappings where action=? and scheme_or_content_type in (%s)";
	public static final String SELECT_SYSTEM_ACTION_BY_CONTENT_TYPES     			   = "select * from workflow_action_mappings where scheme_or_content_type in (%s)";
	public static final String SELECT_SYSTEM_ACTION_BY_SYSTEM_ACTION_AND_SCHEME_OR_CONTENT_TYPE_MAPPING = "select * from workflow_action_mappings where action=? and scheme_or_content_type=?";
	public static final String SELECT_SYSTEM_ACTION_BY_WORKFLOW_ACTION = "select * from workflow_action_mappings where workflow_action=?";
	public static final String INSERT_SYSTEM_ACTION_WORKFLOW_ACTION_MAPPING = "insert into workflow_action_mappings(id, action, workflow_action, scheme_or_content_type) values (?,?,?,?)";
	public static final String UPDATE_SYSTEM_ACTION_WORKFLOW_ACTION_MAPPING = "update workflow_action_mappings set action=?, workflow_action=?, scheme_or_content_type=? where id=?";

	public static final String INSERT_ACTION_FOR_STEP = "insert into workflow_action_step(action_id, step_id, action_order) values (?,?,?)";
	public static final String UPDATE_ACTION_FOR_STEP_ORDER = "update workflow_action_step set action_order=? where action_id=? and step_id=?";

	public static final String INSERT_ACTION= "insert into workflow_action (id, scheme_id, name, condition_to_progress, next_step_id, next_assign, my_order, assignable, commentable, icon, use_role_hierarchy_assign, requires_checkout, show_on, metadata) values (?, ?, ?, ?, ?, ?, ?,?, ?, ?,?,?,?, ? ::jsonb)";

	public static final String UPDATE_ACTION= "update  workflow_action set scheme_id=?, name=?,  condition_to_progress=?, next_step_id=?, next_assign=?, my_order=?, assignable=?, commentable=?, icon=?, use_role_hierarchy_assign=?,requires_checkout=?,show_on=?, metadata = ? ::jsonb where id=?";
	public static final String DELETE_ACTION= "delete from workflow_action where id = ? ";
	public static final String DELETE_ACTION_STEP     = "delete from workflow_action_step where action_id =? and step_id =? ";
	public static final String DELETE_ACTIONS_STEP    = "delete from workflow_action_step where step_id   =? ";
	public static final String DELETE_ACTIONS_BY_STEP = "delete from workflow_action_step where action_id =? ";

	public static final String SELECT_STEP= "select * from workflow_step where id = ? ";
	public static final String INSERT_STEP= "insert into workflow_step (id, name, scheme_id,my_order,resolved,escalation_enable,escalation_action,escalation_time) values (?, ?, ?, ?, ?, ?, ?, ?) ";
	public static final String UPDATE_STEP= "update workflow_step set name=?, scheme_id=?, my_order=?, resolved = ?, escalation_enable = ?, escalation_action=?, escalation_time = ? where id = ?";
	public static final String DELETE_STEP= "delete from workflow_step where id = ?";
	public static final String SELECT_STEP_BY_CONTENTLET= "select workflow_task.id as workflowid, workflow_step.* from workflow_step join workflow_task on workflow_task.status = workflow_step.id where workflow_task.webasset= ? and workflow_task.language_id = ?";
	public static final String SELECT_COUNT_CONTENTLES_BY_STEP= "select count(workflow_task.id) as count from workflow_task join workflow_step on workflow_task.status=workflow_step.id where workflow_step.id=?";

	public static final String SELECT_ACTION_CLASSES_BY_CLASS= "select * from workflow_action_class where clazz = ? order by  my_order";
	public static final String SELECT_ACTION_CLASSES_BY_ACTION= "select * from workflow_action_class where action_id = ? order by  my_order";
	public static final String SELECT_ACTION_CLASS= "select * from workflow_action_class where id = ? ";
	public static final String INSERT_ACTION_CLASS= "insert into workflow_action_class (id, action_id, name, my_order, clazz) values (?,?, ?, ?, ?)";
	public static final String UPDATE_ACTION_CLASS= "update workflow_action_class set action_id= ?, name=?, my_order=?, clazz=? where id =?";
	public static final String DELETE_ACTION_CLASS= "delete from workflow_action_class where id =?";
	public static final String DELETE_ACTION_CLASS_BY_ACTION= "delete from workflow_action_class where action_id =?";

	public static final String SELECT_ACTION_CLASS_PARAMS_BY_ACTIONCLASS= "select * from workflow_action_class_pars where workflow_action_class_id = ?";
	public static final String SELECT_ACTION_CLASS_PARAM= "select * from workflow_action_class_pars where id = ? ";
	public static final String INSERT_ACTION_CLASS_PARAM= "insert into workflow_action_class_pars (id,workflow_action_class_id,key,value) values (?,?, ?, ?)";
	public static final String UPDATE_ACTION_CLASS_PARAM= "update workflow_action_class_pars set workflow_action_class_id= ?, key=?, value=? where id =?";
	public static final String DELETE_ACTION_CLASS_PARAM_BY_ACTION_CLASS= "delete from workflow_action_class_pars where workflow_action_class_id =?";
	public static final String DELETE_ACTION_CLASS_PARAM_BY_ID="delete from workflow_action_class_pars where id=?";

    public static final String RETRIEVE_LAST_STEP_ACTIONID = "select  * from workflow_history where workflowtask_id = ? order by creation_date desc";

    public static final String SELECT_EXPIRED_TASKS = "select workflow_task.id from workflow_task join workflow_step on (workflow_task.status=workflow_step.id) "+
			"where workflow_step.resolved=false and workflow_step.escalation_enable=true and now()>workflow_task.mod_date+ "+
			"cast(cast(workflow_step.escalation_time as varchar(20))||' seconds' as interval) ";

	public static final String SELECT_TASK = "SELECT * FROM workflow_task WHERE webasset = ? AND language_id = ?";
    public static final String SELECT_TASKS_BY_STEP="select * from workflow_task where status = ?";
    public static final String INSERT_WORKFLOW_TASK = "INSERT INTO workflow_task (id, creation_date, mod_date, due_date, created_by, assigned_to, belongs_to, title, description, status, webasset, language_id) values (?,?,?,?,?,?,?,?,?,?,?,?)";
    public static final String UPDATE_WORKFLOW_TASK = "UPDATE workflow_task SET creation_date=?, mod_date=?, due_date=?, created_by=?, assigned_to=?, belongs_to=?, title=?, description=?, status=?, webasset=?, language_id=? WHERE id=?";
	public static final String SELECT_STRUCTS_FOR_SCHEME="select st.* from structure st join workflow_scheme_x_structure wss on st.inode = wss.structure_id where wss.scheme_id = ? order by st.name";
    public static final String DELETE_STRUCTS_FOR_SCHEME="delete from workflow_scheme_x_structure where scheme_id = ?";
	public static final String DELETE_SCHEME="delete from workflow_scheme where id = ?";

	public static final String INSERT_WORKFLOW_HISTORY = "INSERT INTO workflow_history (id, creation_date, made_by, change_desc, workflowtask_id, workflow_action_id, workflow_step_id) values (?,?,?,?,?,?,?)";
	public static final String UPDATE_WORKFLOW_HISTORY = "UPDATE workflow_history SET creation_date=?, made_by=?, change_desc=?, workflowtask_id=?, workflow_action_id=?, workflow_step_id=?  WHERE id=?";

	public static final String INSERT_WORKFLOW_COMMENT = "INSERT INTO workflow_comment (id, creation_date, posted_by, wf_comment, workflowtask_id) values (?,?,?,?,?)";
	public static final String UPDATE_WORKFLOW_COMMENT = "UPDATE workflow_comment SET creation_date=?, posted_by=?, wf_comment=?, workflowtask_id=? WHERE id=?";

}
