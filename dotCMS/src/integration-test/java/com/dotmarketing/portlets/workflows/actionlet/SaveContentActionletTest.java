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

public class SaveContentActionletTest extends BaseWorkflowIntegrationTest {

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
        fields.add(FieldBuilder.builder(TagField.class).name("tag").variable("tag").required(true)
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
    public void test_Publish_With_Save_Contentlet_Actionlet_Tag () throws DotSecurityException, DotDataException {

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

        final List<TagInode> tagInodes = APILocator.getTagAPI().getTagInodesByInode(contentletSaved.getInode());
        Assert.assertNotNull(tagInodes);
        Assert.assertFalse(tagInodes.isEmpty());

        contentletSaved.setTags();
        final Contentlet contentletPublished =
                workflowAPI.fireContentWorkflow(contentletSaved,
                        new ContentletDependencies.Builder()
                                .modUser(APILocator.systemUser())
                                .workflowActionId(SystemWorkflowConstants.WORKFLOW_PUBLISH_ACTION_ID)
                                .build());

        Assert.assertNotNull(contentletPublished);
        Assert.assertEquals("Test", contentletPublished.getStringProperty("title"));
        Assert.assertEquals("Test", contentletPublished.getStringProperty("txt"));
        contentletPublished.setTags();
        Assert.assertEquals("test", contentletPublished.getStringProperty("tag"));
        Assert.assertTrue(contentletPublished.isLive());
    }

}
