package com.dotmarketing.portlets.linkchecker.util;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cmis.proxy.DotInvocationHandler;
import com.dotmarketing.cmis.proxy.DotRequestProxy;
import com.dotmarketing.cmis.proxy.DotResponseProxy;
import com.dotmarketing.portlets.linkchecker.bean.CheckURLBean;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Mailer;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.language.LanguageUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

/**
 * Utility class for replace placeholders into mail msg before it was send.
 * 
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Mar 2, 2012
 */
public class LinkCheckerUtil {
	
	private static String PLACEHOLDER_LIST_LINKS = "{2}";
	private static String PLACEHOLDER_FULL_NAME = "{0}";
	private static String PLACEHOLDER_CONTENT_TITLE = "{1}";
	
	public static String buildEmailBodyWithLinksList(String emailBody, String userFullName, String contentTitle, List<CheckURLBean> list){
		StringBuilder sb = new StringBuilder();
		sb.append("<ul>");
		for(CheckURLBean c : list){
			sb.append("<li>");
			sb.append("<strong> Link: </strong>");
			sb.append(c.getUrl());
			sb.append("; <strong> Http return code: </strong>");
			sb.append(c.getStatusCode());
			sb.append("</li>");
		}
		sb.append("</ul>");
		return emailBody.replace(PLACEHOLDER_LIST_LINKS, sb.toString()).replace(PLACEHOLDER_FULL_NAME, userFullName).replace(PLACEHOLDER_CONTENT_TITLE, contentTitle);
	}
	
	public static String buildPopupMsgWithLinksList(String popupMsg, List<CheckURLBean> list){
		StringBuilder sb = new StringBuilder();
		sb.append("<br />");
		for(CheckURLBean c : list){
			sb.append("<br />");
			sb.append("&nbsp-&nbsp");
			sb.append("<strong> Link: </strong>");
			sb.append(c.getUrl());
			sb.append("; <strong> Http code: </strong>");
			sb.append(c.getStatusCode());
			sb.append("; <strong> Title: </strong>");
			sb.append(c.getTitle());
		}
		return popupMsg.replace(PLACEHOLDER_LIST_LINKS, sb.toString());
	}
	

    public static void sendWorkflowEmail(WorkflowProcessor processor, String[] email, String subject, String emailText, String emailFrom, String emailFromFullName, Boolean isHTML) {
            
        try {
            if (isHTML == null) {
                isHTML = false;
            }
            if (!UtilMethods.isSet(subject)) {
                subject = processor.getContentlet().getTitle() + " --> " + processor.getNextStep().getName() + " (dotCMS "
                        + LanguageUtil.get(processor.getUser(), "Workflow") + ")";
            }

            // get the host of the content
            Host host = APILocator.getHostAPI().find(processor.getContentlet().getHost(), APILocator.getUserAPI().getSystemUser(), false);
            if (host.isSystemHost()) {
                host = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false);
            }

            String link = "http://" + host.getHostname() + Config.getStringProperty("WORKFLOWS_URL") + "&_EXT_21_inode="
                    + String.valueOf(processor.getTask().getId());

            InvocationHandler dotInvocationHandler = new DotInvocationHandler(new HashMap());

            DotRequestProxy requestProxy = (DotRequestProxy) Proxy.newProxyInstance(DotRequestProxy.class.getClassLoader(),
                    new Class[] { DotRequestProxy.class }, dotInvocationHandler);
            requestProxy.put("host", host);
            requestProxy.put("host_id", host.getIdentifier());
            requestProxy.put("user", processor.getUser());
            DotResponseProxy responseProxy = (DotResponseProxy) Proxy.newProxyInstance(DotResponseProxy.class.getClassLoader(),
                    new Class[] { DotResponseProxy.class }, dotInvocationHandler);

            org.apache.velocity.context.Context ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
            ctx.put("host", host);
            ctx.put("host_id", host.getIdentifier());
            ctx.put("user", processor.getUser());
            ctx.put("workflow", processor);
            ctx.put("workflowLink", link);


            

            if (!UtilMethods.isSet(emailText)) {
                emailText = VelocityUtil.mergeTemplate("static/workflow/workflow_email_template.html", ctx);
                isHTML = true;
            } else {
                emailText = VelocityUtil.eval(emailText, ctx);
            }
            for(String x: email){
                Mailer mail = new Mailer();
                mail.setFromEmail(emailFrom);
                mail.setFromName(emailFromFullName);
                mail.setToEmail(x);
                mail.setSubject(VelocityUtil.eval(subject, ctx));
                if (isHTML) {
                    mail.setHTMLAndTextBody(emailText);
                } else {
                    mail.setTextBody(emailText);
                }
                mail.sendMessage();
            }
        } catch (Exception e) {
            throw new DotWorkflowException("Exception ocurred trying to deliver emails for workflow " + e.getMessage());
        }

    }
}
