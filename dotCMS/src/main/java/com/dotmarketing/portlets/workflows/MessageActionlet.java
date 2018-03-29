package com.dotmarketing.portlets.workflows;

import com.dotcms.api.system.event.SystemMessageEventUtil;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.MultiUserReferenceParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.VelocityUtil;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import org.apache.velocity.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private boolean shouldStop = Boolean.FALSE;

    public static final String CURRENT_USER_DEFAULT_VALUE = "CurrentUser";
    private static final List<WorkflowActionletParameter> ACTIONLET_PARAMETERS = new ImmutableList.Builder<WorkflowActionletParameter>()
                                                    .add(new MultiUserReferenceParameter(PARAM_CONTENT_USER,
                                                            "User IDs", CURRENT_USER_DEFAULT_VALUE,
                                                            true))
                                                    .add(new WorkflowActionletParameter(PARAM_CONTENT_MESSAGE,
                                                            "Message",
                                                            StringPool.BLANK,
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

        final String userIds     = getParameterValue(params.get(PARAM_CONTENT_USER));
        final String message     = getParameterValue(params.get(PARAM_CONTENT_MESSAGE));
        final boolean isHtml     = StringUtils.isHtml(message);
        final User  currentUser  = processor.getUser();
        final List<String> users = this.processUsers (userIds.split(ID_DELIMITER), currentUser);

        if (isHtml) {
            this.systemMessageEventUtil.pushRichMediaEvent
                    (this.processMessageValue(processor, message), users);
        } else {
            this.systemMessageEventUtil.pushSimpleTextEvent
                    (this.processMessageValue(processor, message), users);
        }
    }

    private List<String> processUsers(final String[] userList, final User currentUser) {

        final List<String> userIdList = new ArrayList<>();

        for (final String userId : userList) {

             if (CURRENT_USER_DEFAULT_VALUE.equals(userId)) {

                 userIdList.add(currentUser.getUserId());
             } else {
                 userIdList.add(userId);
             }
        }

        return userIdList;
    }

    private String processMessageValue(final WorkflowProcessor processor, final String message) {

        final Context velocityContext = VelocityUtil.getBasicContext();
        velocityContext.put("workflow", processor);
        velocityContext.put("user", processor.getUser());
        velocityContext.put("contentlet", processor.getContentlet());
        velocityContext.put("content", processor.getContentlet());

        try {
            return VelocityUtil.eval(message, velocityContext);
        } catch (Exception e1) {
            Logger.warn(this.getClass(), "unable to parse message, falling back" + e1);
        }

        return message;
    }

}
