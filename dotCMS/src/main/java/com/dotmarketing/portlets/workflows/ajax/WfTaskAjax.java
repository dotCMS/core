package com.dotmarketing.portlets.workflows.ajax;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Deprecated
public class WfTaskAjax extends WfBaseAction {

	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	};

	/**
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	public void executeAction(final HttpServletRequest request,
							  final HttpServletResponse response) throws ServletException, IOException {

		final String wfContentletId   = request.getParameter("wfContentletId");
		final String wfActionAssign   = request.getParameter("wfActionAssign");
		final String wfActionComments = request.getParameter("wfActionComments");
		final String wfActionId       = request.getParameter("wfActionId");
		final String wfPublishDate    = request.getParameter("wfPublishDate");
		final String wfPublishTime    = request.getParameter("wfPublishTime");
		final String wfExpireDate     = request.getParameter("wfExpireDate");
		final String wfExpireTime     = request.getParameter("wfExpireTime");
		final String wfNeverExpire    = request.getParameter("wfNeverExpire");
		final String whereToSend      = request.getParameter("whereToSend");
		final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();

		Contentlet contentlet = null;
		// execute workflow
		try {

			final WorkflowAction action = workflowAPI.findAction(wfActionId, getUser());
			if (action == null) {

				throw new ServletException("No such workflow action");
			}

			contentlet = APILocator.getContentletAPI().find(wfContentletId, getUser(), false);
			contentlet.setStringProperty("wfActionId", action.getId());
			contentlet.setStringProperty("wfActionComments", wfActionComments);
			contentlet.setStringProperty("wfActionAssign", wfActionAssign);
			contentlet.setStringProperty("wfPublishDate", wfPublishDate);
			contentlet.setStringProperty("wfPublishTime", wfPublishTime);
			contentlet.setStringProperty("wfExpireDate", wfExpireDate);
			contentlet.setStringProperty("wfExpireTime", wfExpireTime);
			contentlet.setStringProperty("wfNeverExpire", wfNeverExpire);
			contentlet.setStringProperty("whereToSend", whereToSend);
			APILocator.getWorkflowAPI().fireContentWorkflow(contentlet,
					new ContentletDependencies.Builder()
							.respectAnonymousPermissions(PageMode.get(request).respectAnonPerms)
							.modUser(getUser()).build());
		} catch (Exception e) {

			Logger.error(WfTaskAjax.class, e.getMessage(), e);
			writeError(response, e.getMessage()); 
			throw new ServletException(e.getMessage(),e);
		}

		response.getWriter().println("SUCCESS:" + contentlet.getInode());
	}

	
	
	
	
	
	
	public void executeActions(final HttpServletRequest request,
							   final HttpServletResponse response) throws ServletException, IOException {

		final String wfActionAssign   = request.getParameter(Contentlet.WORKFLOW_ASSIGN_KEY);
		final String wfActionComments = request.getParameter(Contentlet.WORKFLOW_COMMENTS_KEY);
		final String wfActionId 	  = request.getParameter(Contentlet.WORKFLOW_ACTION_KEY);
		final String wfCons 		  = request.getParameter("wfCons");
		final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
		WorkflowAction action = null;

		try {
			action = workflowAPI.findAction(wfActionId, getUser());
			if (action == null) {
				throw new ServletException("No such workflow action");
			}
		} catch(Exception e) {

			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
			return;
		}

		final StringTokenizer stringTokenizer =
				new StringTokenizer(wfCons, ",");
		String token = null;

		while(stringTokenizer.hasMoreTokens()) {

			try {

				token = stringTokenizer.nextToken();
				if (!UtilMethods.isSet(token)){
					continue;
				}

				final Identifier id = APILocator.getIdentifierAPI().findFromInode(token);
				Contentlet contentlet = null;

				try {
					contentlet = APILocator.getContentletAPI().findContentletByIdentifier
							(id.getId(), false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), getUser(), false);
				} catch(Exception e) { /* Quiet */}
				
				if (contentlet == null || ! UtilMethods.isSet(contentlet.getInode())) {

					final List<Language> languages = APILocator.getLanguageAPI().getLanguages();
					for(final Language language : languages) {

						contentlet = APILocator.getContentletAPI().findContentletByIdentifier
								(id.getId(), false, language.getId(), getUser(), false);
						if(contentlet != null && UtilMethods.isSet(contentlet.getInode())){
							break;
						}
					}
				}

				if (null != contentlet) {
					APILocator.getWorkflowAPI().fireContentWorkflow(contentlet,
							new ContentletDependencies.Builder()
									.respectAnonymousPermissions(false)
									.modUser(APILocator.getUserAPI().getSystemUser())
									.workflowActionId(action.getId())
									.workflowActionComments(wfActionComments)
									.workflowAssignKey(wfActionAssign).build());
				}
			} catch(Exception e) {

				writeError(response, "cannot find execute task " + e.getMessage());
				Logger.warn(this.getClass(), "cannot find task " + token + " :" + e.getMessage(), e);
			}
		}
	} // executeActions.

	
	
}
