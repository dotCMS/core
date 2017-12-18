package com.dotcms.rendering.velocity.services;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import com.liferay.portal.model.User;

public interface VelocityCMSObject {
    default User sysUser() {
        try {
            return APILocator.getUserAPI()
                .getSystemUser();
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    public InputStream writeObject(String id1, String id2, boolean live, String language, String filePath)
            throws DotDataException, DotSecurityException;

    public void invalidate(Object obj);

    public void invalidate(Object obj, boolean live);

    default String writeOutVelocity(final String filePath, final String strOut) {
        if (Config.getBooleanProperty("SHOW_VELOCITYFILES", false)) {
            try {
                File f = new File(ConfigUtils.getDynamicVelocityPath() + java.io.File.separator + filePath);
                f.mkdirs();
                f.delete();
                try (final  BufferedWriter out = new java.io.BufferedWriter(new VelocityPrettyWriter(new FileOutputStream(f)))) {
                    out.write(strOut);
                }
            }
            catch (Exception e) {
                new DotStateException(e);
            }
        }
        return strOut;
    }

}
