package com.dotmarketing.portlets.workflows;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.repackage.org.directwebremoting.util.FakeHttpServletRequest;
import com.dotcms.util.ConversionUtils;
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

    private final SystemMessageEventUtil systemMessageEventUtil =
            SystemMessageEventUtil.getInstance();

    private static final long serialVersionUID = 1177885642438262884L;

    private static final String ACTIONLET_NAME  = "Message";
    private static final String HOW_TO          =
            "This actionlet allows to send a message to the client as part of the action.";

    private static final String ID_DELIMITER             = ",";
    private static final String PARAM_CONTENT_USER       = "users";
    private static final String PARAM_CONTENT_MESSAGE    = "message";
    private static final String PARAM_CONTENT_SEVERITY   = "severity";
    private static final String PARAM_CONTENT_LIFE       = "life";
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

    @Override
    public void executeAction(final WorkflowProcessor processor,
            final Map<String, WorkflowActionClassParameter> params) {

        final HttpServletRequest request =
                null == HttpServletRequestThreadLocal.INSTANCE.getRequest()?
                        new FakeHttpServletRequest(): HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse response =
                null == HttpServletResponseThreadLocal.INSTANCE.getResponse()?
                        new MockHttpResponse(): HttpServletResponseThreadLocal.INSTANCE.getResponse();
        final String userIds     = getParameterValue(params.get(PARAM_CONTENT_USER));
        final String message     = getParameterValue(params.get(PARAM_CONTENT_MESSAGE));
        final String severity    = getParameterValue(params.get(PARAM_CONTENT_SEVERITY));
        final String life        = getParameterValue(params.get(PARAM_CONTENT_LIFE));

        final User  currentUser  = processor.getUser();
        final List<String> users = this.processUsers (userIds.split(ID_DELIMITER), currentUser);

        this.systemMessageEventUtil.pushMessage
                    (this.processMessageValue(processor, message,
                            this.getMessageSeverity(severity), this.getLifeSeconds(life),
                            request, response), users);
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

    private List<String> processUsers(final String[] userList, final User currentUser) {

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

    private SystemMessage processMessageValue(final WorkflowProcessor processor, final String message,
                                              final MessageSeverity severity, final long lifeMillis,
                                              final HttpServletRequest request, final HttpServletResponse response) {

        final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
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

        return systemMessageBuilder.setMessage(velocityMessage)
                .setLife(lifeMillis)
                .setSeverity(severity).create();
    }

}
