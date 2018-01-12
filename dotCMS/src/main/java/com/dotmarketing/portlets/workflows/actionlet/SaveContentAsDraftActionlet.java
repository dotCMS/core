package com.dotmarketing.portlets.workflows.actionlet;

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
public class SaveContentAsDraftActionlet extends WorkFlowActionlet {

	private final ContentletAPI   contentletAPI;
	private final RelationshipAPI relationshipAPI;
	private final CategoryAPI     categoryAPI;
	private final PermissionAPI   permissionAPI;

	public SaveContentAsDraftActionlet() {

		this.contentletAPI   = APILocator.getContentletAPI();
		this.relationshipAPI = APILocator.getRelationshipAPI();
		this.categoryAPI     = APILocator.getCategoryAPI();
		this.permissionAPI   = APILocator.getPermissionAPI();
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

		final Map<Relationship, List<Contentlet>> contentRelationships = new HashMap<Relationship, List<Contentlet>>();

		try {

			final Contentlet contentlet             = processor.getContentlet();
			final User user                         = processor.getUser();
			final List<Category > categories        =
					this.categoryAPI.getParents(contentlet, user, false);
			final List< Permission > permissions    =
					this.permissionAPI.getPermissions(contentlet, false, true);

			Logger.debug(this,
					"Saving the content as draft of the contentlet: " + contentlet.getIdentifier());

			contentlet.setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);

			this.buildRelationships(processor.getUser(), contentRelationships, contentlet);

			final Contentlet contentletNew = this.contentletAPI.saveDraft(
					contentlet, contentRelationships, categories, permissions, user, true);

			processor.setContentlet(contentletNew);
			Logger.debug(this,
					"content draft already saved for the contentlet: " + contentlet.getIdentifier());
		} catch (Exception e) {

			Logger.error(this.getClass(),e.getMessage(),e);
			throw new  WorkflowActionFailureException(e.getMessage());
		}
	}

	private void buildRelationships(final User user,
									final Map<Relationship, List<Contentlet>> contentRelationships,
									final Contentlet contentlet) throws DotDataException, DotSecurityException {

		final List<Relationship> relationships =  this.relationshipAPI.
				byContentType(contentlet.getContentType());

		for (final Relationship relationship : relationships) {

            if (!contentRelationships.containsKey(relationship)) {

                contentRelationships
                        .put(relationship, new ArrayList<Contentlet>());
            }

            final List<Contentlet> relatedContents = this.contentletAPI.getRelatedContent(
                    contentlet, relationship, user, true);

            for (final Contentlet relatedContent : relatedContents) {
                contentRelationships.get(relationship).add(relatedContent);
            }
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
