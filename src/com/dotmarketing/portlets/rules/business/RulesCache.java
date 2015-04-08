package com.dotmarketing.portlets.rules.business;

import java.util.List;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
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
	protected static final String CONDITIONGROUPS_GROUP = "ConditionGroupsCache";
	protected static final String CONDITIONS_GROUP = "ConditionsCache";
	protected static final String ACTIONS_GROUP = "ActionsCache";

	@Override
	public String getPrimaryGroup() {
		return PRIMARY_GROUP;
	}

	@Override
	public String[] getGroups() {
		return new String[] { PRIMARY_GROUP, CONDITIONGROUPS_GROUP,
				CONDITIONS_GROUP, ACTIONS_GROUP };
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
		CacheLocator.getCacheAdministrator().flushGroup(CONDITIONGROUPS_GROUP);
	}

	/**
	 * Removes all the conditions from the caching structure.
	 */
	protected void flushConditions() {
		CacheLocator.getCacheAdministrator().flushGroup(CONDITIONS_GROUP);
	}

	/**
	 * Removes all the action lists from the caching structure.
	 */
	protected void flushActions() {
		CacheLocator.getCacheAdministrator().flushGroup(ACTIONS_GROUP);
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
	 * Returns the {@link Rule} object associated to the specified key.
	 * 
	 * @param key
	 *            - The ID of the {@link Rule} object.
	 * @return The associated {@link Rule} object.
	 */
	public abstract Rule getRule(String key);

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
	 * @param conditionGroup
	 *            - The {@link ConditionGroup} where the condition will be
	 *            added.
	 * @param condition
	 *            - The {@link Condition} object to cache.
	 * @return The recently added {@link Condition} object.
	 */
	protected abstract Condition addCondition(ConditionGroup conditionGroup,
			Condition condition);

	/**
	 * Returns the {@link Condition} object associated to the specified key.
	 * 
	 * @param conditionGroup
	 *            - The {@link ConditionGroup} where the condition will be
	 *            retrieved.
	 * @param condition
	 *            - The {@link Condition} object.
	 * @return The associated {@link Condition} object.
	 */
	public abstract Condition getCondition(ConditionGroup conditionGroup,
			Condition condition);

	/**
	 * Removes the {@link Condition} object from the caching structure.
	 * 
	 * @param conditionGroup
	 *            - The {@link ConditionGroup} where the condition will be
	 *            removed.
	 * @param condition
	 *            - The {@link Condition} object that will be removed.
	 */
	public abstract void removeCondition(ConditionGroup conditionGroup,
			Condition condition);

	/**
	 * Adds a list of {@link Condition} objects to their associated
	 * {@link ConditionGroup} in the caching structure. Null or empty condition
	 * lists will not be added to the cache. In case the condition group already
	 * exists, the entry will be updated with the new values.
	 * 
	 * @param conditionGroup
	 *            - The {@link ConditionGroup} object whose conditions will be
	 *            added to the cache.
	 * @param conditions
	 *            - The {@link Condition} object to cache.
	 * @return The recently added list of {@link Condition} objects.
	 */
	protected abstract List<Condition> addConditions(
			ConditionGroup conditionGroup, List<Condition> conditions);

	/**
	 * Returns the list of {@link Condition} objects associated to the specified
	 * {@link ConditionGroup}.
	 * 
	 * @param conditionGroup
	 *            - The {@link ConditionGroup} object.
	 * @return The associated list of {@link Condition} objects.
	 */
	public abstract List<Condition> getConditions(ConditionGroup conditionGroup);

	/**
	 * Removes the list of {@link Condition} objects of a specific
	 * {@link ConditionGroup} from the caching structure.
	 * 
	 * @param key
	 *            - The {@link ConditionGroup} object.
	 */
	public abstract void removeConditions(ConditionGroup conditionGroup);

	/**
	 * Adds a {@link ConditionGroup} object in a rule to the caching structure.
	 * 
	 * @param rule
	 *            - The {@link Rule} object.
	 * @param conditionGroup
	 *            - The {@link ConditionGroup} object to cache.
	 * @return The recently added {@link ConditionGroup} object.
	 */
	protected abstract ConditionGroup addConditionGroup(Rule rule,
			ConditionGroup conditionGroup);

	/**
	 * Returns the {@link ConditionGroup} object associated to the specified
	 * rule.
	 * 
	 * @param rule
	 *            - The {@link Rule} object.
	 * @param conditionGroup
	 *            - The {@link ConditionGroup} object to retrieve.
	 * @return The associated {@link ConditionGroup} object.
	 */
	public abstract ConditionGroup getConditionGroup(Rule rule,
			ConditionGroup conditionGroup);

	/**
	 * Removes the {@link ConditionGroup} object of a specific rule from the
	 * caching structure.
	 * 
	 * @param rule
	 *            - The {@link Rule} object.
	 * @param conditionGroup
	 *            - The {@link ConditionGroup} object to remove.
	 */
	public abstract void removeConditionGroup(Rule rule,
			ConditionGroup conditionGroup);

	/**
	 * Adds a list of {@link ConditionGroup} objects in a rule to the caching
	 * structure.
	 * 
	 * @param rule
	 *            - The {@link Rule} object.
	 * @param conditionGroups
	 *            - The {@link ConditionGroup} objects to cache.
	 * @return The recently added {@link ConditionGroup} object.
	 */
	protected abstract List<ConditionGroup> addConditionGroups(Rule rule,
			List<ConditionGroup> conditionGroups);

	/**
	 * Returns the {@link Condition} object associated to the specified rule.
	 * 
	 * @param rule
	 *            - The {@link Rule} object.
	 * @return The associated list of {@link ConditionGroup} objects.
	 */
	public abstract List<ConditionGroup> getConditionGroups(Rule rule);

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
	 * @param rule
	 *            - The {@link Rule} object.
	 * @param actions
	 *            - The {@link RuleAction} object to cache.
	 * @return The recently added {@link RuleAction} object list.
	 */
	protected abstract RuleAction addAction(Rule rule, RuleAction action);

	/**
	 * Returns the {@link RuleAction} object associated to the specified rule.
	 * 
	 * @param rule
	 *            - The {@link Rule} object.
	 * @return The associated {@link RuleAction} object.
	 */
	public abstract RuleAction getAction(Rule rule, RuleAction action);

	/**
	 * Removes the {@link RuleAction} object of a specific rule from the caching
	 * structure.
	 * 
	 * @param rule
	 *            - The {@link Rule} object.
	 */
	public abstract void removeAction(Rule rule, RuleAction action);

	/**
	 * Adds a list of {@link RuleAction} objects in a rule to the caching
	 * structure. In case the action list already exists, the entry will be
	 * updated with the new values.
	 * 
	 * @param rule
	 *            - The {@link Rule} object.
	 * @param actions
	 *            - The {@link RuleAction} objects to cache.
	 * @return The recently added {@link RuleAction} object list.
	 */
	protected abstract List<RuleAction> addActions(Rule rule,
			List<RuleAction> actions);

	/**
	 * Returns the list of {@link RuleAction} objects associated to the
	 * specified rule.
	 * 
	 * @param rule
	 *            - The {@link Rule} object.
	 * @return The associated list of {@link RuleAction} objects.
	 */
	public abstract List<RuleAction> getActions(Rule rule);

	/**
	 * Removes the list of {@link RuleAction} objects of a specific rule from
	 * the caching structure.
	 * 
	 * @param rule
	 *            - The {@link Rule} object.
	 */
	public abstract void removeActions(Rule rule);

}
