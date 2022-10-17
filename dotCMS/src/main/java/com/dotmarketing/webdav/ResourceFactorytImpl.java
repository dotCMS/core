/**
 * 
 */
package com.dotmarketing.webdav;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import io.milton.http.HttpManager;
import io.milton.http.ResourceFactory;
import io.milton.resource.Resource;
import io.milton.servlet.Initable;

/**
 * @author Jason Tesser
 *
 */
public class ResourceFactorytImpl implements ResourceFactory {

	private DotWebdavHelper dotDavHelper;
	static final String AUTOPUB_PATH = "/webdav/autopub";
	static final String NONPUB_PATH = "/webdav/nonpub";
	static final String LIVE_PATH = "/webdav/live";
	static final String WORKING_PATH = "/webdav/working";
	private HostAPI hostAPI = APILocator.getHostAPI();
	
	public ResourceFactorytImpl() {
		super();
		dotDavHelper = new DotWebdavHelper();
	}
	
	/* (non-Javadoc)
	 * @see io.milton.http.ResourceFactory#getResource(java.lang.String, java.lang.String)
	 */
    @CloseDBIfOpened
	public Resource getResource(String davHost, String url) {
		return getResource(davHost, url, dotDavHelper, hostAPI);
	}

    @VisibleForTesting
	Resource getResource(final String davHost, String url, final DotWebdavHelper dotDavHelper, final HostAPI hostAPI) {
		
        final DavParams davParams = new DavParams(url);
	    


		// DAV ROOT
		if(davParams.isRoot()){
		    return new WebdavRootResourceImpl(url);
		}
		
        if(davParams.isSystem()){
            return new SystemRootResourceImpl();
        }
        
        if(davParams.isHost()) {
            return new HostResourceImpl(url);
        }




		// handle crappy dav clients temp files
		if(davParams.isTempFile()){
			java.io.File tempFile = dotDavHelper.loadTempFile(davParams);
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

		/*
		if(davParams.isLanguages()){
		        String actualPath = davParams.path;
				java.io.File file = new java.io.File(FileUtil.getRealPath("/assets/messages"));
				LanguageFolderResourceImpl lfr = new LanguageFolderResourceImpl(davParams.path);
				return lfr;

				
			}

			if(davParams.name.endsWith(".properties") ){
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
		 */
	      // DAV ROOT
		
		User user=APILocator.getUserAPI().getSystemUser();
		
		
        if(davParams.isFile()){
            IFileAsset file = dotDavHelper.loadFile(davParams,user);
            
            
            return new FileResourceImpl(file,davParams);
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
					Logger.debug(ResourceFactorytImpl.class, "The file for url " + url + " returned null or not in db");
					return null;
				}
				FileResourceImpl fr = new FileResourceImpl(file,url);
				return fr;
			}else{
				Folder folder = dotDavHelper.loadFolder(url,user);
				if(folder == null || !InodeUtils.isSet(folder.getInode())){
					Logger.debug(ResourceFactorytImpl.class, "The folder for url " + url + " returned null or not in db");
					return null;
				}
				FolderResourceImpl fr = new FolderResourceImpl(folder, url);
				return fr;
			}
		} catch (Exception e) {
			Logger.error(ResourceFactorytImpl.class, e.getMessage(), e);
			return null;
		}
	}






}
