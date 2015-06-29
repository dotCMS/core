package com.dotmarketing.portlets.rules.business;

import java.util.*;

import com.dotcms.repackage.org.apache.commons.collections.IteratorUtils;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.rules.model.*;
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
	public List<Rule> addRules(List<Rule> rules) {
		if (rules != null && rules.size() > 0) {
			for (Rule rule : rules) {
				addRule(rule);
			}
		}
		return rules;
	}

    @Override
    public Set<Rule> addRules(Set<Rule> rules, String hostId, Rule.FireOn fireOn) {
        if(!UtilMethods.isSet(hostId) || rules==null || fireOn==null)return null;
        cache.put(hostId + ":" + fireOn, rules, getPrimaryGroup());
        return rules;
    }

    @Override
    public Set<Rule> getRules(String hostId, Rule.FireOn fireOn) {
        if(!UtilMethods.isSet(hostId) || fireOn==null) return null;
        try {
            return (Set<Rule>) cache.get(hostId + ":" + fireOn, PRIMARY_GROUP);
        } catch (DotCacheException e) {
            Logger.debug(RulesCacheImpl.class,e.getMessage(),e);
        }
        return null;
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
	public List<Rule> getRulesByHostId(String hostId) {
		Set<String> ruleIds = this.cache.getKeys(getPrimaryGroup());
		List<Rule> rules = null;
		for (String ruleId : ruleIds) {
			if(ruleId.indexOf(':')>-1) continue;
            Rule rule = getRule(ruleId);
			if (rule.getHost().equals(hostId)) {
				if (rules == null) {
					rules = new ArrayList<Rule>();
				}
				rules.add(rule);
			}
		}
		return rules;
	}

	@Override
	public List<Rule> getRulesByFolderId(String folderId) {
		Set<String> ruleIds = this.cache.getKeys(getPrimaryGroup());
		List<Rule> rules = null;
		for (String ruleId : ruleIds) {
            if(ruleId.indexOf(':')>-1) continue;
			Rule rule = getRule(ruleId);
			if (rule.getFolder().equals(folderId)) {
				if (rules == null) {
					rules = new ArrayList<Rule>();
				}
				rules.add(rule);
			}
		}
		return rules;
	}

	@Override
	public void removeRule(Rule rule) {
		if (rule != null && UtilMethods.isSet(rule.getId())) {
			this.cache.remove(rule.getId(), getPrimaryGroup());
            Set<Rule> rules = getRules(rule.getHost(), rule.getFireOn());
            if(rules!=null) rules.remove(rule);
		}
	}

	@Override
	protected Condition addCondition(String conditionGroupId,
			Condition condition) {
		try {
			List<Condition> conditions = (List<Condition>) this.cache.get(
					conditionGroupId, RULE_CONDITIONS_GROUP);
			if (conditions == null) {
				conditions = new ArrayList<Condition>();
			}
			if (condition != null && UtilMethods.isSet(condition.getId())) {
				conditions.add(condition);
				this.cache.put(conditionGroupId, conditions, RULE_CONDITIONS_GROUP);
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "ConditionsCache entry not found: "
					+ conditionGroupId);
		}
		return condition;
	}

	@Override
	public Condition getCondition(String conditionGroupId, Condition condition) {
		try {
			List<Condition> conditions = (List<Condition>) this.cache.get(
					conditionGroupId, RULE_CONDITIONS_GROUP);
			for (Condition cond : conditions) {
				if (cond.getId().equals(condition.getId())) {
					return cond;
				}
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "ConditionsCache entry not found: "
					+ conditionGroupId);
		}
		return null;
	}

	@Override
	public Condition getCondition(String conditionId) {
		Set<String> conditionGroups = this.cache.getKeys(RULE_CONDITIONS_GROUP);
		for (String conditionGroup : conditionGroups) {
			List<Condition> conditionList = getConditionsByGroupId(conditionGroup);
			for (Condition condition : conditionList) {
				if (condition.getId().equals(conditionId)) {
					return condition;
				}
			}
		}
		return null;
	}

	@Override
	public void removeCondition(String conditionGroupId, Condition condition) {
		try {
			int index = -1;
			Iterator<Condition> conditions = ((List<Condition>) this.cache.get(
					conditionGroupId, RULE_CONDITIONS_GROUP)).iterator();
			for (int i = 0; conditions.hasNext(); i++) {
				Condition cond = conditions.next();
				if (cond.getId().equals(condition.getId())) {
					conditions.remove();
					break;
				}
			}
            this.cache.put(conditionGroupId, IteratorUtils.toList(conditions), RULE_CONDITIONS_GROUP);

		} catch (DotCacheException e) {
			Logger.debug(this, "ConditionsCache entry not found: "
					+ conditionGroupId);
		}
	}

	@Override
	protected List<Condition> addConditions(String conditionGroupId,
			List<Condition> conditions) {
		if (conditions != null && conditions.size() > 0
				&& UtilMethods.isSet(conditionGroupId)) {
			this.cache.put(conditionGroupId, conditions, RULE_CONDITIONS_GROUP);
		}
		return conditions;
	}

	@Override
	public List<Condition> getConditionsByGroupId(String conditionGroupId) {
		List<Condition> conditions = null;
		if (UtilMethods.isSet(conditionGroupId)) {
			try {
				conditions = (List<Condition>) this.cache.get(conditionGroupId,
                        RULE_CONDITIONS_GROUP);
			} catch (DotCacheException e) {
				Logger.debug(this, "ConditionsCache entry not found: "
						+ conditionGroupId);
			}
		}
		return conditions;
	}

	@Override
	public List<Condition> getConditions(String ruleId) {
		List<Condition> conditions = null;
		if (UtilMethods.isSet(ruleId)) {
			List<ConditionGroup> conditionGroups = getConditionGroups(ruleId);
			if (conditionGroups != null & conditionGroups.size() > 0) {
				conditions = new ArrayList<Condition>();
				for (ConditionGroup conditionGroup : conditionGroups) {
					conditions.addAll(getConditionsByGroupId(conditionGroup
							.getId()));
				}
			}
		}
		return conditions;
	}

	@Override
	public void removeConditions(String conditionGroupId) {
		if (UtilMethods.isSet(conditionGroupId)) {
			this.cache.remove(conditionGroupId, RULE_CONDITIONS_GROUP);
		}
	}

	@Override
	public void removeConditionsByRuleId(String ruleId) {
		Set<String> conditionGroups = this.cache.getKeys(RULE_CONDITIONS_GROUP);
		for (String condGroup : conditionGroups) {
			ConditionGroup conditionGroup = getConditionGroup(condGroup);
			if (conditionGroup.getRuleId().equals(ruleId)) {
				removeConditions(condGroup);
			}
		}
	}

	@Override
	protected ConditionGroup addConditionGroup(String ruleId,
			ConditionGroup conditionGroup) {
		if (!UtilMethods.isSet(ruleId)) {
			return null;
		}
		try {
			List<ConditionGroup> conditionGroups = (List<ConditionGroup>) this.cache
					.get(ruleId, RULE_CONDITION_GROUPS_CACHE);
			if (conditionGroups == null) {
				conditionGroups = new ArrayList<ConditionGroup>();
			}
			if (conditionGroup != null
					&& UtilMethods.isSet(conditionGroup.getId())) {
				conditionGroups.add(conditionGroup);
				this.cache.put(ruleId, conditionGroups, RULE_CONDITION_GROUPS_CACHE);
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "ConditionGroupsCache entry not found: "
					+ ruleId);
		}
		return conditionGroup;
	}

	@Override
	public ConditionGroup getConditionGroup(String ruleId,
			ConditionGroup conditionGroup) {
		ConditionGroup cachedConditionGroup = null;
		try {
			List<ConditionGroup> conditionGroups = (List<ConditionGroup>) this.cache
					.get(ruleId, RULE_CONDITION_GROUPS_CACHE);
			for (ConditionGroup condGroup : conditionGroups) {
				if (condGroup.getId().equals(conditionGroup.getId())) {
					cachedConditionGroup = condGroup;
					break;
				}
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "ConditionGroupsCache entry not found: "
					+ ruleId);
		}
		return cachedConditionGroup;
	}

	@Override
	public ConditionGroup getConditionGroup(String conditionGroupId) {
		Set<String> ruleIds = this.cache.getKeys(RULE_CONDITION_GROUPS_CACHE);
		for (String ruleId : ruleIds) {
			List<ConditionGroup> conditionGroups = getConditionGroups(ruleId);
			for (ConditionGroup cachedCondGroup : conditionGroups) {
				if (cachedCondGroup.getId().equals(conditionGroupId)) {
					return cachedCondGroup;
				}
			}
		}
		return null;
	}

	@Override
	public void removeConditionGroup(String ruleId,
			ConditionGroup conditionGroup) {
		try {
			int index = -1;
			Iterator<ConditionGroup> conditionGroups = ((List<ConditionGroup>) this.cache
					.get(ruleId, RULE_CONDITION_GROUPS_CACHE)).iterator();
			for (int i = 0; conditionGroups.hasNext(); i++) {
				ConditionGroup condGroup = conditionGroups.next();
				if (condGroup.getId().equals(conditionGroup.getId())) {
					conditionGroups.remove();
					break;
				}
			}
            this.cache.put(ruleId, IteratorUtils.toList(conditionGroups), RULE_CONDITION_GROUPS_CACHE);
		} catch (DotCacheException e) {
			Logger.debug(this, "ConditionGroupsCache entry not found: "
					+ ruleId);
		}
	}

	@Override
	protected List<ConditionGroup> addConditionGroups(String ruleId,
			List<ConditionGroup> conditionGroups) {
		if (conditionGroups != null && UtilMethods.isSet(ruleId)) {
			this.cache.put(ruleId, conditionGroups, RULE_CONDITION_GROUPS_CACHE);
		}
		return conditionGroups;
	}

	@Override
	public List<ConditionGroup> getConditionGroups(String ruleId) {
		List<ConditionGroup> conditionGroups = null;
		if (UtilMethods.isSet(ruleId)) {
			try {
				conditionGroups = (List<ConditionGroup>) this.cache.get(ruleId,
                        RULE_CONDITION_GROUPS_CACHE);
			} catch (DotCacheException e) {
				Logger.debug(this, "ConditionGroupsCache entry not found: "
						+ ruleId);
			}
		}
		return conditionGroups;
	}

	@Override
	public void removeConditionGroups(Rule rule) {
		String key = rule.getId();
		if (UtilMethods.isSet(key)) {
			this.cache.remove(key, RULE_CONDITION_GROUPS_CACHE);
		}
	}

	@Override
	protected RuleAction addAction(String ruleId, RuleAction action) {
		try {
			List<RuleAction> actions = (List<RuleAction>) this.cache.get(
					ruleId, RULE_ACTIONS_CACHE);
			if (actions == null) {
				actions = new ArrayList<RuleAction>();
			}
			if (action != null && UtilMethods.isSet(action.getId())) {
				actions.add(action);
				this.cache.put(ruleId, actions, RULE_ACTIONS_CACHE);
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "ActionsCache entry not found: " + ruleId);
		}
		return action;
	}

	@Override
	public RuleAction getAction(String ruleId, String actionId) {
		RuleAction cachedAction = null;
		try {
			List<RuleAction> actions = (List<RuleAction>) this.cache.get(
					ruleId, RULE_ACTIONS_CACHE);
			for (RuleAction act : actions) {
				if (act.getId().equals(actionId)) {
					cachedAction = act;
					break;
				}
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "ActionsCache entry not found: " + ruleId);
		}
		return cachedAction;
	}

	@Override
	public RuleAction getAction(String actionId) {
		Set<String> ruleIds = this.cache.getKeys(RULE_ACTIONS_CACHE);
		for (String ruleId : ruleIds) {
			List<RuleAction> actionList = getActions(ruleId);
			for (RuleAction cachedAction : actionList) {
				if (cachedAction.getId().equals(actionId)) {
					return cachedAction;
				}
			}
		}
		return null;
	}

	@Override
	public void removeAction(String ruleId, RuleAction action) {
		try {
			int index = -1;
			List<RuleAction> actions = (List<RuleAction>) this.cache.get(
					ruleId, RULE_ACTIONS_CACHE);
			for (int i = 0; i < actions.size(); i++) {
				RuleAction act = actions.get(i);
				if (act.getId().equals(action.getId())) {
					index = i;
					break;
				}
			}
			if (index >= 0) {
				actions.remove(index);
				this.cache.put(ruleId, actions, RULE_ACTIONS_CACHE);
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "ActionsCache entry not found: " + ruleId);
		}
	}

	@Override
	protected List<RuleAction> addActions(String ruleId,
			List<RuleAction> actions) {
		if (actions != null && UtilMethods.isSet(ruleId)) {
			this.cache.put(ruleId, actions, RULE_ACTIONS_CACHE);
		}
		return actions;
	}

	@Override
	public List<RuleAction> getActions(String ruleId) {
		List<RuleAction> actions = null;
		if (UtilMethods.isSet(ruleId)) {
			try {
				actions = (List<RuleAction>) this.cache.get(ruleId,
                        RULE_ACTIONS_CACHE);
			} catch (DotCacheException e) {
				Logger.debug(this, "ActionsCache entry not found: " + ruleId);
			}
		}
		return actions;
	}

	@Override
	public void removeActions(Rule rule) {
		String key = rule.getId();
		if (UtilMethods.isSet(key)) {
			this.cache.remove(key, RULE_ACTIONS_CACHE);
		}
	}

}
