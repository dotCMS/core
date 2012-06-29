package com.dotmarketing.portlets.workflows.ajax;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.web.VelocityWebUtil;

public class WfTaskAjax extends WfBaseAction {

	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	};

	/**
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	public void executeAction(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String wfContentletId = request.getParameter("wfContentletId");
		String wfActionAssign = request.getParameter("wfActionAssign");
		String wfActionComments = request.getParameter("wfActionComments");
		String wfActionId = request.getParameter("wfActionId");
		WorkflowAPI wapi = APILocator.getWorkflowAPI();

		Contentlet c = null;
		// execute workflow
		try {
			WorkflowAction action = wapi.findAction(wfActionId, getUser());
			if (action == null) {
				throw new ServletException("No such workflow action");
			}

			
			
			// if the worflow requires a checkin
			if(action.requiresCheckout()){
				c = APILocator.getContentletAPI().checkout(wfContentletId, getUser(), true);
				c.setStringProperty("wfActionId", action.getId());
				c.setStringProperty("wfActionComments", wfActionComments);
				c.setStringProperty("wfActionAssign", wfActionAssign);
				
				c = APILocator.getContentletAPI().checkin(c, getUser(), true);
			}
			
			// if the worflow requires a checkin
			else{
				c = APILocator.getContentletAPI().find(wfContentletId, getUser(), false);
				//c = APILocator.getContentletAPI().findContentletByIdentifier(wfContentletId,false,APILocator.getLanguageAPI().getDefaultLanguage().getId(), getUser(), true);
				c.setStringProperty("wfActionId", action.getId());
				c.setStringProperty("wfActionComments", wfActionComments);
				c.setStringProperty("wfActionAssign", wfActionAssign);
				
				
				wapi.fireWorkflowNoCheckin(c, getUser());
			}

		} catch (Exception e) {
			Logger.error(WfTaskAjax.class, e.getMessage(), e);
			writeError(response, e.getMessage()); 
			throw new ServletException(e.getMessage());
		}
		response.getWriter().println("SUCCESS:" + c.getInode());

	}

	
	
	
	
	
	
	public void executeActions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String wfActionAssign = request.getParameter("wfActionAssign");
		String wfActionComments = request.getParameter("wfActionComments");
		String wfActionId = request.getParameter("wfActionId");
		String wfCons = request.getParameter("wfCons");
		WorkflowAPI wapi = APILocator.getWorkflowAPI();
		WorkflowAction action = null;
		try{
			action = wapi.findAction(wfActionId, getUser());
			if (action == null) {
				throw new ServletException("No such workflow action");
			}
		}
		catch(Exception e){
			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
			return;
		}

		StringTokenizer st = new StringTokenizer(wfCons, ",");
		String x = null;
		while(st.hasMoreTokens()){
			try{
				 x = st.nextToken();
				if(!UtilMethods.isSet(x)){
					continue;
				}

				Identifier id = APILocator.getIdentifierAPI().find(x);

				
				Contentlet con = null;
				try{
					con = APILocator.getContentletAPI().findContentletByIdentifier(id.getId(), false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), getUser(), false);
				}
				catch(Exception e){
					
					
				}
				
				if(con == null || ! UtilMethods.isSet(con.getInode())){
					List<Language> langs = APILocator.getLanguageAPI().getLanguages();
					for(Language lang : langs){
						con = (Contentlet) APILocator.getContentletAPI().findContentletByIdentifier(id.getId(), false, lang.getId(), getUser(), false);
						if(con != null && UtilMethods.isSet(con.getInode())){
							break;
						}
					}
				}
				
				if(action.requiresCheckout()){

					Contentlet c = APILocator.getContentletAPI().checkout(con.getInode(), getUser(), true);
	
					c.setStringProperty("wfActionId", action.getId());
					c.setStringProperty("wfActionComments", wfActionComments);
					c.setStringProperty("wfActionAssign", wfActionAssign);
					
					c = APILocator.getContentletAPI().checkin(c, getUser(), true);
				}
				else{
					con.setStringProperty("wfActionId", action.getId());
					con.setStringProperty("wfActionComments", wfActionComments);
					con.setStringProperty("wfActionAssign", wfActionAssign);
					
					wapi.fireWorkflowNoCheckin(con, getUser());
					
				}
				
			}
			catch(Exception e){
				writeError(response, "cannot find execute task " + e.getMessage());
				Logger.warn(this.getClass(), "cannot find task " + x + " :" + e.getMessage(), e);
			}
		}


	}
	
	

	
	
	
	public void renderAction(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String actionId=request.getParameter("actionId");

		try {
			WorkflowAction action = APILocator.getWorkflowAPI().findAction(actionId, getUser());
			if(UtilMethods.isSet( action.getCondition())){
				
				Context ctx= VelocityWebUtil.getVelocityContext(request, response);
				Writer out = response.getWriter();
				
				VelocityUtil.getEngine().evaluate(ctx, out,"WorkflowVelocity:" + action.getName(), action.getCondition());
				return;
				
			}
			else{
				
				request.getRequestDispatcher("/html/portlet/ext/contentlet/contentlet_assign_comment.jsp").forward(request, response);
				
			}
		
		
		
		
		
		
		} catch (Exception e) {
			Logger.error(WfTaskAjax.class,e.getMessage(),e);
		} 
		
		
	}
	
	
	
	
	
	
	
	
	
}
