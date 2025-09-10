package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.*;
import com.dotmarketing.util.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This is the Actionlet for Publishing content.
 */
@Actionlet(publish = true)
public class PublishContentActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;

    public String getName() {
        return "Publish content";
    }

    public String getHowTo() {
        return "This actionlet will publish the content.";
    }

    public WorkflowStep getNextStep() {
        return null;
    }

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        return null;
    }

    public void executeAction(final WorkflowProcessor processor,
                              final Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        try {

            final Contentlet contentlet = processor.getContentlet();
            final int structureType     = contentlet.getStructure().getStructureType();

            if (processor.getContentlet().isArchived()) {
                APILocator.getContentletAPI().unarchive(processor.getContentlet(), processor.getUser(), false);
            }

            //This should never happen, but just in case. Since evertime we publish a contentlet, we generate a new version.
            if (contentlet.getModDate().before(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))) {
                Logger.info(this,
                        () -> "Publishing a content with an old mod date: " + contentlet);
                Thread.dumpStack();
            }

            //First verify if we are handling a HTML page
            if (structureType == Structure.STRUCTURE_TYPE_HTMLPAGE) {

                final HTMLPageAsset htmlPageAsset = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);

                //Get the un-publish content related to this HTMLPage
                List relatedNotPublished = new ArrayList();
                /*
                Returns the list of unpublished related content for this HTML page where
                the user have permissions to publish that related content.
                 */
                relatedNotPublished = PublishFactory.getUnpublishedRelatedAssetsForPage(htmlPageAsset, relatedNotPublished,
                        true, processor.getUser(), false);
                relatedNotPublished.stream().filter(asset -> asset instanceof Contentlet).forEach(
                        asset -> Contentlet.class.cast(asset)
                                .setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE));
                //Publish the page and the related content
                htmlPageAsset.setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);
                this.setIndexPolicy(contentlet, htmlPageAsset);
                PublishFactory.publishHTMLPage(htmlPageAsset, relatedNotPublished, processor.getUser(),
                        processor.getContentletDependencies() != null
                                && processor.getContentletDependencies().isRespectAnonymousPermissions());

            } else {

                contentlet.setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);
                APILocator.getContentletAPI().publish(processor.getContentlet(), processor.getUser(),
                        processor.getContentletDependencies() != null
                                && processor.getContentletDependencies().isRespectAnonymousPermissions());
            }
        } catch (Exception e) {

            Logger.error(PublishContentActionlet.class, e.getMessage(), e);
            throw new WorkflowActionFailureException(e.getMessage(),e);
        }
    } // executeAction.

    private void setIndexPolicy (final Contentlet originContentlet, final Contentlet newContentlet) {

        newContentlet.setIndexPolicy(originContentlet.getIndexPolicy());
        newContentlet.setIndexPolicyDependencies(originContentlet.getIndexPolicyDependencies());
    }
} // E:O:F:PublishContentActionlet.
