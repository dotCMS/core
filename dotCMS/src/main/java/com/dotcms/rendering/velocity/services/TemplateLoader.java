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

import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
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



    public InputStream buildVelocity(Template template, PageMode mode, String filePath) {


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
    public InputStream writeObject(final VelocityResourceKey key) throws DotDataException, DotSecurityException {

        String templateId = key.id1;
        //if the key.id1 is not set means that the template that is being used is a FileAssetTemplate
        //we need to get the identifier from the path
        if(UtilMethods.isNotSet(templateId)){
            templateId = key.path.replaceAll(StringPool.FORWARD_SLASH + key.mode.name() + StringPool.FORWARD_SLASH ,"")
                    .replaceAll(StringPool.PERIOD + "templatelayout","");
        }
        Template template = null;
        if (key.mode.showLive) {
            template = APILocator.getTemplateAPI().findLiveTemplate(templateId,sysUser(), true);

        } else {
            template = APILocator.getTemplateAPI().findWorkingTemplate(templateId,sysUser(), true);
        }

        Logger.debug(this, "DotResourceLoader:\tWriting out Template inode = " + template.getInode());

        return buildVelocity(template, key.mode, key.path);


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
        
        VelocityResourceKey key = new VelocityResourceKey(template, mode);


        DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.remove(key);


    }
}
