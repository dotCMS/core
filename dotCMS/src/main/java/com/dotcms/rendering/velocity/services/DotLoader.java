package com.dotcms.rendering.velocity.services;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import com.liferay.portal.model.User;

public interface DotLoader {
    default User sysUser() {
        try {
            return APILocator.getUserAPI()
                .getSystemUser();
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    public InputStream writeObject(String id1, String id2, PageMode mode, String language, String filePath)
            throws DotDataException, DotSecurityException;

    default void invalidate(Object obj) {
        for(PageMode mode : PageMode.values()) {
            invalidate(obj, mode);
        }
    }

    public void invalidate(Object obj, PageMode mode);

    
    default void invalidate(Object obj, PageMode ...modes) {
        for(PageMode mode : modes) {
            invalidate(obj, mode);
        }
    }
    
    default InputStream writeOutVelocity(final String filePath, final String strOut) {
        if (Config.getBooleanProperty("SHOW_VELOCITYFILES", false)) {
            try {
                File f = new File(ConfigUtils.getDynamicVelocityPath() + java.io.File.separator + filePath);
                f.mkdirs();
                f.delete();
                try (final BufferedWriter out = new java.io.BufferedWriter(new VelocityPrettyWriter(new FileOutputStream(f)))) {
                    out.write(strOut);
                }
            } catch (Exception e) {
                new DotStateException(e);
            }
        }
        try {
            return new ByteArrayInputStream(strOut.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e1) {
            Logger.error(this.getClass(), e1.getMessage(), e1);
            return new ByteArrayInputStream(strOut.getBytes());

        }
    }

}
