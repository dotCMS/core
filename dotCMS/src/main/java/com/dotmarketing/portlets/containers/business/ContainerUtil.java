package com.dotmarketing.portlets.containers.business;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.util.Html;
import org.apache.commons.lang.WordUtils;

import java.util.Arrays;

public class ContainerUtil {

    private static class SingletonHolder {
        private static final ContainerUtil INSTANCE = new ContainerUtil();
    }
    /**
     * Get the instance.
     * @return ContainerUtil
     */
    public static ContainerUtil getInstance() {

        return ContainerUtil.SingletonHolder.INSTANCE;
    } // getInstance.

    private static final String NEW_LINE = Html.br() + Html.space(2);

    /**
     * Helper method to notify when container does not exist
     * @param e {@link Exception}
     * @param containerId {@link String}
     */
    public void notifyException(final Exception e, final String containerId) {

        if (ExceptionUtil.isPreviewOrEditMode(e, HttpServletRequestThreadLocal.INSTANCE.getRequest())) {

            try {

                final String userId = PrincipalThreadLocal.getName();
                if (UtilMethods.isSet(userId)) {

                    final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
                    final StringBuilder message = new StringBuilder();
                    final String errorMessage = WordUtils.wrap
                            (e.getMessage(), 15, Html.br(), false);

                    message.append(Html.h3("Container Not Found Error"))

                            .append(Html.b("Container Id")).append(NEW_LINE)
                            .append(containerId).append(Html.br())
                            .append(Html.pre(errorMessage));

                    systemMessageBuilder.setMessage(message.toString())
                            .setLife(DateUtil.FIVE_SECOND_MILLIS)
                            .setType(MessageType.SIMPLE_MESSAGE)
                            .setSeverity(MessageSeverity.ERROR);

                    SystemMessageEventUtil.getInstance().
                            pushMessage(systemMessageBuilder.create(), Arrays.asList(userId));

                }
            } catch (Exception ex) {
                Logger.error(ContainerUtil.class, "Not found container error on requesting: " + containerId + ", msg: " + ex.getMessage(), ex);
            }
        }
    } // notifyException.
}
