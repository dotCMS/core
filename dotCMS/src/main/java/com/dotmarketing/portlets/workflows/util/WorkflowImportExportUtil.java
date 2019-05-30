package com.dotmarketing.portlets.workflows.util;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.CloseUtils;
import com.dotcms.util.ConversionUtils;
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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class WorkflowImportExportUtil {
	public static final String ACTION_ID = "actionId";
	public static final String STEP_ID = "stepId";
	public static final String ACTION_ORDER = "actionOrder";
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

	public void importWorkflowExport(final File file) throws IOException {

		this.importWorkflowExport(new FileReader(file));
	}

	public void importWorkflowExport(final Reader reader) throws IOException {

		final ObjectMapper mapper       		  = new ObjectMapper();
		final StringWriter stringWriter		      = new StringWriter();
		BufferedReader bufferedReader   		  = null;
		WorkflowSchemeImportExportObject importer = null;


		try {

			bufferedReader = new BufferedReader(reader);
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			String str;
			while ((str = bufferedReader.readLine()) != null) {
				stringWriter.append(str);
			}

			importer = mapper.readValue
					((String) stringWriter.toString(), WorkflowSchemeImportExportObject.class);

			this.importWorkflowExport(importer, APILocator.systemUser());
		} catch (Exception e) {// Catch exception if any
			Logger.error(this.getClass(), "Error: " + e.getMessage(), e);
		} finally {

			CloseUtils.closeQuietly(bufferedReader);
		}
	}

	@WrapInTransaction
	public void importWorkflowExport(final WorkflowSchemeImportExportObject importer,
									 final User user) throws IOException, DotDataException {

		final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();

		try {

			for (final WorkflowScheme scheme : importer.getSchemes()) {

				workflowAPI.saveScheme(scheme, user);
			}

			for (final WorkflowStep step : importer.getSteps()) {

				workflowAPI.saveStep(step, user);
			}

			for (final WorkflowAction action : importer.getActions()) {

				workflowAPI.saveAction(action, null, user);
			}

			for(final Map<String, String> actionStepMap : importer.getActionSteps()){

				workflowAPI.saveAction(actionStepMap.get(ACTION_ID),
						actionStepMap.get(STEP_ID),
						user,
						ConversionUtils.toInt(actionStepMap.get(ACTION_ORDER), 0));
			}

			for (final WorkflowActionClass actionClass : importer.getActionClasses()) {
				workflowAPI.saveActionClass(actionClass, user);
			}

			for(final Map<String, String> map : importer.getWorkflowStructures()) {

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

			workflowAPI.saveWorkflowActionClassParameters(importer.getActionClassParams(), user);

		} catch (Exception e) {// Catch exception if any
			Logger.error(this.getClass(), "Error: " + e.getMessage(), e);
			throw new DotDataException(e);
		}
	}

	public WorkflowSchemeImportExportObject buildExportObject() throws DotDataException, DotSecurityException {

		final WorkflowAPI workflowAPI      = APILocator.getWorkflowAPI();
		final List<WorkflowScheme> schemes = workflowAPI.findSchemes(true);

		return buildExportObject(schemes);
	}

	public WorkflowSchemeImportExportObject buildExportObject(final List<WorkflowScheme> schemes)
			throws DotDataException, DotSecurityException {

		final WorkflowAPI workflowAPI  = APILocator.getWorkflowAPI();
		final List<WorkflowStep> steps = new ArrayList<WorkflowStep>();
		final Set<String> workflowIds  = schemes.stream().map(scheme -> scheme.getId()).collect(Collectors.toSet());
		final List<WorkflowAction> actions 			  = new ArrayList<WorkflowAction>();
		final List<WorkflowActionClass> actionClasses = new ArrayList<WorkflowActionClass>();
		final List<WorkflowActionClassParameter> actionClassParams = new ArrayList<WorkflowActionClassParameter>();
		final List<Map<String, String>> actionStepsListMap 		   = new ArrayList<>();


		for (WorkflowScheme scheme : schemes) {

			// scheme actions
			this.exportSchemeActions(workflowAPI, actions, actionClasses, actionClassParams, scheme);

			// steps actions
			this.exportStepActions(workflowAPI, actionStepsListMap, steps, scheme);
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
		export.setWorkflowStructures(workflowStructures.stream()
				.filter(workflowStructure -> workflowIds.contains(workflowStructure.get("scheme_id").toString()))
				.collect(Collectors.toList()));
		export.setActionSteps(actionStepsListMap);
		return export;
	}

	private void exportStepActions(final WorkflowAPI wapi,
								   final List<Map<String, String>> actionStepsListMap,
								   final List<WorkflowStep> steps,
								   final WorkflowScheme scheme) throws DotDataException, DotSecurityException {

		int stepOrder = 0;
		List<WorkflowStep> mySteps = wapi.findSteps(scheme);
		// steps
		for (WorkflowStep myStep : mySteps) {

            myStep.setMyOrder(stepOrder++);
            int actionOrder = 0;
            // action by step
            final List<WorkflowAction> stepActions = wapi.findActions
					(myStep, APILocator.getUserAPI().getSystemUser());

            for (WorkflowAction workflowAction : stepActions) {

				actionStepsListMap.add(map(ACTION_ID, workflowAction.getId(),
						STEP_ID, myStep.getId(),
						ACTION_ORDER, String.valueOf(actionOrder++)));
			}
        }

		steps.addAll(mySteps);
	}

	@NotNull
	private void exportSchemeActions(final WorkflowAPI wapi,
													 final List<WorkflowAction> actions,
													 final List<WorkflowActionClass> actionClasses,
													 final List<WorkflowActionClassParameter> actionClassParams,
													 final WorkflowScheme scheme) throws DotDataException, DotSecurityException {

		final List<WorkflowAction> schemeActions =
				wapi.findActions(scheme, APILocator.getUserAPI().getSystemUser());

		for (WorkflowAction myAction : schemeActions) {

            int actionClassOrder = 0;
            final List<WorkflowActionClass> myActionClasses = wapi.findActionClasses(myAction);

            for (WorkflowActionClass myActionClass : myActionClasses) {

                myActionClass.setOrder(actionClassOrder++);
                final Map<String, WorkflowActionClassParameter> myActionClassParams = wapi.
						findParamsForActionClass(myActionClass);
                final List<WorkflowActionClassParameter> params = new ArrayList<>();

                for (String x : myActionClassParams.keySet()) {
                    params.add(myActionClassParams.get(x));
                }

                actionClassParams.addAll(params);
            }

            actionClasses.addAll(myActionClasses);
        }

		actions.addAll(schemeActions);
	}

}
