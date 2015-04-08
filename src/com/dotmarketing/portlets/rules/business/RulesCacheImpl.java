package com.dotmarketing.portlets.rules.business;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * Implements the Rule Engine caching functionality. The structures that make up
 * the cache are hierarchically associated in order to improve response times by
 * reducing database round trips.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-06-2015
 *
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
	protected Rule addRule(Rule rule) {
		if (rule != null && UtilMethods.isSet(rule.getId())) {
			String ruleId = rule.getId();
			this.cache.put(ruleId, rule, getPrimaryGroup());
		}
		return rule;
	}

	@Override
	public Rule getRule(String key) {
		Rule rule = null;
		try {
			rule = (Rule) this.cache.get(key, getPrimaryGroup());
		} catch (DotCacheException e) {
			Logger.debug(this, "RulesCache entry not found: " + key);
		}
		return rule;
	}

	@Override
	public void removeRule(Rule rule) {
		if (rule != null && UtilMethods.isSet(rule.getId())) {
			this.cache.remove(rule.getId(), getPrimaryGroup());
		}
	}

	@Override
	protected Condition addCondition(ConditionGroup conditionGroup,
			Condition condition) {
		String key = conditionGroup.getId();
		try {
			List<Condition> conditions = (List<Condition>) this.cache.get(key,
					CONDITIONS_GROUP);
			if (conditions == null) {
				conditions = new ArrayList<Condition>();
			}
			if (condition != null && UtilMethods.isSet(condition.getId())) {
				conditions.add(condition);
				this.cache.put(key, conditions, CONDITIONS_GROUP);
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "ConditionsCache entry not found: " + key);
		}
		return condition;
	}

	@Override
	public Condition getCondition(ConditionGroup conditionGroup,
			Condition condition) {
		String key = conditionGroup.getId();
		Condition cachedCondition = null;
		try {
			List<Condition> conditions = (List<Condition>) this.cache.get(key,
					CONDITIONS_GROUP);
			for (Condition cond : conditions) {
				if (cond.getId().equals(condition.getId())) {
					cachedCondition = cond;
					break;
				}
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "ConditionsCache entry not found: " + key);
		}
		return cachedCondition;
	}

	@Override
	public void removeCondition(ConditionGroup conditionGroup,
			Condition condition) {
		String key = conditionGroup.getId();
		try {
			int index = -1;
			List<Condition> conditions = (List<Condition>) this.cache.get(key,
					CONDITIONS_GROUP);
			for (int i = 0; i < conditions.size(); i++) {
				Condition cond = conditions.get(i);
				if (cond.getId().equals(condition.getId())) {
					index = i;
					break;
				}
			}
			if (index >= 0) {
				conditions.remove(index);
				this.cache.put(key, conditions, CONDITIONS_GROUP);
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "ConditionsCache entry not found: " + key);
		}
	}

	@Override
	protected List<Condition> addConditions(ConditionGroup conditionGroup,
			List<Condition> conditions) {
		String key = conditionGroup.getId();
		if (conditions != null && conditions.size() > 0
				&& UtilMethods.isSet(key)) {
			this.cache.put(key, conditions, CONDITIONS_GROUP);
		}
		return conditions;
	}

	@Override
	public List<Condition> getConditions(ConditionGroup conditionGroup) {
		List<Condition> conditions = null;
		String key = conditionGroup.getId();
		if (UtilMethods.isSet(key)) {
			try {
				conditions = (List<Condition>) this.cache.get(key,
						CONDITIONS_GROUP);
			} catch (DotCacheException e) {
				Logger.debug(this, "ConditionsCache entry not found: " + key);
			}
		}
		return conditions;
	}

	@Override
	public void removeConditions(ConditionGroup conditionGroup) {
		String key = conditionGroup.getId();
		if (UtilMethods.isSet(key)) {
			this.cache.remove(key, CONDITIONS_GROUP);
		}
	}

	@Override
	protected ConditionGroup addConditionGroup(Rule rule,
			ConditionGroup conditionGroup) {
		String key = rule.getId();
		try {
			List<ConditionGroup> conditionGroups = (List<ConditionGroup>) this.cache
					.get(key, CONDITIONGROUPS_GROUP);
			if (conditionGroups == null) {
				conditionGroups = new ArrayList<ConditionGroup>();
			}
			if (conditionGroup != null
					&& UtilMethods.isSet(conditionGroup.getId())) {
				conditionGroups.add(conditionGroup);
				this.cache.put(key, conditionGroups, CONDITIONGROUPS_GROUP);
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "ConditionGroupsCache entry not found: " + key);
		}
		return conditionGroup;
	}

	@Override
	public ConditionGroup getConditionGroup(Rule rule,
			ConditionGroup conditionGroup) {
		String key = rule.getId();
		ConditionGroup cachedConditionGroup = null;
		try {
			List<ConditionGroup> conditionGroups = (List<ConditionGroup>) this.cache
					.get(key, CONDITIONGROUPS_GROUP);
			for (ConditionGroup condGroup : conditionGroups) {
				if (condGroup.getId().equals(conditionGroup.getId())) {
					cachedConditionGroup = condGroup;
					break;
				}
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "ConditionGroupsCache entry not found: " + key);
		}
		return cachedConditionGroup;
	}

	@Override
	public void removeConditionGroup(Rule rule, ConditionGroup conditionGroup) {
		String key = rule.getId();
		try {
			int index = -1;
			List<ConditionGroup> conditionGroups = (List<ConditionGroup>) this.cache
					.get(key, CONDITIONGROUPS_GROUP);
			for (int i = 0; i < conditionGroups.size(); i++) {
				ConditionGroup condGroup = conditionGroups.get(i);
				if (condGroup.getId().equals(conditionGroup.getId())) {
					index = i;
					break;
				}
			}
			if (index >= 0) {
				conditionGroups.remove(index);
				this.cache.put(key, conditionGroups, CONDITIONGROUPS_GROUP);
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "ConditionGroupsCache entry not found: " + key);
		}
	}

	@Override
	protected List<ConditionGroup> addConditionGroups(Rule rule,
			List<ConditionGroup> conditionGroups) {
		String key = rule.getId();
		if (conditionGroups != null && UtilMethods.isSet(key)) {
			this.cache.put(key, conditionGroups, CONDITIONGROUPS_GROUP);
		}
		return conditionGroups;
	}

	@Override
	public List<ConditionGroup> getConditionGroups(Rule rule) {
		String key = rule.getId();
		List<ConditionGroup> conditionGroups = null;
		if (UtilMethods.isSet(key)) {
			try {
				conditionGroups = (List<ConditionGroup>) this.cache.get(key,
						CONDITIONGROUPS_GROUP);
			} catch (DotCacheException e) {
				Logger.debug(this, "ConditionGroupsCache entry not found: "
						+ key);
			}
		}
		return conditionGroups;
	}

	@Override
	public void removeConditionGroups(Rule rule) {
		String key = rule.getId();
		if (UtilMethods.isSet(key)) {
			this.cache.remove(key, CONDITIONGROUPS_GROUP);
		}
	}

	@Override
	protected RuleAction addAction(Rule rule, RuleAction action) {
		String key = rule.getId();
		try {
			List<RuleAction> actions = (List<RuleAction>) this.cache.get(key,
					ACTIONS_GROUP);
			if (actions == null) {
				actions = new ArrayList<RuleAction>();
			}
			if (action != null && UtilMethods.isSet(action.getId())) {
				actions.add(action);
				this.cache.put(key, actions, ACTIONS_GROUP);
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "ActionsCache entry not found: " + key);
		}
		return action;
	}

	@Override
	public RuleAction getAction(Rule rule, RuleAction action) {
		String key = rule.getId();
		RuleAction cachedAction = null;
		try {
			List<RuleAction> actions = (List<RuleAction>) this.cache.get(key,
					ACTIONS_GROUP);
			for (RuleAction act : actions) {
				if (act.getId().equals(action.getId())) {
					cachedAction = act;
					break;
				}
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "ActionsCache entry not found: " + key);
		}
		return cachedAction;
	}

	@Override
	public void removeAction(Rule rule, RuleAction action) {
		String key = rule.getId();
		try {
			int index = -1;
			List<RuleAction> actions = (List<RuleAction>) this.cache.get(key,
					ACTIONS_GROUP);
			for (int i = 0; i < actions.size(); i++) {
				RuleAction act = actions.get(i);
				if (act.getId().equals(action.getId())) {
					index = i;
					break;
				}
			}
			if (index >= 0) {
				actions.remove(index);
				this.cache.put(key, actions, ACTIONS_GROUP);
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "ActionsCache entry not found: " + key);
		}
	}

	@Override
	protected List<RuleAction> addActions(Rule rule, List<RuleAction> actions) {
		String key = rule.getId();
		if (actions != null && UtilMethods.isSet(key)) {
			this.cache.put(key, actions, ACTIONS_GROUP);
		}
		return actions;
	}

	@Override
	public List<RuleAction> getActions(Rule rule) {
		String key = rule.getId();
		List<RuleAction> actions = null;
		if (UtilMethods.isSet(key)) {
			try {
				actions = (List<RuleAction>) this.cache.get(key, ACTIONS_GROUP);
			} catch (DotCacheException e) {
				Logger.debug(this, "ActionsCache entry not found: " + key);
			}
		}
		return actions;
	}

	@Override
	public void removeActions(Rule rule) {
		String key = rule.getId();
		if (UtilMethods.isSet(key)) {
			this.cache.remove(key, ACTIONS_GROUP);
		}
	}

}
