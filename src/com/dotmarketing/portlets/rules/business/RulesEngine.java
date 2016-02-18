package com.dotmarketing.portlets.rules.business;

import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.rules.exception.RuleEngineException;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class RulesEngine {
	private static int SLOW_RULE_LOG_MIN=Config.getIntProperty("SLOW_RULE_LOG_MIN", 100);

    public static void fireRules(HttpServletRequest req, HttpServletResponse res, Ruleable parent, Rule.FireOn fireOn) {
        //Check for the proper license level, the rules engine is an enterprise feature only
        if ( LicenseUtil.getLevel() < 200 ) {
            return;
        }
        if(req !=null ){
        	if("skip".equals(req.getParameter(WebKeys.RULES_ENGINE_PARAM)) || "skip".equals(
        			String.valueOf(req.getAttribute(WebKeys.RULES_ENGINE_PARAM)))){
        		return;
        	}
        }

        if(parent ==null){
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

            Set<Rule> rules = APILocator.getRulesAPI().getRulesByParentFireOn(parent.getIdentifier(), systemUser, false, fireOn);
			List<Rule> firedRules = (List<Rule>) req.getAttribute(WebKeys.RULES_ENGINE_FIRE_LIST);
        	if(firedRules ==null){
        		firedRules = new ArrayList<Rule>();
        		req.setAttribute(WebKeys.RULES_ENGINE_FIRE_LIST, firedRules);
        	}
            for (Rule rule : rules) {
                try {
                	long before = System.currentTimeMillis();
                    rule.checkValid(); // @todo ggranum: this should actually be done on writing to the DB, or at worst reading from.
                    boolean evaled = rule.evaluate(req, res);
                	if(evaled){
                    	@SuppressWarnings("unchecked")

                    	Rule rCopy = new Rule();
                    	rCopy.setId(rule.getId());
                    	rCopy.setName(rule.getName());
                    	rCopy.setParent(rule.getParent());
                    	rCopy.setFireOn(rule.getFireOn());
                    	rCopy.setEnabled(rule.isEnabled());
                    	rCopy.setFolder(rule.getFolder());
                    	rCopy.setShortCircuit(rule.isShortCircuit());
                    	rCopy.setModDate(rule.getModDate());
                    	firedRules.add(rCopy);
                    	
                	}
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
    
    
    public static void fireRules(HttpServletRequest req, HttpServletResponse res, Rule.FireOn fireOn) {
	        Host host;
	        try {
	            host =  WebAPILocator.getHostWebAPI().getCurrentHost(req);
	        } catch (Exception e) {
	            Logger.error(RulesEngine.class, "Unable to retrieve current request host for URI ", e);
	            return;
	        }

    	
    	fireRules(req,res, host, fireOn);
    }
}
