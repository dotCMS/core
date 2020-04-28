package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.business.SaveContentActionletTest;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.tag.model.TagInode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test a save and publish with a content type with non-required tags
 */
public class SaveContentActionletWithTagsTest extends BaseWorkflowIntegrationTest {

    private static WorkflowAPI workflowAPI = null;
    private static ContentTypeAPI contentTypeAPI = null;
    private static ContentType customContentType = null;
    private static final int LIMIT = 20;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();
        workflowAPI = APILocator.getWorkflowAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

        // creates the type to trigger the scheme
        customContentType = createTestType(contentTypeAPI);

        // associated the scheme to the type
        final WorkflowScheme systemWorkflowScheme = workflowAPI.findSystemWorkflowScheme();
        workflowAPI.saveSchemesForStruct(new StructureTransformer(customContentType).asStructure(),
                Collections.singletonList(systemWorkflowScheme));

        setDebugMode(false);
    }

    private static ContentType createTestType(final ContentTypeAPI contentTypeAPI)
            throws DotDataException, DotSecurityException {

        final ContentType type = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                        .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .description("Dot..")
                        .name("DotSaveActionletTest" + System.currentTimeMillis())
                        .owner(APILocator.systemUser().toString())
                        .variable("DotSaveActionletTest" + System.currentTimeMillis()).build());

        final List<Field> fields = new ArrayList<>(type.fields());

        fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TagField.class).name("tag").variable("tag").required(false)
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());

        return contentTypeAPI.save(type, fields);
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup()
            throws DotDataException, DotSecurityException {

        if (null != customContentType) {

            final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
            contentTypeAPI.delete(customContentType);
        }

        cleanupDebug(SaveContentActionletTest.class);
    } // cleanup

    @Test
    public void test_Save_Contentlet_Actionlet_Tags () throws DotSecurityException, DotDataException {

        final Contentlet contentlet = new Contentlet();
        contentlet.setContentType(customContentType);
        contentlet.setProperty("title", "Test");
        contentlet.setProperty("txt", "Test");
        contentlet.setProperty("tag", "test");

        final Contentlet contentletSaved =
                workflowAPI.fireContentWorkflow(contentlet,
                        new ContentletDependencies.Builder()
                                .modUser(APILocator.systemUser())
                                .workflowActionId(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID)
                                .build());

        Assert.assertNotNull(contentletSaved);
        Assert.assertEquals("Test", contentletSaved.getStringProperty("title"));
        Assert.assertEquals("Test", contentletSaved.getStringProperty("txt"));
        contentletSaved.setTags();
        Assert.assertEquals("test", contentletSaved.getStringProperty("tag"));

        //// save 2 override - adding a new tag
        final List<TagInode> tagInodes = APILocator.getTagAPI().getTagInodesByInode(contentletSaved.getInode());
        Assert.assertNotNull(tagInodes);
        Assert.assertFalse(tagInodes.isEmpty());

        contentletSaved.setProperty("tag", "test,testing");

        final Contentlet contentletSaved2 =
                        workflowAPI.fireContentWorkflow(contentletSaved,
                                new ContentletDependencies.Builder()
                                        .modUser(APILocator.systemUser())
                                        .workflowActionId(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID)
                                        .build());

        Assert.assertNotNull(contentletSaved2);
        Assert.assertEquals("Test", contentletSaved2.getStringProperty("title"));
        Assert.assertEquals("Test", contentletSaved2.getStringProperty("txt"));
        contentletSaved2.setTags();
        Assert.assertTrue( contentletSaved2.getStringProperty("tag").contains("testing"));
        Assert.assertTrue( contentletSaved2.getStringProperty("tag").contains("test"));

        //// save 3 override to just one
        contentletSaved2.setProperty("tag", "testing");

        final Contentlet contentletSaved3 =
                workflowAPI.fireContentWorkflow(contentletSaved2,
                        new ContentletDependencies.Builder()
                                .modUser(APILocator.systemUser())
                                .workflowActionId(SystemWorkflowConstants.WORKFLOW_SAVE_PUBLISH_ACTION_ID)
                                .build());

        Assert.assertNotNull(contentletSaved3);
        Assert.assertEquals("Test", contentletSaved3.getStringProperty("title"));
        Assert.assertEquals("Test", contentletSaved3.getStringProperty("txt"));
        contentletSaved3.setTags();
        Assert.assertEquals("testing", contentletSaved3.getStringProperty("tag"));

        ////save 4 cleaning all tags
        contentletSaved3.setProperty("tag", "");

        final Contentlet contentletSaved4 =
                workflowAPI.fireContentWorkflow(contentletSaved3,
                        new ContentletDependencies.Builder()
                                .modUser(APILocator.systemUser())
                                .workflowActionId(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID)
                                .build());

        Assert.assertNotNull(contentletSaved4);
        Assert.assertEquals("Test", contentletSaved4.getStringProperty("title"));
        Assert.assertEquals("Test", contentletSaved4.getStringProperty("txt"));
        contentletSaved4.setTags();
        Assert.assertNull(contentletSaved4.getStringProperty("tag"));
    }
}
