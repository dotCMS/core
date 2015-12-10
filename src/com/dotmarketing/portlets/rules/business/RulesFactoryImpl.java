package com.dotmarketing.portlets.rules.business;

import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotcms.repackage.org.apache.commons.beanutils.BeanUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dotcms.rest.validation.Preconditions.checkNotNull;

public class RulesFactoryImpl implements RulesFactory {


    private static RuleSQL sql = null;
    private static RulesCache cache = null;

    public RulesFactoryImpl() {
        sql = RuleSQL.getInstance();
        cache = CacheLocator.getRulesCache();
    }

    @Override
    public List<Rule> getEnabledRulesByHost(Host host) throws DotDataException {
        List<Rule> ruleList = getAllRulesByHost(host);
        return ruleList.stream().filter(rule -> rule.isEnabled()).collect(Collectors.toList());
    }

    @Override
    public List<Rule> getAllRulesByHost(Host host) throws DotDataException {
        host = checkNotNull(host, "Host is required.");

        if(Strings.isNullOrEmpty(host.getIdentifier())) {
            throw new IllegalArgumentException("Host must have an id.");
        }

        List<String> rulesIds = cache.getRulesIdsByHost(host);
        List<Rule> rules;
        if (rulesIds == null) {
            final DotConnect db = new DotConnect();
            db.setSQL(sql.SELECT_ALL_RULES_BY_HOST);
            db.addParam(host.getIdentifier());
            rules = convertListToObjects(db.loadObjectResults(), Rule.class);
            cache.putRulesByHost(host,rules);
        } else {
            rules = new ArrayList<>();
            for(String ruleId: rulesIds) {
                Rule rule = getRuleById(ruleId);
                if(rule!=null) {
                    rules.add(rule);
                }
            }
        }
        return rules;
    }

    @Override
    public Set<Rule> getRulesByHost(String host, Rule.FireOn fireOn) throws DotDataException {
        Set<Rule> ruleList = cache.getRulesByHostFireOn(host, fireOn);
        if (ruleList == null) {
            final DotConnect db = new DotConnect();
            db.setSQL(sql.SELECT_RULES_BY_HOST_FIRE_ON);
            db.addParam(host);
            db.addParam(fireOn.toString());
            ruleList = convertListToObjectsSet(db.loadObjectResults(), Rule.class);
            cache.addRulesByHostFireOn(ruleList, host, fireOn);
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
    public List<RuleAction> getRuleActionsByRule(String ruleId) throws DotDataException {
        if(Strings.isNullOrEmpty(ruleId)) {
            throw new IllegalArgumentException("Invalid ruleId.");
        }

        Rule rule = getRuleById(ruleId);
        List<String> actionsIds = cache.getActionsIdsByRule(rule);
        List<RuleAction> actions;

        if(actionsIds == null) {
            final DotConnect db = new DotConnect();
            db.setSQL(sql.SELECT_RULE_ACTIONS_BY_RULE);
            db.addParam(ruleId);
            actions = convertListToObjects(db.loadObjectResults(), RuleAction.class);
            getRuleActionsParametersFromDB(actions, db);

            cache.putActionsByRule(rule, actions);
        } else {
            actions = new ArrayList<>();
            for(String actionId: actionsIds) {
                RuleAction action = getRuleActionById(actionId);
                if(action!=null) {
                    actions.add(action);
                }
            }
        }
        return actions;

    }

    @Override
    public RuleAction getRuleActionById(String ruleActionId) throws DotDataException {
        RuleAction action = cache.getAction(ruleActionId);
        if (action == null) {
            final DotConnect db = new DotConnect();
            db.setSQL(sql.SELECT_RULE_ACTION_BY_ID);
            db.addParam(ruleActionId);
            List<RuleAction> result = convertListToObjects(
                db.loadObjectResults(), RuleAction.class);
            if (!result.isEmpty()) {
                action = result.get(0);
                getRuleActionParametersFromDB(action, db);
                cache.addAction(action);
            }
        }
        return action;
    }

    private void getRuleActionParametersFromDB(RuleAction action, DotConnect db) throws DotDataException {
        db.setSQL(sql.SELECT_RULE_ACTIONS_PARAMS);
        db.addParam(action.getId());
        action.setParameters(convertListToObjects(db.loadObjectResults(), RuleActionParameter.class));
    }

    private void getRuleActionsParametersFromDB(List<RuleAction> actions, DotConnect db) throws DotDataException {
        for (RuleAction action : actions) {
            getRuleActionParametersFromDB(action, db);
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
    public List<ConditionGroup> getConditionGroupsByRule(String ruleId) throws DotDataException {
        if(Strings.isNullOrEmpty(ruleId)) {
            throw new IllegalArgumentException("Invalid ruleId.");
        }

        Rule rule = getRuleById(ruleId);
        List<String> groupsIds = cache.getConditionGroupsIdsByRule(rule);
        List<ConditionGroup> groups;

        if(groupsIds == null) {
            final DotConnect db = new DotConnect();
            db.setSQL(sql.SELECT_CONDITION_GROUPS_BY_RULE);
            db.addParam(ruleId);
            groups = convertListToObjects(db.loadObjectResults(), ConditionGroup.class);

            cache.putConditionGroupsByRule(rule, groups);
        } else {
            groups = new ArrayList<>();
            for(String groupId: groupsIds) {
                ConditionGroup group = getConditionGroupById(groupId);
                if(group!=null) {
                    groups.add(group);
                }
            }
        }
        return groups;
    }

    @Override
    public ConditionGroup getConditionGroupById(String conditionGroupId) throws DotDataException {
        ConditionGroup conditionGroup = cache.getConditionGroup(conditionGroupId);

        if (conditionGroup == null) {
            final DotConnect db = new DotConnect();
            db.setSQL(sql.SELECT_CONDITION_GROUP_BY_ID);
            db.addParam(conditionGroupId);
            List<ConditionGroup> result = convertListToObjects(
                db.loadObjectResults(), ConditionGroup.class);
            if (!result.isEmpty()) {
                conditionGroup = result.get(0);
                cache.addConditionGroup(conditionGroup);
            }
        }
        return conditionGroup;
    }

    @Override
    public List<Condition> getConditionsByGroup(String groupId)
        throws DotDataException {
        if(Strings.isNullOrEmpty(groupId)) {
            throw new IllegalArgumentException("Invalid groupId.");
        }

        ConditionGroup group = getConditionGroupById(groupId);
        List<String> conditionsIds = cache.getConditionsIdsByGroup(group);
        List<Condition> conditions;

        if(conditionsIds == null) {
            final DotConnect db = new DotConnect();
            db.setSQL(sql.SELECT_CONDITIONS_BY_GROUP);
            db.addParam(groupId);
            conditions = convertListToObjects(db.loadObjectResults(), Condition.class);

            getConditionsValuesFromDB(conditions, db);

            cache.putConditionsByGroup(group, conditions);
        } else {
            conditions = new ArrayList<>();
            for(String conditionId: conditionsIds) {
                Condition condition = getConditionById(conditionId);
                if(condition!=null) {
                    conditions.add(condition);
                }
            }
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
                getConditionValuesFromDB(condition, db);

                cache.addCondition(condition);
            }
        }
        return condition;
    }

    private void getConditionValuesFromDB(Condition condition, DotConnect db) throws DotDataException {
        db.setSQL(sql.SELECT_CONDITION_VALUES_BY_CONDITION);
        db.addParam(condition.getId());
        condition.setValues(convertListToObjects(db.loadObjectResults(), ConditionValue.class));
    }

    private void getConditionsValuesFromDB(List<Condition> conditions, DotConnect db) throws DotDataException {
        for(Condition condition: conditions) {
            getConditionValuesFromDB(condition, db);
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



            // remove groups who were not provided

            List<ConditionGroup> dbGroups = getConditionGroupsByRule(rule.getId());
            List<String> updatedGroups = rule.getGroups().stream()
                .map(ConditionGroup::getId)
                .collect(Collectors.toList());
            dbGroups.stream()
                .filter(group -> updatedGroups.contains(group.getId()))
                .map(this::deleteRemovedGroupFromRule);
        }

        cache.removeRule(rule);

    }

    private Boolean deleteRemovedGroupFromRule(ConditionGroup group) {
        try {
            deleteConditionGroup(group);
            return true;
        } catch (DotDataException e) {
            Logger.debug(this.getClass(), e.getMessage(), e);
        }
        return false;
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

        cache.removeConditionGroup(group);

        Rule rule  = getRuleById(group.getRuleId());
        cache.removeRule(rule);
        
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

        final DotConnect db = new DotConnect();

        try {
            if (isNew) {

                db.setSQL(sql.INSERT_CONDITION);
                db.addParam(condition.getId());
                db.addParam(condition.getName());
                db.addParam(condition.getConditionletId());
                db.addParam(condition.getConditionGroup());
                db.addParam(condition.getComparison());
                db.addParam(condition.getOperator().toString());
                db.addParam(condition.getPriority());
                db.addParam(condition.getModDate());
                db.loadResult();

                if (condition.getValues() != null) {
                    for (ConditionValue value : condition.getValues()) {
                        value.setId(UUIDGenerator.generateUuid());
                        db.setSQL(sql.INSERT_CONDITION_VALUE);
                        db.addParam(value.getId());
                        db.addParam(condition.getId());
                        db.addParam(value.getKey());
                        db.addParam(value.getValue());
                        db.addParam(value.getPriority());
                        db.loadResult();
                    }
                }

            } else {
                db.setSQL(sql.UPDATE_CONDITION);
                db.addParam(condition.getName());
                db.addParam(condition.getConditionletId());
                db.addParam(condition.getConditionGroup());
                db.addParam(condition.getComparison());
                db.addParam(condition.getOperator().toString());
                db.addParam(condition.getPriority());
                db.addParam(condition.getModDate());
                db.addParam(condition.getId());
                db.loadResult();

                deleteConditionValues(condition);

                if (condition.getValues() != null) {
                    for (ConditionValue value : condition.getValues()) {
                        value.setId(UUIDGenerator.generateUuid());
                        db.setSQL(sql.INSERT_CONDITION_VALUE);
                        db.addParam(value.getId());
                        db.addParam(condition.getId());
                        db.addParam(value.getKey());
                        db.addParam(value.getValue());
                        db.addParam(value.getPriority());
                        db.loadResult();
                    }
                }

            }

            cache.removeCondition(condition);
            ConditionGroup group  = getConditionGroupById(condition.getConditionGroup());
            cache.removeConditionGroup(group);
            Rule rule  = getRuleById(group.getRuleId());
            cache.removeRule(rule);
            

        } catch (DotDataException e) {
            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e1) {
                Logger.warn(this, e1.getMessage(), e1);
            }

            throw e;
        }

    }


    @Override
    public void saveConditionValue(ConditionValue conditionValue) throws DotDataException {

        boolean isNew = true;
        if (UtilMethods.isSet(conditionValue.getId())) {
            try {
                if (getConditionValueById(conditionValue.getId()) != null) {
                    isNew = false;
                }
            } catch (final Exception e) {
                Logger.debug(this.getClass(), e.getMessage(), e);
            }
        } else {
            conditionValue.setId(UUIDGenerator.generateUuid());
        }

        final DotConnect db = new DotConnect();

        try {
            if (isNew) {

                db.setSQL(sql.INSERT_CONDITION_VALUE);
                db.addParam(conditionValue.getId());
                db.addParam(conditionValue.getConditionId());
                db.addParam(conditionValue.getKey());
                db.addParam(conditionValue.getValue());
                db.addParam(conditionValue.getPriority());
                db.loadResult();

            } else {
                db.setSQL(sql.UPDATE_CONDITION_VALUE);
                db.addParam(conditionValue.getConditionId());
                db.addParam(conditionValue.getKey());
                db.addParam(conditionValue.getValue());
                db.addParam(conditionValue.getPriority());
                db.addParam(conditionValue.getId());
                db.loadResult();
            }
            Condition condition = getConditionById(conditionValue.getConditionId());
            cache.removeCondition(condition);
            ConditionGroup group  = getConditionGroupById(condition.getConditionGroup());
            cache.removeConditionGroup(group);
            Rule rule  = getRuleById(group.getRuleId());
            cache.removeRule(rule);
        } catch (DotDataException e) {
            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e1) {
                Logger.warn(this, e1.getMessage(), e1);
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


            if (ruleAction.getParameters() != null) {
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

        } else {
            db.setSQL(sql.UPDATE_RULE_ACTION);
            db.addParam(ruleAction.getName());
            db.addParam(ruleAction.getRuleId());
            db.addParam(ruleAction.getPriority());
            db.addParam(ruleAction.getActionlet());
            db.addParam(ruleAction.getId());
            db.loadResult();

            deleteRuleActionsParameters(ruleAction);

            if (ruleAction.getParameters() != null) {
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
        }

        cache.removeAction(ruleAction);

        Rule rule  = getRuleById(ruleAction.getRuleId());
        cache.removeRule(rule);

    }

    @Override
    public void deleteConditionGroup(ConditionGroup conditionGroup)
    
        throws DotDataException {
    	Rule rule  = getRuleById(conditionGroup.getRuleId());
        final DotConnect db = new DotConnect();
        db.setSQL(sql.DELETE_CONDITION_GROUP_BY_ID);
        db.addParam(conditionGroup.getId());
        db.loadResult();
        cache.removeConditionGroup(conditionGroup);

        cache.removeRule(rule);
    }

    @Override
    public void deleteConditionsByGroup(ConditionGroup conditionGroup)
        throws DotDataException {

        conditionGroup = checkNotNull(conditionGroup, "Condition Group is required.");

        if(Strings.isNullOrEmpty(conditionGroup.getId())) {
            throw new IllegalArgumentException("Condition Group must have an id.");
        }

        List<Condition> conditionsByGroup = getConditionsByGroup(conditionGroup.getId());

        final DotConnect db = new DotConnect();
        db.setSQL(sql.DELETE_CONDITION_BY_GROUP);
        db.addParam(conditionGroup.getId());
        db.loadResult();

        for(Condition condition: conditionsByGroup) {
            cache.removeCondition(condition);
        }
        cache.removeConditionGroup(conditionGroup);
    	Rule rule  = getRuleById(conditionGroup.getRuleId());
        cache.removeRule(rule);
    }

    @Override
    public void deleteCondition(Condition condition) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.DELETE_CONDITION_BY_ID);
        db.addParam(condition.getId());
        db.loadResult();
        cache.removeCondition(condition);
        ConditionGroup group  = getConditionGroupById(condition.getConditionGroup());
        cache.removeConditionGroup(group);
        Rule rule  = getRuleById(group.getRuleId());
        cache.removeRule(rule);
    }

    @Override
    public void deleteConditionValue(ConditionValue conditionValue) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.DELETE_CONDITION_VALUE_BY_ID);
        db.addParam(conditionValue.getId());
        db.loadResult();
        Condition condition = getConditionById(conditionValue.getConditionId());
        cache.removeCondition(condition);
        ConditionGroup group  = getConditionGroupById(condition.getConditionGroup());
        cache.removeConditionGroup(group);
        Rule rule  = getRuleById(group.getRuleId());
        cache.removeRule(rule);
    }

    @Override
    public void deleteRule(Rule rule) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.DELETE_RULE_BY_ID);
        db.addParam(rule.getId());
        db.loadResult();
        cache.removeRule(rule);
    }

    @Override
    public void deleteRuleAction(RuleAction ruleAction) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.DELETE_RULE_ACTION_BY_ID);
        db.addParam(ruleAction.getId());
        db.loadResult();
        cache.removeAction(ruleAction);
        Rule rule  = getRuleById(ruleAction.getRuleId());
        
        
        cache.removeRule(rule);
    }

    @Override
    public void deleteRuleActionsByRule(Rule rule) throws DotDataException {
        rule = checkNotNull(rule, "Rule is required.");

        if(Strings.isNullOrEmpty(rule.getId())) {
            throw new IllegalArgumentException("Rule must have an id.");
        }

        List<RuleAction> actionsByRule = getRuleActionsByRule(rule.getId());

        final DotConnect db = new DotConnect();
        db.setSQL(sql.DELETE_RULE_ACTION_BY_RULE);
        db.addParam(rule.getId());
        db.loadResult();

        for(RuleAction action: actionsByRule) {
            cache.removeAction(action);
            
        }     
        cache.removeRule(rule);

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
        
        cache.removeAction(action);
        Rule rule  = getRuleById(action.getRuleId());
        cache.removeRule(rule);
    }

    @Override
    public void deleteConditionValues(Condition condition) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.DELETE_CONDITION_VALUES_BY_CONDITION);
        db.addParam(condition.getId());
        db.loadResult();
        cache.removeCondition(condition);
        ConditionGroup group  = getConditionGroupById(condition.getConditionGroup());
        cache.removeConditionGroup(group);
        Rule rule  = getRuleById(group.getRuleId());
        cache.removeRule(rule);
    }

    private List convertListToObjects(List<Map<String, Object>> rs, Class clazz)
        throws DotDataException {
        final List ret = new ArrayList();
        try {
            for (final Map<String, Object> map : rs) {
                ret.add(this.convertMaptoObject(map, clazz));
            }
        } catch (final Exception e) {
            throw new DotDataException("cannot convert object to " + clazz + " " + e.getMessage(), e);

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
            throw new DotDataException("cannot convert object to " + clazz + " " + e.getMessage(), e);

        }
        return ret;
    }

    private Object convertMaptoObject(Map<String, Object> map, Class clazz)
        throws InstantiationException, IllegalAccessException,
        InvocationTargetException {

        if (clazz.getName().equals(Rule.class.getName())) {
            return convertRule(map);
        } else if (clazz.getName().equals(RuleAction.class.getName())) {
            return convertRuleAction(map);
        } else if (clazz.getName().equals(Condition.class.getName())) {
            return convertCondition(map);
        } else if (clazz.getName().equals(ConditionGroup.class.getName())) {
            return convertConditionGroup(map);
        } else if (clazz.getName().equals(RuleActionParameter.class.getName())) {
            return convertRuleActionParam(map);
        } else if (clazz.getName().equals(ConditionValue.class.getName())) {
            return convertConditionValue(map);
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
        c.setPriority(Integer.parseInt(row.get("priority").toString()));
        return c;
    }

    public static ConditionValue convertConditionValue(Map<String, Object> row) {
        ConditionValue c = new ConditionValue();
        c.setId(row.get("id").toString());
        c.setConditionId(row.get("condition_id").toString());
        c.setKey(row.get("paramkey").toString());
        Object value = row.get("value");
        c.setValue(value != null ? value.toString() : null);
        c.setPriority(Integer.parseInt(row.get("priority").toString()));
        return c;
    }

    public static RuleAction convertRuleAction(Map<String, Object> row) {
        RuleAction r = new RuleAction();
        r.setId(row.get("id").toString());
        r.setName(row.get("name").toString());
        r.setRuleId(row.get("rule_id").toString());
        r.setPriority(Integer.parseInt(row.get("priority").toString()));
        r.setActionlet(row.get("actionlet").toString());
        return r;
    }

    public static RuleActionParameter convertRuleActionParam(Map<String, Object> row) {
        RuleActionParameter r = new RuleActionParameter();
        r.setId(row.get("id").toString());
        r.setRuleActionId(row.get("rule_action_id").toString());
        r.setKey(row.get("paramkey").toString());
        Object value = row.get("value");
        r.setValue(value == null ? null : String.valueOf(value));
        return r;
    }

    private Object convert(Object obj, Map<String, Object> map) throws IllegalAccessException, InvocationTargetException {
        BeanUtils.copyProperties(obj, map);
        return obj;
    }
}
