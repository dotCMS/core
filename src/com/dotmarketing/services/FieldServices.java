/**
 * 
 */
package com.dotmarketing.services;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.apache.velocity.runtime.resource.ResourceManager;

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
import com.dotmarketing.velocity.DotResourceCache;
import com.liferay.util.FileUtil;

/**
 * @author Jason Tesser
 * @since 1.6.5
 *
 */
public class FieldServices {

	public static void invalidate(String fieldInode, String contentletIdent, boolean EDIT_MODE) throws DotDataException, DotSecurityException {
		removeFieldFile(fieldInode, contentletIdent, EDIT_MODE);
	}
	
	public static InputStream buildVelocity(String fieldInode, String contentInode, boolean EDIT_MODE) throws DotDataException, DotSecurityException {
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
		
		if(fAPI.isElementConstant(field)){
			contFieldValue = field.getValues() == null ? "" : field.getValues();
		}else{
			contFieldValue = contFieldValueObject == null ? "" : contFieldValueObject.toString();
		}
		
		if(contFieldValue != null && contFieldValue.endsWith("#")){
			contFieldValue = contFieldValue.substring(0, contFieldValue.length()-1);
			contFieldValue += "$esc.h";
		}
		
		 if(Config.getBooleanProperty("SHOW_VELOCITYFILES", false)){
			String velocityRootPath = Config.getStringProperty("VELOCITY_ROOT");
			if (velocityRootPath.startsWith("/WEB-INF")) {
			    velocityRootPath = FileUtil.getRealPath(velocityRootPath);
			}
			velocityRootPath += java.io.File.separator;
			
			String folderPath = (!EDIT_MODE) ? "live" + java.io.File.separator: "working" + java.io.File.separator;
			String filePath=folderPath + contentInode + "_" + fieldInode + "." + Config.getStringProperty("VELOCITY_FIELD_EXTENSION");
            //Specify a proper character encoding
         	try{
         		java.io.BufferedOutputStream tmpOut = new java.io.BufferedOutputStream(new java.io.FileOutputStream(new java.io.File(ConfigUtils.getDynamicVelocityPath()+java.io.File.separator + filePath)));
	            OutputStreamWriter out = new OutputStreamWriter(tmpOut, UtilMethods.getCharsetConfiguration());
	            
	            out.write(contFieldValue.toString());
	            
	            out.flush();
	            out.close();
	            tmpOut.close();
         	}catch (Exception e) {
				Logger.error(FieldServices.class,"Unable to write velocity field file");
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
        String velocityRootPath = Config.getStringProperty("VELOCITY_ROOT");
        if (velocityRootPath.startsWith("/WEB-INF")) {
            velocityRootPath = FileUtil.getRealPath(velocityRootPath);
        }
        velocityRootPath += java.io.File.separator;
        String folderPath = (!EDIT_MODE) ? "live" + java.io.File.separator: "working" + java.io.File.separator;
        String filePath=folderPath + contentInode + "_" + fieldInode + "." + Config.getStringProperty("VELOCITY_FIELD_EXTENSION");
        java.io.File f  = new java.io.File(velocityRootPath + filePath);
        f.delete();
        DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath );
    }
}
