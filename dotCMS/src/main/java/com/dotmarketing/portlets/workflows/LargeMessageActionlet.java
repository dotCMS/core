package com.dotmarketing.portlets.workflows;


import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

import static com.dotmarketing.portlets.workflows.util.WorkflowActionletUtil.getParameterValue;

/**
 * This sub action allows the user to send a large message as part of the pipe-line in action.
 * The message could be just a raw or html, also can support velocity interpolation on both of them.
 *
 * @author jsanca
 * @version 5.0
 * @since May 26, 2018
 */
public class LargeMessageActionlet extends MessageActionlet {

    private static final String PARAM_CONTENT_TITLE       = "title";
    private static final String PARAM_CONTENT_WIDTH       = "width";
    private static final String PARAM_CONTENT_LANG        = "lang";
    private static final String PARAM_CONTENT_CODE        = "code";

    private static final String ACTIONLET_NAME  = "Large Message";
    private static final String HOW_TO          =
            "This actionlet allows to send a large message to the client as part of the action.";

    @Override
    public String getName() {
        return ACTIONLET_NAME;
    }

    @Override
    public String getHowTo() {
        return HOW_TO;
    }

    @Override
    public synchronized List<WorkflowActionletParameter> getParameters() {

        final ImmutableList.Builder<WorkflowActionletParameter> workflowActionletParameters =
                new ImmutableList.Builder<>();

        workflowActionletParameters.addAll(super.getParameters())
                .add(new WorkflowActionletParameter(PARAM_CONTENT_TITLE,
                "Title", StringPool.BLANK,
                true))
                .add(new WorkflowActionletParameter(PARAM_CONTENT_WIDTH,
                        "Width", "90%",
                        true))
                .add(new WorkflowActionletParameter(PARAM_CONTENT_LANG,
                        "Language", "java",
                        false))
                .add(new WorkflowActionletParameter(PARAM_CONTENT_CODE,
                        "Code", StringPool.BLANK,
                        false));


        return workflowActionletParameters.build();
    }

    @Override
    protected SystemMessage processMessageValue(final WorkflowProcessor processor, final String message,
                                                final MessageSeverity severity, final long lifeMillis,
                                                final Map<String, WorkflowActionClassParameter> params,
                                                final HttpServletRequest request, final HttpServletResponse response) {

        final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
        final String velocityMessage         = this.evalVelocilyMessage(processor, message, request, response);
        final LargeMessageMap messageMap     = new LargeMessageMap();
        final String title                   = getParameterValue(params.get(PARAM_CONTENT_TITLE));
        final String width                   = getParameterValue(params.get(PARAM_CONTENT_WIDTH));
        final String lang                    = getParameterValue(params.get(PARAM_CONTENT_LANG));
        final String code                    = getParameterValue(params.get(PARAM_CONTENT_CODE));
        messageMap.title(title);
        messageMap.width(width);
        messageMap.body(velocityMessage);

        if (UtilMethods.isSet(lang) && UtilMethods.isSet(code)) {

            messageMap.code(new LargeMessageMap.CodeMessage(lang, code));
        }

        return systemMessageBuilder.setMessage(messageMap)
                .setLife(lifeMillis)
                .setSeverity(severity).create();
    }

    @Override
    protected void pushMessage(final SystemMessage systemMessage, final List<String> users) {

        this.systemMessageEventUtil.pushLargeMessage(systemMessage, users);
    }
}
