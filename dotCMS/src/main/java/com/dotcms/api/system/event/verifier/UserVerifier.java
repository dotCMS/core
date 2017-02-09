package com.dotcms.api.system.event.verifier;


import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.liferay.portal.model.User;

/**
 * Verified that the sessionUser user is the same of the payload visibilityValue
 */
public class UserVerifier implements  PayloadVerifier{

    public UserVerifier(){}

    @Override
    public boolean verified(final Payload payload, final User sessionUser) {
        return sessionUser.getUserId()
                .equals(payload.getVisibilityValue());
    }
}
