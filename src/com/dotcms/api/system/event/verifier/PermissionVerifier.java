package com.dotcms.api.system.event.verifier;


import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.dotcms.exception.BaseRuntimeInternationalizationException;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.api.v1.system.websocket.SessionWrapper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DotDataException;


public class PermissionVerifier implements PayloadVerifier{

    private final PermissionAPI permissionAPI;

    @VisibleForTesting
    public PermissionVerifier(PermissionAPI permissionAPI){
        this.permissionAPI = permissionAPI;
    }

    public PermissionVerifier(){
        permissionAPI = APILocator.getPermissionAPI();
    }

    @Override
    public boolean verified(Payload payload, SessionWrapper session) {
        try {
            return permissionAPI.doesUserHavePermission((Permissionable) payload.getData(),
                    Integer.parseInt(payload.getVisibilityId()),
                    session.getUser(), false);
        } catch (DotDataException e) {
            throw new VerifierException(e);
        }
    }
}
