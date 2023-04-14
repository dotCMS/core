/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.rules;

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
		}
		return null;
	}

    protected String INSERT_RULE = "insert into dot_rule (id, name, fire_on, short_circuit, parent_id, folder, priority, enabled, mod_date) values (?,?,?,?,?,?,?,?,?)";
    protected String UPDATE_RULE = "update dot_rule set name=?, fire_on=?, short_circuit=?, parent_id=?, folder=?, priority=?, enabled=?, mod_date=? where id=?";
    protected String SELECT_ALL_RULES_BY_PARENT_ID = "select * from dot_rule where parent_id=?";
    protected String SELECT_ALL_RULES = "select * from dot_rule";
    protected String SELECT_ENABLED_RULES_BY_PARENT_ID = "select * from dot_rule where parent_id=? and enabled="+DbConnectionFactory.getDBTrue();
    protected String SELECT_RULES_BY_PARENT_ID_FIRE_ON = "select * from dot_rule where parent_id=? and fire_on=? and enabled="+DbConnectionFactory.getDBTrue();
    protected String SELECT_RULES_BY_FOLDER = "select * from dot_rule where folder=?";
    protected String SELECT_RULE_BY_ID = "select * from dot_rule where id=?";
    protected String DELETE_RULE_BY_ID = "delete from dot_rule where id=?";

    protected String INSERT_CONDITION = "insert into rule_condition (id, conditionlet, condition_group, comparison, operator, priority, mod_date) values (?,?,?,?,?,?,?)";
    protected String UPDATE_CONDITION = "update rule_condition set conditionlet=?, condition_group=?, comparison=?, operator=?, priority=?, mod_date=? where id=?";
    protected String SELECT_CONDITION_BY_ID = "select * from rule_condition where id=?";
    protected String SELECT_CONDITIONS_BY_GROUP = "select * from rule_condition where condition_group=? order by priority asc";
    protected String DELETE_CONDITION_BY_ID = "delete from rule_condition where id=?";

    protected String INSERT_CONDITION_GROUP = "insert into rule_condition_group (id, rule_id, operator, priority, mod_date) values (?,?,?,?,?)";
    protected String UPDATE_CONDITION_GROUP = "update rule_condition_group set rule_id=?, operator=?, priority=?, mod_date=? where id=?";
    protected String SELECT_CONDITION_GROUPS_BY_RULE = "select * from rule_condition_group where rule_id=? order by priority asc";
    protected String SELECT_CONDITION_GROUP_BY_ID = "select * from rule_condition_group where id=?";
    protected String DELETE_CONDITION_GROUP_BY_ID = "delete from rule_condition_group where id=?";

    protected String INSERT_CONDITION_VALUE = "insert into rule_condition_value (id, condition_id, paramkey, value, priority) values (?,?,?,?,?)";
    protected String UPDATE_CONDITION_VALUE = "update rule_condition_value set condition_id=?, paramkey=?, value=?, priority=? where id=?";
    protected String SELECT_CONDITION_VALUES_BY_CONDITION = "select * from rule_condition_value where condition_id=?";
    protected String SELECT_CONDITION_VALUE_BY_ID = "select * from rule_condition_value where id=?";
    protected String DELETE_CONDITION_VALUES_BY_CONDITION = "delete from rule_condition_value where condition_id=?";
    protected String DELETE_CONDITION_VALUE_BY_ID = "delete from rule_condition_value where id=?";


    protected String INSERT_RULE_ACTION = "insert into rule_action (id, rule_id, priority, actionlet) values (?,?,?,?)";
    protected String UPDATE_RULE_ACTION = "update rule_action set rule_id=?, priority=?, actionlet=? where id=?";
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
