package com.dotmarketing.webdav;

import com.dotmarketing.util.Logger;
import io.milton.http.LockInfo;
import io.milton.http.LockResult;
import io.milton.http.LockTimeout;
import io.milton.http.LockToken;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.PreConditionFailedException;
import io.milton.resource.LockableResource;

public interface DotResource extends LockableResource {
    
    
    @Override
    default  Object authenticate(String username, String requestedPassword) {

        try {
            return new DotWebdavHelper().authorizePrincipal(username, requestedPassword);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            return null;
        }


    }


    @Override
    default LockResult refreshLock(String token, LockTimeout timeout) throws NotAuthorizedException, PreConditionFailedException {
        
        return LockResult.success(new LockToken(token, new LockInfo(), timeout));
    }

}
