package com.dotmarketing.portlets.workflows.util;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.CloseUtils;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import io.vavr.Tuple2;
import io.vavr.control.Try;

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

				Logger.debug(this, () -> "Importing scheme: " + scheme);
				workflowAPI.saveScheme(scheme, user);
			}

			List<Tuple2<String, String>> stepsWithActions = new ArrayList<>();
			for (final WorkflowStep step : importer.getSteps()) {

				Logger.debug(this, () -> "Importing step: " + step);
			    if(step.getEscalationAction()!=null) {
			        stepsWithActions.add(new Tuple2<>(step.getId(), step.getEscalationAction()));
			        step.setEscalationAction(null);
			    }
				workflowAPI.saveStep(step, user);
			}

			for (final WorkflowAction action : importer.getActions()) {

				Logger.debug(this, () -> "Importing action: " + action);
				final WorkflowAction validatedAction = validateAction (action);
				workflowAPI.saveAction(validatedAction, null, user);
			}
			
			// Now we can save the step and action
			for (Tuple2<String, String> stepsWithAction:stepsWithActions) {
			  WorkflowStep step  = workflowAPI.findStep(stepsWithAction._1);
			  step.setEscalationAction(stepsWithAction._2);
			  workflowAPI.saveStep(step, user);
			}
			
			
			for(final Map<String, String> actionStepMap : importer.getActionSteps()){

				Logger.debug(this, () -> "Importing actionStepMap: " + actionStepMap);
				workflowAPI.saveAction(actionStepMap.get(ACTION_ID),
						actionStepMap.get(STEP_ID),
						user,
						ConversionUtils.toInt(actionStepMap.get(ACTION_ORDER), 0));
			}

 			for (final WorkflowActionClass actionClass : importer.getActionClasses()) {

				Logger.debug(this, () -> "Importing actionClass: " + actionClass);
				workflowAPI.saveActionClass(actionClass, user);
			}

			if (UtilMethods.isSet(importer.getWorkflowStructures())) {
				for (final Map<String, String> map : importer.getWorkflowStructures()) {

					Logger.debug(this, () -> "Importing WorkflowStructures: " + map);
					DotConnect dc = new DotConnect();
					dc.setSQL("delete from workflow_scheme_x_structure where id=?");
					dc.addParam(map.get("id"));
					dc.loadResult();
					dc.setSQL(
							"insert into workflow_scheme_x_structure (id, scheme_id, structure_id) values (?, ?, ?)");
					dc.addParam(map.get("id"));
					dc.addParam(map.get("scheme_id"));
					dc.addParam(map.get("structure_id"));
					dc.loadResult();
				}
			}

			Logger.debug(this,
					() -> "Importing ActionClassParams: " + importer.getActionClassParams());
			workflowAPI.saveWorkflowActionClassParameters(importer.getActionClassParams(), user);

			this.saveSchemeSystemActionMappings      (workflowAPI, importer.getSchemeSystemActionWorkflowActionMappings());
			this.saveContentTypeSystemActionMappings (workflowAPI, importer.getContentTpeSystemActionWorkflowActionMappings());
		} catch (Exception e) {// Catch exception if any
			Logger.error(this.getClass(), "Error: " + e.getMessage(), e);
			throw new DotDataException(e);
		}
	}

	private Role getAnonRole () {
		return Try.of(()->APILocator.getRoleAPI().loadCMSAnonymousRole()).getOrElseThrow(e -> new DotRuntimeException(e));
	}

	private WorkflowAction validateAction(final WorkflowAction action) {

		final String nextAssign = action.getNextAssign();
		final Role role = Try.of(()->APILocator.getRoleAPI().loadRoleById(nextAssign)).getOrNull();
		if (null == role) {

			Logger.warn(this, "The role: " + nextAssign +
					" on the action: " + action.getName() + " does not exists, replacing by current user");
			final Role anonRole = getAnonRole();
			action.setNextAssign(anonRole.getId());
		}

		return action;
	}

	private void saveSchemeSystemActionMappings(
			final WorkflowAPI workflowAPI,
			final List<SystemActionWorkflowActionMapping> systemActionWorkflowActionMappings) throws DotDataException {

		if (UtilMethods.isSet(systemActionWorkflowActionMappings)) {

		    for (final SystemActionWorkflowActionMapping mapping : systemActionWorkflowActionMappings) {
                workflowAPI.mapSystemActionToWorkflowActionForWorkflowScheme(mapping.getSystemAction(),
                        mapping.getWorkflowAction(), (WorkflowScheme) mapping.getOwner());
            }
		}
	}

    private void saveContentTypeSystemActionMappings(
            final WorkflowAPI workflowAPI,
            final List<SystemActionWorkflowActionMapping> systemActionWorkflowActionMappings) throws DotDataException {

        if (UtilMethods.isSet(systemActionWorkflowActionMappings)) {

            for (final SystemActionWorkflowActionMapping mapping : systemActionWorkflowActionMappings) {
                workflowAPI.mapSystemActionToWorkflowActionForContentType(mapping.getSystemAction(),
                        mapping.getWorkflowAction(), (ContentType) mapping.getOwner());
            }
        }
    }

	public WorkflowSchemeImportExportObject buildExportObject() throws DotDataException, DotSecurityException {

		final WorkflowAPI workflowAPI      = APILocator.getWorkflowAPI();
		final List<WorkflowScheme> schemes = workflowAPI.findSchemes(true);

		return buildExportObject(schemes);
	}

	@CloseDBIfOpened
	public WorkflowSchemeImportExportObject buildExportObject(final List<WorkflowScheme> schemes)
			throws DotDataException, DotSecurityException {

		final WorkflowAPI workflowAPI  = APILocator.getWorkflowAPI();
		final List<WorkflowStep> steps = new ArrayList<>();
		final Set<String> workflowIds  = schemes.stream().map(scheme -> scheme.getId()).collect(Collectors.toSet());
		final List<SystemActionWorkflowActionMapping> schemeSystemActionWorkflowActionMappings = new ArrayList<>();
		final List<SystemActionWorkflowActionMapping> contentTypeSystemActionWorkflowActionMappings = new ArrayList<>();
		final List<WorkflowAction> actions 			  = new ArrayList<>();
		final List<WorkflowActionClass> actionClasses = new ArrayList<>();
		final List<WorkflowActionClassParameter> actionClassParams = new ArrayList<>();
		final List<Map<String, String>> actionStepsListMap 		   = new ArrayList<>();


		for (final WorkflowScheme scheme : schemes) {

			// scheme actions
			this.exportSchemeActions(workflowAPI, actions, actionClasses, actionClassParams, scheme);

			// steps actions
			this.exportStepActions(workflowAPI, actionStepsListMap, steps, scheme);
			this.exportSchemeSystemWorkflowActionMappings(workflowAPI, schemeSystemActionWorkflowActionMappings, scheme);
			this.exportSchemeContentTypeSystemWorkflowActionMappings(workflowAPI, contentTypeSystemActionWorkflowActionMappings, scheme);
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
		export.setSchemeSystemActionWorkflowActionMappings(schemeSystemActionWorkflowActionMappings);
		export.setContentTpeSystemActionWorkflowActionMappings(contentTypeSystemActionWorkflowActionMappings);
		return export;
	}

	private void exportSchemeContentTypeSystemWorkflowActionMappings(final WorkflowAPI workflowAPI,
			final List<SystemActionWorkflowActionMapping> contentTypeSystemActionWorkflowActionMappings,
			final WorkflowScheme scheme) throws DotDataException, DotSecurityException {

		final List<ContentType> contentTypes = workflowAPI.findContentTypesForScheme(scheme);
		if (UtilMethods.isSet(contentTypes)) {

			for (final ContentType contentType : contentTypes) {

				final List<SystemActionWorkflowActionMapping> mappings =
						workflowAPI.findSystemActionsByContentType(contentType, APILocator.systemUser());
				if (UtilMethods.isSet(mappings)) {

					for (final SystemActionWorkflowActionMapping mapping : mappings) {

						if (UtilMethods.isSet(mapping) &&
								UtilMethods.isSet(mapping.getWorkflowAction()) &&
								scheme.getId().equals(mapping.getWorkflowAction().getSchemeId())) { // if the action associated to the content type as default action belongs to the scheme, add it

							contentTypeSystemActionWorkflowActionMappings.add(mapping);
						}
					}
				}
			}
		}
	}

	private void exportSchemeSystemWorkflowActionMappings(final WorkflowAPI workflowAPI,
			final List<SystemActionWorkflowActionMapping> systemActionWorkflowActionMappings,
			final WorkflowScheme scheme) throws DotDataException, DotSecurityException {

		final List<SystemActionWorkflowActionMapping> systemActionWorkflowActionMappingsForScheme =
					workflowAPI.findSystemActionsByScheme(scheme, APILocator.systemUser());

		if (UtilMethods.isSet(systemActionWorkflowActionMappingsForScheme)) {
			systemActionWorkflowActionMappings.addAll(systemActionWorkflowActionMappingsForScheme);
		}
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

				actionStepsListMap.add(Map.of(ACTION_ID, workflowAction.getId(),
						STEP_ID, myStep.getId(),
						ACTION_ORDER, String.valueOf(actionOrder++)));
			}
        }

		steps.addAll(mySteps);
	}

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
