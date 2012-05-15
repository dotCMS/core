package com.dotmarketing.portlets.templates.design.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.templates.design.bean.PreviewFileAsset;
import com.dotmarketing.util.Config;

/**
 * This class contains a list of utility's methods for the preview of the template.
 * 
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * May 7, 2012 - 3:54:00 PM
 */
public class PreviewTemplateUtil {
	
	private static SimpleDateFormat SDF = new SimpleDateFormat("MMddyyyy");
	
	/**
	 * This method returns a Versionable list that contains all the JS and CSS imported files into the template. 
	 * 
	 * May 7, 2012 - 4:17:26 PM
	 */
	public static List<Versionable> getTemplateImportedFiles(String _body) throws DotDataException, DotSecurityException{		
		// get all files inodes
		List<PreviewFileAsset> inodes = DesignTemplateUtil.getFilesInodes(_body);
		List<Versionable> result = new ArrayList<Versionable>();

		for(PreviewFileAsset pfa: inodes){
			// check if it's a contentlet
			if(pfa.isContentlet()){
				Contentlet aContentlet = APILocator.getContentletAPI().find(pfa.getInode(), APILocator.getUserAPI().getSystemUser(), false);
				result.add(aContentlet);
			}else{ //a File
				File aFile = APILocator.getFileAPI().find(pfa.getInode(), APILocator.getUserAPI().getSystemUser(), false);
				result.add(aFile);
			}
		}
		return result;
	}
	
	/**
	 * This method save the imported file on preview file system and return the bean that represent the file.   
	 * 
	 * May 7, 2012 - 4:17:26 PM
	 */
	public static PreviewFileAsset savePreviewFileAsset(Versionable asset) throws IOException, DotDataException, DotSecurityException{
		PreviewFileAsset result = new PreviewFileAsset();
		String today = SDF.format(new GregorianCalendar().getTime());
		java.io.File todayPreviewAssetDir = new java.io.File(new java.io.File(Config.CONTEXT.getRealPath(Config.getStringProperty("PREVIEW_ASSET_PATH"))), today);
		if(!todayPreviewAssetDir.exists())
			todayPreviewAssetDir.mkdir();
		
		java.io.File importedFilesAssetDir = new java.io.File(todayPreviewAssetDir, "files");
		if(!importedFilesAssetDir.exists())
			importedFilesAssetDir.mkdir();
		
		java.io.File templatesAssetDir = new java.io.File(todayPreviewAssetDir, "templates");
		if(!templatesAssetDir.exists())
			templatesAssetDir.mkdir();
		
		java.io.File assetFile = null;
		java.io.File previewAsset = null;		
		// if the file imported is a FileAsset/Contentlet...
		if(asset instanceof Contentlet){
			Contentlet c = (Contentlet)asset;
			assetFile = APILocator.getContentletAPI().getBinaryFile(c.getInode(), FileAssetAPI.BINARY_FIELD, APILocator.getUserAPI().getSystemUser());			
			previewAsset = new java.io.File(importedFilesAssetDir, assetFile.getName());
			if(previewAsset.exists())
				previewAsset.delete();
			result.setContentlet(true);
			result.setInode(c.getInode());
			result.setParent(c.getFolder());
		}else if(asset instanceof File){ // is a File
			File f = (File)asset;
			assetFile =  APILocator.getFileAPI().getAssetIOFile(f);
			previewAsset = new java.io.File(importedFilesAssetDir, assetFile.getName());
			if(previewAsset.exists())
				previewAsset.delete();
			result.setContentlet(false);
			result.setInode(f.getInode());
			result.setParent(f.getParent());
		}		
		//set the real path for the body preview
		result.setRealFileSystemPath(previewAsset.getPath().substring(previewAsset.getPath().indexOf("/_preview")));		
		FileInputStream fis = new FileInputStream(assetFile);
		FileOutputStream fos = new FileOutputStream(previewAsset);		
		final byte[] buffer = new byte[ 1024 ];
        int n = 0;
        while ((n = fis.read(buffer)) > 0){
        	fos.write( buffer, 0, n );
        }		
        fis.close();
        fos.close();        
        return result;
	}
	
	/**
	 * This method returns all the containers into the template body.
	 * 
	 * May 7, 2012 - 4:17:26 PM
	 */
	public static List<Container> getContainers(StringBuffer templateBody) throws DotDataException, DotSecurityException {
		List<Container> result = new ArrayList<Container>();
		int i=0;
		String _templateBody = templateBody.toString();
		while(_templateBody.length()>i){
			i = _templateBody.indexOf("#parseContainer('");
			if(i>0){
				//delete the before part
				_templateBody = _templateBody.substring(i);
				String inodeContainer = _templateBody.substring("#parseContainer('".length(), _templateBody.indexOf("')"));
				Container c = APILocator.getContainerAPI().getWorkingContainerById(inodeContainer, APILocator.getUserAPI().getSystemUser(), false);
				if(!c.isForMetadata())
					result.add(c);
				int start = (_templateBody.indexOf("#parseContainer('"+inodeContainer+"')"))+("#parseContainer('"+inodeContainer+"')").length();
				_templateBody = _templateBody.substring(start);
			}else
				break;
			i=0;
		}
		return result;
	}
	
	public static StringBuffer getMockBodyContent(){
		StringBuffer sb = new StringBuffer();
		sb.append("<h3>Lorem ipsum dolor sit amet</h3>");
		sb.append("<p>Nam sollicitudin est eleifend tellus porta semper. Vivamus et arcu sapien, at tincidunt leo. Ut euismod egestas est, vitae aliquam nulla porttitor eu.</p>");
		sb.append("<p>Nam augue purus, sagittis at adipiscing ut, molestie vel lectus. Sed porttitor dapibus libero, id porta justo tristique id.</p>");
		return sb;
	}

	public static boolean removePreviewDirectory(java.io.File todayPreviewAssetDir) {
		if (!todayPreviewAssetDir.exists())
		    return true;
		if (!todayPreviewAssetDir.isDirectory())
		    return false;
		String[] list = todayPreviewAssetDir.list();
		if (list != null) {
			for (int i = 0; i < list.length; i++) {
				java.io.File entry = new java.io.File(todayPreviewAssetDir, list[i]);
				if (entry.isDirectory()){
					if (!removePreviewDirectory(entry))
						return false;
				}else {
					if (!entry.delete())
						return false;
				}
		    }
		}
		return todayPreviewAssetDir.delete();
	}
}
