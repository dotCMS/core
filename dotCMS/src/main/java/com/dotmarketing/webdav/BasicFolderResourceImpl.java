package com.dotmarketing.webdav;

import com.dotcms.repackage.com.bradmcevoy.http.Auth;
import com.dotcms.repackage.com.bradmcevoy.http.FolderResource;
import com.dotcms.repackage.com.bradmcevoy.http.HttpManager;
import com.dotcms.repackage.com.bradmcevoy.http.Range;
import com.dotcms.repackage.com.bradmcevoy.http.Resource;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.BadRequestException;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.NotFoundException;
import com.dotcms.repackage.org.dts.spell.utils.FileUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public abstract class BasicFolderResourceImpl implements FolderResource {
    
    protected String path;
    protected Host host;
    protected boolean isAutoPub;
    protected DotWebdavHelper dotDavHelper=new DotWebdavHelper();
    protected long lang = APILocator.getLanguageAPI().getDefaultLanguage().getId();

    private String originalPath;
    
    public BasicFolderResourceImpl(String path) {
        this.originalPath = path;
        this.path=path.toLowerCase();
        try {
            this.host=APILocator.getHostAPI().findByName(
                    dotDavHelper.getHostName(path),APILocator.getUserAPI().getSystemUser(),false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
			dotDavHelper.stripMapping(path);
		} catch (IOException e) {
			Logger.error( this, "Error happened with uri: [" + path + "]", e);
		}
        this.lang = dotDavHelper.getLanguage();
        this.isAutoPub=dotDavHelper.isAutoPub(path);
    }
    
    public Resource createNew(String newName, final InputStream in, final Long length, final String contentType) throws IOException, DotRuntimeException {

    	if(newName.matches("^\\.(.*)-Spotlight$")){
            // http://jira.dotmarketing.net/browse/DOTCMS-7285
    		newName = newName + ".spotlight";
    	}
   
        final User user = (User)HttpManager.request().getAuthorization().getTag();
        
        if(!this.path.endsWith("/")){
            this.path = this.path + "/";
        }

        newName = this.dotDavHelper.deleteSpecialCharacter(newName);

        if(!dotDavHelper.isTempResource(newName)) {

            try {

                this.dotDavHelper.setResourceContent(this.path + newName, in, contentType, null,
                        java.util.Calendar.getInstance().getTime(), user, this.isAutoPub);
                final IFileAsset iFileAsset = this.dotDavHelper.loadFile(this.path + newName, user);
                final Resource fileResource = new FileResourceImpl(iFileAsset, iFileAsset.getFileName());
                return fileResource;
            } catch (Exception e) {

            	Logger.warn(this, "An error occurred while creating new file: " +
                        (newName != null ? newName : "Unknown")
                		+ " in this path: " + (this.path != null ? this.path : "Unknown") + " "
                		+ e.getMessage(), e);
            	throw new DotRuntimeException(e.getMessage(), e);
            }
        } else {

            return this.createNewTemporalResource(newName, in);
        }
    } // createNew.

    private Resource createNewTemporalResource (final String newName, final InputStream in) {

        try {

            this.originalPath = (!this.originalPath.endsWith("/"))? this.originalPath + "/":this.originalPath;
            final File tempFile = this.dotDavHelper.createTempFile("/" + this.host.getHostname() + this.originalPath + newName);
            FileUtils.copyStreamToFile(tempFile, in, null);
            final Resource tempFileResource = new TempFileResourceImpl(tempFile, this.originalPath + newName, this.isAutoPub);
            return tempFileResource;
        } catch (Exception e){

            Logger.debug(this, "Error creating temp file", e);
            Logger.warn(this, "Error creating temp file", e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    
    public void delete() throws DotRuntimeException{
        User user=(User)HttpManager.request().getAuthorization().getTag();
        try {
            dotDavHelper.removeObject(path, user);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Long getMaxAgeSeconds(Auth arg0) {
        return new Long(60);
    }

    @Override
    public void sendContent(OutputStream arg0, Range arg1,
            Map<String, String> arg2, String arg3) throws IOException,
            NotAuthorizedException, BadRequestException, NotFoundException {
        return;
    }
    
    public String getPath() {
        return path;
    }
}
