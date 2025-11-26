package com.dotmarketing.portlets.workflows.actionlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotcms.util.ConversionUtils;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.CheckboxWorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

/**
 * {@link WorkFlowActionlet} that unlock a {@link Contentlet}
 *
 * This name is not good because this {@link WorkFlowActionlet} is not really checking in the contentlet.
 * We are aware of it and will change it eventually.
 */
public class CheckinContentActionlet extends WorkFlowActionlet {

	public static final String FORCE_UNLOCK_ALLOWED = "force-unlock";

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getName() {
		return "Unlock content";
	}

	public String getHowTo() {

		return "This actionlet will unlock the content.";
	}

	public void executeAction(final WorkflowProcessor processor,
							  final Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {
		try {

			final Contentlet contentlet = processor.getContentlet();
			User user = processor.getUser();
			final boolean forceUnlock = ConversionUtils.toBoolean(params.get(FORCE_UNLOCK_ALLOWED).getValue(), false);
			DotPreconditions.checkNotNull(contentlet);

			if (!contentlet.isNew() && contentlet.isLocked()) {

				contentlet.setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);
				if (forceUnlock) {

					final User finalUser = user;
					user = user.isAdmin()
							|| Try.of(()->APILocator.getContentletAPI().canLock(contentlet, finalUser)).getOrElse(false)?
							user:APILocator.systemUser();
				}

				APILocator.getContentletAPI().unlock(contentlet, user,
						processor.getContentletDependencies() != null
								&& processor.getContentletDependencies().isRespectAnonymousPermissions());
			}
		} catch (Exception e) {
			Logger.error(this.getClass(),e.getMessage(),e);
			throw new  WorkflowActionFailureException(e.getMessage(),e);
		
		}

	}

	public WorkflowStep getNextStep() {

		return null;
	}

	@Override
	public  List<WorkflowActionletParameter> getParameters() {

		final List<WorkflowActionletParameter> workflowActionletParameters = new ArrayList<>();

		workflowActionletParameters.add(new CheckboxWorkflowActionletParameter(FORCE_UNLOCK_ALLOWED, "Force Unlock", "false", false));

		return workflowActionletParameters;
	}
}
