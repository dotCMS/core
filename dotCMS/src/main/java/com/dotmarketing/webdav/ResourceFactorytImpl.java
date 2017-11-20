/**
 * 
 */
package com.dotmarketing.webdav;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.repackage.com.bradmcevoy.common.Path;
import com.dotcms.repackage.com.bradmcevoy.http.ApplicationConfig;
import com.dotcms.repackage.com.bradmcevoy.http.HttpManager;
import com.dotcms.repackage.com.bradmcevoy.http.Initable;
import com.dotcms.repackage.com.bradmcevoy.http.Resource;
import com.dotcms.repackage.com.bradmcevoy.http.ResourceFactory;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * @author Jason Tesser
 *
 */
public class ResourceFactorytImpl implements ResourceFactory, Initable {


	private static final String AUTOPUB_PATH = "/webdav/autopub";
	private static final String NONPUB_PATH = "/webdav/nonpub";
	private static final String LIVE_PATH = "/webdav/live";
	private static final String WORKING_PATH = "/webdav/working";
	private HostAPI hostAPI = APILocator.getHostAPI();
	
	public ResourceFactorytImpl() {
		super();

	}
	
	
	
	
	
	
	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.ResourceFactory#getResource(java.lang.String, java.lang.String)
	 */
	@Override
    @WrapInTransaction
	public Resource getResource(final String davHost, final String url) {

        Path path = Path.path(url.toLowerCase());
        DotWebdavHelper dotDavHelper = new DotWebdavHelper();
        
    	Logger.debug(this, "WebDav ResourceFactory: Host is " + davHost + " and the url is " + url);

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
			
		
			
			String[] splitPath = actualPath.split("/");
			
			
			User user=APILocator.systemUser();
			
			
			
            // handle crappy dav clients temp files
            if(true){

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
            return null;
			
			
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
