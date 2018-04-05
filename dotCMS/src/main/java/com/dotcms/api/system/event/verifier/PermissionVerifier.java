package com.dotcms.api.system.event.verifier;


import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.api.v1.system.websocket.WebSocketUserSessionData;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DotDataException;

/**
 * Verifies if the sessionUser has read permission (against the payload visibilityValue) and over the payload data.
 */
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
    public boolean verified(final Payload payload, final WebSocketUserSessionData userSessionData) {
        try {
            return permissionAPI.doesUserHavePermission((Permissionable) payload.getData(),
                    ConversionUtils.toInt(payload.getVisibilityValue(), PermissionAPI.PERMISSION_READ),
                    userSessionData.getUser(), false);
        } catch (DotDataException e) {
            throw new VerifierException(e);
        }
    }

}