package com.dotcms.api.system.event.verifier;


import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.api.v1.system.websocket.WebSocketUserSessionData;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;

/**
 * Verified that the  sessionUser has the role in the payload visibilityValue.
 */
public class RoleVerifier implements PayloadVerifier {

    private final RoleAPI roleAPI;

    public RoleVerifier() {
        this(APILocator.getRoleAPI());
    }

    @VisibleForTesting
    public RoleVerifier(final RoleAPI roleAPI) {
        this.roleAPI = roleAPI;
    }

    @Override
    public boolean verified(final Payload payload, final WebSocketUserSessionData userSessionData) {
        try {
            return this.checkRoles(userSessionData.getUser(), payload.getVisibilityValue().toString());
        } catch (DotDataException e) {
            throw new VerifierException(e);
        }
    }

    private boolean checkRoles(final User user, final String roleId) throws DotDataException {
        return this.roleAPI.doesUserHaveRole(user, roleId);
    }

}