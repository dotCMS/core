package com.dotmarketing.portlets.rules.business;

import com.dotcms.repackage.org.apache.commons.beanutils.BeanUtils;
import com.dotcms.repackage.org.codehaus.jackson.map.ObjectMapper;
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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class RulesFactoryImpl implements RulesFactory {

    private static RuleSQL sql = null;

    public RulesFactoryImpl() {
        sql = RuleSQL.getInstance();
    }
    @Override
    public List<Rule> getRulesByHost(String host) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.SELECT_RULES_BY_HOST);
        db.addParam(host);
        return this.convertListToObjects(db.loadObjectResults(), Rule.class);
    }

    @Override
    public List<Rule> getRulesByFolder(String folder) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.SELECT_RULES_BY_FOLDER);
        db.addParam(folder);
        return this.convertListToObjects(db.loadObjectResults(), Rule.class);
    }

    @Override
    public List<Rule> getRulesByNameFilter(String nameFilter) {
        return null;
    }

    @Override
    public Rule getRuleById(String id) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.SELECT_RULE_BY_ID);
        db.addParam(id);
        List result = convertListToObjects(db.loadObjectResults(), Rule.class);
        if(!result.isEmpty()) {
            return (Rule) result.get(0);
        }

        return null;
    }

    @Override
    public List<RuleAction> getRuleActionsByRule(String ruleId) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.SELECT_RULE_ACTIONS_BY_RULE);
        db.addParam(ruleId);
        return this.convertListToObjects(db.loadObjectResults(), RuleAction.class);
    }

    @Override
    public RuleAction getRuleActionById(String ruleActionId) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.SELECT_RULE_ACTION_BY_ID);
        db.addParam(ruleActionId);

        List result = convertListToObjects(db.loadObjectResults(), Rule.class);
        if(!result.isEmpty()) {
            return (RuleAction) result.get(0);
        }

        return null;
    }


    public List<ConditionGroup> getConditionGroupsByRule(String ruleId) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.SELECT_CONDITION_GROUPS_BY_RULE);
        db.addParam(ruleId);
        return convertListToObjects(db.loadObjectResults(), ConditionGroup.class);
    }

    @Override
    public ConditionGroup getConditionGroupById(String conditionGroupId) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.SELECT_CONDITION_GROUP_BY_ID);
        db.addParam(conditionGroupId);
        List result = convertListToObjects(db.loadObjectResults(), ConditionGroup.class);
        if(!result.isEmpty()) {
            return (ConditionGroup) result.get(0);
        }

        return null;
    }

    @Override
    public List<Condition> getConditionsByRule(String ruleId) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.SELECT_CONDITIONS_BY_RULE);
        db.addParam(ruleId);
        return convertListToObjects(db.loadObjectResults(), Condition.class);
    }

    @Override
    public Condition getConditionById(String id) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.SELECT_CONDITION_BY_ID);
        db.addParam(id);
        List result = convertListToObjects(db.loadObjectResults(), Condition.class);
        if(!result.isEmpty()) {
            return (Condition) result.get(0);
        }

        return null;
    }

    @Override
    public void saveRule(Rule rule) throws DotDataException{
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
            db.addParam(rule.getFirePolicy().toString());
            db.addParam(rule.isShortCircuit());
            db.addParam(rule.getHost());
            db.addParam(rule.getFolder());
            db.addParam(rule.getFireOrder());
            db.addParam(rule.isEnabled());
            db.addParam(new Date());
            db.loadResult();
        } else {
            db.setSQL(sql.UPDATE_RULE);
            db.addParam(rule.getName());
            db.addParam(rule.getFirePolicy().toString());
            db.addParam(rule.isShortCircuit());
            db.addParam(rule.getHost());
            db.addParam(rule.getFolder());
            db.addParam(rule.getFireOrder());
            db.addParam(rule.isEnabled());
            db.addParam(new Date());
            db.addParam(rule.getId());
            db.loadResult();
        }
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
    }

    @Override
    public void saveRuleAction(RuleAction ruleAction) throws DotDataException{
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
            db.addParam(ruleAction.getFireOrder());
            db.addParam(ruleAction.getActionlet());
            db.loadResult();
        } else {
            db.setSQL(sql.UPDATE_RULE_ACTION);
            db.addParam(ruleAction.getName());
            db.addParam(ruleAction.getRuleId());
            db.addParam(ruleAction.getFireOrder());
            db.addParam(ruleAction.getActionlet());
            db.addParam(ruleAction.getId());
            db.loadResult();
        }
    }

    @Override
    public void deleteCondition(Condition condition) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.DELETE_CONDITION_BY_ID);
        db.addParam(condition.getId());
        db.loadResult();
    }

    @Override
    public void deleteRule(Rule rule) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.DELETE_RULE_BY_ID);
        db.addParam(rule.getId());
        db.loadResult();
    }

    @Override
    public void deleteRuleAction(RuleAction ruleAction) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(sql.DELETE_RULE_ACTION_BY_ID);
        db.addParam(ruleAction.getId());
        db.loadResult();
    }

    private List convertListToObjects(List<Map<String, Object>> rs, Class clazz) throws DotDataException {
        final ObjectMapper m = new ObjectMapper();

        final List ret = new ArrayList();
        try {
            for (final Map<String, Object> map : rs) {
                ret.add(this.convertMaptoObject(map, clazz));
            }
        } catch (final Exception e) {
            throw new DotDataException("cannot convert object to " + clazz + " " + e.getMessage());

        }
        return ret;
    }

    private Object convertMaptoObject(Map<String, Object> map, Class clazz) throws InstantiationException, IllegalAccessException, InvocationTargetException {

        if (clazz.getName().equals(Rule.class.getName())) {
            return this.convertRule(map);
        }  else if (clazz.getName().equals(RuleAction.class.getName())) {
            return this.convertRuleAction(map);
        } else if (clazz.getName().equals(Condition.class.getName())) {
            return this.convertCondition(map);
        }else if (clazz.getName().equals(ConditionGroup.class.getName())) {
            return this.convertConditionGroup(map);
        }{
            return this.convert(clazz.newInstance(), map);
        }
    }

    public static Rule convertRule(Map<String, Object> row){
        Rule r = new Rule();
        r.setId(row.get("id").toString());
        r.setName(row.get("name").toString());
        r.setFirePolicy(Rule.FirePolicy.valueOf(row.get("fire_policy").toString()));
        r.setShortCircuit(DbConnectionFactory.isDBTrue(row.get("short_circuit").toString()));
        r.setHost(row.get("host").toString());
        r.setFolder(row.get("folder").toString());
        r.setFireOrder(Integer.parseInt(row.get("fire_order").toString()));
        r.setEnabled(DbConnectionFactory.isDBTrue(row.get("enabled").toString()));
        return r;
    }

    public static Condition convertCondition(Map<String, Object> row){
        Condition c = new Condition();
        c.setId(row.get("id").toString());
        c.setName(row.get("name").toString());
        c.setRuleId(row.get("rule_id").toString());
        c.setConditionletId(row.get("conditionlet").toString());
        c.setConditionGroup(row.get("condition_group").toString());
        c.setComparison(row.get("comparison").toString());
        c.setOperator(Condition.Operator.valueOf(row.get("operator").toString()));
        c.setInput(row.get("value").toString());
        c.setModDate((Date) row.get("mod_date"));
        return c;
    }

    public static ConditionGroup convertConditionGroup(Map<String, Object> row){
        ConditionGroup c = new ConditionGroup();
        c.setId(row.get("id").toString());
        c.setRuleId(row.get("rule_id").toString());
        c.setOperator(Condition.Operator.valueOf(row.get("operator").toString()));
        c.setModDate((Date) row.get("mod_date"));
        return c;
    }

    public static RuleAction convertRuleAction(Map<String, Object> row){
        RuleAction r = new RuleAction();
        r.setId(row.get("id").toString());
        r.setName(row.get("name").toString());
        r.setRuleId(row.get("rule_id").toString());
        r.setFireOrder(Integer.parseInt(row.get("fire_order").toString()));
        r.setActionlet(row.get("rule_id").toString());
        return r;
    }

    private Object convert(Object obj, Map<String, Object> map) throws IllegalAccessException, InvocationTargetException {
        BeanUtils.copyProperties(obj, map);
        return obj;
    }
}
