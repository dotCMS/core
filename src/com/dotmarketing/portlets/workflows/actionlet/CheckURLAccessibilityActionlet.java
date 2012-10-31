package com.dotmarketing.portlets.workflows.actionlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.checkurl.bean.CheckURLBean;
import com.dotmarketing.portlets.checkurl.util.AdditionalWorkflowEmailUtil;
import com.dotmarketing.portlets.checkurl.util.CheckURLAccessibilityUtil;
import com.dotmarketing.portlets.checkurl.util.FormatUtil;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

/**
 * Workflow actionlet that call the link checker on the given content fields. 
 * 
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Feb 28, 2012
 */
public class CheckURLAccessibilityActionlet extends WorkFlowActionlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5110097144813925204L;
	private static List<WorkflowActionletParameter> paramList = null;
	private UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
	
	@Override
	public List<WorkflowActionletParameter> getParameters() {
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		try{
			if(null==paramList){
				paramList = new ArrayList<WorkflowActionletParameter>();
				paramList.add(new WorkflowActionletParameter("fieldList", LanguageUtil.get(uWebAPI.getLoggedInUser(request), "checkURL.fieldList"), "", true));
			}
		}catch(Exception e){			
		}
		return paramList;
	}

	@Override
	public String getName() {		
		return "Link Checker";
	}

	@Override
	public String getHowTo() {
		return "This actionlet check the links into the specified fields and reject the content with broken links";
	}

	@Override
	public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {
		//try to get the proxy manager
		try {
			CheckURLAccessibilityUtil.reloadProxyConfiguration();
		} catch (DotDataException e) {
			Logger.error(CheckURLAccessibilityActionlet.class, e.getMessage(), e);
		}
		Contentlet con = processor.getContentlet();
		if(null==params.get("fieldList") || "".equals(params.get("fieldList").getValue().trim()))
			throw new WorkflowActionFailureException("Configuration error!<br />Add the parameter on the Workflow SubAction.");
		List<String> fieldList = Arrays.asList(params.get("fieldList").getValue().trim().split("[,]"));
		for(Field f : (List<Field>)con.getStructure().getFieldsBySortOrder()) {
			if(fieldList.contains(f.getVelocityVarName())){
				
				//get the value
				String value = con.getStringProperty(f.getVelocityVarName());
				
				UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
				WebContext ctx = WebContextFactory.get();
				HttpServletRequest request = ctx.getHttpServletRequest();
				
				List<CheckURLBean> httpResponse=null;
                try {
                    httpResponse = APILocator.getLinkCheckerAPI().findInvalidLinks(value);
                } catch (Exception e1) {
                    Logger.error(this, e1.getMessage(), e1);
                    throw new WorkflowActionFailureException(e1.getMessage());
                }
				
				//if there are unreachable URL...
				if(httpResponse.size()>0){
					try {
//						if(ProxyManager.INSTANCE.getMail().isSend()){
							//send a mail
							//Retrieving the current user
							User user = userWebAPI.getLoggedInUser(request);
							String[] emailAddress = new String[]{user.getEmailAddress()};
							
							String emailBody = FormatUtil.buildEmailBodyWithLinksList(LanguageUtil.get(uWebAPI.getLoggedInUser(request), "checkURL.emailBody"), user.getFullName(), con.getTitle(), httpResponse);	
							String emailFrom = LanguageUtil.get(uWebAPI.getLoggedInUser(request),"checkURL.emailFrom");
							String emailSubject = LanguageUtil.get(uWebAPI.getLoggedInUser(request), "checkURL.emailSubject");
							AdditionalWorkflowEmailUtil.sendWorkflowEmail(processor, 
									emailAddress, emailSubject, emailBody, emailFrom, 
									LanguageUtil.get(uWebAPI.getLoggedInUser(request), "checkURL.emailFromFullName"), true);
//						}
						Logger.error(CheckURLAccessibilityActionlet.class,FormatUtil.buildPopupMsgWithLinksList(LanguageUtil.get(uWebAPI.getLoggedInUser(request), "checkURL.errorBrokenLinks"), httpResponse));
						throw new WorkflowActionFailureException(FormatUtil.buildPopupMsgWithLinksList(LanguageUtil.get(uWebAPI.getLoggedInUser(request), "checkURL.errorBrokenLinks"), httpResponse));
					} catch (LanguageException e) {						
						e.printStackTrace();
					} catch (PortalException e) {
						e.printStackTrace();
					} catch (SystemException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

}
