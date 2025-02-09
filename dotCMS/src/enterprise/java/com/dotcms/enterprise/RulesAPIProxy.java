/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.enterprise.rules.RulesAPIImpl;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.Rule.FireOn;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * Provides the developer a single entry point to interact with the dotCMS Rules
 * feature. This Proxy class will hide any implementation details related to our
 * Rules API.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since Mar 3, 2016
 *
 */
public class RulesAPIProxy extends ParentProxy implements RulesAPI {

	private RulesAPI rulesAPIInstance = null;
	
	/**
	 * Returns a single instance of the {@link RulesAPIImpl} class.
	 * 
	 * @return A single instance of the Rules API.
	 */
	private RulesAPI getRulesAPIInstance() {
		if (this.rulesAPIInstance == null) {
			this.rulesAPIInstance = new RulesAPIImpl();
		}
		return this.rulesAPIInstance;
	}
	
	@Override
	protected int[] getAllowedVersions() {
		return new int[] { LicenseLevel.STANDARD.level, LicenseLevel.PROFESSIONAL.level,
				LicenseLevel.PRIME.level, LicenseLevel.PLATFORM.level };
	}

	@Override
	public String addConditionlet(Class conditionletClass) {
		return getRulesAPIInstance().addConditionlet(conditionletClass);
	}

	@Override
	public void removeConditionlet(String conditionletName) {
		getRulesAPIInstance().removeConditionlet(conditionletName);
	}

	@Override
	public String addRuleActionlet(Class actionletClass) {
		return getRulesAPIInstance().addRuleActionlet(actionletClass);
	}

	@Override
	public void removeRuleActionlet(String actionletName) {
		getRulesAPIInstance().removeRuleActionlet(actionletName);
	}

	@Override
	public List<Rule> getEnabledRulesByParent(Ruleable parent, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesAPIInstance().getEnabledRulesByParent(parent, user, respectFrontendRoles);
	}

	@Override
	public List<Rule> getAllRules(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			// In this case, we want to return an empty list if there is no
			// License. We don't want to break the Export functionality in the
			// community edition. Just don't include the rules.
			Logger.warn(this, "An Enterprise License is required to get the list all the rules.");
			return Lists.newArrayList();
		}
		return getRulesAPIInstance().getAllRules(user, respectFrontendRoles);
	}

	@Override
	public List<Rule> getAllRulesByParent(Ruleable parent, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesAPIInstance().getAllRulesByParent(parent, user, respectFrontendRoles);
	}

	@Override
	public List<Rule> getAllRulesByParent(final String parentId, final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		return getRulesAPIInstance().getAllRulesByParent(parentId, user, respectFrontendRoles);
	}

	@Override
	public List<Rule> getAllRulesByParent(final String parentId, final User user)
			throws DotDataException, DotSecurityException {
		return getRulesAPIInstance().getAllRulesByParent(parentId, user);
	}

	@Override
	public Set<Rule> getRulesByParentFireOn(String parent, User user, boolean respectFrontendRoles, FireOn fireOn)
			throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesAPIInstance().getRulesByParentFireOn(parent, user, respectFrontendRoles, fireOn);
	}

	@Override
	public List<Rule> getRulesByNameFilter(String nameFilter, User user, boolean respectFrontendRoles) {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesAPIInstance().getRulesByNameFilter(nameFilter, user, respectFrontendRoles);
	}

	@Override
	public Rule getRuleById(String id, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesAPIInstance().getRuleById(id, user, respectFrontendRoles);
	}

	@Override
	public void deleteRulesByParent(Ruleable parent, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesAPIInstance().deleteRulesByParent(parent, user, respectFrontendRoles);
	}

	@Override
	public void copyRulesByParent(Ruleable parent, Ruleable newParent, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesAPIInstance().copyRulesByParent(parent, newParent, user, respectFrontendRoles);
	}

	@Override
	public void deleteRule(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesAPIInstance().deleteRule(rule, user, respectFrontendRoles);
	}

	@Override
	public List<ConditionGroup> getConditionGroupsByRule(String ruleId, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesAPIInstance().getConditionGroupsByRule(ruleId, user, respectFrontendRoles);
	}

	@Override
	public ConditionGroup getConditionGroupById(String conditionGroupId, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		// We don't need a license here. Just need to be able to call
		// getRuleById on a community license and not break the rules import.
		return getRulesAPIInstance().getConditionGroupById(conditionGroupId, user, respectFrontendRoles);
	}

	@Override
	public List<RuleAction> getRuleActionsByRule(String ruleId, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesAPIInstance().getRuleActionsByRule(ruleId, user, respectFrontendRoles);
	}

	@Override
	public RuleAction getRuleActionById(String ruleActionId, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesAPIInstance().getRuleActionById(ruleActionId, user, respectFrontendRoles);
	}

	@Override
	public List<Condition> getConditionsByConditionGroup(String conditionGroupId, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesAPIInstance().getConditionsByConditionGroup(conditionGroupId, user, respectFrontendRoles);
	}

	@Override
	public Condition getConditionById(String id, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesAPIInstance().getConditionById(id, user, respectFrontendRoles);
	}

	@Override
	public ParameterModel getConditionValueById(String id, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesAPIInstance().getConditionValueById(id, user, respectFrontendRoles);
	}

	@Override
	public void saveRule(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		// We don't want to check the license level here. We need to be able to
		// create a Rule on a community license to not break the rules import.
		getRulesAPIInstance().saveRule(rule, user, respectFrontendRoles);
	}

	@Override
	public void saveRuleNoParentCheck(Rule rule, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		getRulesAPIInstance().saveRuleNoParentCheck(rule, user, respectFrontendRoles);
	}

	@Override
	public void saveCondition(Condition condition, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		// We don't want to check the license level here. We need to be able to
		// create a Condition on a community license to not break the rules
		// import.
		getRulesAPIInstance().saveCondition(condition, user, respectFrontendRoles);
	}

	@Override
	public void saveConditionValue(ParameterModel parameterModel, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesAPIInstance().saveConditionValue(parameterModel, user, respectFrontendRoles);
	}

	@Override
	public void saveConditionGroup(ConditionGroup conditionGroup, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		// We don't want to check the license level here. We need to be able to
		// create a Condition Group on a community license to not break the
		// rules import.
		getRulesAPIInstance().saveConditionGroup(conditionGroup, user, respectFrontendRoles);
	}

	@Override
	public void saveRuleAction(RuleAction ruleAction, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		// We don't want to check the license level here. We need to be able to
		// create a Rule Action on a community license to not break the rules
		// import.
		getRulesAPIInstance().saveRuleAction(ruleAction, user, respectFrontendRoles);
	}

	@Override
	public void deleteCondition(Condition condition, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesAPIInstance().deleteCondition(condition, user, respectFrontendRoles);
	}

	@Override
	public void deleteConditions(ConditionGroup group, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesAPIInstance().deleteConditions(group, user, respectFrontendRoles);
	}

	@Override
	public void deleteConditionValue(ParameterModel parameterModel, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesAPIInstance().deleteConditionValue(parameterModel, user, respectFrontendRoles);
	}

	@Override
	public void deleteConditionValues(Condition condition, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesAPIInstance().deleteConditionValues(condition, user, respectFrontendRoles);
	}

	@Override
	public void deleteConditionGroup(ConditionGroup conditionGroup, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesAPIInstance().deleteConditionGroup(conditionGroup, user, respectFrontendRoles);
	}

	@Override
	public void deleteConditionGroupsByRule(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesAPIInstance().deleteConditionGroupsByRule(rule, user, respectFrontendRoles);
	}

	@Override
	public void deleteRuleAction(RuleAction ruleAction, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesAPIInstance().deleteRuleAction(ruleAction, user, respectFrontendRoles);
	}

	@Override
	public Map<String, ParameterModel> getRuleActionParameters(RuleAction action, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesAPIInstance().getRuleActionParameters(action, user, respectFrontendRoles);
	}

	@Override
	public ParameterModel getRuleActionParameterById(String id, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesAPIInstance().getRuleActionParameterById(id, user, respectFrontendRoles);
	}

	@Override
	public List<Conditionlet<?>> findConditionlets() throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesAPIInstance().findConditionlets();
	}

	@Override
	public Conditionlet findConditionlet(String clazz) {
		// We don't want to check the license level here. We just need to be
		// able to retrieve Contentlets on a community license to not break the
		// rules import.
		return getRulesAPIInstance().findConditionlet(clazz);
	}

	@Override
	public List<RuleActionlet> findActionlets() throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesAPIInstance().findActionlets();
	}

	@Override
	public RuleActionlet findActionlet(String clazz) {
		// We don't want to check the license level here. We just need to be
		// able to retrieve Actionlets on a community license to not break the
		// rules import.
		return getRulesAPIInstance().findActionlet(clazz);
	}

	@Override
	public void registerBundleService() {
		getRulesAPIInstance().registerBundleService();
	}

	@Override
	public void deleteRuleActionsByRule(Rule rule, User user) throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesAPIInstance().deleteRuleActionsByRule(rule, user);
	}

	@Override
	public void disableRule(Rule rule, User user) throws DotDataException, DotSecurityException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesAPIInstance().disableRule(rule, user);
	}

}
