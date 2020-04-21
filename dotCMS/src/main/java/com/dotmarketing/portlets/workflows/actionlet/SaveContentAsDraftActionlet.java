package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.workflows.model.*;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Do the save as draft for a content (checkin)
 * @author jsanca
 */
@Actionlet(save = true)
public class SaveContentAsDraftActionlet extends WorkFlowActionlet {

	private final ContentletAPI   contentletAPI;
	private final CategoryAPI     categoryAPI;
	private final PermissionAPI   permissionAPI;

	public SaveContentAsDraftActionlet() {
		this(
			APILocator.getContentletAPI(),
			APILocator.getCategoryAPI(),
			APILocator.getPermissionAPI()
		);
	}

	@VisibleForTesting
	SaveContentAsDraftActionlet(
			final ContentletAPI   contentletAPI,
			final CategoryAPI     categoryAPI,
			final PermissionAPI   permissionAPI
	) {
		super();
		this.contentletAPI   = contentletAPI;
		this.categoryAPI     = categoryAPI;
		this.permissionAPI   = permissionAPI;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getName() {
		return "Save Draft content";
	}

	public String getHowTo() {

		return "This actionlet will do checkin as draft the content.";
	}

	public void executeAction(final WorkflowProcessor processor,
							  final Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

		ContentletRelationships contentletRelationships = null;

		try {

			final Contentlet contentlet             = processor.getContentlet();
			final User user                         = processor.getUser();
			List<Category > categories        = null;
			final List< Permission > permissions    =
					this.permissionAPI.getPermissions(contentlet, false, true);

			Logger.debug(this,
					"Saving the content as draft of the contentlet: " + contentlet.getIdentifier());

			contentlet.setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);
			final ContentletDependencies contentletDependencies = processor.getContentletDependencies();

			if (null != contentletDependencies) {

				if(null != contentletDependencies.getModUser()) {

					contentlet.setModUser(contentletDependencies.getModUser().getUserId());
				}

				if(null != contentletDependencies.getCategories()) {

					categories = contentletDependencies.getCategories();
				}

				if(null != contentletDependencies.getRelationships()) {

					contentletRelationships
							= contentletDependencies.getRelationships();
				}
			}

			final boolean respectFrontendPermission = contentletDependencies != null ?
					contentletDependencies.isRespectAnonymousPermissions() : user.isFrontendUser();

			//Keeps existing relationships
			if (contentletRelationships == null){
                contentletRelationships = contentletAPI.getAllRelationships(contentlet);
            }

			if (categories == null) {
				categories = this.categoryAPI.getParents(contentlet, user, respectFrontendPermission);
			}

			final Contentlet contentletNew = this.contentletAPI.saveDraft(
					contentlet, contentletRelationships, categories, permissions, user,
					respectFrontendPermission
			);


			processor.setContentlet(contentletNew);
			Logger.debug(this,
					"content draft already saved for the contentlet: " + contentlet.getIdentifier());
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

		return null;
	}
}
