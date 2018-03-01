package com.dotcms.rendering.velocity.viewtools;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.InodeUtils;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;

public class ContainerWebAPI implements ViewTool {

	private static HttpServletRequest request;
    private Context ctx;
    private ViewContext viewContext;
	private User backuser;
	private PermissionAPI permissionAPI;

	public void init(Object initData) {
		viewContext = (ViewContext) initData;
		request = viewContext.getRequest();
        ctx = viewContext.getVelocityContext();

		final UserWebAPI userAPI = WebAPILocator.getUserWebAPI();
		permissionAPI = APILocator.getPermissionAPI();

		try {
			backuser = userAPI.getLoggedInUser(request.getSession());
		} catch (Exception e) {
			Logger.error(this, "Error finding the logged in user", e);
		}
	}

	public String getStructureCode(String containerIdentifier, String structureId) throws Exception {

		try {
			Container c = null;
			User sysUser = null;
			try {
				sysUser = APILocator.getUserAPI().getSystemUser();
			} catch (DotDataException e) {
				Logger.error(DotTemplateTool.class,e.getMessage(),e);
			}
			c = APILocator.getContainerAPI().getWorkingContainerById(containerIdentifier, sysUser, false);

			List<ContainerStructure> csList = APILocator.getContainerAPI().getContainerStructures(c);

			for (ContainerStructure cs : csList) {
				if(cs.getStructureId().equals(structureId)) {

					VelocityUtil vu = new VelocityUtil();
					String parsedCode = vu.parseVelocity(cs.getCode(), ctx);
					return parsedCode;
				}
			}

		} catch (Exception e) {
			Logger.error(getClass(), e.getMessage(), e);
			throw e;
		}

		return "";

	}

	/**
	 * This method checks if the logged in user (frontend) has the required permission over
	 * the passed container id
	 */
	public boolean doesUserHasPermission (String containerInode, int permission, boolean respectFrontendRoles) throws DotDataException {
		try {
			if(!InodeUtils.isSet(containerInode)) {
				return false;
			} else {
				final User systemUser = APILocator.getUserAPI().getSystemUser();
				final Container container = APILocator.getContainerAPI().find(containerInode, systemUser, respectFrontendRoles);
				return permissionAPI.doesUserHavePermission(container, permission, backuser, respectFrontendRoles);
			}
		} catch (DotSecurityException e) {
			return false;
		}
	}
}
