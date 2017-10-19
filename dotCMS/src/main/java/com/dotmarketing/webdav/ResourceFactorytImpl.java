/**
 * 
 */
package com.dotmarketing.webdav;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.repackage.com.bradmcevoy.http.ApplicationConfig;
import com.dotcms.repackage.com.bradmcevoy.http.HttpManager;
import com.dotcms.repackage.com.bradmcevoy.http.Initable;
import com.dotcms.repackage.com.bradmcevoy.http.Resource;
import com.dotcms.repackage.com.bradmcevoy.http.ResourceFactory;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

/**
 * @author Jason Tesser
 *
 */
public class ResourceFactorytImpl implements ResourceFactory, Initable {

	private DotWebdavHelper dotDavHelper;
	private static final String AUTOPUB_PATH = "/webdav/autopub";
	private static final String NONPUB_PATH = "/webdav/nonpub";
	private static final String LIVE_PATH = "/webdav/live";
	private static final String WORKING_PATH = "/webdav/working";
	private HostAPI hostAPI = APILocator.getHostAPI();
	
	public ResourceFactorytImpl() {
		super();
		dotDavHelper = new DotWebdavHelper();
	}
	
	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.ResourceFactory#getResource(java.lang.String, java.lang.String)
	 */
    @WrapInTransaction
	public Resource getResource(String davHost, String url) {
		url = url.toLowerCase();
    	Logger.debug(this, "WebDav ResourceFactory: Host is " + davHost + " and the url is " + url);
		try{
			boolean isFolder = false;
			boolean isResource = false;
			boolean isWebDavRoot = url.equals(AUTOPUB_PATH) || url.equals(NONPUB_PATH) || url.equals(LIVE_PATH + "/" +dotDavHelper.getLanguage()) || url.equals(WORKING_PATH + "/" +dotDavHelper.getLanguage()) 
					|| url.equals(AUTOPUB_PATH + "/") || url.equals(NONPUB_PATH + "/") || url.equals(LIVE_PATH + "/" +dotDavHelper.getLanguage() + "/") || url.equals(WORKING_PATH + "/" +dotDavHelper.getLanguage() + "/") ;
			boolean live = url.startsWith(AUTOPUB_PATH) || url.startsWith(LIVE_PATH);
			boolean working = url.startsWith(NONPUB_PATH) || url.startsWith(WORKING_PATH);
			Host host =null;
			String actualPath = url; 
			
			// DAV ROOT
			if(isWebDavRoot){
				WebdavRootResourceImpl wr = new WebdavRootResourceImpl(url);
				return wr;
			}
			
			
			//SETUP
			if(live){
				actualPath = actualPath.replaceAll(AUTOPUB_PATH, "");
				actualPath = actualPath.replaceAll(LIVE_PATH, "");
				if(actualPath.startsWith("/")){
					actualPath = actualPath.substring(1);
				}
			}else if(working){
				actualPath = actualPath.replaceAll(NONPUB_PATH, "");
				actualPath = actualPath.replaceAll(WORKING_PATH, "");
				if(actualPath.startsWith("/")){
					actualPath = actualPath.substring(1);
				}
			}else{
				return null;
			}
			
			if(!Config.getBooleanProperty("WEBDAV_LEGACY_PATHING", false)){
				actualPath = actualPath.substring(String.valueOf(dotDavHelper.getLanguage()).length()+1);
			}
			
			String[] splitPath = actualPath.split("/");
			
			
			User user=APILocator.getUserAPI().getSystemUser();
			
			
			// Handle root SYSTEM or Root Host view
			if(splitPath != null && splitPath.length == 1){
			    if(dotDavHelper.isTempResource(url)){ 
			        return null;
			    }
			    else {
    				host = hostAPI.findByName(splitPath[0], user, false);
    				if(splitPath[0].equalsIgnoreCase("system")){
    					SystemRootResourceImpl sys = new SystemRootResourceImpl();
    					return sys;
    				}else{
    					HostResourceImpl hr = new HostResourceImpl(url);
    					return hr;
    				}
			    }
			}
		
			
			// handle crappy dav clients temp files
			if(dotDavHelper.isTempResource(url)){
				java.io.File tempFile = dotDavHelper.loadTempFile(url);
				if(tempFile == null || !tempFile.exists()){
					return null;
				}else if(tempFile.isDirectory()){
						TempFolderResourceImpl tr = new TempFolderResourceImpl(url,tempFile,dotDavHelper.isAutoPub(url));
						return tr;
				}else{
					TempFileResourceImpl tr = new TempFileResourceImpl(tempFile,url,dotDavHelper.isAutoPub(url));
					return tr;
				}
			}
			
			
			// handle language files
			if(actualPath.endsWith("system/languages") || actualPath.endsWith("system/languages/") 
					|| actualPath.endsWith("system/languages/archived") || actualPath.endsWith("system/languages/archived/")){
		        java.io.File file = new java.io.File(FileUtil.getRealPath("/assets/messages"));
				if(file.exists() && file.isDirectory()){
					if(actualPath.contains("/archived") && actualPath.endsWith("/")){
						actualPath = actualPath.replace("system/languages/", "");
						if(actualPath.endsWith("/")){
							actualPath = actualPath.substring(0, actualPath.length()-1);
						}
						LanguageFolderResourceImpl lfr = new LanguageFolderResourceImpl(actualPath);
						return lfr;
					}else{
						LanguageFolderResourceImpl lfr = new LanguageFolderResourceImpl("");
						return lfr;
					}
				}
			}
			
			// handle the language files
			if(actualPath.endsWith("/")){
				actualPath = actualPath.substring(0, actualPath.length()-1);
			}
			if(actualPath.startsWith("system/languages") && (actualPath.endsWith(".properties") || actualPath.endsWith(".native"))){
				String fileRelPath = actualPath;
				if(actualPath.contains("system/languages/")){
					fileRelPath = actualPath.replace("system/languages/", "");
					if(fileRelPath.contains("archived")){
						java.io.File file = new java.io.File(FileUtil.getRealPath("/assets/messages") + java.io.File.separator + fileRelPath);
						//fileRelPath = fileRelPath.replace("archived/", "");
						if(fileRelPath.contains(".properties/")){
							LanguageFileResourceImpl lfr = new LanguageFileResourceImpl(fileRelPath);
							return lfr;
						}
						if(file.exists()){
							LanguageFolderResourceImpl lfr = new LanguageFolderResourceImpl(fileRelPath);
							return lfr;
						}
					}

				}
				java.io.File file = new java.io.File(FileUtil.getRealPath("/assets/messages") + java.io.File.separator + fileRelPath);
				if(file.exists()){
					LanguageFileResourceImpl lfr = new LanguageFileResourceImpl(fileRelPath);
					return lfr;
				}
			}
			
			if(dotDavHelper.isResource(url,user)){
				isResource = true;
			}
			
			if(dotDavHelper.isFolder(url,user)){
				isFolder = true;
			}
			if(!isFolder && !isResource){
				return null;
			}
			
			if(!isFolder && isResource){
				IFileAsset file = dotDavHelper.loadFile(url,user);
				if(file == null || !InodeUtils.isSet(file.getInode())){
					Logger.debug(this, "The file for url " + url + " returned null or not in db");
					return null;
				}
				FileResourceImpl fr = new FileResourceImpl(file,url);
				return fr;
			}else{
				Folder folder = dotDavHelper.loadFolder(url,user);
				if(folder == null || !InodeUtils.isSet(folder.getInode())){
					Logger.debug(this, "The folder for url " + url + " returned null or not in db");
					return null;
				}
				FolderResourceImpl fr = new FolderResourceImpl(folder, url);
				return fr;
			}
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.ResourceFactory#getSupportedLevels()
	 */
	public String getSupportedLevels() {
		return "1,2";
	}

    public void init(ApplicationConfig config, HttpManager manager) {
        manager.setEnableExpectContinue(false);
    }

    public void destroy(HttpManager manager) {
        // TODO Auto-generated method stub
        
    }

}
