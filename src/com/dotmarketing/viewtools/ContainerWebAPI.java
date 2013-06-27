package com.dotmarketing.viewtools;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

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

	public void init(Object initData) {
		viewContext = (ViewContext) initData;
		request = viewContext.getRequest();
        ctx = viewContext.getVelocityContext();
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
					if(request.getSession()==null) {
					}
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

}
