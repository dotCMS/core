package com.dotcms.api.system.event;

import com.liferay.portal.model.User;

/**
 * This contract is used to verified a payload.
 * If the payload is valid, will return true, otherwise false.
 * {@link Visibility}'s verifier
 */
public interface PayloadVerifier {

    /**
     * Returns true if the payload is valid.
     * @param payload {@link Payload}
     * @param sessionUser {@link User}
     * @return boolean
     */
    public boolean verified (Payload payload, User sessionUser);
}
