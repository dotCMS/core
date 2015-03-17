package com.dotmarketing.portlets.rules.business;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.Rule;
import com.liferay.portal.model.User;

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
}
