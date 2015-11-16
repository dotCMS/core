package com.dotmarketing.portlets.rules.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RulesEngine {

    public void fireRules(HttpServletRequest req, HttpServletResponse res, Rule.FireOn fireOn) {

        Host host;

        try {
            host =  WebAPILocator.getHostWebAPI().getCurrentHost(req);
        } catch (Exception e) {
            Logger.error(RulesEngine.class, "Unable to retrieve current request host for URI ", e);
            return;
        }

        User systemUser;

        try {
            systemUser = WebAPILocator.getUserWebAPI().getSystemUser();
        } catch (DotDataException e) {
            Logger.error(RulesEngine.class, "Unable to get systemUser", e);
            return;
        }

        try {

            Set<Rule> rules = APILocator.getRulesAPI().getRulesByHostFireOn(host.getIdentifier(), systemUser, false, fireOn);

            for (Rule rule : rules) {
                boolean result = false;
                try {
                    result = rule.evaluate(req, res);
                } catch (DotDataException e) {
                    Logger.error(RulesEngine.class, "Rule could not be evaluated. Rule Id: " + rule.getId(), e);
                }

                // Let's execute the actions
                if(result) {
                    List<RuleAction> actions = APILocator.getRulesAPI().getRuleActionsByRule(rule.getId(), systemUser, false);

                    for (RuleAction action : actions) {
                        RuleActionlet actionlet = APILocator.getRulesAPI().findActionlet(action.getActionlet());
                        Map<String, RuleActionParameter> params = APILocator.getRulesAPI().getRuleActionParameters(action, systemUser, false);
                        actionlet.executeAction(req, res, params);
                    }
                }
            }

        } catch(Exception e) {
            Logger.error(RulesEngine.class, "Unable process rules." + e.getMessage(), e);
        }
    }
}
