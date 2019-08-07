package com.dotcms.rendering.velocity.events;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rendering.velocity.servlet.VelocityModeHandler;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.util.Html;
import java.util.Collections;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.exception.ParseErrorException;

/**
 * Handle for the ParseErrorException
 * @author jsanca
 */
public class ExceptionHandlerParseErrorException implements ExceptionHandler<ParseErrorException> {

    private static final String NEW_LINE = Html.br() + Html.space(2);

    @Override
    public void handle(final ParseErrorException exception) {

        if (isPreviewOrEditMode(exception)) {
            try {
                   handleParseErrorException(exception);
            } catch (Exception ex) {
                Logger.error(VelocityModeHandler.class, "Parsing error on: " + exception.getTemplateName() + ", msg: " + ex.getMessage(), ex);
            }

            throw new PreviewEditParseErrorException(exception);
        }

        throw exception;
    }

    private boolean isPreviewOrEditMode(final Exception exception) {
        return ExceptionUtil.isPreviewOrEditMode(exception, HttpServletRequestThreadLocal.INSTANCE.getRequest());
    }


    static String handleParseErrorException(final ParseErrorException exception) {
        final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
        final StringBuilder message = new StringBuilder();
        final String errorMessage = WordUtils.wrap
                (exception.getMessage(), 15, Html.br(), false);

        message.append(Html.h3("Parsing Error"))

                .append(Html.b("Template")).append(NEW_LINE)
                .append(exception.getTemplateName()).append(Html.br())

                .append(Html.b("Invalid Syntax")).append(NEW_LINE)
                .append(exception.getInvalidSyntax()).append(Html.br())

                .append(Html.b("Column Number")).append(NEW_LINE)
                .append(exception.getColumnNumber()).append(Html.br())

                .append(Html.b("Line Number")).append(NEW_LINE)
                .append(exception.getLineNumber()).append(Html.br())

                .append(Html.pre(errorMessage));
        final String messageAsString = message.toString();
        systemMessageBuilder.setMessage(messageAsString)
                .setLife(DateUtil.FIVE_SECOND_MILLIS)
                .setType(MessageType.SIMPLE_MESSAGE)
                .setSeverity(MessageSeverity.ERROR);

        final String userId = PrincipalThreadLocal.getName();
        if (UtilMethods.isSet(userId)) {
            SystemMessageEventUtil.getInstance().
                    pushMessage(exception.getTemplateName(),
                            systemMessageBuilder.create(), Collections.singletonList(userId));
        }
        return messageAsString;
    }

}