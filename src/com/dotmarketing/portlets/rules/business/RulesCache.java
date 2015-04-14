package com.dotmarketing.portlets.rules.business;

import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.folders.model.Folder;
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
	protected static final String RULE_CONDITION_GROUPS_CACHE = "RuleConditionGroupsCache";
	protected static final String RULE_CONDITIONS_GROUP = "RuleConditionsCache";
	protected static final String RULE_ACTIONS_CACHE = "RuleActionsCache";

	@Override
	public String getPrimaryGroup() {
		return PRIMARY_GROUP;
	}

	@Override
	public String[] getGroups() {
		return new String[] { PRIMARY_GROUP, RULE_CONDITION_GROUPS_CACHE,
                RULE_CONDITIONS_GROUP, RULE_ACTIONS_CACHE};
	}

	/**
	 * Removes all the rules from the caching structure.
	 */
	protected void flushRules() {
		CacheLocator.getCacheAdministrator().flushGroup(PRIMARY_GROUP);
	}

	/**
	 * Removes all the condition groups from the caching structure.
	 */
	protected void flushConditionGroups() {
		CacheLocator.getCacheAdministrator().flushGroup(RULE_CONDITION_GROUPS_CACHE);
	}

	/**
	 * Removes all the conditions from the caching structure.
	 */
	protected void flushConditions() {
		CacheLocator.getCacheAdministrator().flushGroup(RULE_CONDITIONS_GROUP);
	}

	/**
	 * Removes all the action lists from the caching structure.
	 */
	protected void flushActions() {
		CacheLocator.getCacheAdministrator().flushGroup(RULE_ACTIONS_CACHE);
	}

	/**
	 * Adds a {@link Rule} to the caching structure. Null objects or with empty
	 * values will not be added to the cache. In case the rule already exists,
	 * the entry will be updated with the new value.
	 * 
	 * @param rule
	 *            - The {@link Rule} object to cache.
	 * @return The recently added {@link Rule} object.
	 */
	protected abstract Rule addRule(Rule rule);

	/**
	 * Adds a list of {@link Rule} objects to the caching structure. Null or
	 * empty lists will not be added to the cache.
	 * 
	 * @param hostId
	 *            - The {@link Rule} objects.
	 * @return The recently added list of {@link Rule} objects.
	 */
	public abstract List<Rule> addRules(List<Rule> rules);

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
	 * @param hostId
	 *            - The {@link Host} ID.
	 * @return The associated list of {@link Rule} objects.
	 */
	public abstract List<Rule> getRulesByHostId(String hostId);

	/**
	 * Returns the list of {@link Rule} objects that have been created for a
	 * specific folder.
	 * 
	 * @param folderId
	 *            - The {@link Folder} ID.
	 * @return The associated list of {@link Rule} objects.
	 */
	public abstract List<Rule> getRulesByFolderId(String folderId);

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
	 * @param conditionGroupId
	 *            - The {@link ConditionGroup} ID.
	 * @param condition
	 *            - The {@link Condition} object to cache.
	 * @return The recently added {@link Condition} object.
	 */
	protected abstract Condition addCondition(String conditionGroupId,
			Condition condition);

	/**
	 * Returns the {@link Condition} object associated to the specified key.
	 * 
	 * @param conditionGroupId
	 *            - The {@link ConditionGroup} ID.
	 * @param condition
	 *            - The {@link Condition} object.
	 * @return The associated {@link Condition} object.
	 */
	public abstract Condition getCondition(String conditionGroupId,
			Condition condition);

	/**
	 * Returns the {@link Condition} object associated to the specified ID.
	 * 
	 * @param conditionId
	 *            - The {@link Condition} ID.
	 * @return The associated {@link Condition} object.
	 */
	public abstract Condition getCondition(String conditionId);

	/**
	 * Removes the {@link Condition} object from the caching structure.
	 * 
	 * @param conditionGroupId
	 *            - The {@link ConditionGroup} ID where the condition will be
	 *            removed.
	 * @param condition
	 *            - The {@link Condition} object that will be removed.
	 */
	public abstract void removeCondition(String conditionGroupId,
			Condition condition);

	/**
	 * Adds a list of {@link Condition} objects to their associated
	 * {@link ConditionGroup} in the caching structure. Null or empty condition
	 * lists will not be added to the cache. In case the condition group already
	 * exists, the entry will be updated with the new values.
	 * 
	 * @param conditionGroupId
	 *            - The {@link ConditionGroup} ID.
	 * @param conditions
	 *            - The {@link Condition} object to cache.
	 * @return The recently added list of {@link Condition} objects.
	 */
	protected abstract List<Condition> addConditions(String conditionGroupId,
			List<Condition> conditions);

	/**
	 * Returns the list of {@link Condition} objects associated to the specified
	 * {@link ConditionGroup}.
	 * 
	 * @param conditionGroupId
	 *            - The {@link ConditionGroup} ID.
	 * @return The associated list of {@link Condition} objects.
	 */
	public abstract List<Condition> getConditionsByGroupId(
			String conditionGroupId);

	/**
	 * Returns the list of {@link Condition} objects associated to the specified
	 * rule ID.
	 * 
	 * @param ruleId
	 *            - The {@link Rule} object.
	 * @return The associated list of {@link Condition} objects.
	 */
	public abstract List<Condition> getConditions(String ruleId);

	/**
	 * Removes the list of {@link Condition} objects of a specific
	 * {@link ConditionGroup} from the caching structure.
	 * 
	 * @param conditionGroupId
	 *            - The {@link ConditionGroup} ID.
	 */
	public abstract void removeConditions(String conditionGroupId);

	/**
	 * Removes the list of {@link Condition} objects of a specific {@link Rule}
	 * from the caching structure.
	 * 
	 * @param ruleId
	 *            - The {@link Rule} ID.
	 */
	public abstract void removeConditionsByRuleId(String ruleId);

	/**
	 * Adds a {@link ConditionGroup} object in a rule to the caching structure.
	 * 
	 * @param ruleId
	 *            - The {@link Rule} ID.
	 * @param conditionGroup
	 *            - The {@link ConditionGroup} object to cache.
	 * @return The recently added {@link ConditionGroup} object.
	 */
	protected abstract ConditionGroup addConditionGroup(String ruleId,
			ConditionGroup conditionGroup);

	/**
	 * Returns the {@link ConditionGroup} object associated to the specified
	 * rule.
	 * 
	 * @param ruleId
	 *            - The {@link Rule} ID.
	 * @param conditionGroup
	 *            - The {@link ConditionGroup} object to retrieve.
	 * @return The associated {@link ConditionGroup} object.
	 */
	public abstract ConditionGroup getConditionGroup(String ruleId,
			ConditionGroup conditionGroup);

	/**
	 * Returns the {@link ConditionGroup} object associated to the specified ID.
	 * 
	 * @param conditionGroupId
	 *            - The {@link ConditionGroup} ID.
	 * @return The associated {@link ConditionGroup} object.
	 */
	public abstract ConditionGroup getConditionGroup(String conditionGroupId);

	/**
	 * Removes the {@link ConditionGroup} object of a specific rule from the
	 * caching structure.
	 * 
	 * @param ruleId
	 *            - The {@link Rule} ID.
	 * @param conditionGroup
	 *            - The {@link ConditionGroup} object to remove.
	 */
	public abstract void removeConditionGroup(String ruleId,
			ConditionGroup conditionGroup);

	/**
	 * Adds a list of {@link ConditionGroup} objects in a rule to the caching
	 * structure.
	 * 
	 * @param ruleId
	 *            - The {@link Rule} ID.
	 * @param conditionGroups
	 *            - The {@link ConditionGroup} objects to cache.
	 * @return The recently added {@link ConditionGroup} object.
	 */
	protected abstract List<ConditionGroup> addConditionGroups(String ruleId,
			List<ConditionGroup> conditionGroups);

	/**
	 * Returns the {@link Condition} object associated to the specified rule.
	 * 
	 * @param ruleId
	 *            - The {@link Rule} ID.
	 * @return The associated list of {@link ConditionGroup} objects.
	 */
	public abstract List<ConditionGroup> getConditionGroups(String ruleId);

	/**
	 * Removes the list of {@link ConditionGroup} objects of a specific rule
	 * from the caching structure.
	 * 
	 * @param rule
	 *            - The {@link Rule} object.
	 */
	public abstract void removeConditionGroups(Rule rule);

	/**
	 * Adds a {@link RuleAction} object in a rule to the caching structure. In
	 * case the action list already exists, the entry will be updated with the
	 * new values.
	 * 
	 * @param ruleId
	 *            - The {@link Rule} ID.
	 * @param action
	 *            - The {@link RuleAction} object to cache.
	 * @return The recently added {@link RuleAction} object.
	 */
	protected abstract RuleAction addAction(String ruleId, RuleAction action);

	/**
	 * Returns the {@link RuleAction} object associated to the specified rule
	 * and ID.
	 * 
	 * @param ruleId
	 *            - The {@link Rule} ID.
	 * @param actionId
	 *            - The {@link RuleAction} ID.
	 * @return The associated {@link RuleAction} object.
	 */
	public abstract RuleAction getAction(String ruleId, String actionId);

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
	 * @param ruleId
	 *            - The {@link Rule} ID.
	 */
	public abstract void removeAction(String ruleId, RuleAction action);

	/**
	 * Adds a list of {@link RuleAction} objects in a rule to the caching
	 * structure. In case the action list already exists, the entry will be
	 * updated with the new values.
	 * 
	 * @param ruleId
	 *            - The {@link Rule} ID.
	 * @param actions
	 *            - The {@link RuleAction} objects to cache.
	 * @return The recently added {@link RuleAction} object list.
	 */
	protected abstract List<RuleAction> addActions(String ruleId,
			List<RuleAction> actions);

	/**
	 * Returns the list of {@link RuleAction} objects associated to the
	 * specified rule.
	 * 
	 * @param ruleId
	 *            - The {@link Rule} ID.
	 * @return The associated list of {@link RuleAction} objects.
	 */
	public abstract List<RuleAction> getActions(String ruleId);

	/**
	 * Removes the list of {@link RuleAction} objects of a specific rule from
	 * the caching structure.
	 * 
	 * @param rule
	 *            - The {@link Rule} object.
	 */
	public abstract void removeActions(Rule rule);

}
