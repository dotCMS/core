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
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentAsDraftActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
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

/**
 * Test the workflowAPI and multi language
 * @author jsanca
 */
public class WorkflowAPIMultiLanguageTest extends BaseWorkflowIntegrationTest {

    private static CreateSchemeStepActionResult schemeStepActionResult1 = null;
    private static WorkflowStep                 workflowStep2           = null;
    private static WorkflowStep                 workflowStep3           = null;
    private static WorkflowAction               workflowAction2         = null;
    private static WorkflowAPI                  workflowAPI             = null;
    private static ContentletAPI contentletAPI                          = null;
    private static ContentType type                                     = null;
    private static List<Contentlet> contentlet                          = new ArrayList<>();


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        WorkflowAPIMultiLanguageTest.workflowAPI              = APILocator.getWorkflowAPI();
        final ContentTypeAPI contentTypeAPI               = APILocator.getContentTypeAPI(APILocator.systemUser());
        WorkflowAPIMultiLanguageTest.contentletAPI            = APILocator.getContentletAPI();

        /*
        Creates a scheme, with 3 steps

        step1
            action 1 -> go to step 2
            action 2 -> go to step 3

        step2
        step3
         */

        // creates the scheme, step1 and action1
        WorkflowAPIMultiLanguageTest.schemeStepActionResult1 = WorkflowAPIMultiLanguageTest.createSchemeStepActionActionlet
                ("MultiLanguageScheme" + UUIDGenerator.generateUuid(), "step1", "action1", SaveContentActionlet.class);

        // creates the step2 to the same scheme
        WorkflowAPIMultiLanguageTest.workflowStep2 =
                createNewWorkflowStep("step2", WorkflowAPIMultiLanguageTest.schemeStepActionResult1.getScheme().getId());

        // creates the step2 to the same scheme
        WorkflowAPIMultiLanguageTest.workflowStep3 =
                createNewWorkflowStep("step3", WorkflowAPIMultiLanguageTest.schemeStepActionResult1.getScheme().getId());

        // set the next step to step2 for the action 1
        WorkflowAPIMultiLanguageTest.schemeStepActionResult1.getAction().setNextStep(WorkflowAPIMultiLanguageTest.workflowStep2.getId());
        workflowAPI.saveAction(WorkflowAPIMultiLanguageTest.schemeStepActionResult1.getAction(), null, APILocator.systemUser());

        // creates action 2, for the step 1, the next step will be step3
        WorkflowAPIMultiLanguageTest.workflowAction2 =
                createActionActionlet (WorkflowAPIMultiLanguageTest.schemeStepActionResult1.getScheme().getId(),
                    WorkflowAPIMultiLanguageTest.schemeStepActionResult1.getStep().getId(),
                    "action2",
                    SaveContentAsDraftActionlet.class,
                    WorkflowAPIMultiLanguageTest.workflowStep3.getId()).getAction();

        // creates the type to trigger the scheme
        WorkflowAPIMultiLanguageTest.createTestType(contentTypeAPI);

        // associated the scheme to the type
        WorkflowAPIMultiLanguageTest.workflowAPI.saveSchemesForStruct(new StructureTransformer(WorkflowAPIMultiLanguageTest.type).asStructure(),
                Arrays.asList(WorkflowAPIMultiLanguageTest.schemeStepActionResult1.getScheme()));

    }

    private static void createTestType(final ContentTypeAPI contentTypeAPI) throws DotDataException, DotSecurityException {

        WorkflowAPIMultiLanguageTest.type = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                        .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .description("DotWorkflowAPIMultiLanguageTest...")
                        .name("DotWorkflowAPIMultiLanguageTest").owner(APILocator.systemUser().toString())
                        .variable("DotWorkflowAPIMultiLanguageTest").build());

        final List<Field> fields = new ArrayList<>(WorkflowAPIMultiLanguageTest.type.fields());

        fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                .contentTypeId(WorkflowAPIMultiLanguageTest.type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                .contentTypeId(WorkflowAPIMultiLanguageTest.type.id()).dataType(DataTypes.TEXT).indexed(true).build());

        WorkflowAPIMultiLanguageTest.type = contentTypeAPI.save(WorkflowAPIMultiLanguageTest.type, fields);
    }

    @Test
    public void runMultipleLanguageWorkflowTest() throws DotDataException, DotSecurityException {

        final Contentlet contentletEng = new Contentlet();
        final User user                = APILocator.systemUser();
        contentletEng.setContentTypeId(WorkflowAPIMultiLanguageTest.type.id());
        contentletEng.setOwner(APILocator.systemUser().toString());
        contentletEng.setModDate(new Date());
        contentletEng.setLanguageId(this.getEnglishLanguageId ());
        contentletEng.setStringProperty("title", "Test Save");
        contentletEng.setStringProperty("txt",   "Test Save Text");
        contentletEng.setHost(Host.SYSTEM_HOST);
        contentletEng.setFolder(FolderAPI.SYSTEM_FOLDER);
        contentletEng.setIndexPolicy(IndexPolicy.FORCE);

        // first save
        final Contentlet contentletEng1          = WorkflowAPIMultiLanguageTest.contentletAPI.checkin(contentletEng, user, false);
        WorkflowAPIMultiLanguageTest.contentlet.add(contentletEng1); // save it to remove later



        final Contentlet contentletSpanish      = new Contentlet();
        contentletSpanish.setContentTypeId(WorkflowAPIMultiLanguageTest.type.id());
        contentletSpanish.setOwner(APILocator.systemUser().toString());
        contentletSpanish.setModDate(new Date());
        contentletSpanish.setLanguageId(this.getSpanishLanguageId());
        contentletSpanish.setStringProperty("title", "Prueba Salvar");
        contentletSpanish.setStringProperty("txt",   "Prueba Salvar Texto");
        contentletSpanish.setHost(Host.SYSTEM_HOST);
        contentletSpanish.setFolder(FolderAPI.SYSTEM_FOLDER);
        contentletSpanish.setIdentifier(contentletEng1.getIdentifier());
        contentletSpanish.setIndexPolicy(IndexPolicy.FORCE);

        // second save spanish
        final Contentlet contentletSpanish1      = WorkflowAPIMultiLanguageTest.contentletAPI.checkin(contentletSpanish, user, false);
        WorkflowAPIMultiLanguageTest.contentlet.add(contentletSpanish1);

        // triggering the save content action (action1) for the english content
        contentletEng1.setActionId(
                WorkflowAPIMultiLanguageTest.schemeStepActionResult1.getAction().getId());
        contentletEng1.setIndexPolicy(IndexPolicy.FORCE);

        final WorkflowProcessor processor1  =
                WorkflowAPIMultiLanguageTest.workflowAPI.fireWorkflowPreCheckin(contentletEng1, user);

        WorkflowAPIMultiLanguageTest.workflowAPI.fireWorkflowPostCheckin(processor1);

        // triggering the save content as draft action (action 2) for the spanish content (
        contentletSpanish1.setActionId(
                WorkflowAPIMultiLanguageTest.workflowAction2.getId());
        contentletSpanish1.setIndexPolicy(IndexPolicy.FORCE);

        final WorkflowProcessor processor2  =
                WorkflowAPIMultiLanguageTest.workflowAPI.fireWorkflowPreCheckin(contentletSpanish1, user);

        WorkflowAPIMultiLanguageTest.workflowAPI.fireWorkflowPostCheckin(processor2);


        // the contentletEng save by the action must be not null, should has a new version.
        Assert.assertNotNull   (processor1.getContentlet());
        Assert.assertNotNull   (processor2.getContentlet());
        Assert.assertEquals    (processor1.getContentlet().getIdentifier(),      processor2.getContentlet().getIdentifier());
        Assert.assertNotEquals (processor1.getContentlet().getInode(),           processor2.getContentlet().getInode());


        Assert.assertEquals ("Test Save",      processor1.getContentlet().getStringProperty("title"));
        Assert.assertEquals ("Test Save Text", processor1.getContentlet().getStringProperty("txt"));

        Assert.assertEquals ("Prueba Salvar",       processor2.getContentlet().getStringProperty("title"));
        Assert.assertEquals ("Prueba Salvar Texto", processor2.getContentlet().getStringProperty("txt"));

        final  List<WorkflowStep> workflowSteps1 = workflowAPI.findStepsByContentlet(processor1.getContentlet());
        final  List<WorkflowStep> workflowSteps2 = workflowAPI.findStepsByContentlet(processor2.getContentlet());

        Assert.assertNotNull   (workflowSteps1); // should be step 2
        Assert.assertNotNull   (workflowSteps2); // should be step 3


        Assert.assertTrue      (workflowSteps1.size() == 1); // should be step 2
        Assert.assertTrue      (workflowSteps2.size() == 1); // should be step 3

        Assert.assertEquals (workflowSteps1.get(0).getId(),       WorkflowAPIMultiLanguageTest.workflowStep2.getId());
        Assert.assertEquals (workflowSteps2.get(0).getId(),       WorkflowAPIMultiLanguageTest.workflowStep3.getId());
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup() throws DotDataException, DotSecurityException, AlreadyExistException {

        try {

            if (WorkflowAPIMultiLanguageTest.contentlet.size() > 0) {

                WorkflowAPIMultiLanguageTest.contentletAPI.archive(WorkflowAPIMultiLanguageTest.contentlet, APILocator.systemUser(), false);
                WorkflowAPIMultiLanguageTest.contentletAPI.delete(WorkflowAPIMultiLanguageTest.contentlet, APILocator.systemUser(), false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            try {

                if (null != WorkflowAPIMultiLanguageTest.schemeStepActionResult1) {

                    WorkflowAPIMultiLanguageTest.cleanScheme(WorkflowAPIMultiLanguageTest.schemeStepActionResult1.getScheme());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {

                try {
                    if (null != WorkflowAPIMultiLanguageTest.type) {

                        ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
                        contentTypeAPI.delete(WorkflowAPIMultiLanguageTest.type);
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    } // cleanup



}
