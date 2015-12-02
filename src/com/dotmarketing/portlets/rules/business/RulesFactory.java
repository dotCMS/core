package com.dotmarketing.portlets.rules.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.model.*;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RulesFactory {

    List<Rule> getEnabledRulesByHost(Host host) throws DotDataException;

    List<Rule> getAllRulesByHost(Host host) throws DotDataException;

    Set<Rule> getRulesByHost(String host, Rule.FireOn fireOn) throws DotDataException;

    List<Rule> getRulesByNameFilter(String nameFilter);

    Rule getRuleById(String id) throws DotDataException;

    List<RuleAction> getRuleActionsByRule(String ruleId) throws DotDataException;

    RuleAction getRuleActionById(String ruleActionId) throws DotDataException;

    RuleActionParameter getRuleActionParameterById(String id) throws DotDataException;

    List<ConditionGroup> getConditionGroupsByRule(String ruleId) throws DotDataException;

    ConditionGroup getConditionGroupById(String conditionGroupId) throws DotDataException;

    List<Condition> getConditionsByGroup(String groupId) throws DotDataException;

    Condition getConditionById(String id) throws DotDataException ;

    ConditionValue getConditionValueById(String id) throws DotDataException;

    void saveRule(Rule rule) throws DotDataException;

    void saveConditionGroup(ConditionGroup condition) throws DotDataException;

    void saveCondition(Condition condition) throws DotDataException;

    void saveConditionValue(ConditionValue conditionValue) throws DotDataException;

    void saveRuleAction(RuleAction ruleAction) throws DotDataException;

    void deleteRule(Rule rule) throws DotDataException;

    void deleteConditionGroup(ConditionGroup conditionGroup) throws DotDataException;

    void deleteConditionsByGroup(ConditionGroup conditionGroup) throws DotDataException;

    void deleteCondition(Condition condition) throws DotDataException;

    void deleteConditionValue(ConditionValue conditionValue) throws DotDataException;

    void deleteRuleAction(RuleAction ruleAction) throws DotDataException;

    void deleteRuleActionsByRule(Rule rule) throws DotDataException;

    void deleteRuleActionsParameters(RuleAction action) throws DotDataException;

    void deleteConditionValues(Condition condition) throws DotDataException;

    Map<String, RuleActionParameter> getRuleActionParameters(RuleAction action) throws DotDataException;

}
