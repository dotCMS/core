package com.dotcms.api.system.event.verifier;


import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.api.v1.system.websocket.SessionWrapper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;

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
    public boolean verified(Payload payload, SessionWrapper session) {
        try {
            return this.checkRoles(SessionWrapper.class.cast(session).getUser(), payload.getVisibilityId());
        } catch (DotDataException e) {
            throw new VerifierException(e);
        }
    }

    private boolean checkRoles(final User user, final String roleId) throws DotDataException {
        return this.roleAPI.doesUserHaveRole(user, roleId);
    }

}