package com.dotcms.rest.api.v1.vtl;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;

import java.io.Reader;
import java.io.StringReader;

public class RequestBodyVelocityReader implements VelocityReader {

    private static final String SCRIPTING_USER_ROLE_KEY = "Scripting Developer";

    @Override
    public Reader getVelocity(VTLResource.VelocityReaderParams params) throws DotSecurityException, DotDataException {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final boolean canRenderVelocity = APILocator.getRoleAPI()
                .doesUserHaveRole(params.getUser(), roleAPI.loadRoleByKey(SCRIPTING_USER_ROLE_KEY));
        if(!canRenderVelocity) {
            Logger.info(this, "User does not have the required role");
            throw new DotSecurityException("User does not have the required role");
        }
        String velocityString = params.getBodyMap().get("velocity");
        return new StringReader(velocityString);
    }
}
