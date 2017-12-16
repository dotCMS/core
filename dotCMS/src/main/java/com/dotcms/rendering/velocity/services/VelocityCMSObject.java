package com.dotcms.rendering.velocity.services;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;

import java.io.InputStream;

import com.liferay.portal.model.User;

public interface VelocityCMSObject {
    default User sysUser() {
        try {
            return APILocator.getUserAPI().getSystemUser();
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }
    
    public InputStream writeObject(String id1, String id2, boolean live, String language, String filePath) throws DotDataException, DotSecurityException ;
    public void invalidate(Object obj)  ;
    public void invalidate(Object obj, boolean live);
    
    
    
}
