package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.rendering.util.ActionletUtil;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotmarketing.exception.DotDataException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.workflows.WorkflowParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.*;
import org.apache.velocity.context.Context;

import static com.dotmarketing.portlets.workflows.util.WorkflowActionletUtil.getParameterValue;

public class EmailActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        List<WorkflowActionletParameter> params = new ArrayList<>();

        params.add(new WorkflowActionletParameter("fromEmail", "From Email", "", true));
        params.add(new WorkflowActionletParameter("fromName", "From Name", "", true));
        params.add(new WorkflowActionletParameter("toEmail", "To Email", "", true));
        params.add(new WorkflowActionletParameter("toName", "To Name", "", true));
        params.add(new WorkflowActionletParameter("cc", "Cc Email", "", false));
        params.add(new WorkflowActionletParameter("bcc", "Bcc Email", "", false));
        params.add(new WorkflowActionletParameter("emailSubject", "Email Subject", "", true));
        params.add(new WorkflowActionletParameter("emailBody", "Email Body (html)", "", true));
        params.add(new WorkflowActionletParameter("condition",
                "Condition - email will send unless<br>velocity prints 'false'", "", false));
        params.add(new WorkflowActionletParameter("attachment1",
                "Path or field for attachment <br>(e.g./images/logo.png or 'fileAsset')", "", false));
        params.add(new WorkflowActionletParameter("attachment2",
                "Path or field for attachment <br>(e.g./images/logo.png or 'fileAsset')", "", false));
        params.add(new WorkflowActionletParameter("attachment3",
                "Path or field for attachment <br>(e.g./images/logo.png or 'fileAsset')", "", false));
        params.add(new WorkflowActionletParameter("attachment4",
                "Path or field for attachment <br>(e.g./images/logo.png or 'fileAsset')", "", false));
        params.add(new WorkflowActionletParameter("attachment5",
                "Path or field for attachment <br>(e.g./images/logo.png or 'fileAsset')", "", false));
        params.add(WorkflowParameter.CUSTOM_HEADERS.toWorkflowActionletParameter());


        return params;
    }

    @Override
    public String getName() {
        return "Send an Email";
    }

    @Override
    public String getHowTo() {
        return "This actionlet will send an email that can be based on the submitted content. The value of every field here is parsed velocity.  So, to send a custom email to the email address stored in a field called userEmail, put $content.userEmail in the 'to email' field and the system will replace it with the variables from the content";
    }

    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException {

        Contentlet c = processor.getContentlet();

        String rev = c.getStringProperty("ipAddress");
        try {
            rev = DNSUtil.reverseDns(rev);
            c.setStringProperty("ipAddress", rev);
        } catch (Exception e) {
            Logger.error(this.getClass(), "error on reverse lookup" + e.getMessage());
        }

        String toEmail = params.get("toEmail").getValue();
        String toName = params.get("toName").getValue();
        String fromEmail = params.get("fromEmail").getValue();
        String fromName = params.get("fromName").getValue();
        String emailSubject = params.get("emailSubject").getValue();
        String emailBody = params.get("emailBody").getValue();
        String condition = params.get("condition").getValue();
        String cc = params.get("cc").getValue();
        String bcc = params.get("bcc").getValue();
        String customHeaders = getParameterValue(params.get(WorkflowParameter.CUSTOM_HEADERS.getKey()));

        try {
            // get the host of the content
            Host host = APILocator.getHostAPI().find(
                    processor.getContentlet(),
                    APILocator.getUserAPI().getSystemUser(),
                    false);
            if (host.isSystemHost()) {
                host = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false);
            }

            final HttpServletRequest request = ActionletUtil.getRequest(processor.getUser());
            final HttpServletResponse response = ActionletUtil.getResponse();
            final Context ctx = VelocityUtil.getInstance().getContext(request, response);
            ctx.put("host", host);
            ctx.put("host_id", host.getIdentifier());
            ctx.put("user", processor.getUser());
            ctx.put("workflow", processor);
            ctx.put("stepName", processor.getStep().getName());
            ctx.put("stepId", processor.getStep().getId());
            ctx.put("nextAssign", processor.getNextAssign().getName());
            ctx.put("workflowMessage", processor.getWorkflowMessage());
            ctx.put("nextStepResolved", processor.getNextStep().isResolved());
            ctx.put("nextStepId", processor.getNextStep().getId());
            ctx.put("nextStepName", processor.getNextStep().getName());
            if(UtilMethods.isSet(processor.getTask())) {
                ctx.put("workflowTaskTitle", UtilMethods.isSet(processor.getTask().getTitle()) ? processor.getTask().getTitle() : processor.getContentlet().getTitle());
                ctx.put("modDate", processor.getTask().getModDate());
            }
            else {
                ctx.put("workflowTaskTitle",  processor.getContentlet().getTitle());
                ctx.put("modDate",  processor.getContentlet().getModDate());
            }
            ctx.put("contentType", processor.getContentlet().getContentType());

            ctx.put("contentlet", c);
            ctx.put("content", c);



            if(UtilMethods.isSet(condition)){
                condition = VelocityUtil.eval(condition, ctx);
                if(UtilMethods.isSet(condition) && condition.indexOf("false")>-1){
                    Logger.info(this.getClass(), processor.getAction().getName()  + " email condition contains false, skipping email send");
                    return;
                }
            }


            if(UtilMethods.isSet(toEmail)){
                toEmail = VelocityUtil.eval(toEmail, ctx);
            }
            if(UtilMethods.isSet(toName)){
                toName = VelocityUtil.eval(toName, ctx);
            }
            if(UtilMethods.isSet(fromEmail)){
                fromEmail = VelocityUtil.eval(fromEmail, ctx);
            }

            if(UtilMethods.isSet(fromName)){
                fromName = VelocityUtil.eval(fromName, ctx);
            }
            if(UtilMethods.isSet(emailSubject)){
                emailSubject = VelocityUtil.eval(emailSubject, ctx);
            }
            if(UtilMethods.isSet(emailBody)){
                emailBody = VelocityUtil.eval(emailBody, ctx);
            }

            Mailer mail = new Mailer();
            mail.setToName(toName);
            mail.setToEmail(toEmail);
            mail.setFromEmail(fromEmail);
            mail.setFromName(fromName);
            mail.setSubject(emailSubject);

            mail.setHTMLAndTextBody(emailBody);


            if(UtilMethods.isSet(cc)){
                cc = VelocityUtil.eval(cc, ctx);
                mail.setCc(cc);
            }
            if(UtilMethods.isSet(bcc)){
                bcc = VelocityUtil.eval(bcc, ctx);
                mail.setBcc(bcc);
            }

            // Process custom headers if provided
            EmailUtils.processCustomHeaders(mail, ctx, customHeaders);

            for(int x =1;x<6;x++){
                String attachment = params.get("attachment" + x).getValue();

                if (UtilMethods.isSet(attachment)) {
                    Host fileHost = host;
                    File f = null;
                    try {
                        if(attachment.indexOf("/") == -1 && processor.getContentlet().getBinary(attachment).exists()){
                            f = processor.getContentlet().getBinary(attachment);
                        }
                        else{
                            String hostname = attachment;
                            String filename = attachment;
                            if(hostname.startsWith("//")){
                                hostname = hostname.substring(2,hostname.length());
                                filename = hostname.substring(hostname.indexOf("/"), hostname.length());
                                hostname = hostname.substring(0,hostname.indexOf("/"));
                                fileHost = WebAPILocator.getHostWebAPI().resolveHostName(hostname, processor.getUser(), true);

                            }

                            Identifier id = APILocator.getIdentifierAPI().find(fileHost, filename);
                            Optional<ContentletVersionInfo> vinfo = APILocator.getVersionableAPI().getContentletVersionInfo(id.getId(),processor.getContentlet().getLanguageId());

                            if(vinfo.isEmpty()) {
                                throw new DotDataException("Unable to find version info for attachment. Identifier: " + id.getId() + ", lang: " + processor.getContentlet().getLanguageId());
                            }

                            Contentlet cont = APILocator.getContentletAPI().find(vinfo.get().getLiveInode(), processor.getUser(), true);
                            FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(cont);
                            f = fileAsset.getFileAsset();
                        }
                        if(f!=null && f.exists()){
                            mail.addAttachment(f);
                        }
                    } catch (Exception e) {
                        Logger.error(this.getClass(), "Unable to get file attachment: " + e.getMessage());
                    }
                }
            }

            sendEmail(mail, processor);
        } catch (Exception e) {
            Logger.error(EmailActionlet.class, e.getMessage(), e);
        }

    }

    protected void sendEmail(final Mailer mail, final WorkflowProcessor processor) {
        mail.sendMessage();
    }

}
