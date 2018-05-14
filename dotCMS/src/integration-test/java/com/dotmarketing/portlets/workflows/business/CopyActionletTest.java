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
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.workflows.actionlet.CopyActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.event.CopyActionletEvent;
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

public class CopyActionletTest extends BaseWorkflowIntegrationTest {

    private static CreateSchemeStepActionResult schemeStepActionResult = null;
    private static WorkflowAPI                  workflowAPI            = null;
    private static ContentletAPI                contentletAPI          = null;
    private static ContentType                  type                   = null;
    private static Contentlet                   contentlet             = null;
    private static Contentlet                   contentletCopy         = null;


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();
        CopyActionletTest.workflowAPI              = APILocator.getWorkflowAPI();
        final ContentTypeAPI contentTypeAPI               = APILocator.getContentTypeAPI(APILocator.systemUser());
        CopyActionletTest.contentletAPI            = APILocator.getContentletAPI();

        // creates the scheme and actions
        CopyActionletTest.schemeStepActionResult   = CopyActionletTest.createSchemeStepActionActionlet
                ("CopyActionlet" + UUIDGenerator.generateUuid(), "step1", "action1", CopyActionlet.class);

        // creates the type to trigger the scheme
        CopyActionletTest.createTestType(contentTypeAPI);

        // associated the scheme to the type
        CopyActionletTest.workflowAPI.saveSchemesForStruct(new StructureTransformer(CopyActionletTest.type).asStructure(),
                Arrays.asList(CopyActionletTest.schemeStepActionResult.getScheme()));

    }

    private static void createTestType(final ContentTypeAPI contentTypeAPI) throws DotDataException, DotSecurityException {

        CopyActionletTest.type = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                        .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .description("DotCopyActionletTest...")
                        .name("DotCopyActionletTest").owner(APILocator.systemUser().toString())
                        .variable("DotCopyActionletTest").build());

        final List<Field> fields = new ArrayList<>(CopyActionletTest.type.fields());

        fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                .contentTypeId(CopyActionletTest.type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                .contentTypeId(CopyActionletTest.type.id()).dataType(DataTypes.TEXT).indexed(true).build());

        CopyActionletTest.type = contentTypeAPI.save(CopyActionletTest.type, fields);
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup() throws DotDataException, DotSecurityException, AlreadyExistException {

        if (null != CopyActionletTest.type) {

            ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
            contentTypeAPI.delete(CopyActionletTest.type);
        }

        if (null != CopyActionletTest.schemeStepActionResult) {

            CopyActionletTest.cleanScheme(CopyActionletTest.schemeStepActionResult.getScheme());
        }
    } // cleanup

    @Test
    public void saveContentTest() throws DotDataException, DotSecurityException {

        final long       languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final Contentlet contentlet = new Contentlet();
        final User       user       = APILocator.systemUser();
        contentlet.setContentTypeId(CopyActionletTest.type.id());
        contentlet.setOwner(APILocator.systemUser().toString());
        contentlet.setModDate(new Date());
        contentlet.setLanguageId(languageId);
        contentlet.setStringProperty("title", "Test Save");
        contentlet.setStringProperty("txt",   "Test Save Text");
        contentlet.setHost(Host.SYSTEM_HOST);
        contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);

        APILocator.getLocalSystemEventsAPI().subscribe(CopyActionletEvent.class, new EventSubscriber<CopyActionletEvent>() {
            @Override
            public void notify(CopyActionletEvent event) {

                contentletCopy = event.getCopyContentlet();
            }
        });

        // first save
        final Contentlet contentlet1 = CopyActionletTest.contentletAPI.checkin(contentlet, user, false);
        final String firstIdentifier = contentlet1.getIdentifier();
        final String firstInode      = contentlet1.getInode();

        CopyActionletTest.contentlet              = contentlet1;

        // triggering the copy action
        contentlet1.setActionId(
                this.schemeStepActionResult.getAction().getId());
        contentlet1.setBoolProperty(CopyActionlet.NOTIFY_SYNC_COPY_EVENT,
                true);

        final WorkflowProcessor processor  =
                CopyActionletTest.workflowAPI.fireWorkflowPreCheckin(contentlet1, user);

        CopyActionletTest.workflowAPI.fireWorkflowPostCheckin(processor);

        Assert.assertNotNull(contentletCopy);
        Assert.assertNotNull(contentletCopy.getIdentifier());
        Assert.assertNotNull(contentletCopy.getInode());
        contentletAPI.isInodeIndexed(contentletCopy.getInode());

        final Contentlet contentlet2  = CopyActionletTest.contentletAPI.findContentletByIdentifier
                (CopyActionletTest.contentletCopy.getIdentifier(),
                        false, languageId, user, false);

        // the contentlet save by the action must be not null, should has a new version.
        Assert.assertNotNull(contentlet2);
        Assert.assertNotNull(contentlet2.getIdentifier());
        Assert.assertNotNull(contentlet2.getInode());
        Assert.assertFalse  (contentlet2.getIdentifier().equals(firstIdentifier));
        Assert.assertFalse  (contentlet2.getInode().equals(firstInode));
        Assert.assertEquals ("Test Save",      contentlet2.getStringProperty("title"));
        Assert.assertEquals ("Test Save Text", contentlet2.getStringProperty("txt"));

    }
}