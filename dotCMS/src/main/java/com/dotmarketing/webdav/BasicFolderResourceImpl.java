package com.dotmarketing.webdav;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import com.dotcms.util.CloseUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.milton.http.Auth;
import io.milton.http.HttpManager;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.FolderResource;
import io.milton.resource.Resource;

public abstract class BasicFolderResourceImpl implements FolderResource {
    
    protected final String path;
    protected final Host host;
    protected final boolean isAutoPub;
    protected final DotWebdavHelper dotDavHelper=new DotWebdavHelper();
    protected final long lang ;
    private final String originalPath;
    
    public BasicFolderResourceImpl(String path) {
        this.originalPath = path;
        this.path= ("/" + path + "/").replaceAll("//", "/");

        
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
        


        newName = this.dotDavHelper.deleteSpecialCharacter(newName);

        try {
        
            final File tempFile = dotDavHelper.getOrCreateTempFile(path , newName);
            IOUtils.copy(in, Files.newOutputStream(tempFile.toPath()));
            
            if(tempFile.length()==0 && length>0) {
                throw new DotRuntimeException("File:" + newName + " has length of 0");
            }
            
            
            if(dotDavHelper.isTempResource(newName)) {
                return this.createNewTemporalResource(tempFile);
            }
            

        


            this.dotDavHelper.setResourceContent(this.path + newName, tempFile, user, this.isAutoPub);
            final IFileAsset iFileAsset = this.dotDavHelper.loadFile(this.path + newName, user);
            final Resource fileResource = new FileResourceImpl(iFileAsset, iFileAsset.getFileName());
            return fileResource;
        } catch (Exception e) {

        	Logger.warn(this, "An error occurred while creating new file: " +
                    (newName != null ? newName : "Unknown")
            		+ " in this path: " + (this.path != null ? this.path : "Unknown") + " "
            		+ e.getMessage(), e);
        	throw new DotRuntimeException(e.getMessage(), e);
        }finally {
            CloseUtils.closeQuietly(in);
        }

    } // createNew.

    private Resource createNewTemporalResource(final File tempFile) {

        try {


            return new TempFileResourceImpl(tempFile, this.originalPath + tempFile.getName(), this.isAutoPub);
            
        } catch (Exception e) {

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
