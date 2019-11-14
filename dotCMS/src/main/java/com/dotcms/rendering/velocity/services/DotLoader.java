package com.dotcms.rendering.velocity.services;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * The DotLoaders exist to distill CMS objects down to Velocity templates - 
 * which can then be cached in Velocity and parsed very quickly.
 * This is done because the performance of rendering velocity templates is
 * ~10x greater vs. pulling the objects out of a cache and using them RAW
 * within Velocity
 * @author will
 *
 */
public interface DotLoader {
    default User sysUser() {
        try {
            return APILocator.getUserAPI()
                .getSystemUser();
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    public InputStream writeObject(VelocityResourceKey key)
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
                try (final VelocityPrettyWriter out = new VelocityPrettyWriter(System.out)) {
                    out.write(strOut);
                }
            } catch (Exception e) {
                new DotStateException(e);
            }
        }

        //  This is very useful for debugging purposes un-comment as needed
        //  try {
        //    final VelocityPrettyWriter out = new VelocityPrettyWriter(System.out);
        //    out.write(strOut);
        //    out.flush();
        //  } catch (Exception e) {
        //      Logger.error(DotLoader.class,e);
        //  }

        return new ByteArrayInputStream(strOut.getBytes(StandardCharsets.UTF_8));
    }

}
