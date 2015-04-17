/*
 * CharsetEncodingFilter.java
 *
 * Created on 24 October 2007
 */
package com.dotmarketing.filters;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.portlets.rules.business.RulesCache;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.model.*;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class RulesEngineFilter implements Filter {

    private RulesAPI rulesAPI = APILocator.getRulesAPI();
    private RulesCache rulesCache = CacheLocator.getRulesCache();
    private User systemUser;
    private ScriptEngine engine;

	public void init(FilterConfig arg0) throws ServletException {
        try {
            systemUser = WebAPILocator.getUserWebAPI().getSystemUser();
            ScriptEngineManager manager = new ScriptEngineManager();
            engine = manager.getEngineByName("js");
        } catch (DotDataException e) {
            Logger.error(this, "Unable to get systemUser", e);
        }
    }

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain filterChain) throws IOException, ServletException {
		
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

        Host host;

        try {
            host =  WebAPILocator.getHostWebAPI().getCurrentHost(req);
        } catch (Exception e) {
            Logger.error(this, "Unable to retrieve current request host for URI ");
            throw new ServletException(e.getMessage(), e);
        }

        try {

            Map<Rule, Boolean> evaluatedHostRules = rulesCache.getEvaluatedRulesByHost(host);

            List<Rule> hostRules = rulesAPI.getRulesByHost(host.getIdentifier(), systemUser, false);

            // figure out the rules whose evaluation is not cached

            List<Rule> notCachedEvaluations = new ArrayList<>(hostRules);

            if(evaluatedHostRules!=null)
                notCachedEvaluations.removeAll(evaluatedHostRules.keySet());

            for (Rule rule : notCachedEvaluations) {
                evaluateRule(host, rule, req, res);
            }

        } catch(DotDataException | DotSecurityException e) {
            Logger.error(this, "Unable process rules." + e.getMessage());
        } catch (ScriptException e) {
            Logger.error(this, "Unable to evaluate rules" + e.getMessage());
        }

        filterChain.doFilter(request, response);
	}
	
	public void destroy() {
	}

    private void evaluateRule(Host host, Rule rule, HttpServletRequest req, HttpServletResponse res) throws DotSecurityException, DotDataException, ScriptException {
        StringBuilder ruleExpression = new StringBuilder();
        StringBuilder groupExpression = new StringBuilder();

        List<ConditionGroup> groups = rulesAPI.getConditionGroupsByRule(rule.getId(), systemUser, false);

        for (ConditionGroup group : groups) {

            if(ruleExpression.length()>0) {
                ruleExpression.append(" ").append(group.getOperator().toString()).append(" ");
            }

            ruleExpression.append("(");

            List<Condition> conditions = rulesAPI.getConditionsByConditionGroup(group.getId(), systemUser, false);

            for (Condition condition : conditions) {
                String conditionletId = condition.getConditionletId();

                Conditionlet conditionlet = rulesAPI.findConditionlet(conditionletId);
                boolean evaluationValue = conditionlet.evaluate(req, res, condition.getComparison(), condition.getValues());

                if(groupExpression.length()>0) {
                    groupExpression.append(condition.getOperator().toString());
                }

                groupExpression.append(evaluationValue);

            }

            ruleExpression.append(groupExpression);

            ruleExpression.append(")");
        }


        Boolean result = (Boolean) engine.eval(ruleExpression.toString());

        // Let's put this evaluation in cache

        rulesCache.addEvaluatedRule(host, rule, result);

        // Let's execute the actions
        if(result) {
            List<RuleAction> actions = rulesAPI.getRuleActionsByRule(rule.getId(), systemUser, false);

            for (RuleAction action : actions) {
                RuleActionlet actionlet = rulesAPI.findActionlet(action.getActionlet());
                Map<String, RuleActionParameter> params = rulesAPI.getRuleActionParameters(action, systemUser, false);
                actionlet.executeAction(params);
            }
        }


    }
}
