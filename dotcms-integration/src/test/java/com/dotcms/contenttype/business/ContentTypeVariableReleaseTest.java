package com.dotcms.contenttype.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Covers what a caller sees right after asking for a Content Type to be deleted: the variable is
 * free straight away, even though removing the content it held runs in the background, and the
 * workflow system action mappings that were keyed by that variable are gone before anybody else
 * can claim it.
 *
 * @author dotCMS
 */
public class ContentTypeVariableReleaseTest extends IntegrationTestBase {

    private static ContentTypeAPI contentTypeAPI;
    private static WorkflowAPI workflowAPI;
    private static User systemUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
        workflowAPI = APILocator.getWorkflowAPI();
    }

    /**
     * Given a Content Type that holds content, when it is deleted and a new Content Type with the
     * same variable is created immediately afterwards, then the creation succeeds. Before the
     * variable was released inside the deletion's own transaction this failed with
     * {@code Invalid content type variable}, because the row being deleted still carried the name
     * while its contentlets were relocated in the background.
     */
    @Test
    public void delete_releasesTheVariableRightAway() throws Exception {

        final String variable = uniqueVariable();
        final ContentType original = newType(variable);
        new ContentletDataGen(original.id()).nextPersisted();

        contentTypeAPI.delete(original);

        final ContentType replacement = newType(variable);
        try {
            assertEquals("The new Content Type should hold the released variable",
                    variable, replacement.variable());
            assertNotEquals("The new Content Type should be a different one",
                    original.id(), replacement.id());
        } finally {
            contentTypeAPI.delete(replacement);
        }
    }

    /**
     * Given a Content Type with a system action mapping, when it is deleted, then the mapping is
     * already gone by the time the call returns. The mappings are stored against the variable, so
     * they cannot be left for the background disposal to clean up: by then the variable may belong
     * to somebody else.
     */
    @Test
    public void delete_removesTheSystemActionMappingsBeforeReturning() throws Exception {

        final String variable = uniqueVariable();
        final ContentType type = newType(variable);
        mapNewAction(type);
        assertEquals("The mapping should exist before the delete", 1, countMappings(variable));

        contentTypeAPI.delete(type);

        assertEquals("The mapping should be gone as soon as the delete returns",
                0, countMappings(variable));
    }

    /**
     * Given a Content Type that was deleted and a new one that took its variable, when the
     * background disposal of the old one finishes, then the new Content Type keeps its own system
     * action mappings. This is the failure mode that releasing the variable early would otherwise
     * introduce: a cleanup keyed by name, running after somebody else owns the name.
     */
    @Test
    public void delete_leavesTheMappingsOfWhoeverTakesTheVariableAlone() throws Exception {

        final String variable = uniqueVariable();
        final ContentType original = newType(variable);
        new ContentletDataGen(original.id()).nextPersisted();
        mapNewAction(original);

        contentTypeAPI.delete(original);

        final ContentType replacement = newType(variable);
        try {
            mapNewAction(replacement);
            waitUntilGone(original.inode());

            assertEquals("The replacement should still own exactly one mapping",
                    1, countMappings(variable));
        } finally {
            contentTypeAPI.delete(replacement);
        }
    }

    /**
     * Creates a simple Content Type carrying the given variable.
     *
     * @param variable the velocity variable the new {@link ContentType} gets
     * @return the persisted {@link ContentType}
     */
    private static ContentType newType(final String variable) {
        return new ContentTypeDataGen().name(variable).velocityVarName(variable).nextPersisted();
    }

    /**
     * Builds a variable that no other Content Type in the database can be holding.
     *
     * @return a unique velocity variable
     */
    private static String uniqueVariable() {
        return "releaseVarTest" + UUIDGenerator.generateUuid().replace("-", "");
    }

    /**
     * Points the Content Type's {@code NEW} system action at the first action of the System
     * Workflow, which is enough to put a row in {@code workflow_action_mappings}.
     *
     * @param contentType the {@link ContentType} getting the mapping
     * @throws DotDataException if the mapping cannot be stored
     */
    private static void mapNewAction(final ContentType contentType) throws Exception {
        final WorkflowScheme systemWorkflow = workflowAPI.findSystemWorkflowScheme();
        final List<WorkflowAction> actions = workflowAPI.findActions(systemWorkflow, systemUser);
        assertTrue("The System Workflow should expose at least one action", !actions.isEmpty());
        workflowAPI.mapSystemActionToWorkflowActionForContentType(
                WorkflowAPI.SystemAction.NEW, actions.get(0), contentType);
    }

    /**
     * Counts the system action mappings stored under a Content Type variable. Read straight from
     * the table because the mappings outlive the Content Type object they belonged to, which is
     * the whole point of these tests.
     *
     * @param variable the velocity variable the mappings are keyed by
     * @return how many mappings carry that variable
     * @throws DotDataException if the query fails
     */
    private static int countMappings(final String variable) throws DotDataException {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("select count(*) as mapping_count from workflow_action_mappings "
                        + "where scheme_or_content_type = ?")
                .addParam(variable)
                .loadObjectResults();
        return Integer.parseInt(rows.get(0).get("mapping_count").toString());
    }

    /**
     * Waits, within reason, for the background disposal to remove the Content Type row. The
     * relocation runs on a single-threaded pool shared with every other pending deletion, so how
     * long it takes is not something the test can predict.
     *
     * @param inode the inode of the {@link ContentType} being disposed of
     * @throws DotDataException if the lookup fails
     */
    private static void waitUntilGone(final String inode) throws Exception {
        final long deadline = System.currentTimeMillis() + 60_000L;
        while (System.currentTimeMillis() < deadline) {
            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL("select inode from structure where inode = ?")
                    .addParam(inode)
                    .loadObjectResults();
            if (rows.isEmpty()) {
                return;
            }
            Thread.sleep(250L);
        }
        throw new IllegalStateException(
                "The Content Type with inode " + inode + " was never disposed of");
    }
}
