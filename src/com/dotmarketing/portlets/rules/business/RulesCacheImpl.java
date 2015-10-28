package com.dotmarketing.portlets.rules.business;

import java.util.*;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.apache.commons.collections.IteratorUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.portlets.rules.model.*;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotcms.rest.validation.Preconditions;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull;

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
	public List<String> getRulesByHost(Host host) {
		host = checkNotNull(host, "Host is required");
		String hostInode = checkNotNull(host.getInode(), "Host Id is required");
        try {
            return (List<String>) cache.get(HOST_RULES_CACHE_GROUP + hostInode, HOST_RULES_CACHE_GROUP);
        } catch (DotCacheException e) {
            Logger.debug(RulesCacheImpl.class,e.getMessage(),e);
            return null;
        }
	}

    @Override
    public void putRulesByHost(Host host, List<Rule> rules) {
        host = checkNotNull(host, "Host Id is required");
        rules = checkNotNull(rules, "Rule List is required");

        List<String> rulesIds = new ArrayList<>();

        for(Rule rule: rules) {
            rulesIds.add(rule.getId());
            addRule(rule);
        }

        String hostInode = checkNotNull(host.getInode(), "Host Id is required");

        cache.put(HOST_RULES_CACHE_GROUP +hostInode, rulesIds, HOST_RULES_CACHE_GROUP);
    }

	@Override
	public void removeRule(Rule rule) {
		if (rule != null && UtilMethods.isSet(rule.getId())) {
			this.cache.remove(rule.getId(), getPrimaryGroup());
            Set<Rule> rules = getRules(rule.getHost(), rule.getFireOn());
            if(rules!=null) rules.remove(rule);
            Host host =
            List<String> rulesIds = getRulesByHost(rule.getHost());
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
		// TODO - reimplement
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
		return conditions!=null?ImmutableList.copyOf(conditions):null;
	}

	@Override
	public void removeConditions(String conditionGroupId) {
		if (UtilMethods.isSet(conditionGroupId)) {
			this.cache.remove(conditionGroupId, RULE_CONDITIONS_GROUP);
		}
	}

	@Override
	public void removeConditionsByRuleId(String ruleId) {
		// TODO - reimplement
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
		// TODO - reimplement
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
		return conditionGroups!=null?ImmutableList.copyOf(conditionGroups):null;
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
		// TODO - reimplement
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
		return actions!=null?ImmutableList.copyOf(actions):null;
	}

	@Override
	public void removeActions(Rule rule) {
		String key = rule.getId();
		if (UtilMethods.isSet(key)) {
			this.cache.remove(key, RULE_ACTIONS_CACHE);
		}
	}

}
