package com.dotcms.api.system.event.verifier;


import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.dotcms.exception.BaseRuntimeInternationalizationException;
import com.dotcms.rest.api.v1.system.websocket.SessionWrapper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DotDataException;

/**
 * Created by freddyrodriguez on 16/9/16.
 */
public class PermissionVerifier implements PayloadVerifier<Integer>{

    private PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    public PermissionVerifier(){

    }

    @Override
    public boolean verified(Payload payload, SessionWrapper session) {
        try {
            return permissionAPI.doesUserHavePermission((Permissionable) payload.getData(),
                    Integer.parseInt(payload.getVisibilityId()),
                    session.getUser(), false);
        } catch (DotDataException e) {
            throw new BaseRuntimeInternationalizationException(e);
        }
    }
}
