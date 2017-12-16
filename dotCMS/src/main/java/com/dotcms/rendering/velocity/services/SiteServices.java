package com.dotcms.rendering.velocity.services;


import com.dotcms.rendering.velocity.DotResourceCache;
import com.dotcms.rendering.velocity.VelocityType;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPI;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.liferay.portal.model.User;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import org.apache.velocity.runtime.resource.ResourceManager;

/**
 * @author will
 *
 *         To change this generated comment edit the template variable "typecomment":
 *         Window>Preferences>Java>Templates. To enable and disable the creation of type comments go
 *         to Window>Preferences>Java>Code Generation.
 */
public class SiteServices implements VelocityCMSObject {

    public static void invalidate(Host host) {


        invalidate(host, false);
        invalidate(host, true);
    }

    public static void invalidate(Host host, boolean EDIT_MODE) {
        removeHostFile(host, EDIT_MODE);
    }

    @SuppressWarnings("unchecked")
    public static InputStream buildStream(Host host, boolean EDIT_MODE, String filePath) throws DotDataException, DotSecurityException {

        InputStream result;
        StringBuilder sb = new StringBuilder();
        HostVariableAPI hostVariableAPI = APILocator.getHostVariableAPI();
        UserAPI userAPI = APILocator.getUserAPI();
        User user = userAPI.getSystemUser();
        List hvars = hostVariableAPI.getVariablesForHost(host.getIdentifier(), user, false);

        if (hvars.size() > 0) {
            Iterator hostvars = hvars.iterator();

            sb.append("#set ($host_variable = $contents.getEmptyMap())");
            int counter = 1;
            while (hostvars.hasNext()) {
                HostVariable next = (HostVariable) hostvars.next();
                sb.append("#set ($_dummy  = $host_variable.put(\"")
                    .append(String.valueOf(next.getKey()))
                    .append("\", \"")
                    .append(String.valueOf(UtilMethods.espaceForVelocity(next.getValue())))
                    .append("\"))");

                counter++;

            }
        }

        if (Config.getBooleanProperty("SHOW_VELOCITYFILES", false)) {
            File f = new File(ConfigUtils.getDynamicVelocityPath() + java.io.File.separator + filePath);
            f.mkdirs();
            f.delete();
            try (BufferedOutputStream tmpOut = new BufferedOutputStream(Files.newOutputStream(f.toPath()));
                    OutputStreamWriter out = new OutputStreamWriter(tmpOut, UtilMethods.getCharsetConfiguration())) {
                out.write(sb.toString());
                out.flush();
            } catch (Exception e) {
                Logger.error(SiteServices.class, e.toString(), e);
            }

        }

        try {
            result = new ByteArrayInputStream(sb.toString()
                .getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e1) {
            result = new ByteArrayInputStream(sb.toString()
                .getBytes());
            Logger.error(ContainerServices.class, e1.getMessage(), e1);
        }
        return result;
    }


    public static void unpublishPageFile(Host host) {

        removeHostFile(host, false);
    }



    public static void removeHostFile(Host host, boolean EDIT_MODE) {
        String folderPath = (!EDIT_MODE) ? "live" + java.io.File.separator : "working" + java.io.File.separator;
        String velocityRootPath = VelocityUtil.getVelocityRootPath();
        String filePath = folderPath + host.getIdentifier() + "." + VelocityType.SITE.fileExtension;
        velocityRootPath += java.io.File.separator;
        java.io.File f = new java.io.File(velocityRootPath + filePath);
        f.delete();
        DotResourceCache vc = CacheLocator.getVeloctyResourceCache2();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath);
    }

    @Override
    public InputStream writeObject(String id1, String id2, boolean live, String language, String filePath)
            throws DotDataException, DotSecurityException {

        Host host = APILocator.getHostAPI()
            .find(id1, sysUser(), false);

        return buildStream(host, !live, filePath);

    }

    @Override
    public void invalidate(Object obj) {
        removeHostFile((Host)obj, true);
        removeHostFile((Host)obj, false);
    }

    @Override
    public void invalidate(Object obj, boolean live) {
        removeHostFile((Host)obj, !live);

    }

}
