package com.dotcms.rendering.velocity.services;


import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HiddenField;
import com.dotcms.rendering.velocity.util.VelocityUtil;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.velocity.runtime.resource.ResourceManager;

/**
 * @author Jason Tesser
 * @since 1.6.5
 *
 */
public class FieldLoader implements VelocityCMSObject {
	public void invalidate(String fieldInode, String contentletIdent, boolean EDIT_MODE,String filePath) throws DotDataException, DotSecurityException {
		removeFieldFile(fieldInode, contentletIdent, EDIT_MODE);
	}
	
	public InputStream buildVelocity(String fieldInode, String contentInode, boolean EDIT_MODE,String filePath) throws DotDataException, DotSecurityException {
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
		

		
		try {

			result = new ByteArrayInputStream(contFieldValue.getBytes("UTF-8"));
	         writeOutVelocity(filePath, contFieldValue);
		} catch (UnsupportedEncodingException e1) {
			result = new ByteArrayInputStream(contFieldValue.getBytes());
			Logger.error(this.getClass(),e1.getMessage(), e1);
		}
        
		return result;
	}
	
	public void removeFieldFile (String fieldInode, String contentInode, boolean EDIT_MODE) {
        String velocityRootPath = VelocityUtil.getVelocityRootPath();
        velocityRootPath += java.io.File.separator;
        String folderPath = (!EDIT_MODE) ? "live" + java.io.File.separator: "working" + java.io.File.separator;
        String filePath=folderPath + contentInode + "_" + fieldInode + "." + VelocityType.FIELD.fileExtension;
        java.io.File f  = new java.io.File(velocityRootPath + filePath);
        f.delete();
        DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath );
    }

    @Override
    public InputStream writeObject(String id1, String id2, boolean live, String language, String filePath)
            throws DotDataException, DotSecurityException {

            return this.buildVelocity(id2, id1, !live, filePath);
        
    }

    @Override
    public void invalidate(Object obj) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void invalidate(Object obj, boolean live) {
        // TODO Auto-generated method stub
        
    }
}
