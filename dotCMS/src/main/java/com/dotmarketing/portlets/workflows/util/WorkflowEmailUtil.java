package com.dotmarketing.portlets.workflows.util;

import com.dotcms.company.CompanyAPI;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.rest.api.v1.system.ConfigurationHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.*;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author David
 */
public class WorkflowEmailUtil {

    //fall-back "from address" and "from name".
    // These should be used only on very rare cases if the saved email is not good.
    //But we have guards in place to make sure the saved data is sound.
    public static final String FALLBACK_FROM_ADDRESS = "website@dotcms.com";
    public static final String FALLBACK_FROM_NAME = "admin";

    /**
	 * This method will take an email address and a {@link WorkflowProcessor}
	 * and send a generic workflow email to them. If subject is null, it will be
	 * intelligently inferred, and if the email text is null, the system will
	 * try send the file: "static/workflow/workflow_email_template.vtl".
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
    public static void sendWorkflowEmail(WorkflowProcessor processor, String[] email, String subject, String emailText,
                                         Boolean isHTML) {
        sendWorkflowEmail(processor, email, subject, emailText, isHTML, null);
    }

    /**
     * Sends a workflow email to the specified recipients using the given {@link WorkflowProcessor}.
     * If the subject is null, it is inferred based on the contentlet title and next step.
     * If the email text is null, a default email template ("static/workflow/workflow_email_template.vtl")
     * will be used and processed with Velocity.
     * <p>
     * The email body and subject will be evaluated using Velocity, with a context containing
     * workflow-related variables such as {@code $workflow}, {@code $user}, {@code $host}, and more.
     * <p>
     * If custom headers are provided, they will be processed and added to the email.
     *
     * @param processor     The {@link WorkflowProcessor} containing workflow-related data.
     * @param email         Array of recipient email addresses.
     * @param subject       The email subject (can be null, in which case it will be inferred).
     * @param emailText     The email body (can be null, in which case a default template is used).
     * @param isHTML        Boolean flag indicating if the email should be sent as HTML (default is false).
     * @param customHeaders Optional custom headers to be added to the email.
     */
    public static void sendWorkflowEmail(WorkflowProcessor processor, String[] email, String subject, String emailText,
            Boolean isHTML, String customHeaders) {


        try {
            if (isHTML == null) {
                isHTML = false;
            }
            if (!UtilMethods.isSet(subject)) {
                subject = processor.getContentlet().getTitle() + " --> " + processor.getNextStep().getName()
                        + " (dotCMS " + LanguageUtil.get(processor.getUser(), "Workflow") + ")";
            }

            // get the host of the content
            Host host = APILocator
                    .getHostAPI()
                    .find(processor.getContentlet(), APILocator.getUserAPI().getSystemUser(), false);
            if (host.isSystemHost()) {
                host = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false);
            }

            List<Layout> layouts = APILocator.getLayoutAPI().findAllLayouts();
            Layout layout = new Layout();
            for (Layout lout : layouts) {
                if (lout.getPortletIds().contains(PortletID.WORKFLOW)) {
                    layout = lout;
                    break;
                }
            }
            String link = Config.getStringProperty("WORKFLOW_OVERRIDE_LINK_URL");
            if (!UtilMethods.isSet(link)) {
                String serverScheme = Config.getStringProperty("WEB_SERVER_SCHEME", "https");
                link += serverScheme + "://" + host.getHostname() ;
            }
            if (null != processor.getTask()) {
                link += "/dotAdmin/#/c/workflow/" + processor.getTask().getId();
            } else {
                link += "/dotAdmin/#/c/workflow";
            }

            HttpServletRequest requestProxy = new FakeHttpRequest(host.getHostname(), null).request();
            HttpServletResponse responseProxy = new BaseResponse().response();
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
            ctx.put("workflowTaskTitle",
                    null != processor.getTask() && UtilMethods.isSet(processor.getTask().getTitle())
                            ? processor.getTask().getTitle()
                            : processor.getContentlet().getTitle());
            ctx.put("modDate", null != processor.getTask() ? processor.getTask().getModDate()
                    : processor.getContentlet().getModDate());
            ctx.put("structureName", processor.getContentlet().getStructure().getName());

            ctx.put("contentlet", processor.getContentlet());
            ctx.put("content", processor.getContentlet());


            if (!UtilMethods.isSet(emailText)) {
                emailText = VelocityUtil.getInstance().mergeTemplate("static/workflow/workflow_email_template.vtl", ctx);
            } else {
                emailText = VelocityUtil.eval(emailText, ctx);
            }

            final Tuple2<String,String> senderInfo = resolveSenderInfo(processor.getUser());

            final String emailAddress = senderInfo._1;
            final String fromName = senderInfo._2;

            for (final String x : email) {
                final Mailer mail = new Mailer();
                mail.setFromEmail(emailAddress);
                mail.setFromName(fromName);
                mail.setToEmail(x);
                mail.setSubject(VelocityUtil.eval(subject, ctx));
                if (isHTML) {
                    mail.setHTMLAndTextBody(emailText);
                } else {
                    mail.setTextBody(emailText);
                }
                // Process custom headers if provided
                EmailUtils.processCustomHeaders(mail, ctx, customHeaders);
                mail.sendMessage();
            }
        } catch (Exception e) {
            Logger.error(WorkflowEmailUtil.class,
                    "Exception occurred trying to deliver emails for workflow " + e.getMessage(), e);
        }

    }

    /**
     * Workflow mail Sender resolution
     * @param workflowUser
     * @return
     */
    private static Tuple2<String,String> resolveSenderInfo(final User workflowUser) throws DotDataException{
        return resolveSenderInfo(workflowUser, APILocator.getUserAPI(), APILocator.getCompanyAPI());
    }

    /**
     * Parameterizable method meant to facilitate testing
     * We support something like "dotCMS Website <website@dotcms.com>" as the company email from there will extract
     * @param workflowUser
     * @param userAPI
     * @param companyAPI
     * @return
     */
    @VisibleForTesting
    static Tuple2<String, String> resolveSenderInfo(final User workflowUser, final UserAPI userAPI,
            final CompanyAPI companyAPI) throws DotDataException {

        if (null != workflowUser && !userAPI.getAnonymousUser().equals(workflowUser) && !userAPI
                .getSystemUser().equals(workflowUser)) {
            Logger.debug(WorkflowEmailUtil.class, String.format(
                    "User [%s] trigger email actionlet. Trying to use company email as the from address.",
                    workflowUser.getEmailAddress()));
        }
        //If we reach this point. Then User is anonymous or system-user or null
        final Company defaultCompany = companyAPI.getDefaultCompany();
        final String fromMail = Try.of(() -> {
            if (UtilMethods.isSet(defaultCompany.getEmailAddress())) {
                return defaultCompany.getEmailAddress();
            }
            Logger.error(WorkflowEmailUtil.class, String.format(
                    "Company email address isn't set or contains errors. Falling back to [%s] as the from address.",
                    FALLBACK_FROM_ADDRESS));
            return FALLBACK_FROM_ADDRESS;
        }).get();

        final Tuple2<String, String> mailAndSenderWithFallBacks = Try.of(() -> {

            final Tuple2<String, String> mailAndSender = ConfigurationHelper.INSTANCE
                    .parseMailAndSender(fromMail);

            final String emailAddress = mailAndSender._1;
            final String personal = mailAndSender._2;

            if (UtilMethods.isSet(emailAddress)) {
                return Tuple.of(emailAddress,
                        UtilMethods.isSet(personal) ? personal : FALLBACK_FROM_NAME);
            } else {
                Logger.warn(WorkflowEmailUtil.class,
                        () -> "Unable to extract a from-name from the provided company Address. Default to `admin`. ");
                return Tuple.of(FALLBACK_FROM_ADDRESS, FALLBACK_FROM_NAME);
            }

        }).get();

        Logger.warn(WorkflowEmailUtil.class, () -> String
                .format("Defaulting to Company's configured email address `%s` instead of workflow user's email `%s` as email sender..",
                        fromMail, null != workflowUser ? workflowUser.getEmailAddress() : "n/a"));

        return mailAndSenderWithFallBacks;

    }

	/**
	 * This method will take the "nextAssign" role from the processor and use it
	 * to build a list of workflow emails to be sent out, either to 1 person or
	 * to a group of people that belong to the next assign role. If subject is
	 * null, it will be intelligently inferred, and if the email text is null,
	 * the system will try send the file:
	 * "static/workflow/workflow_email_template.vtl". Both the subject and the
	 * emailBody will be parsed by Velocity with an $workflow object in their
	 * context This object is the {@link WorkflowProcessor} object that has
	 * access to every aspect of the workflow task.
	 *
	 * @param processor
	 * @param subject
	 * @param emailText
	 * @param isHTML
	 */
	public static void sendWorkflowMessageToNextAssign(WorkflowProcessor processor, String subject, String emailText, Boolean isHTML, String customHeaders) {

		try {

			Role assignedTo = null;

			assignedTo = processor.getNextAssign();
			if (assignedTo == null) {
				throw new WorkflowActionFailureException("Next assign does not exist");
			}

			// add the next assign if a user
			Set<String> recipients = new HashSet<>();
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

			sendWorkflowEmail(processor, to, subject, emailText, true, customHeaders);


		} catch (Exception e) {
            Logger.error(WorkflowEmailUtil.class,
                    "Exception occurred trying to deliver emails for workflow " + e.getMessage(), e);
		}
	}

}
