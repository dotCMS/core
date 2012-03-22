package com.dotmarketing.portlets.workflows.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;

public class WorkflowImportExportUtil {
	private static WorkflowImportExportUtil workflowImportExportUtil;

	public static WorkflowImportExportUtil getInstance() {
		if (workflowImportExportUtil == null) {
			synchronized ("WorkflowImportExportUtil.class") {
				if (workflowImportExportUtil == null) {
					workflowImportExportUtil = new WorkflowImportExportUtil();

				}
			}

		}
		return workflowImportExportUtil;

	}

	
	
	public String exportWorkflowsToJson() throws IOException, DotDataException, DotSecurityException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(buildExportObject());
	
	}
	
	
	public void exportWorkflows(File file) throws IOException {

		BufferedWriter out = null;
		try {
			FileWriter fstream = new FileWriter(file);
			out = new BufferedWriter(fstream);
		

			out.write(exportWorkflowsToJson());

		} catch (Exception e) {// Catch exception if any
			Logger.error(this.getClass(), "Error: " + e.getMessage(), e);
		} finally {

			out.close();

		}
	}

	public void importWorkflowExport(File file) throws IOException {

		WorkflowAPI wapi = APILocator.getWorkflowAPI();
		BufferedReader in = null;
		try {
			FileReader fstream = new FileReader(file);
			in = new BufferedReader(fstream);
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			StringWriter sw = new StringWriter();
			String str;
			while ((str = in.readLine()) != null) {
				sw.append(str);
			}

			WorkflowSchemeImportExportObject importer = mapper.readValue((String) sw.toString(), WorkflowSchemeImportExportObject.class);

			for (WorkflowScheme scheme : importer.getSchemes()) {
				wapi.saveScheme(scheme);
			}
			for (WorkflowStep step : importer.getSteps()) {
				wapi.saveStep(step);
			}

			for (WorkflowAction aciton : importer.getActions()) {
				wapi.saveAction(aciton, null);
			}

			for (WorkflowActionClass actionClass : importer.getActionClasses()) {
				wapi.saveActionClass(actionClass);
			}
			
			
			for(Map<String, String> map : importer.getWorkflowStructures()){
				DotConnect dc = new DotConnect();
				dc.setSQL("delete from workflow_scheme_x_structure where id=?");
				dc.addParam(map.get("id"));
				dc.loadResult();
				dc.setSQL("insert into workflow_scheme_x_structure (id, scheme_id, structure_id) values (?, ?, ?)");
				dc.addParam(map.get("id"));
				dc.addParam(map.get("scheme_id"));
				dc.addParam(map.get("structure_id"));
				dc.loadResult();
			}
			
			
			
			

			wapi.saveWorkflowActionClassParameters(importer.getActionClassParams());

		} catch (Exception e) {// Catch exception if any
			Logger.error(this.getClass(), "Error: " + e.getMessage(), e);
		} finally {

			in.close();

		}
	}

	public WorkflowSchemeImportExportObject buildExportObject() throws DotDataException, DotSecurityException {
		WorkflowAPI wapi = APILocator.getWorkflowAPI();
		List<WorkflowScheme> schemes = wapi.findSchemes(true);
		List<WorkflowStep> steps = new ArrayList<WorkflowStep>();
		List<WorkflowAction> actions = new ArrayList<WorkflowAction>();
		List<WorkflowActionClass> actionClasses = new ArrayList<WorkflowActionClass>();
		List<WorkflowActionClassParameter> actionClassParams = new ArrayList<WorkflowActionClassParameter>();

		for (WorkflowScheme scheme : schemes) {

			int stepOrder = 0;
			List<WorkflowStep> mySteps = wapi.findSteps(scheme);
			for (WorkflowStep myStep : mySteps) {
				myStep.setMyOrder(stepOrder++);
				int actionOrder = 0;
				List<WorkflowAction> myActions = wapi.findActions(myStep, APILocator.getUserAPI().getSystemUser());
				for (WorkflowAction myAction : myActions) {
					myAction.setOrder(actionOrder++);
					int actionClassOrder = 0;
					List<WorkflowActionClass> myActionClasses = wapi.findActionClasses(myAction);
					for (WorkflowActionClass myActionClass : myActionClasses) {
						myActionClass.setOrder(actionClassOrder++);
						Map<String, WorkflowActionClassParameter> myActionClassParams = wapi.findParamsForActionClass(myActionClass);
						List<WorkflowActionClassParameter> params = new ArrayList<WorkflowActionClassParameter>();
						for (String x : myActionClassParams.keySet()) {
							params.add(myActionClassParams.get(x));
						}
						actionClassParams.addAll(params);
					}
					actionClasses.addAll(myActionClasses);
				}
				actions.addAll(myActions);
			}
			steps.addAll(mySteps);
		}
		
		DotConnect dc = new DotConnect();
		dc.setSQL("select id, scheme_id, structure_id from workflow_scheme_x_structure");
		List<Map<String, String>> workflowStructures = dc.loadResults(); 
		
		
		
		
		
		
		
		
		WorkflowSchemeImportExportObject export = new WorkflowSchemeImportExportObject();
		export.setSchemes(schemes);

		export.setSteps(steps);
		export.setActions(actions);
		export.setActionClasses(actionClasses);
		export.setActionClassParams(actionClassParams);
		export.setWorkflowStructures(workflowStructures);
		return export;

	}

}
