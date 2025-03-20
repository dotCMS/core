/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.rules;

import com.dotmarketing.business.Ruleable;
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

/**
 * This class provides CRUD operations to interact with information related to
 * Rules, an enterprise-only feature in dotCMS.
 * <p>
 * Rules allow webmasters (or permissioned users), to configure responsive front
 * end behavior based on a variety of visitor conditions. Rules can be set up to
 * both support personalization for individual users, and to identify and serve
 * content to different types of users based on differing characteristics of
 * site Visitors and Visitor behavior during a session. Conditions in the rules
 * will determine when the Rule fires, and Actions can be specified to take when
 * the Conditions are met.
 * 
 * @author root
 * @version 1.0
 * @since Jan 20, 2016
 *
 */
public interface RulesAPI extends ConditionletOSGIService, RuleActionletOSGIService{

    /**
     *
     * @param parent
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<Rule> getEnabledRulesByParent(Ruleable parent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<Rule> getAllRules(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


    /**
     * Return all the Rules for the user
     *
     * @param parentId Rules's parent id, it could be a Host's is or a Page's Id
     * @param user it method will return all the rules which this user have permission
     * @param respectFrontendRoles if it is true then respect the anonymous role
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<Rule> getAllRulesByParent(final String parentId, final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException;

    /**
     * Return all the Rules for the user with respectFrontendRoles in false
     *
     * @param parentId Rules's parent id, it could be a Host's is or a Page's Id
     * @param user it method will return all the rules which this user have permission
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<Rule> getAllRulesByParent(final String parentId, final User user)
            throws DotDataException, DotSecurityException;

    /**
     *
     * @param parent
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<Rule> getAllRulesByParent(Ruleable parent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


    /**
     *
     * @param parent
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Set<Rule> getRulesByParentFireOn(String parent, User user, boolean respectFrontendRoles, Rule.FireOn fireOn) throws DotDataException, DotSecurityException;

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
     * Removes all the rules under the parent passed as parameter.
     *
     * @param parent
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void deleteRulesByParent(Ruleable parent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     * Copies all the rules under the parent passed as parameter.
     *
     * @param originalParent
     * @param newParent
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void copyRulesByParent(Ruleable originalParent, Ruleable newParent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

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
    ParameterModel getConditionValueById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

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
     * Same as saveRule but without performing the parent validation
     */
    void saveRuleNoParentCheck(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

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
     * @param parameterModel
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void saveConditionValue(ParameterModel parameterModel, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


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
     * @param group
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void deleteConditions(ConditionGroup group, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     * 
     * @param condition
     * @throws DotDataException
     */
    void deleteConditionValues(Condition condition, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     *
     * @param parameterModel
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void deleteConditionValue(ParameterModel parameterModel, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

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
     * @param rule
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void deleteConditionGroupsByRule(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
    
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
    Map<String, ParameterModel> getRuleActionParameters(RuleAction action, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


    /**
     *
     * @param id
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    ParameterModel getRuleActionParameterById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


    /**
     *
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<Conditionlet<?>> findConditionlets() throws DotDataException, DotSecurityException;

    /**
     *
     * @param clazz
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Conditionlet findConditionlet(String clazz);

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
    RuleActionlet findActionlet(String clazz);

    /**
     * 
     */
    void registerBundleService();

    /**
     * 
     * @param rule
     * @param user
     * @throws DotDataException
     * @throws DotSecurityException
     */
	void deleteRuleActionsByRule(Rule rule, User user) throws DotDataException, DotSecurityException;

    /**
     * Util Method to update Rule.isEnable to false.
     *
     * @param rule Object Rule that you want to disable.
     * @param user The user that has permission to view and edit.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void disableRule(Rule rule, User user) throws DotDataException, DotSecurityException;

}
