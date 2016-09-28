package com.dotcms.api.system.event;

import com.dotcms.api.system.event.verifier.PermissionVerifier;
import com.dotcms.api.system.event.verifier.RoleVerifier;
import com.dotcms.api.system.event.verifier.UserVerifier;
import com.dotcms.rest.api.v1.system.websocket.SessionWrapper;

/**
 * Visibility for a {@link SystemEvent}, it may be by user, role, or global
 * @author jsanca
 */
public enum Visibility {

    USER(new UserVerifier()),
    ROLE(new RoleVerifier()),
    PERMISSION(new PermissionVerifier()),
    GLOBAL(null);

    private PayloadVerifier payloadVerifier;

    Visibility(PayloadVerifier payloadVerifier){
        this.payloadVerifier = payloadVerifier;
    }

    public <T> boolean verified(SessionWrapper session, Payload payload) {
        boolean result = true;

        if (payloadVerifier != null){
            result = payloadVerifier.verified(payload, session);
        }

        return result;
    }

} // E:O:F:Visibility.
