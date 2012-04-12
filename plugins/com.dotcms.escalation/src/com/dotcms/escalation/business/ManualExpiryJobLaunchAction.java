package com.dotcms.escalation.business;


import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import com.dotcms.escalation.util.EscalationJob;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Logger;

public class ManualExpiryJobLaunchAction extends DotPortletAction {

	
	private String pluginId = "com.dotcms.escalation";
	private PluginAPI pluginAPI = APILocator.getPluginAPI();
	private WorkflowAPI wAPI = APILocator.getWorkflowAPI();
	private ExpiryTaskAPI expAPI = ExpiryTaskAPI.getInstance();

	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest req, HttpServletResponse res) throws Exception {
		System.out.println("EXECUTE ESCALATION ACTION");
		return super.execute(mapping, form, req, res);
	}

	@Override
	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res) throws Exception {

		
		String roleKeyToEscale = pluginAPI.loadProperty(pluginId, "escalation.job.java.roleToEscale");
		String[] taskToEscale = (String[]) req.getParameterValues("task");
		//String selectedRole = ((EscalationForm)form).getUser();

		for (int i = 0; i < taskToEscale.length; i++) {

			String taskWebAsset = APILocator.getWorkflowAPI().findTaskById(taskToEscale[i]).getWebasset();
			
			WorkflowHistory whistory;
			try {
				whistory = (WorkflowHistory) wAPI.retrieveLastStepAction(taskToEscale[i]);
				
				Contentlet c = APILocator.getContentletAPI().findContentletByIdentifier(taskWebAsset,false,APILocator.getLanguageAPI().getDefaultLanguage().getId(), APILocator.getUserAPI().getSystemUser(), true);
				c.setStringProperty(Contentlet.WORKFLOW_ACTION_KEY, whistory.getActionId());
				//WorkflowProcessor workflow = wAPI.fireWorkflowPreCheckin(c);
				
				_escaleTask(taskToEscale[i], APILocator.getRoleAPI().loadRoleByKey(roleKeyToEscale).getId());

				
			} catch (DotSecurityException e) {
				Logger.error(EscalationJob.class, e.getMessage(), e);
			}

		

		}
	}

	public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req, RenderResponse res) throws Exception {
		
		return mapping.findForward("portlet.ext.plugins.expCont.struts");

	}
	
	

	private void _escaleTask(String taskId, String roleId) throws Exception {
		WorkflowTask task = (WorkflowTask) wAPI.findTaskById(taskId);
		expAPI.escaleTask(task.getId(), roleId);
	}
}
