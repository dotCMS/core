package com.dotmarketing.cms.sendtofriend.action;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

import com.dotcms.util.SecurityUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.factories.EmailFactory;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class SendArcticleToFriendAction  extends DispatchAction {	

	private HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();

	@SuppressWarnings("unchecked")
	public ActionForward unspecified(ActionMapping rMapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception 
	{	 
		//### Parameters ###
		//Get the request Parameters		
		Map<String,Object> parameters = new TreeMap<String, Object>();
		parameters.putAll(request.getParameterMap());
		
					
		//Get the host information
		Host currentHost = hostWebAPI.getCurrentHost(request);		
		
		//Get the user Information
		User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);		
		
		//Get the link or the article
		String sendLink = request.getParameter("send") ;
		boolean isLink = (UtilMethods.isSet(sendLink) && sendLink.equals("link"));
		
		parameters.put("subject", request.getParameter("subject"));
		parameters.put("from", request.getParameter("from"));
		parameters.put("to", request.getParameter("to"));
		parameters.put("message", request.getParameter("message"));
		parameters.put("username", request.getParameter("username"));
		
		if(isLink)
			parameters.put("linkurl", request.getParameter("linkurl"));
		else
			parameters.put("articletext", request.getParameter("articletext"));
		
		parameters.put("emailTemplate", request.getParameter("emailTemplate"));
		
		parameters.put("order", "username,message,articletext,linkurl");
		parameters.put("prettyOrder", "Message From,Message,Article Text,Visit This Link");
		
		parameters.put("request", request);
		parameters.put("response", response);
		
		//Parameters to Validate
		Set<String> parametersToValidate = new HashSet<String>();
		parametersToValidate.add("Message");
		EmailFactory.sendParameterizedEmail(parameters, parametersToValidate, currentHost, user);
		
		if (request.getParameter("return") != null)
		{
			ActionForward af =  new ActionForward(SecurityUtils.stripReferer(request.getParameter("return")));
			af.setRedirect(true);
			return af;
		} else if (request.getParameter("returnUrl") != null) {
			ActionForward af =  new ActionForward(SecurityUtils.stripReferer(request.getParameter("returnUrl")));
			af.setRedirect(true);
			return af;
		} else {
			return rMapping.findForward("thankYouPage");
		}

	}
	
}
