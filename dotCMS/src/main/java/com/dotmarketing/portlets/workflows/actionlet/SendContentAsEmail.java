package com.dotmarketing.portlets.workflows.actionlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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
  private static final  String FROM_EMAIL="fromEmail";
  private static final  String FROM_NAME="fromName";
  private static final  String TO_EMAIL="toEmail";
  private static final  String BCC="BCC";
  private static final  String EMAIL_SUBJECT="emailSubject";
  private static final  String CONDITION="condition";
  private static final  String EMAIL_TEMPLATE="emailTemplate";
  
  @Override
  public List<WorkflowActionletParameter> getParameters() {
    

    
    List<WorkflowActionletParameter> params = new ArrayList<WorkflowActionletParameter>();
    params.add(new WorkflowActionletParameter(FROM_EMAIL, "From Email", "$company.emailAddress", true));
    params.add(new WorkflowActionletParameter(FROM_NAME, "From Name", "dotCMS", true));
    params.add(new WorkflowActionletParameter(TO_EMAIL, "To Email", "${content.formEmail}", true));
    params.add(new WorkflowActionletParameter(EMAIL_SUBJECT, "Email Subject", "[dotCMS] new ${contentTypeName}", true));
    params.add(new WorkflowActionletParameter(EMAIL_TEMPLATE, "The email template to parse",
        "#dotParse('static/form/email-form-entry.vtl')", true));
    
    params.add(new WorkflowActionletParameter(CONDITION, "Condition - email will send unless<br>velocity prints 'false'", "", false));
    params.add(new WorkflowActionletParameter(BCC, "Bcc Email", "", false));

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
    final ContentType contentType = contentlet.getContentType();
    final Company company = APILocator.getCompanyAPI().getDefaultCompany();
    final Host host = resolveHost(contentlet);
    final HttpServletRequest requestProxy = resolveRequest(contentlet);
    final HttpServletResponse responseProxy = new BaseResponse().response();
    final String ipAddress = Try.of(() -> DNSUtil.reverseDns(resolveIPAddress(contentlet))).getOrElse("n/a");
    

    Context context = VelocityUtil.getWebContext(VelocityUtil.getBasicContext(), requestProxy, responseProxy);
    context.put("host", host);
    context.put("ipAddress", ipAddress);
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


    VelocityEval velocity = new VelocityEval(context);
    
    

    final String toEmail = velocity.eval(params.get(TO_EMAIL).getValue());
    final String fromEmail = velocity.eval(params.get(FROM_EMAIL).getOrDefault(company.getEmailAddress()));
    final String fromName = velocity.eval(params.get(FROM_NAME).getOrDefault("dotCMS"));
    final String emailSubject = velocity.eval(params.get(EMAIL_SUBJECT).getValue());
    final String condition = velocity.eval(params.get(CONDITION).getValue());
    final String emailTemplate = velocity.eval(params.get(EMAIL_TEMPLATE).getValue());
    final String bcc = velocity.eval(params.get(BCC).getValue());

    if (UtilMethods.isSet(condition) && condition.indexOf("false") > -1) {
      Logger.info(this.getClass(), processor.getAction().getName() + " email condition contains false, skipping email send");
      return;
    }


    final Mailer mail = new Mailer();
    mail.setToEmail(toEmail);
    mail.setFromEmail(fromEmail);
    mail.setFromName(fromName);
    mail.setSubject(emailSubject);
    mail.setHTMLAndTextBody(emailTemplate);
    if (UtilMethods.isSet(bcc)) {
      mail.setBcc(bcc);
    }
    
    //send the binaries as attachments
    contentType.fields()
      .stream()
      .filter(f -> f instanceof BinaryField)
      .filter(f -> Try.of(()->contentlet.getBinary(f.variable())).getOrNull() !=null)
      .forEach(f -> mail.addAttachment(Try.of(()->contentlet.getBinary(f.variable())).getOrNull()));

    mail.sendMessage();
  }

  /**
   * Best efforts to determine the ipAddress of the content 
   * @param contentlet
   * @return
   */
  private String resolveIPAddress(final Contentlet contentlet) {
    return (String) (getInsensitive(contentlet, "ipaddress") != null ? getInsensitive(contentlet, "ipaddress")
        : getInsensitive(contentlet, "ip") != null ? getInsensitive(contentlet, "ip") : "n/a");
  }

  /**
   * gets an HTTPRequest to use for velocity
   * @param contentlet
   * @return
   */
  private HttpServletRequest resolveRequest(final Contentlet contentlet) {

    return (HttpServletRequestThreadLocal.INSTANCE.getRequest() != null) ? HttpServletRequestThreadLocal.INSTANCE.getRequest()
        : new MockHttpRequest(resolveHost(contentlet).getHostname(), null).request();
  }

  /**
   * Best efforts to determine the HOST of the content 
   * @param contentlet
   * @return
   */
  private Host resolveHost(final Contentlet contentlet) {
    return Try
        .of(() -> Host.SYSTEM_HOST.equals(contentlet.getHost()) ? APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false)
            : APILocator.getHostAPI().find(contentlet.getHost(), APILocator.systemUser(), false))
        .getOrElse(APILocator.systemHost());
  }

  /**
   * returns the value from the content map regardless of case
   * @param contentlet
   * @param key
   * @return
   */
  private String getInsensitive(final Contentlet contentlet, final String key) {
    Optional<Entry<String, Object>> entry = contentlet.getMap().entrySet().parallelStream().filter(e -> e.getKey().equalsIgnoreCase(key)).findAny();
    return entry.isPresent() ? String.valueOf(entry.get().getValue()) : null;

  }

  private class VelocityEval {
    private final Context context;

    public VelocityEval(Context context) {
      this.context = context;
    }

    String eval(String toEval) {
      return Try.of(() -> VelocityUtil.eval(toEval, this.context)).getOrNull();
    }

  }

}
