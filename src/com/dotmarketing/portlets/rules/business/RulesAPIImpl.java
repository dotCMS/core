package com.dotmarketing.portlets.rules.business;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.actionlet.CountRequestsActionlet;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.actionlet.TestActionlet;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersBrowserConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersBrowserHeaderConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersCityConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersCountryConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersCurrentUrlConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersDateTimeConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersHostConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersIpAddressConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersLandingPageUrlConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersLanguageConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersLogInConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersOperatingSystemConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersPageVisitsConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersPlatformConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersReferringUrlConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersSiteVisitsConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersStateConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersTimeConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersUrlParameterConditionlet;
import com.dotmarketing.portlets.rules.conditionlet.UsersVisitedUrlConditionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull;

public class RulesAPIImpl implements RulesAPI {

    private static final Map<String, Conditionlet> conditionletMap = Maps.newHashMap();
    private static final Map<String, RuleActionlet> actionletMap = Maps.newHashMap();
    private final PermissionAPI perAPI;
    private final RulesFactory rulesFactory;
    private final List<Class<? extends Conditionlet>> defaultConditionletClasses =
            ImmutableList.<Class<? extends Conditionlet>>builder()
                         .add(UsersBrowserConditionlet.class)
                         .add(UsersBrowserHeaderConditionlet.class)
                         .add(UsersCityConditionlet.class)
                         .add(UsersCountryConditionlet.class)
                         .add(UsersCurrentUrlConditionlet.class)
                         .add(UsersDateTimeConditionlet.class)
                         .add(UsersHostConditionlet.class)
                         .add(UsersIpAddressConditionlet.class)
                         .add(UsersLandingPageUrlConditionlet.class)
                         .add(UsersLanguageConditionlet.class)
                         .add(UsersLogInConditionlet.class)
                         .add(UsersOperatingSystemConditionlet.class)
                         .add(UsersPageVisitsConditionlet.class)
                         .add(UsersPlatformConditionlet.class)
                         .add(UsersReferringUrlConditionlet.class)
                         .add(UsersSiteVisitsConditionlet.class)
                         .add(UsersStateConditionlet.class)
                         .add(UsersTimeConditionlet.class)
                         .add(UsersUrlParameterConditionlet.class)
                         .add(UsersVisitedUrlConditionlet.class)
                         .build();
    private final List<Class<? extends RuleActionlet>> defaultActionletClasses =
            ImmutableList.<Class<? extends RuleActionlet>>builder()
                         .add(CountRequestsActionlet.class)
                         .add(TestActionlet.class).build();

    public RulesAPIImpl() {
        perAPI = APILocator.getPermissionAPI();
        rulesFactory = FactoryLocator.getRulesFactory();
        initConditionlets();
        initActionletMap();
    }

    public List<Rule> getRulesByHost(String host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(host)) {
            return new ArrayList<>();
        }

        return perAPI.filterCollection(rulesFactory.getRulesByHost(host), PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
    }

    public Set<Rule> getRulesByHost(String host, User user, boolean respectFrontendRoles, Rule.FireOn fireOn) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(host)) {
            return new HashSet<>();
        }

        return new HashSet<>(perAPI.filterCollection(new ArrayList<>(rulesFactory.getRulesByHost(host, fireOn)), PermissionAPI.PERMISSION_READ, respectFrontendRoles, user));
    }

    public List<Rule> getRulesByFolder(String folder, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(folder)) {
            return new ArrayList<>();
        }

        return perAPI.filterCollection(rulesFactory.getRulesByHost(folder), PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
    }

    public List<Rule> getRulesByNameFilter(String nameFilter, User user, boolean respectFrontendRoles) {
        return null;
    }

    public Rule getRuleById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(id)) {
            return null;
        }

        Rule rule = rulesFactory.getRuleById(id);

        if(!UtilMethods.isSet(rule)) {
            Logger.info(this, "There is no rule with the given id: " + id);
            return null;
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId());
        }
        return rule;
    }

    public void deleteRule(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException  {
        if(!UtilMethods.isSet(rule)) {
            return;
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot delete rule: " + rule.getId());
        }

        // delete the Condition Groups of the rule first

        List<ConditionGroup> groups = rulesFactory.getConditionGroupsByRule(rule.getId());

        for (ConditionGroup group : groups) {
            deleteConditionGroup(group, user, respectFrontendRoles);
        }

        // delete the Rule Actions

        deleteRuleActionsByRule(rule, user);

        // delete the Rule
        rulesFactory.deleteRule(rule);
    }

    public void deleteRuleActionsByRule(Rule rule, User user) throws DotDataException, DotSecurityException  {
        if(!UtilMethods.isSet(rule)) {
            return;
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot delete rule: " + rule.getId());
        }

        List<RuleAction> actions = rulesFactory.getRuleActionsByRule(rule.getId());

        // delete action parameters
        for (RuleAction action : actions) {
            rulesFactory.deleteRuleActionsParameters(action);

            // delete the action
            rulesFactory.deleteRuleAction(action);
        }

    }

    public List<ConditionGroup> getConditionGroupsByRule(String ruleId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(ruleId)) {
            return new ArrayList<>();
        }

        Rule rule = rulesFactory.getRuleById(ruleId);

        if(!UtilMethods.isSet(rule)) {
            Logger.info(this, "There is no rule with the given id: " + ruleId);
            return new ArrayList<>();
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId());
        }
        return rulesFactory.getConditionGroupsByRule(ruleId);
    }

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

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId() + " including any of its conditions/groups");
        }

        return conditionGroup;
    }

    public List<RuleAction> getRuleActionsByRule(String ruleId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(ruleId)) {
            return new ArrayList<>();
        }

        Rule rule = rulesFactory.getRuleById(ruleId);

        if(!UtilMethods.isSet(rule)) {
            Logger.info(this, "There is no rule with the given id: " + ruleId);
            return new ArrayList<>();
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId());
        }
        return rulesFactory.getRuleActionsByRule(ruleId);
    }

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

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId() + " including any of its conditions/groups");
        }

        return action;
    }

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

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId());
        }

        return rulesFactory.getConditionsByGroup(conditionGroup.getId());
    }

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

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId() + " including any of its conditions");
        }

        return condition;
    }

    @Override
    public ConditionValue getConditionValueById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(id)) {
            return null;
        }

        ConditionValue value = rulesFactory.getConditionValueById(id);

        if(!UtilMethods.isSet(value)) {
            Logger.info(this, "There is no Condition Value with the given id: " + id);
            return null;
        }

        Condition condition = rulesFactory.getConditionById(value.getConditionId());

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

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId());
        }
        return value;

    }

    public void saveRule(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        rule = checkNotNull(rule, "Rule is required.");
        user = checkNotNull(user, "User is required.");

        if (!perAPI.doesUserHavePermissions(PermissionAPI.PermissionableType.RULES, PermissionAPI.PERMISSION_EDIT, user)) {
            throw new DotSecurityException("User " + user + " does not have permissions to Edit Rules");
        }

        rulesFactory.saveRule(rule);
    }

    public void saveConditionGroup(ConditionGroup conditionGroup, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        conditionGroup = checkNotNull(conditionGroup, "ConditionGroup is required.");
        Rule rule = checkNotNull(rulesFactory.getRuleById(conditionGroup.getRuleId()), "There is no rule with the given id: " + conditionGroup.getRuleId());

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot save rule: " + rule.getId() + " or its groups/conditions ");
        }

        rulesFactory.saveConditionGroup(conditionGroup);
    }

    public void saveCondition(Condition condition, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        condition = checkNotNull(condition, "Condition is required.");

        ConditionGroup group = checkNotNull(getConditionGroupById(condition.getConditionGroup(), user, true),
                                                   "Invalid ConditionGroup specified: %s",
                                                   condition.getConditionGroup());

        Rule rule = rulesFactory.getRuleById(group.getRuleId()); // Can only be null if there is a schema integrity failure.

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot save rule: " + rule.getId() + " or its conditions ");
        }

        rulesFactory.saveCondition(condition);
    }

    public void saveRuleAction(RuleAction ruleAction, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        ruleAction = checkNotNull(ruleAction, "RuleAction is required");
        user = checkNotNull(user, "User is required");

        Rule rule = rulesFactory.getRuleById(ruleAction.getRuleId());

        if(rule==null) {
            Logger.info(this, "There is no rule with the given id: " + ruleAction.getRuleId());
            throw new DotDataException("There is no Rule with the provided ruleId: "+ruleAction.getRuleId());
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot edit rule: " + rule.getId());
        }

        rulesFactory.saveRuleAction(ruleAction);
    }

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

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot delete rule: " + rule.getId() + " or its conditions ");
        }

        // delete the condition values
        rulesFactory.deleteConditionValues(condition);

        // delete the condition
        rulesFactory.deleteCondition(condition);
    }

    public void deleteConditions(ConditionGroup group, User user) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(group)) {
            return;
        }

        Rule rule = rulesFactory.getRuleById(group.getRuleId());

        if(rule==null) {
            Logger.info(this, "There is no rule with the given id: " + group.getRuleId());
            throw new DotDataException("There is no Rule with the provided ruleId: " + group.getRuleId());
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot delete rule: " + rule.getId() + " or its conditions ");
        }

        // delete the condition values
        for (Condition condition : group.getConditions()) {
            rulesFactory.deleteConditionValues(condition);
        }

        // delete the condition
        rulesFactory.deleteConditionsByGroup(group);
    }

    public void deleteConditionGroup(ConditionGroup conditionGroup, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(conditionGroup)) {
            return;
        }

        Rule rule = rulesFactory.getRuleById(conditionGroup.getRuleId());

        if(!UtilMethods.isSet(rule)) {
            Logger.info(this, "There is no rule with the given id: " + conditionGroup.getRuleId());
            return;
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot delete rule: " + rule.getId() + " or its conditions ");
        }

        // delete the conditions

        deleteConditions(conditionGroup, user);

        rulesFactory.deleteConditionGroup(conditionGroup);
    }

    public void deleteRuleAction(RuleAction ruleAction, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(ruleAction)) {
            return;
        }

        Rule rule = rulesFactory.getRuleById(ruleAction.getRuleId());

        if(!UtilMethods.isSet(rule)) {
            Logger.info(this, "There is no rule with the given id: " + ruleAction.getRuleId());
            return;
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot delete rule: " + rule.getId() + " or its conditions ");
        }

        rulesFactory.deleteRuleActionsParameters(ruleAction);

        rulesFactory.deleteRuleAction(ruleAction);
    }

    public Map<String, RuleActionParameter> getRuleActionParameters(RuleAction action, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        String ruleId = action.getRuleId();

        if (!UtilMethods.isSet(ruleId)) {
            return new HashMap<>();
        }

        Rule rule = rulesFactory.getRuleById(ruleId);

        if (!UtilMethods.isSet(rule)) {
            Logger.info(this, "There is no rule with the given id: " + action.getRuleId());
            return new HashMap<>();
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId());
        }
        return rulesFactory.getRuleActionParameters(action);
    }

    public RuleActionParameter getRuleActionParameterById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(id)) {
            return null;
        }

        RuleActionParameter parameter = rulesFactory.getRuleActionParameterById(id);

        if(!UtilMethods.isSet(parameter)) {
            Logger.info(this, "There is no RuleAction Parameter with the given id: " + id);
            return null;
        }

        RuleAction action = rulesFactory.getRuleActionById(parameter.getRuleActionId());

        if(action==null) {
            Logger.info(this, "There is no RuleAction associated with the given RuleAction Parameter: " + id);
            return null;
        }

        Rule rule = rulesFactory.getRuleById(action.getRuleId());

        if(rule==null) {
            Logger.info(this, "There is no rule associated with the given RuleAction Parameter: " + id);
            return null;
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId());
        }
        return parameter;
    }

    private void initConditionlets() {
        synchronized (conditionletMap) {
            // get the dotmarketing-config.properties conditionlet classes
            List<Conditionlet> conditionlets = Lists.newArrayList(getCustomConditionlets());
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
                        Logger.warn(RulesAPIImpl.class, "Conditionlet with name '" + clazz.getSimpleName() + "' already registered.");
                    }
                } catch (InstantiationException | IllegalAccessException e) {
                    Logger.error(RulesAPIImpl.class, e.getMessage(), e);
                }
            }
        }
    }

    private List<Conditionlet> getCustomConditionlets() {
        List<Conditionlet> customClasses = Lists.newArrayList();
        String customClassesStr = Config.getStringProperty(WebKeys.RULES_CONDITIONLET_CLASSES, null, false);
        if(customClassesStr != null) {

            String[] st = customClassesStr.split(",");
            for (String className : st) {
                try {
                    Conditionlet e = (Conditionlet)Class.forName(className.trim()).newInstance();
                    customClasses.add(e);
                } catch (Exception e1) {
                    Logger.error(RulesAPIImpl.class, "Error instantiating class '" + className + "' " + e1.getMessage(), e1);
                }
            }
        }
        return customClasses;
    }

    private List<Conditionlet> getDefaultConditionlets() {
        List<Conditionlet> instances = Lists.newArrayList();
        // get the included (shipped with) conditionlet classes
        for (Class<? extends Conditionlet> z : defaultConditionletClasses) {
            try {
                instances.add(z.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                Logger.error(RulesAPIImpl.class, e.getMessage(), e);
            }
        }
        return instances;
    }

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

    private List<RuleActionlet> getDefaultActionlets(List<RuleActionlet> defaultActionlets) {
        List<RuleActionlet> instances = Lists.newArrayList(defaultActionlets);

        // get the included (shipped with) actionlet classes
        for (Class<? extends RuleActionlet> z : defaultActionletClasses) {
            try {
                instances.add(z.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                Logger.error(RulesAPIImpl.class, e.getMessage(), e);
            }
        }
        return instances;
    }

    private List<RuleActionlet> getCustomActionlets() {
        List<RuleActionlet> instances = Lists.newArrayList();
        String customClassesStr = Config.getStringProperty(WebKeys.RULES_ACTIONLET_CLASSES, null, false);
        if(customClassesStr != null) {

            String[] st = customClassesStr.split(",");
            for (String className : st) {
                try {
                    RuleActionlet e1 = (RuleActionlet)Class.forName(className.trim()).newInstance();
                    instances.add(e1);
                } catch (Exception e1) {
                    Logger.error(RulesAPIImpl.class, "Error instantiating class '" + className + "' " + e1.getMessage(), e1);
                }
            }
        }
        return instances;
    }

    public List<Conditionlet> findConditionlets() throws DotDataException, DotSecurityException {
        return new ArrayList<>(conditionletMap.values());
    }

    public Conditionlet findConditionlet(String clazz) throws DotDataException, DotSecurityException {
        return conditionletMap.get(clazz);
    }

    public List<RuleActionlet> findActionlets() throws DotDataException, DotSecurityException {
        return new ArrayList<>(actionletMap.values());
    }

    public RuleActionlet findActionlet(String actionletId) throws DotDataException, DotSecurityException {
        return actionletMap.get(actionletId);
    }
}
