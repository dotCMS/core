package com.dotcms.rendering.velocity.events;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.rendering.velocity.servlet.VelocityModeHandler;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.util.Html;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.exception.ParseErrorException;

import java.util.Arrays;

/**
 * Handle for the ParseErrorException
 * @author jsanca
 */
public class ExceptionHandlerParseErrorException implements ExceptionHandler<ParseErrorException> {

    private static final String NEW_LINE = Html.br() + Html.space(2);

    @Override
    public void handle(final ParseErrorException e) {

        if (isEditMode(e)) {
            try {

                final String userId = PrincipalThreadLocal.getName();
                if (UtilMethods.isSet(userId)) {
                    final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
                    final StringBuilder message = new StringBuilder();
                    final String errorMessage = WordUtils.wrap
                            (e.getMessage(), 15, Html.br(), false);

                    message.append(Html.h3("Parsing Error"))

                            .append(Html.b("Template")).append(NEW_LINE)
                            .append(e.getTemplateName()).append(Html.br())

                            .append(Html.b("Invalid Syntax")).append(NEW_LINE)
                            .append(e.getInvalidSyntax()).append(Html.br())

                            .append(Html.b("Column Number")).append(NEW_LINE)
                            .append(e.getColumnNumber()).append(Html.br())

                            .append(Html.b("Line Number")).append(NEW_LINE)
                            .append(e.getLineNumber()).append(Html.br())

                            .append(Html.pre(errorMessage));

                    systemMessageBuilder.setMessage(message.toString())
                            .setLife(DateUtil.FIVE_SECOND_MILLIS)
                            .setType(MessageType.SIMPLE_MESSAGE)
                            .setSeverity(MessageSeverity.ERROR);

                    SystemMessageEventUtil.getInstance().
                            pushMessage(systemMessageBuilder.create(), Arrays.asList(userId));

                }
            } catch (Exception ex) {
                Logger.error(VelocityModeHandler.class, "Parsing error on: " + e.getTemplateName() + ", msg: " + ex.getMessage(), ex);
            }
        }

        throw e;
    }

    boolean isEditMode(final Exception e) {

        boolean ret = false;

        for (final StackTraceElement ste : e.getStackTrace()) {
            if (ste.getClassName().indexOf("EditMode") > -1 || ste.getMethodName().indexOf("EditMode") > -1) {

                ret = true;
                break;
            }
        }

        return ret;
    }
}
