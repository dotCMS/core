package com.dotmarketing.portlets.rules;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotcms.enterprise.rules.RulesAPI;
//import com.dotmarketing.portlets.rules.conditionlet.MockTrueConditionlet;
import com.dotmarketing.portlets.rules.model.Rule;
import com.liferay.portal.model.User;

/**
 * @author Geoff M. Granum
 */
public class RuleDataGen {

    private static final RulesAPI rulesAPI = APILocator.getRulesAPI();
    private static final HostAPI hostAPI = APILocator.getHostAPI();
    private static final User user;
    private static final Host defaultHost;

    static {
        try {
            user = APILocator.getUserAPI().getSystemUser();
            defaultHost = hostAPI.findDefaultHost(user, false);
//            rulesAPI.addConditionlet(MockTrueConditionlet.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Rule.FireOn fireOn = Rule.FireOn.EVERY_PAGE;
    private String name = "defaultName";
    private Host host;

    public RuleDataGen() {
    }

    public RuleDataGen(Rule.FireOn fireOn) {
        this.fireOn = fireOn;
    }

    public Rule next() {

        Rule rule = new Rule();
        rule.setName(name);
        rule.setParent(host != null ? host.getIdentifier() : defaultHost.getIdentifier());
        rule.setEnabled(true);
        rule.setFireOn(fireOn);
        return rule;
    }

    public Rule nextPersisted() {
        return persist(next());
    }

    public Rule persist(Rule rule) {
        try {
            rulesAPI.saveRule(rule, user, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rule;
    }

    public void remove(Rule rule){
        try {
            rulesAPI.deleteRule(rule, user, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RuleDataGen name(String name) {
        this.name = name;
        return this;
    }

    public RuleDataGen host(final Host host) {
        this.host = host;
        return this;
    }
}
 
