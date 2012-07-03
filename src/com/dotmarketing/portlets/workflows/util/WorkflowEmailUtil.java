package com.dotmarketing.portlets.workflows.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.velocity.tools.view.context.ChainedContext;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.cmis.proxy.DotInvocationHandler;
import com.dotmarketing.cmis.proxy.DotRequestProxy;
import com.dotmarketing.cmis.proxy.DotResponseProxy;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Mailer;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

/**
 * @author David
 */
public class WorkflowEmailUtil {

	/**
	 * This method will take an email address and a {@link WorkflowProcessor}
	 * and send a generic workflow email to them. If subject is null, it will be
	 * intelligently inferred, and if the email text is null, the system will
	 * try send the file: "static/workflow/workflow_email_template.html".
	 *
	 * Both the subject and the emailBody will be parsed by Velocity with an
	 * $workflow object in their context This object is the
	 * {@link WorkflowProcessor} object that has access to every aspect of the
	 * workflow task.
	 *
	 * @param processor
	 * @param subject
	 * @param emailText
	 * @param isHTML
	 */

	public static void sendWorkflowEmail(WorkflowProcessor processor, String[] email, String subject, String emailText, Boolean isHTML) {


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

			InvocationHandler dotInvocationHandler = new DotInvocationHandler();

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
			ctx.put("stepName", processor.getStep().getName());
			ctx.put("stepId", processor.getStep().getId());
			ctx.put("nextAssign", processor.getNextAssign().getName());
			ctx.put("workflowMessage", processor.getWorkflowMessage());
			ctx.put("nextStepResolved", processor.getNextStep().isResolved());
			ctx.put("nextStepId", processor.getNextStep().getId());
			ctx.put("nextStepName", processor.getNextStep().getName());
			ctx.put("workflowTaskTitle", UtilMethods.isSet(processor.getTask().getTitle())?processor.getTask().getTitle() : processor.getContentlet().getTitle());
			ctx.put("modDate", processor.getTask().getModDate());
			ctx.put("structureName", processor.getContentlet().getStructure().getName());



			if (!UtilMethods.isSet(emailText)) {
				emailText = VelocityUtil.mergeTemplate("static/workflow/workflow_email_template.html", ctx);
				isHTML = true;
			} else {
				emailText = VelocityUtil.eval(emailText, ctx);
			}
			for(String x: email){
				Mailer mail = new Mailer();
				mail.setFromEmail(processor.getUser().getEmailAddress());
				mail.setFromName(processor.getUser().getFullName());
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

	/**
	 * This method will take the "nextAssign" role from the processor and use it
	 * to build a list of workflow emails to be sent out, either to 1 person or
	 * to a group of people that belong to the next assign role. If subject is
	 * null, it will be intelligently inferred, and if the email text is null,
	 * the system will try send the file:
	 * "static/workflow/workflow_email_template.html". Both the subject and the
	 * emailBody will be parsed by Velocity with an $workflow object in their
	 * context This object is the {@link WorkflowProcessor} object that has
	 * access to every aspect of the workflow task.
	 *
	 * @param processor
	 * @param subject
	 * @param emailText
	 * @param isHTML
	 */
	public static void sendWorkflowMessageToNextAssign(WorkflowProcessor processor, String subject, String emailText, Boolean isHTML) {

		try {

			Role assignedTo = null;

			assignedTo = processor.getNextAssign();
			if (assignedTo == null) {
				throw new WorkflowActionFailureException("Next assign does not exist");
			}

			// add the next assign if a user
			Set<String> recipients = new HashSet<String>();
			try {
				recipients.add(APILocator.getUserAPI()
						.loadUserById(assignedTo.getRoleKey(), APILocator.getUserAPI().getSystemUser(), false).getEmailAddress());
			} catch (Exception e) {

			}

			// add the user if next assign if a role
			try {
				List<User> users = APILocator.getRoleAPI().findUsersForRole(assignedTo, false);
				for(User u : users){
					recipients.add(u.getEmailAddress());
				}
			} catch (Exception e) {

			}

			String[] to = (String[]) recipients.toArray(new String[recipients.size()]);
			// send'em workflows

			sendWorkflowEmail(processor, to, subject, null, true);


		} catch (Exception e) {

			throw new DotWorkflowException("Exception ocurred trying to deliver emails for workflow " + e.getMessage());
		}
	}

}
