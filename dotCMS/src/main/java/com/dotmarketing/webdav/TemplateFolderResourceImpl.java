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
import io.milton.resource.LockableResource;
import io.milton.resource.*;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.resource.Resource;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.PreConditionFailedException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * @author Jason Tesser
 * 
 */
public class TemplateFolderResourceImpl implements LockableResource,
		LockingCollectionResource, FolderResource, MakeCollectionableResource {

	public static final String TEMPLATE_FOLDER="_TEMPLATE";
	private DotWebdavHelper dotDavHelper;
	private String path;

	private PermissionAPI perAPI;
	private Host host;
	private TemplateAPI tapi;

	public TemplateFolderResourceImpl(String path, Host host) {
		this.perAPI = APILocator.getPermissionAPI();
		this.dotDavHelper = new DotWebdavHelper();
		this.path = path ;
		this.host = host;
		this.tapi = APILocator.getTemplateAPI();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.milton.http.MakeCollectionableResource#createCollection(java.
	 * lang.String)
	 */
	public CollectionResource createCollection(String newName)
			throws DotRuntimeException {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.milton.resource.CollectionResource#child(java.lang.String)
	 */
	public Resource child(String childName) {
		if (!UtilMethods.isSet(childName)) {
			return null;
		}
		List<Resource> children = getChildren();

		for (Resource resource : children) {
			TemplateFolderResourceImpl tfr = (TemplateFolderResourceImpl) resource;
			if (childName.equalsIgnoreCase(tfr.getName())) {
				return resource;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.milton.resource.Resource#authenticate(java.lang.String,
	 * java.lang.String)
	 */
	public Object authenticate(String username, String password) {
		try {
			return dotDavHelper.authorizePrincipal(username, password);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.milton.resource.Resource#authorise(io.milton.http.Request,
	 * io.milton.http.Request.Method, io.milton.http.Auth)
	 */
	public boolean authorise(Request req, Method method, Auth auth) {
		try {

			if (auth == null)
				return false;
			else { 
			    User user=(User)auth.getTag();
			    if (method.isWrite) {
    				return perAPI.doesUserHavePermission(host,
    						PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, false);
    			} else if (!method.isWrite) {
    				return perAPI.doesUserHavePermission(host,
    						PermissionAPI.PERMISSION_READ, user, false);
    			}
			}
		} catch (DotDataException e) {
			Logger.error(TemplateFolderResourceImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.milton.resource.Resource#checkRedirect(io.milton.http.Request)
	 */
	public String checkRedirect(Request req) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.milton.resource.Resource#getContentLength()
	 */
	public Long getContentLength() {
		return (long) 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.milton.resource.Resource#getContentType(java.lang.String)
	 */
	public String getContentType(String arg0) {
		return "folder";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.milton.resource.Resource#getModifiedDate()
	 */
	public Date getModifiedDate() {
		return new Date();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.milton.resource.Resource#getRealm()
	 */
	public String getRealm() {
		return CompanyUtils.getDefaultCompany().getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.milton.resource.Resource#getUniqueId()
	 */
	public String getUniqueId() {
		return path;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.milton.http.PutableResource#createNew(java.lang.String,
	 * java.io.InputStream, java.lang.Long, java.lang.String)
	 */
	public Resource createNew(String newName, InputStream in, Long length,
			String contentType) throws IOException, DotRuntimeException {

		throw new DotRuntimeException("Cannot create new template folder");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.milton.http.CopyableResource#copyTo(io.milton.http.
	 * CollectionResource, java.lang.String)
	 */
	public void copyTo(CollectionResource collRes, String name) {
		throw new DotRuntimeException("Cannot copy template folder");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.milton.http.DeletableResource#delete()
	 */
	public void delete() throws DotRuntimeException {
		throw new DotRuntimeException("Cannot delete template folder");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.milton.http.GetableResource#getMaxAgeSeconds()
	 */
	public Long getMaxAgeSeconds() {
		return new Long(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.milton.http.GetableResource#sendContent(java.io.OutputStream,
	 * io.milton.http.Range, java.util.Map)
	 */
	public void sendContent(OutputStream arg0, Range arg1,
			Map<String, String> arg2, String arg3) throws IOException {
		return;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.milton.http.MoveableResource#moveTo(io.milton.http.
	 * CollectionResource, java.lang.String)
	 */
	public void moveTo(CollectionResource collRes, String name)
			throws DotRuntimeException {
		throw new DotRuntimeException("Cannot move template folder");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.milton.http.PropFindableResource#getCreateDate()
	 */
	public Date getCreateDate() {
		return host.getModDate();
	}

	public String getName() {
		return TEMPLATE_FOLDER;
	}

	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public LockResult lock(LockTimeout timeout, LockInfo lockInfo) {
		return dotDavHelper.lock(timeout, lockInfo, getUniqueId());
		// return dotDavHelper.lock(lockInfo, user, file.getIdentifier() + "");
	}

	public LockResult refreshLock(String token) {
		return dotDavHelper.refreshLock(getUniqueId());
		// return dotDavHelper.refreshLock(token);
	}

	public void unlock(String tokenId) {
		dotDavHelper.unlock(getUniqueId());
		// dotDavHelper.unlock(tokenId);
	}

	public LockToken getCurrentLock() {
		return dotDavHelper.getCurrentLock(getUniqueId());
	}

	public Long getMaxAgeSeconds(Auth arg0) {
		return (long) 60;
	}

	public LockToken createAndLock(String name, LockTimeout timeout, LockInfo lockInfo)
			throws NotAuthorizedException {
		createCollection(name);
		return lock(timeout, lockInfo).getLockToken();
	}

	public List<Resource> getChildren() {
		/*
		List<Template> templates;
		try {
			templates = tapi.findTemplatesUserCanUse(user, host.getHostname(),
					true, 0, 100);

			List<Resource> tempRes = new ArrayList<Resource>();
			for (Template t : templates) {
				TemplateFileResourceImpl tfr = new TemplateFileResourceImpl(t, host);
				tempRes.add(tfr);

			}
			return tempRes;
		} catch (DotDataException e) {
			Logger.error(this.getClass(), "Cannot list templates on host "
					+ host.getHostname() + " " + e.getMessage());

		} catch (DotSecurityException e) {
			Logger.error(this.getClass(), "Cannot list templates on host "
					+ host.getHostname() + " " + e.getMessage());

		}
		*/
		return new ArrayList();
	}

    @Override
    public LockResult refreshLock(String token, LockTimeout timeout) throws NotAuthorizedException, PreConditionFailedException {
        // TODO Auto-generated method stub
        return null;
    }
}
