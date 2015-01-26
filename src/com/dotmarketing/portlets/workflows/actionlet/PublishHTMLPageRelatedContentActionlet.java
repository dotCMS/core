package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Jonathan Gamba
 *         Date: 1/26/15
 */
public class PublishHTMLPageRelatedContentActionlet extends WorkFlowActionlet {

    @Override
    public String getName () {
        return "Publish HTML page and related content";
    }

    @Override
    public String getHowTo () {
        return "This actionlet will publish the HTML page and all the related content.";
    }

    @Override
    public List<WorkflowActionletParameter> getParameters () {
        return null;
    }

    @Override
    public void executeAction ( WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params ) throws WorkflowActionFailureException {

        try {

            Contentlet contentlet = processor.getContentlet();
            int structureType = contentlet.getStructure().getStructureType();

            //First lets make sure we are handling a HTML page
            if ( structureType == Structure.STRUCTURE_TYPE_HTMLPAGE ) {

                HTMLPageAsset htmlPageAsset = APILocator.getHTMLPageAssetAPI().fromContentlet( contentlet );

                if ( processor.getContentlet().isArchived() ) {
                    APILocator.getContentletAPI().unarchive( processor.getContentlet(), processor.getUser(), false );
                }

                //Get the un-publish content related to this HTMLPage
                List relatedNotPublished = new ArrayList();
                relatedNotPublished = PublishFactory.getUnpublishedRelatedAssetsForPage( htmlPageAsset, relatedNotPublished, true, processor.getUser(), false );
                //Publish the page and the related content
                PublishFactory.publishHTMLPage( htmlPageAsset, relatedNotPublished, processor.getUser(), false );
            } else {
                throw new WorkflowActionFailureException( "Unable to execute Actionlet for contentlet of type [" + structureType + "] " +
                        "and Identifier [" + contentlet.getIdentifier() + "]" );
            }

        } catch ( Exception e ) {
            Logger.error( PublishHTMLPageRelatedContentActionlet.class, e.getMessage(), e );
            throw new WorkflowActionFailureException( e.getMessage() );
        }

    }

}