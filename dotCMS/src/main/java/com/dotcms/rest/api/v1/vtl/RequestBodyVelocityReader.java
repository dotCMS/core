package com.dotcms.rest.api.v1.vtl;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;

import java.io.Reader;
import java.io.StringReader;

/**
 * This strategy reads the velocity code from a {@link java.util.Map} in the entry mapped to a convention-based
 * key {@link RequestBodyVelocityReader#EMBEDDED_VELOCITY_KEY_NAME}
 */

public class RequestBodyVelocityReader implements VelocityReader {

    static final String EMBEDDED_VELOCITY_KEY_NAME = "velocity";

    private static final String SCRIPTING_USER_ROLE_KEY = "Scripting Developer";

    @Override
    public Reader getVelocity(final VTLResource.VelocityReaderParams params) throws DotSecurityException, DotDataException {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final boolean canRenderVelocity = APILocator.getRoleAPI()
                .doesUserHaveRole(params.getUser(), roleAPI.loadRoleByKey(SCRIPTING_USER_ROLE_KEY));
        if(!canRenderVelocity) {
            Logger.warn(this, "User does not have the required role. User: " + params.getUser());
            throw new DotSecurityException("User does not have the required role");
        }
        final String velocityString = (String)params.getBodyMap().get(EMBEDDED_VELOCITY_KEY_NAME);
        return new StringReader(velocityString);
    }
}
