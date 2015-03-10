package com.dotmarketing.portlets.rules.business;

import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.Rule;

import java.util.List;

public interface RulesFactory {

    List<Rule> getRules(String hostOrFolder);

    List<Rule> getRulesByNameFilter(String nameFilter);

    Rule getRuleById(String id);

    void deleteRule(Rule rule);

    List<Condition> getConditionsByRule(String ruleId);

    Condition getConditionById(String id);

    void saveRule(Rule rule);

    void saveCondition(Condition condition);

    void deleteCondition(Condition condition);


}
