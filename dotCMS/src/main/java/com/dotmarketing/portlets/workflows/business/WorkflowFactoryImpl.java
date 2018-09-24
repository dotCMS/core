package com.dotmarketing.portlets.workflows.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.DbContentTypeTransformer;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.*;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import org.apache.commons.beanutils.BeanUtils;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Implementation class for the {@link WorkFlowFactory}.
 *
 * @author root
 * @since Mar, 22, 2012
 */

public class WorkflowFactoryImpl implements WorkFlowFactory {

	private final WorkflowCache cache;
	private final WorkflowSQL   sql;


	/**
	 * Creates an instance of the {@link WorkFlowFactory}.
	 */
	public WorkflowFactoryImpl() {
		this.sql   = WorkflowSQL.getInstance();
		this.cache = CacheLocator.getWorkFlowCache();
	}

	@Override
	public void attachFileToTask(WorkflowTask task, String fileInode) throws DotDataException {
		final WorkFlowTaskFiles taskFile = new WorkFlowTaskFiles();
		taskFile.setWorkflowtaskId(task.getId());
		taskFile.setFileInode(fileInode);
		HibernateUtil.save(taskFile);
	}

	/**
	 *
	 * @param obj
	 * @param map
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private Object convert(Object obj, Map<String, Object> map) throws IllegalAccessException, InvocationTargetException {
		BeanUtils.copyProperties(obj, map);
		return obj;
	}

	/**
	 *
	 * @param row
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private WorkflowAction convertAction(Map<String, Object> row) throws IllegalAccessException, InvocationTargetException {
		final WorkflowAction action = new WorkflowAction();
		row.put("schemeId", row.get("scheme_id"));
		row.put("condition", row.get("condition_to_progress"));
		row.put("nextStep", row.get("next_step_id"));
		row.put("nextAssign", row.get("next_assign"));
		row.put("order", row.get("my_order"));
		row.put("requiresCheckout", row.get("requires_checkout"));
		row.put("showOn", WorkflowState.toSet(row.get("show_on")));
		row.put("roleHierarchyForAssign", row.get("use_role_hierarchy_assign"));

		BeanUtils.copyProperties(action, row);
		return action;
	}

	/**
	 *
	 * @param row
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private WorkflowActionClass convertActionClass(Map<String, Object> row) throws IllegalAccessException, InvocationTargetException {
		final WorkflowActionClass actionClass = new WorkflowActionClass();

		row.put("clazz", row.get("clazz"));

		row.put("order", row.get("my_order"));
		row.put("actionId", row.get("action_id"));
		BeanUtils.copyProperties(actionClass, row);
		return actionClass;
	}

	/**
	 *
	 * @param row
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private WorkflowActionClassParameter convertActionClassParameter(Map<String, Object> row) throws IllegalAccessException, InvocationTargetException {
		final WorkflowActionClassParameter param = new WorkflowActionClassParameter();
		row.put("actionClassId", row.get("workflow_action_class_id"));
		BeanUtils.copyProperties(param, row);
		return param;
	}

	/**
	 *
	 * @param rs
	 * @param clazz
	 * @return
	 * @throws DotDataException
	 */
	private List convertListToObjects(List<Map<String, Object>> rs, Class clazz) throws DotDataException {
		final List ret = new ArrayList();
		try {
			for (final Map<String, Object> map : rs) {
				ret.add(this.convertMaptoObject(map, clazz));
			}
		} catch (final Exception e) {
			throw new DotDataException("cannot convert object to " + clazz + " " + e.getMessage());

		}
		return ret;
	}

	/**
	 *
	 * @param map
	 * @param clazz
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private Object convertMaptoObject(Map<String, Object> map, Class clazz) throws InstantiationException, IllegalAccessException, InvocationTargetException {

		if(ContentType.class.equals(clazz)){
			//Content type is an abstract class therefore it can not be instantiated directly
			return new DbContentTypeTransformer(map).from();
		}

		final Object obj = clazz.newInstance();

		if (obj instanceof WorkflowAction) {
			return this.convertAction(map);
		} else if (obj instanceof WorkflowStep) {
			return this.convertStep(map);
		} else if (obj instanceof WorkflowActionClass) {
			return this.convertActionClass(map);
		} else if (obj instanceof WorkflowActionClassParameter) {
			return this.convertActionClassParameter(map);
		} else if (obj instanceof WorkflowScheme) {
			return this.convertScheme(map);
		} else if (obj instanceof WorkflowHistory) {
			return this.convertHistory(map);
		} else if (obj instanceof WorkflowTask) {
			return this.convertTask(map);
		} else {
			return this.convert(obj, map);
		}
	}

	/**
	 *
	 * @param row
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private WorkflowScheme convertScheme(Map<String, Object> row) throws IllegalAccessException, InvocationTargetException {
		final WorkflowScheme scheme = new WorkflowScheme();
		row.put("entryActionId", row.get("entry_action_id"));
		row.put("defaultScheme", row.get("default_scheme"));
		row.put("modDate", row.get("mod_date"));

		BeanUtils.copyProperties(scheme, row);

		return scheme;
	}

	/**
	 *
	 * @param row
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private WorkflowStep convertStep(Map<String, Object> row) throws IllegalAccessException, InvocationTargetException {
		final WorkflowStep step = new WorkflowStep();
		row.put("myOrder", row.get("my_order"));
		row.put("schemeId", row.get("scheme_id"));
		row.put("enableEscalation", row.get("escalation_enable"));
		row.put("escalationAction", row.get("escalation_action"));
		row.put("escalationTime", row.get("escalation_time"));
		BeanUtils.copyProperties(step, row);

		return step;
	}

	private WorkflowTask convertTask(Map<String, Object> row)
			throws IllegalAccessException, InvocationTargetException {

		final WorkflowTask task = new WorkflowTask();
		row.put("languageId", row.get("language_id"));
		row.put("creationDate", row.get("creation_date"));
		row.put("modDate", row.get("mod_date"));
		row.put("dueDate", row.get("due_date"));
		row.put("createdBy", row.get("created_by"));
		row.put("assignedTo", row.get("assigned_to"));
		row.put("belongsTo", row.get("belongs_to"));
		BeanUtils.copyProperties(task, row);

		return task;
	}

	/**
	 *
	 * @param row
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private WorkflowHistory convertHistory(Map<String, Object> row) throws IllegalAccessException, InvocationTargetException {
		final WorkflowHistory scheme = new WorkflowHistory();
		row.put("actionId", row.get("workflow_action_id"));

		BeanUtils.copyProperties(scheme, row);

		return scheme;
	}

	@Override
	public void copyWorkflowAction(WorkflowAction from, WorkflowStep step) throws DotDataException {
		throw new DotWorkflowException("Not implemented");
	}

	@Override
	public void copyWorkflowActionClass(WorkflowActionClass from, WorkflowAction action) throws DotDataException {
		throw new DotWorkflowException("Not implemented");
	}

	@Override
	public void copyWorkflowActionClassParameter(WorkflowActionClassParameter from, WorkflowActionClass actionClass) throws DotDataException {
		throw new DotWorkflowException("Not implemented");
	}

	@Override
	public void copyWorkflowStep(WorkflowStep from, WorkflowScheme scheme) throws DotDataException {
		throw new DotWorkflowException("Not implemented");
	}

	@Override
	public int countTasks(WorkflowSearcher searcher) throws DotDataException {
		DotConnect dc = getWorkflowSqlQuery(searcher, true);
		return dc.getInt("mycount");
	}

	@Override
	public void deleteAction(final WorkflowAction action) throws DotDataException, AlreadyExistException {

		Logger.debug(this,
				"Removing action steps dependencies, for the action: " + action.getId());

		final List<Map<String, Object>> stepIdList =
				new DotConnect().setSQL(sql.SELECT_STEPS_ID_BY_ACTION)
				.addParam(action.getId()).loadObjectResults();

		if (null != stepIdList && stepIdList.size() > 0) {
			new DotConnect().setSQL(sql.DELETE_ACTIONS_BY_STEP)
					.addParam(action.getId()).loadResult();

			for (Map<String, Object> stepIdRow : stepIdList) {
				Logger.debug(this,
						"Removing action steps cache " + stepIdRow.get("stepid"));
				final WorkflowStep proxyStep = new WorkflowStep();
				proxyStep.setId((String)stepIdRow.get("stepid"));
				cache.removeActions(proxyStep);
			}
		}

		Logger.debug(this,
				"Removing the action: " + action.getId());

		new DotConnect().setSQL(sql.DELETE_ACTION)
				.addParam(action.getId()).loadResult();

		final WorkflowScheme proxyScheme = new WorkflowScheme();
		proxyScheme.setId(action.getSchemeId());
		cache.removeActions(proxyScheme);
		cache.remove(action);

		// update scheme mod date
		final WorkflowScheme scheme = findScheme(action.getSchemeId());
		saveScheme(scheme);
	}

	@Override
	public void deleteAction(final WorkflowAction action, final WorkflowStep step) throws DotDataException, AlreadyExistException {

		Logger.debug(this, "Deleting the action: " + action.getId() +
						", from the step: " + step.getId());

		new DotConnect().setSQL(sql.DELETE_ACTION_STEP)
				.addParam(action.getId()).addParam(step.getId()).loadResult();

		Logger.debug(this, "Cleaning the actions from the step CACHE: " + step.getId());
		cache.removeActions(step);

		Logger.debug(this, "Updating the scheme: " + step.getSchemeId());
		// update scheme mod date
		final WorkflowScheme scheme = findScheme(step.getSchemeId());
		saveScheme(scheme);
	} // deleteAction

	@Override
	public void deleteActions(final WorkflowStep step) throws DotDataException, AlreadyExistException {

		Logger.debug(this, "Removing the actions associated to the step: " + step.getId());
		new DotConnect().setSQL(sql.DELETE_ACTIONS_STEP)
				.addParam(step.getId()).loadResult();

		Logger.debug(this, "Removing the actions cache associated to the step: " + step.getId());
		final List<WorkflowAction> actions =
				this.cache.getActions(step);
		if (null != actions) {
			actions.stream().forEach(action -> this.cache.remove(action));
		}
		cache.removeActions(step);


		Logger.debug(this, "Updating schema associated to the step: " + step.getId());
		// update scheme mod date
		final WorkflowScheme scheme = findScheme(step.getSchemeId());
		saveScheme(scheme);
	} // deleteActions.

	@Override
	public void deleteActionClass(WorkflowActionClass actionClass) throws DotDataException, AlreadyExistException {
		String actionId = actionClass.getActionId();
		final DotConnect db = new DotConnect();
		db.setSQL(sql.DELETE_ACTION_CLASS_PARAM_BY_ACTION_CLASS);
		db.addParam(actionClass.getId());
		db.loadResult();

		db.setSQL(sql.DELETE_ACTION_CLASS);
		db.addParam(actionClass.getId());
		db.loadResult();

		// update scheme mod date
		final WorkflowAction action = findAction(actionId);
		final WorkflowScheme scheme = findScheme(action.getSchemeId());
		saveScheme(scheme);
		this.cache.remove(action);
	}

	/**
	 *
	 * @param action
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws AlreadyExistException
	 */
	public void deleteActionClassByAction(WorkflowAction action) throws DotDataException, DotSecurityException, AlreadyExistException {

		new DotConnect().setSQL(sql.DELETE_ACTION_CLASS_BY_ACTION).addParam(action.getId()).loadResult();

		// update scheme mod date
		final WorkflowScheme scheme = findScheme(action.getSchemeId());
		saveScheme(scheme);
		this.cache.remove(action);
	}

	@Override
	public void deleteComment(WorkflowComment comment) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL("delete from workflow_comment where id = ?");
		db.addParam(comment.getId());
		db.loadResult();
	}

	@Override
	public void deleteStep(final WorkflowStep step,
						   final Consumer<WorkflowTask> workflowTaskConsumer) throws DotDataException, AlreadyExistException {
		final String schemeId = step.getSchemeId();
		final DotConnect db   = new DotConnect();

		// delete tasks referencing it
		db.setSQL("select id from workflow_task where status=?");
		db.addParam(step.getId());
		for(final Map<String,Object> resultMap : db.loadObjectResults()) {

			final String taskId		= (String) resultMap.get("id");
			final WorkflowTask task = findWorkFlowTaskById(taskId);
			deleteWorkflowTask(task);
			if (null != workflowTaskConsumer) {
				workflowTaskConsumer.accept(task);
			}
		}

		db.setSQL(sql.DELETE_STEP);
		db.addParam(step.getId());
		db.loadResult();
		cache.remove(step);

		// update scheme mod date
		WorkflowScheme scheme = findScheme(schemeId);
		saveScheme(scheme);
	}

	@Override
	public int getCountContentletsReferencingStep(WorkflowStep step) throws DotDataException{

		final DotConnect db = new DotConnect();

		// get step related assets
		db.setSQL(sql.SELECT_COUNT_CONTENTLES_BY_STEP);
		db.addParam(step.getId());
		Map<String,Object> res = db.loadObjectResults().get(0);
		return ConversionUtils.toInt(res.get("count"), 0);
	}

	@Override
	public void deleteWorkflowActionClassParameters(WorkflowActionClass actionClass) throws DotDataException, AlreadyExistException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.DELETE_ACTION_CLASS_PARAM_BY_ACTION_CLASS);
		db.addParam(actionClass.getId());
		db.loadResult();

		// update scheme mod date
		WorkflowAction action = findAction(actionClass.getActionId());
		WorkflowScheme scheme = findScheme(action.getSchemeId());
		saveScheme(scheme);

	}

	@Override
	public void deleteWorkflowHistory(WorkflowHistory history) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL("delete from workflow_history where id = ?");
		db.addParam(history.getId());
		db.loadResult();
	}

	@Override
	public void deleteWorkflowTask(WorkflowTask task) throws DotDataException {
		final DotConnect db = new DotConnect();

		HibernateUtil.evict(task);

		Contentlet c = new Contentlet();
		c.setIdentifier(task.getWebasset());
		c.setLanguageId(task.getLanguageId());

		boolean localTransaction = false;
		try {
			localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

			/* Clean the comments */
			db.setSQL("delete from workflow_comment where workflowtask_id = ?");
			db.addParam(task.getId());
			db.loadResult();

			/* Clean the history */
			db.setSQL("delete from workflow_history where workflowtask_id = ?");
			db.addParam(task.getId());
			db.loadResult();

			/* Clean the files of task */
			db.setSQL("delete from workflowtask_files where workflowtask_id = ?");
			db.addParam(task.getId());
			db.loadResult();

			/* delete the task */
			db.setSQL("delete from workflow_task where id = ?");
			db.addParam(task.getId());
			db.loadResult();

			if(localTransaction){
				HibernateUtil.closeAndCommitTransaction();
			}

		} catch (final Exception e) {
			if(localTransaction){
				HibernateUtil.rollbackTransaction();
			}
			Logger.error(this, "deleteWorkflowTask failed:" + e, e);
			throw new DotDataException(e);
		}
		finally {
			cache.remove(task);
		}
	}

	@Override
	public WorkflowAction findAction(String id) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.SELECT_ACTION);
		db.addParam(id);
		try {
			return (WorkflowAction) this.convertListToObjects(db.loadObjectResults(), WorkflowAction.class).get(0);
		} catch (IndexOutOfBoundsException ioob) {
			return null;
		}
	}

	@Override
	public WorkflowAction findAction(final String actionId,
									 final String stepId) throws DotDataException {

		final DotConnect db = new DotConnect();
		db.setSQL(sql.SELECT_ACTION_BY_STEP);
		db.addParam(actionId).addParam(stepId);

		try {
			return (WorkflowAction) this.convertListToObjects(db.loadObjectResults(), WorkflowAction.class).get(0);
		} catch (IndexOutOfBoundsException ioob) {
			return null;
		}
	}

	@Override
	public WorkflowActionClass findActionClass(String id) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.SELECT_ACTION_CLASS);
		db.addParam(id);

		try {
			return (WorkflowActionClass) this.convertListToObjects(db.loadObjectResults(), WorkflowActionClass.class).get(0);
		} catch (IndexOutOfBoundsException ioob) {
			return null;
		}
	}

	@Override
	public List<WorkflowActionClass> findActionClasses(final WorkflowAction action) throws DotDataException {

		List<WorkflowActionClass> classes = cache.getActionClasses(action);

		if (null == classes) {

			classes = this.convertListToObjects(new DotConnect().setSQL(sql.SELECT_ACTION_CLASSES_BY_ACTION)
					.addParam(action.getId()).loadObjectResults(), WorkflowActionClass.class);

			classes = (classes == null)?Collections.emptyList():classes;

			cache.addActionClasses(action, classes);
		}

		return classes;
	}

	public WorkflowActionClassParameter findActionClassParameter(String id) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.SELECT_ACTION_CLASS_PARAM);
		db.addParam(id);
		return (WorkflowActionClassParameter) this.convertListToObjects(db.loadObjectResults(), WorkflowActionClassParameter.class).get(0);
	}

	@Override
	public List<WorkflowAction> findActions(final WorkflowStep step) throws DotDataException {

		List<WorkflowAction> actions = cache.getActions(step);

		if(null == actions) {

			actions = this.convertListToObjects(
					new DotConnect().setSQL(sql.SELECT_ACTIONS_BY_STEP)
							.addParam(step.getId()).loadObjectResults(), WorkflowAction.class);

			if (null == actions) {

				actions = Collections.emptyList();
				cache.addActions(step, actions);
				return actions;
			}

			cache.addActions(step, actions);
		}

		// we need always a copy to avoid futher modification to the WorkflowAction since they are not immutable.
		return ImmutableList.copyOf(actions);
	}

	@Override
	public List<WorkflowAction> findActions(final WorkflowScheme scheme) throws DotDataException {

		List<WorkflowAction> actions = cache.getActions(scheme);
		if(null == actions) {

			final DotConnect db = new DotConnect();
			db.setSQL(sql.SELECT_ACTIONS_BY_SCHEME);
			db.addParam(scheme.getId());
			actions =  this.convertListToObjects(db.loadObjectResults(), WorkflowAction.class);

			if(actions == null) {
				actions = new ArrayList<>();
			}

			cache.addActions(scheme, actions);
		}

		return actions;
	} // findActions.


	@Override
	public Map<String, WorkflowActionClassParameter> findParamsForActionClass(WorkflowActionClass actionClass) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.SELECT_ACTION_CLASS_PARAMS_BY_ACTIONCLASS);
		db.addParam(actionClass.getId());
		final List<WorkflowActionClassParameter> list = (List<WorkflowActionClassParameter>) this.convertListToObjects(db.loadObjectResults(), WorkflowActionClassParameter.class);
		final Map<String, WorkflowActionClassParameter> map = new LinkedHashMap<String, WorkflowActionClassParameter>();
		for (final WorkflowActionClassParameter param : list) {
			map.put(param.getKey(), param);
		}

		return map;

	}

	@Override
	public WorkflowScheme findScheme(String id) throws DotDataException {
		WorkflowScheme scheme = cache.getScheme(id);
		if (scheme == null) {
			try {
				final DotConnect db = new DotConnect();
				db.setSQL(sql.SELECT_SCHEME);
				db.addParam(id);
				scheme = (WorkflowScheme) this.convertListToObjects(db.loadObjectResults(), WorkflowScheme.class).get(0);
				cache.add(scheme);
			} catch (final IndexOutOfBoundsException e) {
				throw new DoesNotExistException("Workflow-does-not-exists-scheme");
			} catch (final Exception e) {
				throw new DotDataException(e.getMessage(), e);
			}
		}
		return scheme;
	}


	@Override
	public List<WorkflowScheme> findSchemesForStruct(final String structId) throws DotDataException {

		List<WorkflowScheme> schemes = cache.getSchemesByStruct(structId);

		if (schemes != null) {

			// checks if any of the schemes has been invalidated (save recently and needs to refresh the schemes for the content type).
			if (!schemes.stream().filter(scheme -> null == cache.getScheme(scheme.getId())).findFirst().isPresent()) {
				return schemes;
			}
		}

		final DotConnect db = new DotConnect();
		db.setSQL(sql.SELECT_SCHEME_BY_STRUCT);
		db.addParam(structId);
		try {
			schemes = this.convertListToObjects(db.loadObjectResults(), WorkflowScheme.class);
		} catch (final Exception er) {
			schemes = new ArrayList();
		}

		cache.addForStructure(structId, schemes);
		schemes.stream().forEach(scheme -> cache.add(scheme));
		return schemes;

	}

	@Override
	public WorkflowScheme findSystemWorkflow() throws DotDataException {
		return this.findScheme(WorkFlowFactory.SYSTEM_WORKFLOW_ID);
	}

	@Override
	public List<WorkflowScheme> findSchemes(boolean showArchived) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.SELECT_SCHEMES);
		db.addParam(false);
		db.addParam(showArchived);
		return this.convertListToObjects(db.loadObjectResults(), WorkflowScheme.class);
	}

	@Override
	public List<WorkflowScheme> findArchivedSchemes() throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.SELECT_SCHEMES);
		db.addParam(true);
		db.addParam(true);
		return this.convertListToObjects(db.loadObjectResults(), WorkflowScheme.class);
	}

	@Override
	public WorkflowStep findStep(String id) throws DotDataException {
		WorkflowStep step = cache.getStep(id);
		if (step == null) {
			final DotConnect db = new DotConnect();
			db.setSQL(sql.SELECT_STEP);
			db.addParam(id);
			try{
			  step = (WorkflowStep) this.convertListToObjects(db.loadObjectResults(), WorkflowStep.class).get(0);
			  cache.add(step);
			}catch (IndexOutOfBoundsException e){
				throw new DoesNotExistException("Workflow-does-not-exists-step");
			}
		}
		return step;
	}

	@Override
	public List<WorkflowStep> findStepsByContentlet(final Contentlet contentlet, final List<WorkflowScheme> schemes) throws DotDataException {
		List<WorkflowStep> steps            = new ArrayList<>();
        List<WorkflowStep> currentSteps     = cache.getSteps(contentlet);
		String workflowTaskId        		= null;
		List<Map<String, Object>> dbResults = null;

		if (currentSteps == null) {
            WorkflowStep step = null;
			try {
				final DotConnect db = new DotConnect();
				db.setSQL(sql.SELECT_STEP_BY_CONTENTLET);
				db.addParam(contentlet.getIdentifier());
				db.addParam(contentlet.getLanguageId());

				dbResults = db.loadObjectResults();
                step      = (WorkflowStep) this.convertListToObjects
						(dbResults, WorkflowStep.class).get(0);
                steps.add(step);

				workflowTaskId =  (String)dbResults.get(0).get("workflowid");
			} catch (final Exception e) {
				Logger.debug(this.getClass(), e.getMessage());
			}

			if (step == null) {
				try {
					for(WorkflowScheme scheme : schemes) {
						final List<WorkflowStep> schemeSteps = this.findSteps(scheme);
						if(UtilMethods.isSet(schemeSteps)){
						   step = schemeSteps.get(0);
						   steps.add(step);
						}
					}
					//Add to cache list of steps
				} catch (final Exception e) {
					throw new DotDataException("Unable to find workflow step for content id:" + contentlet.getIdentifier());
				}
			}
		} else {
			steps.addAll(currentSteps);
		}
        // if the existing task belongs to another workflow schema, then remove it
		if (steps.size() == 1 && !existSchemeIdOnSchemesList(steps.get(0).getSchemeId(),schemes)) {

			if (null != workflowTaskId && !(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) ) {
				this.deleteWorkflowTask(this.findWorkFlowTaskById(workflowTaskId));
			}

            steps = Collections.emptyList();
		}

        cache.addSteps(contentlet, steps);

		return steps;
	}

	@Override
	public boolean existSchemeIdOnSchemesList(String schemeId, List<WorkflowScheme> schemes){
	    boolean exist = false;
	    for(WorkflowScheme scheme : schemes){
	        if(schemeId.equals(scheme.getId())){
	            exist = true;
	            break;
            }
        }
        return exist;
    }

	@Override
	public List<WorkflowStep> findSteps(WorkflowScheme scheme) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.SELECT_STEPS_BY_SCHEME);
		db.addParam(scheme.getId());
		return this.convertListToObjects(db.loadObjectResults(), WorkflowStep.class);

	}

	@Override
	public WorkflowTask findTaskByContentlet(final Contentlet contentlet) throws DotDataException {

		WorkflowTask task = null;

		if (!UtilMethods.isSet(contentlet.getIdentifier()) || cache.is404(contentlet)) {
			return task;
		}

		task = cache.getTask(contentlet);

		if (task == null) {

			final DotConnect db = new DotConnect();
			db.setSQL(WorkflowSQL.SELECT_TASK);
			db.addParam(contentlet.getIdentifier());
			db.addParam(contentlet.getLanguageId());

			List<WorkflowTask> foundTasks = this
					.convertListToObjects(db.loadObjectResults(), WorkflowTask.class);
			if (null != foundTasks && !foundTasks.isEmpty()) {
				task = foundTasks.get(0);
			}

			if (null != task && null != task.getId()) {
				cache.addTask(contentlet, task);
			} else {
				cache.add404Task(contentlet);
			}
		}

		return task;
	}

	@Override
	public WorkflowComment findWorkFlowCommentById(String id) throws DotDataException {
		final HibernateUtil hu = new HibernateUtil(WorkflowComment.class);
		hu.setQuery("from workflow_comment in class com.dotmarketing.portlets.workflows.model.WorkflowComment where id = ?");
		hu.setParam(id);
		return (WorkflowComment) hu.load();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<WorkflowComment> findWorkFlowComments(WorkflowTask task) throws DotDataException {
		final HibernateUtil hu = new HibernateUtil(WorkflowComment.class);
		hu.setQuery("from workflow_comment in class com.dotmarketing.portlets.workflows.model.WorkflowComment " + "where workflowtask_id = ? order by creation_date desc");
		hu.setParam(task.getId());
		return (List<WorkflowComment>) hu.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<WorkflowHistory> findWorkflowHistory(WorkflowTask task) throws DotDataException {
		final HibernateUtil hu = new HibernateUtil(WorkflowHistory.class);
		hu.setQuery("from workflow_history in class com.dotmarketing.portlets.workflows.model.WorkflowHistory " + "where workflowtask_id = ? order by creation_date");
		hu.setParam(task.getId());
		return (List<WorkflowHistory>) hu.list();
	}

	@Override
	public WorkflowHistory findWorkFlowHistoryById(String id) throws DotDataException {
		final HibernateUtil hu = new HibernateUtil(WorkflowHistory.class);
		hu.setQuery("from workflow_history in class com.dotmarketing.portlets.workflows.model.WorkflowHistory where id = ?");
		hu.setParam(id);
		return (WorkflowHistory) hu.load();
	}

	@Override
	public WorkflowTask findWorkFlowTaskById(String id) throws DotDataException {
		final HibernateUtil hu = new HibernateUtil(WorkflowTask.class);
		hu.setQuery("from workflow_task in class com.dotmarketing.portlets.workflows.model.WorkflowTask where id = ?");
		hu.setParam(id);
		return (WorkflowTask) hu.load();
	}

	@SuppressWarnings("unchecked")
	public List<Contentlet> findWorkflowTaskFilesAsContent(WorkflowTask task, User user) throws DotDataException {
		final HibernateUtil hu = new HibernateUtil(WorkFlowTaskFiles.class);
		hu.setQuery("from workflow_task_files in class com.dotmarketing.portlets.workflows.model.WorkFlowTaskFiles where workflowtask_id = ?");
		hu.setParam(task.getId());
		List<Contentlet> contents = new ArrayList<Contentlet>();
		List<WorkFlowTaskFiles> l =  hu.list();

		for (WorkFlowTaskFiles f : l) {
			try {
				contents.add(APILocator.getContentletAPI().find(f.getFileInode(), user, false));
			} catch (DotSecurityException e) {
				throw new DotDataException(e.getMessage(),e);
			} catch(ClassCastException c) {
				// not file as contentlet
			}

		}


		return contents;
	}

	/**
	 *
	 * @param searcher
	 * @param counting
	 * @return
	 * @throws DotDataException
	 */
	private DotConnect getWorkflowSqlQuery(WorkflowSearcher searcher, boolean counting) throws DotDataException {

		DotConnect dc = new DotConnect();
		final StringBuilder query = new StringBuilder();

		if(counting)
			query.append("select count(*) as mycount from workflow_task ");
		else
			query.append("select workflow_task.*  from workflow_task ");


		query.append(", workflow_scheme, workflow_step ");
		query.append(" where  ");
		if (UtilMethods.isSet(searcher.getKeywords())) {
			query.append(" (lower(workflow_task.title) like ? or ");
			if(DbConnectionFactory.isMsSql())
				query.append(" lower(cast(workflow_task.description as varchar(max))) like ? )  and ");
			else
				query.append(" lower(workflow_task.description) like ? )  and ");
		}

		if(!searcher.getShow4All() || !(APILocator.getRoleAPI().doesUserHaveRole(searcher.getUser(), APILocator.getRoleAPI().loadCMSAdminRole())
				|| APILocator.getRoleAPI().doesUserHaveRole(searcher.getUser(),RoleAPI.WORKFLOW_ADMIN_ROLE_KEY))) {
			final List<Role> userRoles = new ArrayList<Role>();
			if (UtilMethods.isSet(searcher.getAssignedTo())) {


				final Role r = APILocator.getRoleAPI().loadRoleById(searcher.getAssignedTo());
				if(r!=null)userRoles.add(r);
			} else {
				userRoles.addAll(APILocator.getRoleAPI().loadRolesForUser(searcher.getUser().getUserId(), false));
				userRoles.add(APILocator.getRoleAPI().getUserRole(searcher.getUser()));

			}

			String rolesString = "";

			for (final Role role : userRoles) {
				if (!rolesString.equals("")) {
					rolesString += ",";
				}
				rolesString += "'" + role.getId() + "'";
			}

			if(rolesString.length()>0){
				query.append(" ( workflow_task.assigned_to in (" + rolesString + ")  ) and ");
			}
		}
		query.append(" workflow_step.id = workflow_task.status and workflow_step.scheme_id = workflow_scheme.id and ");

		if(searcher.getDaysOld()!=-1) {
			if(DbConnectionFactory.isMySql())
				query.append(" datediff(now(),workflow_task.creation_date)>=?");
			else if(DbConnectionFactory.isPostgres())
				query.append(" extract(day from (now()-workflow_task.creation_date))>=?");
			else if(DbConnectionFactory.isMsSql())
				query.append(" datediff(d,workflow_task.creation_date,GETDATE())>=?");
			else if(DbConnectionFactory.isOracle())
				query.append(" floor(sysdate-workflow_task.creation_date)>=?");
			else if(DbConnectionFactory.isH2())
				query.append(" datediff('YEAR',current_date(),workflow_task.creation_date)>=?");

			query.append(" and ");
		}

		if (!searcher.isClosed() && searcher.isOpen()) {
			query.append("  workflow_step.resolved = " + DbConnectionFactory.getDBFalse() + " and ");
		} else if (searcher.isClosed() && !searcher.isOpen()) {
			query.append(" workflow_step.resolved = " + DbConnectionFactory.getDBTrue() + " and ");
		}

		if (UtilMethods.isSet(searcher.getSchemeId())) {
			query.append(" workflow_scheme.id = ? and ");
		}

		if (UtilMethods.isSet(searcher.getStepId())) {
			query.append(" workflow_step.id = ? and ");
		}

		query.append(" 1=1  ");
		if (!counting) {
			query.append(" order by ");
			String orderby="";
			if (!UtilMethods.isSet(searcher.getStepId())) {
				// condition.append(" status , ");
			}
			if (UtilMethods.isSet(searcher.getOrderBy())) {
				orderby=searcher.getOrderBy().replaceAll("[^\\w_\\. ]", "");
			} else {

				orderby="mod_date desc";
			}
			query.append(orderby.replace("mod_date", "workflow_task.mod_date"));
		}

		dc.setSQL(query.toString());

		// now we need to add the params

		if (UtilMethods.isSet(searcher.getKeywords())) {
			dc.addParam("%" + searcher.getKeywords().trim().toLowerCase() + "%");
			dc.addParam("%" + searcher.getKeywords().trim().toLowerCase() + "%");
		}

		if(searcher.getDaysOld()!=-1) {
			dc.addParam(searcher.getDaysOld());
		}

		if (UtilMethods.isSet(searcher.getSchemeId())) {
			dc.addParam(searcher.getSchemeId());
		}

		if (UtilMethods.isSet(searcher.getStepId())) {
			dc.addParam(searcher.getStepId());
		}

		return dc;

	}

	@Override
	public void removeAttachedFile(WorkflowTask task, String fileInode) throws DotDataException {
		final String query = "delete from workflowtask_files where workflowtask_id = ? and file_inode = ?";
		final DotConnect dc = new DotConnect();
		dc.setSQL(query);
		dc.addParam(task.getId());
		dc.addParam(fileInode);
		dc.loadResult();

	}

	/**
	 *
	 * @param actionId
	 * @return
	 */
	public boolean existsAction (final String actionId) {

		boolean exists = false;

		try {

			exists = null != this.findAction(actionId);
		} catch (final Exception e) {
			Logger.debug(this.getClass(), e.getMessage(), e);
		}

		return exists;
	} // existsAction.

	@Override
	public void saveAction(final WorkflowAction workflowAction,
						   final WorkflowStep workflowStep)  throws DotDataException,AlreadyExistException {

		this.saveAction(workflowAction, workflowStep, 0);
	} // saveAction

	@Override
	public void saveAction(final WorkflowAction workflowAction,
						   final WorkflowStep workflowStep,
						   final int order)  throws DotDataException,AlreadyExistException {

		boolean isNew = true;
		if (UtilMethods.isSet(workflowAction.getId()) && UtilMethods.isSet(workflowStep.getId())) {
			isNew = (null == findAction(workflowAction.getId(), workflowStep.getId()));
		}
		if (isNew) {
			new DotConnect().setSQL(sql.INSERT_ACTION_FOR_STEP)
					.addParam(workflowAction.getId())
					.addParam(workflowStep.getId())
					.addParam(order)
					.loadResult();
		} else {
			new DotConnect().setSQL(sql.UPDATE_ACTION_FOR_STEP_ORDER)
					.addParam(order)
					.addParam(workflowAction.getId())
					.addParam(workflowStep.getId())
					.loadResult();
		}

		final WorkflowStep proxyStep = new WorkflowStep();
		proxyStep.setId(workflowStep.getId());
		cache.removeActions(proxyStep);

		final WorkflowScheme proxyScheme = new WorkflowScheme();
		proxyScheme.setId(workflowAction.getSchemeId());
		cache.removeActions(proxyScheme);

		// update workflowScheme mod date
		final WorkflowScheme scheme = findScheme(workflowAction.getSchemeId());
		saveScheme(scheme);
	} // saveAction.

	@Override
	public void updateOrder(final WorkflowAction workflowAction,
							final WorkflowStep workflowStep,
							final int order)  throws DotDataException,AlreadyExistException {

		new DotConnect().setSQL(sql.UPDATE_ACTION_FOR_STEP_ORDER)
				.addParam(order)
				.addParam(workflowAction.getId())
				.addParam(workflowStep.getId())
				.loadResult();

		final WorkflowStep proxyStep = new WorkflowStep();
		proxyStep.setId(workflowStep.getId());
		cache.removeActions(proxyStep);

		final WorkflowScheme proxyScheme = new WorkflowScheme();
		proxyScheme.setId(workflowAction.getSchemeId());
		cache.removeActions(proxyScheme);

		// update workflowScheme mod date
		final WorkflowScheme scheme = findScheme(workflowAction.getSchemeId());
		saveScheme(scheme);
	} // updateOrder.

	/**
	 *
	 * @param workflowAction
	 * @return
	 */
	private String getNextStep (final WorkflowAction workflowAction) {

		return (!UtilMethods.isSet(workflowAction.getNextStep()))?
				WorkflowAction.CURRENT_STEP: workflowAction.getNextStep();
	}

	@Override
	public void saveAction(final WorkflowAction action) throws DotDataException,AlreadyExistException {

		boolean isNew = true;
		if (UtilMethods.isSet(action.getId())) {

			isNew = !this.existsAction(action.getId());
		} else {
			action.setId(UUIDGenerator.generateUuid());
		}

		final String     nextStep = this.getNextStep(action);
		final DotConnect db       = new DotConnect();
		if (isNew) {
			db.setSQL(sql.INSERT_ACTION);
			db.addParam(action.getId());
			db.addParam(action.getSchemeId());
			db.addParam(action.getName());
			db.addParam(action.getCondition());
			db.addParam(nextStep);
			db.addParam(action.getNextAssign());
			db.addParam(action.getOrder());
			db.addParam(action.isAssignable());
			db.addParam(action.isCommentable());
			db.addParam(action.getIcon());
			db.addParam(action.isRoleHierarchyForAssign());
			db.addParam(action.isRequiresCheckout());
			db.addParam(WorkflowState.toCommaSeparatedString(action.getShowOn()));
			db.loadResult();
		} else {
			db.setSQL(sql.UPDATE_ACTION);
			db.addParam(action.getSchemeId());
			db.addParam(action.getName());
			db.addParam(action.getCondition());
			db.addParam(nextStep);
			db.addParam(action.getNextAssign());
			db.addParam(action.getOrder());
			db.addParam(action.isAssignable());
			db.addParam(action.isCommentable());
			db.addParam(action.getIcon());
			db.addParam(action.isRoleHierarchyForAssign());
			db.addParam(action.isRequiresCheckout());
			db.addParam(WorkflowState.toCommaSeparatedString(action.getShowOn()));
			db.addParam(action.getId());
			db.loadResult();
		}

		final List<WorkflowStep> relatedProxiesSteps =
				this.findProxiesSteps(action);
		relatedProxiesSteps.forEach( cache::removeActions );

		final WorkflowScheme proxyScheme = new WorkflowScheme();
		proxyScheme.setId(action.getSchemeId());
		cache.removeActions(proxyScheme);

		// update workflowScheme mod date
		final WorkflowScheme scheme = findScheme(action.getSchemeId());
		saveScheme(scheme);

	}

	/**
	 *
	 * @param action
	 * @return
	 * @throws DotDataException
	 */
	public List<WorkflowStep> findProxiesSteps(final WorkflowAction action) throws DotDataException {

		final ImmutableList.Builder<WorkflowStep> stepsBuilder =
				new ImmutableList.Builder<>();

		final List<Map<String, Object>> stepIdList =
				new DotConnect().setSQL(sql.SELECT_STEPS_ID_BY_ACTION)
						.addParam(action.getId()).loadObjectResults();

		if (null != stepIdList) {

			stepIdList.forEach( mapRow ->  stepsBuilder.add
					(this.buildProxyWorkflowStep((String)mapRow.get("stepid"))) );
		}

		return stepsBuilder.build();
	}

	/**
	 *
	 * @param stepId
	 * @return
	 */
	private WorkflowStep buildProxyWorkflowStep (final String stepId) {

		final WorkflowStep proxyWorkflowStep =
				new WorkflowStep();

		proxyWorkflowStep.setId(stepId);

		return proxyWorkflowStep;
	}

	@Override
	public void saveActionClass(final WorkflowActionClass actionClass) throws DotDataException,AlreadyExistException {

		boolean isNew = true;
		if (UtilMethods.isSet(actionClass.getId())) {
			try {
				final WorkflowActionClass test = this.findActionClass(actionClass.getId());
				if (test != null) {
					isNew = false;
				}
			} catch (final Exception e) {
				Logger.debug(this.getClass(), e.getMessage(), e);
			}
		} else {
			actionClass.setId(UUIDGenerator.generateUuid());
		}

		final DotConnect db = new DotConnect();
		if (isNew) {
			db.setSQL(sql.INSERT_ACTION_CLASS);
			db.addParam(actionClass.getId());
			db.addParam(actionClass.getActionId());
			db.addParam(actionClass.getName());
			db.addParam(actionClass.getOrder());
			db.addParam(actionClass.getClazz());
			db.loadResult();
		} else {
			db.setSQL(sql.UPDATE_ACTION_CLASS);
			db.addParam(actionClass.getActionId());
			db.addParam(actionClass.getName());
			db.addParam(actionClass.getOrder());
			db.addParam(actionClass.getClazz());
			db.addParam(actionClass.getId());

			db.loadResult();
		}
		// cache.remove(step);

		// update workflowScheme mod date
		final WorkflowAction action = findAction(actionClass.getActionId());
		final WorkflowScheme scheme = findScheme(action.getSchemeId());

		saveScheme(scheme);
		cache.remove(action);
	}

	@Override
	public void saveComment(WorkflowComment comment) throws DotDataException {
		if(InodeUtils.isSet(comment.getId())) {
			boolean update=false;
			try {
				HibernateUtil.load(WorkflowComment.class, comment.getId());
				// if no exception it exists. just update
				update=true;
			}
			catch(Exception ex) {
				// if it doesn't then save with primary key
				HibernateUtil.saveWithPrimaryKey(comment, comment.getId());
			}
			if(update) {
				HibernateUtil.update(comment);
			}
		}
		else {
			HibernateUtil.save(comment);
		}

	}

	@Override
	public void saveScheme(WorkflowScheme scheme) throws DotDataException, AlreadyExistException {

		boolean isNew = true;
		if (UtilMethods.isSet(scheme.getId())) {
			try {
				final WorkflowScheme test = this.findScheme(scheme.getId());
				if (test != null) {
					isNew = false;
				}
			} catch (final Exception e) {
				Logger.debug(this.getClass(), e.getMessage(), e);
			}
		} else {
			scheme.setId(UUIDGenerator.generateUuid());
		}

		scheme.setModDate(new Date());

		final DotConnect db = new DotConnect();
		try {

			if (isNew) {

				db.setSQL(sql.INSERT_SCHEME);
				db.addParam(scheme.getId());
				db.addParam(scheme.getName());
				db.addParam(scheme.getDescription());
				db.addParam(scheme.isArchived());
				db.addParam(false);
				db.addParam(scheme.isDefaultScheme());
				db.addParam(scheme.getModDate());
				db.loadResult();
			} else {
				db.setSQL(sql.UPDATE_SCHEME);
				db.addParam(scheme.getName());
				db.addParam(scheme.getDescription());
				db.addParam(scheme.isArchived());
				db.addParam(false);
				db.addParam(scheme.getModDate());
				db.addParam(scheme.getId());
				db.loadResult();

			}
			cache.remove(scheme);
		} catch (final Exception e) {
			throw new DotDataException(e.getMessage(),e);
		}
	}

	public void forceDeleteSchemeForContentType(final String contentTypeId) throws DotDataException {

		try {

			Logger.info(this, "Deleting the schemes associated to the content type: " + contentTypeId);

			new DotConnect().setSQL(sql.DELETE_SCHEME_FOR_STRUCT)
				.addParam(contentTypeId).loadResult();

			cache.removeStructure(contentTypeId);
		} catch (final Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotDataException(e.getMessage(),e);
		}
	}

	@Override
	public void deleteSchemeForStruct(final String struc) throws DotDataException {
		if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
			return;
		}

		this.forceDeleteSchemeForContentType(struc);
	}

	@Override
	public void saveSchemeIdsForContentType(final String contentTypeInode,
											final List<String> schemesIds,
											final Consumer<WorkflowTask> workflowTaskConsumer) throws DotDataException {

		if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {

			return;
		}

		try {

			final DotConnect db = new DotConnect();
			db.setSQL(sql.DELETE_SCHEME_FOR_STRUCT);
			db.addParam(contentTypeInode);
			db.loadResult();

			final ImmutableList.Builder<WorkflowStep> stepBuilder = new ImmutableList.Builder<>();
			for(String id : schemesIds) {
				db.setSQL(sql.INSERT_SCHEME_FOR_STRUCT);
				db.addParam(UUIDGenerator.generateUuid());
				db.addParam(id);
				db.addParam(contentTypeInode);
				db.loadResult();

				stepBuilder.addAll(this.findSteps(this.findScheme(id)));
			}
			// update all tasks for the content type and reset their step to
			// null
			this.cleanWorkflowTaskStatus(contentTypeInode, stepBuilder.build(), workflowTaskConsumer);
			this.checkContentTypeWorkflowTaskNullStatus(contentTypeInode, workflowTaskConsumer);

			// we have to clear the saved steps/tasks for all contentlets using
			// this workflow

			cache.removeStructure(contentTypeInode);
			cache.clearStepsCache();
		} catch (final Exception e) {

			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotDataException(e.getMessage(),e);
		}
	}

	private void checkContentTypeWorkflowTaskNullStatus(final String contentTypeInode,
														final Consumer<WorkflowTask> workflowTaskConsumer) throws DotDataException {

		try {

			final List<WorkflowTask> tasks = this
					.convertListToObjects(new DotConnect()
							.setSQL(sql.SELECT_TASK_NULL_BY_STRUCT)
							.addParam(contentTypeInode).loadObjectResults(), WorkflowTask.class);

			//clean cache
			tasks.stream().forEach(task -> {

				if (null != workflowTaskConsumer) {

					workflowTaskConsumer.accept(task);
				}
			});
		} catch (final Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
	}

	/**
	 * Set workflow tasks with status null, for all the existing workflow task with the specified
	 * contentTypeInode and not in the list of steps availables fot the content type schemes
	 *
	 * @param contentTypeInode Content Type Inode {@link String}
	 * @param steps List of valid Steps {@link List}
	 * @param workflowTaskConsumer {@link Consumer}
	 */
	private void cleanWorkflowTaskStatus(final String contentTypeInode,
										 final List<WorkflowStep> steps,
										 final Consumer<WorkflowTask> workflowTaskConsumer)
			throws DotDataException {
		try {

			String condition = "";
			if (steps.size() > 0) {
				condition = " and status not in (";
				StringBuilder parameters = new StringBuilder();
				for (WorkflowStep step : steps) {
					parameters.append(", ?");
				}
				condition += parameters.toString().substring(1) + " )";
			}

			final DotConnect db = new DotConnect();
			db.setSQL(sql.SELECT_TASK_STEPS_TO_CLEAN_BY_STRUCT + condition);
			db.addParam(contentTypeInode);
			if (steps.size() > 0) {
				for (WorkflowStep step : steps) {
					db.addParam(step.getId());
				}
			}
			final List<WorkflowTask> tasks = this
					.convertListToObjects(db.loadObjectResults(), WorkflowTask.class);

			//clean cache
			tasks.stream().forEach(task -> {

				cache.remove(task);
				if (null != workflowTaskConsumer) {

					workflowTaskConsumer.accept(task);
				}
			});

			db.setSQL(sql.UPDATE_STEPS_BY_STRUCT + condition);
			db.addParam((Object) null);
			db.addParam(contentTypeInode);
			if (steps.size() > 0) {
				for (WorkflowStep step : steps) {
					db.addParam(step.getId());
				}
			}
			db.loadResult();

		} catch (final Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@Override
	public void saveSchemesForStruct(final String contentTypeInode,
									 final List<WorkflowScheme> schemes,
									 final Consumer<WorkflowTask> workflowTaskConsumer) throws DotDataException {

		final List<String> ids = schemes.stream()
				.map(scheme -> scheme.getId())
				.collect(Collectors.toList());

		this.saveSchemeIdsForContentType(contentTypeInode, ids, workflowTaskConsumer);
	}

	@Override
	public void saveStep(WorkflowStep step) throws DotDataException, AlreadyExistException {

		boolean isNew = true;
		if (UtilMethods.isSet(step.getId())) {
			try {
				final WorkflowStep test = this.findStep(step.getId());
				if (test != null) {
					isNew = false;
				}
			} catch (final Exception e) {
				Logger.debug(this.getClass(), e.getMessage(), e);
			}
		} else {
			step.setId(UUIDGenerator.generateUuid());
		}

		final DotConnect db = new DotConnect();
		if (isNew) {

			db.setSQL(sql.INSERT_STEP);
			db.addParam(step.getId());
			db.addParam(step.getName());
			db.addParam(step.getSchemeId());
			db.addParam(step.getMyOrder());
			db.addParam(step.isResolved());
			db.addParam(step.isEnableEscalation());
			if(step.isEnableEscalation()) {
				db.addParam(step.getEscalationAction());
				db.addParam(step.getEscalationTime());
			}
			else {
				db.addParam((Object)null);
				db.addParam(0);
			}
			db.loadResult();
		} else {
			db.setSQL(sql.UPDATE_STEP);
			db.addParam(step.getName());
			db.addParam(step.getSchemeId());
			db.addParam(step.getMyOrder());
			db.addParam(step.isResolved());
			db.addParam(step.isEnableEscalation());
			if(step.isEnableEscalation()) {
				db.addParam(step.getEscalationAction());
				db.addParam(step.getEscalationTime());
			}
			else {
				db.addParam((Object)null);
				db.addParam(0);
			}
			db.addParam(step.getId());
			db.loadResult();
		}
		cache.remove(step);

		// update workflowScheme mod date
		WorkflowScheme scheme = findScheme(step.getSchemeId());
		saveScheme(scheme);

	}

	@Override
	public void saveWorkflowActionClassParameter(WorkflowActionClassParameter param) throws DotDataException, AlreadyExistException {

		boolean isNew = true;
		if (UtilMethods.isSet(param.getId())) {
			try {
				final WorkflowActionClassParameter test = this.findActionClassParameter(param.getId());
				if (test != null) {
					isNew = false;
				}
			} catch (final Exception e) {
				Logger.debug(this.getClass(), e.getMessage(), e);
			}
		} else {
			param.setId(UUIDGenerator.generateUuid());
		}

		final DotConnect db = new DotConnect();
		if (isNew) {
			db.setSQL(sql.INSERT_ACTION_CLASS_PARAM);
			db.addParam(param.getId());
			db.addParam(param.getActionClassId());
			db.addParam(param.getKey());
			db.addParam(param.getValue());
			db.loadResult();
		} else {
			db.setSQL(sql.UPDATE_ACTION_CLASS_PARAM);

			db.addParam(param.getActionClassId());
			db.addParam(param.getKey());
			db.addParam(param.getValue());
			db.addParam(param.getId());

			db.loadResult();
		}

		// update workflowScheme mod date
		final WorkflowActionClass actionClass = findActionClass(param.getActionClassId());
		final WorkflowAction action = findAction(actionClass.getActionId());
		final WorkflowScheme scheme = findScheme(action.getSchemeId());
		saveScheme(scheme);
	}

	@Override
	public void saveWorkflowHistory(WorkflowHistory history) throws DotDataException {
		if(InodeUtils.isSet(history.getId())) {
			boolean update=false;
			try {
				HibernateUtil.load(WorkflowHistory.class, history.getId());
				// if exists just update
				update=true;
			}
			catch(Exception ex) {
				// if not then save with existing key
				HibernateUtil.saveWithPrimaryKey(history, history.getId());	            
			}
			if(update) {
				HibernateUtil.update(history);
			}
		}
		else {
			HibernateUtil.save(history);
		}
	}

	@Override
	public void saveWorkflowTask ( WorkflowTask task ) throws DotDataException {

		if ( task.isNew() ) {
			HibernateUtil.save( task );
		} else {

			try {
				Object currentWorkflowTask = HibernateUtil.load( WorkflowTask.class, task.getId() );
				HibernateUtil.evict( currentWorkflowTask );//Remove the object from hibernate cache, we used just to verify if exist

				// if the object exists no exception is thrown so just update it

				HibernateUtil.update( task );
			} catch ( Exception ex ) {
				// if it doesn't exists then save with that primary key
				HibernateUtil.saveWithPrimaryKey( task, task.getId() );
			}
		}

		cache.remove( task );
	}

	@Override
	public List<WorkflowTask> searchTasks(WorkflowSearcher searcher) throws DotDataException {
		DotConnect dc = getWorkflowSqlQuery(searcher, false);
		dc.setStartRow(searcher.getCount() * searcher.getPage());
		dc.setMaxRows(searcher.getCount());
		List<Map<String,Object>> results = dc.loadObjectResults();
		List<WorkflowTask> wfTasks = new ArrayList<WorkflowTask>();

		for (Map<String, Object> row : results) {
			WorkflowTask wt = new WorkflowTask();
			wt.setId(getStringValue(row, "id"));
			wt.setCreationDate((Date)row.get("creation_date"));
			wt.setModDate((Date)row.get("mod_date"));
			wt.setDueDate((Date)row.get("due_date"));
			wt.setCreatedBy(getStringValue(row, "created_by"));
			wt.setAssignedTo(getStringValue(row, "assigned_to"));
			wt.setBelongsTo(getStringValue(row, "belongs_to"));
			wt.setTitle(getStringValue(row, "title"));
			wt.setDescription(getStringValue(row, "description"));
			wt.setStatus(getStringValue(row, "status"));
			wt.setWebasset(getStringValue(row, "webasset"));
			wt.setLanguageId(getLongValue(row, "language_id"));
			wfTasks.add(wt);
		}

		return wfTasks;
	}

	/**
	 *
	 * @param row
	 * @param key
	 * @return
	 */
	private String getStringValue(Map<String, Object> row, String key) {
		Object value = row.get(key);
		return (value == null) ? "" : value.toString();
	}

	/**
	 *
	 * @param row
	 * @param key
	 * @return
	 */
	private Long getLongValue(final Map<String, Object> row, final String key) {
		return ConversionUtils.toLong(row.get(key), 0L);
	}

	@Override
	public List<WorkflowTask> searchAllTasks(WorkflowSearcher searcher) throws DotDataException {

		final HibernateUtil hu = new HibernateUtil(WorkflowTask.class);
		final StringWriter sw = new StringWriter();
		sw.append("select {workflow_task.*}  from workflow_task   ");
		hu.setSQLQuery(sw.toString());
		if (searcher != null) {
			hu.setMaxResults(searcher.getCount());
			hu.setFirstResult(searcher.getCount() * searcher.getPage());
		}

		return (List<WorkflowTask>) hu.list();

	}

	@Override
	public WorkflowHistory retrieveLastStepAction(String taskId) throws DotDataException {

		final DotConnect db = new DotConnect();
		try {

			db.setSQL(sql.RETRIEVE_LAST_STEP_ACTIONID);
			db.addParam(taskId);
			db.loadResult();
		} catch (final Exception e) {
			Logger.debug(this.getClass(), e.getMessage(), e);
		}

		return (WorkflowHistory) this.convertListToObjects(db.loadObjectResults(), WorkflowHistory.class).get(0);

	}

	@Override
	public List<WorkflowTask> findExpiredTasks() throws DotDataException, DotSecurityException {
		final DotConnect db = new DotConnect();
		List<WorkflowTask> list=new ArrayList<WorkflowTask>();
		try {
			db.setSQL(sql.SELECT_EXPIRED_TASKS);
			List<Map<String,Object>> results=db.loadResults();
			for (Map<String, Object> map : results) {
				String taskId=(String)map.get("id");
				WorkflowTask task=findWorkFlowTaskById(taskId);
				list.add(task);
			}
		} catch (final Exception e) {
			Logger.error(this, e.getMessage(), e);
		}
		finally {
			HibernateUtil.getSession().clear();
		}
		return list;
	}

	@Override
	public WorkflowScheme findSchemeByName(String schemaName) throws DotDataException {
		WorkflowScheme scheme = null;
		try {
			final DotConnect db = new DotConnect();
			db.setSQL(sql.SELECT_SCHEME_NAME);
			db.addParam((schemaName != null ? schemaName.trim() : ""));
			List<WorkflowScheme> list = this.convertListToObjects(db.loadObjectResults(), WorkflowScheme.class);
			scheme = list.size()>0 ? (WorkflowScheme)list.get(0) : null;
		} catch (final Exception e) {
			throw new DotDataException(e.getMessage(),e);
		}
		return scheme;
	}

	@Override
	public void deleteWorkflowActionClassParameter(WorkflowActionClassParameter param) throws DotDataException, AlreadyExistException {
		DotConnect db=new DotConnect();
		db.setSQL(sql.DELETE_ACTION_CLASS_PARAM_BY_ID);
		db.addParam(param.getId());
		db.loadResult();

		// update scheme mod date
		WorkflowActionClass clazz = findActionClass(param.getActionClassId());
		WorkflowAction action = findAction(clazz.getActionId());
		WorkflowScheme scheme = findScheme(action.getSchemeId());
		saveScheme(scheme);
	}

	@Override
	public void updateUserReferences(String userId, String userRoleId, String replacementUserId, String replacementUserRoleId)throws DotDataException, DotSecurityException{
		DotConnect dc = new DotConnect();

		try {
			dc.setSQL("select id from workflow_task where (assigned_to = ? or assigned_to=? or created_by=? or created_by=?)");
			dc.addParam(userId);
			dc.addParam(userRoleId);
			dc.addParam(userId);
			dc.addParam(userRoleId);
			List<HashMap<String, String>> tasks = dc.loadResults();

			dc.setSQL("update workflow_comment set posted_by=? where posted_by  = ?");
			dc.addParam(replacementUserId);
			dc.addParam(userId);
			dc.loadResults();

			dc.setSQL("update workflow_comment set posted_by=? where posted_by  = ?");
			dc.addParam(replacementUserRoleId);
			dc.addParam(userRoleId);
			dc.loadResults();

			dc.setSQL("update workflow_task set assigned_to=? where assigned_to  = ?");
			dc.addParam(replacementUserRoleId);
			dc.addParam(userRoleId);
			dc.loadResult();

			dc.setSQL("update workflow_task set created_by=? where created_by  = ?");
			dc.addParam(replacementUserId);
			dc.addParam(userId);
			dc.loadResult();

			dc.setSQL("update workflow_task set created_by=? where created_by  = ?");
			dc.addParam(replacementUserRoleId);
			dc.addParam(userRoleId);
			dc.loadResult();

			dc.setSQL("update workflow_action set next_assign=? where next_assign = ?");
			dc.addParam(replacementUserRoleId);
			dc.addParam(userRoleId);
			dc.loadResult();

			for(HashMap<String, String> val : tasks){
				String id = val.get("id");
				WorkflowTask task = findWorkFlowTaskById(id);
				cache.remove(task);
				
				dc.setSQL("select workflow_step.id from workflow_step join workflow_task on workflow_task.status = workflow_step.id where workflow_task.webasset= ?");
				dc.addParam(task.getWebasset());
				List<HashMap<String, String>> steps = dc.loadResults();
				for(HashMap<String, String> v : steps){
					String stepId = v.get("id");
					WorkflowStep step = findStep(stepId);
					cache.remove(step);					
				}
			}
		} catch (DotDataException e) {
			Logger.error(WorkFlowFactory.class,e.getMessage(),e);
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@Override
	public void updateStepReferences(String stepId, String replacementStepId) throws DotDataException, DotSecurityException {
		DotConnect dc = new DotConnect();

		try {
			// Replace references and clear cache for workflow actions
			dc.setSQL("select step_id from workflow_action where next_step_id = ?");
			dc.addParam(stepId);
			List<HashMap<String, String>> actionStepIds = dc.loadResults();

			if (replacementStepId != null){
				dc.setSQL("update workflow_action set next_step_id = ? where next_step_id = ?");
				dc.addParam(replacementStepId);
				dc.addParam(stepId);
				dc.loadResult();

			} else {
				dc.setSQL("update workflow_action set next_step_id = step_id where next_step_id = ?");
				dc.addParam(stepId);
				dc.loadResult();
			}

			for(HashMap<String, String> v : actionStepIds){
				String id = v.get("step_id");
				WorkflowStep step = findStep(id);
				cache.remove(step);					
			}

			
			// Replace references and clear cache for workflow tasks
			dc.setSQL("select id from workflow_task where status = ?");
			dc.addParam(stepId);
			List<HashMap<String, String>> taskIds = dc.loadResults();

			dc.setSQL("update workflow_task set status = ? where status = ?");
			dc.addParam(replacementStepId);
			dc.addParam(stepId);
			dc.loadResults();

			for(HashMap<String, String> val : taskIds){
				String id = val.get("id");
				WorkflowTask task = findWorkFlowTaskById(id);
				cache.remove(task);
			}

		} catch (DotDataException e) {
			Logger.error(WorkFlowFactory.class,e.getMessage(),e);
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@Override
	public List<WorkflowTask> findTasksByStep(final String stepId) throws DotDataException, DotSecurityException {
		List<WorkflowTask> tasks;
		DotConnect dc = new DotConnect();

		try {
			dc.setSQL(sql.SELECT_TASKS_BY_STEP);
			dc.addParam(stepId);
			tasks =  this.convertListToObjects(dc.loadObjectResults(), WorkflowTask.class);
		} catch (DotDataException e) {
			Logger.error(WorkFlowFactory.class,e.getMessage(),e);
			throw new DotDataException(e.getMessage(), e);
		}
		return tasks;
	}

	@Override
	public List<ContentType> findContentTypesByScheme(final WorkflowScheme scheme) throws DotDataException, DotSecurityException{
		List<ContentType> contentTypes;
		DotConnect dc = new DotConnect();
		try {
			dc.setSQL(sql.SELECT_STRUCTS_FOR_SCHEME);
			dc.addParam(scheme.getId());
			contentTypes =  this.convertListToObjects(dc.loadObjectResults(), ContentType.class);

		} catch (DotDataException e) {
			Logger.error(WorkFlowFactory.class,e.getMessage(),e);
			throw new DotDataException(e.getMessage(), e);
		}
		return contentTypes;
	}

	@Override
	public void deleteScheme(final WorkflowScheme scheme) throws DotDataException, DotSecurityException{
		DotConnect dc = new DotConnect();
		try {
			//delete association of content types with the scheme
			dc.setSQL(sql.DELETE_STRUCTS_FOR_SCHEME);
			dc.addParam(scheme.getId());
			dc.loadResult();

			//delete the scheme
			dc.setSQL(sql.DELETE_SCHEME);
			dc.addParam(scheme.getId());
			dc.loadResult();

			final List<WorkflowAction> actions =
					this.cache.getActions(scheme);
			if (null != actions) {
				actions.stream().forEach(action -> this.cache.remove(action));
			}
			this.cache.remove(scheme);
		} catch (DotDataException e) {
			Logger.error(WorkFlowFactory.class,e.getMessage(),e);
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@Override
	public Set<String> findNullTaskContentletIdentifiersForScheme(final String workflowSchemeId) throws DotDataException {
		final DotConnect dc = new DotConnect();
		try {
			dc.setSQL(sql.SELECT_NULL_TASK_CONTENTLET_FOR_WORKFLOW);
			dc.addParam(workflowSchemeId);
			final List<Map<String, String>> result = dc.loadResults();
            return result.stream().map(row -> row.get("identifier")).collect(Collectors.toSet());
		} catch (DotDataException e) {
			Logger.error(WorkFlowFactory.class,e.getMessage(),e);
			throw new DotDataException(e.getMessage(), e);
		}
	}
}
