package com.dotmarketing.portlets.workflows;


import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotmarketing.portlets.workflows.model.MultiUserReferenceParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
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
    private static final String PARAM_CONTENT_HEIGHT      = "height";
    private static final String PARAM_CONTENT_LANG        = "lang";
    private static final String PARAM_CONTENT_CODE        = "code";
    private static final String PARAM_CONTENT_USER        = "users";
    private static final String PARAM_CONTENT_MESSAGE     = "message";

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

        workflowActionletParameters
                .add(new MultiUserReferenceParameter(PARAM_CONTENT_USER,
                "User IDs", CURRENT_USER_DEFAULT_VALUE,
                true))
                .add(new WorkflowActionletParameter(PARAM_CONTENT_MESSAGE,
                        "Message",
                        StringPool.BLANK,
                        true))
                .add(new WorkflowActionletParameter(PARAM_CONTENT_TITLE,
                "Title", StringPool.BLANK,
                true))
                .add(new WorkflowActionletParameter(PARAM_CONTENT_WIDTH,
                        "Width", "90%",
                        true))
                .add(new WorkflowActionletParameter(PARAM_CONTENT_HEIGHT,
                        "Height", "90%",
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
    public void executeAction(final WorkflowProcessor processor,
                              final Map<String, WorkflowActionClassParameter> params) {

        final User currentUser          = processor.getUser();
        final HttpServletRequest request =
                null == HttpServletRequestThreadLocal.INSTANCE.getRequest()?
                        this.mockRequest(currentUser): HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse response =
                null == HttpServletResponseThreadLocal.INSTANCE.getResponse()?
                        this.mockResponse(): HttpServletResponseThreadLocal.INSTANCE.getResponse();
        final String userIds     = getParameterValue(params.get(PARAM_CONTENT_USER));
        final String message     = getParameterValue(params.get(PARAM_CONTENT_MESSAGE));

        final List<String> users = this.processUsers (userIds.split(ID_DELIMITER), currentUser);

        this.systemMessageEventUtil.pushLargeMessage(this.processMessageValue(processor, message,
                params, request, response), users);
    }

    protected LargeMessageMap processMessageValue(final WorkflowProcessor processor, final String message,
                                                final Map<String, WorkflowActionClassParameter> params,
                                                final HttpServletRequest request, final HttpServletResponse response) {

        final String velocityMessage         = this.evalVelocityMessage(processor, message, request, response);
        final LargeMessageMap messageMap     = new LargeMessageMap();
        final String title                   = getParameterValue(params.get(PARAM_CONTENT_TITLE));
        final String width                   = getParameterValue(params.get(PARAM_CONTENT_WIDTH));
        final String height                  = getParameterValue(params.get(PARAM_CONTENT_HEIGHT));
        final String lang                    = getParameterValue(params.get(PARAM_CONTENT_LANG));
        final String code                    = getParameterValue(params.get(PARAM_CONTENT_CODE));
        messageMap.title(title);
        messageMap.width(width);
        messageMap.height(height);
        messageMap.body(velocityMessage);

        if (UtilMethods.isSet(lang) && UtilMethods.isSet(code)) {

            messageMap.code(new LargeMessageMap.CodeMessage(lang, code));
        }

        return messageMap;
    }

}
