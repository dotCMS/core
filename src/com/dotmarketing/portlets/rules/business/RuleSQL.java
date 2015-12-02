package com.dotmarketing.portlets.rules.business;

import com.dotmarketing.db.DbConnectionFactory;

abstract class RuleSQL {

	static protected RuleSQL getInstance() {
		if (DbConnectionFactory.isMySql()) {
			return new MySQLRuleSQL();
		} else if (DbConnectionFactory.isPostgres()) {
			return new PostgresRuleSQL();
		} else if (DbConnectionFactory.isMsSql()) {
			return new MSSQLRuleSQL();
		} else if (DbConnectionFactory.isOracle()) {
			return new OracleRuleSQL();
		} else if (DbConnectionFactory.isH2()) {
		    return new H2RuleSQL();
		}
		return null;
	}

    protected String INSERT_RULE = "insert into dot_rule (id, name, fire_on, short_circuit, host, folder, priority, enabled, mod_date) values (?,?,?,?,?,?,?,?,?)";
    protected String UPDATE_RULE = "update dot_rule set name=?, fire_on=?, short_circuit=?, host=?, folder=?, priority=?, enabled=?, mod_date=? where id=?";
    protected String SELECT_ALL_RULES_BY_HOST = "select * from dot_rule where host=?";
    protected String SELECT_ENABLED_RULES_BY_HOST = "select * from dot_rule where host=? and enabled="+DbConnectionFactory.getDBTrue();
    protected String SELECT_RULES_BY_HOST_FIRE_ON = "select * from dot_rule where host=? and fire_on=? and enabled="+DbConnectionFactory.getDBTrue();
    protected String SELECT_RULES_BY_FOLDER = "select * from dot_rule where folder=?";
    protected String SELECT_RULE_BY_ID = "select * from dot_rule where id=?";
    protected String DELETE_RULE_BY_ID = "delete from dot_rule where id=?";

    protected String INSERT_CONDITION = "insert into rule_condition (id, name, conditionlet, condition_group, comparison, operator, priority, mod_date) values (?,?,?,?,?,?,?,?)";
    protected String UPDATE_CONDITION = "update rule_condition set name=?, conditionlet=?, condition_group=?, comparison=?, operator=?, priority=?, mod_date=? where id=?";
    protected String SELECT_CONDITION_BY_ID = "select * from rule_condition where id=?";
    protected String SELECT_CONDITIONS_BY_GROUP = "select * from rule_condition where condition_group=?";
    protected String DELETE_CONDITION_BY_ID = "delete from rule_condition where id=?";
    protected String DELETE_CONDITION_BY_GROUP = "delete from rule_condition where condition_group=?";

    protected String INSERT_CONDITION_GROUP = "insert into rule_condition_group (id, rule_id, operator, priority, mod_date) values (?,?,?,?,?)";
    protected String UPDATE_CONDITION_GROUP = "update rule_condition_group set rule_id=?, operator=?, priority=?, mod_date=? where id=?";
    protected String SELECT_CONDITION_GROUPS_BY_RULE = "select * from rule_condition_group where rule_id=?";
    protected String SELECT_CONDITION_GROUP_BY_ID = "select * from rule_condition_group where id=?";
    protected String DELETE_CONDITION_GROUP_BY_ID = "delete from rule_condition_group where id=?";

    protected String INSERT_CONDITION_VALUE = "insert into rule_condition_value (id, condition_id, paramkey, value, priority) values (?,?,?,?,?)";
    protected String UPDATE_CONDITION_VALUE = "update rule_condition_value set condition_id=?, paramkey=?, value=?, priority=? where id=?";
    protected String SELECT_CONDITION_VALUES_BY_CONDITION = "select * from rule_condition_value where condition_id=?";
    protected String SELECT_CONDITION_VALUE_BY_ID = "select * from rule_condition_value where id=?";
    protected String DELETE_CONDITION_VALUES_BY_CONDITION = "delete from rule_condition_value where condition_id=?";
    protected String DELETE_CONDITION_VALUE_BY_ID = "delete from rule_condition_value where id=?";


    protected String INSERT_RULE_ACTION = "insert into rule_action (id, name, rule_id, priority, actionlet) values (?,?,?,?,?)";
    protected String UPDATE_RULE_ACTION = "update rule_action set name=?, rule_id=?, priority=?, actionlet=? where id=?";
    protected String SELECT_RULE_ACTIONS_BY_RULE = "select * from rule_action where rule_id=?";
    protected String SELECT_RULE_ACTION_BY_ID = "select * from rule_action where id=?";
    protected String DELETE_RULE_ACTION_BY_ID = "delete from rule_action where id=?";
    protected String DELETE_RULE_ACTION_BY_RULE = "delete from rule_action where rule_id=?";

    protected String INSERT_RULE_ACTION_PARAM = "insert into rule_action_pars (id, rule_action_id, paramkey, value) values (?,?,?,?)";
    protected String UPDATE_RULE_ACTION_PARAM = "update rule_action_pars set rule_action_id=?, paramkey=?, value=? where id=?";
    protected String SELECT_RULE_ACTIONS_PARAMS = "select * from rule_action_pars where rule_action_id=?";
    protected String SELECT_RULE_ACTION_PARAMS = "select * from rule_action_pars where id=?";
    protected String DELETE_RULE_ACTION_PARAM_BY_ID = "delete from rule_action_pars where id=?";
    protected String DELETE_RULE_ACTION_PARAM_BY_ACTION = "delete from rule_action_pars where rule_action_id=?";


}
