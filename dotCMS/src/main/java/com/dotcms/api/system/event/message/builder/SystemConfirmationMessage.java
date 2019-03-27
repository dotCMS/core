package com.dotcms.api.system.event.message.builder;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;

import java.io.Serializable;
import java.util.Set;

/**
 * A confirmation message is a {@link SystemMessage} but includes callback to execute if the user choose YES or an optional if the answers is NO.
 * @author jsanca
 */
public class SystemConfirmationMessage extends SystemMessage implements Serializable {

    private final String callbackOnYes;     // required
    private final String callbackOnNo;      // optional


    public SystemConfirmationMessage(final Object message,
                                     final String[] portletIdList,
                                     final String callbackOnYes,
                                     final String callbackOnNo) {
        super(message, portletIdList, 3000l, MessageSeverity.INFO, MessageType.CONFIRMATION_MESSAGE);
        this.callbackOnYes = callbackOnYes;
        this.callbackOnNo = callbackOnNo;
    }

    public String getCallbackOnYes() {
        return callbackOnYes;
    }

    public String getCallbackOnNo() {
        return callbackOnNo;
    }

    @Override
    public String toString() {
        return "SystemConfirmationMessage{" +
                "callbackOnYes='" + callbackOnYes + '\'' +
                ", callbackOnNo='" + callbackOnNo + '\'' +
                ", message=" + super.toString() +
                '}';
    }
}
