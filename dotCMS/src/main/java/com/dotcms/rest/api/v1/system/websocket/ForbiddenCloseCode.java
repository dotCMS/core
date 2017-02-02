package com.dotcms.rest.api.v1.system.websocket;

import com.dotcms.repackage.javax.ws.rs.core.Response;

import javax.websocket.CloseReason;

/**
 * Custom Forbidden close code.
 * @author jsanca
 */
public class ForbiddenCloseCode implements CloseReason.CloseCode {
    @Override
    public int getCode() {
        return Response.Status.FORBIDDEN.getStatusCode();
    }
}
