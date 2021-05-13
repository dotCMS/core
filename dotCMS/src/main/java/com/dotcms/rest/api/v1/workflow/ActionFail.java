package com.dotcms.rest.api.v1.workflow;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class ActionFail {

    private final String identifier;
    private final String inode;
    private final String errorMessage;

    private ActionFail(final String inode, final String identifier, final String errorMessage) {
        this.inode = inode;
        this.identifier = identifier;
        this.errorMessage = errorMessage;
    }

    public String getInode() {
        return inode;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static ActionFail newInstance(final User user ,final String inode, final Exception e){

        final Throwable rootCause = ExceptionUtil.getRootCause(e);
        final String message = getMessage(rootCause, user);
        return new ActionFail(inode, null, message);
    }

    public static ActionFail newInstanceById(final User user ,final String identifier, final Exception e){

        final Throwable rootCause = ExceptionUtil.getRootCause(e);
        final String message = getMessage(rootCause, user);

        return new ActionFail(null, identifier, message);
    }

    private static String getMessage (final Throwable rootCause, final User user) {

        String message = rootCause.getMessage();
        if(UtilMethods.isSet(message)) {
            final String[] tokens = message.split("\\s+");
            if (tokens.length == 1) {
                //Message is i18ned
                message = ExceptionUtil.getLocalizedMessageOrDefault(user, message, message, null);
            }
        } else {
            message = rootCause.toString();
        }

        return message;
    }

}
