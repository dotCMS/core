package com.dotmarketing.portlets.workflows.ajax;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class WfSchemeAjax extends WfBaseAction {
	 public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{};

	public void save(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		WorkflowAPI wapi = APILocator.getWorkflowAPI();


		String schemeName = request.getParameter("schemeName");
		String schemeId = request.getParameter("schemeId");
		String schemeDescription = request.getParameter("schemeDescription");
		boolean schemeArchived = (request.getParameter("schemeArchived") != null);
		boolean schemeMandatory = (request.getParameter("schemeMandatory") != null);
		String schemeEntryAction = request.getParameter("schemeEntryAction");
		if(!UtilMethods.isSet(schemeEntryAction)){
			schemeEntryAction=null;
		}
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
		newScheme.setMandatory(schemeMandatory);
		newScheme.setEntryActionId(schemeEntryAction);

		try {
			wapi.saveScheme(newScheme);
			response.getWriter().println("SUCCESS");
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
		}



	}


}
