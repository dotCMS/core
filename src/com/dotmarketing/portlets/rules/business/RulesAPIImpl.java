package com.dotmarketing.portlets.rules.business;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.actionlet.*;
import com.dotmarketing.portlets.rules.conditionlet.*;
import com.dotmarketing.portlets.rules.model.*;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

import java.util.*;
import java.util.Arrays;
import java.util.Collections;

public class RulesAPIImpl implements RulesAPI {

    private PermissionAPI perAPI;
    private RulesFactory rulesFactory;

    private List<Class> conditionletClasses;
    private List<Class> actionletClasses;

    private static Map<String, Conditionlet> conditionletMap;
    private static Map<String, RuleActionlet> actionletMap;

    public RulesAPIImpl() {
        perAPI = APILocator.getPermissionAPI();
        rulesFactory = FactoryLocator.getRulesFactory();

        conditionletClasses = new ArrayList<Class>();
        actionletClasses = new ArrayList<Class>();

        // Add default conditionlet classes
        conditionletClasses.addAll(Arrays.asList(new Class[]{
                UsersCountryConditionlet.class,
                UsersCityConditionlet.class,
                UsersStateConditionlet.class,
                UsersLanguageConditionlet.class,
                UsersBrowserConditionlet.class,
                UsersHostConditionlet.class,
                UsersIpAddressConditionlet.class,
                UsersOperatingSystemConditionlet.class,
                UsersSiteVisitsConditionlet.class,
                UsersVisitedUrlConditionlet.class,
                MockTrueConditionlet.class
        }));

        actionletClasses.addAll(Arrays.asList(new Class[]{
                CountRequestsActionlet.class,
                TestActionlet.class
        }));

        refreshConditionletMap();
        refreshActionletMap();
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

        List<ConditionGroup> auxList = new ArrayList<>(groups);

        for (int i = 0; i < auxList.size(); i++) {

            ConditionGroup group = groups.get(i);

            deleteConditionGroup(group, user, respectFrontendRoles);
        }

        // delete the Rule Actions

        deleteRuleActionsByRule(rule, user, respectFrontendRoles);

        // delete the Rule
        rulesFactory.deleteRule(rule);
    }

    public void deleteRuleActionsByRule(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException  {
        if(!UtilMethods.isSet(rule)) {
            return;
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot delete rule: " + rule.getId());
        }

        List<RuleAction> actions = rulesFactory.getRuleActionsByRule(rule.getId());

        List<RuleAction> aux = new ArrayList<>(actions);

        // delete action parameters
        for (RuleAction action : aux) {
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

    public List<Condition> getConditionsByRule(String ruleId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Rule rule = rulesFactory.getRuleById(ruleId);

        if(!UtilMethods.isSet(rule)) {
            Logger.info(this, "There is no rule with the given id: " + ruleId);
            return new ArrayList<>();
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId());
        }
        return rulesFactory.getConditionsByRule(ruleId);
    }

    public Condition getConditionById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Condition condition = rulesFactory.getConditionById(id);

        if(condition==null) {
            Logger.info(this, "There is no condition with the given id: " + id);
            return null;
        }

        Rule rule = rulesFactory.getRuleById(condition.getRuleId());

        if(rule==null) {
            Logger.info(this, "There is no rule with the given id: " + condition.getRuleId());
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

        Rule rule = rulesFactory.getRuleById(condition.getRuleId());

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
        if(!UtilMethods.isSet(rule))
            return;

        if (!perAPI.doesUserHavePermissions(PermissionAPI.PermissionableType.RULES, PermissionAPI.PERMISSION_EDIT, user)) {
            throw new DotSecurityException("User " + user + " does not have permissions to Edit Rules");
        }

        rulesFactory.saveRule(rule);
    }

    public void saveConditionGroup(ConditionGroup conditionGroup, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(conditionGroup))
            return;

        Rule rule = rulesFactory.getRuleById(conditionGroup.getRuleId());

        if(rule==null) {
            Logger.info(this, "There is no rule with the given id: " + conditionGroup.getRuleId());
            return;
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot save rule: " + rule.getId() + " or its groups/conditions ");
        }

        rulesFactory.saveConditionGroup(conditionGroup);
    }

    public void saveCondition(Condition condition, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(condition))
            return;

        Rule rule = rulesFactory.getRuleById(condition.getRuleId());

        if(rule==null) {
            Logger.info(this, "There is no rule with the given id: " + condition.getRuleId());
            return;
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot save rule: " + rule.getId() + " or its conditions ");
        }

        rulesFactory.saveCondition(condition);
    }

    public void saveRuleAction(RuleAction ruleAction, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(ruleAction))
            return;

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

        Rule rule = rulesFactory.getRuleById(condition.getRuleId());

        if(rule==null) {
            Logger.info(this, "There is no rule with the given id: " + condition.getRuleId());
            throw new DotDataException("There is no Rule with the provided ruleId: " + condition.getRuleId());
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot delete rule: " + rule.getId() + " or its conditions ");
        }

        // delete the condition values
        rulesFactory.deleteConditionValues(condition);

        // delete the condition
        rulesFactory.deleteCondition(condition);
    }

    public void deleteConditions(ConditionGroup group, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
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

        deleteConditions(conditionGroup, user, respectFrontendRoles);

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

    private void refreshConditionletMap() {
        conditionletMap = null;
        if (conditionletMap == null) {
            synchronized (this.getClass()) {
                if (conditionletMap == null) {

                    // get the dotmarketing-config.properties conditionlet classes
                    List<Conditionlet> conditionletList = getCustomClasses(WebKeys.RULES_CONDITIONLET_CLASSES);

                    // get the included (shipped with) actionlet classes
                    for (Class<Conditionlet> z : conditionletClasses) {
                        try {
                            conditionletList.add(z.newInstance());
                        } catch (InstantiationException e) {
                            Logger.error(RulesAPIImpl.class, e.getMessage(), e);
                        } catch (IllegalAccessException e) {
                            Logger.error(RulesAPIImpl.class, e.getMessage(), e);
                        }
                    }

                    Collections.sort(conditionletList, new ConditionletComparator());
                    conditionletMap = new LinkedHashMap<String, Conditionlet>();
                    for(Conditionlet conditionlet : conditionletList){

                        try {
                            conditionletMap.put(conditionlet.getClass().getSimpleName(),conditionlet.getClass().newInstance());
                            if ( !conditionletClasses.contains( conditionlet.getClass() ) ) {
                                conditionletClasses.add( conditionlet.getClass() );
                            }
                        } catch (InstantiationException e) {
                            Logger.error(RulesAPIImpl.class,e.getMessage(),e);
                        } catch (IllegalAccessException e) {
                            Logger.error(RulesAPIImpl.class,e.getMessage(),e);
                        }
                    }
                }
            }

        }
    }


    private void refreshActionletMap() {
        actionletMap = null;
        if (actionletMap == null) {
            synchronized (this.getClass()) {
                if (actionletMap == null) {

                    // get the dotmarketing-config.properties actionlet classes
                    List<RuleActionlet> actionletList = getCustomClasses(WebKeys.RULES_ACTIONLET_CLASSES);

                    // get the included (shipped with) actionlet classes
                    for (Class<RuleActionlet> z : actionletClasses) {
                        try {
                            actionletList.add(z.newInstance());
                        } catch (InstantiationException e) {
                            Logger.error(RulesAPIImpl.class, e.getMessage(), e);
                        } catch (IllegalAccessException e) {
                            Logger.error(RulesAPIImpl.class, e.getMessage(), e);
                        }
                    }

                    Collections.sort(actionletList, new ActionletComparator());
                    actionletMap = new LinkedHashMap<String, RuleActionlet>();
                    for(RuleActionlet actionlet : actionletList){

                        try {
                            actionletMap.put(actionlet.getClass().getSimpleName(),actionlet.getClass().newInstance());
                            if ( !actionletClasses.contains( actionlet.getClass() ) ) {
                                actionletClasses.add( actionlet.getClass() );
                            }
                        } catch (InstantiationException e) {
                            Logger.error(RulesAPIImpl.class,e.getMessage(),e);
                        } catch (IllegalAccessException e) {
                            Logger.error(RulesAPIImpl.class,e.getMessage(),e);
                        }
                    }
                }
            }

        }
    }

    private <E> List<E> getCustomClasses(String webKey) {
        List<E> customClasses = new ArrayList<E>();
        String customClassesStr = Config.getStringProperty(webKey);

        StringTokenizer st = new StringTokenizer(customClassesStr, ",");
        while (st.hasMoreTokens()) {
            String clazz = st.nextToken();
            try {
                E e = (E) Class.forName(clazz.trim()).newInstance();
                customClasses.add(e);
            } catch (Exception e1) {
                Logger.error(RulesAPIImpl.class, e1.getMessage(), e1);
            }
        }

        return customClasses;
    }


    private class ConditionletComparator implements Comparator<Conditionlet>{

        public int compare(Conditionlet o1, Conditionlet o2) {
            return o1.getLocalizedName().compareTo(o2.getLocalizedName());

        }

    }

    private class ActionletComparator implements Comparator<RuleActionlet>{

        public int compare(RuleActionlet o1, RuleActionlet o2) {
            return o1.getLocalizedName().compareTo(o2.getLocalizedName());

        }

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

    public RuleActionlet findActionlet(String clazz) throws DotDataException, DotSecurityException {
        return actionletMap.get(clazz);
    }
}
