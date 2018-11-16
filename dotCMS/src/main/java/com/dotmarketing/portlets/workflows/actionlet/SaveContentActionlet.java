package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.*;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;

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

			Logger.debug(this,
					"Saving the content of the contentlet: " + contentlet.getIdentifier());

			final boolean    isNew              = this.isNew (contentlet);

			Logger.debug(this, ()->"\n*********----------- Checkout BEGIN : " + Thread.currentThread().getName() + ", id: " + contentlet.getIdentifier());

			final Contentlet checkoutContentlet = isNew? contentlet:
					this.contentletAPI.checkout(contentlet.getInode(), processor.getUser(), false);

			Logger.debug(this, ()->"\n*********----------- Checkout END : " + Thread.currentThread().getName() + ", id: " + contentlet.getIdentifier());

			if (!isNew) {

				final String inode = checkoutContentlet.getInode();

				Logger.debug(this, ()->"\n*********----------- Copy BEGIN : " + Thread.currentThread().getName() + ", id: " + contentlet.getIdentifier());

				this.contentletAPI.copyProperties(checkoutContentlet, contentlet.getMap());

				Logger.debug(this, ()->"\n*********----------- Copy BEGIN : " + Thread.currentThread().getName() + ", id: " + contentlet.getIdentifier());
				checkoutContentlet.setInode(inode);
			}

			checkoutContentlet.setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);

			Logger.debug(this, ()->"\n*********----------- CHECKIN BEGIN : " + Thread.currentThread().getName() + ", id: " + contentlet.getIdentifier());

			final Contentlet contentletNew = (null != processor.getContentletDependencies())?
					this.contentletAPI.checkin(checkoutContentlet, processor.getContentletDependencies()):
					this.contentletAPI.checkin(checkoutContentlet, processor.getUser(), false);

			Logger.debug(this, ()->"\n*********----------- CHECKIN END : " + Thread.currentThread().getName() + ", id: " + contentlet.getIdentifier());
			processor.setContentlet(contentletNew);

			Logger.debug(this,
					"content version already saved for the contentlet: " + contentlet.getIdentifier());
		} catch (Exception e) {

			Logger.error(this.getClass(),e.getMessage(),e);
			throw new  WorkflowActionFailureException(e.getMessage(),e);
		}
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
