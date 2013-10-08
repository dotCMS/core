package com.dotmarketing.portlets.cmsmaintenance.ajax;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;


/**
 * This class allows user to modify File Assets content using a string search and replace.
 * This method only modify the file, not the database.
 * @author Oswaldo
 *
 */
public class AssetsSearchAndReplaceAjax extends AjaxAction{

	private FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	
	/**
	 * Replace the specified String, with the new given value in all the specified File Assets
	 * @param searchText Text to search
	 * @param replaceText Text replacement
	 * @param assets List of File Assets to modify 
	 * @param generateNewAssetVersion True if you want to generate a new version of the file assets; false if you want to overwrite the current version
	 * @param publish True is you want to publish the new file asset version; false if you do not want to publish the new version
	 * @param user User executing the assets search and replace
	 * @return Map<String,Object> Map with the amount of file processed, updated and errors.
	 */
	private Map<String,Object> AssetsSearchAndReplace(String searchText, String replaceText, List<FileAsset> assets,boolean generateNewAssetVersion, boolean publish,User user) {
		Map<String,Object> results = new HashMap<String,Object>();
		String errorMessages = "";
		int toprocess=assets.size();
		int processed=0;
		int errors=0;
		int matches=0;
		for(FileAsset file : assets){	
			try{
				String currentText = getWorkingTextFile(file);
				if(currentText.indexOf(searchText) != -1){
					String newText = currentText.replaceAll(searchText, replaceText);
					saveFileText(file, newText, user, generateNewAssetVersion, publish,false);
					matches++;
				}
				processed++;
			}catch(Exception e){
				Logger.error(AssetsSearchAndReplaceAjax.class, e.getMessage(), e);
				errorMessages=errorMessages+"<br/>"+e.getMessage();
				errors++;
			}
		}
		results.put("toprocess", toprocess);
		results.put("processed", processed);
		results.put("matches", matches);
		results.put("errors", errors);
		results.put("errorMessages", errorMessages);
		return results;
	}

	/**
	 * Get the text of the specified File Asset
	 * @param file File Asset
	 * @return String String with the text of the specified File Asset
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws PortalException
	 * @throws SystemException
	 * @throws IOException
	 */
	private String getWorkingTextFile(FileAsset file) throws DotDataException, DotSecurityException,
	PortalException, SystemException, IOException {
		java.io.File fileIO = file.getFileAsset();
		FileInputStream fios = new FileInputStream(fileIO);
		byte[] data = new byte[fios.available()];
		fios.read(data);
		String text = new String(data);
		return text;
	}

	/**
	 * Save the File Asset with the new text
	 * @param file File Asset to save
	 * @param newText Text to save
	 * @param user User executing the assets search and replace
	 * @param generateNewAssetVersion True if you want to generate a new version of the file assets; false if you want to overwrite the current version
	 * @param publish True is you want to publish the new file asset version; false if you do not want to publish the new version
	 * @param respectFrontendRoles
	 * @throws PortalException
	 * @throws SystemException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws IOException
	 */
	@SuppressWarnings("static-access")
	private void saveFileText(FileAsset file, String newText,User user, boolean generateNewAssetVersion, boolean publish,boolean respectFrontendRoles) throws PortalException, SystemException,
	DotDataException, DotSecurityException, IOException {

		java.io.File fileData = null;
		if(generateNewAssetVersion){
			fileData =  new java.io.File(APILocator.getFileAPI().getRealAssetPath() + java.io.File.separator + file.getInode().charAt(0)
					+ java.io.File.separator + file.getInode().charAt(1) + java.io.File.separator + file.getInode()
					+ java.io.File.separator + APILocator.getFileAssetAPI().BINARY_FIELD + java.io.File.separator + WebKeys.TEMP_FILE_PREFIX + file.getFileAsset().getName());
		}else{
			fileData = file.getFileAsset();
		}
		fileData.deleteOnExit();
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(fileData);
			fos.write(newText.getBytes());
		} finally {
			if (fos != null)
				fos.close();
		}
		if(generateNewAssetVersion){
			file.setBinary(FileAssetAPI.BINARY_FIELD, fileData);
			Contentlet newAsset =null;
			file.setInode(null);
			newAsset = conAPI.checkin(file, user, false);
			APILocator.getVersionableAPI().setWorking(newAsset);
			if(publish){
				APILocator.getVersionableAPI().setLive(newAsset);
			}
			conAPI.refresh(newAsset);
		}
	}

	/**
	 * Replace File Assets content searching by file extension
	 * @param request
	 * @param response
	 */
	public void replaceByFiles(HttpServletRequest request, HttpServletResponse response) {
		Map<String, String> map = getURIParams();
		String results="";
		try {
			Logger.info(AssetsSearchAndReplaceAjax.class, "Beginning search and replace by Files");
			User user = APILocator.getUserAPI().loadUserById(map.get("user"), APILocator.getUserAPI().getSystemUser(), false);
			String searchText = map.get("searchText");
			String replaceText = map.get("replaceText");			
			boolean generateNewAssetVersion = new Boolean(map.get("generateNewAssetVersion"));
			boolean publish = new Boolean(map.get("publish"));
			String assetExtensions = map.get("replaceByFiles");
			String hosts = map.get("hosts");
			String hostQuery="";
			if(UtilMethods.isSet(hosts) && !hosts.equals("all")){
				for(String hostId : hosts.split(",")){
					hostQuery=hostQuery+" conhost:"+hostId;
				}
				hostQuery="+("+hostQuery+")";
			}
			String fileExtensions="";
			if(UtilMethods.isSet(assetExtensions)){
				for(String extension : assetExtensions.split(",")){
					fileExtensions=fileExtensions+" fileAsset.fileName:*."+extension;
				}
				fileExtensions=" +("+fileExtensions+")";
			}
			String query="+structureName:fileAsset +structureType:"+Structure.STRUCTURE_TYPE_FILEASSET+" +working:true +deleted:false "+hostQuery+" "+fileExtensions;
			long numberOfContentltet = conAPI.indexCount(query, user, true);

			int perSearch = Integer.parseInt(Config.getStringProperty("ASSETS_SEARCH_AND_REPLACE_MAX_NUMBER_OF_ASSET_TO_SEARCH"));
			int offset = 0;
			int numberOfCycles=0;
			long totalCyles = (numberOfContentltet/perSearch)+1;
			
			long toprocess=numberOfContentltet;
			int processed=0;
			int errors=0;
			int matches=0;
			String errorMessages="";
			
			while(numberOfCycles < totalCyles){
				HibernateUtil.startTransaction();
				
				offset = numberOfCycles * perSearch;
				List<Contentlet> fileContents = conAPI.search(query, perSearch, offset, "modDate asc" , user, false);
				List<FileAsset> fileAssets = fileAssetAPI.fromContentlets(fileContents);
				Map<String,Object> tempResults = AssetsSearchAndReplace(searchText, replaceText, fileAssets, generateNewAssetVersion, publish, user);
				
				HibernateUtil.commitTransaction();
				processed= processed+(Integer)tempResults.get("processed");
				matches= matches+(Integer)tempResults.get("matches");
				errors= errors+(Integer)tempResults.get("errors");
				errorMessages= errorMessages+"<br/>"+tempResults.get("errorMessages");
								
				numberOfCycles++;
			}			
			results=toprocess+"|"+processed+"|"+matches+"|"+errors+"|"+errorMessages;
			Logger.debug(AssetsSearchAndReplaceAjax.class, "Files to process: "+toprocess+" - Processed: "+processed+" - Matches: "+matches+" - Errors: "+errors+" - Error Messages"+errorMessages);
			Logger.info(AssetsSearchAndReplaceAjax.class, "Search and replace by Files has ended");
			response.getWriter().print(results);
		} catch (Exception e) {
			try {
				HibernateUtil.rollbackTransaction();
				response.getWriter().print(e.getMessage());
			} catch (Exception ex) {
				Logger.error(AssetsSearchAndReplaceAjax.class, e.getMessage(), e.getCause());
				return;
			}
		}

	}

	/**
	 * Replace File Assets content searching by File Asset identifiers
	 * @param request
	 * @param response
	 */
	public void replaceByIds(HttpServletRequest request, HttpServletResponse response) {
		Map<String, String> map = getURIParams();
		String results="";
		try {
			Logger.info(AssetsSearchAndReplaceAjax.class, "Beginning search and replace by Ids");
			User user = APILocator.getUserAPI().loadUserById(map.get("user"), APILocator.getUserAPI().getSystemUser(), false);
			String searchText = map.get("searchText");
			String replaceText = map.get("replaceText");
			String identifierList = map.get("replaceByIds");
			boolean generateNewAssetVersion = new Boolean(map.get("generateNewAssetVersion"));
			boolean publish = new Boolean(map.get("publish"));

			List<FileAsset> assetsList = new ArrayList<FileAsset>();
			for(String fileId : identifierList.split(",")){	
				Contentlet fileContent = conAPI.search("+identifier:"+fileId+" +structureType:" + Structure.STRUCTURE_TYPE_FILEASSET+" +working:true +deleted:false", 1, 0, "modDate asc" , user, false).get(0);
				FileAsset file = fileAssetAPI.fromContentlet(fileContent);
				assetsList.add(file);
			}
			
			long numberOfContentltet = assetsList.size();
			long toprocess=numberOfContentltet;
			int processed=0;
			int errors=0;
			int matches=0;
			String errorMessages="";
			
			HibernateUtil.startTransaction();
				
			Map<String,Object> tempResults = AssetsSearchAndReplace(searchText, replaceText, assetsList, generateNewAssetVersion, publish, user);
				
			HibernateUtil.commitTransaction();
			processed= processed+(Integer)tempResults.get("processed");
			matches= matches+(Integer)tempResults.get("matches");
			errors= errors+(Integer)tempResults.get("errors");
			errorMessages= errorMessages+"<br/>"+tempResults.get("errorMessages");
								
			results=toprocess+"|"+processed+"|"+matches+"|"+errors+"|"+errorMessages;
			Logger.debug(AssetsSearchAndReplaceAjax.class, "Files to process: "+toprocess+" - Processed: "+processed+" - Matches: "+matches+" - Errors: "+errors+" - Error Messages"+errorMessages);
			Logger.info(AssetsSearchAndReplaceAjax.class, "Search and replace by Ids has finished");
			response.getWriter().print(results);
		} catch (Exception e) {
			try {
				HibernateUtil.rollbackTransaction();
				response.getWriter().print(e.getMessage());
			} catch (Exception ex) {
				Logger.error(AssetsSearchAndReplaceAjax.class, e.getMessage(), e.getCause());
				return;
			}
		}

	}
	
	/**
	 * Return a string with the amount of File Assets to be processed
	 * @param request
	 * @param response
	 */
	public void assetCountByFiles(HttpServletRequest request, HttpServletResponse response) {
		Map<String, String> map = getURIParams();
		String results="";
		try {
			User user = APILocator.getUserAPI().loadUserById(map.get("user"), APILocator.getUserAPI().getSystemUser(), false);
			String assetExtensions = map.get("replaceByFiles");
			String hosts = map.get("hosts");
			String hostQuery="";
			if(UtilMethods.isSet(hosts) && !hosts.equals("all")){
				for(String hostId : hosts.split(",")){
					hostQuery=hostQuery+" conhost:"+hostId;
				}
				hostQuery="+("+hostQuery+")";
			}
			String fileExtensions="";
			if(UtilMethods.isSet(assetExtensions)){
				for(String extension : assetExtensions.split(",")){
					fileExtensions=fileExtensions+" fileAsset.fileName:*."+extension;
				}
				fileExtensions=" +("+fileExtensions+")";
			}
			String query="+structureName:fileAsset +structureType:"+Structure.STRUCTURE_TYPE_FILEASSET+" +working:true +deleted:false "+hostQuery+" "+fileExtensions;
			long numberOfContentlet = conAPI.indexCount(query, user, true);

			results = ""+numberOfContentlet;		
			Logger.debug(AssetsSearchAndReplaceAjax.class, numberOfContentlet+"File Asset(s) to process");
			response.getWriter().print(results);
		} catch (Exception e) {
			try {
				response.getWriter().print(e.getMessage());
			} catch (Exception ex) {
				Logger.error(AssetsSearchAndReplaceAjax.class, e.getMessage(), e.getCause());
				return;
			}
		}
	}
	
	/**
	 * Manage the Assets search and replace ajax requests
	 */
	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String url = request.getRequestURI().toString();
		String cmd = "";		
		if(url.indexOf("assetCountByFiles") != -1){
			cmd = "assetCountByFiles";	
		}else if(url.indexOf("replaceByIds") != -1){
			cmd = "replaceByIds";		
		}else if(url.indexOf("replaceByFiles") != -1){
			cmd = "replaceByFiles";				
		}else{
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		java.lang.reflect.Method meth = null;
		Class<?> partypes[] = new Class[] { HttpServletRequest.class, HttpServletResponse.class };
		Object arglist[] = new Object[] { request, response };

		try {
			meth = this.getClass().getMethod(cmd, partypes);

		} catch (Exception e) {

			try {
				cmd = "action";
				meth = this.getClass().getMethod(cmd, partypes);
			} catch (Exception ex) {
				Logger.error(AssetsSearchAndReplaceAjax.class, "Trying to run method:" + cmd);
				Logger.error(AssetsSearchAndReplaceAjax.class, e.getMessage(), e.getCause());
				return;
			}
		}
		try {
			meth.invoke(this, arglist);
		} catch (Exception e) {
			Logger.error(AssetsSearchAndReplaceAjax.class, "Trying to run method:" + cmd);
			Logger.error(AssetsSearchAndReplaceAjax.class, e.getMessage(), e.getCause());
			return;
		}		
	}	

	/**
	 * Initializes this instance using the given data
	 */
	@Override
	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		return;

	}
}