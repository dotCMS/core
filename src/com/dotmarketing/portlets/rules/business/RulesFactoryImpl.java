package com.dotmarketing.portlets.rules.business;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.repackage.org.apache.commons.beanutils.BeanUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.model.*;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

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
    public Set<Rule> getRulesByHost(String host, Rule.FireOn fireOn) throws DotDataException {
        Set<Rule> ruleList = cache.getRules(host, fireOn);
        if (ruleList == null) {
            final DotConnect db = new DotConnect();
            db.setSQL(sql.SELECT_RULES_BY_HOST_FIRE_ON);
            db.addParam(host);
            db.addParam(fireOn.toString());
            ruleList = convertListToObjectsSet(db.loadObjectResults(), Rule.class);
            cache.addRules(ruleList, host, fireOn);
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

            getRuleActionParametersFromDB(actionList, db);

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
					db.loadObjectResults(), RuleAction.class);
			if (!result.isEmpty()) {
				action = result.get(0);
                List<RuleAction> actions = new ArrayList();
                actions.add(action);
                getRuleActionParametersFromDB(actions, db);

                cache.addAction(action.getRuleId(), action);
			}
		}
		return action;
	}

    private void getRuleActionParametersFromDB(List<RuleAction> actions, DotConnect db) throws DotDataException {
        for (RuleAction action : actions) {
            db.setSQL(sql.SELECT_RULE_ACTIONS_PARAMS);
            db.addParam(action.getId());
            action.setParameters(convertListToObjects(db.loadObjectResults(), RuleActionParameter.class));
        }
    }

    @Override
    public RuleActionParameter getRuleActionParameterById(String id) throws DotDataException {
        RuleActionParameter param = null;

        final DotConnect db = new DotConnect();
        db.setSQL(sql.SELECT_RULE_ACTION_PARAMS);
        db.addParam(id);
        List<RuleActionParameter> result = convertListToObjects(db.loadObjectResults(),
                RuleActionParameter.class);
        if (!result.isEmpty()) {
            param = result.get(0);
        }

        return param;
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

            getConditionValuesFromDB(conditions, db);
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
			conditions = convertListToObjects(db.loadObjectResults(),
					Condition.class);

            getConditionValuesFromDB(conditions, db);

            cache.addConditions(groupId, conditions);
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
				condition = result.get(0);
                List conditions = new ArrayList<Condition>();
                conditions.add(condition);
                getConditionValuesFromDB(conditions, db);

				cache.addCondition(condition.getConditionGroup(), condition);
			}
		}
		return condition;
	}

    private void getConditionValuesFromDB(List<Condition> conditions, DotConnect db) throws DotDataException {
        for (Condition condition : conditions) {
            db.setSQL(sql.SELECT_CONDITION_VALUES_BY_CONDITION);
            db.addParam(condition.getId());
            condition.setValues(convertListToObjects(db.loadObjectResults(), ConditionValue.class));
        }
    }

    @Override
    public ConditionValue getConditionValueById(String id) throws DotDataException {

        ConditionValue value = null;
        final DotConnect db = new DotConnect();
        db.setSQL(sql.SELECT_CONDITION_VALUE_BY_ID);
        db.addParam(id);
        List<ConditionValue> result = convertListToObjects(
                db.loadObjectResults(), ConditionValue.class);
        if (!result.isEmpty()) {
            value = result.get(0);
        }

        return value;
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

            cache.removeRule(rule);


		}

        Set<Rule> rules = cache.getRules(rule.getHost(), rule.getFireOn());
        if(rules==null) {
            rules = new HashSet<>();
        }

        rules.add(rule);

        cache.addRules(rules, rule.getHost(), rule.getFireOn());

	}

    @Override
    public void saveConditionGroup(ConditionGroup group) throws DotDataException {

        group.setModDate(new Date());

        boolean isNew = true;
        if (UtilMethods.isSet(group.getId())) {
            try {
                if (getConditionGroupById(group.getId()) != null) {
                    isNew = false;
                }
            } catch (final Exception e) {
                Logger.debug(this.getClass(), e.getMessage(), e);
            }
        } else {
            group.setId(UUIDGenerator.generateUuid());
        }

        final DotConnect db = new DotConnect();
        if (isNew) {

            db.setSQL(sql.INSERT_CONDITION_GROUP);
            db.addParam(group.getId());
            db.addParam(group.getRuleId());
            db.addParam(group.getOperator().toString());
            db.addParam(group.getPriority());
            db.addParam(group.getModDate());
            db.loadResult();
        } else {
            db.setSQL(sql.UPDATE_CONDITION_GROUP);
            db.addParam(group.getRuleId());
            db.addParam(group.getOperator().toString());
            db.addParam(group.getPriority());
            db.addParam(group.getModDate());
            db.addParam(group.getId());
            db.loadResult();
        }
        cache.addConditionGroup(group.getRuleId(), group);
    }

    @Override
	public void saveCondition(Condition condition) throws DotDataException {
        condition.setModDate(new Date());

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

        HibernateUtil.startTransaction();
        final DotConnect db = new DotConnect();

        try {
            if (isNew) {

                db.setSQL(sql.INSERT_CONDITION);
                db.addParam(condition.getId());
                db.addParam(condition.getName());
                db.addParam(condition.getRuleId());
                db.addParam(condition.getConditionletId());
                db.addParam(condition.getConditionGroup());
                db.addParam(condition.getComparison());
                db.addParam(condition.getOperator().toString());
                db.addParam(condition.getPriority());
                db.addParam(condition.getModDate());
                db.loadResult();

                if(condition.getValues()!=null) {
                    for (ConditionValue value : condition.getValues()) {
                        value.setId(UUIDGenerator.generateUuid());
                        db.setSQL(sql.INSERT_CONDITION_VALUE);
                        db.addParam(value.getId());
                        db.addParam(condition.getId());
                        db.addParam(value.getValue());
                        db.addParam(value.getPriority());
                        db.loadResult();
                    }
                }

            } else {
                db.setSQL(sql.UPDATE_CONDITION);
                db.addParam(condition.getName());
                db.addParam(condition.getRuleId());
                db.addParam(condition.getConditionletId());
                db.addParam(condition.getConditionGroup());
                db.addParam(condition.getComparison());
                db.addParam(condition.getOperator().toString());
                db.addParam(condition.getPriority());
                db.addParam(condition.getModDate());
                db.addParam(condition.getId());
                db.loadResult();

                if(condition.getValues()!=null) {
                    for (ConditionValue value : condition.getValues()) {
                        db.setSQL(sql.UPDATE_CONDITION_VALUE);
                        db.addParam(condition.getId());
                        db.addParam(value.getValue());
                        db.addParam(value.getPriority());
                        db.addParam(value.getId());
                        db.loadResult();
                    }
                }

                cache.removeCondition(condition.getConditionGroup(), condition);
            }

            HibernateUtil.commitTransaction();

        } catch(DotDataException e) {
            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e1) {
                Logger.warn(this, e1.getMessage(),e1);
            }

            throw e;
        }


	}

	@Override
	public void saveRuleAction(RuleAction ruleAction) throws DotDataException {
		boolean isNew = true;
		if (UtilMethods.isSet(ruleAction.getId())) {
			try {
				if (getRuleActionById(ruleAction.getId()) != null) {
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


            if(ruleAction.getParameters()!=null) {
                for (RuleActionParameter parameter : ruleAction.getParameters()) {
                    parameter.setId(UUIDGenerator.generateUuid());
                    db.setSQL(sql.INSERT_RULE_ACTION_PARAM);
                    db.addParam(parameter.getId());
                    db.addParam(ruleAction.getId());
                    db.addParam(parameter.getKey());
                    db.addParam(parameter.getValue());
                    db.loadResult();
                }
            }

            cache.addAction(ruleAction.getRuleId(), ruleAction);

		} else {
			db.setSQL(sql.UPDATE_RULE_ACTION);
			db.addParam(ruleAction.getName());
			db.addParam(ruleAction.getRuleId());
			db.addParam(ruleAction.getPriority());
			db.addParam(ruleAction.getActionlet());
			db.addParam(ruleAction.getId());
			db.loadResult();

            if(ruleAction.getParameters()!=null) {
                for (RuleActionParameter parameter : ruleAction.getParameters()) {
                    parameter.setId(UUIDGenerator.generateUuid());
                    db.setSQL(sql.UPDATE_RULE_ACTION_PARAM);
                    db.addParam(parameter.getRuleActionId());
                    db.addParam(parameter.getKey());
                    db.addParam(parameter.getValue());
                    db.addParam(parameter.getId());
                    db.loadResult();
                }
            }

            cache.removeAction(ruleAction.getRuleId(), ruleAction);
            cache.addAction(ruleAction.getRuleId(), ruleAction);
		}
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

    @Override
    public void deleteRuleActionsByRule(Rule rule) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.DELETE_RULE_ACTION_BY_RULE);
        db.addParam(rule.getId());
        db.loadResult();
        cache.removeActions(rule);
    }



    @Override
    public Map<String, RuleActionParameter> getRuleActionParameters(RuleAction action) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.SELECT_RULE_ACTIONS_PARAMS);
        db.addParam(action.getId());
        List<RuleActionParameter> params = convertListToObjects(db.loadObjectResults(),
                RuleActionParameter.class);

        final Map<String, RuleActionParameter> map = new LinkedHashMap<>();
        for (final RuleActionParameter param : params) {
            map.put(param.getKey(), param);
        }

        return map;
    }

    @Override
    public void deleteRuleActionsParameters(RuleAction action) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.DELETE_RULE_ACTION_PARAM_BY_ACTION);
        db.addParam(action.getId());
        db.loadResult();
    }

    @Override
    public void deleteConditionValues(Condition condition) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.DELETE_CONDITION_VALUES_BY_CONDITION);
        db.addParam(condition.getId());
        db.loadResult();
    }

    private List convertListToObjects(List<Map<String, Object>> rs, Class clazz)
			throws DotDataException {
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

    private Set convertListToObjectsSet(List<Map<String, Object>> rs, Class clazz)
            throws DotDataException {
        final Set ret = new HashSet();
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
		} else if (clazz.getName().equals(RuleActionParameter.class.getName())) {
            return this.convertRuleActionParam(map);
        } else if (clazz.getName().equals(ConditionValue.class.getName())) {
            return this.convertConditionValue(map);
        } else {
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
        c.setOperator(Condition.Operator.valueOf(row.get("operator").toString()));
        c.setPriority(Integer.parseInt(row.get("priority").toString()));
        c.setModDate((Date) row.get("mod_date"));
        return c;
	}

	public static ConditionGroup convertConditionGroup(Map<String, Object> row) {
        ConditionGroup c = new ConditionGroup();
        c.setId(row.get("id").toString());
        c.setRuleId(row.get("rule_id").toString());
        c.setOperator(Condition.Operator.valueOf(row.get("operator").toString()));
        c.setModDate((Date) row.get("mod_date"));
        return c;
	}

    public static ConditionValue convertConditionValue(Map<String, Object> row){
        ConditionValue c = new ConditionValue();
        c.setId(row.get("id").toString());
        c.setConditionId(row.get("condition_id").toString());
        c.setValue(row.get("value").toString());
        c.setPriority(Integer.parseInt(row.get("priority").toString()));
        return c;
    }

    public static RuleAction convertRuleAction(Map<String, Object> row){
        RuleAction r = new RuleAction();
        r.setId(row.get("id").toString());
        r.setName(row.get("name").toString());
        r.setRuleId(row.get("rule_id").toString());
        r.setPriority(Integer.parseInt(row.get("priority").toString()));
        r.setActionlet(row.get("rule_id").toString());
        return r;
    }

    public static RuleActionParameter convertRuleActionParam(Map<String, Object> row){
        RuleActionParameter r = new RuleActionParameter();
        r.setId(row.get("id").toString());
        r.setRuleActionId(row.get("rule_action_id").toString());
        r.setKey(row.get("key").toString());
        r.setValue(row.get("value").toString());
        return r;
    }

    private Object convert(Object obj, Map<String, Object> map) throws IllegalAccessException, InvocationTargetException {
        BeanUtils.copyProperties(obj, map);
        return obj;
    }
}
