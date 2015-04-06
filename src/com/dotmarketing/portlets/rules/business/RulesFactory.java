package com.dotmarketing.portlets.rules.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;

import java.util.List;

public interface RulesFactory {

    List<Rule> getRulesByHost(String host) throws DotDataException;

    List<Rule> getRulesByFolder(String folder) throws DotDataException;

    List<Rule> getRulesByNameFilter(String nameFilter);

    Rule getRuleById(String id) throws DotDataException;

    List<RuleAction> getRuleActionsByRule(String ruleId) throws DotDataException;

    RuleAction getRuleActionById(String ruleActionId) throws DotDataException;

    List<ConditionGroup> getConditionGroupsByRule(String ruleId) throws DotDataException;

    ConditionGroup getConditionGroupById(String conditionGroupId) throws DotDataException;

    List<Condition> getConditionsByRule(String ruleId) throws DotDataException;

    List<Condition> getConditionsByGroup(String groupId) throws DotDataException;

    Condition getConditionById(String id) throws DotDataException ;

    void saveRule(Rule rule) throws DotDataException;

    void saveCondition(Condition condition) throws DotDataException;

    void saveRuleAction(RuleAction ruleAction) throws DotDataException;

    void deleteRule(Rule rule) throws DotDataException;

    void deleteConditionGroup(ConditionGroup conditionGroup) throws DotDataException;

    void deleteConditionsByGroup(ConditionGroup conditionGroup) throws DotDataException;

    void deleteCondition(Condition condition) throws DotDataException;

    void deleteRuleAction(RuleAction ruleAction) throws DotDataException;


}
