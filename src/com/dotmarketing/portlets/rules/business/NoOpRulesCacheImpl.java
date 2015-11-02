//package com.dotmarketing.portlets.rules.business;
//
//import com.dotmarketing.beans.Host;
//import com.dotmarketing.portlets.rules.model.Condition;
//import com.dotmarketing.portlets.rules.model.ConditionGroup;
//import com.dotmarketing.portlets.rules.model.Rule;
//import com.dotmarketing.portlets.rules.model.RuleAction;
//import java.util.List;
//import java.util.Set;
//
///**
// * @author Geoff M. Granum
// */
//public class NoOpRulesCacheImpl extends RulesCache {
//
//    @Override
//    public void clearCache() {
//    }
//
//    @Override
//    public List<Rule> addRules(List<Rule> rules) {
//        return rules;
//    }
//
//    @Override
//    public Set<Rule> addRulesByHostFireOn(Set<Rule> rules, String hostId, Rule.FireOn fireOn) {
//        return rules;
//    }
//
//    @Override
//    public Set<Rule> getRulesByHostFireOn(String hostId, Rule.FireOn fireOn) {
//        return null;
//    }
//
//    @Override
//    public Rule getRule(String ruleId) {
//        return null;
//    }
//
//    @Override
//    public List<String> getRulesIdsByHost(Host hostId) {
//        return null;
//    }
//
//    @Override
//    public void putRulesByHost(Host host, List<Rule> rules) {
//
//    }
//
//    @Override
//    public void removeRule(Rule rule) {
//    }
//
//    @Override
//    public Condition getCondition(String conditionGroupId, Condition condition) {
//        return null;
//    }
//
//    @Override
//    public Condition getCondition(String conditionId) {
//        return null;
//    }
//
//    @Override
//    public void removeCondition(String conditionGroupId, Condition condition) {
//    }
//
//    @Override
//    public List<Condition> getConditionsByGroupId(String conditionGroupId) {
//        return null;
//    }
//
//    @Override
//    public List<Condition> getConditions(String ruleId) {
//        return null;
//    }
//
//    @Override
//    public void removeConditions(String conditionGroupId) {
//
//    }
//
//    @Override
//    public void removeConditionsByRuleId(String ruleId) {
//
//    }
//
//    @Override
//    public ConditionGroup getConditionGroup(String ruleId, ConditionGroup conditionGroup) {
//        return null;
//    }
//
//    @Override
//    public ConditionGroup getConditionGroup(String conditionGroupId) {
//        return null;
//    }
//
//    @Override
//    public void removeConditionGroup(String ruleId, ConditionGroup conditionGroup) {
//    }
//
//    @Override
//    public List<ConditionGroup> getConditionGroups(String ruleId) {
//        return null;
//    }
//
//    @Override
//    public void removeConditionGroups(Rule rule) {
//    }
//
//    @Override
//    public RuleAction getAction(String ruleId, String actionId) {
//        return null;
//    }
//
//    @Override
//    public RuleAction getAction(String actionId) {
//        return null;
//    }
//
//    @Override
//    public void removeAction(String ruleId, RuleAction action) {
//    }
//
//    @Override
//    public List<RuleAction> getActions(String ruleId) {
//        return null;
//    }
//
//    @Override
//    public void removeActions(Rule rule) {
//    }
//
//    @Override
//    protected Rule addRule(Rule rule) {
//        return rule;
//    }
//
//    @Override
//    protected Condition addCondition(String conditionGroupId, Condition condition) {
//        return null;
//    }
//
//    @Override
//    protected List<Condition> addConditions(String conditionGroupId, List<Condition> conditions) {
//        return conditions;
//    }
//
//    @Override
//    protected ConditionGroup addConditionGroup(String ruleId, ConditionGroup conditionGroup) {
//        return conditionGroup;
//    }
//
//    @Override
//    protected List<ConditionGroup> addConditionGroups(String ruleId, List<ConditionGroup> conditionGroups) {
//        return conditionGroups;
//    }
//
//    @Override
//    protected RuleAction addAction(String ruleId, RuleAction action) {
//        return action;
//    }
//
//    @Override
//    protected List<RuleAction> addActions(String ruleId, List<RuleAction> actions) {
//        return actions;
//    }
//}
//
