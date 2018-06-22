package com.dotcms.rest.api.v1.workflow;

import com.dotcms.exception.ExceptionUtil;
import com.liferay.portal.model.User;

public class ActionFail {

    private final String inode;
    private final String errorMessage;

    private ActionFail(final String inode, final String errorMessage) {
        this.inode = inode;
        this.errorMessage = errorMessage;
    }

    public String getInode() {
        return inode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static ActionFail newInstance(final User user ,final String inode, final Exception e){

        String message = e.getMessage();
        final String [] tokens = message.split("\\s+");
        if(tokens.length == 1){
            //Message is i18ned
           message = ExceptionUtil.getLocalizedMessageOrDefault(user, message, message,null);
        }

        return new ActionFail(inode, message);
    }

}
