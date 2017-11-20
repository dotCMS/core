package com.dotmarketing.webdav2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.dotcms.repackage.com.bradmcevoy.http.Auth;
import com.dotcms.repackage.com.bradmcevoy.http.FolderResource;
import com.dotcms.repackage.com.bradmcevoy.http.HttpManager;
import com.dotcms.repackage.com.bradmcevoy.http.Range;
import com.dotcms.repackage.com.bradmcevoy.http.Resource;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.BadRequestException;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.NotFoundException;
import com.dotcms.repackage.org.dts.spell.utils.FileUtils;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public abstract class BasicFolderResourceImpl implements FolderResource {
    

    protected final DotWebDavObject davObject;

    
    private String originalPath;
    
    public BasicFolderResourceImpl(DotWebDavObject davObject) {
      this.davObject = davObject;
    }
    
    public Resource createNew(String newName, InputStream in, Long length, String contentType) throws IOException, DotRuntimeException {
    	if(newName.matches("^\\.(.*)-Spotlight$")){
            // http://jira.dotmarketing.net/browse/DOTCMS-7285
    		newName = newName + ".spotlight";
    	}

        User user=(User)HttpManager.request().getAuthorization().getTag();
        
        final String newPath = davObject.assetPath + "/" + newName;
        System.err.println("createNew:" + newPath);
        if(!davObject.temp){
            try {
            	davObject.setResourceContent(newPath, in, contentType, null, java.util.Calendar.getInstance().getTime(), user, davObject.live);
                final FileAsset iFileAsset = davObject.loadFile();
                final Resource fileResource = new FileResourceImpl(iFileAsset, iFileAsset.getFileName());
                return fileResource;
                
            }catch (Exception e){
            	Logger.error(this, "An error occurred while creating new file: " + (newName != null ? newName : "Unknown") 
                		+ " in this path: " + (davObject.assetPath != null ? davObject.assetPath : "Unknown") + " " 
                		+ e.getMessage(), e);
            	throw new DotRuntimeException(e.getMessage(), e);
            }
        } else {
            try {

               
  
                originalPath = (!originalPath.endsWith("/"))?originalPath + "/":originalPath;
                final File tempFile = davObject.createTempFile(newName);
                if(length==0){
                  tempFile.mkdirs();
                  return new TempFolderResourceImpl( tempFile, davObject);
                }
                else{
                  FileUtils.copyStreamToFile(tempFile, in, null);
                  return new TempFileResourceImpl(tempFile, davObject);
                }

            } catch (Exception e){
                Logger.error(this, "Error creating temp file", e);
                throw new DotRuntimeException(e.getMessage(), e);
            }
        }
    }

    
    public void delete() throws DotRuntimeException{
        User user=(User)HttpManager.request().getAuthorization().getTag();
        try {
            davObject.removeObject(davObject.assetPath.toString(), user);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    public String getPath(){
      return davObject.fullPath.toPath();
      
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
    

}
