package com.dotcms.rendering.velocity.services;


import com.dotcms.rendering.velocity.util.VelocityUtil;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPI;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

import org.apache.velocity.runtime.resource.ResourceManager;

import com.liferay.portal.model.User;


public class SiteLoader implements DotLoader {

    public void invalidate(Host host) {


        invalidate(host, false);
        invalidate(host, true);
    }

    public void invalidate(Host host, boolean EDIT_MODE) {
        removeHostFile(host, EDIT_MODE);
    }

    @SuppressWarnings("unchecked")
    public InputStream buildStream(Host host, boolean EDIT_MODE, String filePath) throws DotDataException, DotSecurityException {

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

        return writeOutVelocity(filePath, sb.toString());
    }


    public void unpublishPageFile(Host host) {

        removeHostFile(host, false);
    }



    public void removeHostFile(Host host, boolean EDIT_MODE) {
        String folderPath = (!EDIT_MODE) ? "live" + java.io.File.separator : "working" + java.io.File.separator;
        String velocityRootPath = VelocityUtil.getVelocityRootPath();
        String filePath = folderPath + host.getIdentifier() + "." + VelocityType.SITE.fileExtension;
        velocityRootPath += java.io.File.separator;
        java.io.File f = new java.io.File(velocityRootPath + filePath);
        f.delete();
        DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
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
