package com.dotmarketing.portlets.workflows.business;

import static com.dotcms.rendering.velocity.util.VelocityUtil.convertToVelocityVariable;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.DbContentTypeTransformer;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.transform.TransformerLocator;
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
import com.dotmarketing.portlets.contentlet.util.ActionletUtil;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.portlets.workflows.model.WorkFlowTaskFiles;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowSearcher;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.portlets.workflows.model.transform.WorkflowSchemeTransformer;
import com.dotmarketing.portlets.workflows.model.transform.WorkflowTaskTransformer;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.beanutils.BeanUtils;
import org.postgresql.util.PGobject;

/**
 * Implementation class for the {@link WorkFlowFactory}.
 *
 * @author root
 * @since Mar, 22, 2012
 */
public class WorkflowFactoryImpl implements WorkFlowFactory {

    public static final int PARTITION_IN_SIZE = 100;
    private final WorkflowCache cache;
    private static final ObjectMapper JSON_MAPPER = DotObjectMapperProvider.getInstance()
            .getDefaultObjectMapper();

    // Column names for the workflow_action table
    private static final String WA_SCHEME_ID_COLUMN = "scheme_id";
    private static final String WA_CONDITION_TO_PROGRESS_COLUMN = "condition_to_progress";
    private static final String WA_NEXT_STEP_ID_COLUMN = "next_step_id";
    private static final String WA_NEXT_ASSIGN_COLUMN = "next_assign";
    private static final String WA_MY_ORDER_COLUMN = "my_order";
    private static final String WA_REQUIRES_CHECKOUT_COLUMN = "requires_checkout";
    private static final String WA_SHOW_ON_COLUMN = "show_on";
    private static final String WA_USE_ROLE_HIERARCHY_ASSIGN_COLUMN = "use_role_hierarchy_assign";
    private static final String WA_METADATA_COLUMN = "metadata";

    public static final String VALID_VARIABLE_NAME_REGEX = "[_A-Za-z][_0-9A-Za-z]*";

    /**
     * Creates an instance of the {@link WorkFlowFactory}.
     */
    public WorkflowFactoryImpl() {
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
     * @param obj
     * @param map
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private Object convert(Object obj, Map<String, Object> map)
            throws IllegalAccessException, InvocationTargetException {
        BeanUtils.copyProperties(obj, map);
        return obj;
    }

    /**
     * Takes the Workflow Action values from the database and creates an instance of the
     * {@link WorkflowAction} class with them.
     *
     * @param row The Map with the values coming from the database.
     *
     * @return An instance of the {@link WorkflowAction} class.
     *
     * @throws IllegalAccessException    An error occurred when copying the properties into the
     *                                   {@link WorkflowAction} class.
     * @throws InvocationTargetException An error occurred when copying the properties into the
     *                                   {@link WorkflowAction} class.
     */
    private WorkflowAction convertAction(final Map<String, Object> row)
            throws IllegalAccessException, InvocationTargetException {
        final WorkflowAction action = new WorkflowAction();
        row.put("schemeId", row.get(WA_SCHEME_ID_COLUMN));
        row.put("condition", row.get(WA_CONDITION_TO_PROGRESS_COLUMN));
        row.put("nextStep", row.get(WA_NEXT_STEP_ID_COLUMN));
        row.put("nextAssign", row.get(WA_NEXT_ASSIGN_COLUMN));
        row.put("order", row.get(WA_MY_ORDER_COLUMN));
        row.put("requiresCheckout", row.get(WA_REQUIRES_CHECKOUT_COLUMN));
        row.put("showOn", WorkflowState.toSet(row.get(WA_SHOW_ON_COLUMN)));
        row.put("roleHierarchyForAssign", row.get(WA_USE_ROLE_HIERARCHY_ASSIGN_COLUMN));
        row.computeIfPresent(WA_METADATA_COLUMN, (k, o) -> Try.of(() -> JSON_MAPPER.readValue(((PGobject) row.get(WA_METADATA_COLUMN)).getValue(),
                Map.class)).getOrElse(new HashMap<String, Object>()));
        BeanUtils.copyProperties(action, row);
        action.setPushPublishActionlet(ActionletUtil.hasPushPublishActionlet(action));
        action.setOnlyBatchActionlet(ActionletUtil.hasOnlyBatchActionlet(action));
        return action;
    }

    /**
     * @param row
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private WorkflowActionClass convertActionClass(Map<String, Object> row)
            throws IllegalAccessException, InvocationTargetException {
        final WorkflowActionClass actionClass = new WorkflowActionClass();

        row.put("clazz", row.get("clazz"));

        row.put("order", row.get("my_order"));
        row.put("actionId", row.get("action_id"));
        BeanUtils.copyProperties(actionClass, row);
        return actionClass;
    }

    /**
     * @param row
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private WorkflowActionClassParameter convertActionClassParameter(Map<String, Object> row)
            throws IllegalAccessException, InvocationTargetException {
        final WorkflowActionClassParameter param = new WorkflowActionClassParameter();
        row.put("actionClassId", row.get("workflow_action_class_id"));
        BeanUtils.copyProperties(param, row);
        return param;
    }

    /**
     * @param rs
     * @param clazz
     * @return
     * @throws DotDataException
     */
    private <T> List<T> convertListToObjects(List<Map<String, Object>> rs, Class<T> clazz)
            throws DotDataException {
        final List ret = new ArrayList();
        try {
            for (final Map<String, Object> map : rs) {
                ret.add(this.convertMaptoObject(map, clazz));
            }
        } catch (final Exception e) {
            Logger.error(this, "cannot convert object to " + clazz + " " + e.getMessage(), e);
            throw new DotDataException("cannot convert object to " + clazz + " " + e.getMessage(),
                    e);
        }

        return ret;
    }

    /**
     * @param map
     * @param clazz
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private Object convertMaptoObject(Map<String, Object> map, Class clazz)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {

        if (ContentType.class.equals(clazz)) {
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
            return WorkflowSchemeTransformer.transform(map);
        } else if (obj instanceof WorkflowHistory) {
            return this.convertHistory(map);
        } else if (obj instanceof WorkflowTask) {
            return WorkflowTaskTransformer.transform(map);
        } else {
            return this.convert(obj, map);
        }
    }

    /**
     * @param row
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private WorkflowStep convertStep(Map<String, Object> row)
            throws IllegalAccessException, InvocationTargetException {
        final WorkflowStep step = new WorkflowStep();
        row.put("myOrder", row.get("my_order"));
        row.put("schemeId", row.get("scheme_id"));
        row.put("enableEscalation", row.get("escalation_enable"));
        row.put("escalationAction", row.get("escalation_action"));
        row.put("escalationTime", row.get("escalation_time"));
        BeanUtils.copyProperties(step, row);

        return step;
    }

    /**
     * @param row
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private WorkflowHistory convertHistory(Map<String, Object> row)
            throws IllegalAccessException, InvocationTargetException {
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
    public void copyWorkflowActionClass(WorkflowActionClass from, WorkflowAction action)
            throws DotDataException {
        throw new DotWorkflowException("Not implemented");
    }

    @Override
    public void copyWorkflowActionClassParameter(WorkflowActionClassParameter from,
            WorkflowActionClass actionClass) throws DotDataException {
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
    public void deleteAction(final WorkflowAction action)
            throws DotDataException, AlreadyExistException {

        Logger.debug(this,
                "Removing action steps dependencies, for the action: " + action.getId());

        final List<Map<String, Object>> stepIdList =
                new DotConnect().setSQL(WorkflowSQL.SELECT_STEPS_ID_BY_ACTION)
                        .addParam(action.getId()).loadObjectResults();

        if (null != stepIdList && stepIdList.size() > 0) {
            new DotConnect().setSQL(WorkflowSQL.DELETE_ACTIONS_BY_STEP)
                    .addParam(action.getId()).loadResult();

            for (Map<String, Object> stepIdRow : stepIdList) {
                Logger.debug(this,
                        "Removing action steps cache " + stepIdRow.get("stepid"));
                final WorkflowStep proxyStep = new WorkflowStep();
                proxyStep.setId((String) stepIdRow.get("stepid"));
                cache.removeActions(proxyStep);
            }
        }

        Logger.debug(this,
                "Removing the action: " + action.getId());

        new DotConnect().setSQL(WorkflowSQL.DELETE_ACTION)
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
    public void deleteAction(final WorkflowAction action, final WorkflowStep step)
            throws DotDataException, AlreadyExistException {

        Logger.debug(this, "Deleting the action: " + action.getId() +
                ", from the step: " + step.getId());

        new DotConnect().setSQL(WorkflowSQL.DELETE_ACTION_STEP)
                .addParam(action.getId()).addParam(step.getId()).loadResult();

        Logger.debug(this, "Cleaning the actions from the step CACHE: " + step.getId());
        cache.removeActions(step);

        Logger.debug(this, "Updating the scheme: " + step.getSchemeId());
        // update scheme mod date
        final WorkflowScheme scheme = findScheme(step.getSchemeId());
        saveScheme(scheme);
    } // deleteAction

    @Override
    public void deleteActions(final WorkflowStep step)
            throws DotDataException, AlreadyExistException {

        Logger.debug(this, "Removing the actions associated to the step: " + step.getId());
        new DotConnect().setSQL(WorkflowSQL.DELETE_ACTIONS_STEP)
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
    public void deleteActionClass(WorkflowActionClass actionClass)
            throws DotDataException, AlreadyExistException {
        String actionId = actionClass.getActionId();
        final DotConnect db = new DotConnect();
        db.setSQL(WorkflowSQL.DELETE_ACTION_CLASS_PARAM_BY_ACTION_CLASS);
        db.addParam(actionClass.getId());
        db.loadResult();

        db.setSQL(WorkflowSQL.DELETE_ACTION_CLASS);
        db.addParam(actionClass.getId());
        db.loadResult();

        // update scheme mod date
        final WorkflowAction action = findAction(actionId);
        final WorkflowScheme scheme = findScheme(action.getSchemeId());
        saveScheme(scheme);
        this.cache.remove(action);
    }

    /**
     * @param action
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws AlreadyExistException
     */
    public void deleteActionClassByAction(WorkflowAction action)
            throws DotDataException, DotSecurityException, AlreadyExistException {

        new DotConnect().setSQL(WorkflowSQL.DELETE_ACTION_CLASS_BY_ACTION).addParam(action.getId())
                .loadResult();

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
            final Consumer<WorkflowTask> workflowTaskConsumer)
            throws DotDataException, AlreadyExistException {
        final String schemeId = step.getSchemeId();
        final DotConnect db = new DotConnect();

        // delete tasks referencing it
        db.setSQL("select id from workflow_task where status=?");
        db.addParam(step.getId());
        for (final Map<String, Object> resultMap : db.loadObjectResults()) {

            final String taskId = (String) resultMap.get("id");
            final WorkflowTask task = findWorkFlowTaskById(taskId);
            deleteWorkflowTask(task);
            if (null != workflowTaskConsumer) {
                workflowTaskConsumer.accept(task);
            }
        }

        db.setSQL(WorkflowSQL.DELETE_STEP);
        db.addParam(step.getId());
        db.loadResult();
        cache.remove(step);

        // update scheme mod date
        WorkflowScheme scheme = findScheme(schemeId);
        saveScheme(scheme);
    }

    @Override
    public int getCountContentletsReferencingStep(WorkflowStep step) throws DotDataException {

        final DotConnect db = new DotConnect();

        // get step related assets
        db.setSQL(WorkflowSQL.SELECT_COUNT_CONTENTLES_BY_STEP);
        db.addParam(step.getId());
        Map<String, Object> res = db.loadObjectResults().get(0);
        return ConversionUtils.toInt(res.get("count"), 0);
    }

    @Override
    public void deleteWorkflowActionClassParameters(WorkflowActionClass actionClass)
            throws DotDataException, AlreadyExistException {
        final DotConnect db = new DotConnect();
        db.setSQL(WorkflowSQL.DELETE_ACTION_CLASS_PARAM_BY_ACTION_CLASS);
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
    public int deleteWorkflowHistoryOldVersions(final Date olderThan) throws DotDataException {

        DotConnect dotConnect = new DotConnect();

        //Get the count of the workflow history records before deleting.
        String countSQL = "select count(*) as count from workflow_history";
        dotConnect.setSQL(countSQL);
        List<Map<String, String>> result = dotConnect.loadResults();
        final int before = Integer.parseInt(result.get(0).get("count"));

        // Delete the records using a given date
        dotConnect.setSQL("delete from workflow_history where creation_date < ?");
        dotConnect.addParam(olderThan);
        dotConnect.loadResult();

        //Get the count of the workflow history records after deleting.
        dotConnect.setSQL(countSQL);
        result = dotConnect.loadResults();
        final int after = Integer.parseInt(result.get(0).get("count"));

        return before - after;
    }

    @Override
    public void deleteWorkflowTaskByContentletIdAnyLanguage(final String webAsset)
            throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL("SELECT * FROM workflow_task WHERE webasset = ?");
        db.addParam(webAsset);
        final List<WorkflowTask> tasksToClearFromCache = this
                .convertListToObjects(db.loadObjectResults(), WorkflowTask.class);

		if (tasksToClearFromCache.isEmpty()) {
		    return;
		}
		final String parameters = "(" + String.join(",", tasksToClearFromCache.stream().map(q->"?").collect(Collectors.toList())) +")";

		db.setSQL("delete from workflow_comment where workflowtask_id in " + parameters);
        tasksToClearFromCache.forEach(t -> db.addParam(t.getId()));
		db.loadResult();

		db.setSQL("delete from workflow_history where workflowtask_id in " + parameters);
        tasksToClearFromCache.forEach(t -> db.addParam(t.getId()));
        db.loadResult();

        db.setSQL("delete from workflowtask_files where workflowtask_id in " + parameters   );
        tasksToClearFromCache.forEach(t -> db.addParam(t.getId()));
        db.loadResult();

		db.setSQL("delete from workflow_task where webasset = ?").addParam(webAsset).loadResult();

        tasksToClearFromCache.forEach(cache::remove);
    }

    @Override
    public void deleteWorkflowTaskByContentletIdAndLanguage(final String webAsset,
            final long languageId) throws DotDataException {

        final List<WorkflowTask> tasksToClearFromCache = this
                .convertListToObjects(new DotConnect()
                        .setSQL("SELECT * FROM workflow_task WHERE webasset = ? and language_id=?")
                        .addParam(webAsset)
                        .addParam(languageId).loadObjectResults(), WorkflowTask.class);

        new DotConnect().setSQL(
                        "delete from workflow_comment where workflowtask_id   in (select id from workflow_task where webasset = ? and language_id=?)")
                .addParam(webAsset).addParam(languageId).loadResult();

        new DotConnect().setSQL(
                        "delete from workflow_history where workflowtask_id   in (select id from workflow_task where webasset = ? and language_id=?)")
                .addParam(webAsset).addParam(languageId).loadResult();

        new DotConnect().setSQL(
                        "delete from workflowtask_files where workflowtask_id in (select id from workflow_task where webasset = ? and language_id=?)")
                .addParam(webAsset).addParam(languageId).loadResult();

        new DotConnect().setSQL("delete from workflow_task where webasset = ? and language_id=?")
                .addParam(webAsset).addParam(languageId).loadResult();

        tasksToClearFromCache.forEach(cache::remove);

    }

    @Override
    public long countAllSchemasSteps() throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL("SELECT COUNT(*) FROM workflow_step " +
                "INNER JOIN workflow_scheme ON workflow_scheme.id=workflow_step.scheme_id " +
                "WHERE archived = false");
        final Map results = (Map) db.loadResults().get(0);

        return Long.parseLong((String) results.get("count"));

    }

    @Override
    public long countAllSchemasActions() throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL("SELECT COUNT(*) FROM workflow_action " +
                "INNER JOIN workflow_scheme ON workflow_scheme.id=workflow_action.scheme_id " +
                "WHERE archived = false");
        final List<Map<String, Object>> results = (List<Map<String, Object>>) db.loadResults();
        final Map<String, Object> result = results.get(0);

        return Long.parseLong((String) result.get("count"));
    }

    @Override
    public long countAllSchemasSubActions() throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL("SELECT COUNT(*) " +
                "FROM workflow_action_class " +
                "INNER JOIN workflow_action ON workflow_action.id=workflow_action_class.action_id " +
                "INNER JOIN workflow_scheme ON workflow_scheme.id=workflow_action.scheme_id " +
                "WHERE archived = false");

        final List<Map<String, Object>> results = (List<Map<String, Object>>) db.loadResults();
        final Map<String, Object> result = results.get(0);

        return Long.parseLong((String) result.get("count"));
    }

    @Override
    public long countAllSchemasUniqueSubActions() throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL("SELECT COUNT(distinct workflow_action_class.name) " +
                "FROM workflow_action_class " +
                "INNER JOIN workflow_action ON workflow_action.id=workflow_action_class.action_id " +
                "INNER JOIN workflow_scheme ON workflow_scheme.id=workflow_action.scheme_id " +
                "WHERE archived = false");

        final List<Map<String, Object>> results = (List<Map<String, Object>>) db.loadResults();
        final Map<String, Object> result = results.get(0);

        return Long.parseLong((String) result.get("count"));
    }

    @Override
    public void deleteWorkflowTaskByLanguage(final Language language) throws DotDataException {

        final List<WorkflowTask> tasksToClearFromCache = this
                .convertListToObjects(new DotConnect()
                        .setSQL("SELECT * FROM workflow_task WHERE language_id=?")
                        .addParam(language.getId()).loadObjectResults(), WorkflowTask.class);

        new DotConnect().setSQL(
                        "delete from workflow_comment where workflowtask_id   in (select id from workflow_task where language_id=?)")
                .addParam(language.getId()).loadResult();

        new DotConnect().setSQL(
                        "delete from workflow_history where workflowtask_id   in (select id from workflow_task where language_id=?)")
                .addParam(language.getId()).loadResult();

        new DotConnect().setSQL(
                        "delete from workflowtask_files where workflowtask_id in (select id from workflow_task where language_id=?)")
                .addParam(language.getId()).loadResult();

        new DotConnect().setSQL("delete from workflow_task where language_id=?")
                .addParam(language.getId()).loadResult();

        tasksToClearFromCache.forEach(cache::remove);
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

            if (localTransaction) {
                HibernateUtil.closeAndCommitTransaction();
            }

        } catch (final Exception e) {
            if (localTransaction) {
                HibernateUtil.rollbackTransaction();
            }
            Logger.error(this, "deleteWorkflowTask failed:" + e, e);
            throw new DotDataException(e);
        } finally {
            cache.remove(task);
        }
    }

    @Override
    public WorkflowAction findAction(String id) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(WorkflowSQL.SELECT_ACTION);
        db.addParam(id);
        try {
            return  (WorkflowAction) this.convertListToObjects(db.loadObjectResults(),
                    WorkflowAction.class).get(0);
        } catch (IndexOutOfBoundsException ioob) {
            return null;
        }
    }

    @Override
    public WorkflowAction findAction(final String actionId,
            final String stepId) throws DotDataException {

        final DotConnect db = new DotConnect();
        db.setSQL(WorkflowSQL.SELECT_ACTION_BY_STEP);
        db.addParam(actionId).addParam(stepId);

        try {
            return (WorkflowAction) this.convertListToObjects(db.loadObjectResults(),
                    WorkflowAction.class).get(0);
        } catch (IndexOutOfBoundsException ioob) {
            return null;
        }
    }

    @Override
    public WorkflowActionClass findActionClass(String id) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(WorkflowSQL.SELECT_ACTION_CLASS);
        db.addParam(id);

        try {
            return (WorkflowActionClass) this.convertListToObjects(db.loadObjectResults(),
                    WorkflowActionClass.class).get(0);
        } catch (IndexOutOfBoundsException ioob) {
            return null;
        }
    }

    @Override
    public List<WorkflowActionClass> findActionClasses(final WorkflowAction action)
            throws DotDataException {

        List<WorkflowActionClass> classes = cache.getActionClasses(action);

        if (null == classes) {

            classes = this.convertListToObjects(
                    new DotConnect().setSQL(WorkflowSQL.SELECT_ACTION_CLASSES_BY_ACTION)
                            .addParam(action.getId()).loadObjectResults(),
                    WorkflowActionClass.class);

            cache.addActionClasses(action, classes);
        }

        return classes;
    }

    @Override
    public List<WorkflowActionClass> findActionClassesByClassName(final String actionClassName)
            throws DotDataException {

        return this.convertListToObjects(
                new DotConnect().setSQL(WorkflowSQL.SELECT_ACTION_CLASSES_BY_CLASS)
                        .addParam(actionClassName).loadObjectResults(), WorkflowActionClass.class);
    }


    public WorkflowActionClassParameter findActionClassParameter(String id)
            throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(WorkflowSQL.SELECT_ACTION_CLASS_PARAM);
        db.addParam(id);
        return (WorkflowActionClassParameter) this.convertListToObjects(db.loadObjectResults(),
                WorkflowActionClassParameter.class).get(0);
    }

    @Override
    public List<WorkflowAction> findActions(final WorkflowStep step) throws DotDataException {

        List<WorkflowAction> actions = cache.getActions(step);

        if (null == actions) {

            actions = this.convertListToObjects(
                    new DotConnect().setSQL(WorkflowSQL.SELECT_ACTIONS_BY_STEP)
                            .addParam(step.getId()).loadObjectResults(), WorkflowAction.class);

            cache.addActions(step, actions);
        }

        // we need always a copy to avoid futher modification to the WorkflowAction since they are not immutable.
        return ImmutableList.copyOf(actions);
    }

    @Override
    public List<WorkflowAction> findActions(final WorkflowScheme scheme) throws DotDataException {

        List<WorkflowAction> actions = cache.getActions(scheme);
        if (null == actions) {

            final DotConnect db = new DotConnect();
            db.setSQL(WorkflowSQL.SELECT_ACTIONS_BY_SCHEME);
            db.addParam(scheme.getId());
            actions = this.convertListToObjects(db.loadObjectResults(), WorkflowAction.class);

            cache.addActions(scheme, actions);
        }

        return actions;
    } // findActions.


    @Override
    public Map<String, WorkflowActionClassParameter> findParamsForActionClass(
            WorkflowActionClass actionClass) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(WorkflowSQL.SELECT_ACTION_CLASS_PARAMS_BY_ACTIONCLASS);
        db.addParam(actionClass.getId());
        final List<WorkflowActionClassParameter> list = (List<WorkflowActionClassParameter>) this.convertListToObjects(
                db.loadObjectResults(), WorkflowActionClassParameter.class);
        final Map<String, WorkflowActionClassParameter> map = loadDefaultActionClassParams(
                actionClass);
        for (final WorkflowActionClassParameter param : list) {
            map.put(param.getKey(), param);
        }

        return map;

    }

    /**
     * When an actionlet is added, we do not add the default values to the db.
     * <p>
     * you need to make sure that the default values of the actionlet are persisted, otherwise if
     * you try to use it, it will throw an NPE
     */
    private Map<String, WorkflowActionClassParameter> loadDefaultActionClassParams(
            final WorkflowActionClass actionClass) {

        final Map<String, WorkflowActionClassParameter> map = new LinkedHashMap<>();

        if (actionClass != null && actionClass.getActionlet() != null
                && actionClass.getActionlet().getParameters() != null) {
            for (WorkflowActionletParameter param : actionClass.getActionlet().getParameters()) {
                WorkflowActionClassParameter instanceParam = new WorkflowActionClassParameter();
                instanceParam.setActionClassId(actionClass.getId());
                instanceParam.setKey(param.getKey());
                instanceParam.setValue(param.getDefaultValue());
                map.put(param.getKey(), instanceParam);
            }
        }
        return map;


    }

    @Override
    public WorkflowScheme findScheme(String idOrVar) throws DotDataException {

        WorkflowScheme scheme = cache.getScheme(idOrVar);

        if (scheme == null) {

            /*
             1. If the if has UUID format lets try to find the workflow by id.
             2. If not let's try to find it by variable name.
             3. If the id is a really old inode, it will not have the UUID format but still need to catch that case.
             */
            if (UUIDUtil.isUUID(idOrVar)) {
                scheme = dbFindSchemeById(idOrVar);
            } else {
                try {
                    scheme = dbFindSchemeByVariable(idOrVar);
                } catch (DoesNotExistException e) {
                    scheme = dbFindSchemeById(idOrVar);
                }

            }

            cache.add(scheme);
        }

        return scheme;
    }


    @Override
    public List<WorkflowScheme> findSchemesForStruct(final String structId)
            throws DotDataException {

        List<WorkflowScheme> schemes = cache.getSchemesByStruct(structId);

        if (schemes != null) {

            // checks if any of the schemes has been invalidated (save recently and needs to refresh the schemes for the content type).
            if (schemes.stream().filter(scheme -> null == cache.getScheme(scheme.getId()))
                    .findFirst().isEmpty()) {
                return schemes;
            }
        }

        final DotConnect db = new DotConnect();
        db.setSQL(WorkflowSQL.SELECT_SCHEME_BY_STRUCT);
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
        db.setSQL(WorkflowSQL.SELECT_SCHEMES);
        db.addParam(false);
        db.addParam(showArchived);
        return this.convertListToObjects(db.loadObjectResults(), WorkflowScheme.class);
    }

    @Override
    public List<WorkflowScheme> findArchivedSchemes() throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(WorkflowSQL.SELECT_SCHEMES);
        db.addParam(true);
        db.addParam(true);
        return this.convertListToObjects(db.loadObjectResults(), WorkflowScheme.class);
    }

    @Override
    public WorkflowStep findStep(String id) throws DotDataException {
        WorkflowStep step = cache.getStep(id);
        if (step == null) {
            final DotConnect db = new DotConnect();
            db.setSQL(WorkflowSQL.SELECT_STEP);
            db.addParam(id);
            try {
                step = (WorkflowStep) this.convertListToObjects(db.loadObjectResults(),
                        WorkflowStep.class).get(0);
                cache.add(step);
            } catch (IndexOutOfBoundsException e) {
                throw new DoesNotExistException("Workflow-does-not-exists-step");
            }
        }
        return step;
    }

    @Override
    public Optional<WorkflowStep> findFirstStep(final String actionId, final String actionSchemeId)
            throws DotDataException {

        WorkflowStep workflowStep = null;
        final List<Map<String, Object>> workflowStepRows =
                new DotConnect().setMaxRows(1).setSQL(WorkflowSQL.SELECT_STEPS_BY_ACTION)
                        .addParam(actionId).loadObjectResults();

        try {

            workflowStep = UtilMethods.isSet(workflowStepRows) && workflowStepRows.size() > 0 ?
                    this.convertStep(workflowStepRows.get(0)) :
                    this.findFirstStep(actionSchemeId).orElse(null);
        } catch (IllegalAccessException | InvocationTargetException e) {

            throw new DotDataException(e);
        }

        return Optional.ofNullable(workflowStep);
    }

    @Override
    public Optional<WorkflowStep> findFirstStep(final String schemeId) throws DotDataException {

        return this.findSteps(this.findScheme(schemeId))
                .stream().findFirst();
    }

    @Override
    public List<WorkflowStep> findStepsByContentlet(final Contentlet contentlet,
            final List<WorkflowScheme> schemes) throws DotDataException {
        List<WorkflowStep> steps = new ArrayList<>();
        List<WorkflowStep> currentSteps = cache.getSteps(contentlet);
        String workflowTaskId = null;
        List<Map<String, Object>> dbResults = null;

        if (currentSteps == null) {
            WorkflowStep step = null;
            try {
                final DotConnect db = new DotConnect();
                db.setSQL(WorkflowSQL.SELECT_STEP_BY_CONTENTLET);
                db.addParam(contentlet.getIdentifier());
                db.addParam(contentlet.getLanguageId());

                dbResults = db.loadObjectResults();
                step = (WorkflowStep) this.convertListToObjects
                        (dbResults, WorkflowStep.class).get(0);
                steps.add(step);

                workflowTaskId = (String) dbResults.get(0).get("workflowid");
            } catch (final Exception e) {
                Logger.debug(this.getClass(), e.getMessage());
            }

            if (step == null) {
                try {
                    for (WorkflowScheme scheme : schemes) {
                        final List<WorkflowStep> schemeSteps = this.findSteps(scheme);
                        if (UtilMethods.isSet(schemeSteps)) {
                            step = schemeSteps.get(0);
                            steps.add(step);
                        }
                    }
                    //Add to cache list of steps
                } catch (final Exception e) {
                    throw new DotDataException("Unable to find workflow step for content id:"
                            + contentlet.getIdentifier());
                }
            }
        } else {
            steps.addAll(currentSteps);
        }
        // if the existing task belongs to another workflow schema, then remove it
        if (steps.size() == 1 && !existSchemeIdOnSchemesList(steps.get(0).getSchemeId(), schemes)) {

            if (null != workflowTaskId && !(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)) {
                this.deleteWorkflowTask(this.findWorkFlowTaskById(workflowTaskId));
            }

            // if it is the community license, has a diff workflow of the system workflow instead of removing the steps, use the first steps of the system workflow
            if ((LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) && null != workflowTaskId
                    && !WorkflowAPI.SYSTEM_WORKFLOW_ID.equals(workflowTaskId)) {

                steps.clear();
                final List<WorkflowStep> schemeSteps = this.findSteps(this.findSystemWorkflow());
                if (UtilMethods.isSet(schemeSteps)) {
                    final WorkflowStep step = schemeSteps.get(0);
                    steps.add(step);
                }
            } else {
                steps = Collections.emptyList();
            }
        }

        cache.addSteps(contentlet, steps);

        return steps;
    }

    @Override
    public boolean existSchemeIdOnSchemesList(String schemeId, List<WorkflowScheme> schemes) {
        boolean exist = false;
        for (WorkflowScheme scheme : schemes) {
            if (schemeId.equals(scheme.getId())) {
                exist = true;
                break;
            }
        }
        return exist;
    }

    @Override
    public List<WorkflowStep> findSteps(WorkflowScheme scheme) throws DotDataException {
        final DotConnect db = new DotConnect();
        db.setSQL(WorkflowSQL.SELECT_STEPS_BY_SCHEME);
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
        hu.setQuery(
                "from workflow_comment in class com.dotmarketing.portlets.workflows.model.WorkflowComment where id = ?");
        hu.setParam(id);
        return (WorkflowComment) hu.load();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<WorkflowComment> findWorkFlowComments(WorkflowTask task) throws DotDataException {
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(
                "SELECT * FROM workflow_comment WHERE workflowtask_id = ? ORDER BY creation_date DESC");
        dotConnect.addParam(task.getId());
        final List<Map<String, Object>> results = dotConnect.loadObjectResults();
        return TransformerLocator.createWorkflowCommentTransformer(results).asList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<WorkflowHistory> findWorkflowHistory(WorkflowTask task) throws DotDataException {
        final HibernateUtil hu = new HibernateUtil(WorkflowHistory.class);
        hu.setQuery(
                "from workflow_history in class com.dotmarketing.portlets.workflows.model.WorkflowHistory "
                        + "where workflowtask_id = ? order by creation_date");
        hu.setParam(task.getId());
        return (List<WorkflowHistory>) hu.list();
    }

    @Override
    public WorkflowHistory findWorkFlowHistoryById(String id) throws DotDataException {
        final HibernateUtil hu = new HibernateUtil(WorkflowHistory.class);
        hu.setQuery(
                "from workflow_history in class com.dotmarketing.portlets.workflows.model.WorkflowHistory where id = ?");
        hu.setParam(id);
        return (WorkflowHistory) hu.load();
    }

    @Override
    public WorkflowTask findWorkFlowTaskById(String id) throws DotDataException {
        final HibernateUtil hu = new HibernateUtil(WorkflowTask.class);
        hu.setQuery(
                "from workflow_task in class com.dotmarketing.portlets.workflows.model.WorkflowTask where id = ?");
        hu.setParam(id);
        return (WorkflowTask) hu.load();
    }

    @SuppressWarnings("unchecked")
    public List<Contentlet> findWorkflowTaskFilesAsContent(WorkflowTask task, User user)
            throws DotDataException {
        final HibernateUtil hu = new HibernateUtil(WorkFlowTaskFiles.class);
        hu.setQuery(
                "from workflow_task_files in class com.dotmarketing.portlets.workflows.model.WorkFlowTaskFiles where workflowtask_id = ?");
        hu.setParam(task.getId());
        List<Contentlet> contents = new ArrayList<>();
        List<WorkFlowTaskFiles> l = hu.list();

        for (WorkFlowTaskFiles f : l) {
            try {
                contents.add(APILocator.getContentletAPI().find(f.getFileInode(), user, false));
            } catch (DotSecurityException e) {
                throw new DotDataException(e.getMessage(), e);
            } catch (ClassCastException c) {
                // not file as contentlet
            }

        }

        return contents;
    }

    /**
     * @param searcher
     * @param counting
     * @return
     * @throws DotDataException
     */
    private DotConnect getWorkflowSqlQuery(WorkflowSearcher searcher, boolean counting)
            throws DotDataException {

        DotConnect dc = new DotConnect();
        final StringBuilder query = new StringBuilder();

		if (counting) {
			query.append("select count(*) as mycount from workflow_task ");
		} else {
			query.append("select workflow_task.*  from workflow_task ");
		}

        query.append(", workflow_scheme, workflow_step ");
        query.append(" where  ");
        if (UtilMethods.isSet(searcher.getKeywords())) {
            query.append(" (lower(workflow_task.title) like ? or ");
			if (DbConnectionFactory.isMsSql()) {
				query.append(
						" lower(cast(workflow_task.description as varchar(max))) like ? )  and ");
			} else {
				query.append(" lower(workflow_task.description) like ? )  and ");
			}
        }

        if (!searcher.getShow4All() || !(APILocator.getRoleAPI()
                .doesUserHaveRole(searcher.getUser(), APILocator.getRoleAPI().loadCMSAdminRole())
                || APILocator.getRoleAPI()
                .doesUserHaveRole(searcher.getUser(), RoleAPI.WORKFLOW_ADMIN_ROLE_KEY))) {
            final List<Role> userRoles = new ArrayList<>();
            if (UtilMethods.isSet(searcher.getAssignedTo())) {

                final Role r = APILocator.getRoleAPI().loadRoleById(searcher.getAssignedTo());
				if (r != null) {
					userRoles.add(r);
				}
            } else {
                userRoles.addAll(APILocator.getRoleAPI()
                        .loadRolesForUser(searcher.getUser().getUserId(), false));
                userRoles.add(APILocator.getRoleAPI().getUserRole(searcher.getUser()));

            }

            String rolesString = "";

            for (final Role role : userRoles) {
                if (!rolesString.equals("")) {
                    rolesString += ",";
                }
                rolesString += "'" + role.getId() + "'";
            }

            if (rolesString.length() > 0) {
                query.append(" ( workflow_task.assigned_to in (" + rolesString + ")  ) and ");
            }
        }
        query.append(
                " workflow_step.id = workflow_task.status and workflow_step.scheme_id = workflow_scheme.id and ");

        if (searcher.getDaysOld() != -1) {
			if (DbConnectionFactory.isMySql()) {
				query.append(" datediff(now(),workflow_task.creation_date)>=?");
			} else if (DbConnectionFactory.isPostgres()) {
				query.append(" extract(day from (now()-workflow_task.creation_date))>=?");
			} else if (DbConnectionFactory.isMsSql()) {
				query.append(" datediff(d,workflow_task.creation_date,GETDATE())>=?");
			} else if (DbConnectionFactory.isOracle()) {
				query.append(" floor(sysdate-workflow_task.creation_date)>=?");
			}

            query.append(" and ");
        }

        if (!searcher.isClosed() && searcher.isOpen()) {
            query.append(
                    "  workflow_step.resolved = " + DbConnectionFactory.getDBFalse() + " and ");
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
            String orderby = "";
            if (!UtilMethods.isSet(searcher.getStepId())) {
                // condition.append(" status , ");
            }
            if (UtilMethods.isSet(searcher.getOrderBy())) {
                orderby = searcher.getOrderBy().replaceAll("[^\\w_\\. ]", "");
            } else {

                orderby = "mod_date desc";
            }
            query.append(orderby.replace("mod_date", "workflow_task.mod_date"));
        }

        dc.setSQL(query.toString());

        // now we need to add the params

        if (UtilMethods.isSet(searcher.getKeywords())) {
            dc.addParam("%" + searcher.getKeywords().trim().toLowerCase() + "%");
            dc.addParam("%" + searcher.getKeywords().trim().toLowerCase() + "%");
        }

        if (searcher.getDaysOld() != -1) {
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
     * @param actionId
     * @return
     */
    public boolean existsAction(final String actionId) {

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
            final WorkflowStep workflowStep) throws DotDataException, AlreadyExistException {

        this.saveAction(workflowAction, workflowStep, 0);
    } // saveAction

    /////////
    @Override
    public List<Map<String, Object>> findSystemActionsByContentType(final ContentType contentType)
            throws DotDataException {

        List<Map<String, Object>> results = this.cache.findSystemActionsByContentType(
                contentType.variable());
        if (null == results) {

            results = new DotConnect().setSQL(
                            WorkflowSQL.SELECT_SYSTEM_ACTION_BY_SCHEME_OR_CONTENT_TYPE_MAPPING)
                    .addParam(contentType.variable())
                    .loadObjectResults();

            this.cache.addSystemActionsByContentType(contentType.variable(),
                    UtilMethods.isSet(results) ? results : Collections.emptyList());
        }

        return results;
    }

    @Override
    public Map<String, List<Map<String, Object>>> findSystemActionsMapByContentType(
            final List<ContentType> contentTypes) throws DotDataException {

        final ImmutableMap.Builder<String, List<Map<String, Object>>> mappingMapBuilder = new ImmutableMap.Builder<>();
        final String selectQueryTemplate = WorkflowSQL.SELECT_SYSTEM_ACTION_BY_CONTENT_TYPES;
        final Set<String> notFoundContentTypes = new HashSet<>();

        for (final ContentType contentType : contentTypes) {

            final String variable = contentType.variable();
            final List<Map<String, Object>> results = this.cache.findSystemActionsByContentType(
                    contentType.variable());
            if (null != results) {

                mappingMapBuilder.put(variable, results);
            } else {

                notFoundContentTypes.add(variable);
            }
        }

        if (!notFoundContentTypes.isEmpty()) {

            findSystemActionsMapByContentType(mappingMapBuilder, selectQueryTemplate,
                    notFoundContentTypes);
        }

        return mappingMapBuilder.build();
    }

    private void findSystemActionsMapByContentType(
            final ImmutableMap.Builder<String, List<Map<String, Object>>> mappingMapBuilder,
            final String selectQueryTemplate,
            final Set<String> notFoundContentTypesSet) throws DotDataException {

        final List<List<String>> notFoundContentTypesListOfList = Lists.partition
                (new ArrayList<>(notFoundContentTypesSet),
                        100); // select in does not support more than 100

        for (final List<String> notFoundContentTypes : notFoundContentTypesListOfList) {

            final DotConnect dotConnect = new DotConnect()
                    .setSQL(String.format(selectQueryTemplate,
                            this.createQueryIn(notFoundContentTypes)));

            notFoundContentTypes.stream().forEach(dotConnect::addObject);

            final List<Map<String, Object>> mappingRows = dotConnect.loadObjectResults();

            if (!mappingRows.isEmpty()) {

                final Map<String, List<Map<String, Object>>> dbMappingMap = new HashMap<>();

                for (final Map<String, Object> rowMap : mappingRows) {

                    final String variable = (String) rowMap.get("scheme_or_content_type");
                    dbMappingMap.computeIfAbsent(variable, key -> new ArrayList<>()).add(rowMap);
                }

                for (final Map.Entry<String, List<Map<String, Object>>> contentTypeResultsEntry : dbMappingMap.entrySet()) {

                    final String variable = contentTypeResultsEntry.getKey();
                    final List<Map<String, Object>> results = contentTypeResultsEntry.getValue();
                    this.cache.addSystemActionsByContentType(variable,
                            UtilMethods.isSet(results) ? results : Collections.emptyList());
                }

                mappingMapBuilder.putAll(dbMappingMap);
            }
        }
    }

    /**
     * Finds the {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction}'s by
     * {@link WorkflowScheme}
     *
     * @param workflowScheme {@link WorkflowScheme}
     * @return List of Rows
     */
    @Override
    public List<Map<String, Object>> findSystemActionsByScheme(final WorkflowScheme workflowScheme)
            throws DotDataException {

        List<Map<String, Object>> results = this.cache.findSystemActionsByScheme(
                workflowScheme.getId());
        if (null == results) {

            results = new DotConnect()
                    .setSQL(WorkflowSQL.SELECT_SYSTEM_ACTION_BY_SCHEME_OR_CONTENT_TYPE_MAPPING)
                    .addParam(workflowScheme.getId())
                    .loadObjectResults();

            this.cache.addSystemActionsByScheme(workflowScheme.getId(),
                    UtilMethods.isSet(results) ? results : Collections.emptyList());
        }

        return results;
    }

    @Override
    public List<Map<String, Object>> findSystemActionsByWorkflowAction(
            final WorkflowAction workflowAction) throws DotDataException {

        List<Map<String, Object>> results = this.cache.findSystemActionsByWorkflowAction(
                workflowAction.getId());
        if (null == results) {

            results = new DotConnect()
                    .setSQL(WorkflowSQL.SELECT_SYSTEM_ACTION_BY_WORKFLOW_ACTION)
                    .addParam(workflowAction.getId())
                    .loadObjectResults();

            this.cache.addSystemActionsByWorkflowAction(workflowAction.getId(),
                    UtilMethods.isSet(results) ? results : Collections.emptyList());
        }

        return results;
    }

    /**
     * Finds the {@link SystemActionWorkflowActionMapping}  associated to the
     * {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction} and
     * {@link ContentType}
     *
     * @param systemAction {@link
     *                     com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction}
     * @param contentType  {@link ContentType}
     * @return Map<String, Object>
     */
    @Override
    public Map<String, Object> findSystemActionByContentType(
            final WorkflowAPI.SystemAction systemAction, final ContentType contentType)
            throws DotDataException {

        Map<String, Object> mappingRow = this.cache.findSystemActionByContentType(
                systemAction.name(), contentType.variable());
        if (!UtilMethods.isSet(mappingRow)) {

            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL(WorkflowSQL.SELECT_SYSTEM_ACTION_BY_SYSTEM_ACTION_AND_SCHEME_OR_CONTENT_TYPE_MAPPING)
                    .addParam(systemAction.name())
                    .addParam(contentType.variable())
                    .loadObjectResults();

            if (UtilMethods.isSet(rows)) {

                mappingRow = rows.get(0);
            }

            this.cache.addSystemActionBySystemActionNameAndContentTypeVariable(
                    systemAction.name(), contentType.variable(),
                    UtilMethods.isSet(mappingRow) ? mappingRow : Collections.emptyMap());
        }

        return mappingRow;
    }

    /**
     * Finds the list of {@link SystemActionWorkflowActionMapping}  associated to the
     * {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction} and
     * {@link List} of {@link WorkflowScheme}'s
     *
     * @param systemAction {@link
     *                     com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction}
     * @param schemesList  {@link List} of {@link WorkflowScheme}'s
     * @return List<Map < String, Object>>
     */
    @Override
    public List<Map<String, Object>> findSystemActionsBySchemes(
            final WorkflowAPI.SystemAction systemAction, final List<WorkflowScheme> schemesList)
            throws DotDataException {

        final List<String> schemeIdList = schemesList.stream().map(WorkflowScheme::getId)
                .collect(Collectors.toList());
        List<Map<String, Object>> results = this.cache.findSystemActionsBySchemes(
                systemAction.name(), schemeIdList);

        if (null == results && UtilMethods.isSet(schemesList)) {

            final List<List<WorkflowScheme>> schemeListOfList =
                    Lists.partition(schemesList,
                            PARTITION_IN_SIZE); // select in does not support more than 100.
            results = new ArrayList<>();

            for (final List<WorkflowScheme> schemes : schemeListOfList) {

                final DotConnect dotConnect = new DotConnect()
                        .setSQL(String.format(WorkflowSQL.SELECT_SYSTEM_ACTION_BY_SYSTEM_ACTION_AND_SCHEMES,
                                this.createQueryIn(schemes)))
                        .addParam(systemAction.name());

                for (final WorkflowScheme scheme : schemes) {

                    dotConnect.addParam(scheme.getId());
                }

                results.addAll(dotConnect.loadObjectResults());
            }

            if (UtilMethods.isSet(results)) {

                this.cache.addSystemActionsBySystemActionNameAndSchemeIds(
                        systemAction.name(), schemeIdList, results);
            }
        }

        return results;
    }

    @Override
    public Map<String, Object> findSystemActionByIdentifier(final String identifier)
            throws DotDataException {

        final List<Map<String, Object>> rows = new DotConnect().setSQL(
                        WorkflowSQL.SELECT_SYSTEM_ACTION_BY_IDENTIFIER)
                .addParam(identifier)
                .loadObjectResults();
        return UtilMethods.isSet(rows) ? rows.get(0) : Collections.emptyMap();
    }

    @Override
    public boolean deleteSystemAction(final SystemActionWorkflowActionMapping mapping)
            throws DotDataException {

        new DotConnect().setSQL(WorkflowSQL.DELETE_SYSTEM_ACTION_BY_IDENTIFIER)
                .addParam(mapping.getIdentifier())
                .loadResult();

        this.cache.removeSystemActionWorkflowActionMapping(mapping);

        return true; //  todo: if rows affected 0 -> false
    }

    @Override
    public void deleteSystemActionsByWorkflowAction(final WorkflowAction action)
            throws DotDataException {

        Logger.debug(this, () -> "Removing system action mappings associated to the action: "
                + action.getId());

        final List<Map<String, Object>> mappingsToClean = new DotConnect().setSQL(
                        WorkflowSQL.SELECT_SYSTEM_ACTION_BY_WORKFLOW_ACTION).
                addParam(action.getId())
                .loadObjectResults();

        new DotConnect().setSQL(WorkflowSQL.DELETE_SYSTEM_ACTION_BY_WORKFLOW_ACTION_ID)
                .addParam(action.getId())
                .loadResult();

        this.cache.removeSystemActionsByWorkflowAction(action.getId());

        if (UtilMethods.isSet(mappingsToClean)) {

            for (final Map<String, Object> mappingRow : mappingsToClean) {

                final String owner = (String) mappingRow.get("scheme_or_content_type");
                this.cache.removeSystemActionsByContentType(owner);
                this.cache.removeSystemActionsByScheme(owner);
            }
        }
    }

    private String createQueryIn(final Collection list) {

        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < list.size(); ++i) {

            builder.append("?");
            if (i < list.size() - 1) {

                builder.append(",");
            }
        }

        return builder.toString();
    }


    @Override
    public SystemActionWorkflowActionMapping saveSystemActionWorkflowActionMapping(
            final SystemActionWorkflowActionMapping mapping) throws DotDataException {

        final String ownerKey = this.getOwnerKey(mapping, mapping.getOwner());
        SystemActionWorkflowActionMapping toReturn = mapping;

        final List<Map<String, Object>> existsRow =
                new DotConnect().setSQL(
                                WorkflowSQL.SELECT_SYSTEM_ACTION_BY_SYSTEM_ACTION_AND_SCHEME_OR_CONTENT_TYPE_MAPPING)
                        .addParam(mapping.getSystemAction().name())
                        .addParam(ownerKey)
                        .loadObjectResults();

        if (!UtilMethods.isSet(existsRow)) {

            new DotConnect().setSQL(WorkflowSQL.INSERT_SYSTEM_ACTION_WORKFLOW_ACTION_MAPPING)
                    .addParam(mapping.getIdentifier())
                    .addParam(mapping.getSystemAction().name())
                    .addParam(mapping.getWorkflowAction().getId())
                    .addParam(ownerKey)
                    .loadResult();
        } else {

            // if exists, we override the id and update.
            final String existingId = (String) existsRow.get(0).get("id");
            toReturn = new SystemActionWorkflowActionMapping(existingId,
                    mapping.getSystemAction(),
                    mapping.getWorkflowAction(),
                    mapping.getOwner());

            new DotConnect().setSQL(WorkflowSQL.UPDATE_SYSTEM_ACTION_WORKFLOW_ACTION_MAPPING)
                    .addParam(mapping.getSystemAction().name())
                    .addParam(mapping.getWorkflowAction().getId())
                    .addParam(ownerKey)
                    .addParam(existingId)
                    .loadResult();
        }

        this.cache.removeSystemActionWorkflowActionMapping(mapping);

        return toReturn;
    }

    /*
     * The owner could be a ContentType (variable) or WorkflowScheme (identifier)
     */
    private String getOwnerKey(final SystemActionWorkflowActionMapping mapping,
            final Object ownerContentTypeOrScheme) {

        return mapping.isOwnerContentType() ?
                ContentType.class.cast(ownerContentTypeOrScheme).variable() :
                WorkflowScheme.class.cast(ownerContentTypeOrScheme).getId();
    }

    ///////

    @Override
    public void saveAction(final WorkflowAction workflowAction,
            final WorkflowStep workflowStep,
            final int order) throws DotDataException, AlreadyExistException {

        boolean isNew = true;
        if (UtilMethods.isSet(workflowAction.getId()) && UtilMethods.isSet(workflowStep.getId())) {
            isNew = (null == findAction(workflowAction.getId(), workflowStep.getId()));
        }
        if (isNew) {
            new DotConnect().setSQL(WorkflowSQL.INSERT_ACTION_FOR_STEP)
                    .addParam(workflowAction.getId())
                    .addParam(workflowStep.getId())
                    .addParam(order)
                    .loadResult();
        } else {
            new DotConnect().setSQL(WorkflowSQL.UPDATE_ACTION_FOR_STEP_ORDER)
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
            final int order) throws DotDataException, AlreadyExistException {

        new DotConnect().setSQL(WorkflowSQL.UPDATE_ACTION_FOR_STEP_ORDER)
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
     * @param workflowAction
     * @return
     */
    private String getNextStep(final WorkflowAction workflowAction) {

        return (!UtilMethods.isSet(workflowAction.getNextStep())) ?
                WorkflowAction.CURRENT_STEP : workflowAction.getNextStep();
    }

    @Override
    public void saveAction(final WorkflowAction action)
            throws DotDataException, AlreadyExistException {

        boolean isNew = true;
        if (UtilMethods.isSet(action.getId())) {
            isNew = !this.existsAction(action.getId());
        } else {
            action.setId(UUIDGenerator.generateUuid());
        }

        final String nextStep = this.getNextStep(action);
        final DotConnect db = new DotConnect();
        if (isNew) {
            db.setSQL(WorkflowSQL.INSERT_ACTION);
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
            db.addJSONParam(action.getMetadata());
        } else {
            db.setSQL(WorkflowSQL.UPDATE_ACTION);
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
            db.addJSONParam(action.getMetadata());
            db.addParam(action.getId());
        }
        db.loadResult();
        final List<WorkflowStep> relatedProxiesSteps =
                this.findProxiesSteps(action);
        relatedProxiesSteps.forEach(cache::removeActions);

        final WorkflowScheme proxyScheme = new WorkflowScheme();
        proxyScheme.setId(action.getSchemeId());
        cache.removeActions(proxyScheme);
        // update workflowScheme mod date
        final WorkflowScheme scheme = findScheme(action.getSchemeId());
        saveScheme(scheme);
    }

    /**
     * @param action
     * @return
     * @throws DotDataException
     */
    public List<WorkflowStep> findProxiesSteps(final WorkflowAction action)
            throws DotDataException {

        final ImmutableList.Builder<WorkflowStep> stepsBuilder =
                new ImmutableList.Builder<>();

        final List<Map<String, Object>> stepIdList =
                new DotConnect().setSQL(WorkflowSQL.SELECT_STEPS_ID_BY_ACTION)
                        .addParam(action.getId()).loadObjectResults();

        if (null != stepIdList) {

            stepIdList.forEach(mapRow -> stepsBuilder.add
                    (this.buildProxyWorkflowStep((String) mapRow.get("stepid"))));
        }

        return stepsBuilder.build();
    }

    /**
     * @param stepId
     * @return
     */
    private WorkflowStep buildProxyWorkflowStep(final String stepId) {

        final WorkflowStep proxyWorkflowStep =
                new WorkflowStep();

        proxyWorkflowStep.setId(stepId);

        return proxyWorkflowStep;
    }

    @Override
    public void saveActionClass(final WorkflowActionClass actionClass)
            throws DotDataException, AlreadyExistException {

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
            db.setSQL(WorkflowSQL.INSERT_ACTION_CLASS);
            db.addParam(actionClass.getId());
            db.addParam(actionClass.getActionId());
            db.addParam(actionClass.getName());
            db.addParam(actionClass.getOrder());
            db.addParam(actionClass.getClazz());
            db.loadResult();


        } else {
            db.setSQL(WorkflowSQL.UPDATE_ACTION_CLASS);
            db.addParam(actionClass.getActionId());
            db.addParam(actionClass.getName());
            db.addParam(actionClass.getOrder());
            db.addParam(actionClass.getClazz());
            db.addParam(actionClass.getId());

            db.loadResult();
        }

        // update workflowScheme mod date
        final WorkflowAction action = findAction(actionClass.getActionId());
        final WorkflowScheme scheme = findScheme(action.getSchemeId());

        saveScheme(scheme);
        cache.remove(action);
    }

    @Override
    public void saveComment(WorkflowComment comment) throws DotDataException {
        boolean isNew = true;
        if (UtilMethods.isSet(comment.getId())) {
            try {
                final WorkflowComment test = this.findWorkFlowCommentById(comment.getId());
                if (test != null) {
                    isNew = false;
                }
            } catch (final Exception e) {
                Logger.debug(this.getClass(), e.getMessage(), e);
            }
        } else {
            comment.setId(UUIDGenerator.generateUuid());
        }

        final DotConnect db = new DotConnect();

        if (isNew) {
            db.setSQL(WorkflowSQL.INSERT_WORKFLOW_COMMENT);
            db.addParam(comment.getId());
            setCommentDBParams(comment, db);
            db.loadResult();
        } else {
            db.setSQL(WorkflowSQL.UPDATE_WORKFLOW_COMMENT);
            setCommentDBParams(comment, db);
            db.addParam(comment.getId());
            db.loadResult();
        }

    }

    private void setCommentDBParams(WorkflowComment comment, DotConnect db) {
        db.addParam(comment.getCreationDate());
        db.addParam(comment.getPostedBy());
        db.addParam(comment.getComment());
        db.addParam(comment.getWorkflowtaskId());
    }

    @Override
    public void saveScheme(WorkflowScheme scheme) throws DotDataException, AlreadyExistException {

        boolean isNew = isNewScheme(scheme);

        final DotConnect db = new DotConnect();
        try {

            if (isNew) {

                // Validating the variable name and generating a new one if necessary
                if (UtilMethods.isSet(scheme.getVariableName())) {

                    // Make sure the variable name is not already in use
                    if (doesWorkflowWithVariableExist(scheme.getVariableName())) {
                        throw new IllegalArgumentException(
                                String.format(
                                        "Invalid Workflow Scheme variable name [%s]. Already exist.",
                                        scheme.getVariableName()
                                )
                        );
                    }
                } else {

                    // Generating a new variable name for this workflow scheme
                    final String generatedVariableName = suggestVariableName(scheme.getName());
                    if (!generatedVariableName.matches(VALID_VARIABLE_NAME_REGEX)) {

                        throw new IllegalArgumentException(
                                String.format(
                                        "Invalid Workflow Scheme variable name [%s]",
                                        scheme.getVariableName()
                                )
                        );
                    }

                    scheme.setVariableName(generatedVariableName);
                }

                // Generate an ID if the scheme does not already have one
                if (UtilMethods.isEmpty(scheme.getId())) {
                    scheme.setId(generateSchemaId(scheme));
                }

                db.setSQL(WorkflowSQL.INSERT_SCHEME);
                db.addParam(scheme.getId());
                db.addParam(scheme.getName());
                db.addParam(scheme.getVariableName());
                db.addParam(scheme.getDescription());
                db.addParam(scheme.isArchived());
                db.addParam(false);
                db.addParam(scheme.isDefaultScheme());
                db.addParam(new Date());
                db.loadResult();
            } else {
                db.setSQL(WorkflowSQL.UPDATE_SCHEME);
                db.addParam(scheme.getName());
                db.addParam(scheme.getDescription());
                db.addParam(scheme.isArchived());
                db.addParam(false);
                db.addParam(new Date());
                db.addParam(scheme.getId());
                db.loadResult();

            }

            cache.remove(scheme);
        } catch (final Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    public void forceDeleteSchemeForContentType(final String contentTypeId)
            throws DotDataException {

        try {

            Logger.info(this,
                    "Deleting the schemes associated to the content type: " + contentTypeId);

            new DotConnect().setSQL(WorkflowSQL.DELETE_SCHEME_FOR_STRUCT)
                    .addParam(contentTypeId).loadResult();

            cache.removeStructure(contentTypeId);
        } catch (final Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
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
            final Set<String> schemesIds,
            final Consumer<WorkflowTask> workflowTaskConsumer) throws DotDataException {

        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {

            return;
        }

        try {

            final DotConnect db = new DotConnect();
            db.setSQL(WorkflowSQL.DELETE_SCHEME_FOR_STRUCT);
            db.addParam(contentTypeInode);
            db.loadResult();

            final ImmutableList.Builder<WorkflowStep> stepBuilder = new ImmutableList.Builder<>();
            for (final String id : schemesIds) {
                db.setSQL(WorkflowSQL.INSERT_SCHEME_FOR_STRUCT);
                db.addParam(UUIDGenerator.generateUuid());
                db.addParam(id);
                db.addParam(contentTypeInode);
                db.loadResult();

                stepBuilder.addAll(this.findSteps(this.findScheme(id)));
            }
            // update all tasks for the content type and reset their step to
            // null
            this.cleanWorkflowTaskStatus(contentTypeInode, stepBuilder.build(),
                    workflowTaskConsumer);
            this.checkContentTypeWorkflowTaskNullStatus(contentTypeInode, workflowTaskConsumer);

            // we have to clear the saved steps/tasks for all contentlets using
            // this workflow

            cache.removeStructure(contentTypeInode);
            cache.clearStepsCache();
        } catch (final Exception e) {

            Logger.error(this.getClass(), e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
        }
    }

    private void checkContentTypeWorkflowTaskNullStatus(final String contentTypeInode,
            final Consumer<WorkflowTask> workflowTaskConsumer) throws DotDataException {

        try {

            final List<WorkflowTask> tasks = this
                    .convertListToObjects(new DotConnect()
                            .setSQL(WorkflowSQL.SELECT_TASK_NULL_BY_STRUCT)
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
     * @param contentTypeInode     Content Type Inode {@link String}
     * @param steps                List of valid Steps {@link List}
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
                for (int i = 0; i < steps.size(); i++) {
                    parameters.append(", ?");
                }
                condition += parameters.toString().substring(1) + " )";
            }

            final DotConnect db = new DotConnect();
            db.setSQL(WorkflowSQL.SELECT_TASK_STEPS_TO_CLEAN_BY_STRUCT + condition);
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

            db.setSQL(WorkflowSQL.UPDATE_STEPS_BY_STRUCT + condition);
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

        final Set<String> ids = schemes.stream()
                .map(WorkflowScheme::getId)
                .collect(Collectors.toSet());

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

            db.setSQL(WorkflowSQL.INSERT_STEP);
            db.addParam(step.getId());
            db.addParam(step.getName());
            db.addParam(step.getSchemeId());
            db.addParam(step.getMyOrder());
            db.addParam(step.isResolved());
            db.addParam(step.isEnableEscalation());
            if (step.isEnableEscalation()) {
                db.addParam(step.getEscalationAction());
                db.addParam(step.getEscalationTime());
            } else {
                db.addParam((Object) null);
                db.addParam(0);
            }
            db.loadResult();
        } else {
            db.setSQL(WorkflowSQL.UPDATE_STEP);
            db.addParam(step.getName());
            db.addParam(step.getSchemeId());
            db.addParam(step.getMyOrder());
            db.addParam(step.isResolved());
            db.addParam(step.isEnableEscalation());
            if (step.isEnableEscalation()) {
                db.addParam(step.getEscalationAction());
                db.addParam(step.getEscalationTime());
            } else {
                db.addParam((Object) null);
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
    public void saveWorkflowActionClassParameter(WorkflowActionClassParameter param)
            throws DotDataException, AlreadyExistException {

        boolean isNew = true;
        if (UtilMethods.isSet(param.getId())) {
            try {
                final WorkflowActionClassParameter test = this.findActionClassParameter(
                        param.getId());
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
            db.setSQL(WorkflowSQL.INSERT_ACTION_CLASS_PARAM);
            db.addParam(param.getId());
            db.addParam(param.getActionClassId());
            db.addParam(param.getKey());
            db.addParam(param.getValue());
            db.loadResult();
        } else {
            db.setSQL(WorkflowSQL.UPDATE_ACTION_CLASS_PARAM);

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
        boolean isNew = true;
        if (UtilMethods.isSet(history.getId())) {
            try {
                final WorkflowHistory test = this.findWorkFlowHistoryById(history.getId());
                if (test != null) {
                    isNew = false;
                }
            } catch (final Exception e) {
                Logger.debug(this.getClass(), e.getMessage(), e);
            }
        } else {
            history.setId(UUIDGenerator.generateUuid());
        }

        final DotConnect db = new DotConnect();

        if (isNew) {
            db.setSQL(WorkflowSQL.INSERT_WORKFLOW_HISTORY);
            db.addParam(history.getId());
            setHistoryDBParams(history, db);
            db.loadResult();
        } else {
            db.setSQL(WorkflowSQL.UPDATE_WORKFLOW_HISTORY);
            setHistoryDBParams(history, db);
            db.addParam(history.getId());
            db.loadResult();
        }
    }

    private void setHistoryDBParams(WorkflowHistory history, DotConnect db) {
        db.addParam(history.getCreationDate());
        db.addParam(history.getMadeBy());
        db.addParam(history.getRawChangeDescription());
        db.addParam(history.getWorkflowtaskId());
        db.addParam(history.getActionId());
        db.addParam(history.getStepId());
    }

    @Override
    public void saveWorkflowTask(WorkflowTask task) throws DotDataException {

        final DotConnect db = new DotConnect()
                .setSQL(WorkflowSQL.SELECT_TASK)
                .addParam(task.getWebasset())
                .addParam(task.getLanguageId());

        final WorkflowTask dbTask = Try
                .of(() -> this.convertListToObjects(db.loadObjectResults(), WorkflowTask.class)
                        .get(0))
                .getOrNull();

        final boolean isNew = !UtilMethods.isSet(dbTask) || UtilMethods.isEmpty(dbTask.getId());

        if (isNew) {
            task.setId(UUIDGenerator.generateUuid());
            db.setSQL(WorkflowSQL.INSERT_WORKFLOW_TASK);
            db.addParam(task.getId());
            setTaskDBParams(task, db);
        } else {
            task.setId(dbTask.getId());
            db.setSQL(WorkflowSQL.UPDATE_WORKFLOW_TASK);
            setTaskDBParams(task, db);
            db.addParam(task.getId());
        }
        db.loadResult();

        cache.remove(task);
    }

    private void setTaskDBParams(WorkflowTask task, DotConnect db) {
        db.addParam(task.getCreationDate());
        db.addParam(task.getModDate());
        db.addParam(task.getDueDate());
        db.addParam(task.getCreatedBy());
        db.addParam(task.getAssignedTo());
        db.addParam(task.getBelongsTo());
        db.addParam(task.getTitle());
        db.addParam(task.getDescription());
        db.addParam(task.getStatus());
        db.addParam(task.getWebasset());
        db.addParam(task.getLanguageId());
    }

    @Override
    public List<WorkflowTask> searchTasks(WorkflowSearcher searcher) throws DotDataException {
        DotConnect dc = getWorkflowSqlQuery(searcher, false);
        dc.setStartRow(searcher.getCount() * searcher.getPage());
        dc.setMaxRows(searcher.getCount());
        List<Map<String, Object>> results = dc.loadObjectResults();
        List<WorkflowTask> wfTasks = new ArrayList<>();

        for (Map<String, Object> row : results) {
            WorkflowTask wt = new WorkflowTask();
            wt.setId(getStringValue(row, "id"));
            wt.setCreationDate((Date) row.get("creation_date"));
            wt.setModDate((Date) row.get("mod_date"));
            wt.setDueDate((Date) row.get("due_date"));
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
     * @param row
     * @param key
     * @return
     */
    private String getStringValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return (value == null) ? "" : value.toString();
    }

    /**
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

            db.setSQL(WorkflowSQL.RETRIEVE_LAST_STEP_ACTIONID);
            db.addParam(taskId);
            db.loadResult();
        } catch (final Exception e) {
            Logger.debug(this.getClass(), e.getMessage(), e);
        }

        return (WorkflowHistory) this.convertListToObjects(db.loadObjectResults(),
                WorkflowHistory.class).get(0);

    }

    @Override
    public List<WorkflowTask> findExpiredTasks() throws DotDataException, DotSecurityException {
        final DotConnect db = new DotConnect();
        List<WorkflowTask> list = new ArrayList<>();
        try {
            db.setSQL(WorkflowSQL.SELECT_EXPIRED_TASKS);
            List<Map<String, Object>> results = db.loadResults();
            for (Map<String, Object> map : results) {
                String taskId = (String) map.get("id");
                WorkflowTask task = findWorkFlowTaskById(taskId);
                list.add(task);
            }
        } catch (final Exception e) {
            Logger.error(this, e.getMessage(), e);
        } finally {
            //Why do we need to clear the session here?
            HibernateUtil.getSessionIfOpened().ifPresent(session -> session.clear());
        }
        return list;
    }

    @Override
    public WorkflowScheme findSchemeByName(String schemaName) throws DotDataException {
        WorkflowScheme scheme = null;
        try {
            final DotConnect db = new DotConnect();
            db.setSQL(WorkflowSQL.SELECT_SCHEME_NAME);
            db.addParam((schemaName != null ? schemaName.trim() : ""));
            List<WorkflowScheme> list = this.convertListToObjects(db.loadObjectResults(),
                    WorkflowScheme.class);
            scheme = list.size() > 0 ? (WorkflowScheme) list.get(0) : null;
        } catch (final Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
        return scheme;
    }

    /**
     * Finds a WorkflowScheme based on the given variable name.
     *
     * @param variableName the name of the variable to search for
     * @return the WorkflowScheme object that matches the given variable name
     * @throws DotDataException if an error occurs during the search operation
     */
    private WorkflowScheme dbFindSchemeByVariable(String variableName) throws DotDataException {

        final DotConnect dc = new DotConnect();
        dc.setSQL(WorkflowSQL.SELECT_SCHEME_BY_VARIABLE_NAME);
        dc.addParam(variableName.toLowerCase());

        List<Map<String, Object>> results;
        results = dc.loadObjectResults();
        if (results.isEmpty()) {
            throw new DoesNotExistException(
                    String.format(
                            "Workflow scheme [%s] not found", variableName
                    )
            );
        }

        return this.convertListToObjects(results, WorkflowScheme.class).get(0);
    }

    /**
     * Finds a WorkflowScheme based on the given id.
     *
     * @param id The ID of the workflow scheme to be retrieved.
     * @return The workflow scheme with the specified ID.
     * @throws DotDataException      If there is an error while performing the database operation.
     * @throws DoesNotExistException If the workflow scheme with the specified ID is not found in
     *                               the database.
     */
    private WorkflowScheme dbFindSchemeById(String id) throws DotDataException {

        final DotConnect dc = new DotConnect();
        dc.setSQL(WorkflowSQL.SELECT_SCHEME);
        dc.addParam(id);

        List<Map<String, Object>> results;
        results = dc.loadObjectResults();
        if (results.isEmpty()) {
            throw new DoesNotExistException(
                    String.format(
                            "Workflow scheme [%s] not found", id
                    )
            );
        }

        return this.convertListToObjects(results, WorkflowScheme.class).get(0);
    }

    @Override
    public void deleteWorkflowActionClassParameter(WorkflowActionClassParameter param)
            throws DotDataException, AlreadyExistException {
        DotConnect db = new DotConnect();
        db.setSQL(WorkflowSQL.DELETE_ACTION_CLASS_PARAM_BY_ID);
        db.addParam(param.getId());
        db.loadResult();

        // update scheme mod date
        WorkflowActionClass clazz = findActionClass(param.getActionClassId());
        WorkflowAction action = findAction(clazz.getActionId());
        WorkflowScheme scheme = findScheme(action.getSchemeId());
        saveScheme(scheme);
    }

    @Override
    public void updateUserReferences(String userId, String userRoleId, String replacementUserId,
            String replacementUserRoleId) throws DotDataException, DotSecurityException {
        DotConnect dc = new DotConnect();

        try {
            dc.setSQL(
                    "select id from workflow_task where (assigned_to = ? or assigned_to=? or created_by=? or created_by=?)");
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

            for (HashMap<String, String> val : tasks) {
                String id = val.get("id");
                WorkflowTask task = findWorkFlowTaskById(id);
                cache.remove(task);

                dc.setSQL(
                        "select workflow_step.id from workflow_step join workflow_task on workflow_task.status = workflow_step.id where workflow_task.webasset= ?");
                dc.addParam(task.getWebasset());
                List<HashMap<String, String>> steps = dc.loadResults();
                for (HashMap<String, String> v : steps) {
                    String stepId = v.get("id");
                    WorkflowStep step = findStep(stepId);
                    cache.remove(step);
                }
            }
        } catch (DotDataException e) {
            Logger.error(WorkFlowFactory.class, e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    public void updateStepReferences(String stepId, String replacementStepId)
            throws DotDataException, DotSecurityException {
        DotConnect dc = new DotConnect();

        try {
            // Replace references and clear cache for workflow actions
            dc.setSQL("select step_id from workflow_action where next_step_id = ?");
            dc.addParam(stepId);
            List<HashMap<String, String>> actionStepIds = dc.loadResults();

            if (replacementStepId != null) {
                dc.setSQL("update workflow_action set next_step_id = ? where next_step_id = ?");
                dc.addParam(replacementStepId);
                dc.addParam(stepId);
                dc.loadResult();

            } else {
                dc.setSQL(
                        "update workflow_action set next_step_id = step_id where next_step_id = ?");
                dc.addParam(stepId);
                dc.loadResult();
            }

            for (HashMap<String, String> v : actionStepIds) {
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

            for (HashMap<String, String> val : taskIds) {
                String id = val.get("id");
                WorkflowTask task = findWorkFlowTaskById(id);
                cache.remove(task);
            }

        } catch (DotDataException e) {
            Logger.error(WorkFlowFactory.class, e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    public List<WorkflowTask> findTasksByStep(final String stepId)
            throws DotDataException, DotSecurityException {
        List<WorkflowTask> tasks;
        DotConnect dc = new DotConnect();

        try {
            dc.setSQL(WorkflowSQL.SELECT_TASKS_BY_STEP);
            dc.addParam(stepId);
            tasks = this.convertListToObjects(dc.loadObjectResults(), WorkflowTask.class);
        } catch (DotDataException e) {
            Logger.error(WorkFlowFactory.class, e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
        }
        return tasks;
    }

    @Override
    public List<ContentType> findContentTypesByScheme(final WorkflowScheme scheme)
            throws DotDataException, DotSecurityException {
        List<ContentType> contentTypes;
        DotConnect dc = new DotConnect();
        try {
            dc.setSQL(WorkflowSQL.SELECT_STRUCTS_FOR_SCHEME);
            dc.addParam(scheme.getId());
            contentTypes = this.convertListToObjects(dc.loadObjectResults(), ContentType.class);

        } catch (DotDataException e) {
            Logger.error(WorkFlowFactory.class, e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
        }
        return contentTypes;
    }

    @Override
    public void deleteScheme(final WorkflowScheme scheme)
            throws DotDataException, DotSecurityException {
        DotConnect dc = new DotConnect();
        try {
            //delete association of content types with the scheme
            dc.setSQL(WorkflowSQL.DELETE_STRUCTS_FOR_SCHEME);
            dc.addParam(scheme.getId());
            dc.loadResult();

            //delete the scheme
            dc.setSQL(WorkflowSQL.DELETE_SCHEME);
            dc.addParam(scheme.getId());
            dc.loadResult();

            final List<WorkflowAction> actions =
                    this.cache.getActions(scheme);
            if (null != actions) {
                actions.stream().forEach(action -> this.cache.remove(action));
            }

            this.deleteSystemActionsByScheme(scheme);
            this.cache.remove(scheme);
        } catch (DotDataException e) {
            Logger.error(WorkFlowFactory.class, e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteSystemActionsByScheme(final WorkflowScheme scheme) throws DotDataException {

        Logger.debug(this,
                () -> "Deleting the system action mappings associated to the scheme: "
                        + scheme.getId());

        new DotConnect()
                .setSQL(WorkflowSQL.DELETE_SYSTEM_ACTION_BY_SCHEME_OR_CONTENT_TYPE)
                .addParam(scheme.getId())
                .loadResult();
        this.cache.removeSystemActionsByScheme(scheme.getId());
    }

    @Override
    public void deleteSystemActionsByContentType(final String contentTypeVariable)
            throws DotDataException {

        Logger.debug(this,
                () -> "Deleting the system action mappings associated to the content type: "
                        + contentTypeVariable);

        new DotConnect()
                .setSQL(WorkflowSQL.DELETE_SYSTEM_ACTION_BY_SCHEME_OR_CONTENT_TYPE)
                .addParam(contentTypeVariable)
                .loadResult();
        this.cache.removeSystemActionsByContentType(contentTypeVariable);
    }

    @Override
    public Set<String> findNullTaskContentletIdentifiersForScheme(final String workflowSchemeId)
            throws DotDataException {
        final DotConnect dc = new DotConnect();
        try {
            dc.setSQL(WorkflowSQL.SELECT_NULL_TASK_CONTENTLET_FOR_WORKFLOW);
            dc.addParam(workflowSchemeId);
            final List<Map<String, String>> result = dc.loadResults();
            return result.stream().map(row -> row.get("identifier")).collect(Collectors.toSet());
        } catch (DotDataException e) {
            Logger.error(WorkFlowFactory.class, e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    public int countWorkflowSchemes(final boolean includeArchived) {
        DotConnect dc = new DotConnect();
        final StringBuilder query = new StringBuilder("SELECT count(*) as mycount FROM workflow_scheme ");

        if(!includeArchived) {
            query.append(" WHERE archived != ? ");
        }
        dc.setSQL(query.toString());

        if(!includeArchived) {
            dc.addParam(true);
        }
        return dc.getInt("mycount");
    }

    /**
     * Simple method to check if a workflow scheme, based on its id, already exists in the database
     *
     * @param scheme WorkflowScheme to be checked
     * @return boolean true if the scheme already exists, false otherwise
     */
    private boolean isNewScheme(WorkflowScheme scheme) {

        boolean isNew = true;
        if (UtilMethods.isSet(scheme.getId())) {
            try {
                final WorkflowScheme foundWorkflow = this.findScheme(scheme.getId());
                if (foundWorkflow != null) {
                    isNew = false;
                }
            } catch (final Exception e) {
                Logger.debug(this.getClass(), e.getMessage(), e);
            }
        }

        return isNew;
    }

    /**
     * Generates the scheme id, using a deterministic id generator, the deterministic id generator
     * uses the scheme variable name as the seed for the generation.
     *
     * @param scheme the scheme to generate the id
     * @return the generated id
     */
    private String generateSchemaId(final WorkflowScheme scheme) {
        return APILocator.getDeterministicIdentifierAPI().generateDeterministicIdBestEffort(scheme);
    }

    /**
     * Suggests a variable name for a given workflow name.
     *
     * @param workflowName the name of the workflow
     * @return a suggested variable name
     * @throws DotDataException if unable to suggest a variable name for the workflow scheme
     */
    private String suggestVariableName(final String workflowName) throws DotDataException {

        DotConnect dc = new DotConnect();

        final String suggestedVarName = convertToVelocityVariable(workflowName, true);
        String varName = suggestedVarName;

        for (int i = 1; i < 10000; i++) {

            dc.setSQL(WorkflowSQL.SELECT_COUNT_BY_VARIABLE_NAME);
            dc.addParam(varName.toLowerCase());
            if (dc.getInt("test") == 0) {
                return varName;
            }

            varName = suggestedVarName + i;
        }

        throw new DotDataException(
                "Unable to suggest a variable name for workflow schem.  Got to:" + varName);
    }

    /**
     * Checks whether a workflow with the given variable name exists.
     *
     * @param variableName the name of the variable to check
     * @return true if a workflow with the variable exists, false otherwise
     * @throws DotDataException if an error occurs while checking for the workflow
     */
    private boolean doesWorkflowWithVariableExist(String variableName) throws DotDataException {

        boolean exist = false;

        try {
            final var workflowScheme = findScheme(variableName);
            exist = UtilMethods.isSet(workflowScheme);
        } catch (DoesNotExistException e) {
            // nothing to do - moving on
        }

        return exist;
    }

}
