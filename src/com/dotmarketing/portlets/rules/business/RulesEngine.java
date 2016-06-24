package com.dotmarketing.portlets.rules.business;

import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.rules.exception.RuleEngineException;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Provides the entry point to trigger the execution of user-specified Rules for
 * a given parent (such as, a Site or Host). Rules can be kicked off under the
 * following conditions:
 * <ul>
 * <li>Every time a page is visited.</li>
 * <li>Every request.</li>
 * <li>Once per visitor.</li>
 * <li>Once per visit to a site.</li>
 * </ul>
 * 
 * @author Daniel Silva
 * @version 1.0
 * @since Apr 24, 2015
 *
 */
public final class RulesEngine {
	
	private static int SLOW_RULE_LOG_MIN=Config.getIntProperty("SLOW_RULE_LOG_MIN", 100);
	
	private static final String SKIP_RULES_EXECUTION = "skip"; 

	/**
	 * Triggers a specific category of Rules associated to the site (Host) based
	 * on the requested resource.
	 * 
	 * @param req
	 *            - The {@link HttpServletRequest} object.
	 * @param res
	 *            - The {@link HttpServletResponse} object.
	 * @param fireOn
	 *            - The category of the rules that will be fired: Every page,
	 *            every request, etc.
	 */
	public static void fireRules(HttpServletRequest req, HttpServletResponse res, Rule.FireOn fireOn) {
		Host host;
		try {
			host = WebAPILocator.getHostWebAPI().getCurrentHost(req);
		} catch (Exception e) {
			Logger.error(RulesEngine.class, "Unable to retrieve current request host for URI: " + req.getRequestURL(), e);
			return;
		}
		fireRules(req, res, host, fireOn);
	}
	
	/**
	 * Triggers a specific category of Rules associated to the specified parent
	 * object based on the requested resource.
	 * 
	 * @param req
	 *            - The {@link HttpServletRequest} object.
	 * @param res
	 *            - The {@link HttpServletResponse} object.
	 * @param parent
	 *            - The object whose associated rules will be fired. For
	 *            example, the {@link Host} object.
	 * @param fireOn
	 *            - The category of the rules that will be fired: Every page,
	 *            every request, etc.
	 */
	public static void fireRules(HttpServletRequest req, HttpServletResponse res, Ruleable parent, Rule.FireOn fireOn) {

        //Check for the proper license level, the rules engine is an enterprise feature only
        if ( LicenseUtil.getLevel() < 200 ) {
            return;
        }
        if (!UtilMethods.isSet(req)) {
        	throw new DotRuntimeException("ERROR: HttpServletRequest is null");
        }
		if (SKIP_RULES_EXECUTION.equalsIgnoreCase(req.getParameter(WebKeys.RULES_ENGINE_PARAM))
				|| SKIP_RULES_EXECUTION.equalsIgnoreCase(String.valueOf(req.getParameter(WebKeys.RULES_ENGINE_PARAM)))) {
			return;
		}
        if (!UtilMethods.isSet(parent)) {
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

			Set<Rule> rules = APILocator.getRulesAPI().getRulesByParentFireOn(parent.getIdentifier(), systemUser, false,
					fireOn);
            for (Rule rule : rules) {
                try {
                	long before = System.currentTimeMillis();
                    rule.checkValid(); // @todo ggranum: this should actually be done on writing to the DB, or at worst reading from.
                    boolean evaled = rule.evaluate(req, res);

					if (evaled) {
						Rule rCopy = new Rule();
						rCopy.setId(rule.getId());
						rCopy.setName(rule.getName());
						rCopy.setParent(rule.getParent());
						rCopy.setFireOn(rule.getFireOn());
						rCopy.setEnabled(rule.isEnabled());
						rCopy.setFolder(rule.getFolder());
						rCopy.setShortCircuit(rule.isShortCircuit());
						rCopy.setModDate(rule.getModDate());
						
						trackFiredRule(rCopy, req);
					}
                    long after = System.currentTimeMillis();
        			if((after - before) > SLOW_RULE_LOG_MIN) {
						Logger.warn(RulesEngine.class, "Rule ID: " + rule.getId()
								+ " is running too slow. The rule is fired on: " + rule.getFireOn().name());
        			}
                } catch (RuleEngineException e) {
                    Logger.error(RulesEngine.class, "Rule could not be evaluated. Rule ID: " + rule.getId(), e);
                }
            }

        } catch(Exception e) {
            Logger.error(RulesEngine.class, "Unable process rules: " + e.getMessage(), e);
        }
    }

	/**
	 * Keeps track of the rules that have been fired for a given HTTP request.
	 * This will allow Web developers to access the list of rules that were
	 * triggered after a issuing a request. Developers can examine both the rules
	 * fired in the current request or the rules fired through the current session.
	 *
	 * 
	 * @param firedRule
	 *            - The {@link Rule} that has been fired.
	 * @param req
	 *            - The {@link HttpServletRequest} object.
	 */
	@SuppressWarnings("unchecked")
	private static void trackFiredRule(Rule firedRule, HttpServletRequest req) {
		FiredRulesList firedRulesRequest = (FiredRulesList) req.getAttribute(WebKeys.RULES_ENGINE_FIRE_LIST);
		FiredRulesList firedRulesSession = (FiredRulesList) req.getSession(true).getAttribute(WebKeys.RULES_ENGINE_FIRE_LIST);


		if (!UtilMethods.isSet(firedRulesRequest)) {
			firedRulesRequest = new FiredRulesList();
			req.setAttribute(WebKeys.RULES_ENGINE_FIRE_LIST, firedRulesRequest);
		}

		if (!UtilMethods.isSet(firedRulesSession)) {
			firedRulesSession = new FiredRulesList();
			req.getSession(true).setAttribute(WebKeys.RULES_ENGINE_FIRE_LIST, firedRulesSession);
		}

		Date now = new Date();
		FiredRule ruleFired = new FiredRule(now, firedRule);
		firedRulesRequest.add( ruleFired );
		firedRulesSession.add( ruleFired );
	}

}
