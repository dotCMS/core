package com.dotmarketing.portlets.rules.business;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.visitor.domain.Visitor;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.portlets.rules.exception.RuleEngineException;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.CookieUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

import io.vavr.control.Try;

import java.util.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
	
	
	
	

    public static void fireRules(final HttpServletRequest request, final HttpServletResponse response) {
        
        Optional<Visitor> visitor = APILocator.getVisitorAPI().getVisitor(request);

        boolean newVisitor = false;
        boolean newVisit = false;

        /*
         * JIRA http://jira.dotmarketing.net/browse/DOTCMS-4659 //Set long lived cookie
         * regardless of who this is
         */
        String _dotCMSID =
                UtilMethods.getCookieValue(request.getCookies(), com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);

        if (!UtilMethods.isSet(_dotCMSID)) {
            // create unique generator engine
            Cookie idCookie = CookieUtil.createCookie();
            _dotCMSID = idCookie.getValue();
            response.addCookie(idCookie);
            newVisitor = true;

            if (visitor.isPresent()) {
                visitor.get()
                    .setDmid(UUID.fromString(_dotCMSID));
            }

        }

        String _oncePerVisitCookie = UtilMethods.getCookieValue(request.getCookies(), WebKeys.ONCE_PER_VISIT_COOKIE);

        if (!UtilMethods.isSet(_oncePerVisitCookie)) {
            newVisit = true;
        }

        if (newVisitor) {
            fireRules(request, response, Rule.FireOn.ONCE_PER_VISITOR);
            if (response.isCommitted()) {
                /*
                 * Some form of redirect, error, or the request has already been fulfilled in
                 * some fashion by one or more of the actionlets.
                 */
                Logger.debug(RulesEngine.class, "A ONCE_PER_VISITOR RuleEngine Action has committed the response.");
                return;
            }
        }

        if (newVisit) {
            fireRules(request, response, Rule.FireOn.ONCE_PER_VISIT);
            if (response.isCommitted()) {
                /*
                 * Some form of redirect, error, or the request has already been fulfilled in
                 * some fashion by one or more of the actionlets.
                 */
                Logger.debug(RulesEngine.class, "A ONCE_PER_VISIT RuleEngine Action has committed the response.");
                return;
            }
        }

        fireRules(request, response, Rule.FireOn.EVERY_PAGE);

    }
	
	
	private final static String DOT_RULES_FIRED_ALREADY = "DOT_RULES_FIRED_ALREADY";
	
	/**
	 * Triggers a specific category of Rules associated to the specified parent
	 * object based on the requested resource.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @param response
	 *            - The {@link HttpServletResponse} object.
	 * @param parent
	 *            - The object whose associated rules will be fired. For
	 *            example, the {@link Host} object.
	 * @param fireOn
	 *            - The category of the rules that will be fired: Every page,
	 *            every request, etc.
	 */
	public static void fireRules(final HttpServletRequest request, final HttpServletResponse response,
								 final Ruleable parent, final Rule.FireOn fireOn) {

        //Check for the proper license level, the rules engine is an enterprise feature only
        if ( LicenseUtil.getLevel() < LicenseLevel.STANDARD.level ) {
            return;
        }
        if(response.isCommitted()) {
          return;
        }
        if (!UtilMethods.isSet(request)) {
        	throw new DotRuntimeException("ERROR: HttpServletRequest is null");
        }

        
        final Set<String> alreadyFiredRulesFor =request.getAttribute(DOT_RULES_FIRED_ALREADY)!=null?(Set<String>)request.getAttribute(DOT_RULES_FIRED_ALREADY):new HashSet<String>();
        final String ruleRunKey = parent.getIdentifier() +"_"+ fireOn.name();
        if(alreadyFiredRulesFor.contains(ruleRunKey)) {
          Logger.warn(RulesEngine.class, "we have already run the rules for:" + ruleRunKey);
          return;
        }
        alreadyFiredRulesFor.add(ruleRunKey);
        request.setAttribute(DOT_RULES_FIRED_ALREADY,alreadyFiredRulesFor);

		if (SKIP_RULES_EXECUTION.equalsIgnoreCase(request.getParameter(WebKeys.RULES_ENGINE_PARAM))
				|| SKIP_RULES_EXECUTION.equalsIgnoreCase(String.valueOf(request.getParameter(WebKeys.RULES_ENGINE_PARAM)))) {
			return;
		}
        if (!UtilMethods.isSet(parent)) {
        	return;
        }

        final User systemUser = APILocator.systemUser();

        try {

			final Set<Rule> rules = APILocator.getRulesAPI().getRulesByParentFireOn(parent.getIdentifier(), systemUser, false,
					fireOn);
            for (final Rule rule : rules) {
                try {

					request.setAttribute(WebKeys.RULES_ENGINE_PARAM_CURRENT_RULE_ID, rule.getId());
                	long before = System.currentTimeMillis();
                    rule.checkValid(); // @todo ggranum: this should actually be done on writing to the DB, or at worst reading from.
                    boolean evaled = rule.evaluate(request, response);

                    if(response.isCommitted()) {
                      return;
                    }
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
						
						trackFiredRule(rCopy, request);
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

        } catch(InvalidLicenseException ile){
          Logger.debug(RulesEngine.class,  ile.getMessage());
        }
        catch(Exception e) {
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
		HttpSession session = req.getSession(false);
		FiredRulesList firedRulesSession = (session==null) ? new FiredRulesList() :  (FiredRulesList) session.getAttribute(WebKeys.RULES_ENGINE_FIRE_LIST);


		if (!UtilMethods.isSet(firedRulesRequest)) {
			firedRulesRequest = new FiredRulesList();
			req.setAttribute(WebKeys.RULES_ENGINE_FIRE_LIST, firedRulesRequest);
		}

		if (!UtilMethods.isSet(firedRulesSession)) {
			firedRulesSession = new FiredRulesList();
			if(session!=null)
			  session.setAttribute(WebKeys.RULES_ENGINE_FIRE_LIST, firedRulesSession);
		}

		Date now = new Date();
		FiredRule ruleFired = new FiredRule(now, firedRule);
		firedRulesRequest.add( ruleFired );
		firedRulesSession.add( ruleFired );
	}

}
