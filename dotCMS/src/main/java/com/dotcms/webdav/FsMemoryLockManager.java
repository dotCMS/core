/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package com.dotcms.webdav;

import com.dotcms.repackage.com.bradmcevoy.http.*;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.dotcms.repackage.com.ettrema.http.fs.LockManager;
import com.dotmarketing.util.Logger;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 *
 */
public class FsMemoryLockManager implements LockManager {


    /**
     * maps current locks by the file associated with the resource
     */
    Map<File, CurrentLock> locksByFile;
    Map<String, CurrentLock> locksByToken;

    public FsMemoryLockManager() {
        locksByFile = new HashMap<File, CurrentLock>();
        locksByToken = new HashMap<String, CurrentLock>();
    }

    @Override
    public synchronized LockResult lock( LockTimeout timeout, LockInfo lockInfo, LockableResource r ) {
        FsResource resource = (FsResource) r;
        LockToken currentLock = currentLock( resource );
        if( currentLock != null ) {
            return LockResult.failed( LockResult.FailureReason.ALREADY_LOCKED );
        }

        LockToken newToken = new LockToken( UUID.randomUUID().toString(), lockInfo, timeout );
        CurrentLock newLock = new CurrentLock( resource.getFile(), newToken, lockInfo.lockedByUser );
        locksByFile.put( resource.getFile(), newLock );
        locksByToken.put( newToken.tokenId, newLock );
        return LockResult.success( newToken );
    }

    @Override
    public synchronized LockResult refresh( String tokenId, LockableResource resource ) {
        CurrentLock curLock = locksByToken.get( tokenId );
        if( curLock == null ) {
          Logger.info(FileSystemResourceFactory.class, "can't refresh because no lock");
            return LockResult.failed( LockResult.FailureReason.PRECONDITION_FAILED );
        } else {
            curLock.token.setFrom( new Date() );
            return LockResult.success( curLock.token );
        }
    }

    @Override
    public synchronized void unlock( String tokenId, LockableResource r ) throws NotAuthorizedException {
        FsResource resource = (FsResource) r;
        LockToken lockToken = currentLock( resource );
        if( lockToken == null ) {
          Logger.info(FileSystemResourceFactory.class, "not locked" );
            return;
        }
        if( lockToken.tokenId.equals( tokenId ) ) {
            removeLock( lockToken );
        } else {
            throw new NotAuthorizedException( resource );
        }
    }

    private LockToken currentLock( FsResource resource ) {
        CurrentLock curLock = locksByFile.get( resource.getFile() );
        if( curLock == null ) return null;
        LockToken token = curLock.token;
        if( token.isExpired() ) {
            removeLock( token );
            return null;
        } else {
            return token;
        }
    }

    private void removeLock( LockToken token ) {
      Logger.info(FileSystemResourceFactory.class, "removeLock: " + token.tokenId );
        CurrentLock currentLock = locksByToken.get( token.tokenId );
        if( currentLock != null ) {
            locksByFile.remove( currentLock.file );
            locksByToken.remove( currentLock.token.tokenId );
        } else {
          Logger.info(FileSystemResourceFactory.class,"couldnt find lock: " + token.tokenId );
        }
    }

    @Override
    public LockToken getCurrentToken( LockableResource r ) {
        FsResource resource = (FsResource) r;
        CurrentLock lock = locksByFile.get( resource.getFile() );
        if( lock == null ) return null;
        LockToken token = new LockToken();
        token.info = new LockInfo( LockInfo.LockScope.EXCLUSIVE, LockInfo.LockType.WRITE, lock.lockedByUser, LockInfo.LockDepth.ZERO );
        token.info.lockedByUser = lock.lockedByUser;
        token.timeout = lock.token.timeout;
        token.tokenId = lock.token.tokenId;
        return token;
    }

    class CurrentLock {

        final File file;
        final LockToken token;
        final String lockedByUser;

        public CurrentLock( File file, LockToken token, String lockedByUser ) {
            this.file = file;
            this.token = token;
            this.lockedByUser = lockedByUser;
        }
    }
}
