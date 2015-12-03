package com.dotmarketing.portlets.rules.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.actionlet.RuleActionletOSGIService;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.conditionlet.ConditionletOSGIService;
import com.dotmarketing.portlets.rules.model.*;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RulesAPI extends ConditionletOSGIService, RuleActionletOSGIService{

    /**
     *
     * @param host
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<Rule> getEnabledRulesByHost(Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param host
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<Rule> getAllRulesByHost(Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param host
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Set<Rule> getRulesByHostFireOn(String host, User user, boolean respectFrontendRoles, Rule.FireOn fireOn) throws DotDataException, DotSecurityException;

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
     * @param conditionGroupId
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    ConditionGroup getConditionGroupById(String conditionGroupId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param ruleId
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<RuleAction> getRuleActionsByRule(String ruleId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param ruleActionId
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    RuleAction getRuleActionById(String ruleActionId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


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
     * @param id
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    ConditionValue getConditionValueById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

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
     * @param conditionValue
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void saveConditionValue(ConditionValue conditionValue, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


    /**
     *
     * @param conditionGroup
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void saveConditionGroup(ConditionGroup conditionGroup, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param ruleAction
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void saveRuleAction(RuleAction ruleAction, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

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
     * @param conditionValue
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void deleteConditionValue(ConditionValue conditionValue, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param conditionGroup
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void deleteConditionGroup(ConditionGroup conditionGroup, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param ruleAction
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void deleteRuleAction(RuleAction ruleAction, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param action
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Map<String, RuleActionParameter> getRuleActionParameters(RuleAction action, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


    /**
     *
     * @param id
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    RuleActionParameter getRuleActionParameterById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


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

    void registerBundleService();

}
