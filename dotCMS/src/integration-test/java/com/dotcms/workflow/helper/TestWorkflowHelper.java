package com.dotcms.workflow.helper;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test {@link WorkflowHelper}
 * @author jsanca
 */
public class TestWorkflowHelper extends IntegrationTestBase {

    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        user = APILocator.getUserAPI().getSystemUser();
    }

    /**
     * Test the getActionIdByName
     * - Creates a content type content
     * - Create a new content based on that content type
     * - Get the action Id by Name for Save, successfully
     * - Get the action Id by Name for Archive, successfully even if it is not in the firs step.
     * - Does checkin to persist the contentlet by firing save
     * - Ask again for the Archive, successfully
     * - Finally with the same contentlet, Get the action Id by Name for a non existing action, returns null as expected.
     */
    @Test
    public void test_getActionIdByName () throws Exception {

        //1 create a content type and associated to system workflow
        final ContentType contentGenericType = new ContentTypeDataGen().workflowId(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                .baseContentType(BaseContentType.CONTENT)
                .field(new FieldDataGen().name("title").velocityVarName("title").next())
                .field(new FieldDataGen().name("body").velocityVarName("body").next()).nextPersisted();

        final String unicodeText = "Numéro de téléphone";
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentGenericType.id());
        final Contentlet contentlet    = contentletDataGen.setProperty("title", "TestContent")
                .setProperty("body", unicodeText ).languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId()).nextPersisted();

        //2 get an action on the first step for a new contentlet
        final WorkflowHelper workflowHelper = WorkflowHelper.getInstance();
        final String saveActionId = workflowHelper.getActionIdOnList("Save", contentlet, user);
        Assert.assertEquals("The action returned should be Save", SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID, saveActionId);

        //3 get an action on the thrid step for a new contentlet
        String archiveActionId = workflowHelper.getActionIdOnList("Archive", contentlet, user);
        Assert.assertEquals("The action returned should be Archive", SystemWorkflowConstants.WORKFLOW_ARCHIVE_ACTION_ID, archiveActionId);

        final Contentlet contentletCheckin = APILocator.getWorkflowAPI().fireContentWorkflow(contentlet,
                new ContentletDependencies.Builder().workflowActionId(saveActionId).modUser(user).indexPolicy(IndexPolicy.FORCE).build());

        //4 get archive in a unpublish step
        archiveActionId = workflowHelper.getActionIdOnList("Archive", contentletCheckin, user);
        Assert.assertEquals("The action returned should be Archive", SystemWorkflowConstants.WORKFLOW_ARCHIVE_ACTION_ID, archiveActionId);

        //5 looking for non-existing Action
        archiveActionId = workflowHelper.getActionIdOnList("Non Existing Action", contentletCheckin, user);
        Assert.assertNull("The action returned should be Archive", archiveActionId);
    }
}
