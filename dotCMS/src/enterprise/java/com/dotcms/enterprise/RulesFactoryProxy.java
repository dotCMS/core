/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.rules.RulesFactory;
import com.dotcms.enterprise.rules.RulesFactoryImpl;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.Rule.FireOn;
import com.dotmarketing.portlets.rules.model.RuleAction;

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
public class RulesFactoryProxy extends ParentProxy implements RulesFactory {

	private RulesFactory rulesFactoryImpl = null;

	/**
	 * Returns a single instance of the {@link RulesFactoryImpl} class.
	 * 
	 * @return A single instance of the Rules Factory.
	 */
	private RulesFactory getRulesFactoryInstance() {
		if (this.rulesFactoryImpl == null) {
			this.rulesFactoryImpl = new RulesFactoryImpl();
		}
		return this.rulesFactoryImpl;
	}

	@Override
	protected int[] getAllowedVersions() {
		return new int[] { LicenseLevel.STANDARD.level, LicenseLevel.PROFESSIONAL.level,
				LicenseLevel.PRIME.level,LicenseLevel.PLATFORM.level };
	}

	@Override
	public List<Rule> getEnabledRulesByParent(Ruleable parent) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesFactoryInstance().getEnabledRulesByParent(parent);
	}

	@Override
	public List<Rule> getAllRulesByParent(Ruleable parent) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesFactoryInstance().getAllRulesByParent(parent);
	}

	@Override
	public List<Rule> getAllRules() throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesFactoryInstance().getAllRules();
	}

	@Override
	public Set<Rule> getRulesByParent(String parent, FireOn fireOn) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesFactoryInstance().getRulesByParent(parent, fireOn);
	}

	@Override
	public List<Rule> getRulesByNameFilter(String nameFilter) {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesFactoryInstance().getRulesByNameFilter(nameFilter);
	}

	@Override
	public Rule getRuleById(String id) throws DotDataException {
		// We don't need a license level here. We just need to be able to call
		// this method on a community license to not break the rules import.
		return getRulesFactoryInstance().getRuleById(id);
	}

    @Override
    public Rule getRuleByIdFromDB(final String id) throws DotDataException {
        // We don't need a license level here. We just need to be able to call
        // this method on a community license to not break the rules import.
	    return getRulesFactoryInstance().getRuleByIdFromDB(id);
    }

    @Override
	public List<RuleAction> getRuleActionsByRule(String ruleId) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesFactoryInstance().getRuleActionsByRule(ruleId);
	}

    @Override
    public List<RuleAction> getRuleActionsByRuleFromDB(final String ruleId) throws DotDataException {
        if (!allowExecution()) {
            throw new InvalidLicenseException("Enterprise License required");
        }
        return getRulesFactoryInstance().getRuleActionsByRuleFromDB(ruleId);
    }

    @Override
	public RuleAction getRuleActionById(String ruleActionId) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesFactoryInstance().getRuleActionById(ruleActionId);
	}

	@Override
	public ParameterModel getRuleActionParameterById(String id) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesFactoryInstance().getRuleActionParameterById(id);
	}

	@Override
	public List<ConditionGroup> getConditionGroupsByRule(String ruleId) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesFactoryInstance().getConditionGroupsByRule(ruleId);
	}

    @Override
    public List<ConditionGroup> getConditionGroupsByRuleFromDB(final String ruleId) throws DotDataException {
        if (!allowExecution()) {
            throw new InvalidLicenseException("Enterprise License required");
        }
        return getRulesFactoryInstance().getConditionGroupsByRuleFromDB(ruleId);
    }

    @Override
	public ConditionGroup getConditionGroupById(String conditionGroupId) throws DotDataException {
		// We don't need a license level here. We just need to be able to call
		// this method on a community license to not break the rules import.
		return getRulesFactoryInstance().getConditionGroupById(conditionGroupId);
	}

	@Override
	public List<Condition> getConditionsByGroup(String groupId) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesFactoryInstance().getConditionsByGroup(groupId);
	}

	@Override
	public Condition getConditionById(String id) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesFactoryInstance().getConditionById(id);
	}

	@Override
	public ParameterModel getConditionValueById(String id) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesFactoryInstance().getConditionValueById(id);
	}

	@Override
	public void saveRule(Rule rule) throws DotDataException {
		// We don't need a license level here. We just need to be able to call
		// this method on a community license to not break the rules import.
		getRulesFactoryInstance().saveRule(rule);
	}

	@Override
	public void saveConditionGroup(ConditionGroup group) throws DotDataException {
		// We don't need a license level here. We just need to be able to call
		// this method on a community license to not break the rules import.
		getRulesFactoryInstance().saveConditionGroup(group);
	}

	@Override
	public void saveCondition(Condition condition) throws DotDataException {
		// We don't need a license level here. We just need to be able to call
		// this method on a community license to not break the rules import.
		getRulesFactoryInstance().saveCondition(condition);
	}

	@Override
	public void saveConditionValue(ParameterModel parameterModel) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesFactoryInstance().saveConditionValue(parameterModel);
	}

	@Override
	public void saveRuleAction(RuleAction ruleAction) throws DotDataException {
		// We don't need a license level here. We just need to be able to call
		// this method on a community license to not break the rules import.
		getRulesFactoryInstance().saveRuleAction(ruleAction);
	}

	@Override
	public void deleteRule(Rule rule) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesFactoryInstance().deleteRule(rule);
	}

	@Override
	public void deleteConditionGroup(ConditionGroup conditionGroup) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesFactoryInstance().deleteConditionGroup(conditionGroup);
	}

	@Override
	public void deleteConditionsByGroup(ConditionGroup conditionGroup) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesFactoryInstance().deleteConditionsByGroup(conditionGroup);
	}

	@Override
	public void deleteCondition(Condition condition) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesFactoryInstance().deleteCondition(condition);
	}

	@Override
	public void deleteConditionValue(ParameterModel parameterModel) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesFactoryInstance().deleteConditionValue(parameterModel);
	}

	@Override
	public void deleteRuleAction(RuleAction ruleAction) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesFactoryInstance().deleteRuleAction(ruleAction);
	}

	@Override
	public void deleteRuleActionsByRule(Rule rule) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesFactoryInstance().deleteRuleActionsByRule(rule);
	}

	@Override
	public void deleteRuleActionsParameters(RuleAction action) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesFactoryInstance().deleteRuleActionsParameters(action);
	}

	@Override
	public void deleteConditionValues(Condition condition) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		getRulesFactoryInstance().deleteConditionValues(condition);
	}

	@Override
	public Map<String, ParameterModel> getRuleActionParameters(RuleAction action) throws DotDataException {
		if (!allowExecution()) {
			throw new InvalidLicenseException("Enterprise License required");
		}
		return getRulesFactoryInstance().getRuleActionParameters(action);
	}

}
