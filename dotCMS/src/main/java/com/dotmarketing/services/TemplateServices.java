package com.dotmarketing.services;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.velocity.DotResourceCache;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.velocity.runtime.resource.ResourceManager;

/**
 * @author will
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class TemplateServices {
	
    public static void invalidate(Template template) throws DotStateException, DotDataException {

    	Identifier identifier = APILocator.getIdentifierAPI().find(template);
    	invalidate(template, identifier, false);

    }

    public static void invalidate(Template template, boolean EDIT_MODE) throws DotStateException, DotDataException {

    	Identifier identifier = APILocator.getIdentifierAPI().find(template);
    	invalidate(template, identifier, EDIT_MODE);

    }
	
    public static InputStream buildVelocity(Template template, boolean EDIT_MODE) throws DotStateException, DotDataException {
    	Identifier identifier = APILocator.getIdentifierAPI().find(template);
    	return buildVelocity(template, identifier, EDIT_MODE);
    }
    
    public static InputStream buildVelocity(Template template, Identifier identifier, boolean EDIT_MODE) {

    	InputStream result;
    	StringBuilder templateBody = new StringBuilder();
        try {
            String folderPath = (!EDIT_MODE) ? "live" + java.io.File.separator: "working" + java.io.File.separator;
            String filePath=folderPath + identifier.getInode() + "." + Config.getStringProperty("VELOCITY_TEMPLATE_EXTENSION");

            templateBody.append(Constants.TEMPLATE_PREPROCESS);
            templateBody.append(template.getBody());
            templateBody.append(Constants.TEMPLATE_POSTPROCESS);
            
            if(Config.getBooleanProperty("SHOW_VELOCITYFILES", false)){
            	final BufferedOutputStream tmpOut = new BufferedOutputStream(
            	        Files.newOutputStream(
            	                Paths.get(ConfigUtils.getDynamicVelocityPath()+java.io.File.separator + filePath)));
	            //Specify a proper character encoding
	            OutputStreamWriter out = new OutputStreamWriter(tmpOut, UtilMethods.getCharsetConfiguration());
	            
	            out.write(templateBody.toString());
	            
	            out.flush();
	            out.close();
	            tmpOut.close();
            }
            
        } catch (Exception e) {
	        Logger.error(TemplateServices.class, e.toString(), e);
        }
        
        try {
			result = new ByteArrayInputStream(templateBody.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			result = new ByteArrayInputStream(templateBody.toString().getBytes());
			Logger.error(TemplateServices.class,e1.getMessage(), e1);
		}
        return result;
    }
    
	public static void invalidate(Template template, Identifier identifier, boolean EDIT_MODE) {
        removeTemplateFile(template, identifier, EDIT_MODE);
    }

    
    public static void unpublishTemplateFile(Template asset) throws DotStateException, DotDataException {

        Identifier identifier = APILocator.getIdentifierAPI().find(asset);
        removeTemplateFile(asset, identifier, false);
        removeTemplateFile(asset, identifier, true);
    }
    
    public static void removeTemplateFile(Template asset, boolean EDIT_MODE) throws DotStateException, DotDataException {

        Identifier identifier = APILocator.getIdentifierAPI().find(asset);
        removeTemplateFile(asset, identifier, EDIT_MODE);
    }
    
    public static void removeTemplateFile (Template asset, Identifier identifier, boolean EDIT_MODE) {
        String velocityRootPath = VelocityUtil.getVelocityRootPath();
        velocityRootPath += java.io.File.separator;

        String folderPath = (!EDIT_MODE) ? "live" + java.io.File.separator: "working" + java.io.File.separator;
        String filePath=folderPath + identifier.getInode() + "." + Config.getStringProperty("VELOCITY_TEMPLATE_EXTENSION");
        java.io.File f  = new java.io.File(velocityRootPath + filePath);
        f.delete(); // todo: check if the file exists before remove?
        DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath );
    }    
}
