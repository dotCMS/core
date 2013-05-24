package com.dotmarketing.viewtools;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
    private static HttpServletResponse response;
    Context ctx;
    private static User sysUser = null;

	public void init(Object initData) {
		ViewContext context = (ViewContext) initData;
		request = context.getRequest();
        response = context.getResponse();
        ctx = context.getVelocityContext();
        try {
			sysUser = APILocator.getUserAPI().getSystemUser();
		} catch (DotDataException e) {
			Logger.error(DotTemplateTool.class,e.getMessage(),e);
		}
	}

	public String getStructureCode(String containerIdentifier, String structureId) throws Exception {

		try {
			Container c;
			c = APILocator.getContainerAPI().getWorkingContainerById(containerIdentifier, sysUser, false);

			List<ContainerStructure> csList = APILocator.getContainerAPI().getContainerStructures(c);

			for (ContainerStructure cs : csList) {
				if(cs.getStructureId().equals(structureId)) {
					org.apache.velocity.context.Context context = VelocityUtil.getWebContext(request, response);
					VelocityUtil vu = new VelocityUtil();
					String parsedCode = vu.parseVelocity(cs.getCode(), context);
					return parsedCode;
				}
			}

		} catch (Exception e) {
			Logger.error(getClass(), e.getMessage());
			throw e;
		}

		return "";

	}

}
