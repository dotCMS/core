package com.dotmarketing.portlets.workflows.business;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.workflows.model.WorkFlowTaskFiles;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowSearcher;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class WorkflowFactoryImpl implements WorkFlowFactory {

	private static WorkflowCache cache = null;
	private static WorkflowSQL sql = null;

	public WorkflowFactoryImpl() {
		sql = WorkflowSQL.getInstance();
		cache = CacheLocator.getWorkFlowCache();
	}

	public void attachFileToTask(WorkflowTask task, String fileInode) throws DotDataException {
		final WorkFlowTaskFiles taskFile = new WorkFlowTaskFiles();
		taskFile.setWorkflowtaskId(task.getId());
		taskFile.setFileInode(fileInode);
		HibernateUtil.save(taskFile);
	}

	private Object convert(Object obj, Map<String, Object> map) throws IllegalAccessException, InvocationTargetException {
		BeanUtils.copyProperties(obj, map);
		return obj;
	}

	private WorkflowAction convertAction(Map<String, Object> row) throws IllegalAccessException, InvocationTargetException {
		final WorkflowAction action = new WorkflowAction();
		row.put("stepId", row.get("step_id"));
		row.put("condition", row.get("condition_to_progress"));
		row.put("nextStep", row.get("next_step_id"));
		row.put("nextAssign", row.get("next_assign"));
		row.put("order", row.get("my_order"));
		row.put("requiresCheckout", row.get("requires_checkout"));
		row.put("roleHierarchyForAssign", row.get("use_role_hierarchy_assign"));

		BeanUtils.copyProperties(action, row);
		return action;
	}

	private WorkflowActionClass convertActionClass(Map<String, Object> row) throws IllegalAccessException, InvocationTargetException {
		final WorkflowActionClass actionClass = new WorkflowActionClass();

		row.put("clazz", row.get("clazz"));

		row.put("myOrder", row.get("my_order"));
		row.put("actionId", row.get("action_id"));
		BeanUtils.copyProperties(actionClass, row);
		return actionClass;
	}

	private WorkflowActionClassParameter convertActionClassParameter(Map<String, Object> row) throws IllegalAccessException, InvocationTargetException {
		final WorkflowActionClassParameter param = new WorkflowActionClassParameter();
		row.put("actionClassId", row.get("workflow_action_class_id"));
		BeanUtils.copyProperties(param, row);
		return param;
	}

	private List convertListToObjects(List<Map<String, Object>> rs, Class clazz) throws DotDataException {
		final ObjectMapper m = new ObjectMapper();

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

	private Object convertMaptoObject(Map<String, Object> map, Class clazz) throws InstantiationException, IllegalAccessException, InvocationTargetException {

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
		} else {
			return this.convert(obj, map);
		}
	}

	private WorkflowScheme convertScheme(Map<String, Object> row) throws IllegalAccessException, InvocationTargetException {
		final WorkflowScheme scheme = new WorkflowScheme();
		row.put("entryActionId", row.get("entry_action_id"));
		row.put("defaultScheme", row.get("default_scheme"));
		row.put("modDate", row.get("mod_date"));

		BeanUtils.copyProperties(scheme, row);

		return scheme;
	}

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

	 private WorkflowHistory convertHistory(Map<String, Object> row) throws IllegalAccessException, InvocationTargetException {
			final WorkflowHistory scheme = new WorkflowHistory();
			row.put("actionId", row.get("workflow_action_id"));

			BeanUtils.copyProperties(scheme, row);

			return scheme;
		    }

	public void copyWorkflowAction(WorkflowAction from, WorkflowStep step) throws DotDataException {
		throw new DotWorkflowException("Not implemented");
	}

	public void copyWorkflowActionClass(WorkflowActionClass from, WorkflowAction action) throws DotDataException {
		throw new DotWorkflowException("Not implemented");
	}

	public void copyWorkflowActionClassParameter(WorkflowActionClassParameter from, WorkflowActionClass actionClass) throws DotDataException {
		throw new DotWorkflowException("Not implemented");
	}

	public void copyWorkflowStep(WorkflowStep from, WorkflowScheme scheme) throws DotDataException {
		throw new DotWorkflowException("Not implemented");
	}

	public int countTasks(WorkflowSearcher searcher) throws DotDataException {
		DotConnect dc = getWorkflowSqlQuery(searcher, true);
		return dc.getInt("mycount");
	}

	public WorkflowScheme createDefaultScheme() throws DotDataException, DotSecurityException {

		WorkflowScheme scheme = cache.getDefaultScheme();
		if (scheme == null) {
			synchronized ("schemeDefault") {
				Logger.info(this.getClass(), "Creating Default Workflow Schema");
				scheme = cache.getDefaultScheme();
				if (scheme != null) {
					return scheme;
				}
				final DotConnect db = new DotConnect();
				db.setSQL(sql.SELECT_DEFAULT_SCHEME);
				final List<Map<String, Object>> results = db.loadResults();
				if (results.size() > 0) {
					try {
						return (WorkflowScheme) this.convertMaptoObject(results.get(0), WorkflowScheme.class);
					} catch (final Exception e) {
						throw new DotDataException(e.getMessage());
					}
				}

				HibernateUtil.startTransaction();
				try {

					scheme = new WorkflowScheme();
					scheme.setId(UUIDGenerator.generateUuid());
					scheme.setArchived(false);
					scheme.setCreationDate(new Date());
					scheme.setMandatory(false);
					scheme.setName("Default Scheme");
					scheme.setDescription("This is the default workflow scheme that will be applied to all content");
					scheme.setDefaultScheme(true);

					db.setSQL(sql.INSERT_SCHEME);
					db.addParam(scheme.getId());
					db.addParam(scheme.getName());
					db.addParam(scheme.getDescription());
					db.addParam(scheme.isArchived());
					db.addParam(scheme.isMandatory());
					db.addParam((String) null);
					db.addParam(scheme.isDefaultScheme());
					db.loadResult();

					db.setSQL(sql.UPDATE_SCHEME_SET_TO_DEFAULT);
					db.addParam(scheme.getId());
					db.loadResult();

					final WorkflowStep initial = new WorkflowStep();
					initial.setName("Initial State");
					initial.setResolved(true);
					initial.setMyOrder(1);
					initial.setSchemeId(scheme.getId());
					this.saveStep(initial);

					final WorkflowStep second = new WorkflowStep();
					second.setName("Content Entry");
					second.setResolved(false);
					second.setMyOrder(2);
					second.setSchemeId(scheme.getId());
					this.saveStep(second);

					final WorkflowStep third = new WorkflowStep();
					third.setName("Closed");
					third.setResolved(true);
					third.setMyOrder(3);
					third.setSchemeId(scheme.getId());
					this.saveStep(third);

					final List<WorkflowAction> actions = new ArrayList<WorkflowAction>();

					WorkflowAction action = new WorkflowAction();
					action.setName("Assign Workflow");
					action.setAssignable(true);
					action.setCommentable(true);
					action.setIcon("workflowIcon");
					action.setOrder(1);
					action.setStepId(initial.getId());
					action.setNextAssign(APILocator.getRoleAPI().loadCMSAdminRole().getId());
					action.setNextStep(second.getId());
					this.saveAction(action);
					actions.add(action);

					action = new WorkflowAction();
					action.setName("Reassign Workflow");
					action.setAssignable(true);
					action.setCommentable(true);
					action.setIcon("cancelIcon");
					action.setOrder(1);
					action.setStepId(second.getId());
					action.setNextAssign(APILocator.getRoleAPI().loadCMSAdminRole().getId());
					action.setNextStep(second.getId());
					this.saveAction(action);
					actions.add(action);

					action = new WorkflowAction();
					action.setName("Resolve Workflow");
					action.setAssignable(false);
					action.setCommentable(false);
					action.setIcon("closeIcon");
					action.setOrder(2);
					action.setStepId(second.getId());
					action.setNextAssign(APILocator.getRoleAPI().loadCMSAdminRole().getId());
					action.setNextStep(third.getId());
					this.saveAction(action);
					actions.add(action);

					action = new WorkflowAction();
					action.setName("Reopen");
					action.setAssignable(true);
					action.setCommentable(true);
					action.setIcon("workflowIcon");
					action.setOrder(1);
					action.setStepId(third.getId());
					action.setNextAssign(APILocator.getRoleAPI().loadCMSAdminRole().getId());
					action.setNextStep(second.getId());
					this.saveAction(action);
					actions.add(action);

					for (final WorkflowAction a : actions) {
						final Permission p = new Permission();
						p.setPermission(PermissionAPI.PERMISSION_USE);
						p.setRoleId(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
						p.setType(a.getPermissionType());
						p.setInode(a.getId());
						APILocator.getPermissionAPI().save(p, a, APILocator.getUserAPI().getSystemUser(), false);
					}
					HibernateUtil.commitTransaction();
					cache.addDefaultScheme(scheme);
				} catch (final Exception e) {
					HibernateUtil.rollbackTransaction();
					Logger.warn(this, e.getMessage(), e);
					return null;
				}

			}
		}
		return scheme;

	}

	public void deleteAction(WorkflowAction action) throws DotDataException {
		String stepId = action.getStepId();
		final DotConnect db = new DotConnect();
		db.setSQL(sql.DELETE_ACTION);
		db.addParam(action.getId());
		db.loadResult();
		WorkflowStep proxy = new WorkflowStep();
		proxy.setId(action.getStepId());
		cache.removeActions(proxy);

		// update scheme mod date
		WorkflowStep step = findStep(stepId);
		WorkflowScheme scheme = findScheme(step.getSchemeId());
		saveScheme(scheme);
	}

	public void deleteActionClass(WorkflowActionClass actionClass) throws DotDataException {
		String actionId = actionClass.getActionId();
		final DotConnect db = new DotConnect();
		db.setSQL(sql.DELETE_ACTION_CLASS_PARAM_BY_ACTION_CLASS);
		db.addParam(actionClass.getId());
		db.loadResult();

		db.setSQL(sql.DELETE_ACTION_CLASS);
		db.addParam(actionClass.getId());
		db.loadResult();
		

		// update scheme mod date
		WorkflowAction action = findAction(actionId);
		WorkflowStep step = findStep(action.getStepId());
		WorkflowScheme scheme = findScheme(step.getSchemeId());
		saveScheme(scheme);
	}

	public void deleteActionClassByAction(WorkflowAction action) throws DotDataException, DotSecurityException {
		String actionId = action.getId();
		final DotConnect db = new DotConnect();
		db.setSQL(sql.DELETE_ACTION_CLASS_BY_ACTION);
		db.addParam(action.getId());
		
		// update scheme mod date
		WorkflowStep step = findStep(actionId);
		WorkflowScheme scheme = findScheme(step.getSchemeId());
		saveScheme(scheme);
	}

	public void deleteComment(WorkflowComment comment) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL("delete from workflow_comment where id = ?");
		db.addParam(comment.getId());
		db.loadResult();
	}

	public void deleteStep(WorkflowStep step) throws DotDataException {
		String schemeId = step.getSchemeId();
		final DotConnect db = new DotConnect();
		db.setSQL(sql.DELETE_STEP);
		db.addParam(step.getId());
		db.loadResult();
		cache.remove(step);
		
		// update scheme mod date
		WorkflowScheme scheme = findScheme(schemeId);
		saveScheme(scheme);
	}

	public void deleteWorkflowActionClassParameters(WorkflowActionClass actionClass) throws DotDataException {
		String actionClassId = actionClass.getId();
		final DotConnect db = new DotConnect();
		db.setSQL(sql.DELETE_ACTION_CLASS_PARAM_BY_ACTION_CLASS);
		db.addParam(actionClass.getId());
		db.loadResult();
		
		// update scheme mod date
		WorkflowAction action = findAction(actionClassId);
		WorkflowStep step = findStep(action.getStepId());
		WorkflowScheme scheme = findScheme(step.getSchemeId());
		saveScheme(scheme);

	}

	public void deleteWorkflowHistory(WorkflowHistory history) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL("delete from workflow_history where id = ?");
		db.addParam(history.getId());
		db.loadResult();
	}

	public void deleteWorkflowTask(WorkflowTask task) throws DotDataException {
		final DotConnect db = new DotConnect();

		Contentlet c = new Contentlet();
		c.setIdentifier(task.getWebasset());

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

		} catch (final Exception e) {
			if(localTransaction){
				HibernateUtil.rollbackTransaction();
			}
			Logger.error(this, "deleteWorkflowTask failed:" + e, e);
			throw new DotDataException(e.toString());
		}
		finally{
			if(localTransaction){
				HibernateUtil.commitTransaction();
			}
			cache.remove(c);
		}
	}

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

	public WorkflowActionClass findActionClass(String id) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.SELECT_ACTION_CLASS);
		db.addParam(id);
		return (WorkflowActionClass) this.convertListToObjects(db.loadObjectResults(), WorkflowActionClass.class).get(0);
	}

	public List<WorkflowActionClass> findActionClasses(WorkflowAction action) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.SELECT_ACTION_CLASSES_BY_ACTION);
		db.addParam(action.getId());
		return this.convertListToObjects(db.loadObjectResults(), WorkflowActionClass.class);
	}

	public WorkflowActionClassParameter findActionClassParameter(String id) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.SELECT_ACTION_CLASS_PARAM);
		db.addParam(id);
		return (WorkflowActionClassParameter) this.convertListToObjects(db.loadObjectResults(), WorkflowActionClassParameter.class).get(0);
	}

	public List<WorkflowAction> findActions(WorkflowStep step) throws DotDataException {

		List<WorkflowAction> actions = cache.getActions(step);
		if(actions ==null){
			final DotConnect db = new DotConnect();
			db.setSQL(sql.SELECT_ACTIONS_BY_STEP);
			db.addParam(step.getId());
			actions =  this.convertListToObjects(db.loadObjectResults(), WorkflowAction.class);
			if(actions == null) actions= new ArrayList<WorkflowAction>();

			cache.addActions(step, actions);
		}
		return actions;
	}

	public WorkflowScheme findDefaultScheme() throws DotDataException {
		WorkflowScheme scheme = cache.getDefaultScheme();
		if (scheme == null) {
			try {
				final DotConnect db = new DotConnect();
				db.setSQL(sql.SELECT_DEFAULT_SCHEME);
				try {
					scheme = (WorkflowScheme) this.convertListToObjects(db.loadObjectResults(), WorkflowScheme.class).get(0);
				} catch (final Exception ex) {

					throw new DotDataException("default scheme does not exist");
				}

				cache.addDefaultScheme(scheme);
			} catch (final Exception e) {
				throw new DotDataException(e.getMessage());
			}
		}
		return scheme;
	}

	public Map<String, WorkflowActionClassParameter> findParamsForActionClass(WorkflowActionClass actionClass) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.SELECT_ACTION_CLASS_PARAMS_BY_ACTIONCLASS);
		db.addParam(actionClass.getId());
		final List<WorkflowActionClassParameter> list = (List<WorkflowActionClassParameter>) this.convertListToObjects(db.loadObjectResults(), WorkflowActionClassParameter.class);
		final Map<String, WorkflowActionClassParameter> map = new LinkedHashMap();
		for (final WorkflowActionClassParameter param : list) {
			map.put(param.getKey(), param);
		}

		return map;

	}

	public WorkflowScheme findScheme(String id) throws DotDataException {
		WorkflowScheme scheme = cache.getScheme(id);
		if (scheme == null) {
			try {
				final DotConnect db = new DotConnect();
				db.setSQL(sql.SELECT_SCHEME);
				db.addParam(id);
				scheme = (WorkflowScheme) this.convertListToObjects(db.loadObjectResults(), WorkflowScheme.class).get(0);
				cache.add(scheme);
			} catch (final Exception e) {
				throw new DotDataException(e.getMessage());
			}
		}
		return scheme;
	}

	public WorkflowScheme findSchemeForStruct(String structId) throws DotDataException {

		if (LicenseUtil.getLevel() < 200) {
			return this.findDefaultScheme();
		}

		WorkflowScheme scheme = null;
		scheme = cache.getSchemeByStruct(structId);

		if (scheme != null) {
			return scheme;
		}

		final DotConnect db = new DotConnect();
		db.setSQL(sql.SELECT_SCHEME_BY_STRUCT);
		db.addParam(structId);
		try {
			scheme = (WorkflowScheme) this.convertListToObjects(db.loadObjectResults(), WorkflowScheme.class).get(0);
		} catch (final Exception er) {

			scheme = this.findDefaultScheme();

		}

		cache.addForStructure(structId, scheme);
		return scheme;

	}

	public List<WorkflowScheme> findSchemes(boolean showArchived) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.SELECT_SCHEMES);
		db.addParam(false);
		db.addParam(showArchived);
		return this.convertListToObjects(db.loadObjectResults(), WorkflowScheme.class);
	}

	public WorkflowStep findStep(String id) throws DotDataException {
		WorkflowStep step = cache.getStep(id);
		if (step == null) {
			final DotConnect db = new DotConnect();
			db.setSQL(sql.SELECT_STEP);
			db.addParam(id);
			step = (WorkflowStep) this.convertListToObjects(db.loadObjectResults(), WorkflowStep.class).get(0);
			cache.add(step);
		}
		return step;
	}

	public WorkflowStep findStepByContentlet(Contentlet contentlet) throws DotDataException {
		WorkflowStep step = cache.getStep(contentlet);
		final WorkflowScheme scheme = this.findSchemeForStruct(contentlet.getStructureInode());
		if ((step == null) || !step.getSchemeId().equals(scheme.getId())) {
			try {
				final DotConnect db = new DotConnect();
				db.setSQL(sql.SELECT_STEP_BY_CONTENTLET);
				db.addParam(contentlet.getIdentifier());
				step = (WorkflowStep) this.convertListToObjects(db.loadObjectResults(), WorkflowStep.class).get(0);
			} catch (final Exception e) {
				Logger.debug(this.getClass(), e.getMessage());
			}

			if (step == null) {
				try {
					step = this.findSteps(scheme).get(0);
				} catch (final Exception e) {
					throw new DotDataException("Unable to find workflow step for content id:" + contentlet.getIdentifier());
				}
			}

			// if the existing task belongs to another workflow schema, update
			// to the latest schema
			if (!step.getSchemeId().equals(scheme.getId())) {
				step = this.findSteps(scheme).get(0);
				final DotConnect db = new DotConnect();
				db.setSQL(sql.RESET_CONTENTLET_STEPS);
				db.addParam(step.getId());
				db.addParam(contentlet.getIdentifier());
				db.loadResult();

			}
			cache.remove(contentlet);
			cache.addStep(contentlet, step);
		}
		return step;
	}

	public List<WorkflowStep> findSteps(WorkflowScheme scheme) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(sql.SELECT_STEPS_BY_SCHEME);
		db.addParam(scheme.getId());
		return this.convertListToObjects(db.loadObjectResults(), WorkflowStep.class);

	}

	public WorkflowTask findTaskByContentlet(Contentlet contentlet) throws DotDataException {
		WorkflowTask task = cache.getTask(contentlet);
		if (task == null) {
			final HibernateUtil hu = new HibernateUtil(WorkflowTask.class);
			hu.setQuery("from workflow_task in class com.dotmarketing.portlets.workflows.model.WorkflowTask where webasset = ?");
			hu.setParam(contentlet.getIdentifier());
			task = (WorkflowTask) hu.load();
			if (task != null) {
				cache.addTask(contentlet, task);
			}
		}
		return task;
	}

	public WorkflowComment findWorkFlowCommentById(String id) throws DotDataException {
		final HibernateUtil hu = new HibernateUtil(WorkflowComment.class);
		hu.setQuery("from workflow_comment in class com.dotmarketing.portlets.workflows.model.WorkflowComment where id = ?");
		hu.setParam(id);
		return (WorkflowComment) hu.load();
	}

	@SuppressWarnings("unchecked")
	public List<WorkflowComment> findWorkFlowComments(WorkflowTask task) throws DotDataException {
		final HibernateUtil hu = new HibernateUtil(WorkflowComment.class);
		hu.setQuery("from workflow_comment in class com.dotmarketing.portlets.workflows.model.WorkflowComment " + "where workflowtask_id = ? order by creation_date desc");
		hu.setParam(task.getId());
		return (List<WorkflowComment>) hu.list();
	}

	@SuppressWarnings("unchecked")
	public List<WorkflowHistory> findWorkflowHistory(WorkflowTask task) throws DotDataException {
		final HibernateUtil hu = new HibernateUtil(WorkflowHistory.class);
		hu.setQuery("from workflow_history in class com.dotmarketing.portlets.workflows.model.WorkflowHistory " + "where workflowtask_id = ? order by creation_date");
		hu.setParam(task.getId());
		return (List<WorkflowHistory>) hu.list();
	}

	public WorkflowHistory findWorkFlowHistoryById(String id) throws DotDataException {
		final HibernateUtil hu = new HibernateUtil(WorkflowHistory.class);
		hu.setQuery("from workflow_history in class com.dotmarketing.portlets.workflows.model.WorkflowHistory where id = ?");
		hu.setParam(id);
		return (WorkflowHistory) hu.load();
	}

	public WorkflowTask findWorkFlowTaskById(String id) throws DotDataException {
		final HibernateUtil hu = new HibernateUtil(WorkflowTask.class);
		hu.setQuery("from workflow_task in class com.dotmarketing.portlets.workflows.model.WorkflowTask where id = ?");
		hu.setParam(id);
		return (WorkflowTask) hu.load();
	}

	@SuppressWarnings("unchecked")
	public List<IFileAsset> findWorkflowTaskFiles(WorkflowTask task) throws DotDataException {
		final HibernateUtil hu = new HibernateUtil(File.class);
		hu.setSQLQuery("select {file_asset.*} from file_asset,inode file_asset_1_,workflow_task,workflowtask_files tf"

				+ " where tf.file_inode = file_asset.inode and file_asset.inode = file_asset_1_.inode "
				+ " and workflow_task.id = tf.workflowtask_id and workflow_task." +
				"id = ? ");

		hu.setParam(task.getId());
		return (List<IFileAsset>) hu.list();
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
				throw new DotDataException(e.getMessage());
			} catch(ClassCastException c) {
				// not file as contentlet
			}

		}


		return contents;
	}

	private DotConnect getWorkflowSqlQuery(WorkflowSearcher searcher, boolean counting) throws DotDataException {

		final boolean isAdministrator = APILocator.getRoleAPI().doesUserHaveRole(searcher.getUser(), APILocator.getRoleAPI().loadCMSAdminRole());
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

    			final Role r = new Role();
    			r.setId(searcher.getAssignedTo());
    			userRoles.add(r);
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

    		query.append(" ( workflow_task.assigned_to in (" + rolesString + ")  ) and ");
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
				orderby=searcher.getOrderBy();
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

	public void removeAttachedFile(WorkflowTask task, String fileInode) throws DotDataException {
		final String query = "delete from workflowtask_files where workflowtask_id = ? and file_inode = ?";
		final DotConnect dc = new DotConnect();
		dc.setSQL(query);
		dc.addParam(task.getId());
		dc.addParam(fileInode);
		dc.loadResult();

	}

	public void saveAction(WorkflowAction action) throws DotDataException {

		boolean isNew = true;
		if (UtilMethods.isSet(action.getId())) {
			try {
				final WorkflowAction test = this.findAction(action.getId());
				if (test != null) {
					isNew = false;
				}
			} catch (final Exception e) {
				Logger.debug(this.getClass(), e.getMessage(), e);
			}
		} else {
			action.setId(UUIDGenerator.generateUuid());
		}

		final DotConnect db = new DotConnect();
		if (isNew) {
			db.setSQL(sql.INSERT_ACTION);
			db.addParam(action.getId());
			db.addParam(action.getStepId());
			db.addParam(action.getName());
			db.addParam(action.getCondition());
			db.addParam(action.getNextStep());
			db.addParam(action.getNextAssign());
			db.addParam(action.getOrder());
			db.addParam(action.isAssignable());
			db.addParam(action.isCommentable());
			db.addParam(action.getIcon());
			db.addParam(action.isRoleHierarchyForAssign());
			db.addParam(action.isRequiresCheckout());
			db.loadResult();
		} else {
			db.setSQL(sql.UPDATE_ACTION);
			db.addParam(action.getStepId());
			db.addParam(action.getName());
			db.addParam(action.getCondition());
			db.addParam(action.getNextStep());
			db.addParam(action.getNextAssign());
			db.addParam(action.getOrder());
			db.addParam(action.isAssignable());
			db.addParam(action.isCommentable());
			db.addParam(action.getIcon());
			db.addParam(action.isRoleHierarchyForAssign());
			db.addParam(action.isRequiresCheckout());
			db.addParam(action.getId());
			db.loadResult();
		}
		WorkflowStep proxy = new WorkflowStep();
		proxy.setId(action.getStepId());
		cache.removeActions(proxy);
		

		// update workflowScheme mod date
		WorkflowStep step = findStep(action.getStepId());
		WorkflowScheme scheme = findScheme(step.getSchemeId());
		saveScheme(scheme);

	}

	public void saveActionClass(WorkflowActionClass actionClass) throws DotDataException {

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
		WorkflowAction action = findAction(actionClass.getActionId());
		WorkflowStep step = findStep(action.getStepId());
		WorkflowScheme scheme = findScheme(step.getSchemeId());
		saveScheme(scheme);
	}

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

	public void saveScheme(WorkflowScheme scheme) throws DotDataException {

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
				db.addParam(scheme.isMandatory());
				db.addParam(scheme.getEntryActionId());
				db.addParam(scheme.isDefaultScheme());
				db.addParam(scheme.getModDate());
				db.loadResult();
			} else {
				db.setSQL(sql.UPDATE_SCHEME);
				db.addParam(scheme.getName());
				db.addParam(scheme.getDescription());
				db.addParam(scheme.isArchived());
				db.addParam(scheme.isMandatory());
				db.addParam(scheme.getEntryActionId());
				db.addParam(scheme.getModDate());
				db.addParam(scheme.getId());
				db.loadResult();

			}
			cache.remove(scheme);
		} catch (final Exception e) {
			throw new DotDataException(e.getMessage());
		}
	}

	public void deleteSchemeForStruct(String struc) throws DotDataException {
		if (LicenseUtil.getLevel() < 200) {
			return;
		}

		try {
			final DotConnect db = new DotConnect();
			db.setSQL(sql.DELETE_SCHEME_FOR_STRUCT);
			db.addParam(struc);
			db.loadResult();
		} catch (final Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotDataException(e.getMessage());
		}
	}

	public void saveSchemeForStruct(String struc, WorkflowScheme scheme) throws DotDataException {

		if (LicenseUtil.getLevel() < 200) {
			return;
		}
		try {

			final DotConnect db = new DotConnect();
			db.setSQL(sql.DELETE_SCHEME_FOR_STRUCT);
			db.addParam(struc);
			db.loadResult();

			db.setSQL(sql.INSERT_SCHEME_FOR_STRUCT);
			db.addParam(UUIDGenerator.generateUuid());
			db.addParam(scheme.getId());
			db.addParam(struc);
			db.loadResult();

			// update all tasks for the content type and reset their step to
			// null
			db.setSQL(sql.UPDATE_STEPS_BY_STRUCT);
			db.addParam((Object) null);
			db.addParam(struc);
			db.loadResult();

			// we have to clear the saved steps/tasks for all contentlets using
			// this workflow

			cache.removeStructure(struc);

			cache.clearStepsCache();
		} catch (final Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotDataException(e.getMessage());
		}
	}

	public void saveStep(WorkflowStep step) throws DotDataException {

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

	public void saveWorkflowActionClassParameter(WorkflowActionClassParameter param) throws DotDataException {

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
		WorkflowActionClass actionClass = findActionClass(param.getActionClassId());
		WorkflowAction action = findAction(actionClass.getActionId());
		WorkflowStep step = findStep(action.getStepId());
		WorkflowScheme scheme = findScheme(step.getSchemeId());
		saveScheme(scheme);
	}

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

	public void saveWorkflowTask(WorkflowTask task) throws DotDataException {
		if (task.isNew()) {
			HibernateUtil.save(task);
		} else {
		    boolean update=false;
		    try {
		        HibernateUtil.load(WorkflowTask.class, task.getId());
		        // if the object exists no exception is thrown just update
		        update=true;
		    }
		    catch(Exception ex) {
		        // if it doesn't exists then save with that primary key
		        HibernateUtil.saveWithPrimaryKey(task, task.getId());
		    }
		    if(update) {
                HibernateUtil.update(task);
		    }
		}
		cache.remove(task);
	}

	public List<WorkflowTask> searchTasks(WorkflowSearcher searcher) throws DotDataException {
		DotConnect dc = getWorkflowSqlQuery(searcher, false);
		dc.setStartRow(searcher.getCount() * searcher.getPage());
		dc.setMaxRows(searcher.getCount());
		List<Map<String,Object>> results = dc.loadObjectResults();
		List<WorkflowTask> wfTasks = new ArrayList<WorkflowTask>();

		for (Map<String, Object> row : results) {
			WorkflowTask wt = new WorkflowTask();
			wt.setId(row.get("id").toString());
			wt.setCreationDate((Date)row.get("creation_date"));
			wt.setModDate((Date)row.get("mod_date"));
			wt.setDueDate((Date)row.get("due_date"));
			wt.setCreatedBy(row.get("created_by").toString());
			wt.setAssignedTo(row.get("assigned_to").toString());
			wt.setBelongsTo(row.get("belongs_to")!=null?row.get("belongs_to").toString():"");
			wt.setTitle(row.get("title").toString());
			wt.setDescription(row.get("description")!=null?row.get("description").toString():"");
			wt.setStatus(row.get("status").toString());
			wt.setWebasset(row.get("webasset").toString());
			wfTasks.add(wt);
		}

		return wfTasks;
	}

	// christian escalation
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

	public WorkflowHistory retrieveLastStepAction(String taskId) throws DotDataException {

		final DotConnect db = new DotConnect();
		try {

			db.setSQL(sql.RETRIEVE_LAST_STEP_ACTIONID);
			db.addParam(taskId);
			db.loadResult();
			System.out.println("QUERY retrieveTaskId ESEGUITA");

		} catch (final Exception e) {
			Logger.debug(this.getClass(), e.getMessage(), e);
		}

		return (WorkflowHistory) this.convertListToObjects(db.loadObjectResults(), WorkflowHistory.class).get(0);

	}
	// christian escalation

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
            db.addParam(schemaName);
            List<WorkflowScheme> list = this.convertListToObjects(db.loadObjectResults(), WorkflowScheme.class);
            scheme = list.size()>0 ? (WorkflowScheme)list.get(0) : null;
        } catch (final Exception e) {
            throw new DotDataException(e.getMessage(),e);
        }
        return scheme;
    }
}