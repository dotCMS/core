package com.dotmarketing.portlets.rules.business;

import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.rules.exception.RuleEngineException;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class RulesEngine {
	private static int SLOW_RULE_LOG_MIN=Config.getIntProperty("SLOW_RULE_LOG_MIN", 100);

    public static void fireRules(HttpServletRequest req, HttpServletResponse res, Rule.FireOn fireOn) {

        //Check for the proper license level, the rules engine is an enterprise feature only
        if ( LicenseUtil.getLevel() < 200 ) {
            return;
        }

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

            Set<Rule> rules = APILocator.getRulesAPI().getRulesByParentFireOn(host.getIdentifier(), systemUser, false, fireOn);
            System.out.println("rules.size() = " + rules.size());

            for (Rule rule : rules) {
                try {
                	long before = System.currentTimeMillis();
                    rule.checkValid(); // @todo ggranum: this should actually be done on writing to the DB, or at worst reading from.
                    rule.evaluate(req, res);
                    long after = System.currentTimeMillis();
        			if((after - before) > SLOW_RULE_LOG_MIN) {
        				Logger.warn(RulesEngine.class, "Rule ID:"+rule.getId()+" is running too slow. The rule is fired on: "+ rule.getFireOn().name());
        			}
                } catch (RuleEngineException e) {
                    Logger.error(RulesEngine.class, "Rule could not be evaluated. Rule Id: " + rule.getId(), e);
                }
            }

        } catch(Exception e) {
            Logger.error(RulesEngine.class, "Unable process rules." + e.getMessage(), e);
        }
    }
}
