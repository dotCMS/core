package com.dotmarketing.webdav;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.FolderResource;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.exceptions.NotFoundException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public abstract class BasicFolderResourceImpl implements FolderResource {
    
    protected String path;
    protected Host host;
    protected boolean isAutoPub;
    protected DotWebdavHelper dotDavHelper=new DotWebdavHelper();
    
    public BasicFolderResourceImpl(String path) {
        this.path=path;
        try {
            this.host=APILocator.getHostAPI().findByName(
                    dotDavHelper.getHostName(path),APILocator.getUserAPI().getSystemUser(),false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.isAutoPub=dotDavHelper.isAutoPub(path);
    }
    
    public Resource createNew(String newName, InputStream in, Long length, String contentType) throws IOException, DotRuntimeException {
    	if(newName.matches("^\\.(.*)-Spotlight$")){
            // http://jira.dotmarketing.net/browse/DOTCMS-7285
            return null;
    	}
        
        User user=(User)HttpManager.request().getAuthorization().getTag();
        
        if(!path.endsWith("/")){
            path = path + "/";
        }
        if(!dotDavHelper.isTempResource(newName)){
            
            
            try {
                dotDavHelper.createResource(path + newName, isAutoPub, user);
            } catch (Exception e) {
                Logger.error(FolderResourceImpl.class,e.getMessage(),e);
                throw new DotRuntimeException(e.getMessage(), e);
            }
            IFileAsset f = null;
            try {
                dotDavHelper.setResourceContent(path + newName, in, contentType, null, java.util.Calendar.getInstance().getTime(), user, isAutoPub);
                f = dotDavHelper.loadFile(path + newName,user);
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
            }
            FileResourceImpl fr = new FileResourceImpl(f, f.getFileName());
            return fr;
        }
        
        String p = path;
        if(!p.endsWith("/")){
            p = p + "/";
        }
        File f = dotDavHelper.createTempFile("/" + host.getHostname() + p + newName);
        FileOutputStream fos = new FileOutputStream(f);
        byte[] buf = new byte[256];
        int read = -1;
        while ((read = in.read()) != -1) {
            fos.write(read);
        }
        TempFileResourceImpl tr = new TempFileResourceImpl(f, path + newName, isAutoPub);
        return tr;
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
