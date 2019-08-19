package com.dotmarketing.portlets.workflows.actionlet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.transform.ContentletToMapTransformer;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.DNSUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Mailer;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.Company;

import io.vavr.control.Try;

public class SendContentAsEmail extends WorkFlowActionlet {

  private static final long serialVersionUID = 1L;

  @Override
  public List<WorkflowActionletParameter> getParameters() {
    List<WorkflowActionletParameter> params = new ArrayList<WorkflowActionletParameter>();

    params.add(new WorkflowActionletParameter("fromEmail", "From Email", "$company.emailAddress", true));
    params.add(new WorkflowActionletParameter("fromName", "From Name", "dotCMS", true));
    params.add(new WorkflowActionletParameter("toEmail", "To Email", "${content.formEmail}", true));

    params.add(new WorkflowActionletParameter("bcc", "Bcc Email", "", false));
    params.add(new WorkflowActionletParameter("emailSubject", "Email Subject", "[dotCMS] new ${contentTypeName}", true));
    params.add(new WorkflowActionletParameter("condition", "Condition - email will send unless<br>velocity prints 'false'", "", false));
    params.add(new WorkflowActionletParameter("emailTemplate", "The email template to parse",
        "#dotParse('static/form/email-form-entry.vtl')", true));

    return params;
  }

  @Override
  public String getName() {
    return "Send a piece of content as an Email";
  }

  @Override
  public String getHowTo() {
    return "This actionlet will send an email that can be based on the submitted content. The value of every field here is parsed velocity.  So, to send a custom email to the email address stored in a field called userEmail, put $content.userEmail in the 'to email' field and the system will replace it with the variables from the content";
  }

  @Override
  public void executeAction(final WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
      throws WorkflowActionFailureException {

    final Contentlet contentlet = processor.getContentlet();
    final Map<String, Object> valueMap = new TreeMap<>();
    valueMap.putAll(Try.of(() -> ContentletUtil.getContentPrintableMap(APILocator.systemUser(), contentlet))
        .getOrElseThrow(e -> new DotRuntimeException(e)));
    final Company company = APILocator.getCompanyAPI().getDefaultCompany();
    final Host host = resolveHost(contentlet);
    HttpServletRequest requestProxy = resolveRequest(contentlet);
    String ipAddress = Try.of(() -> DNSUtil.reverseDns(resolveIPAddress(valueMap))).getOrElse("n/a");
    valueMap.put("ipAddress", ipAddress);
    String toEmail = params.get("toEmail").getValue();
    String fromEmail = params.get("fromEmail").getOrDefault(company.getEmailAddress());
    String fromName = params.get("fromName").getOrDefault("dotCMS");
    String emailSubject = params.get("emailSubject").getValue();
    String condition = params.get("condition").getValue();
    String emailTemplate = params.get("emailTemplate").getValue();
    String bcc = params.get("bcc").getValue();

    HttpServletResponse responseProxy = new BaseResponse().response();
    Context context = VelocityUtil.getWebContext(VelocityUtil.getBasicContext(), requestProxy, responseProxy);
    context.put("host", host);
    context.put("host_id", host.getIdentifier());
    context.put("user", processor.getUser());
    context.put("company", APILocator.getCompanyAPI().getDefaultCompany());
    context.put("workflow", processor);
    context.put("stepName", processor.getStep().getName());
    context.put("stepId", processor.getStep().getId());
    context.put("nextAssign", processor.getNextAssign().getName());
    context.put("workflowMessage", processor.getWorkflowMessage());
    context.put("nextStepResolved", processor.getNextStep().isResolved());
    context.put("nextStepId", processor.getNextStep().getId());
    context.put("nextStepName", processor.getNextStep().getName());
    if (UtilMethods.isSet(processor.getTask())) {
      context.put("workflowTaskTitle",
          UtilMethods.isSet(processor.getTask().getTitle()) ? processor.getTask().getTitle() : processor.getContentlet().getTitle());
      context.put("modDate", processor.getTask().getModDate());
    } else {
      context.put("workflowTaskTitle", processor.getContentlet().getTitle());
      context.put("modDate", processor.getContentlet().getModDate());
    }
    context.put("contentTypeName", processor.getContentlet().getContentType().name());

    context.put("contentlet", contentlet);
    context.put("content", contentlet);
    context.put("map", valueMap);

    VelocityEval velocity = new VelocityEval(context);

    condition = velocity.eval(condition);
    if (UtilMethods.isSet(condition) && condition.indexOf("false") > -1) {
      Logger.info(this.getClass(), processor.getAction().getName() + " email condition contains false, skipping email send");
      return;
    }

    toEmail = velocity.eval(toEmail);
    fromEmail = velocity.eval(fromEmail);
    fromName = velocity.eval(fromName);
    emailSubject = velocity.eval(emailSubject);
    emailTemplate = velocity.eval(emailTemplate);

    Mailer mail = new Mailer();
    mail.setToEmail(toEmail);
    mail.setFromEmail(fromEmail);
    mail.setFromName(fromName);
    mail.setSubject(emailSubject);
    mail.setHTMLAndTextBody(emailTemplate);

    if (UtilMethods.isSet(bcc)) {
      bcc = velocity.eval(bcc);
      mail.setBcc(bcc);
    }
    valueMap.entrySet().stream().filter(e -> e.getValue() instanceof File).forEach(e -> mail.addAttachment((File) e.getValue()));

    mail.sendMessage();
  }

  private String resolveIPAddress(final Map<String, Object> map) {
    return (String) (getInsensitive(map, "ipaddress") != null ? getInsensitive(map, "ipaddress")
        : getInsensitive(map, "ip") != null ? getInsensitive(map, "ip") : "n/a");
  }

  private HttpServletRequest resolveRequest(final Contentlet contentlet) {

    return (HttpServletRequestThreadLocal.INSTANCE.getRequest() != null) ? HttpServletRequestThreadLocal.INSTANCE.getRequest()
        : new MockHttpRequest(resolveHost(contentlet).getHostname(), null).request();
  }

  private Host resolveHost(final Contentlet contentlet) {
    return Try
        .of(() -> Host.SYSTEM_HOST.equals(contentlet.getHost()) ? APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false)
            : APILocator.getHostAPI().find(contentlet.getHost(), APILocator.systemUser(), false))
        .getOrElse(APILocator.systemHost());
  }

  private String getInsensitive(final Map<String, Object> map, final String key) {
    Optional<Entry<String, Object>> entry = map.entrySet().parallelStream().filter(e -> e.getKey().equalsIgnoreCase(key)).findAny();
    return entry.isPresent() ? String.valueOf(entry.get().getValue()) : null;

  }

  class VelocityEval {
    private final Context context;

    public VelocityEval(Context context) {
      super();
      this.context = context;
    }

    String eval(String toEval) {
      return Try.of(() -> VelocityUtil.eval(toEval, this.context)).getOrNull();
    }

  }

}
