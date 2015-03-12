package com.dotmarketing.portlets.rules.business;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.Rule;
import com.liferay.portal.model.User;

import java.util.List;

public class RulesAPIImpl {

    private PermissionAPI perAPI;
    private RulesFactory rulesFactory;

    public RulesAPIImpl() {
        perAPI = APILocator.getPermissionAPI();
        rulesFactory = FactoryLocator.getRulesFactory();
    }

    List<Rule> getRulesByHost(String host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        return perAPI.filterCollection(rulesFactory.getRulesByHost(host), PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
    }

    List<Rule> getRulesByFolder(String folder, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        return perAPI.filterCollection(rulesFactory.getRulesByHost(folder), PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
    }

    List<Rule> getRulesByNameFilter(String nameFilter, User user, boolean respectFrontendRoles) {
        return null;
    }

    Rule getRuleById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Rule rule = rulesFactory.getRuleById(id);

        if (!APILocator.getPermissionAPI().doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId());
        }
        return rule;
    }

    void deleteRule(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException  {
        if (!APILocator.getPermissionAPI().doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot delete rule: " + rule.getId());
        }

        // delete the conditions of the rule first

        List<Condition> conditions = rulesFactory.getConditionsByRule(rule.getId());

        for (int i = 0; i < conditions.size(); i++) {
            rulesFactory.deleteCondition(conditions.get(i));
        }

        rulesFactory.deleteRule(rule);

    }

    List<Condition> getConditionsByRule(String ruleId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Rule rule = rulesFactory.getRuleById(ruleId);

        if (!APILocator.getPermissionAPI().doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId());
        }
        return rulesFactory.getConditionsByRule(ruleId);
    }

    Condition getConditionById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Condition condition = rulesFactory.getConditionById(id);
        Rule rule = rulesFactory.getRuleById(condition.getRuleId());

        if (!APILocator.getPermissionAPI().doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId() + " including any of its conditions");
        }

        return condition;
    }

    void saveRule(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if (!APILocator.getPermissionAPI().doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot save rule: " + rule.getId());
        }

        rulesFactory.saveRule(rule);
    }

    void saveCondition(Condition condition, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Rule rule = rulesFactory.getRuleById(condition.getRuleId());

        if (!APILocator.getPermissionAPI().doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot save rule: " + rule.getId() + " or its conditions ");
        }

        rulesFactory.saveCondition(condition);
    }

    void deleteCondition(Condition condition, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Rule rule = rulesFactory.getRuleById(condition.getRuleId());

        if (!APILocator.getPermissionAPI().doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot delete rule: " + rule.getId() + " or its conditions ");
        }

        rulesFactory.deleteCondition(condition);
    }
}
