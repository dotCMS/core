package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;

/**
 * Do the save for a content (checkin)
 * @author jsanca
 */
@Actionlet(save = true)
public class SaveContentActionlet extends WorkFlowActionlet {

	private final ContentletAPI contentletAPI;

	public SaveContentActionlet () {

		this(APILocator.getContentletAPI());
	}

	@VisibleForTesting
	protected SaveContentActionlet (final ContentletAPI contentletAPI) {

		this.contentletAPI = contentletAPI;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getName() {
		return "Save content";
	}

	public String getHowTo() {

		return "This actionlet will checkin the content.";
	}

	@WrapInTransaction
	public void executeAction(final WorkflowProcessor processor,
							  final Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

		try {

			final Contentlet contentlet =
					processor.getContentlet();
			final User user = processor.getUser();

			Logger.debug(this,
					()->"Saving the content of the contentlet: " + contentlet.getIdentifier());

			final Contentlet checkoutContentlet = this.checkout(contentlet, processor.getUser());

			checkoutContentlet.setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);
			checkoutContentlet.setProperty(Contentlet.TO_BE_PUBLISH, contentlet.getBoolProperty(Contentlet.TO_BE_PUBLISH));
			checkoutContentlet.setProperty(Contentlet.SKIP_RELATIONSHIPS_VALIDATION,
					contentlet.getBoolProperty(Contentlet.SKIP_RELATIONSHIPS_VALIDATION));

			final ContentletDependencies contentletDependencies = processor.getContentletDependencies();
			final boolean respectFrontendPermission = contentletDependencies != null ?
					contentletDependencies.isRespectAnonymousPermissions() : user.isFrontendUser();

			final Contentlet contentletNew = (null != contentletDependencies)?
					this.contentletAPI.checkin(checkoutContentlet, contentletDependencies):
					this.contentletAPI.checkin(checkoutContentlet, processor.getUser(), respectFrontendPermission);

			this.setIndexPolicy(contentlet, contentletNew);
			this.setSpecialVariables(contentlet, contentletNew);

			processor.setContentlet(contentletNew);

			Logger.debug(this,
					()->"content version already saved for the contentlet: " + contentlet.getIdentifier());
		} catch (final Exception e) {
			if (!(e instanceof DotContentletValidationException)) {
				Logger.error(this.getClass(), ExceptionUtil.getErrorMessage(e), e);
			}
            throw new WorkflowActionFailureException(ExceptionUtil.getErrorMessage(e), e);
		}
	}

	public Contentlet checkout (final Contentlet contentlet, final User user)
			throws DotSecurityException, DotDataException {

		final boolean    isNew              = this.isNew (contentlet);
		final Contentlet checkoutContentlet = isNew? contentlet:
				this.contentletAPI.checkout(contentlet.getInode(), user, false);

		if (!isNew) {

			final String inode = checkoutContentlet.getInode();
			this.contentletAPI.copyProperties(checkoutContentlet, contentlet.getMap());
			this.setIndexPolicy(contentlet, checkoutContentlet);
			checkoutContentlet.setInode(inode);
			this.setSpecialVariables (contentlet, checkoutContentlet);
		}

		return checkoutContentlet;
	}

	private void setSpecialVariables(final Contentlet contentlet, final Contentlet checkoutContentlet) {

		if (contentlet.getMap().containsKey(Contentlet.VALIDATE_EMPTY_FILE)) {

			checkoutContentlet.getMap().put(Contentlet.VALIDATE_EMPTY_FILE, contentlet.getMap().get(Contentlet.VALIDATE_EMPTY_FILE));
		}
	}

	private void setIndexPolicy (final Contentlet originContentlet, final Contentlet newContentlet) {

		newContentlet.setIndexPolicy(originContentlet.getIndexPolicy());
		newContentlet.setIndexPolicyDependencies(originContentlet.getIndexPolicyDependencies());
	}

	private boolean isNew(final Contentlet contentlet) {
		return !UtilMethods.isSet(contentlet.getIdentifier()) || contentlet.isNew();
	}

	public WorkflowStep getNextStep() {

		return null;
	}

	@Override
	public  List<WorkflowActionletParameter> getParameters() {

		return null;
	}
}
