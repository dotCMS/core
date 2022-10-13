/**
 * 
 */
package com.dotmarketing.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.milton.http.Auth;
import io.milton.resource.CollectionResource;
import io.milton.resource.FolderResource;
import io.milton.http.LockInfo;
import io.milton.http.LockResult;
import io.milton.http.LockTimeout;
import io.milton.http.LockToken;
import io.milton.resource.LockingCollectionResource;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.resource.Resource;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.PreConditionFailedException;
import com.dotmarketing.business.Role;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * @author jasontesser
 *
 */
public class SystemRootResourceImpl implements FolderResource, LockingCollectionResource {

	private DotWebdavHelper dotDavHelper;
	
	
	public SystemRootResourceImpl() {
		dotDavHelper = new DotWebdavHelper();
	}
	
	/* (non-Javadoc)
	 * @see io.milton.http.MakeCollectionableResource#createCollection(java.lang.String)
	 */
	public CollectionResource createCollection(String newName) {
		return null;
	}

	/* (non-Javadoc)
	 * @see io.milton.resource.CollectionResource#child(java.lang.String)
	 */
	public Resource child(String childName) {
		return new LanguageFolderResourceImpl("");
	}

	/* (non-Javadoc)
	 * @see io.milton.resource.CollectionResource#getChildren()
	 */
	public List<? extends Resource> getChildren() {
		List<Resource> result = new ArrayList<Resource>();
		LanguageFolderResourceImpl lfr = new LanguageFolderResourceImpl("");
		result.add(lfr);
		return result;
	}

	/* (non-Javadoc)
	 * @see io.milton.resource.Resource#authenticate(java.lang.String, java.lang.String)
	 */
	public Object authenticate(String username, String password) {
		try {
			User user =  dotDavHelper.authorizePrincipal(username, password);
			//Get the Administrator Role to validate if the user has permission			
			Role cmsAdminRole = com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole();
			if(com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, cmsAdminRole.getId())){
				return user;
			}else{
				return null;
			}
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see io.milton.resource.Resource#authorise(io.milton.http.Request, io.milton.http.Request.Method, io.milton.http.Auth)
	 */
	public boolean authorise(Request req, Method method, Auth auth) {
		if(auth == null)
			return false;
		else{
			return true;
		}
	}

	/* (non-Javadoc)
	 * @see io.milton.resource.Resource#checkRedirect(io.milton.http.Request)
	 */
	public String checkRedirect(Request req) {
		return null;
	}

	/* (non-Javadoc)
	 * @see io.milton.resource.Resource#getContentLength()
	 */
	public Long getContentLength() {
		return (long)0;
	}

	/* (non-Javadoc)
	 * @see io.milton.resource.Resource#getContentType(java.lang.String)
	 */
	public String getContentType(String arg0) {
		return null;
	}

	/* (non-Javadoc)
	 * @see io.milton.resource.Resource#getModifiedDate()
	 */
	public Date getModifiedDate() {
		return new Date();
	}

	/* (non-Javadoc)
	 * @see io.milton.resource.Resource#getRealm()
	 */
	public String getRealm() {
		return null;
	}

	/* (non-Javadoc)
	 * @see io.milton.resource.Resource#getUniqueId()
	 */
	public String getUniqueId() {
		return "system".hashCode() + "";
	}

	/* (non-Javadoc)
	 * @see io.milton.http.PutableResource#createNew(java.lang.String, java.io.InputStream, java.lang.Long, java.lang.String)
	 */
	public Resource createNew(String newName, InputStream in, Long length, String contentType) throws IOException {
		return null;
	}

	/* (non-Javadoc)
	 * @see io.milton.http.CopyableResource#copyTo(io.milton.resource.CollectionResource, java.lang.String)
	 */
	public void copyTo(CollectionResource collRes, String name) {
		throw new RuntimeException("Not allowed to implement copy");
	}

	/* (non-Javadoc)
	 * @see io.milton.http.DeletableResource#delete()
	 */
	public void delete() {
		throw new RuntimeException("Cannot Delete System Folder");
	}

	/* (non-Javadoc)
	 * @see io.milton.http.GetableResource#getMaxAgeSeconds()
	 */
	public Long getMaxAgeSeconds() {
		return new Long(60);
	}

	/* (non-Javadoc)
	 * @see io.milton.http.GetableResource#sendContent(java.io.OutputStream, io.milton.http.Range, java.util.Map)
	 */
	public void sendContent(OutputStream arg0, Range arg1, Map<String, String> arg2, String arg3) throws IOException {
		return;
	}

	/* (non-Javadoc)
	 * @see io.milton.http.MoveableResource#moveTo(io.milton.resource.CollectionResource, java.lang.String)
	 */
	public void moveTo(CollectionResource collRes, String name) {
		throw new RuntimeException("Not allowed to implement move");
	}

	/* (non-Javadoc)
	 * @see io.milton.http.PropFindableResource#getCreateDate()
	 */
	public Date getCreateDate() {
		 return new Date();
	}

	public String getName() {
		return "system";
	}

	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	public LockResult lock(LockTimeout timeout, LockInfo lockInfo) {
		return dotDavHelper.lock(timeout, lockInfo, getUniqueId());
//		return dotDavHelper.lock(lockInfo, user, file.getIdentifier() + "");
	}

	public LockResult refreshLock(String token) {
		return dotDavHelper.refreshLock(getUniqueId());
//		return dotDavHelper.refreshLock(token);
	}

	public void unlock(String tokenId) {
		dotDavHelper.unlock(getUniqueId());
//		dotDavHelper.unlock(tokenId);
	}

	public LockToken getCurrentLock() {
		return dotDavHelper.getCurrentLock(getUniqueId());
	}

	public Long getMaxAgeSeconds(Auth arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public LockToken createAndLock(String name, LockTimeout timeout, LockInfo lockInfo)
			throws NotAuthorizedException {
		createCollection(name);
		return lock(timeout, lockInfo).getLockToken();
	}

    @Override
    public LockResult refreshLock(String token, LockTimeout timeout) throws NotAuthorizedException, PreConditionFailedException {
        // TODO Auto-generated method stub
        return null;
    }

}
