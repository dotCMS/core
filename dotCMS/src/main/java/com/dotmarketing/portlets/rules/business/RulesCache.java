package com.dotmarketing.portlets.rules.business;

import java.util.List;
import java.util.Set;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;

/**
 * Provides a caching mechanism to improve response times regarding the access
 * to rules, condition lists, and related objects of the Rules Engine. This
 * cache will be composed of several groups associated to the different sections
 * that make up a rule.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-06-2015
 *
 */
public abstract class RulesCache implements Cachable {

	// Caching groups for the different sections of a rule
	protected static final String PRIMARY_GROUP = "RulesCache";
	protected static final String CONDITIONS_CACHE = "ConditionsCache";
	protected static final String CONDITION_GROUPS_CACHE = "ConditionsGroupsCache";
	protected static final String ACTIONS_CACHE = "ActionsCache";
    protected static final String PARENT_RULES_CACHE = "ParentRulesCache";
    protected static final String RULE_CONDITION_GROUPS = "RuleConditionGroupsCache";
    protected static final String CONDITION_GROUP_CONDITIONS_CACHE = "ConditionsGroupConditionsCache";
    protected static final String RULE_ACTIONS_CACHE = "RuleActionsCache";

	@Override
	public String getPrimaryGroup() {
		return PRIMARY_GROUP;
	}

	@Override
	public String[] getGroups() {
		return new String[] { PRIMARY_GROUP, CONDITIONS_CACHE, CONDITION_GROUPS_CACHE, ACTIONS_CACHE
            , PARENT_RULES_CACHE, RULE_CONDITION_GROUPS, CONDITION_GROUP_CONDITIONS_CACHE, RULE_ACTIONS_CACHE};
	}

	/**
	 * Adds a {@link Rule} to the caching structure. Null objects or with empty
	 * values will not be added to the cache. In case the rule already exists,
	 * the entry will be updated with the new value.
	 * 
	 * @param rule
	 *            - The {@link Rule} object to cache.
	 */
	public abstract void addRule(Rule rule);

    /**
     * Adds a list of {@link Rule} objects under the {@Link Ruleable} with the given parentId
     * and whose 'Fire On' matches the given fireOn
     *
     * @param rules
     * @param parentId
     * @param fireOn
     * @return
     */
    public abstract void addRulesByParentFireOn(Set<Rule> rules, String parentId, Rule.FireOn fireOn);

    /**
     * Returns a list of {@link Rule} objects under the {@Link Ruleable} with the given parentId
     * and whose 'Fire On' matches the given fireOn
     * @param parentId
     * @param fireOn
     * @return
     */
    public abstract Set<Rule> getRulesByParentFireOn(String parentId, Rule.FireOn fireOn);


    /**
	 * Returns the {@link Rule} object associated to the specified key.
	 * 
	 * @param ruleId
	 *            - The ID of the {@link Rule} object.
	 * @return The associated {@link Rule} object.
	 */
	public abstract Rule getRule(String ruleId);

	/**
	 * Returns the list of {@link Rule} objects that have been created for a
	 * specific site (host).
	 * 
	 * @param parent
	 *            - The {@link Ruleable}.
	 * @return The associated list of {@link Rule} objects.
	 */
    public abstract List<String> getRulesIdsByParent(Ruleable parent);

    /**
     * Puts the list of {@link Rule} objects that have been created for a
     * specific site (parent).
     * @param parent - The {@link Ruleable}.
     * @param rules - The list of {@link Rule}.
     */

    public abstract void putRulesByParent(Ruleable parent, List<Rule> rules);

	/**
	 * Removes the {@link Rule} object from the caching structure.
	 * 
	 * @param rule
	 *            - The {@link Rule} object that will be removed.
	 */
	public abstract void removeRule(Rule rule);

	/**
	 * Adds a {@link Condition} to the caching structure. Null objects or with
	 * empty values will not be added to the cache. In case the condition
	 * already exists, the entry will be updated with the new value.
	 * 
	 * @param condition
	 *            - The {@link Condition} object to cache.
	 */
	public abstract void addCondition(Condition condition);

	/**
	 * Returns the {@link Condition} object associated to the specified key.
	 * 
	 * @param conditionId
	 *            - The {@link Condition} ID.
	 * @return The associated {@link Condition} object.
	 */
	public abstract Condition getCondition(String conditionId);

	/**
	 * Removes the {@link Condition} object from the caching structure.
	 * 
	 * @param condition
	 *            - The {@link Condition} object that will be removed.
	 */
	public abstract void removeCondition(Condition condition);

	/**
	 * Adds a list of {@link Condition} to their associated
	 * {@link ConditionGroup} in the caching structure. Null or empty conditions ids
	 * lists will not be added to the cache. In case the condition group already
	 * exists, the entry will be updated with the new values.
	 * 
	 * @param conditionGroup
	 *            - The {@link ConditionGroup}.
	 * @param conditions
	 *            - The {@link Condition} object to cache.
	 */
	public abstract void putConditionsByGroup(ConditionGroup conditionGroup, List<Condition> conditions);

	/**
	 * Returns the list of {@link Condition} ids associated to the specified
	 * {@link ConditionGroup}.
	 * 
	 * @param conditionGroup
	 *            - The {@link ConditionGroup}.
	 * @return The associated list of {@link Condition} objects.
	 */
    public abstract List<String> getConditionsIdsByGroup(ConditionGroup conditionGroup);

	/**
	 * Adds a {@link ConditionGroup} object to the caching structure.
	 * 
	 * @param conditionGroup
	 *            - The {@link ConditionGroup} object to cache.
	 */
	public abstract void addConditionGroup(ConditionGroup conditionGroup);

	/**
	 * Returns the {@link ConditionGroup} object associated to the specified ID.
	 * 
	 * @param conditionGroupId
	 *            - The {@link ConditionGroup} ID.
	 * @return The associated {@link ConditionGroup} object.
	 */
	public abstract ConditionGroup getConditionGroup(String conditionGroupId);

	/**
	 * Removes the {@link ConditionGroup} object from the
	 * caching structure.
	 * 
	 * @param conditionGroup
	 *            - The {@link ConditionGroup} object to remove.
	 */
	public abstract void removeConditionGroup(ConditionGroup conditionGroup);

	/**
	 * Adds a list of {@link ConditionGroup} objects in a rule to the caching
	 * structure.
	 * 
	 * @param rule
	 *            - The {@link Rule}.
	 * @param groups
	 *            - The {@link ConditionGroup} objects to cache.
	 * @return The recently added {@link ConditionGroup} object.
	 */
    public abstract void putConditionGroupsByRule(Rule rule, List<ConditionGroup> groups);

	/**
	 * Returns the {@link Condition} ids associated to the specified rule.
	 * 
	 * @param rule
	 *            - The {@link Rule}.
	 * @return The associated list of {@link ConditionGroup} ids.
	 */
	public abstract List<String> getConditionGroupsIdsByRule(Rule rule);

	/**
	 * Adds a {@link RuleAction} object to the caching structure.
	 * 
	 * @param action
	 *            - The {@link RuleAction} object to cache.
	 */
	public abstract void addAction(RuleAction action);

	/**
	 * Returns the {@link RuleAction} object associated to the specified ID.
	 * 
	 * @param actionId
	 *            - The {@link RuleAction} ID.
	 * @return The associated {@link RuleAction} object.
	 */
	public abstract RuleAction getAction(String actionId);

	/**
	 * Removes the {@link RuleAction} object of a specific rule from the caching
	 * structure.
	 * 
	 */
	public abstract void removeAction(RuleAction action);

	/**
	 * Adds a list of {@link RuleAction} objects in a rule to the caching
	 * structure.
	 * 
	 * @param rule
	 *            - The {@link Rule}.
	 * @param actions
	 *            - The {@link RuleAction} objects to cache.
	 * @return The recently added {@link RuleAction} object list.
	 */
    public abstract void putActionsByRule(Rule rule, List<RuleAction> actions);

	/**
	 * Returns the list of {@link RuleAction} ids associated to the
	 * specified rule.
	 * 
	 * @param rule
	 *            - The {@link Rule}.
	 * @return The associated list of {@link RuleAction} ids.
	 */
    public abstract List<String> getActionsIdsByRule(Rule rule);


}
