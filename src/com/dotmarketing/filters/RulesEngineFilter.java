/*
 * CharsetEncodingFilter.java
 *
 * Created on 24 October 2007
 */
package com.dotmarketing.filters;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.CookieUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


public class RulesEngineFilter implements Filter {

    private RulesAPI rulesAPI = APILocator.getRulesAPI();

	public void init(FilterConfig arg0) throws ServletException {
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
        User systemUser = null;

        try {
            APILocator.getUserAPI().getSystemUser();
            List<Rule> hostRules = rulesAPI.getRulesByHost(host.getIdentifier(), systemUser, false);

            for (Rule rule : hostRules) {
                List<ConditionGroup> groups = rulesAPI.getConditionGroupsByRule(rule.getId(), systemUser, false);

                for (ConditionGroup group : groups) {
                    List<Condition> conditions = rulesAPI.getConditionsByConditionGroup(group.getId(), systemUser, false);

                    for (Condition condition : conditions) {
                        String conditionletId = condition.getConditionletId();

                        Conditionlet conditionlet = rulesAPI.findConditionlet(conditionletId);
                        conditionlet.evaluate(req, res, condition.getComparison(), condition.getValues());

                    }
                }
            }

        } catch(DotDataException | DotSecurityException e) {
            Logger.error(this, "Unable process rules." + e.getMessage());
            throw new ServletException("Unable process rules." + e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
	}
	
	public void destroy() {
	}
}
