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

    protected String INSERT_RULE = "insert into dot_rule (id, name, fire_policy, short_circuit, host, folder, fire_order, enabled) values (?,?,?,?,?,?,?,?)";
    protected String UPDATE_RULE = "update dot_rule set name=?, fire_policy=?, short_circuit=?, host=?, folder=?, fire_order=?, enabled=? where id=?";
    protected String SELECT_RULES_BY_HOST = "select * from dot_rule where host=?";
    protected String SELECT_RULES_BY_FOLDER = "select * from dot_rule where folder=?";
    protected String SELECT_RULE_BY_ID = "select * from dot_rule where id=?";
    protected String DELETE_RULE_BY_ID = "delete from dot_rule where id=?";

    protected String INSERT_CONDITION = "insert into rule_condition (id, name, rule_id, conditionlet, rel_operator, value) values (?,?,?,?,?,?,?,?)";
    protected String UPDATE_CONDITION = "update rule_condition set name=?, rule_id=?, conditionlet=?, rel_operator=?, value=? where id=?";
    protected String SELECT_CONDITION_BY_ID = "select * from rule_condition where id=?";
    protected String SELECT_CONDITIONS_BY_RULE = "select * from rule_condition where rule_id=?";
    protected String DELETE_CONDITION_BY_ID = "delete from rule_condition where id=?";

    protected String INSERT_RULE_ACTION = "insert into rule_action (id, name, rule_id, fire_order, actionlet) values (?,?,?,?,?)";
    protected String UPDATE_RULE_ACTION = "update rule_action set name=?, rule_id=?, fire_order=?, actionlet=? where id=?";
    protected String SELECT_RULE_ACTIONS_BY_RULE = "select * from rule_action where rule_id=?";
    protected String SELECT_RULE_ACTION_BY_ID = "select * from rule_action where id=?";
    protected String DELETE_RULE_ACTION_BY_ID = "delete from rule_action where id=?";



}
