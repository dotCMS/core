package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.portlets.rules.model.Condition;
import com.liferay.portal.model.User;

/**
 * @author Geoff M. Granum
 */
public class ConditionDataGen {

    private final RulesAPI rulesAPI = APILocator.getRulesAPI();
    private static final User user;

    static {
        try {
            user = APILocator.getUserAPI().getSystemUser();
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    private String groupId;
    private Condition.Operator operator = Condition.Operator.AND;


    public ConditionDataGen() {
    }

    public Condition next() {
        Condition condition = new Condition();
        condition.setName("meh");
        condition.setConditionletId(MockTrueConditionlet.class.getSimpleName());
        condition.setOperator(operator);
        condition.setConditionGroup(groupId);
        condition.setComparison("is");

        return condition;
    }

    public Condition nextPersisted() {
        return persist(next());
    }

    public Condition persist(Condition next) {
        try {
            rulesAPI.saveCondition(next, user, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return next;
    }

    public ConditionDataGen groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public ConditionDataGen operator(Condition.Operator operator) {
        this.operator = operator;
        return this;
    }
}
 
