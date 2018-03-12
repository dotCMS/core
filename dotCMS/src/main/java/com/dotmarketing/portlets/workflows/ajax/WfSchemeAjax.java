package com.dotmarketing.portlets.workflows.ajax;

import com.dotcms.workflow.form.WorkflowSchemeForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class WfSchemeAjax extends WfBaseAction {

	private final UserWebAPI userWebAPI     = WebAPILocator.getUserWebAPI();


    public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    public void save(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        final String schemeName = request.getParameter("schemeName");
        final String schemeId = request.getParameter("schemeId");
        final String schemeDescription = request.getParameter("schemeDescription");
        final boolean schemeArchived = (request.getParameter("schemeArchived") != null);
        final WorkflowSchemeForm schemeForm = new WorkflowSchemeForm.Builder().schemeName(schemeName).schemeDescription(schemeDescription).schemeArchived(schemeArchived).build();

        try {
            final WorkflowHelper helper = WorkflowHelper.getInstance();
            helper.saveOrUpdate(schemeId, schemeForm, getUser());
            response.getWriter().println("SUCCESS");
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            writeError(response, e.getMessage());
        }

    }

}
