package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.liferay.portal.model.User;

/**
 * @author Geoff M. Granum
 */
public class ConditionGroupDataGen {

    private final RulesAPI rulesAPI = APILocator.getRulesAPI();
    private static final User user;

    static {
        try {
            user = APILocator.getUserAPI().getSystemUser();
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    private String ruleId;
    private Condition.Operator operator = Condition.Operator.AND;

    public ConditionGroupDataGen() {
    }

    public ConditionGroup next() {
        ConditionGroup group = new ConditionGroup();
        group.setOperator(operator);
        group.setRuleId(ruleId);
        return group;
    }

    public ConditionGroup nextPersisted() {
        return persist((next()));
    }

    public ConditionGroup persist(ConditionGroup group){
        try {
            rulesAPI.saveConditionGroup(group, user, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return group;
    }

    public ConditionGroupDataGen ruleId(String ruleId) {
        this.ruleId = ruleId;
        return this;
    }

    public ConditionGroupDataGen operator(Condition.Operator operator) {
        this.operator = operator;
        return this;
    }
}
 
