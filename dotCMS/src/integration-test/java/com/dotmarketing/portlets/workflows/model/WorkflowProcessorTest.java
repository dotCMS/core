package com.dotmarketing.portlets.workflows.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.liferay.portal.model.User;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;

public class WorkflowProcessorTest extends BaseWorkflowIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Test that verifies that the attribute history remains null until we call the getHistory method
     * Method to test {@link WorkflowProcessor#getHistory()}
     * @throws DotDataException
     */
    @Test
    public void Test_Load_Workflow_History_Lazily () throws DotDataException {

        //First create all the context
        final User systemUser = APILocator.systemUser();
        final Language language = APILocator.getLanguageAPI().getDefaultLanguage();
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();

        final WorkflowScheme systemWorkflowScheme = workflowAPI.findSystemWorkflowScheme();
        final ContentType contentType = new ContentTypeDataGen()
                .workflowId(systemWorkflowScheme.getId())
                .nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .languageId(language.getId())
                .nextPersisted();

        assertNotNull(contentlet);

        final WorkflowTask workflowTask = workflowAPI.findTaskByContentlet(contentlet);
        assertNotNull(workflowTask);

        final int max = 5;
        final Set<String> savedWorkflowIds = new HashSet<>(max);

        for (int i = 0; i < max; i++) {
            final WorkflowHistory workflowHistory = new WorkflowHistory();
            workflowHistory.setChangeDescription("workflow history description");
            workflowHistory.setCreationDate(new Date());
            workflowHistory.setMadeBy(systemUser.getUserId());
            workflowHistory.setWorkflowtaskId(workflowTask.getId());
            workflowAPI.saveWorkflowHistory(workflowHistory);
            savedWorkflowIds.add(workflowHistory.getId());
        }

        //Then the actual test begins here
        final WorkflowProcessor processor = new WorkflowProcessor(contentlet, systemUser);
        assertNotNull(processor.getTask());
        assertEquals(workflowTask.getId(), processor.getTask().getId());
        assertNull(processor.history);
        final List<WorkflowHistory> workflowHistory = processor.getHistory();
        assertEquals(workflowHistory.size(), max);
        for (final WorkflowHistory entry : workflowHistory) {
            assertTrue(savedWorkflowIds.contains(entry.getId()));
        }
    }

}
