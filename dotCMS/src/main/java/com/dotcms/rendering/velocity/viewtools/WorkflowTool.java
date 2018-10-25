package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.rendering.velocity.viewtools.exception.DotToolException;
import com.dotcms.rest.MapToContentletPopulator;
import com.dotmarketing.business.Permissionable;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.RELATIONSHIP_KEY;


/**
 * This class is a thin wrapper - mostly read only, for the WorkflowAPI
 * @author will
 *
 */
public class WorkflowTool implements ViewTool {

	private final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
	private final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
	private final RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
	private User user;
	private HttpServletRequest request;

	public void init(final Object initData) {
		request = ((ViewContext) initData).getRequest();
		user = getUser(request);
	}

	public WorkflowTask findTaskByContentlet(Contentlet contentlet) throws DotDataException {
		return workflowAPI.findTaskByContentlet(contentlet);
	}

	public List<WorkflowStep> findStepsByContentlet(Contentlet contentlet) throws DotDataException {
		return workflowAPI.findStepsByContentlet(contentlet);
	}

	public WorkflowTask findTaskById(String id) throws DotDataException {
		return workflowAPI.findTaskById(id);
	}

	public List<WorkflowComment> findWorkFlowComments(WorkflowTask task) throws DotDataException {
		return workflowAPI.findWorkFlowComments(task);
	}

	public List<WorkflowHistory> findWorkflowHistory(WorkflowTask task) throws DotDataException {

		return workflowAPI.findWorkflowHistory(task);
	}

	public List<WorkflowScheme> findSchemes(boolean showArchived) throws DotDataException {

		return workflowAPI.findSchemes(showArchived);
	}

	public WorkflowScheme findScheme(String id) throws DotDataException, DotSecurityException {
		return workflowAPI.findScheme(id);
	}

	public List<WorkflowScheme> findSchemesForStruct(Structure struct) throws DotDataException {
		return workflowAPI.findSchemesForStruct(struct);
	}

	public List<WorkflowStep> findSteps(WorkflowScheme scheme) throws DotDataException {
		return workflowAPI.findSteps(scheme);
	}

	public WorkflowAction findAction(String id, User user) throws DotDataException, DotSecurityException {
		return workflowAPI.findAction(id, user);
	}

	public List<WorkflowAction> findAvailableActions(Contentlet contentlet, User user) throws DotDataException, DotSecurityException {
		return workflowAPI.findAvailableActions(contentlet, user);
	}

	public List<WorkflowAction> findActions(WorkflowStep step, User user) throws DotDataException, DotSecurityException {
		return workflowAPI.findActions(step, user);
	}

	public List<WorkflowAction> findActions(WorkflowStep step, User user, Permissionable permissionable) throws DotDataException, DotSecurityException {
		return workflowAPI.findActions(step, user, permissionable);
	}

	public List<WorkflowAction> findActions(List<WorkflowStep> steps, User user) throws DotDataException, DotSecurityException {
		return workflowAPI.findActions(steps, user);
	}

	public List<WorkflowAction> findActions(List<WorkflowStep> steps, User user,
			Permissionable permissionable) throws DotDataException, DotSecurityException {
		return workflowAPI.findActions(steps, user, permissionable);
	}

	public WorkflowStep findStep(String id) throws DotDataException, DotSecurityException {
		return workflowAPI.findStep(id);
	}

	public List<WorkflowActionClass> findActionClasses(WorkflowAction action) throws DotDataException {
		return workflowAPI.findActionClasses(action);
	}

	public WorkflowActionClass findActionClass(String id) throws DotDataException {
		return workflowAPI.findActionClass(id);
	}

	public Map<String, WorkflowActionClassParameter> findParamsForActionClass(WorkflowActionClass actionClass) throws DotDataException {
		return workflowAPI.findParamsForActionClass(actionClass);
	}

	public List<WorkFlowActionlet> findActionlets() throws DotDataException {
		return workflowAPI.findActionlets();
	}

	public WorkFlowActionlet findActionlet(String clazz) throws DotDataException {
		return workflowAPI.findActionlet(clazz);
	}

	public WorkflowProcessor fireWorkflowPreCheckin(Contentlet contentlet, User user) throws DotDataException, DotWorkflowException,
			DotContentletValidationException {
		return workflowAPI.fireWorkflowPreCheckin(contentlet, user);
	}

	public void fireWorkflowPostCheckin(WorkflowProcessor wflow) throws DotDataException, DotWorkflowException {
		workflowAPI.fireWorkflowPostCheckin(wflow);
	}

	public WorkflowProcessor fireWorkflowNoCheckin(Contentlet contentlet, User user) throws DotDataException, DotWorkflowException,
			DotContentletValidationException {
		return workflowAPI.fireWorkflowNoCheckin(contentlet,user);
	}

	/**
	 * Fires a Workflow Action identified by wfActionId using the given map of properties of a contentlet.
	 * @param wfActionId Id of the action to perform
	 * @param properties Map of properties of the contentlet to process
	 * @return the resulting content after performing the action
	 * @throws DotToolException runtime exception to wrap the original exception
	 */

	public Contentlet fire(final String wfActionId, final Map<String, Object> properties) throws DotToolException {
		Contentlet contentlet = new Contentlet();

		try {
			final MapToContentletPopulator mapToContentletPopulator = MapToContentletPopulator.INSTANCE;
			contentlet = mapToContentletPopulator.populate(contentlet, properties);

			final boolean ALLOW_FRONT_END_SAVING = Config
					.getBooleanProperty("WORKFLOW_TOOL_ALLOW_FRONT_END_SAVING", false);

			final List<Category> cats = categoryAPI.getCategoriesFromContent(contentlet, user, ALLOW_FRONT_END_SAVING);

			final Map<Relationship, List<Contentlet>> relationships = (Map<Relationship, List<Contentlet>>)
					contentlet.get(RELATIONSHIP_KEY);

			final ContentletDependencies contentletDependencies = new ContentletDependencies.Builder()
					.workflowActionId(wfActionId)
					.respectAnonymousPermissions(ALLOW_FRONT_END_SAVING)
					.modUser(user).categories(cats)
					.relationships(relationshipAPI.getContentletRelationshipsFromMap(contentlet, relationships))
					.indexPolicy(IndexPolicyProvider.getInstance().forSingleContent())
					.build();

			contentlet = workflowAPI.fireContentWorkflow(contentlet, contentletDependencies);

		} catch (Exception e) {
			throw new DotToolException(e);
		}

		return  contentlet;
	}

}