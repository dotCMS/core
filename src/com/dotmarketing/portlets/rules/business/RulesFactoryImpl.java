package com.dotmarketing.portlets.rules.business;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.repackage.org.apache.commons.beanutils.BeanUtils;
import com.dotcms.repackage.org.codehaus.jackson.map.ObjectMapper;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;

/**
 * Provides access to all the information of related to rules.
 * 
 * @author Daniel Silva
 * @version 1.0
 * @since 03-10-2015
 *
 */
public class RulesFactoryImpl implements RulesFactory {

	private static RuleSQL sql = null;
	private static RulesCache cache = null;

	public RulesFactoryImpl() {
		sql = RuleSQL.getInstance();
		cache = CacheLocator.getRulesCache();
	}

	@Override
	public List<Rule> getRulesByHost(String host) throws DotDataException {
		List<Rule> ruleList = cache.getRulesByHostId(host);
		if (ruleList == null) {
			final DotConnect db = new DotConnect();
			db.setSQL(sql.SELECT_RULES_BY_HOST);
			db.addParam(host);
			ruleList = convertListToObjects(db.loadObjectResults(), Rule.class);
			cache.addRules(ruleList);
		}
		return ruleList;
	}

	@Override
	public List<Rule> getRulesByFolder(String folder) throws DotDataException {
		List<Rule> ruleList = cache.getRulesByHostId(folder);
		if (ruleList == null) {
			final DotConnect db = new DotConnect();
			db.setSQL(sql.SELECT_RULES_BY_FOLDER);
			db.addParam(folder);
			ruleList = convertListToObjects(db.loadObjectResults(), Rule.class);
			cache.addRules(ruleList);
		}
		return ruleList;
	}

	@Override
	public List<Rule> getRulesByNameFilter(String nameFilter) {
		return null;
	}

	@Override
	public Rule getRuleById(String id) throws DotDataException {
		Rule rule = cache.getRule(id);
		if (rule == null) {
			final DotConnect db = new DotConnect();
			db.setSQL(sql.SELECT_RULE_BY_ID);
			db.addParam(id);
			List<Rule> result = convertListToObjects(db.loadObjectResults(),
					Rule.class);
			if (!result.isEmpty()) {
				rule = result.get(0);
				cache.addRule(rule);
			}
		}
		return rule;
	}

	@Override
	public List<RuleAction> getRuleActionsByRule(String ruleId)
			throws DotDataException {
		List<RuleAction> actionList = cache.getActions(ruleId);
		if (actionList == null) {
			final DotConnect db = new DotConnect();
			db.setSQL(sql.SELECT_RULE_ACTIONS_BY_RULE);
			db.addParam(ruleId);
			actionList = convertListToObjects(db.loadObjectResults(),
					RuleAction.class);
			cache.addActions(ruleId, actionList);
		}
		return actionList;
	}

	@Override
	public RuleAction getRuleActionById(String ruleActionId)
			throws DotDataException {
		RuleAction action = cache.getAction(ruleActionId);
		if (action == null) {
			final DotConnect db = new DotConnect();
			db.setSQL(sql.SELECT_RULE_ACTION_BY_ID);
			db.addParam(ruleActionId);
			List<RuleAction> result = convertListToObjects(
					db.loadObjectResults(), Rule.class);
			if (!result.isEmpty()) {
				action = (RuleAction) result.get(0);
				cache.addAction(action.getRuleId(), action);
			}
		}
		return action;
	}

	@Override
	public List<ConditionGroup> getConditionGroupsByRule(String ruleId)
			throws DotDataException {
		List<ConditionGroup> conditionGroups = cache.getConditionGroups(ruleId);
		if (conditionGroups == null) {
			final DotConnect db = new DotConnect();
			db.setSQL(sql.SELECT_CONDITION_GROUPS_BY_RULE);
			db.addParam(ruleId);
			conditionGroups = convertListToObjects(db.loadObjectResults(),
					ConditionGroup.class);
			cache.addConditionGroups(ruleId, conditionGroups);
		}
		return conditionGroups;
	}

	@Override
	public ConditionGroup getConditionGroupById(String conditionGroupId)
			throws DotDataException {
		ConditionGroup conditionGroup = cache
				.getConditionGroup(conditionGroupId);
		if (conditionGroup == null) {
			final DotConnect db = new DotConnect();
			db.setSQL(sql.SELECT_CONDITION_GROUP_BY_ID);
			db.addParam(conditionGroupId);
			List<ConditionGroup> result = convertListToObjects(
					db.loadObjectResults(), ConditionGroup.class);
			if (!result.isEmpty()) {
				conditionGroup = (ConditionGroup) result.get(0);
				cache.addConditionGroup(conditionGroup.getRuleId(),
						conditionGroup);
			}
		}
		return conditionGroup;
	}

	@Override
	public List<Condition> getConditionsByRule(String ruleId)
			throws DotDataException {
		List<Condition> conditions = cache.getConditions(ruleId);
		if (conditions == null) {
			final DotConnect db = new DotConnect();
			db.setSQL(sql.SELECT_CONDITIONS_BY_RULE);
			db.addParam(ruleId);
			conditions = convertListToObjects(db.loadObjectResults(),
					Condition.class);
		}
		return conditions;
	}

	@Override
	public List<Condition> getConditionsByGroup(String groupId)
			throws DotDataException {
		List<Condition> conditions = cache.getConditionsByGroupId(groupId);
		if (conditions == null) {
			final DotConnect db = new DotConnect();
			db.setSQL(sql.SELECT_CONDITIONS_BY_GROUP);
			db.addParam(groupId);
			cache.addConditions(groupId, conditions);
			conditions = convertListToObjects(db.loadObjectResults(),
					Condition.class);
		}
		return conditions;
	}

	@Override
	public Condition getConditionById(String id) throws DotDataException {
		Condition condition = cache.getCondition(id);
		if (condition == null) {
			final DotConnect db = new DotConnect();
			db.setSQL(sql.SELECT_CONDITION_BY_ID);
			db.addParam(id);
			List<Condition> result = convertListToObjects(
					db.loadObjectResults(), Condition.class);
			if (!result.isEmpty()) {
				condition = (Condition) result.get(0);
				cache.addCondition(condition.getConditionGroup(), condition);
			}
		}
		return condition;
	}

	@Override
	public void saveRule(Rule rule) throws DotDataException {
		boolean isNew = true;
		if (UtilMethods.isSet(rule.getId())) {
			try {
				if (getRuleById(rule.getId()) != null) {
					isNew = false;
				}
			} catch (final Exception e) {
				Logger.debug(this.getClass(), e.getMessage(), e);
			}
		} else {
			rule.setId(UUIDGenerator.generateUuid());
		}

		final DotConnect db = new DotConnect();
		if (isNew) {
			db.setSQL(sql.INSERT_RULE);
			db.addParam(rule.getId());
			db.addParam(rule.getName());
			db.addParam(rule.getFireOn().toString());
			db.addParam(rule.isShortCircuit());
			db.addParam(rule.getHost());
			db.addParam(rule.getFolder());
			db.addParam(rule.getPriority());
			db.addParam(rule.isEnabled());
			db.addParam(new Date());
			db.loadResult();
		} else {
			db.setSQL(sql.UPDATE_RULE);
			db.addParam(rule.getName());
			db.addParam(rule.getFireOn().toString());
			db.addParam(rule.isShortCircuit());
			db.addParam(rule.getHost());
			db.addParam(rule.getFolder());
			db.addParam(rule.getPriority());
			db.addParam(rule.isEnabled());
			db.addParam(new Date());
			db.addParam(rule.getId());
			db.loadResult();
		}
		cache.addRule(rule);
	}

	@Override
	public void saveCondition(Condition condition) throws DotDataException {
		boolean isNew = true;
		if (UtilMethods.isSet(condition.getId())) {
			try {
				if (getConditionById(condition.getId()) != null) {
					isNew = false;
				}
			} catch (final Exception e) {
				Logger.debug(this.getClass(), e.getMessage(), e);
			}
		} else {
			condition.setId(UUIDGenerator.generateUuid());
		}
		final DotConnect db = new DotConnect();
		if (isNew) {
			db.setSQL(sql.INSERT_CONDITION);
			db.addParam(condition.getId());
			db.addParam(condition.getName());
			db.addParam(condition.getRuleId());
			db.addParam(condition.getConditionletId());
			db.addParam(condition.getComparison());
			db.addParam(condition.getInput());
			db.loadResult();
		} else {
			db.setSQL(sql.UPDATE_CONDITION);
			db.addParam(condition.getName());
			db.addParam(condition.getRuleId());
			db.addParam(condition.getConditionletId());
			db.addParam(condition.getComparison());
			db.addParam(condition.getInput());
			db.addParam(condition.getId());
			db.loadResult();
		}
		cache.addCondition(condition.getConditionGroup(), condition);
	}

	@Override
	public void saveRuleAction(RuleAction ruleAction) throws DotDataException {
		boolean isNew = true;
		if (UtilMethods.isSet(ruleAction.getId())) {
			try {
				if (getRuleById(ruleAction.getId()) != null) {
					isNew = false;
				}
			} catch (final Exception e) {
				Logger.debug(this.getClass(), e.getMessage(), e);
			}
		} else {
			ruleAction.setId(UUIDGenerator.generateUuid());
		}
		final DotConnect db = new DotConnect();
		if (isNew) {
			db.setSQL(sql.INSERT_RULE_ACTION);
			db.addParam(ruleAction.getId());
			db.addParam(ruleAction.getName());
			db.addParam(ruleAction.getRuleId());
			db.addParam(ruleAction.getPriority());
			db.addParam(ruleAction.getActionlet());
			db.loadResult();
		} else {
			db.setSQL(sql.UPDATE_RULE_ACTION);
			db.addParam(ruleAction.getName());
			db.addParam(ruleAction.getRuleId());
			db.addParam(ruleAction.getPriority());
			db.addParam(ruleAction.getActionlet());
			db.addParam(ruleAction.getId());
			db.loadResult();
		}
		cache.addAction(ruleAction.getRuleId(), ruleAction);
	}

	@Override
	public void deleteConditionGroup(ConditionGroup conditionGroup)
			throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.DELETE_CONDITION_GROUP_BY_ID);
		db.addParam(conditionGroup.getId());
		db.loadResult();
		cache.removeConditionGroup(conditionGroup.getRuleId(), conditionGroup);
		cache.removeConditions(conditionGroup.getId());
	}

	@Override
	public void deleteConditionsByGroup(ConditionGroup conditionGroup)
			throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.DELETE_CONDITION_BY_GROUP);
		db.addParam(conditionGroup.getId());
		db.loadResult();
		cache.removeConditions(conditionGroup.getId());
	}

	@Override
	public void deleteCondition(Condition condition) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.DELETE_CONDITION_BY_ID);
		db.addParam(condition.getId());
		db.loadResult();
		cache.removeCondition(condition.getConditionGroup(), condition);
	}

	@Override
	public void deleteRule(Rule rule) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.DELETE_RULE_BY_ID);
		db.addParam(rule.getId());
		db.loadResult();
		cache.removeRule(rule);
		cache.removeConditionGroups(rule);
		cache.removeConditionsByRuleId(rule.getId());
		cache.removeActions(rule);
	}

	@Override
	public void deleteRuleAction(RuleAction ruleAction) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.DELETE_RULE_ACTION_BY_ID);
		db.addParam(ruleAction.getId());
		db.loadResult();
		cache.removeAction(ruleAction.getRuleId(), ruleAction);
	}

	private List convertListToObjects(List<Map<String, Object>> rs, Class clazz)
			throws DotDataException {
		final ObjectMapper m = new ObjectMapper();

		final List ret = new ArrayList();
		try {
			for (final Map<String, Object> map : rs) {
				ret.add(this.convertMaptoObject(map, clazz));
			}
		} catch (final Exception e) {
			throw new DotDataException("cannot convert object to " + clazz
					+ " " + e.getMessage());

		}
		return ret;
	}

	private Object convertMaptoObject(Map<String, Object> map, Class clazz)
			throws InstantiationException, IllegalAccessException,
			InvocationTargetException {

		if (clazz.getName().equals(Rule.class.getName())) {
			return this.convertRule(map);
		} else if (clazz.getName().equals(RuleAction.class.getName())) {
			return this.convertRuleAction(map);
		} else if (clazz.getName().equals(Condition.class.getName())) {
			return this.convertCondition(map);
		} else if (clazz.getName().equals(ConditionGroup.class.getName())) {
			return this.convertConditionGroup(map);
		}
		{
			return this.convert(clazz.newInstance(), map);
		}
	}

	public static Rule convertRule(Map<String, Object> row) {
		Rule r = new Rule();
		r.setId(row.get("id").toString());
		r.setName(row.get("name").toString());
		r.setFireOn(Rule.FireOn.valueOf(row.get("fire_on").toString()));
		r.setShortCircuit(DbConnectionFactory.isDBTrue(row.get("short_circuit")
				.toString()));
		r.setHost(row.get("host").toString());
		r.setFolder(row.get("folder").toString());
		r.setPriority(Integer.parseInt(row.get("priority").toString()));
		r.setEnabled(DbConnectionFactory
				.isDBTrue(row.get("enabled").toString()));
		return r;
	}

	public static Condition convertCondition(Map<String, Object> row) {
		Condition c = new Condition();
		c.setId(row.get("id").toString());
		c.setName(row.get("name").toString());
		c.setRuleId(row.get("rule_id").toString());
		c.setConditionletId(row.get("conditionlet").toString());
		c.setConditionGroup(row.get("condition_group").toString());
		c.setComparison(row.get("comparison").toString());
		c.setOperator(Condition.Operator
				.valueOf(row.get("operator").toString()));
		c.setInput(row.get("value").toString());
		c.setModDate((Date) row.get("mod_date"));
		return c;
	}

	public static ConditionGroup convertConditionGroup(Map<String, Object> row) {
		ConditionGroup c = new ConditionGroup();
		c.setId(row.get("id").toString());
		c.setRuleId(row.get("rule_id").toString());
		c.setOperator(Condition.Operator
				.valueOf(row.get("operator").toString()));
		c.setModDate((Date) row.get("mod_date"));
		return c;
	}

	public static RuleAction convertRuleAction(Map<String, Object> row) {
		RuleAction r = new RuleAction();
		r.setId(row.get("id").toString());
		r.setName(row.get("name").toString());
		r.setRuleId(row.get("rule_id").toString());
		r.setPriority(Integer.parseInt(row.get("priority").toString()));
		r.setActionlet(row.get("rule_id").toString());
		return r;
	}

	private Object convert(Object obj, Map<String, Object> map)
			throws IllegalAccessException, InvocationTargetException {
		BeanUtils.copyProperties(obj, map);
		return obj;
	}
}
