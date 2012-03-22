package com.dotmarketing.webdav;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FileItem;
import com.bradmcevoy.http.FileResource;
import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockResult;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.LockableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * 
 * @author Jason Tesser
 */
public class TemplateFileResourceImpl implements FileResource, LockableResource {

	private DotWebdavHelper dotDavHelper;
	private final Template template;
	private User user;
	private TemplateAPI tapi;
	private Host host;

	public TemplateFileResourceImpl(Template t, Host h) {
		this.template = t;
		tapi = APILocator.getTemplateAPI();
		this.host = h;
	}

	public String getUniqueId() {
		return template.getTitle().hashCode() + "";
	}

	public int compareTo(Object o) {
		if (o instanceof Resource) {
			Resource res = (Resource) o;
			return this.getUniqueId().compareTo(res.getUniqueId());
		} else {
			return -1;
		}
	}

	public void sendContent(OutputStream out, Range range, Map<String, String> params, String arg3) throws IOException {
		if (template != null)
			out.write(template.getBody().getBytes());
	}

	public String getName() {
		String x = template.getTitle();
		try {
			x = URLEncoder.encode(x, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Logger.error(this, e.getMessage(), e);
		}
		x = x + " | " + template.getIdentifier(); 
		return x;
	}

	public Object authenticate(String username, String password) {
		try {
			this.user = dotDavHelper.authorizePrincipal(username, password);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
		return this.user;
	}

	public boolean authorise(Request request, Request.Method method, Auth auth) {
		try {
			if (auth == null)
				return false;
			else if (method.isWrite) {
				return APILocator.getPermissionAPI().doesUserHavePermission(host,
						PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, false);
			} else if (!method.isWrite) {
				return APILocator.getPermissionAPI().doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user,
						false);
			}
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);

		}
		return false;
	}

	public String getRealm() {
		return null;
	}

	public Date getModifiedDate() {
		Date dt = template.getModDate();
		// log.debug("static resource modified: " + dt);
		return dt;
	}

	public Long getContentLength() {
		return (long) template.getBody().length();
	}

	public String getContentType(String accepts) {

		return "text/plain";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.bradmcevoy.http.PutableResource#createNew(java.lang.String,
	 * java.io.InputStream, java.lang.Long, java.lang.String)
	 */
	public Resource createNew(String newName, InputStream in, Long length, String contentType) throws IOException,
			DotRuntimeException {

		StringWriter sw = new StringWriter();

		int read = -1;
		while ((read = in.read()) != -1) {
			sw.write(read);
		}
		Template t = new Template();
		t.setTitle(newName);
		t.setiDate(new Date());
		t.setModDate(new Date());
		t.setBody(sw.toString());
		try {
			tapi.saveTemplate(t, host, user, false);
		} catch (DotDataException e) {
			Logger.error(this.getClass(), "Cannot save template " + t.getTitle() + " on host " + host.getHostname()
					+ " " + e.getMessage());

		} catch (DotSecurityException e) {
			Logger.error(this.getClass(), "Cannot save template " + t.getTitle() + " on host " + host.getHostname()
					+ " " + e.getMessage());
		}

		TemplateFileResourceImpl tfri = new TemplateFileResourceImpl(t, host);
		return tfri;
	}

	public String checkRedirect(Request request) {
		return null;
	}

	public Long getMaxAgeSeconds() {
		return (long) 60;
	}

	public void copyTo(CollectionResource collRes, String name) {
		if (collRes instanceof TemplateFolderResourceImpl) {

		}
		throw new RuntimeException("Not allowed to implement copy");
	}

	public void delete() {

		try {
			tapi.delete(template, user, false);

		} catch (Exception e) {
			Logger.error(
					this.getClass(),
					"Cannot delete template " + template.getTitle() + " on host " + host.getHostname() + " "
							+ e.getMessage());
		}

	}

	public void moveTo(CollectionResource collRes, String name) {
		throw new RuntimeException("Not allowed to implement move");
	}

	public String processForm(Map<String, String> parameters, Map<String, FileItem> files) {
		// TODO Auto-generated method stub
		return null;
	}

	public Date getCreateDate() {
		// TODO Auto-generated method stub
		return template.getiDate();
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

}
