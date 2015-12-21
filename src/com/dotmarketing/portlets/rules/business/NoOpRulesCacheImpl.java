package com.dotmarketing.portlets.rules.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import java.util.List;
import java.util.Set;

/**
 * @author Geoff M. Granum
 */
public class NoOpRulesCacheImpl extends RulesCache {

    @Override
    public void addRule(Rule rule) {

    }

    @Override
    public void addRulesByHostFireOn(Set<Rule> rules, String hostId, Rule.FireOn fireOn) {

    }

    @Override
    public Set<Rule> getRulesByHostFireOn(String hostId, Rule.FireOn fireOn) {
        return null;
    }

    @Override
    public Rule getRule(String ruleId) {
        return null;
    }

    @Override
    public List<String> getRulesIdsByHost(Host host) {
        return null;
    }

    @Override
    public void putRulesByHost(Host host, List<Rule> rules) {

    }

    @Override
    public void removeRule(Rule rule) {

    }

    @Override
    public void addCondition(Condition condition) {

    }

    @Override
    public Condition getCondition(String conditionId) {
        return null;
    }

    @Override
    public void removeCondition(Condition condition) {

    }

    @Override
    public void putConditionsByGroup(ConditionGroup conditionGroup, List<Condition> conditions) {

    }

    @Override
    public List<String> getConditionsIdsByGroup(ConditionGroup conditionGroup) {
        return null;
    }

    @Override
    public void addConditionGroup(ConditionGroup conditionGroup) {

    }

    @Override
    public ConditionGroup getConditionGroup(String conditionGroupId) {
        return null;
    }

    @Override
    public void removeConditionGroup(ConditionGroup conditionGroup) {

    }

    @Override
    public void putConditionGroupsByRule(Rule rule, List<ConditionGroup> groups) {

    }

    @Override
    public List<String> getConditionGroupsIdsByRule(Rule rule) {
        return null;
    }

    @Override
    public void addAction(RuleAction action) {

    }

    @Override
    public RuleAction getAction(String actionId) {
        return null;
    }

    @Override
    public void removeAction(RuleAction action) {

    }

    @Override
    public void putActionsByRule(Rule rule, List<RuleAction> actions) {

    }

    @Override
    public List<String> getActionsIdsByRule(Rule rule) {
        return null;
    }

    @Override
    public void clearCache() {

    }
}

