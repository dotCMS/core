package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.sql.SQLException;
import java.util.Date;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task05200WorkflowTaskUniqueKeyTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private void removeConstraintIfAny() throws DotDataException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        //Certain db engines store unique constraints as indices
        final DotConnect dotConnect = new DotConnect();
        try {
           dotConnect.setSQL("alter table workflow_task drop index unique_workflow_task");
           dotConnect.loadResult();
       }catch (DotDataException e){
           //Nah.
       }
        try {
            dotConnect.setSQL("alter table workflow_task drop constraint unique_workflow_task");
            dotConnect.loadResult();
        }catch (DotDataException e){
            //Nah.
        }
    }

    @Test
    public void Test_Upgrade_Task() throws DotDataException {
        removeConstraintIfAny();
        final Task05200WorkflowTaskUniqueKey task =  new Task05200WorkflowTaskUniqueKey();
        assertTrue(task.forceRun());
        task.executeUpgrade();
    }

    @Test
    public void Test_Upgrade_Task_When_Duped_WorkflowTasks_Exists() throws DotDataException, DotSecurityException {

        removeConstraintIfAny();

        final User systemUser = APILocator.systemUser();
        final Language language = APILocator.getLanguageAPI().getDefaultLanguage();
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();

        final Contentlet contentlet = TestDataUtils.getPageContent(true, language.getId());

        //save workflow task
        final WorkflowStep workflowStep           = workflowAPI.findStep(
                SystemWorkflowConstants.WORKFLOW_NEW_STEP_ID);
        workflowAPI.deleteWorkflowTaskByContentletIdAnyLanguage(contentlet, systemUser);
        final WorkflowTask workflowTask = workflowAPI
                .createWorkflowTask(contentlet, systemUser, workflowStep, "test", "test");
        workflowAPI.saveWorkflowTask(workflowTask);

        //save workflow comment
        final WorkflowComment comment = new WorkflowComment();
        comment.setComment(workflowTask.getDescription());
        comment.setCreationDate(new Date());
        comment.setPostedBy(systemUser.getUserId());
        comment.setWorkflowtaskId(workflowTask.getId());
        workflowAPI.saveComment(comment);

        //save workflow history
        final WorkflowHistory hist = new WorkflowHistory ();
        hist.setChangeDescription("workflow history description");
        hist.setCreationDate(new Date());
        hist.setMadeBy(systemUser.getUserId());
        hist.setWorkflowtaskId(workflowTask.getId());
        workflowAPI.saveWorkflowHistory(hist);

        //save workflow task file
        final Contentlet fileAsset = TestDataUtils.getFileAssetContent(true, language.getId());
        workflowAPI.attachFileToTask(workflowTask, fileAsset.getInode());

        //duplicate the workflow task
        final WorkflowTask dupeWorkflowTask = workflowAPI
                .createWorkflowTask(contentlet, systemUser, workflowStep, "test", "test");

        saveWorkflowTask(dupeWorkflowTask);

        assertTrue(dupeWorkflowTask != null && dupeWorkflowTask.getId() != null);
        assertEquals(workflowTask.getWebasset(), dupeWorkflowTask.getWebasset());
        assertEquals(workflowTask.getLanguageId(), dupeWorkflowTask.getLanguageId());

        final Task05200WorkflowTaskUniqueKey task =  new Task05200WorkflowTaskUniqueKey();
        assertTrue(task.forceRun());
        task.executeUpgrade();

        //verify the first task and its dependencies have been deleted
        final WorkflowTask result = workflowAPI.findTaskById(workflowTask.getId());
        assertNull(result.getId());

        assertFalse(UtilMethods.isSet(workflowAPI.findWorkFlowComments(workflowTask)));
        assertFalse(UtilMethods.isSet(workflowAPI.findWorkflowHistory(workflowTask)));
        assertFalse(UtilMethods.isSet(workflowAPI.findWorkflowTaskFilesAsContent(workflowTask, systemUser)));

        //verify the newest workflow task exists
        assertEquals(dupeWorkflowTask.getId(), workflowAPI.findTaskById(dupeWorkflowTask.getId()).getId());
    }

    static String INSERT_WORKFLOW_TASK = "INSERT INTO workflow_task (id, creation_date, mod_date, due_date, created_by, assigned_to, belongs_to, title, description, status, webasset, language_id) values (?,?,?,?,?,?,?,?,?,?,?,?)";

    private void saveWorkflowTask(final WorkflowTask task) throws DotDataException {
        task.setId(UUIDGenerator.generateUuid());

        final DotConnect db = new DotConnect()
            .setSQL(INSERT_WORKFLOW_TASK)
            .addParam(task.getId())
            .addParam(task.getCreationDate())
            .addParam(task.getModDate())
            .addParam(task.getDueDate())
            .addParam(task.getCreatedBy())
            .addParam(task.getAssignedTo())
            .addParam(task.getBelongsTo())
            .addParam(task.getTitle())
            .addParam(task.getDescription())
            .addParam(task.getStatus())
            .addParam(task.getWebasset())
            .addParam(task.getLanguageId());

        db.loadResult();
    }

    @Test
    public void Test_Upgrade_Task_When_Duped_With_Null_Languages_WorkflowTasks_Exists() throws DotDataException, DotSecurityException {

        removeConstraintIfAny();

        final User systemUser = APILocator.systemUser();
        final Language language = APILocator.getLanguageAPI().getDefaultLanguage();
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();

        final Contentlet contentlet = TestDataUtils.getPageContent(true, language.getId());

        //save workflow task
        final WorkflowStep workflowStep = workflowAPI.findStep(SystemWorkflowConstants.WORKFLOW_NEW_STEP_ID);
        workflowAPI.deleteWorkflowTaskByContentletIdAnyLanguage(contentlet, systemUser);
        final WorkflowTask workflowTask = workflowAPI.createWorkflowTask(
                contentlet,
                systemUser,
                workflowStep,
                "test",
                "test");
        workflowAPI.saveWorkflowTask(workflowTask);

        new DotConnect()
                .setSQL("UPDATE workflow_task SET language_id=null WHERE id=?")
                .addParam(workflowTask.getId())
                .loadResult();

        //save workflow comment
        final WorkflowComment comment = new WorkflowComment();
        comment.setComment(workflowTask.getDescription());
        comment.setCreationDate(new Date());
        comment.setPostedBy(systemUser.getUserId());
        comment.setWorkflowtaskId(workflowTask.getId());
        workflowAPI.saveComment(comment);

        //save workflow history
        final WorkflowHistory hist = new WorkflowHistory ();
        hist.setChangeDescription("workflow history description");
        hist.setCreationDate(new Date());
        hist.setMadeBy(systemUser.getUserId());
        hist.setWorkflowtaskId(workflowTask.getId());
        workflowAPI.saveWorkflowHistory(hist);

        //save workflow task file
        final Contentlet fileAsset = TestDataUtils.getFileAssetContent(true, language.getId());
        workflowAPI.attachFileToTask(workflowTask, fileAsset.getInode());

        //duplicate the workflow task
        final WorkflowTask dupeWorkflowTask = workflowAPI.createWorkflowTask(
                contentlet,
                systemUser,
                workflowStep,
                "test",
                "test");

        workflowAPI.saveWorkflowTask(dupeWorkflowTask);

        new DotConnect()
                .setSQL("UPDATE workflow_task SET language_id=null WHERE id=?")
                .addParam(dupeWorkflowTask.getId())
                .loadResult();

        assertTrue(dupeWorkflowTask != null && dupeWorkflowTask.getId() != null);
        assertEquals(workflowTask.getWebasset(), dupeWorkflowTask.getWebasset());
        assertEquals(workflowTask.getLanguageId(), dupeWorkflowTask.getLanguageId());

        final Task05200WorkflowTaskUniqueKey task = new Task05200WorkflowTaskUniqueKey();
        assertTrue(task.forceRun());
        task.executeUpgrade();

        //verify the first task and its dependencies have been deleted
        final WorkflowTask WTResult = workflowAPI.findTaskById(workflowTask.getId());
        final WorkflowTask dupeWTResult = workflowAPI.findTaskById(dupeWorkflowTask.getId());
        assertTrue(WTResult.getId()==null || dupeWTResult.getId()==null);

        assertFalse(UtilMethods.isSet(workflowAPI.findWorkFlowComments(dupeWorkflowTask)));
        assertFalse(UtilMethods.isSet(workflowAPI.findWorkflowHistory(dupeWorkflowTask)));
        assertFalse(UtilMethods.isSet(workflowAPI.findWorkflowTaskFilesAsContent(dupeWorkflowTask, systemUser)));
    }

}
