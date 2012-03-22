package com.dotmarketing.scripting.viewtool;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.bsf.BSFException;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.tools.view.context.ViewContext;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.scripting.util.BSFUtil;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public abstract class AbstractScriptingTool implements ScriptingTool {

	protected HttpServletRequest request;
	protected Host host;
	protected Context ctx;
	private InternalContextAdapterImpl ica;
	protected User user = null;
	protected User backuser = null;
	protected boolean respectFrontendRoles = false;
	protected UserWebAPI userAPI;
	protected BSFUtil bsfUtil;
	protected boolean engineInited = false;
	protected List<Object> args = null;
	
	public void init(Object obj) {
		if(!Config.getBooleanProperty("ENABLE_SCRIPTING", false)){
			return;
		}
		ViewContext context = (ViewContext) obj;
		
		this.request = context.getRequest();
		ctx = context.getVelocityContext();		
		try {
			host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
		} catch (PortalException e1) {
			Logger.error(this,e1.getMessage(),e1);
		} catch (SystemException e1) {
			Logger.error(this,e1.getMessage(),e1);
		} catch (DotDataException e1) {
			Logger.error(this,e1.getMessage(),e1);
		} catch (DotSecurityException e1) {
			Logger.error(this,e1.getMessage(),e1);
		}
		userAPI = WebAPILocator.getUserWebAPI();
		try {
			user = userAPI.getLoggedInFrontendUser(request);
			backuser = userAPI.getLoggedInUser(request);
			respectFrontendRoles = true;
		} catch (Exception e) {
			Logger.error(this, "Error finding the logged in user", e);
		}
	}
	
	public void initEngine() {
		bsfUtil = BSFUtil.getInstance();
		engineInited = true;
		set("request", request);
		set("session", request.getSession());
		set("velcontext", ctx);
	}
	
	public void set(String key, Object val) {
		if(!engineInited){
			initEngine();
		}
		try {
			bsfUtil.declareBean(key, val);
		} catch (BSFException e) {
			Logger.error(this, e.getMessage(), e);
		}
	}

	/**
	 * Evaluates a file of code and returns the value of the last expression with the 
	 * exception of PHP which returns the wrapper object.
	 * You can do this instead of executing the file. Like the execFile the file
	 * should now be cached with the key being the filePath. 
	 * @param filePath - dotCMS file path
	 * @return The last call of the 
	 */
	protected Object evalFile(String filePath){
		if(!engineInited){
			initEngine();
		}
		try {
			return bsfUtil.evalFile(filePath, host);
		} catch (BSFException e) {
			Logger.error(this, e.getMessage(), e);
		} catch (Exception e) {
			Logger.error(this, "Problem reading the file : " + e.getMessage(), e);
		}
		return null;
	}
	
	protected boolean canUserEvalute() throws DotDataException, DotSecurityException{
		if(!Config.getBooleanProperty("ENABLE_SCRIPTING", false)){
			Logger.warn(this.getClass(), "Scripting called and ENABLE_SCRIPTING set to false");
			return false;
		}
		ica = new InternalContextAdapterImpl(ctx);
		String fieldResourceName = ica.getCurrentTemplateName();
		String conInode = fieldResourceName.substring(fieldResourceName.indexOf("/") + 1, fieldResourceName.indexOf("_"));
		Contentlet con = APILocator.getContentletAPI().find(conInode, APILocator.getUserAPI().getSystemUser(), true);
		User mu = userAPI.loadUserById(con.getModUser(), APILocator.getUserAPI().getSystemUser(), true);
		Role scripting =APILocator.getRoleAPI().loadRoleByKey("Scripting Developer");
		return APILocator.getRoleAPI().doesUserHaveRole(mu, scripting);
	}
	
}
