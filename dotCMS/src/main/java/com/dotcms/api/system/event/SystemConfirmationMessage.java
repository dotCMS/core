package com.dotcms.api.system.event;

import java.io.Serializable;

/**
 * A confirmation message is a {@link SystemMessage} but includes callback to execute if the user choose YES or an optional if the answers is NO.
 * @author jsanca
 */
public class SystemConfirmationMessage implements Serializable {

    private final String callbackOnYes;     // required
    private final String callbackOnNo;      // optional
    private final SystemEventType type;     // inner type
    private final SystemMessage   message;

    public SystemConfirmationMessage(final String callbackOnYes,
                                     final String callbackOnNo,
                                     final SystemEventType type,
                                     final SystemMessage message) {

        this.callbackOnYes = callbackOnYes;
        this.callbackOnNo = callbackOnNo;
        this.type = type;
        this.message = message;
    }

    public String getCallbackOnYes() {
        return callbackOnYes;
    }

    public String getCallbackOnNo() {
        return callbackOnNo;
    }

    public SystemEventType getType() {
        return type;
    }

    public SystemMessage getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "SystemConfirmationMessage{" +
                "callbackOnYes='" + callbackOnYes + '\'' +
                ", callbackOnNo='" + callbackOnNo + '\'' +
                ", type=" + type +
                ", message=" + message +
                '}';
    }
}
