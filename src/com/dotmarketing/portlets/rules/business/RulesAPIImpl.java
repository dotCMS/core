package com.dotmarketing.portlets.rules.business;

import com.dotcms.repackage.edu.emory.mathcs.backport.java.util.*;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
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

        if (!APILocator.getPermissionAPI().doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId());
        }
        return rule;
    }

    public void deleteRule(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException  {
        if (!APILocator.getPermissionAPI().doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
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

        if (!APILocator.getPermissionAPI().doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId());
        }
        return rulesFactory.getConditionGroupsByRule(ruleId);
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

        if (!APILocator.getPermissionAPI().doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId());
        }
        return rulesFactory.getConditionsByRule(ruleId);
    }

    public Condition getConditionById(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Condition condition = rulesFactory.getConditionById(id);
        Rule rule = rulesFactory.getRuleById(condition.getRuleId());

        if (!APILocator.getPermissionAPI().doesUserHavePermission(rule, PermissionAPI.PERMISSION_USE, user, true)) {
            throw new DotSecurityException("User " + user + " cannot read rule: " + rule.getId() + " including any of its conditions");
        }

        return condition;
    }

    public void saveRule(Rule rule, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if (!APILocator.getPermissionAPI().doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot save rule: " + rule.getId());
        }

        rulesFactory.saveRule(rule);
    }

    public void saveCondition(Condition condition, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Rule rule = rulesFactory.getRuleById(condition.getRuleId());

        if (!APILocator.getPermissionAPI().doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot save rule: " + rule.getId() + " or its conditions ");
        }

        rulesFactory.saveCondition(condition);
    }

    public void deleteCondition(Condition condition, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        Rule rule = rulesFactory.getRuleById(condition.getRuleId());

        if (!APILocator.getPermissionAPI().doesUserHavePermission(rule, PermissionAPI.PERMISSION_EDIT, user, true)) {
            throw new DotSecurityException("User " + user + " cannot delete rule: " + rule.getId() + " or its conditions ");
        }

        rulesFactory.deleteCondition(condition);
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
                    for(Conditionlet actionlet : conditionletList){

                        try {
                            conditionletMap.put(actionlet.getClass().getCanonicalName(),actionlet.getClass().newInstance());
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


    private void refreshActionletMap() {
        actionletMap = null;
        if (actionletMap == null) {
            synchronized (this.getClass()) {
                if (actionletMap == null) {

                    // get the dotmarketing-config.properties actionlet classes
                    List<RuleActionlet> conditionletList = getCustomClasses(WebKeys.RULES_ACTIONLET_CLASSES);

                    // get the included (shipped with) actionlet classes
                    for (Class<RuleActionlet> z : actionletClasses) {
                        try {
                            conditionletList.add(z.newInstance());
                        } catch (InstantiationException e) {
                            Logger.error(RulesAPIImpl.class, e.getMessage(), e);
                        } catch (IllegalAccessException e) {
                            Logger.error(RulesAPIImpl.class, e.getMessage(), e);
                        }
                    }

                    Collections.sort(conditionletList, new ActionletComparator());
                    actionletMap = new LinkedHashMap<String, RuleActionlet>();
                    for(RuleActionlet actionlet : conditionletList){

                        try {
                            actionletMap.put(actionlet.getClass().getCanonicalName(),actionlet.getClass().newInstance());
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
}
