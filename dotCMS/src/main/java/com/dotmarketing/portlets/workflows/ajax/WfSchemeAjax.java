package com.dotmarketing.portlets.workflows.ajax;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.workflow.form.WorkflowSchemeForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Deprecated
public class WfSchemeAjax extends WfBaseAction {

    private final UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();

    public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}

    public void save(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        final String schemeName = request.getParameter("schemeName");
        final String schemeId = request.getParameter("schemeId");
        final String schemeDescription = request.getParameter("schemeDescription");

        final boolean schemeArchived =
                (request.getParameter("schemeArchived") != null) ? Boolean.parseBoolean(request.getParameter("schemeArchived"))
                        : false;
        final WorkflowSchemeForm schemeForm = new WorkflowSchemeForm.Builder().schemeName(schemeName)
            .schemeDescription(schemeDescription)
            .schemeArchived(schemeArchived)
            .build();

        try {
            final User user = this.userWebAPI.getUser(request);
            final String responseMessage = saveScheme(schemeId, schemeForm, user);
            response.getWriter().println(responseMessage);
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage());
            Logger.debug(this.getClass(), e.getMessage(), e);
            writeError(response, e.getMessage());
        }

    }

    @WrapInTransaction
    private String saveScheme(String schemeId, WorkflowSchemeForm schemeForm, User user)
            throws DotDataException, AlreadyExistException, DotSecurityException {

        final WorkflowHelper helper = WorkflowHelper.getInstance();
        helper.saveOrUpdate(schemeId, schemeForm, user);
        return "SUCCESS";
    }

}
