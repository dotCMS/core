package com.dotmarketing.webdav;

import com.dotmarketing.util.Logger;
import io.milton.http.Auth;
import io.milton.http.LockInfo;
import io.milton.http.LockResult;
import io.milton.http.LockTimeout;
import io.milton.http.LockToken;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.PreConditionFailedException;
import io.milton.resource.LockableResource;

public interface DotResource extends LockableResource {


    @Override
    default Object authenticate(String username, String requestedPassword) {

        try {
            return DotWebdavHelper.instance().authorizePrincipal(username, requestedPassword);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            return null;
        }


    }


    @Override
    default LockResult refreshLock(String token, LockTimeout timeout) throws NotAuthorizedException, PreConditionFailedException {

        return LockResult.success(new LockToken(token, new LockInfo(), timeout));
    }

    @Override
    default LockResult lock(LockTimeout timeout, LockInfo lockInfo) {
        return DotWebdavHelper.instance().lock(timeout, lockInfo, getUniqueId());
        // return dotDavHelper.lock(lockInfo, user, file.getIdentifier() + "");
    }

    @Override
    default LockResult refreshLock(String token) {
        return DotWebdavHelper.instance().refreshLock(getUniqueId());
        // return dotDavHelper.refreshLock(token);
    }

    @Override
    default void unlock(String tokenId) {
        DotWebdavHelper.instance().unlock(getUniqueId());
        // dotDavHelper.unlock(tokenId);
    }

    @Override
    default LockToken getCurrentLock() {
        return DotWebdavHelper.instance().getCurrentLock(getUniqueId());
    }

    @Override
    default Long getMaxAgeSeconds(Auth arg0) {
        return (long) 60;
    }



}
