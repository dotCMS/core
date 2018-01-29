package com.dotmarketing.portlets.workflows.business;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class SaveContentActionletTest extends BaseWorkflowIntegrationTest {

    private static CreateSchemeStepActionResult schemeStepActionResult = null;
    private static WorkflowAPI                  workflowAPI            = null;
    private static ContentletAPI                contentletAPI          = null;
    private static ContentType                  type                   = null;
    private static Contentlet                   contentlet             = null;


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();
        SaveContentActionletTest.workflowAPI              = APILocator.getWorkflowAPI();
        final ContentTypeAPI contentTypeAPI               = APILocator.getContentTypeAPI(APILocator.systemUser());
        SaveContentActionletTest.contentletAPI            = APILocator.getContentletAPI();

        // creates the scheme and actions
        SaveContentActionletTest.schemeStepActionResult   = SaveContentActionletTest.createSchemeStepActionActionlet
                ("saveContentScheme" + UUIDGenerator.generateUuid(), "step1", "action1", SaveContentActionlet.class);

        // creates the type to trigger the scheme
        SaveContentActionletTest.createTestType(contentTypeAPI);

        // associated the scheme to the type
        SaveContentActionletTest.workflowAPI.saveSchemesForStruct(new StructureTransformer(SaveContentActionletTest.type).asStructure(),
                Arrays.asList(SaveContentActionletTest.schemeStepActionResult.getScheme()));

    }

    private static void createTestType(final ContentTypeAPI contentTypeAPI) throws DotDataException, DotSecurityException {

        SaveContentActionletTest.type = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                .description("DotSaveContentActionletTest...")
                .name("DotSaveContentActionletTest").owner(APILocator.systemUser().toString())
                .variable("DotSaveContentActionletTest").build());

        final List<Field> fields = new ArrayList<>(SaveContentActionletTest.type.fields());

        fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                .contentTypeId(SaveContentActionletTest.type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                .contentTypeId(SaveContentActionletTest.type.id()).dataType(DataTypes.TEXT).indexed(true).build());

        SaveContentActionletTest.type = contentTypeAPI.save(SaveContentActionletTest.type, fields);
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup() throws DotDataException, DotSecurityException, AlreadyExistException {

        try {

            if (null != SaveContentActionletTest.contentlet) {

                SaveContentActionletTest.contentletAPI.archive(SaveContentActionletTest.contentlet, APILocator.systemUser(), false);
                SaveContentActionletTest.contentletAPI.delete(SaveContentActionletTest.contentlet, APILocator.systemUser(), false);
            }
        } finally {

            try {

                if (null != SaveContentActionletTest.schemeStepActionResult) {

                    SaveContentActionletTest.cleanScheme(SaveContentActionletTest.schemeStepActionResult.getScheme());
                }
            } finally {

                if (null != SaveContentActionletTest.type) {

                    ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
                    contentTypeAPI.delete(SaveContentActionletTest.type);
                }
            }
        }
    } // cleanup

    @Test
    public void saveContentTest() throws DotDataException, DotSecurityException {

        final long       languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final Contentlet contentlet = new Contentlet();
        final User       user       = APILocator.systemUser();
        contentlet.setContentTypeId(SaveContentActionletTest.type.id());
        contentlet.setOwner(APILocator.systemUser().toString());
        contentlet.setModDate(new Date());
        contentlet.setLanguageId(languageId);
        contentlet.setStringProperty("title", "Test Save");
        contentlet.setStringProperty("txt",   "Test Save Text");
        contentlet.setHost(Host.SYSTEM_HOST);
        contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);

        // first save
        final Contentlet contentlet1 = SaveContentActionletTest.contentletAPI.checkin(contentlet, user, false);
        final String firstInode      = contentlet1.getInode();
        SaveContentActionletTest.contentlet              = contentlet1;

        // triggering the save content action
        contentlet1.setStringProperty(Contentlet.WORKFLOW_ACTION_KEY,
                this.schemeStepActionResult.getAction().getId());
        contentlet1.setStringProperty("title", "Test Save 2");
        contentlet1.setStringProperty("txt",   "Test Save Text 2");

        final WorkflowProcessor processor  =
                SaveContentActionletTest.workflowAPI.fireWorkflowPreCheckin(contentlet1, user);

        SaveContentActionletTest.workflowAPI.fireWorkflowPostCheckin(processor);

        contentletAPI.isInodeIndexed(processor.getContentlet().getInode());

        final Contentlet contentlet2  = SaveContentActionletTest.contentletAPI.findContentletByIdentifier
                (SaveContentActionletTest.contentlet.getIdentifier(),
                false, languageId, user, false);

        // the contentlet save by the action must be not null, should has a new version.
        Assert.assertNotNull(contentlet2);
        Assert.assertNotNull(contentlet2.getInode());
        Assert.assertFalse  (contentlet2.getInode().equals(firstInode));
        Assert.assertEquals ("Test Save 2",      contentlet2.getStringProperty("title"));
        Assert.assertEquals ("Test Save Text 2", contentlet2.getStringProperty("txt"));

    }
}