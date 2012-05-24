package com.dotmarketing.portlets.workflows.ajax;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;

public class WfStepAjax extends WfBaseAction {



	 public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{};
	
	/**
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	public void reorder(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String stepId = request.getParameter("stepId");
		String o = request.getParameter("stepOrder");
		String stepName = request.getParameter("stepName");
		boolean enableEscalation=request.getParameter("enableEscalation")!=null;
		String escalationAction = request.getParameter("escalationAction");
		String escalationTime = request.getParameter("escalationTime");
		WorkflowAPI wapi = APILocator.getWorkflowAPI();
		boolean stepResolved = request.getParameter("stepResolved") != null;
		int order = 0;
		try {
			WorkflowStep step = wapi.findStep(stepId);
			if(step.isNew()){
				writeError(response, "Cannot-edit-step");
				return;
			}
			step.setName(stepName);
			step.setResolved(stepResolved);
			order = step.getMyOrder();
			try{
				order = Integer.parseInt(o);
				wapi.reorderStep(step, order );
			}
			catch(Exception e1){
				wapi.saveStep(step);
			}

			
		}
		catch(Exception e){
			Logger.error(this.getClass(),e.getMessage(),e);
			writeError(response, e.getMessage());
			return;
		}
			
	}
	
	public void delete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String stepId = request.getParameter("stepId");

		WorkflowAPI wapi = APILocator.getWorkflowAPI();
		
		try {

			WorkflowStep step = wapi.findStep(stepId);
			wapi.deleteStep(step);
		} catch (DotDataException e) {
			Logger.error(this.getClass(),e.getMessage(),e);

				
				writeError(response, "Delete Failed:"+  e.getMessage());

		}
		
		
	}
	public void add(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, Object> map = request.getParameterMap();
		
		
		String stepName = URLDecoder.decode(request.getParameter("stepName"), "UTF-8");
		String schemeId = request.getParameter("schemeId");
		boolean stepResolved = request.getParameter("stepResolved") != null;
		WorkflowAPI wapi = APILocator.getWorkflowAPI();
		
		try {

			List<WorkflowStep> steps =wapi.findSteps(wapi.findScheme(schemeId));
			int maxOrder = 0;
			for(WorkflowStep step : steps){
				if(step.getMyOrder() > maxOrder){
					maxOrder = step.getMyOrder() ;
				}
			}
			WorkflowStep step = new WorkflowStep();
			step.setMyOrder(maxOrder +1);
			step.setName(stepName);
			step.setSchemeId(schemeId);
			step.setResolved(stepResolved);
			wapi.saveStep(step);
			
		} catch (DotDataException e) {
			Logger.error(this.getClass(),e.getMessage(),e);
			writeError(response, e.getMessage());
		}
		
		
	}

	
	public void listByScheme(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String schemeId = request.getParameter("schemeId");
		if(schemeId ==null || schemeId.length() < 1){
			return;
		}
		WorkflowAPI wapi = APILocator.getWorkflowAPI();

		try {
			WorkflowScheme scheme = wapi.findScheme(schemeId);
			List<WorkflowStep> steps =  wapi.findSteps(scheme);
			
			
            response.getWriter().write(stepsToJson(steps));

			
		}
		catch(Exception e){
			Logger.error(this.getClass(),e.getMessage(),e);
			writeError(response, e.getMessage());
			return;
		}
			
	}
	
	
	private String stepsToJson(List<WorkflowStep> steps) throws JsonGenerationException, JsonMappingException, IOException{
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Map<String,Object> m = new LinkedHashMap<String, Object>();
        
        List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
        for(WorkflowStep step : steps){

        	Map<String,Object> map = new HashMap<String,Object>();
        	map.put("name", step.getName()   );
        	map.put("id", step.getId());
    		list.add(map);
        }
        

        m.put("identifier", "id");
        m.put("label", "name");
        m.put("items", list);
		return mapper.defaultPrettyPrintingWriter().writeValueAsString(m);
	}
	
	
	
}
