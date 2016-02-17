package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.LogicalOperator;
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
    private LogicalOperator operator = LogicalOperator.AND;


    public ConditionDataGen() {
    }

    public Condition next() {
        Condition condition = new Condition();
        condition.setConditionletId(UsersCountryConditionlet.class.getSimpleName());
        condition.setOperator(operator);
        condition.setConditionGroup(groupId);

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

    public ConditionDataGen operator(LogicalOperator operator) {
        this.operator = operator;
        return this;
    }
}
 
