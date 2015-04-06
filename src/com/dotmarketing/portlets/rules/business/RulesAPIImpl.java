package com.dotmarketing.portlets.rules.business;

import com.dotcms.repackage.edu.emory.mathcs.backport.java.util.*;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.conditionlet.VisitorsCountryConditionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
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
                VisitorsCountryConditionlet.class
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

        // delete the conditions of the rule first

        List<Condition> conditions = rulesFactory.getConditionsByRule(rule.getId());

        for (Condition condition : conditions) {
            rulesFactory.deleteCondition(condition);
        }

        rulesFactory.deleteRule(rule);
    }

    public List<ConditionGroup> getConditionGroupsByRule(String ruleId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(ruleId)) {
            return new ArrayList<>();
        }

        Rule rule = rulesFactory.getRuleById(ruleId);

        if(!UtilMethods.isSet(rule)) {
            return new ArrayList<>();
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId());
        }
        return rulesFactory.getConditionGroupsByRule(ruleId);
    }

    public ConditionGroup getConditionGroupById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        ConditionGroup conditionGroup = rulesFactory.getConditionGroupById(id);

        Rule rule = rulesFactory.getRuleById(conditionGroup.getRuleId());

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
            return new ArrayList<>();
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId());
        }
        return rulesFactory.getRuleActionsByRule(ruleId);
    }

    public RuleAction getRuleActionById(String ruleActionId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        RuleAction action = rulesFactory.getRuleActionById(ruleActionId);

        Rule rule = rulesFactory.getRuleById(action.getRuleId());

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
            return new ArrayList<>();
        }

        return null; // TODO working on this one
    }

    public List<Condition> getConditionsByRule(String ruleId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Rule rule = rulesFactory.getRuleById(ruleId);

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId());
        }
        return rulesFactory.getConditionsByRule(ruleId);
    }

    public Condition getConditionById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Condition condition = rulesFactory.getConditionById(id);
        Rule rule = rulesFactory.getRuleById(condition.getRuleId());

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId() + " including any of its conditions");
        }

        return condition;
    }

    public void saveRule(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if (!perAPI.doesUserHavePermissions(PermissionAPI.PermissionableType.RULES, PermissionAPI.PERMISSION_EDIT, user)) {
            throw new DotSecurityException("User " + user + " does not have permissions to Edit Rules");
        }

        rulesFactory.saveRule(rule);
    }

    public void saveCondition(Condition condition, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Rule rule = rulesFactory.getRuleById(condition.getRuleId());

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot save rule: " + rule.getId() + " or its conditions ");
        }

        rulesFactory.saveCondition(condition);
    }

    public void deleteCondition(Condition condition, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(condition)) {
            return;
        }

        Rule rule = rulesFactory.getRuleById(condition.getRuleId());

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot delete rule: " + rule.getId() + " or its conditions ");
        }

        rulesFactory.deleteCondition(condition);
    }

    public void deleteConditionGroup(ConditionGroup conditionGroup, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(conditionGroup)) {
            return;
        }

        Rule rule = rulesFactory.getRuleById(conditionGroup.getRuleId());

        if(!UtilMethods.isSet(rule)) {
            return;
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot delete rule: " + rule.getId() + " or its conditions ");
        }

        rulesFactory.deleteConditionsByGroup(conditionGroup);

        rulesFactory.deleteConditionGroup(conditionGroup);
    }

    public void deleteRuleAction(RuleAction ruleAction, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if(!UtilMethods.isSet(ruleAction)) {
            return;
        }

        Rule rule = rulesFactory.getRuleById(ruleAction.getRuleId());

        if(!UtilMethods.isSet(rule)) {
            return;
        }

        if (!perAPI.doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot delete rule: " + rule.getId() + " or its conditions ");
        }

        rulesFactory.deleteRuleAction(ruleAction);
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
