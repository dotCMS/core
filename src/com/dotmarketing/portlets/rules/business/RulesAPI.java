package com.dotmarketing.portlets.rules.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.List;

public interface RulesAPI {

    /**
     *
     * @param host
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<Rule> getRulesByHost(String host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param folder
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<Rule> getRulesByFolder(String folder, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param nameFilter
     * @param user
     * @param respectFrontendRoles
     * @return
     */
    List<Rule> getRulesByNameFilter(String nameFilter, User user, boolean respectFrontendRoles);

    /**
     *
     * @param id
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Rule getRuleById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param rule
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void deleteRule(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


    /**
     *
     * @param ruleId
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<ConditionGroup> getConditionGroupsByRule(String ruleId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param ruleId
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<RuleAction> getActionsByRule(String ruleId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


    /**
     *
     * @param conditionGroupId
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<Condition> getConditionsByConditionGroup(String conditionGroupId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param ruleId
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<Condition> getConditionsByRule(String ruleId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param id
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Condition getConditionById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param rule
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void saveRule(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param condition
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void saveCondition(Condition condition, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param condition
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void deleteCondition(Condition condition, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<Conditionlet> findConditionlets() throws DotDataException, DotSecurityException;

    /**
     *
     * @param clazz
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Conditionlet findConditionlet(String clazz) throws DotDataException, DotSecurityException;

    /**
     *
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<RuleActionlet> findActionlets() throws DotDataException, DotSecurityException;

    /**
     *
     * @param clazz
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    RuleActionlet findActionlet(String clazz) throws DotDataException, DotSecurityException;
}
