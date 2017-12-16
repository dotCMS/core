package com.dotcms.rendering.velocity.services;


import com.dotcms.rendering.velocity.DotResourceCache;
import com.dotcms.rendering.velocity.VelocityType;
import com.dotcms.rendering.velocity.util.VelocityUtil;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.velocity.runtime.resource.ResourceManager;

/**
 * @author Jason Tesser
 * @since 1.6.5
 *
 */
public class FieldServices implements VelocityCMSObject {
	public static void invalidate(String fieldInode, String contentletIdent, boolean EDIT_MODE,String filePath) throws DotDataException, DotSecurityException {
		removeFieldFile(fieldInode, contentletIdent, EDIT_MODE);
	}
	
	public static InputStream buildVelocity(String fieldInode, String contentInode, boolean EDIT_MODE,String filePath) throws DotDataException, DotSecurityException {
		InputStream result;
		Field field = FieldFactory.getFieldByInode(fieldInode);
		if(!UtilMethods.isSet(field)){
			Logger.warn(FieldServices.class,"Field not found.  Unable to load velocity code");
			return new ByteArrayInputStream("".toString().getBytes());
		}
		ContentletAPI conAPI = APILocator.getContentletAPI();
		FieldAPI fAPI = APILocator.getFieldAPI();
		Contentlet content = conAPI.find(contentInode, APILocator.getUserAPI().getSystemUser(), true);
		if(!UtilMethods.isSet(content)){
			Logger.warn(FieldServices.class,"Content not found.  Unable to load velocity code");
			return new ByteArrayInputStream("".toString().getBytes());
		}
		Object contFieldValueObject = conAPI.getFieldValue(content, field);
		String contFieldValue = "";
		
		if(fAPI.isElementConstant(field) || fAPI.isElementHidden(field)){
			contFieldValue = field.getValues() == null ? "" : field.getValues();
		}else{
			contFieldValue = contFieldValueObject == null ? "" : contFieldValueObject.toString();
		}
		
		if(contFieldValue != null && contFieldValue.endsWith("#")){
			contFieldValue = contFieldValue.substring(0, contFieldValue.length()-1);
			contFieldValue += "$esc.h";
		}
		
        if (Config.getBooleanProperty("SHOW_VELOCITYFILES", false)) {
            File f = new File(ConfigUtils.getDynamicVelocityPath() + java.io.File.separator + filePath);
            f.mkdirs();
            f.delete();
            try (BufferedOutputStream tmpOut = new BufferedOutputStream(Files.newOutputStream(f.toPath()));
                    OutputStreamWriter out = new OutputStreamWriter(tmpOut, UtilMethods.getCharsetConfiguration())) {
                out.write(contFieldValue.toString());
                out.flush();
            } catch (Exception e) {
                Logger.error(ContentletServices.class, e.toString(), e);
            }

        }
		
		try {
			result = new ByteArrayInputStream(contFieldValue.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			result = new ByteArrayInputStream(contFieldValue.getBytes());
			Logger.error(FieldServices.class,e1.getMessage(), e1);
		}
		return result;
	}
	
	public static void removeFieldFile (String fieldInode, String contentInode, boolean EDIT_MODE) {
        String velocityRootPath = VelocityUtil.getVelocityRootPath();
        velocityRootPath += java.io.File.separator;
        String folderPath = (!EDIT_MODE) ? "live" + java.io.File.separator: "working" + java.io.File.separator;
        String filePath=folderPath + contentInode + "_" + fieldInode + "." + VelocityType.FIELD.fileExtension;
        java.io.File f  = new java.io.File(velocityRootPath + filePath);
        f.delete();
        DotResourceCache vc = CacheLocator.getVeloctyResourceCache2();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath );
    }

    @Override
    public InputStream writeObject(String id1, String id2, boolean live, String language, String filePath)
            throws DotDataException, DotSecurityException {

            return FieldServices.buildVelocity(id2, id1, !live, filePath);
        
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
