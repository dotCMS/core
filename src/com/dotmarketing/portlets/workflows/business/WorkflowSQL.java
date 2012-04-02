package com.dotmarketing.portlets.workflows.business;

import com.dotmarketing.db.DbConnectionFactory;

abstract class WorkflowSQL {
    protected static final String MYSQL = "MySQL";
    protected static final String POSTGRESQL = "PostgreSQL";
    protected static final String ORACLE = "Oracle";
    protected static final String MSSQL = "Microsoft SQL Server";

    static protected WorkflowSQL getInstance() {
	String x = DbConnectionFactory.getDBType();
	if (MYSQL.equals(x)) {
	    return new MySQLWorkflowSQL();
	} else if (POSTGRESQL.equals(x)) {
	    return new PostgresWorkflowSQL();
	} else if (MSSQL.equals(x)) {
	    return new MSSQLWorkflowSQL();
	} else {
	    return new OracleWorkflowSQL();
	}
    }

    protected static String SELECT_SCHEMES = "select * from workflow_scheme where (archived = ? or archived = ?) order by name";

    protected static String SELECT_SCHEME = "select * from workflow_scheme where id = ?";

    protected static String SELECT_DEFAULT_SCHEME = "select * from workflow_scheme where default_scheme = " + DbConnectionFactory.getDBTrue() + " ";

    protected static String UPDATE_SCHEME_SET_TO_DEFAULT = "update workflow_scheme set default_scheme = " + DbConnectionFactory.getDBTrue() + " where id = ? ";

    protected static String UPDATE_STEPS_BY_STRUCT = "update workflow_task set status = ? where webasset in (select identifier from contentlet where contentlet.structure_inode = ? group by identifier) ";
    protected static String DELETE_SCHEME_FOR_STRUCT = "delete from workflow_scheme_x_structure where structure_id = ?";
    protected static String INSERT_SCHEME_FOR_STRUCT = "insert into workflow_scheme_x_structure (id, scheme_id, structure_id) values ( ?, ?, ?)";
    protected static String SELECT_SCHEME_BY_STRUCT = "select * from workflow_scheme, workflow_scheme_x_structure where workflow_scheme.id = workflow_scheme_x_structure.scheme_id and workflow_scheme_x_structure.structure_id = ?";

    protected static String INSERT_SCHEME = "insert into workflow_scheme (id, name, description, archived, mandatory, entry_action_id, default_scheme) values (?,?,?,?,?,?,?)";

    protected static String UPDATE_SCHEME = "update workflow_scheme set name = ?,description =?, archived=?,mandatory=?,entry_action_id=? where id =? ";

    protected static String SELECT_STEPS_BY_SCHEME = "select * from workflow_step where scheme_id = ? order by  my_order";
    protected static String SELECT_ACTIONS_BY_STEP = "select * from workflow_action where step_id = ? order by  my_order";
    protected static String SELECT_ACTION = "select * from workflow_action where id = ? ";
    protected static String INSERT_ACTION = "insert into workflow_action (id, step_id, name, condition_to_progress, next_step_id, next_assign, my_order, assignable, commentable, icon, use_role_hierarchy_assign, requires_checkout) values (?, ?, ?, ?, ?, ?, ?,?, ?, ?,?,?)";
    protected static String UPDATE_ACTION = "update  workflow_action set step_id=?, name=?,  condition_to_progress=?, next_step_id=?, next_assign=?, my_order=?, assignable=?, commentable=?, icon=?, use_role_hierarchy_assign=?,requires_checkout=? where id=?";
    protected static String DELETE_ACTION = "delete from workflow_action where id = ? ";

    protected static String SELECT_STEP = "select * from workflow_step where id = ? ";
    protected static String INSERT_STEP = "insert into workflow_step (id, name, scheme_id,my_order,resolved) values (?, ?, ?, ?, ?) ";
    protected static String UPDATE_STEP = "update workflow_step set name=?, scheme_id=?, my_order=?, resolved = ? where id = ?";
    protected static String DELETE_STEP = "delete from workflow_step where id = ?";
    protected static String SELECT_STEP_BY_CONTENTLET = "select workflow_step.* from workflow_step, workflow_task where workflow_task.status = workflow_step.id and workflow_task.webasset= ?";
    protected static String RESET_CONTENTLET_STEPS = "update workflow_task set status = ? where webasset= ?";

    protected static String SELECT_ACTION_CLASSES_BY_ACTION = "select * from workflow_action_class where action_id = ? order by  my_order";
    protected static String SELECT_ACTION_CLASS = "select * from workflow_action_class where id = ? ";
    protected static String INSERT_ACTION_CLASS = "insert into workflow_action_class (id, action_id, name, my_order, clazz) values (?,?, ?, ?, ?)";
    protected static String UPDATE_ACTION_CLASS = "update workflow_action_class set action_id= ?, name=?, my_order=?, clazz=? where id =?";
    protected static String DELETE_ACTION_CLASS = "delete from workflow_action_class where id =?";
    protected static String DELETE_ACTION_CLASS_BY_ACTION = "delete from workflow_action_class where action_id =?";

    protected static String SELECT_ACTION_CLASS_PARAMS_BY_ACTIONCLASS = "select * from workflow_action_class_pars where workflow_action_class_id = ?";
    protected static String SELECT_ACTION_CLASS_PARAM = "select * from workflow_action_class_pars where id = ? ";
    protected static String INSERT_ACTION_CLASS_PARAM = "insert into workflow_action_class_pars (id,workflow_action_class_id,key,value) values (?,?, ?, ?)";
    protected static String UPDATE_ACTION_CLASS_PARAM = "update workflow_action_class_pars set workflow_action_class_id= ?, key=?, value=? where id =?";
    protected static String DELETE_ACTION_CLASS_PARAM_BY_ACTION_CLASS = "delete from workflow_action_class_pars where workflow_action_class_id =?";

    // chri
    protected static String UPDATE_USER_ASSIGNTO_TASK = "update workflow_task set assigned_to = ? where id = ?";
    protected static String RETRIEVE_LAST_STEP_ACTIONID = "select  * from workflow_history where workflowtask_id = ? order by creation_date desc";
    protected static String RETRIEVE_TASK = "select  * from workflow_history where workflowtask_id = ? order by creation_date desc";
    // chri

}
