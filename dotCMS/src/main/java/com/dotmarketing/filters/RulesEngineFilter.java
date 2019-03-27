///*
// * CharsetEncodingFilter.java
// *
// * Created on 24 October 2007
// */
//package com.dotmarketing.filters;
//
//import com.dotmarketing.beans.Host;
//import com.dotmarketing.business.APILocator;
//import com.dotmarketing.business.CacheLocator;
//import com.dotmarketing.business.web.WebAPILocator;
//import com.dotmarketing.exception.DotDataException;
//import com.dotmarketing.exception.DotSecurityException;
//import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
//import com.dotmarketing.portlets.rules.business.RulesAPI;
//import com.dotmarketing.portlets.rules.business.RulesCache;
//import com.dotmarketing.portlets.rules.model.Rule;
//import com.dotmarketing.portlets.rules.model.RuleAction;
//import com.dotmarketing.portlets.rules.model.RuleActionParameter;
//import com.dotmarketing.util.Logger;
//import com.dotmarketing.util.UtilMethods;
//import com.liferay.portal.model.User;
//
//import javax.servlet.*;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.util.*;
//
//
//public class RulesEngineFilter implements Filter {
//
//    private RulesAPI rulesAPI = APILocator.getRulesAPI();
//    private RulesCache rulesCache = CacheLocator.getRulesCache();
//    private User systemUser;
//
//	public void init(FilterConfig arg0) throws ServletException {
//        try {
//            systemUser = WebAPILocator.getUserWebAPI().getSystemUser();
//        } catch (DotDataException e) {
//            Logger.error(this, "Unable to get systemUser", e);
//        }
//    }
//
//	public void doFilter(ServletRequest request, ServletResponse response,
//			FilterChain filterChain) throws IOException, ServletException {
//
//		HttpServletRequest req = (HttpServletRequest) request;
//		HttpServletResponse res = (HttpServletResponse) response;
//
//        Host host;
//
//        try {
//            host =  WebAPILocator.getHostWebAPI().getCurrentHost(req);
//        } catch (Exception e) {
//            Logger.error(this, "Unable to retrieve current request host for URI ");
//            throw new ServletException(e.getMessage(), e);
//        }
//
//        try {
//
//            // ONCE PER VISITOR RULES
//
//            String longLivedCookie = UtilMethods.getCookieValue(req.getCookies(),
//                    com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);
//
//            if (!UtilMethods.isSet(longLivedCookie)) {
//                // lets get these rules evaluated and executed
//
//
//                executeRules(oncePerVisitorRules, req, res);
//
//            }
//
//
//        } catch(DotDataException | DotSecurityException e) {
//            Logger.error(this, "Unable process rules." + e.getMessage());
//        }
//
//        filterChain.doFilter(request, response);
//	}
//
//    private void executeRules(Collection<Rule> rules, HttpServletRequest req, HttpServletResponse res) throws DotSecurityException, DotDataException {
//
//    }
//
//	public void destroy() {
//	}
//
//}
