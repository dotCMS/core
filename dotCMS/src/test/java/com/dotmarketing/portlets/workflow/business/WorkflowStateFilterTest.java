package com.dotmarketing.portlets.workflow.business;

import com.dotcms.UnitTestBase;
import static com.dotcms.util.CollectionsUtils.*;
import com.dotmarketing.portlets.workflows.business.ContentletStateOptions;
import com.dotmarketing.portlets.workflows.business.WorkflowStateFilter;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class WorkflowStateFilterTest  extends UnitTestBase {

    @Test()
    public void testEmptyFilter() throws Exception {

        final WorkflowStateFilter workflowStateFilter = new WorkflowStateFilter();
        final WorkflowAction     action               = new WorkflowAction();
        final ContentletStateOptions optionsNew       = new ContentletStateOptions(true, false, false, true, true);

        action.setShowOn(Collections.emptySet());
        Assert.assertFalse(workflowStateFilter.filter(action, optionsNew));

        final ContentletStateOptions optionsPublished  = new ContentletStateOptions(false, false, false, true, false);

        action.setShowOn(Collections.emptySet());
        Assert.assertFalse(workflowStateFilter.filter(action, optionsPublished));
    }

    @Test()
    public void testNewFilter() throws Exception {

        final WorkflowStateFilter workflowStateFilter = new WorkflowStateFilter();
        final WorkflowAction     action               = new WorkflowAction();
        final ContentletStateOptions optionsNew       = new ContentletStateOptions(true, false, false, true, true);

        action.setShowOn(set(WorkflowState.NEW, WorkflowState.LOCKED));
        Assert.assertTrue(workflowStateFilter.filter(action, optionsNew));

        action.setShowOn(set(WorkflowState.NEW));
        Assert.assertFalse(workflowStateFilter.filter(action, optionsNew));

    }

    @Test()
    public void testPublishFilter() throws Exception {

        final WorkflowStateFilter workflowStateFilter = new WorkflowStateFilter();
        final WorkflowAction     action               = new WorkflowAction();
        final ContentletStateOptions optionsPublished = new ContentletStateOptions(false, true, false, true, true);

        action.setShowOn(set(WorkflowState.LOCKED, WorkflowState.PUBLISHED));
        Assert.assertTrue(workflowStateFilter.filter(action, optionsPublished));

        action.setShowOn(set(WorkflowState.PUBLISHED));
        Assert.assertFalse(workflowStateFilter.filter(action, optionsPublished));

        action.setShowOn(set(WorkflowState.LOCKED, WorkflowState.UNPUBLISHED));
        Assert.assertFalse(workflowStateFilter.filter(action, optionsPublished));
    }

    @Test()
    public void testUnpublishFilter() throws Exception {

        final WorkflowStateFilter workflowStateFilter = new WorkflowStateFilter();
        final WorkflowAction     action               = new WorkflowAction();
        final ContentletStateOptions optionsUnpublished = new ContentletStateOptions(false, false, false, true, false);

        action.setShowOn(set(WorkflowState.UNLOCKED, WorkflowState.UNPUBLISHED));
        Assert.assertTrue(workflowStateFilter.filter(action, optionsUnpublished));

        action.setShowOn(set(WorkflowState.PUBLISHED));
        Assert.assertFalse(workflowStateFilter.filter(action, optionsUnpublished));

        action.setShowOn(set(WorkflowState.LOCKED, WorkflowState.PUBLISHED));
        Assert.assertFalse(workflowStateFilter.filter(action, optionsUnpublished));
    }

    @Test()
    public void testArchiveFilter() throws Exception {

        final WorkflowStateFilter workflowStateFilter = new WorkflowStateFilter();
        final WorkflowAction     action               = new WorkflowAction();
        final ContentletStateOptions optionsArchive = new ContentletStateOptions(false, false, true, true, false);

        action.setShowOn(set(WorkflowState.UNLOCKED, WorkflowState.ARCHIVED));
        Assert.assertTrue(workflowStateFilter.filter(action, optionsArchive));

        action.setShowOn(set(WorkflowState.PUBLISHED));
        Assert.assertFalse(workflowStateFilter.filter(action, optionsArchive));

        action.setShowOn(set(WorkflowState.LOCKED, WorkflowState.PUBLISHED));
        Assert.assertFalse(workflowStateFilter.filter(action, optionsArchive));
    }
}