/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.rules;


import com.dotmarketing.business.Ruleable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.rules.model.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RulesFactory {

    List<Rule> getEnabledRulesByParent(Ruleable parent) throws DotDataException;

    List<Rule> getAllRulesByParent(Ruleable parent) throws DotDataException;

    List<Rule> getAllRules() throws DotDataException;

    Set<Rule> getRulesByParent(String parent, Rule.FireOn fireOn) throws DotDataException;

    List<Rule> getRulesByNameFilter(String nameFilter);

    Rule getRuleById(String id) throws DotDataException;

    /**
     * This method is intended to be used only on logic that involves delete operations on rules
     * @param id
     * @return
     * @throws DotDataException
     */
    Rule getRuleByIdFromDB(String id) throws DotDataException;

    List<RuleAction> getRuleActionsByRule(String ruleId) throws DotDataException;

    /**
     * This method is intended to be used only on logic that involves delete operations on rules
     * @param ruleId
     * @return
     * @throws DotDataException
     */
    List<RuleAction> getRuleActionsByRuleFromDB(String ruleId) throws DotDataException;

    RuleAction getRuleActionById(String ruleActionId) throws DotDataException;

    ParameterModel getRuleActionParameterById(String id) throws DotDataException;

    List<ConditionGroup> getConditionGroupsByRule(String ruleId) throws DotDataException;

    /**
     * This method is intended to be used only on logic that involves delete operations on rules
     * @param ruleId
     * @return
     * @throws DotDataException
     */
    List<ConditionGroup> getConditionGroupsByRuleFromDB(String ruleId)
            throws DotDataException;

    ConditionGroup getConditionGroupById(String conditionGroupId) throws DotDataException;

    List<Condition> getConditionsByGroup(String groupId) throws DotDataException;

    Condition getConditionById(String id) throws DotDataException ;

    ParameterModel getConditionValueById(String id) throws DotDataException;

    void saveRule(Rule rule) throws DotDataException;

    void saveConditionGroup(ConditionGroup condition) throws DotDataException;

    void saveCondition(Condition condition) throws DotDataException;

    void saveConditionValue(ParameterModel parameterModel) throws DotDataException;

    void saveRuleAction(RuleAction ruleAction) throws DotDataException;

    void deleteRule(Rule rule) throws DotDataException;

    void deleteConditionGroup(ConditionGroup conditionGroup) throws DotDataException;

    void deleteConditionsByGroup(ConditionGroup conditionGroup) throws DotDataException;

    void deleteCondition(Condition condition) throws DotDataException;

    void deleteConditionValue(ParameterModel parameterModel) throws DotDataException;

    void deleteRuleAction(RuleAction ruleAction) throws DotDataException;

    void deleteRuleActionsByRule(Rule rule) throws DotDataException;

    void deleteRuleActionsParameters(RuleAction action) throws DotDataException;

    void deleteConditionValues(Condition condition) throws DotDataException;

    Map<String, ParameterModel> getRuleActionParameters(RuleAction action) throws DotDataException;

}
