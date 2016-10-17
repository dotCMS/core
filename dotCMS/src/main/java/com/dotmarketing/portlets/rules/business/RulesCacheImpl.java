package com.dotmarketing.portlets.rules.business;

import java.util.*;
import java.util.stream.Collectors;

import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.portlets.rules.model.*;
import com.dotmarketing.util.Logger;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull;

/**
 * Implements the Rule Engine caching functionality. The structures that make up
 * the cache are hierarchically associated in order to improve response times by
 * reducing database round trips.
 *
 * @author Jose Castro
 * @version 1.0
 * @since 04-06-2015
 */
public class RulesCacheImpl extends RulesCache {

    protected DotCacheAdministrator cache = null;

    /**
     * Default constructor. Instantiates the {@link DotCacheAdministrator}
     * object used to store all the rules information.
     */
    public RulesCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
    }

    @Override
    public void clearCache() {
        for (String cacheGroup : getGroups()) {
            cache.flushGroup(cacheGroup);
        }
    }

    @Override
    public void addRule(Rule rule) {
        rule = checkNotNull(rule, "Rule is required.");

        if (Strings.isNullOrEmpty(rule.getId())) {
            throw new IllegalArgumentException("Rule must have an id.");
        }

        this.cache.put(rule.getId(), rule, getPrimaryGroup());
    }

    @Override
    public Rule getRule(String ruleId) {
        Rule rule = null;
        try {
            rule = (Rule) this.cache.get(ruleId, getPrimaryGroup());
        } catch (DotCacheException e) {
            Logger.debug(this, "RulesCache entry not found: " + ruleId);
        }
        return rule;
    }

    @Override
    public void removeRule(Rule rule) {
        rule = checkNotNull(rule, "Rule is required.");

        if (Strings.isNullOrEmpty(rule.getId())) {
            throw new IllegalArgumentException("Rule must have an Id.");
        }

        this.cache.remove(rule.getId(), getPrimaryGroup());

        for(Rule.FireOn fireOn: Rule.FireOn.values()) {
            cache.remove(rule.getParent() + ":" + fireOn, getPrimaryGroup());
        }

        // let's clean the
        cache.remove(rule.getParent(), PARENT_RULES_CACHE);

    }

    @Override
    public void putRulesByParent(Ruleable parent, List<Rule> rules) {
    	parent = checkNotNull(parent, "parent is required");

        String parentIdentifier = parent.getIdentifier();

        if (Strings.isNullOrEmpty(parentIdentifier)) {
            throw new IllegalArgumentException("Parent must have an identifier.");
        }

        rules = checkNotNull(rules, "Rules List is required");

        for (Rule rule : rules) {
            addRule(rule);
        }

        List<String> rulesIds = rules.stream().map(Rule::getId).collect(Collectors.toList());

        cache.put(parentIdentifier, rulesIds, PARENT_RULES_CACHE);
    }

    @Override
    public List<String> getRulesIdsByParent(Ruleable parent) {
    	parent = checkNotNull(parent, "Parent is required");
        String parentIdentifier = parent.getIdentifier();

        if (Strings.isNullOrEmpty(parentIdentifier)) {
            throw new IllegalArgumentException("Parent must have an identifier.");
        }
        try {
            return (List<String>) cache.get(parentIdentifier, PARENT_RULES_CACHE);
        } catch (DotCacheException e) {
            Logger.debug(RulesCacheImpl.class, e.getMessage(), e);
            return null;
        }
    }


    @Override
    public void addRulesByParentFireOn(Set<Rule> rules, String parentIdentifier, Rule.FireOn fireOn) {
        rules = checkNotNull(rules, "Rules list is required.");

        if (Strings.isNullOrEmpty(parentIdentifier)) {
            throw new IllegalArgumentException("Invalid parent identifier.");
        }

        fireOn = checkNotNull(fireOn, "FireOn is required.");

        cache.remove(parentIdentifier + ":" + fireOn, getPrimaryGroup());
        cache.put(parentIdentifier + ":" + fireOn, rules, getPrimaryGroup());
    }

    @Override
    public Set<Rule> getRulesByParentFireOn(String parentIdentifier, Rule.FireOn fireOn) {
        if (Strings.isNullOrEmpty(parentIdentifier)) {
            throw new IllegalArgumentException("Invalid parent identifier.");
        }

        fireOn = checkNotNull(fireOn, "FireOn is required.");

        try {
            return (Set<Rule>) cache.get(parentIdentifier + ":" + fireOn, PRIMARY_GROUP);
        } catch (DotCacheException e) {
            Logger.debug(RulesCacheImpl.class, e.getMessage(), e);
        }
        return null;
    }


    @Override
    public void addCondition(Condition condition) {
        condition = checkNotNull(condition, "Condition is required.");

        if (Strings.isNullOrEmpty(condition.getId())) {
            throw new IllegalArgumentException("Condition must have an Id");
        }

        this.cache.put(condition.getId(), condition, CONDITIONS_CACHE);
    }

    @Override
    public Condition getCondition(String conditionId) {
        if (Strings.isNullOrEmpty(conditionId)) {
            throw new IllegalArgumentException("Invalid conditionId.");
        }

        Condition condition = null;

        try {
            condition = (Condition) this.cache.get(conditionId, CONDITIONS_CACHE);
        } catch (DotCacheException e) {
            Logger.debug(this, "ConditionsCache entry not found: " + conditionId);
        }
        return condition;
    }

    @Override
    public void removeCondition(Condition condition) {
        condition = checkNotNull(condition, "Condition is required.");

        if (Strings.isNullOrEmpty(condition.getId())) {
            throw new IllegalArgumentException("Condition must have an Id.");
        }

        this.cache.remove(condition.getId(), CONDITIONS_CACHE);
        this.cache.remove(condition.getConditionGroup(), CONDITION_GROUP_CONDITIONS_CACHE);
        this.cache.remove(condition.getConditionGroup(), CONDITION_GROUPS_CACHE);
    }

    @Override
    public void putConditionsByGroup(ConditionGroup conditionGroup, List<Condition> conditions) {
        conditionGroup = checkNotNull(conditionGroup, "Condition Group is required");

        String groupId = conditionGroup.getId();

        if (Strings.isNullOrEmpty(groupId)) {
            throw new IllegalArgumentException("Condition Group must have an id.");
        }

        conditions = checkNotNull(conditions, "Condition List is required");

        for (Condition condition : conditions) {
            addCondition(condition);
        }

        List<String> conditionsIds = conditions.stream().map(Condition::getId).collect(Collectors.toList());

        cache.put(groupId, conditionsIds, CONDITION_GROUP_CONDITIONS_CACHE);
    }

    @Override
    public List<String> getConditionsIdsByGroup(ConditionGroup conditionGroup) {
        conditionGroup = checkNotNull(conditionGroup, "Condition Group is required");
        String conditionGroupId = conditionGroup.getId();

        if (Strings.isNullOrEmpty(conditionGroupId)) {
            throw new IllegalArgumentException("Condition Group must have an id.");
        }
        try {
            return (List<String>) cache.get(conditionGroupId, CONDITION_GROUP_CONDITIONS_CACHE);
        } catch (DotCacheException e) {
            Logger.debug(RulesCacheImpl.class, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void addConditionGroup(ConditionGroup conditionGroup) {
        conditionGroup = checkNotNull(conditionGroup, "Condition Group is required.");

        if (Strings.isNullOrEmpty(conditionGroup.getId())) {
            throw new IllegalArgumentException("Condition Group must have an Id");
        }

        this.cache.put(conditionGroup.getId(), conditionGroup, CONDITION_GROUPS_CACHE);
    }


    @Override
    public ConditionGroup getConditionGroup(String conditionGroupId) {
        if (Strings.isNullOrEmpty(conditionGroupId)) {
            throw new IllegalArgumentException("Invalid conditionGroupId.");
        }

        ConditionGroup conditionGroup = null;

        try {
            conditionGroup = (ConditionGroup) this.cache.get(conditionGroupId, CONDITION_GROUPS_CACHE);
        } catch (DotCacheException e) {
            Logger.debug(this, "ConditionsCache entry not found: " + conditionGroupId);
        }
        return conditionGroup;
    }

    @Override
    public void removeConditionGroup(ConditionGroup conditionGroup) {
        conditionGroup = checkNotNull(conditionGroup, "Condition Group is required.");

        if (Strings.isNullOrEmpty(conditionGroup.getId())) {
            throw new IllegalArgumentException("Condition must have an Id.");
        }

        this.cache.remove(conditionGroup.getId(), CONDITION_GROUPS_CACHE);
        this.cache.remove(conditionGroup.getRuleId(), RULE_CONDITION_GROUPS);
    }

    @Override
    public void putConditionGroupsByRule(Rule rule, List<ConditionGroup> groups) {
        rule = checkNotNull(rule, "Rule is required.");

        String ruleId = rule.getId();

        if (Strings.isNullOrEmpty(ruleId)) {
            throw new IllegalArgumentException("Rule must have an id.");
        }

        groups = checkNotNull(groups, "Condition List is required");

        for (ConditionGroup group : groups) {
            addConditionGroup(group);
        }

        List<String> conditionGroupsIds = groups.stream().map(ConditionGroup::getId).collect(Collectors.toList());

        cache.put(ruleId, conditionGroupsIds, RULE_CONDITION_GROUPS);
    }

    @Override
    public List<String> getConditionGroupsIdsByRule(Rule rule) {
        rule = checkNotNull(rule, "Rule is required");
        String ruleId = rule.getId();

        if (Strings.isNullOrEmpty(ruleId)) {
            throw new IllegalArgumentException("Rule must have an id.");
        }
        try {
            return (List<String>) cache.get(ruleId, RULE_CONDITION_GROUPS);
        } catch (DotCacheException e) {
            Logger.debug(RulesCacheImpl.class, e.getMessage(), e);
            return null;
        }
    }


    @Override
    public void addAction(RuleAction action) {
        action = checkNotNull(action, "Action is required.");

        if (Strings.isNullOrEmpty(action.getId())) {
            throw new IllegalArgumentException("Action must have an Id");
        }

        this.cache.put(action.getId(), action, ACTIONS_CACHE);
    }

    @Override
    public RuleAction getAction(String actionId) {
        if (Strings.isNullOrEmpty(actionId)) {
            throw new IllegalArgumentException("Invalid actionId.");
        }

        RuleAction action = null;

        try {
            action = (RuleAction) this.cache.get(actionId, ACTIONS_CACHE);
        } catch (DotCacheException e) {
            Logger.debug(this, "ActionsCache entry not found: " + actionId);
        }
        return action;
    }

    @Override
    public void removeAction(RuleAction action) {
        action = checkNotNull(action, "Rule Action is required.");

        if (Strings.isNullOrEmpty(action.getId())) {
            throw new IllegalArgumentException("Rule Action must have an Id.");
        }

        this.cache.remove(action.getId(), ACTIONS_CACHE);
        this.cache.remove(action.getRuleId(), RULE_ACTIONS_CACHE);
    }

    @Override
    public void putActionsByRule(Rule rule, List<RuleAction> actions) {
        rule = checkNotNull(rule, "Rule is required.");

        String ruleId = rule.getId();

        if (Strings.isNullOrEmpty(ruleId)) {
            throw new IllegalArgumentException("Rule must have an id.");
        }

        actions = checkNotNull(actions, "Action List is required");

        for (RuleAction action : actions) {
            addAction(action);
        }

        List<String> actionsIds = actions.stream().map(RuleAction::getId).collect(Collectors.toList());

        cache.put(ruleId, actionsIds, RULE_ACTIONS_CACHE);
    }

    @Override
    public List<String> getActionsIdsByRule(Rule rule) {
        rule = checkNotNull(rule, "Rule is required");
        String ruleId = rule.getId();

        if (Strings.isNullOrEmpty(ruleId)) {
            throw new IllegalArgumentException("Rule must have an id.");
        }
        try {
            return (List<String>) cache.get(ruleId, RULE_ACTIONS_CACHE);
        } catch (DotCacheException e) {
            Logger.debug(RulesCacheImpl.class, e.getMessage(), e);
            return null;
        }
    }


}
