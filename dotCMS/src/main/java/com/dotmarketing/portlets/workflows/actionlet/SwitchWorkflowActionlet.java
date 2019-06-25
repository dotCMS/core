
package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.rendering.velocity.viewtools.exception.DotToolException;
import com.dotcms.rest.MapToContentletPopulator;
import com.dotcms.system.event.local.type.content.CommitListenerEvent;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.FlushCacheRunnable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.*;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.RELATIONSHIP_KEY;

/**
 * This actionlet does the switch between one workflow step to another workflow step.
 * @author jsanca
 */
public class SwitchWorkflowActionlet extends ResetTaskActionlet {

	private static final long serialVersionUID = -3399186955215452961L;
	private static final List<WorkflowActionletParameter> paramList =
			new ImmutableList.Builder<WorkflowActionletParameter>()
						.add(new WorkflowActionletParameter("workflowActionId", "Workflow Action Id", null, true))
						.build();

	@Override
	public String getName() {
		return "Switch Workflow";
	}

	@Override
	public String getHowTo() {

		return "Action to switch to another Workflow";
	}

	@Override
	public void executeAction(final WorkflowProcessor processor,
							  final Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {

		final Contentlet contentlet = processor.getContentlet();
		final WorkflowActionClassParameter workflowActionIdParam =
				params.get("workflowActionId");
		super.executeAction(processor, params);

		try {

			HibernateUtil.addCommitListener(new FlushCacheRunnable() {
				public void run() {
					//Triggering event listener when this commit listener is executed
					fireAction (contentlet.getIdentifier(),
							processor.getUser(), workflowActionIdParam.getValue());
				}
			});

		} catch (DotHibernateException e1) {

			Logger.warn(this, e1.getMessage());
		}
	}

	private void fireAction(final String identifier, final User user, final String workflowActionId) {

		try {

			final ContentletAPI contentletAPI = APILocator.getContentletAPI();
			final CategoryAPI   categoryAPI   = APILocator.getCategoryAPI();
			final WorkflowAPI   workflowAPI   = APILocator.getWorkflowAPI();
			final boolean allowFrontEndSaving = false;
			final Contentlet contentlet       = contentletAPI.findContentletByIdentifierAnyLanguage(identifier);
			final List<Category> categories   = categoryAPI.getCategoriesFromContent(contentlet, user, allowFrontEndSaving);

			Logger.debug(this, ()-> "Running the fire action on switch workflow actionlet, with this wfId: " + workflowActionId
										+ ", identifier: " + identifier);
			final ContentletDependencies contentletDependencies = new ContentletDependencies.Builder()
					.indexPolicy(IndexPolicyProvider.getInstance().forSingleContent())
					.respectAnonymousPermissions(allowFrontEndSaving)
					.workflowActionId(workflowActionId)
					.modUser(user).categories(categories).build();

			workflowAPI.fireContentWorkflow(contentlet, contentletDependencies);
		} catch (Exception e) {

			Logger.error(this, e.getMessage(), e);
		}
	}


	@Override
	public  List<WorkflowActionletParameter> getParameters() {

		return paramList;
	}
}
