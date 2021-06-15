package com.dotmarketing.portlets.workflows;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.MultiUserReferenceParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.VelocityUtil;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dotmarketing.portlets.workflows.util.WorkflowActionletUtil.getParameterValue;

/**
 * This sub action allows the user to send message as part of the pipe-line in action.
 * The message could be just a raw or html, also can support velocity interpolation on both of them.
 *
 * @author jsanca
 * @version 5.0
 * @since May 26, 2018
 */
public class MessageActionlet extends WorkFlowActionlet {

    protected final SystemMessageEventUtil systemMessageEventUtil =
            SystemMessageEventUtil.getInstance();

    private static final long serialVersionUID = 1177885642438262884L;

    private static final String ACTIONLET_NAME  = "Message";
    private static final String HOW_TO          =
            "This actionlet allows to send a message to the client as part of the action.";

    protected static final String ID_DELIMITER             = ",";
    protected static final String PARAM_CONTENT_USER       = "users";
    protected static final String PARAM_CONTENT_MESSAGE    = "message";
    protected static final String PARAM_CONTENT_SEVERITY   = "severity";
    protected static final String PARAM_CONTENT_LIFE       = "life";
    private boolean shouldStop = Boolean.FALSE;

    public static final String CURRENT_USER_DEFAULT_VALUE = MultiUserReferenceParameter.CURRENT_USER_VALUE;
    private static final List<WorkflowActionletParameter> ACTIONLET_PARAMETERS = new ImmutableList.Builder<WorkflowActionletParameter>()
                                                    .add(new MultiUserReferenceParameter(PARAM_CONTENT_USER,
                                                            "User IDs", CURRENT_USER_DEFAULT_VALUE,
                                                            true))
                                                    .add(new WorkflowActionletParameter(PARAM_CONTENT_MESSAGE,
                                                            "Message",
                                                            StringPool.BLANK,
                                                            true))
                                                    .add(new WorkflowActionletParameter(PARAM_CONTENT_SEVERITY,
                                                            "Severity(INFO,WARNING, ERROR)",
                                                            "INFO",
                                                            true))
                                                    .add(new WorkflowActionletParameter(PARAM_CONTENT_LIFE,
                                                            "Life Seconds",
                                                            "5",
                                                            true))
                                                    .build();

    @Override
    public synchronized List<WorkflowActionletParameter> getParameters() {
        return ACTIONLET_PARAMETERS;
    }

    @Override
    public String getName() {
        return ACTIONLET_NAME;
    }

    @Override
    public String getHowTo() {
        return HOW_TO;
    }

    @Override
    public boolean stopProcessing() {
        return this.shouldStop;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof WorkFlowActionlet) {
            return getClass().equals(obj.getClass());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (shouldStop ? 1 : 0);
    }

    protected HttpServletRequest  mockRequest (final User  currentUser) {

        final Host host = Try.of(()->APILocator.getHostAPI()
                .findDefaultHost(currentUser, false)).getOrElse(APILocator.systemHost());
        return new MockAttributeRequest(
                        new MockSessionRequest(
                                new MockHttpRequest(host.getHostname(), StringPool.FORWARD_SLASH).request()
                        ).request()
                ).request();
    }

    protected HttpServletResponse mockResponse () {

        return new BaseResponse().response();
    }


    @Override
    public void executeAction(final WorkflowProcessor processor,
            final Map<String, WorkflowActionClassParameter> params) {

        final User  currentUser          = processor.getUser();
        final HttpServletRequest request =
                null == HttpServletRequestThreadLocal.INSTANCE.getRequest()?
                        this.mockRequest(currentUser): HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse response =
                null == HttpServletResponseThreadLocal.INSTANCE.getResponse()?
                        this.mockResponse(): HttpServletResponseThreadLocal.INSTANCE.getResponse();
        final String userIds     = getParameterValue(params.get(PARAM_CONTENT_USER));
        final String message     = getParameterValue(params.get(PARAM_CONTENT_MESSAGE));
        final String severity    = getParameterValue(params.get(PARAM_CONTENT_SEVERITY));
        final String life        = getParameterValue(params.get(PARAM_CONTENT_LIFE));


        final List<String> users = this.processUsers (userIds.split(ID_DELIMITER), currentUser);

        this.pushMessage
                    (this.processMessageValue(processor, message,
                            this.getMessageSeverity(severity), this.getLifeSeconds(life),
                            params, request, response), users);
    }

    protected void pushMessage (final SystemMessage systemMessage, final List<String> users) {

        this.systemMessageEventUtil.pushMessage(systemMessage, users);
    }

    private MessageSeverity getMessageSeverity(final String severity) {

        MessageSeverity messageSeverity = MessageSeverity.INFO;
        try {
            messageSeverity = MessageSeverity.valueOf(severity);
        } catch (Exception e) {
            Logger.error(this, e.getMessage());
        }
        return messageSeverity;
    }

    private long getLifeSeconds(final String lifeSeconds) {

        final int second = ConversionUtils.toInt(lifeSeconds, 5);

        return TimeUnit.MILLISECONDS.convert(second, TimeUnit.SECONDS);
    }

    protected List<String> processUsers(final String[] userList, final User currentUser) {

        final List<String> userIdList = new ArrayList<>();

        for (final String userId : userList) {

             if (CURRENT_USER_DEFAULT_VALUE.equalsIgnoreCase(userId)) {

                 userIdList.add(currentUser.getUserId());
             } else {
                 userIdList.add(userId);
             }
        }

        return userIdList;
    }

    protected SystemMessage processMessageValue(final WorkflowProcessor processor, final String message,
                                              final MessageSeverity severity, final long lifeMillis,
                                              final Map<String, WorkflowActionClassParameter> params,
                                              final HttpServletRequest request, final HttpServletResponse response) {

        final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
        final String velocityMessage        = this.evalVelocityMessage(processor, message, request, response);
        return systemMessageBuilder.setMessage(velocityMessage)
                .setLife(lifeMillis)
                .setSeverity(severity).create();
    }

    protected String evalVelocityMessage(final WorkflowProcessor processor, final String message,
                                         final HttpServletRequest request, final HttpServletResponse response) {

        final Context velocityContext = VelocityUtil.getInstance().getContext(request, response);
        String velocityMessage        = message;
        velocityContext.put("workflow", processor);
        velocityContext.put("user", processor.getUser());
        velocityContext.put("contentlet", processor.getContentlet());
        velocityContext.put("content", processor.getContentlet());

        try {
            velocityMessage = VelocityUtil.eval(message, velocityContext);
        } catch (Exception e1) {
            Logger.warn(this.getClass(), "unable to parse message, falling back" + e1);
        }

        return velocityMessage;
    }

}
