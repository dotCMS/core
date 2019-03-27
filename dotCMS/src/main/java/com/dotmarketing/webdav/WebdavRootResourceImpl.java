package com.dotmarketing.webdav;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.repackage.com.bradmcevoy.http.Auth;
import com.dotcms.repackage.com.bradmcevoy.http.CollectionResource;
import com.dotcms.repackage.com.bradmcevoy.http.GetableResource;
import com.dotcms.repackage.com.bradmcevoy.http.HttpManager;
import com.dotcms.repackage.com.bradmcevoy.http.LockInfo;
import com.dotcms.repackage.com.bradmcevoy.http.LockResult;
import com.dotcms.repackage.com.bradmcevoy.http.LockTimeout;
import com.dotcms.repackage.com.bradmcevoy.http.LockToken;
import com.dotcms.repackage.com.bradmcevoy.http.LockableResource;
import com.dotcms.repackage.com.bradmcevoy.http.PropFindableResource;
import com.dotcms.repackage.com.bradmcevoy.http.Range;
import com.dotcms.repackage.com.bradmcevoy.http.Request;
import com.dotcms.repackage.com.bradmcevoy.http.Request.Method;
import com.dotcms.repackage.com.bradmcevoy.http.Resource;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.BadRequestException;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.NotFoundException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class WebdavRootResourceImpl implements Resource, PropFindableResource, CollectionResource, LockableResource, GetableResource {

	private DotWebdavHelper dotDavHelper;
	private String path;
	
	public WebdavRootResourceImpl(String path) {
		dotDavHelper = new DotWebdavHelper();
		this.path=path;
	}
	
	public Object authenticate(String username, String password) {
		try {
			return dotDavHelper.authorizePrincipal(username, password);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
	}

	public boolean authorise(Request request, Method method, Auth auth) {
		if(auth == null)
			return false;
		else{
			return true;
		}
	}

	public String checkRedirect(Request request) {
		// TODO Auto-generated method stub
		return null;
	}

	public Long getContentLength() {
		// TODO Auto-generated method stub
		return new Long(0);
	}

	public String getContentType(String accepts) {
		// TODO Auto-generated method stub
		return null;
	}

	public Date getModifiedDate() {
		return new Date();
	}

	public String getName() {
		// TODO Auto-generated method stub
		return "davroot";
	}

	public String getRealm() {
		return null;
	}

	public String getUniqueId() {
		return "0";
	}

	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	public Resource child(String childName) {
		List<Host> hosts = listHosts();
		SystemRootResourceImpl sys = new SystemRootResourceImpl();
		if(childName.equalsIgnoreCase(sys.getName())){
			return sys;
		}
		for (Host host : hosts) {
			if(childName.equalsIgnoreCase(host.getHostname())){
			    String sep="/";
			    if(path.endsWith(sep)) sep="";
				HostResourceImpl hr = new HostResourceImpl(path + sep + host.getHostname());
				return hr;
			}
		}
		return null;
	}

	public List<? extends Resource> getChildren() {
	    User user=(User)HttpManager.request().getAuthorization().getTag();
		List<Host> hosts = listHosts();
		List<Resource> hrs = new ArrayList<Resource>();
		for (Host host : hosts) {
		    String sep="/";
            if(path.endsWith(sep)) sep="";
			HostResourceImpl hr = new HostResourceImpl(path + sep + host.getHostname());
			hr.setHost(host);
			hrs.add(hr);
		}
		try {		
			Role adminRole = com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole();
			if(com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,adminRole)){
				hrs.add(new SystemRootResourceImpl());
			}
		} catch (DotDataException e) {
			Logger.error(WebdavRootResourceImpl.class,e.getMessage(),e);
		}
		return hrs;
	}

	private List<Host> listHosts(){
	    User user=(User)HttpManager.request().getAuthorization().getTag();
		HostAPI hostAPI = APILocator.getHostAPI();
		List<Host> hosts;
		try {
			hosts = hostAPI.findAll(user, false);
			hosts.remove(APILocator.getHostAPI().findSystemHost());
		} catch (DotDataException e) {
			Logger.error(WebdavRootResourceImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(WebdavRootResourceImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return hosts;
	}

	public Date getCreateDate() {
		return new Date();
	}

	public Long getMaxAgeSeconds() {
		return new Long(60);
	}

	public LockToken getCurrentLock() {
		// TODO Auto-generated method stub
		return null;
	}

	public LockResult lock(LockTimeout arg0, LockInfo arg1)
			throws NotAuthorizedException {
		// TODO Auto-generated method stub
		return null;
	}

	public LockResult refreshLock(String arg0) throws NotAuthorizedException {
		// TODO Auto-generated method stub
		return null;
	}

	public void unlock(String arg0) throws NotAuthorizedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Long getMaxAgeSeconds(Auth arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendContent(OutputStream arg0, Range arg1,
			Map<String, String> arg2, String arg3) throws IOException,
			NotAuthorizedException, BadRequestException, NotFoundException {
		// TODO Auto-generated method stub
		
	}

}
