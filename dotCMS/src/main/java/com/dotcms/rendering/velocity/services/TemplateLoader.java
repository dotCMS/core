package com.dotcms.rendering.velocity.services;


import com.dotcms.rendering.velocity.util.VelocityUtil;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;

import java.io.InputStream;

import org.apache.velocity.runtime.resource.ResourceManager;

/**
 * @author will
 *
 *         To change this generated comment edit the template variable "typecomment":
 *         Window>Preferences>Java>Templates. To enable and disable the creation of type comments go
 *         to Window>Preferences>Java>Code Generation.
 */
public class TemplateLoader implements DotLoader {



    public InputStream buildVelocity(Template template, PageMode mode, String filePath)
            throws DotStateException, DotDataException {
        Identifier identifier = APILocator.getIdentifierAPI()
            .find(template);
        return buildVelocity(template, identifier, mode, filePath);
    }

    public InputStream buildVelocity(Template template, Identifier identifier, PageMode mode, String filePath) {


        StringBuilder templateBody = new StringBuilder();


        if(template.isDrawed()){
            templateBody.append(template.getDrawedBody());
        }
        else {
            templateBody.append(template.getBody());
        }


        return writeOutVelocity(filePath, templateBody.toString());
    }



    @Override
    public InputStream writeObject(String id1, String id2, PageMode mode, String language, String filePath)
            throws DotDataException, DotSecurityException {

        Identifier identifier = APILocator.getIdentifierAPI()
            .find(id1);
        VersionableAPI versionableAPI = APILocator.getVersionableAPI();
        Template template = null;
        if (mode.showLive) {
            template = (Template) versionableAPI.findLiveVersion(identifier, sysUser(), true);

        } else {
            template = (Template) versionableAPI.findWorkingVersion(identifier, sysUser(), true);
        }

        Logger.debug(this, "DotResourceLoader:\tWriting out Template inode = " + template.getInode());

        return buildVelocity(template, mode, filePath);


    }

    @Override
    public void invalidate(Object obj) {
        for (PageMode mode : PageMode.values()) {
            invalidate(obj, mode);
        }

    }

    @Override
    public void invalidate(Object obj, PageMode mode) {
        Template template = (Template) obj;
        String velocityRootPath = VelocityUtil.getVelocityRootPath();
        velocityRootPath += java.io.File.separator;

        String folderPath = mode.name() + java.io.File.separator;
        String filePath = folderPath + template.getIdentifier() + "." + VelocityType.TEMPLATE.fileExtension;
        java.io.File f = new java.io.File(velocityRootPath + filePath);
        f.delete(); // todo: check if the file exists before remove?
        DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath);


    }
}
