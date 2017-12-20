package com.dotcms.rendering.velocity.services;


import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HiddenField;
import com.dotcms.rendering.velocity.util.VelocityUtil;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.velocity.runtime.resource.ResourceManager;

/**
 * @author Jason Tesser
 * @since 1.6.5
 *
 */
public class FieldLoader implements DotLoader {
	public void invalidate(String fieldInode, String contentletIdent, PageMode mode,String filePath) throws DotDataException, DotSecurityException {
		removeFieldFile(fieldInode, contentletIdent, mode);
	}
	
	public InputStream buildVelocity(String fieldInode, String contentInode, PageMode mode,String filePath) throws DotDataException, DotSecurityException {
		InputStream result;
		Field field = APILocator.getContentTypeFieldAPI().find(fieldInode);
		if(!UtilMethods.isSet(field)){
			Logger.warn(this.getClass(),"Field not found.  Unable to load velocity code");
			return new ByteArrayInputStream("".toString().getBytes());
		}
		ContentletAPI conAPI = APILocator.getContentletAPI();
		Contentlet content = conAPI.find(contentInode, APILocator.getUserAPI().getSystemUser(), true);
		if(!UtilMethods.isSet(content)){
			Logger.warn(this.getClass(),"Content not found.  Unable to load velocity code");
			return new ByteArrayInputStream("".toString().getBytes());
		}
		Object contFieldValueObject = conAPI.getFieldValue(content, field);
		String contFieldValue = "";
		
		
		
		if(field instanceof ConstantField || field instanceof HiddenField){
			contFieldValue = field.values() == null ? "" : field.values();
		}else{
			contFieldValue = contFieldValueObject == null ? "" : contFieldValueObject.toString();
		}
		
		if(contFieldValue != null && contFieldValue.endsWith("#")){
			contFieldValue = contFieldValue.substring(0, contFieldValue.length()-1);
			contFieldValue += "$esc.h";
		}
		

        return writeOutVelocity(filePath, contFieldValue);


	}
	
	public void removeFieldFile (String fieldInode, String contentInode, PageMode mode) {
        String velocityRootPath = VelocityUtil.getVelocityRootPath();
        velocityRootPath += java.io.File.separator;
        String folderPath = mode.name() + java.io.File.separator;
        String filePath=folderPath + contentInode + "_" + fieldInode + "." + VelocityType.FIELD.fileExtension;
        java.io.File f  = new java.io.File(velocityRootPath + filePath);
        f.delete();
        DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath );
    }

    @Override
    public InputStream writeObject(String id1, String id2, PageMode mode, String language, String filePath)
            throws DotDataException, DotSecurityException {

            return this.buildVelocity(id2, id1, mode, filePath);
        
    }



    @Override
    public void invalidate(Object obj, PageMode mode) {
        throw new DotStateException("Not Implemented, use removeFieldFile");
        
    }
}
