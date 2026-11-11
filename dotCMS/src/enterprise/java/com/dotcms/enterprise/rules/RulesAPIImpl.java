/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.rules;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.experiments.model.Experiment;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.osgi.HostActivator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.rules.actionlet.CountRulesActionlet;
import com.dotmarketing.portlets.rules.actionlet.PersonaActionlet;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.actionlet.RuleActionletOSGIService;
import com.dotmarketing.portlets.rules.actionlet.RuleAnalyticsFireUserEventActionlet;
import com.dotmarketing.portlets.rules.actionlet.SendRedirectActionlet;
import com.dotmarketing.portlets.rules.actionlet.SetRequestAttributeActionlet;
import com.dotmarketing.portlets.rules.actionlet.SetResponseHeaderActionlet;
import com.dotmarketing.portlets.rules.actionlet.SetSessionAttributeActionlet;
import com.dotmarketing.portlets.rules.actionlet.StopProcessingActionlet;
import com.dotmarketing.portlets.rules.actionlet.VisitorTagsActionlet;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.conditionlet.ConditionletOSGIService;
import com.dotmarketing.portlets.rules.conditionlet.CurrentSessionLanguageConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.DateTimeConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.HttpMethodConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.NumberOfTimesPreviouslyVisitedConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.PagesViewedConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.PersonaConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.ReferringURLConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.RequestAttributeConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.RequestHeaderConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.RequestParameterConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.SessionAttributeConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersBrowserConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersBrowserLanguageConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersCountryConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersLogInConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersPlatformConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersSiteVisitsConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.VisitedUrlConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.VisitorOperatingSystemConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.VisitorsCurrentUrlConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.VisitorsGeolocationConditionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.dotcms.util.DotPreconditions.checkNotNull;

/**
 * This API exposes useful methods to access information related to the dotCMS
 * Rules enterprise feature.
 * 
 * @author Moved to EE by Jonathan Gamba
 * @version 1.0
 * @since Jan 20, 2016
 *
 */
public class RulesAPIImpl implements RulesAPI {

    private final PermissionAPI permissionAPI;
    private final RulesFactory rulesFactory;

    private static final Map<String, Conditionlet<?>> conditionletMap = Maps.newHashMap();
    private static final Map<String, RuleActionlet> actionletMap = Maps.newHashMap();

    private static final List<Class> conditionletOSGIclasses = new ArrayList<>();
    private static final List<Class> actionletOSGIclasses = new ArrayList<>();

    /**
     *  Move these into defaultConditionletClasses list below to re-enable. Disable by doing the opposite.
     * .add(UsersCityConditionlet.class)
     * .add(UsersDateTimeConditionlet.class)
     * .add(UsersHostConditionlet.class)
     * .add(UsersIpAddressConditionlet.class)
     * .add(UsersLandingPageUrlConditionlet.class)
     * .add(UsersOperatingSystemConditionlet.class)
     * .add(UsersPageVisitsConditionlet.class)
     * .add(UsersReferringUrlConditionlet.class)
     * .add(UsersStateConditionlet.class)
     * .add(UsersTimeConditionlet.class)
     * .add(UsersUrlParameterConditionlet.class)
     */
    private final List<Class<? extends Conditionlet<?>>> defaultConditionletClasses =
            ImmutableList.<Class<? extends Conditionlet<?>>>builder()
                    .add(UsersCountryConditionlet.class)
                    .add(RequestHeaderConditionlet.class)
                    .add(SessionAttributeConditionlet.class)
                    .add(UsersPlatformConditionlet.class)
                    .add(CurrentSessionLanguageConditionlet.class)
                    .add(ReferringURLConditionlet.class)
                    .add(DateTimeConditionlet.class)
                    .add(VisitedUrlConditionlet.class)
                    .add(UsersBrowserLanguageConditionlet.class)
                    .add(UsersBrowserConditionlet.class)
                    .add(VisitorsCurrentUrlConditionlet.class)
                    .add(UsersLogInConditionlet.class)
                    .add(UsersSiteVisitsConditionlet.class)
                    .add(PagesViewedConditionlet.class)
                    .add(RequestParameterConditionlet.class)
                    .add(RequestAttributeConditionlet.class)
                    .add(VisitorOperatingSystemConditionlet.class)
                    .add(NumberOfTimesPreviouslyVisitedConditionlet.class)
                    .add(VisitorsGeolocationConditionlet.class)
                    .add(PersonaConditionlet.class)
                    .add(HttpMethodConditionlet.class)
                    .build();
    private final List<Class<? extends RuleActionlet<?>>> defaultActionletClasses =
            ImmutableList.<Class<? extends RuleActionlet<?>>>builder()
                    .add(CountRulesActionlet.class)
                    .add(SetSessionAttributeActionlet.class)
                    .add(SetRequestAttributeActionlet.class)
                    .add(SetResponseHeaderActionlet.class)
                    .add(PersonaActionlet.class)
                    .add(VisitorTagsActionlet.class)
                    .add(SendRedirectActionlet.class)
                    .add(StopProcessingActionlet.class)
                    .add(RuleAnalyticsFireUserEventActionlet.class)
                    .build();

    public RulesAPIImpl() {
        permissionAPI = APILocator.getPermissionAPI();
        rulesFactory = FactoryLocator.getRulesFactory();
        refreshConditionletsMap();
        refreshActionletsMap();
        initActionletMap();
    }

    @CloseDBIfOpened
    @Override
    public List<Rule> getEnabledRulesByParent(Ruleable host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(host)) {
            return new ArrayList<>();
        }

        List<Rule> rules = rulesFactory.getAllRulesByParent(host);

        if(rules.isEmpty()){
        	return new ArrayList<>();
        }

        checkRulePermission(user, rules.get(0), PermissionAPI.PERMISSION_USE, respectFrontendRoles);

        return rules;
    }

    @CloseDBIfOpened
    @Override
    public List<Rule> getAllRules(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return permissionAPI
				.filterCollection(rulesFactory.getAllRules(), PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
    }

    /**
     * {@inheritDoc}
     * <p>The parent is looked up as a site first, then as a page, archived versions included, so
     * the rules of an archived page are listed under the same permission check as a live one.</p>
     */
    @CloseDBIfOpened
    @Override
    public List<Rule> getAllRulesByParent(final String parentId, final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        Host host = APILocator.getHostAPI().find(parentId, user, false);

        if (host == null) {
            final Contentlet contentlet = APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(parentId, true);
            return  getAllRulesByParent(contentlet, user, respectFrontendRoles);
        } else {
            return  getAllRulesByParent(host, user, respectFrontendRoles);
        }
    }

    @CloseDBIfOpened
    @Override
    public List<Rule> getAllRulesByParent(final String parentId, final User user)
            throws DotDataException, DotSecurityException {
        return getAllRulesByParent(parentId, user, false);
    }

    @CloseDBIfOpened
    @Override
    public List<Rule> getAllRulesByParent(
            final Ruleable parent,
            final User user,
            final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        // The following logic only applies for pages and hosts
        if (!UtilMethods.isSet(parent) || (parent instanceof Contentlet &&
                (!((Contentlet) parent).isHost() && !((Contentlet) parent).isHTMLPage()))) {
            return Collections.emptyList();
        }

        final List<Rule> rules = rulesFactory.getAllRulesByParent(parent);
        if (rules.isEmpty()) {
            return Collections.emptyList();
        }

        if (parent instanceof Contentlet) {
            checkRulePermission(user, (Contentlet) parent, PermissionAPI.PERMISSION_READ, respectFrontendRoles);
        } else {
            checkRulePermission(user, parent.getIdentifier(), PermissionAPI.PERMISSION_READ, respectFrontendRoles);
        }
        return rules;
    }

    private Optional<Permissionable> getParentPermissionable(final Ruleable parent) throws DotDataException, DotSecurityException {

        if (!isPageOrHost(parent)) {
            return Optional.empty();
        }

        Permissionable parentPermissionable = null;

        if (parent instanceof Host || (parent instanceof Contentlet && ((Contentlet) parent).isHost())) {
            parentPermissionable = parent;
        } else {
            final Contentlet contentParent = (Contentlet) parent;
            final Folder folder = APILocator.getFolderAPI().find(contentParent.getFolder(), APILocator.systemUser(),
                    false);

            parentPermissionable = folder.getPath().equals("/") ?
                    APILocator.getHostAPI().find(contentParent.getHost(), APILocator.systemUser(), false) :
                    folder;
        }

        return Optional.of(parentPermissionable);
    }

    private boolean isPageOrHost(final Ruleable parent) {
        return UtilMethods.isSet(parent) &&
                (parent instanceof Contentlet &&
                        (Contentlet.class.cast(parent).isHost() || Contentlet.class.cast(parent).isHTMLPage())
                );
    }

    /**
     * Checks that the user holds the given Rules permission on the parent of the rule.
     *
     * @param user                 the user to check
     * @param rule                 the rule whose parent is checked
     * @param permissionLevel      the Rules permission level required
     * @param respectFrontendRoles whether front-end roles are respected
     * @throws DotDataException     if the parent cannot be looked up
     * @throws DotSecurityException if the user does not hold the permission on the rule's parent
     */
    private void checkRulePermission(
            final User user,
            final Rule rule,
            final int permissionLevel, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        this.checkRulePermission(user, rule.getParent(), permissionLevel, respectFrontendRoles);
    }

    /**
     * Checks that the user holds the given Rules permission on the rule parent with the given
     * identifier. The parent must be a site or a page (live or archived), checked through
     * {@link #checkRulePermission(User, Contentlet, int, boolean)}, or an experiment, which
     * requires EDIT on the experiment's page for every operation, checked the way
     * {@code ExperimentsAPIImpl} checks reading and editing an experiment (EDIT granted through a
     * front-end role counts there too). Any other parent is refused, except for the system user.
     *
     * @param user                 the user to check
     * @param parentId             identifier of the rule's parent
     * @param permissionLevel      the Rules permission level required
     * @param respectFrontendRoles whether front-end roles are respected for a site or page parent
     * @throws DotDataException     if the parent cannot be looked up
     * @throws DotSecurityException if the parent is not a site, page or experiment the user may
     *                              work on
     */
    private void checkRulePermission(
            final User user,
            final String parentId,
            final int permissionLevel, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        if (isSystemUser(user)) {
            return;
        }

        final Contentlet parent = UtilMethods.isSet(parentId)
                ? APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(parentId, true)
                : null;
        if (parent != null) {
            this.checkRulePermission(user, parent, permissionLevel, respectFrontendRoles);
            return;
        }

        final Optional<Experiment> experiment = UtilMethods.isSet(parentId)
                ? FactoryLocator.getExperimentsFactory().find(parentId)
                : Optional.empty();
        if (experiment.isPresent()) {
            final PermissionableProxy experimentPage = new PermissionableProxy();
            experimentPage.setIdentifier(experiment.get().pageId());
            experimentPage.setType("htmlpage");

            if (!permissionAPI.doesUserHavePermission(experimentPage, PermissionLevel.EDIT.getType(), user)) {
                throw denied(user, permissionLevel);
            }
            return;
        }

        throw denied(user, permissionLevel);
    }

    /**
     * Checks that the user holds the given Rules permission on a site or page parent. The parent
     * must be a site or a page; writes on a page also need PUBLISH on the page itself.
     *
     * @param user                 the user to check
     * @param parent               the site or page owning the rules
     * @param permissionLevel      the Rules permission level required
     * @param respectFrontendRoles whether front-end roles are respected
     * @throws DotDataException     if the parent's permissions cannot be looked up
     * @throws DotSecurityException if the parent is not a site or page, or the user does not hold
     *                              the permission on it
     */
    private void checkRulePermission(
            final User user,
            final Contentlet parent,
            final int permissionLevel, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        if (isSystemUser(user)) {
            return;
        }

        final Permissionable parentPermissionable = getParentPermissionable(parent)
                .orElseThrow(() -> denied(user, permissionLevel));
        if (!permissionAPI.doesUserHavePermissions(parentPermissionable, "RULES: " + permissionLevel, user, respectFrontendRoles)) {
            throw denied(user, permissionLevel);
        }

        if (parent.isHTMLPage() && PermissionAPI.PERMISSION_PUBLISH == permissionLevel) {
            try {
                permissionAPI.checkPermission(parent, PermissionLevel.PUBLISH, user);
            } catch (DotSecurityException e) {
                throw new DotSecurityException(String.format("User %s does not have permissions to PUBLISH PAGE: %s", user.getUserId(), parent.getIdentifier()));
            }
        }
    }

    /**
     * When a stored rule component (condition group, condition, condition value or action) is saved
     * under a different rule than the one it belongs to, also requires PUBLISH on the rule it
     * belongs to now, so only a user who may edit both rules can move it. A stored component whose
     * rule no longer exists has nothing to protect, so no extra check is made for it.
     *
     * @param user                 the user saving the component
     * @param storedRuleId         the rule the stored component belongs to, or {@code null} when the
     *                             component is new
     * @param ruleId               the rule the component is being saved under
     * @param respectFrontendRoles whether front-end roles are respected
     * @throws DotDataException     if the stored rule cannot be looked up
     * @throws DotSecurityException if the user may not edit the rule the component belongs to now
     */
    private void checkStoredRulePermission(final User user, final String storedRuleId,
            final String ruleId, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        if (!UtilMethods.isSet(storedRuleId) || storedRuleId.equals(ruleId)) {
            return;
        }
        final Rule storedRule = rulesFactory.getRuleById(storedRuleId);
        if (storedRule != null) {
            checkRulePermission(user, storedRule, PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles);
        }
    }

    /**
     * Returns the rule a stored action belongs to.
     *
     * @param actionId the action id, may be {@code null} for a new action
     * @return the rule id, or {@code null} when no action with that id is stored
     * @throws DotDataException if the action cannot be looked up
     */
    private String storedRuleIdOfAction(final String actionId) throws DotDataException {
        final RuleAction stored = UtilMethods.isSet(actionId) ? rulesFactory.getRuleActionById(actionId) : null;
        return stored != null ? stored.getRuleId() : null;
    }

    /**
     * Returns the rule a stored condition group belongs to.
     *
     * @param groupId the condition group id, may be {@code null} for a new group
     * @return the rule id, or {@code null} when no group with that id is stored
     * @throws DotDataException if the group cannot be looked up
     */
    private String storedRuleIdOfGroup(final String groupId) throws DotDataException {
        final ConditionGroup stored = UtilMethods.isSet(groupId) ? rulesFactory.getConditionGroupById(groupId) : null;
        return stored != null ? stored.getRuleId() : null;
    }

    /**
     * Returns the rule a stored condition belongs to, through its condition group.
     *
     * @param conditionId the condition id, may be {@code null} for a new condition
     * @return the rule id, or {@code null} when no condition with that id is stored
     * @throws DotDataException if the condition or its group cannot be looked up
     */
    private String storedRuleIdOfCondition(final String conditionId) throws DotDataException {
        final Condition stored = UtilMethods.isSet(conditionId) ? rulesFactory.getConditionById(conditionId) : null;
        return stored != null ? storedRuleIdOfGroup(stored.getConditionGroup()) : null;
    }

    /**
     * Returns the rule a stored condition value belongs to, through its condition and group.
     *
     * @param valueId the condition value id, may be {@code null} for a new value
     * @return the rule id, or {@code null} when no value with that id is stored
     * @throws DotDataException if the value, its condition or its group cannot be looked up
     */
    private String storedRuleIdOfConditionValue(final String valueId) throws DotDataException {
        final ParameterModel stored = UtilMethods.isSet(valueId) ? rulesFactory.getConditionValueById(valueId) : null;
        return stored != null ? storedRuleIdOfCondition(stored.getOwnerId()) : null;
    }

    /**
     * Tells whether the given user is the system user.
     *
     * @param user the user to test, may be {@code null}
     * @return {@code true} only for the system user
     */
    private boolean isSystemUser(final User user) {
        return user != null && UserAPI.SYSTEM_USER_ID.equals(user.getUserId());
    }

    /**
     * Builds the refusal returned when the user may not work on the rules of a parent.
     *
     * @param user            the refused user, may be {@code null}
     * @param permissionLevel the Rules permission level that was required
     * @return the exception to throw
     */
    private DotSecurityException denied(final User user, final int permissionLevel) {
        final String permissionType = (PermissionLevel.USE.getType() == permissionLevel || PermissionLevel.READ.getType() == permissionLevel) ?
                "VIEW" : "PUBLISH";
        final String userId = user != null ? user.getUserId() : null;
        return new DotSecurityException(String.format("User %s does not have permissions to %s Rules", userId, permissionType));
    }

    @CloseDBIfOpened
    @Override
    public Set<Rule> getRulesByParentFireOn(String parent, User user, boolean respectFrontendRoles, Rule.FireOn fireOn) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(parent)) {
            return new HashSet<>();
        }

        ArrayList<Rule> rules = new ArrayList<>(rulesFactory.getRulesByParent(parent, fireOn));

        if(rules.isEmpty()){
        	return new HashSet<>();
        }

        checkRulePermission(user, rules.get(0), PermissionAPI.PERMISSION_USE, respectFrontendRoles);

        return new HashSet<>(rules);
    }

    @Override
    public List<Rule> getRulesByNameFilter(String nameFilter, User user, boolean respectFrontendRoles) {
        return null;
    }

    @CloseDBIfOpened
    @Override
    public Rule getRuleById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(id)) {
            return null;
        }

        Rule rule = rulesFactory.getRuleById(id);

        if(!UtilMethods.isSet(rule)) {
            Logger.info(this, "There is no rule with the given id: " + id);
            return null;
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_USE, respectFrontendRoles);

        return rule;
    }

    // todo: should it be a transaction (all or nothing)
    @Override
    public void deleteRulesByParent(Ruleable parent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        List<Rule> rulesByParent = getAllRulesByParent(parent, user, respectFrontendRoles);
        for (Rule rule : rulesByParent) {
            deleteRule(rule, user, respectFrontendRoles);
        }

    }

    // todo: should it be a transaction (all or nothing)
    @Override
    public void copyRulesByParent(Ruleable parent, Ruleable newParent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        List<Rule> newRulesByParent = Lists.newArrayList();

        //Make deep copy of the Rules.
        for (Rule rule : getAllRulesByParent(parent, user, respectFrontendRoles)) {
            newRulesByParent.add(new Rule(rule));
        }

        //Iterate over the new copy of Rules.
        for (Rule rule : newRulesByParent) {
            //Remove to cache to prevent same object in memory.
            CacheLocator.getRulesCache().removeRule(rule);

            rule.setId(UUIDGenerator.generateUuid());
            rule.setParent(newParent.getIdentifier());
            rule.setParentPermissionable(newParent);

            saveRule(rule, user, respectFrontendRoles);

            //Handling Conditions.
            for (ConditionGroup group : rule.getGroups()) {
                group.setId(UUIDGenerator.generateUuid());
                group.setRuleId(rule.getId());

                saveConditionGroup(group, user, respectFrontendRoles);

                for (Condition condition : group.getConditions()) {
                    condition.setId(UUIDGenerator.generateUuid());
                    condition.setConditionGroup(group.getId());

                    for (ParameterModel parameterModel : condition.getValues()) {
                        parameterModel.setId(UUIDGenerator.generateUuid());
                        parameterModel.setOwnerId(condition.getId());
                    }

                    saveCondition(condition, user, respectFrontendRoles);
                }
            }

            //Handling Rule Actions.
            for (RuleAction ruleAction : rule.getRuleActions()) {
                ruleAction.setId(UUIDGenerator.generateUuid());
                ruleAction.setRuleId(rule.getId());

                for (ParameterModel parameterModel : ruleAction.getParameters().values()) {
                    parameterModel.setId(UUIDGenerator.generateUuid());
                    parameterModel.setOwnerId(ruleAction.getId());
                }

                saveRuleAction(ruleAction, user, respectFrontendRoles);
            }
        }

    }

    @WrapInTransaction
    @Override
    public void deleteRule(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException  {
        if(!UtilMethods.isSet(rule)) {
            return;
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles);

        // delete the Condition Groups of the rule first

        List<ConditionGroup> groups = rulesFactory.getConditionGroupsByRuleFromDB(rule.getId());

        for (ConditionGroup group : groups) {
            deleteConditionGroup(group, user, respectFrontendRoles);
        }

        // delete the Rule Actions

        deleteRuleActionsByRule(rule, user);

        // delete the Rule
        rulesFactory.deleteRule(rule);
        String userID = user != null ? user.getUserId() : "";
        ActivityLogger.logInfo(getClass(), "Removed Rule: " + rule.getId(), "Date: " + DateUtil.getCurrentDate() + "; " + "User:"+ userID);
    }

    @WrapInTransaction
    @Override
    public void deleteRuleActionsByRule(Rule rule, User user) throws DotDataException, DotSecurityException  {
        if(!UtilMethods.isSet(rule)) {
            return;
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_PUBLISH, false);


        List<RuleAction> actions = rulesFactory.getRuleActionsByRuleFromDB(rule.getId());

        // delete action parameters
        for (RuleAction action : actions) {
            rulesFactory.deleteRuleActionsParameters(action);

            // delete the action
            rulesFactory.deleteRuleAction(action);
            String userID = user != null ? user.getUserId() : "";
            ActivityLogger.logInfo(getClass(), "Removed Rule Action: " + action.getId(), "Date: " + DateUtil.getCurrentDate() + "; " + "User:"+ userID);
        }

    }

    @CloseDBIfOpened
    @Override
    public List<ConditionGroup> getConditionGroupsByRule(String ruleId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(ruleId)) {
            return new ArrayList<>();
        }

        Rule rule = rulesFactory.getRuleById(ruleId);

        if(!UtilMethods.isSet(rule)) {
            Logger.info(this, "There is no rule with the given id: " + ruleId);
            return new ArrayList<>();
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_USE, respectFrontendRoles);

        return rulesFactory.getConditionGroupsByRule(ruleId);
    }

    @CloseDBIfOpened
    @Override
    public ConditionGroup getConditionGroupById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        ConditionGroup conditionGroup = rulesFactory.getConditionGroupById(id);

        if(conditionGroup==null) {
            Logger.info(this, "There is no condition group with the given id: " + id);
            return null;
        }

        Rule rule = rulesFactory.getRuleById(conditionGroup.getRuleId());

        if(rule==null){
            Logger.info(this, "There is no rule with the given id: " + conditionGroup.getRuleId());
            return null;
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_USE, respectFrontendRoles);

        return conditionGroup;
    }

    @CloseDBIfOpened
    @Override
    public List<RuleAction> getRuleActionsByRule(String ruleId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(ruleId)) {
            return new ArrayList<>();
        }

        Rule rule = rulesFactory.getRuleById(ruleId);

        if(!UtilMethods.isSet(rule)) {
            Logger.info(this, "There is no rule with the given id: " + ruleId);
            return new ArrayList<>();
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_USE, respectFrontendRoles);

        return rulesFactory.getRuleActionsByRule(ruleId);
    }

    @CloseDBIfOpened
    @Override
    public RuleAction getRuleActionById(String ruleActionId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        RuleAction action = rulesFactory.getRuleActionById(ruleActionId);

        if(action==null) {
            Logger.info(this, "There is no action with the given id: " + ruleActionId);
            return null;
        }

        Rule rule = rulesFactory.getRuleById(action.getRuleId());

        if(rule==null) {
            Logger.info(this, "There is no rule with the given id: " + action.getRuleId());
            return null;
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_USE, respectFrontendRoles);

        return action;
    }

    @CloseDBIfOpened
    @Override
    public List<Condition> getConditionsByConditionGroup(String conditionGroupId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(conditionGroupId)) {
            return new ArrayList<>();
        }

        ConditionGroup conditionGroup = rulesFactory.getConditionGroupById(conditionGroupId);

        if(!UtilMethods.isSet(conditionGroup)) {
            Logger.info(this, "There is no condition group with the given id: " + conditionGroupId);
            return new ArrayList<>();
        }

        Rule rule = rulesFactory.getRuleById(conditionGroup.getRuleId());

        if(rule==null) {
            Logger.info(this, "There is no rule with the given id: " + conditionGroup.getRuleId());
            return null;
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_USE, respectFrontendRoles);

        return rulesFactory.getConditionsByGroup(conditionGroup.getId());
    }

    @CloseDBIfOpened
    @Override
    public Condition getConditionById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Condition condition = rulesFactory.getConditionById(id);

        if(condition==null) {
            Logger.info(this, "There is no condition with the given id: " + id);
            return null;
        }

        ConditionGroup group = getConditionGroupById(condition.getConditionGroup(), user, true);

        Rule rule = rulesFactory.getRuleById(group.getRuleId());

        if(rule==null) {
            Logger.info(this, "There is no rule with the given id: " + group.getRuleId());
            return null;
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_USE, respectFrontendRoles);

        return condition;
    }

    @CloseDBIfOpened
    @Override
    public ParameterModel getConditionValueById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(id)) {
            return null;
        }

        ParameterModel value = rulesFactory.getConditionValueById(id);

        if(!UtilMethods.isSet(value)) {
            Logger.info(this, "There is no Condition Value with the given id: " + id);
            return null;
        }

        Condition condition = rulesFactory.getConditionById(value.getOwnerId());

        if(condition==null) {
            Logger.info(this, "There is no condition associated with the given Condition Value: " + id);
            return null;
        }

        ConditionGroup group = getConditionGroupById(condition.getConditionGroup(), user, true);

        Rule rule = rulesFactory.getRuleById(group.getRuleId());

        if(rule==null) {
            Logger.info(this, "There is no rule associated with the given Condition Value: " + id);
            return null;
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_USE, respectFrontendRoles);

        return value;

    }

    @WrapInTransaction
    @Override
    public void saveRule(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        rule = checkNotNull(rule, "Rule is required.");
        user = checkNotNull(user, "User is required.");

        checkParentIsAllowed(rule, user);
        saveRuleNoParentCheck(rule, user, respectFrontendRoles);
    }

    @WrapInTransaction
    @Override
    public void saveRuleNoParentCheck(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        rule = checkNotNull(rule, "Rule is required.");
        user = checkNotNull(user, "User is required.");

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles);

        rulesFactory.saveRule(rule);
        String userID = user != null ? user.getUserId() : "";
        ActivityLogger.logInfo(getClass(), "Saved/Updated Rule: " + rule.getId(), "Date: " + DateUtil.getCurrentDate() + "; " + "User:"+ userID);
    }

    /**
     * This util method checks is the Parent is a Host or a Page,
     * In case Parent is a page it checks that FireOn = EVERY_PAGE.
     *
     * @param rule Rule Object you want to check.
     * @throws DotDataException in case is not a Host or Page.
     * @throws DotSecurityException in case problem with Permission.
     */
    private void checkParentIsAllowed(final Rule rule, final User user) throws DotDataException, DotSecurityException{
        Identifier identifier = APILocator.getIdentifierAPI().find(rule.getParent());

        if(!UtilMethods.isSet(identifier) || !UtilMethods.isSet(identifier.getAssetType())){
            throw new DotDataException("Parent Identifier: " + rule.getParent() + " does NOT exist.");
        }

        if(!identifier.getAssetType().equals("contentlet")){
            throw new DotDataException("Rule's Parent: " + rule.getParent() + " is not a Host or a Page");
        }

        List<Versionable> versionableListlist = APILocator.getVersionableAPI().findAllVersions(rule.getParent());

        if(!versionableListlist.isEmpty()) {
            Versionable parenVersionable = versionableListlist.get(0);
            Contentlet parentContentlet = APILocator.getContentletAPI().find(parenVersionable.getInode(), APILocator.getUserAPI().getSystemUser(), false);

            if(!parentContentlet.isHost() && !parentContentlet.isHTMLPage()){
                throw new DotDataException("Rule's Parent: " + rule.getParent() + " is not a Host or a Page");
            }
            if(parentContentlet.isHTMLPage() && !rule.getFireOn().equals(Rule.FireOn.EVERY_PAGE)){
                throw new DotDataException("Rule's Parent: " + rule.getParent() + " is a Page, Rule's Fire on Value needs to be EVERY_PAGE");
            }
        } else {
            throw new DotDataException("Rule's Parent: " + rule.getParent() + " is not a Host or a Page");
        }

    }


    /**
     * {@inheritDoc}
     * <p>Saving a stored condition group under a different rule also requires PUBLISH on the rule it belongs to
     * now.</p>
     */
    @WrapInTransaction
    @Override
    public void saveConditionGroup(ConditionGroup conditionGroup, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        conditionGroup = checkNotNull(conditionGroup, "ConditionGroup is required.");
        Rule rule = checkNotNull(rulesFactory.getRuleById(conditionGroup.getRuleId()),
                DotRuntimeException.class,
                "Invalid Rule specified: %s",
                conditionGroup.getRuleId());

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles);
        checkStoredRulePermission(user, storedRuleIdOfGroup(conditionGroup.getId()), rule.getId(),
                respectFrontendRoles);

        rulesFactory.saveConditionGroup(conditionGroup);
        String userID = user != null ? user.getUserId() : "";
        ActivityLogger.logInfo(getClass(), "Saved/Updated Condition Group: " + conditionGroup.getId(), "Date: " + DateUtil.getCurrentDate() + "; " + "User:"+ userID);
    }

    /**
     * {@inheritDoc}
     * <p>Saving a stored condition under a different condition group also requires PUBLISH on the rule it belongs to
     * now.</p>
     */
    @WrapInTransaction
    @Override
    public void saveCondition(Condition condition, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        condition = checkNotNull(condition, "Condition is required.");

        ConditionGroup group = checkNotNull(getConditionGroupById(condition.getConditionGroup(), user, true),
                DotRuntimeException.class,
                "Invalid ConditionGroup specified: %s",
                condition.getConditionGroup());

        Rule rule = rulesFactory.getRuleById(group.getRuleId()); // Can only be null if there is a schema integrity failure.

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles);
        checkStoredRulePermission(user, storedRuleIdOfCondition(condition.getId()), group.getRuleId(),
                respectFrontendRoles);

        rulesFactory.saveCondition(condition);
        String userID = user != null ? user.getUserId() : "";
        ActivityLogger.logInfo(getClass(), "Saved/Updated Rule Condition: " + condition.getId(), "Date: " + DateUtil.getCurrentDate() + "; " + "User:"+ userID);
    }

    /**
     * {@inheritDoc}
     * <p>Saving a stored condition value under a different condition also requires PUBLISH on the rule it belongs to
     * now.</p>
     */
    @WrapInTransaction
    @Override
    public void saveConditionValue(ParameterModel parameterModel, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        parameterModel = checkNotNull(parameterModel, "Condition Value is required.");

        Condition condition = checkNotNull(getConditionById(parameterModel.getOwnerId(), user, true),
                                                         DotRuntimeException.class,
                                                         "Invalid Condition specified: %s",
                                                         parameterModel.getOwnerId());

        ConditionGroup group = checkNotNull(getConditionGroupById(condition.getConditionGroup(), user, true),
                DotRuntimeException.class,
                "Invalid ConditionGroup specified: %s",
                condition.getConditionGroup());

        Rule rule = rulesFactory.getRuleById(group.getRuleId()); // Can only be null if there is a schema integrity failure.

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles);
        checkStoredRulePermission(user, storedRuleIdOfConditionValue(parameterModel.getId()),
                group.getRuleId(), respectFrontendRoles);

        rulesFactory.saveConditionValue(parameterModel);
        String userID = user != null ? user.getUserId() : "";
        ActivityLogger.logInfo(getClass(), "Saved/Updated Rule Condition Value: " + parameterModel.getId(), "Date: " + DateUtil.getCurrentDate() + "; " + "User:"+ userID);
    }

    /**
     * {@inheritDoc}
     * <p>Saving a stored action under a different rule also requires PUBLISH on the rule it belongs to
     * now.</p>
     */
    @WrapInTransaction
    @Override
    public void saveRuleAction(RuleAction ruleAction, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        ruleAction = checkNotNull(ruleAction, "RuleAction is required");
        user = checkNotNull(user, "User is required");

        Rule rule = rulesFactory.getRuleById(ruleAction.getRuleId());

        if(rule==null) {
            Logger.info(this, "There is no rule with the given id: " + ruleAction.getRuleId());
            throw new DotDataException("There is no Rule with the provided ruleId: "+ruleAction.getRuleId());
        }

        if(!Config.getBooleanProperty("BYPASS_RULE_ACTIONLET_VALIDATION", false) && findActionlet(ruleAction.getActionlet())==null) {
            Logger.info(this, "There is no actionlet with the given id: " + ruleAction.getActionlet());
            throw new DotDataException("There is no actionlet with the provided actionletId: "+ruleAction.getActionlet());
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles);
        checkStoredRulePermission(user, storedRuleIdOfAction(ruleAction.getId()), rule.getId(),
                respectFrontendRoles);

        rulesFactory.saveRuleAction(ruleAction);
        String userID = user != null ? user.getUserId() : "";
        ActivityLogger.logInfo(getClass(), "Saved/Updated Rule Action: " + ruleAction.getId(), "Date: " + DateUtil.getCurrentDate() + "; " + "User:"+ userID);
    }

    @WrapInTransaction
    @Override
    public void deleteCondition(Condition condition, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(condition)) {
            return;
        }

        ConditionGroup group = getConditionGroupById(condition.getConditionGroup(), user, true);

        Rule rule = rulesFactory.getRuleById(group.getRuleId());

        if(rule==null) {
            Logger.info(this, "There is no rule with the given id: " + group.getRuleId());
            throw new DotDataException("There is no Rule with the provided ruleId: " + group.getRuleId());
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles);

        // delete the condition values
        rulesFactory.deleteConditionValues(condition);

        // delete the condition
        rulesFactory.deleteCondition(condition);
        String userID = user != null ? user.getUserId() : "";
        ActivityLogger.logInfo(getClass(), "Removed Rule Condition: " + condition.getId(), "Date: " + DateUtil.getCurrentDate() + "; " + "User:"+ userID);
    }

    @WrapInTransaction
    @Override
    public void deleteConditionValue(ParameterModel parameterModel, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(parameterModel)) {
            return;
        }

        Condition condition = getConditionById(parameterModel.getOwnerId(), user, false);

        ConditionGroup group = getConditionGroupById(condition.getConditionGroup(), user, false);

        Rule rule = rulesFactory.getRuleById(group.getRuleId());

        if(rule==null) {
            Logger.info(this, "There is no rule with the given id: " + group.getRuleId());
            throw new DotDataException("There is no Rule with the provided ruleId: " + group.getRuleId());
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles);

        rulesFactory.deleteConditionValue(parameterModel);
        String userID = user != null ? user.getUserId() : "";
        ActivityLogger.logInfo(getClass(), "Removed Rule Condition Value: " + parameterModel.getId(), "Date: " + DateUtil.getCurrentDate() + "; " + "User:"+ userID);

    }

    @WrapInTransaction
	@Override
	public void deleteConditionValues(Condition condition, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		if (!UtilMethods.isSet(condition) || StringUtils.isBlank(condition.getId())) {
			return;
		}
		ConditionGroup group = getConditionGroupById(condition.getConditionGroup(), user, false);
		Rule rule = rulesFactory.getRuleById(group.getRuleId());
		if (rule == null) {
			Logger.info(this, "There is no rule with the given id: " + group.getRuleId());
			throw new DotDataException("There is no Rule with the provided ruleId: " + group.getRuleId());
		}
        checkRulePermission(user, rule, PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles);
        rulesFactory.deleteConditionValues(condition);
		String userID = user != null ? user.getUserId() : "";
		ActivityLogger.logInfo(getClass(), "Removed Values of Condition: " + condition.getId(),
				"; Date: " + DateUtil.getCurrentDate() + "; User: " + userID);
	}

	@WrapInTransaction
    @Override
    public void deleteConditions(ConditionGroup group, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		if (!UtilMethods.isSet(group) || StringUtils.isBlank(group.getId())) {
			return;
		}

        Rule rule = rulesFactory.getRuleByIdFromDB(group.getRuleId());

        if(rule==null) {
            Logger.info(this, "There is no rule with the given id: " + group.getRuleId());
            throw new DotDataException("There is no Rule with the provided ruleId: " + group.getRuleId());
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles);

        // delete the condition values
        for (Condition condition : group.getConditions()) {
            rulesFactory.deleteConditionValues(condition);
        }

        // delete the condition
        rulesFactory.deleteConditionsByGroup(group);
        String userID = user != null ? user.getUserId() : "";
        ActivityLogger.logInfo(getClass(), "Removed Rule Conditions By Group: " + group.getId(), "Date: " + DateUtil.getCurrentDate() + "; " + "User:"+ userID);
    }

    @WrapInTransaction
    @Override
    public void deleteConditionGroup(ConditionGroup conditionGroup, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(conditionGroup)) {
            return;
        }

        Rule rule = rulesFactory.getRuleByIdFromDB(conditionGroup.getRuleId());

        if(!UtilMethods.isSet(rule)) {
            Logger.info(this, "There is no rule with the given id: " + conditionGroup.getRuleId());
            return;
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles);

        // delete the conditions

        deleteConditions(conditionGroup, user, respectFrontendRoles);

        rulesFactory.deleteConditionGroup(conditionGroup);
        String userID = user != null ? user.getUserId() : "";
        ActivityLogger.logInfo(getClass(), "Removed Rule Condition Group: " + conditionGroup.getId(), "Date: " + DateUtil.getCurrentDate() + "; " + "User:"+ userID);
    }

    @WrapInTransaction
	@Override
	public void deleteConditionGroupsByRule(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		if (!UtilMethods.isSet(rule) || StringUtils.isBlank(rule.getId())) {
			return;
		}
        checkRulePermission(user, rule, PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles);
        List<ConditionGroup> conditionGroups = rulesFactory.getConditionGroupsByRule(rule.getId());
		for (ConditionGroup conditionGroup : conditionGroups) {
			rulesFactory.deleteConditionGroup(conditionGroup);
			String userID = user != null ? user.getUserId() : "";
			ActivityLogger.logInfo(getClass(), "Removed Condition Group: " + conditionGroup.getId(),
					"; Date: " + DateUtil.getCurrentDate() + "; User: " + userID);
		}
	}

	@WrapInTransaction
    @Override
    public void deleteRuleAction(RuleAction ruleAction, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(ruleAction)) {
            return;
        }

        Rule rule = rulesFactory.getRuleById(ruleAction.getRuleId());

        if(!UtilMethods.isSet(rule)) {
            Logger.info(this, "There is no rule with the given id: " + ruleAction.getRuleId());
            return;
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles);

        rulesFactory.deleteRuleActionsParameters(ruleAction);

        rulesFactory.deleteRuleAction(ruleAction);
        String userID = user != null ? user.getUserId() : "";
        ActivityLogger.logInfo(getClass(), "Removed Rule Action: " + ruleAction.getId(), "Date: " + DateUtil.getCurrentDate() + "; " + "User:"+ userID);
    }

    @CloseDBIfOpened
    @Override
    public Map<String, ParameterModel> getRuleActionParameters(RuleAction action, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        String ruleId = action.getRuleId();

        if (!UtilMethods.isSet(ruleId)) {
            return new HashMap<>();
        }

        Rule rule = rulesFactory.getRuleById(ruleId);

        if (!UtilMethods.isSet(rule)) {
            Logger.info(this, "There is no rule with the given id: " + action.getRuleId());
            return new HashMap<>();
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_USE, respectFrontendRoles);

        return rulesFactory.getRuleActionParameters(action);
    }

    @CloseDBIfOpened
    @Override
    public ParameterModel getRuleActionParameterById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(id)) {
            return null;
        }

        ParameterModel parameter = rulesFactory.getRuleActionParameterById(id);

        if(!UtilMethods.isSet(parameter)) {
            Logger.info(this, "There is no RuleAction Parameter with the given id: " + id);
            return null;
        }

        RuleAction action = rulesFactory.getRuleActionById(parameter.getOwnerId());

        if(action==null) {
            Logger.info(this, "There is no RuleAction associated with the given RuleAction Parameter: " + id);
            return null;
        }

        Rule rule = rulesFactory.getRuleById(action.getRuleId());

        if(rule==null) {
            Logger.info(this, "There is no rule associated with the given RuleAction Parameter: " + id);
            return null;
        }

        checkRulePermission(user, rule, PermissionAPI.PERMISSION_USE, respectFrontendRoles);

        return parameter;
    }

    /**
     * 
     */
    private void refreshConditionletsMap() {
        synchronized (conditionletMap) {
            //Get the OSGi ones.
            List<Conditionlet> conditionlets = Lists.newArrayList(getCustomConditionlets());
            //Get the default ones.
            conditionlets.addAll(getDefaultConditionlets());

            for (Conditionlet conditionlet : conditionlets) {
                try {
                    Class<? extends Conditionlet> clazz = conditionlet.getClass();
                    Conditionlet instance = clazz.newInstance();
                    String id = instance.getId();
                    if(!conditionletMap.containsKey(id)){
                        conditionletMap.putIfAbsent(id, instance);
                    }
                    else {
                        Logger.info(RulesAPIImpl.class, "Conditionlet with name '" + clazz.getSimpleName() + "' already registered.");
                    }
                } catch (Exception | Error e) {
                    Logger.error(RulesAPIImpl.class, e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 
     * @return
     */
    private List<Conditionlet> getCustomConditionlets() {
        //Get the Conditionlets form OSGI.
        List<Conditionlet> customConditionlets = Lists.newArrayList();

        for (Class<Conditionlet> z : conditionletOSGIclasses) {
            try {
                customConditionlets.add(z.newInstance());
            } catch (Exception | Error e) {
                Logger.error(RulesAPIImpl.class, e.getMessage(), e);
            }
        }

        return customConditionlets;
    }

    /**
     * 
     * @return
     */
    private List<Conditionlet> getDefaultConditionlets() {
        List<Conditionlet> instances = Lists.newArrayList();
        // get the included (shipped with) conditionlet classes
        for (Class<? extends Conditionlet> z : defaultConditionletClasses) {
            try {
                instances.add(z.newInstance());
            } catch (Exception | Error e) {
                Logger.error(RulesAPIImpl.class, e.getMessage(), e);
            }
        }
        return instances;
    }

    /**
     * 
     */
    private void initActionletMap() {
        synchronized (actionletMap) {
            // get the dotmarketing-config.properties actionlet classes
            List<RuleActionlet> defaultActionlets = getCustomActionlets();
            List<RuleActionlet> actionlets = Lists.newArrayList(defaultActionlets);
            actionlets.addAll(getDefaultActionlets(defaultActionlets));

            for (RuleActionlet actionlet : actionlets) {
                try {
                    Class<? extends RuleActionlet> clazz = actionlet.getClass();
                    RuleActionlet instance = clazz.newInstance();
                    String id = instance.getId();
                    if(!actionletMap.containsKey(id)) {
                        actionletMap.put(id, instance);
                    } else {
                        Logger.warn(RulesAPIImpl.class, "Actionlet with name '" + id + "' already registered.");
                    }
                } catch (InstantiationException | IllegalAccessException e) {
                    Logger.error(RulesAPIImpl.class, e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 
     * @param defaultActionlets
     * @return
     */
    private List<RuleActionlet> getDefaultActionlets(List<RuleActionlet> defaultActionlets) {
        List<RuleActionlet> instances = Lists.newArrayList(defaultActionlets);

        // get the included (shipped with) actionlet classes
        for (Class<? extends RuleActionlet> z : defaultActionletClasses) {
            try {
                instances.add(z.newInstance());
            } catch (Exception | Error e) {
                Logger.error(RulesAPIImpl.class, e.getMessage(), e);
            }
        }

        return instances;
    }

    /**
     * Loads the list of contentlets including OSGi ones.
     * @return
     */
    private List<RuleActionlet> getCustomActionlets() {
        List<RuleActionlet> instances = Lists.newArrayList();
        String customClassesStr = Config.getStringProperty(WebKeys.RULES_ACTIONLET_CLASSES, null);
        if(UtilMethods.isSet(customClassesStr)) {

            String[] st = customClassesStr.split(",");
            for (String className : st) {
                try {
                    RuleActionlet e1 = (RuleActionlet)Class.forName(className.trim()).newInstance();
                    instances.add(e1);
                } catch (Exception | Error e1) {
                    Logger.error(RulesAPIImpl.class, "Error instantiating class '" + className + "' " + e1.getMessage(), e1);
                }
            }
        }
        // includes OSGi actionlet
        for(Class<RuleActionlet> a : actionletOSGIclasses) {
            try {
            	instances.add(a.newInstance());
            } catch (Exception | Error e1) {
                Logger.error(RulesAPIImpl.class, "Error instantiating class '" + a.getClass() + "' " + e1.getMessage(), e1);
            }
        }

        return instances;
    }

    @Override
    public List<Conditionlet<?>> findConditionlets() throws DotDataException, DotSecurityException {
        return new ArrayList<>(conditionletMap.values());
    }

    @Override
    public Conditionlet findConditionlet(String clazz) {
        return conditionletMap.get(clazz);
    }

    @Override
    public List<RuleActionlet> findActionlets() throws DotDataException, DotSecurityException {
        return new ArrayList<>(actionletMap.values());
    }

    @Override
    public RuleActionlet findActionlet(String actionletId) {
        return actionletMap.get(actionletId);
    }

    @Override
    public void registerBundleService () {

        if(Config.getBooleanProperty("felix.osgi.enable", true)){
            // Register main service
            BundleContext context = HostActivator.instance().getBundleContext();
            Hashtable<String, String> props = new Hashtable<>();
            context.registerService(ConditionletOSGIService.class.getName(), this, props);
            context.registerService(RuleActionletOSGIService.class.getName(), this, props);
        }
    }

    /**
     * Adds a given Conditionlet class to the list of Rules Engine Conditionlet, this method will instantiate and
     * initialize (init method) the given Conditionlet.
     *
     * @param conditionletClass
     */
    @Override
    public String addConditionlet(Class conditionletClass) {

        if(!conditionletOSGIclasses.contains(conditionletClass)) {
            conditionletOSGIclasses.add(conditionletClass);
        }
        refreshConditionletsMap();
        return conditionletClass.getCanonicalName();
    }

    /**
     * Removes a given Conditionlet class from the list of Rules Engine Conditionlet.
     *
     * @param conditionletName
     */
    @Override
    public void removeConditionlet(String conditionletName) {

        Conditionlet conditionlet = conditionletMap.get(conditionletName);
        conditionletOSGIclasses.remove(conditionlet.getClass());
        conditionletMap.remove(conditionletName);
        refreshConditionletsMap();
    }

    @Override
    public String addRuleActionlet(Class actionletClass){

        actionletOSGIclasses.add(actionletClass);
    	refreshActionletsMap();
    	return actionletClass.getCanonicalName();
    }

    @Override
    public void removeRuleActionlet(String actionletName) {

        RuleActionlet actionlet = actionletMap.get(actionletName);
        actionletOSGIclasses.remove(actionlet.getClass());
        actionletMap.remove(actionletName);
        refreshActionletsMap();
    }

    /**
     * 
     */
    private void refreshActionletsMap() {
        synchronized (actionletMap) {
        	actionletMap.clear();
            //Get the OSGi ones.
            List<RuleActionlet> actionlets = Lists.newArrayList(getCustomActionlets());
            //Get the default ones.
            actionlets.addAll(getDefaultActionlets(new ArrayList<>()));

			for (RuleActionlet actionlet : actionlets) {
                try {
                    Class<? extends RuleActionlet> clazz = actionlet.getClass();
                    RuleActionlet instance = clazz.newInstance();
                    String id = instance.getId();
                    if(!actionletMap.containsKey(id)){
                    	actionletMap.putIfAbsent(id, instance);
                    }
                    else {
                        Logger.info(RulesAPIImpl.class, "Actionlet with name '" + clazz.getSimpleName() + "' already registered.");
                    }
                } catch (Exception | Error e) {
                    Logger.error(RulesAPIImpl.class, e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void disableRule(Rule rule, User user) throws DotDataException, DotSecurityException {
        rule.setEnabled(false);
        this.saveRule(rule, user, false);
    }

}
