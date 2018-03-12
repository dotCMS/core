package com.dotmarketing.portlets.workflows.ajax;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
import org.apache.commons.beanutils.BeanUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class WfSchemeAjax extends WfBaseAction {

	private final UserWebAPI userWebAPI     = WebAPILocator.getUserWebAPI();

	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{};

	public void save(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		WorkflowAPI wapi = APILocator.getWorkflowAPI();


		String schemeName = request.getParameter("schemeName");
		String schemeId = request.getParameter("schemeId");
		String schemeDescription = request.getParameter("schemeDescription");
		boolean schemeArchived = (request.getParameter("schemeArchived") != null);

		WorkflowScheme newScheme = new WorkflowScheme();

		try {

			WorkflowScheme origScheme = APILocator.getWorkflowAPI().findScheme(schemeId);
			BeanUtils.copyProperties(newScheme, origScheme);
		} catch (Exception e) {
			Logger.debug(this.getClass(), "Unable to find scheme" + schemeId);
		}

		newScheme.setArchived(schemeArchived);
		newScheme.setDescription(schemeDescription);
		newScheme.setName(schemeName);

		try {
			wapi.saveScheme(newScheme, this.userWebAPI.getUser(request));
			response.getWriter().println("SUCCESS");
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
		}



	}


}
