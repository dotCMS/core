package com.dotmarketing.portlets.workflows.business;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentAsDraftActionlet;
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

public class SaveContentDraftActionletTest extends BaseWorkflowIntegrationTest {

    private static CreateSchemeStepActionResult schemeStepActionResult = null;
    private static WorkflowAPI                  workflowAPI            = null;
    private static ContentletAPI                contentletAPI          = null;
    private static ContentType                  type                   = null;
    private static Contentlet                   contentlet             = null;


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();
        SaveContentDraftActionletTest.workflowAPI              = APILocator.getWorkflowAPI();
        final ContentTypeAPI contentTypeAPI                    = APILocator.getContentTypeAPI(APILocator.systemUser());
        SaveContentDraftActionletTest.contentletAPI            = APILocator.getContentletAPI();

        // creates the scheme and actions
        SaveContentDraftActionletTest.schemeStepActionResult   = SaveContentDraftActionletTest.createSchemeStepActionActionlet
                ("saveContentScheme" + UUIDGenerator.generateUuid(), "step1", "action1", SaveContentAsDraftActionlet.class);

        // creates the type to trigger the scheme
        SaveContentDraftActionletTest.createTestType(contentTypeAPI);

        // associated the scheme to the type
        SaveContentDraftActionletTest.workflowAPI.saveSchemesForStruct(new StructureTransformer(SaveContentDraftActionletTest.type).asStructure(),
                Arrays.asList(SaveContentDraftActionletTest.schemeStepActionResult.getScheme()));

    }

    private static void createTestType(final ContentTypeAPI contentTypeAPI) throws DotDataException, DotSecurityException {

        SaveContentDraftActionletTest.type = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                .description("SaveContentAsDraftActionletTest...")
                .name("SaveContentDraftAsActionletTest").owner(APILocator.systemUser().toString())
                .variable("SaveContentAsDraftActionletTest").build());

        final List<Field> fields = new ArrayList<>(SaveContentDraftActionletTest.type.fields());

        fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                .contentTypeId(SaveContentDraftActionletTest.type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                .contentTypeId(SaveContentDraftActionletTest.type.id()).dataType(DataTypes.TEXT).indexed(true).build());

        SaveContentDraftActionletTest.type = contentTypeAPI.save(SaveContentDraftActionletTest.type, fields);
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup() throws DotDataException, DotSecurityException, AlreadyExistException {

        try {

            if (null != SaveContentDraftActionletTest.contentlet) {

                SaveContentDraftActionletTest.contentletAPI.delete(SaveContentDraftActionletTest.contentlet, APILocator.systemUser(), false);
            }
        } finally {

            try {

                if (null != SaveContentDraftActionletTest.schemeStepActionResult) {

                    SaveContentDraftActionletTest.cleanScheme(SaveContentDraftActionletTest.schemeStepActionResult.getScheme());
                }
            } finally {

                if (null != SaveContentDraftActionletTest.type) {

                    ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
                    contentTypeAPI.delete(SaveContentDraftActionletTest.type);
                }
            }
        }
    } // cleanup

    @Test
    public void saveContentDraftTest() throws DotDataException, DotSecurityException {

        final long       languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final Contentlet contentlet = new Contentlet();
        final User       user       = APILocator.systemUser();
        contentlet.setContentTypeId(SaveContentDraftActionletTest.type.id());
        contentlet.setOwner(user.toString());
        contentlet.setModUser(user.getUserId());
        contentlet.setModDate(new Date());
        contentlet.setLanguageId(languageId);
        contentlet.setStringProperty("title", "Test Save");
        contentlet.setStringProperty("txt",   "Test Save Text");
        contentlet.setHost(Host.SYSTEM_HOST);
        contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);


        // first save
        final Contentlet contentlet1 = SaveContentDraftActionletTest.contentletAPI.checkin(contentlet, user, false);
        SaveContentDraftActionletTest.contentlet = contentlet1;

        contentlet1.setStringProperty("title", "Test Save 1");
        contentlet1.setStringProperty("txt", "Test Save Text 1");

        contentletAPI.isInodeIndexed(contentlet1.getInode());

        final Contentlet contentlet2 = SaveContentDraftActionletTest.contentletAPI.checkout(contentlet1.getInode(), user, false);

        contentlet2.setStringProperty("title", "Test Save 2");
        contentlet2.setStringProperty("txt", "Test Save Text 2");

        final Contentlet contentlet3 = SaveContentDraftActionletTest.contentletAPI.checkin(contentlet2, user, false);

        contentletAPI.isInodeIndexed(contentlet3.getInode());

        final Contentlet contentlet4 = SaveContentDraftActionletTest.contentletAPI.
                find(contentlet3.getInode(), user, false);
        // triggering the save content action

        contentlet4.setStringProperty(Contentlet.WORKFLOW_ACTION_KEY,
                this.schemeStepActionResult.getAction().getId());
        contentlet4.setStringProperty("title", "Test Save 3");
        contentlet4.setStringProperty("txt", "Test Save Text 3");

        if (SaveContentDraftActionletTest.contentletAPI.canLock(contentlet4, user) && contentlet4.isLocked()) {
            SaveContentDraftActionletTest.contentletAPI.unlock(contentlet4, user, false);
        }

        SaveContentDraftActionletTest.workflowAPI.fireWorkflowNoCheckin(contentlet4, user);

        final Contentlet contentlet5 = SaveContentDraftActionletTest.contentletAPI.findContentletByIdentifier
                (SaveContentDraftActionletTest.contentlet.getIdentifier(),
                        false, languageId, user, false);

        // the contentlet save by the action must be not null, should has the same version.
        Assert.assertNotNull(contentlet5);
        Assert.assertNotNull(contentlet5.getInode());
        Assert.assertTrue(contentlet5.getInode().equals(contentlet3.getInode()));
        Assert.assertEquals("Test Save 3", contentlet5.getStringProperty("title"));
        Assert.assertEquals("Test Save Text 3", contentlet5.getStringProperty("txt"));


    }
}