package com.dotmarketing.scripting.viewtool;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;


import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class JSPTool implements ViewTool {

	private HttpServletRequest request;
	private static int isWarned = 0;
	private HttpServletResponse response;
	private Context ctx;
	private InternalContextAdapterImpl ica;
	private UserWebAPI userAPI;

	public void init(Object obj) {
		if (!Config.getBooleanProperty("ENABLE_SCRIPTING", false)) {
			return;
		}

		ViewContext context = (ViewContext) obj;

		this.request = context.getRequest();
		ctx = context.getVelocityContext();
		this.request = context.getRequest();
		this.response = context.getResponse();
	}

	/**
	 * The include method takes a path to a jsp that lives on the actual file system and parses it for inclusion in the
	 * velocity template. The user trying to call this method in content or widget must have "Scripting Developer" 
	 * role in the CMS AND ENABLE_SCRIPTING must be set to true in dotmarketing-config.properties.
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public String include(String path) throws Exception {

		if (!canUserEvalute()) {
			Logger.error(this, "User Has No Permission To Evalute Code");
			throw new Exception("User Has No Permission To Evalute Code");

		}

		HttpServletResponseWrapper res = new JSPVelocityWrapper(response);

		try {
			request.getRequestDispatcher(path).include(request, res);
		} catch (Exception e) {
			Logger.error(this.getClass(), e.toString());
			if (request.getSession().getAttribute("EDIT_MODE") != null) {
				return e.getMessage();
			}

		}
		return ((JSPVelocityWrapper) res).getResponseString();

	}

	private class JSPVelocityWrapper extends HttpServletResponseWrapper {

		private StringWriter writer = new StringWriter();

		public JSPVelocityWrapper(HttpServletResponse response) {
			super(response);

		}

		public String getResponseString() {
			return writer.toString();
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			// TODO Auto-generated method stub
			return super.getOutputStream();
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			PrintWriter pw = new PrintWriter(writer);
			return pw;
		}

	}

	protected boolean canUserEvalute() throws DotDataException, DotSecurityException {
		if (!Config.getBooleanProperty("ENABLE_SCRIPTING", false)) {
			Logger.warn(this.getClass(), "Scripting called and ENABLE_SCRIPTING set to false");
			return false;
		}
		ica = new InternalContextAdapterImpl(ctx);
		String fieldResourceName = ica.getCurrentTemplateName();
		String inode = null;
		String userId = null;
		if (fieldResourceName.indexOf("field") > -1) {
			inode = fieldResourceName.substring(fieldResourceName.lastIndexOf("/") + 1, fieldResourceName.indexOf("_"));
			Contentlet con = APILocator.getContentletAPI().find(inode, APILocator.getUserAPI().getSystemUser(), true);
			userId = con.getModUser();
		} else if (fieldResourceName.indexOf("template") > -1) {
			inode = fieldResourceName.substring(fieldResourceName.lastIndexOf("/") + 1, fieldResourceName.indexOf("."));
			Template t = APILocator.getTemplateAPI().findWorkingTemplate(inode,
					APILocator.getUserAPI().getSystemUser(), true);
			userId = t.getModUser();
			if (isWarned < 5) {
				Logger.warn(this.getClass(), "calling $jsp.include from a template results in a db hit");
				isWarned++;
			}

		} else if (fieldResourceName.indexOf("container") > -1) {
			inode = fieldResourceName.substring(fieldResourceName.lastIndexOf("/") + 1, fieldResourceName.indexOf("."));
			Container c = APILocator.getContainerAPI().getWorkingContainerById(inode,
					APILocator.getUserAPI().getSystemUser(), true);
			userId = c.getModUser();
			if (isWarned < 5) {
				Logger.warn(this.getClass(), "calling $jsp.include from a container results in a db hit");
				isWarned++;
			}
		}
		if(userId ==null){
			return false;
		}
		User mu = APILocator.getUserAPI().loadUserById(userId, APILocator.getUserAPI().getSystemUser(), true);
		Role scripting = APILocator.getRoleAPI().loadRoleByKey("Scripting Developer");
		return APILocator.getRoleAPI().doesUserHaveRole(mu, scripting);
	}

}