package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.liferay.portal.model.User;

/**
 * @author Geoff M. Granum
 */
public class RuleActionDataGen {

    private static final User user;
    private static final RulesAPI rulesAPI = APILocator.getRulesAPI();

    static {
        try {
            user = APILocator.getUserAPI().getSystemUser();
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    private Class<? extends RuleActionlet> actionlet = SetSessionAttributeActionlet.class;
    private int priority = 10;
    private String ruleId;

    public RuleActionDataGen() {
    }

    public RuleAction next() {
        RuleAction next = new RuleAction();
        next.setActionlet(actionlet.getSimpleName());
        next.setPriority(priority);
        next.setRuleId(ruleId);
        return next;
    }

    public RuleAction nextPersisted() {
        return persist(next());
    }

    public RuleAction persist(RuleAction action){
        try {
            rulesAPI.saveRuleAction(action, user, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return action;
    }

    public RuleActionDataGen actionlet(Class<? extends RuleActionlet> actionlet) {
        this.actionlet = actionlet;
        return this;
    }

    public RuleActionDataGen priority(int priority) {
        this.priority = priority;
        return this;
    }

    public RuleActionDataGen ruleId(String ruleId) {
        this.ruleId = ruleId;
        return this;
    }

    public RuleActionDataGen rule(final Rule rule) {
        this.ruleId = rule.getId();
        return this;
    }
}
 
