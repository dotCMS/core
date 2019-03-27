package com.dotmarketing.portlets.workflow.business;

import com.dotcms.UnitTestBase;
import com.dotmarketing.portlets.workflows.business.ContentletStateOptions;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowStateFilter;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

import static com.dotcms.util.CollectionsUtils.set;

public class WorkflowStateFilterTest  extends UnitTestBase {

    @Test()
    public void testEmptyFilter() throws Exception {

        final WorkflowStateFilter workflowStateFilter = new WorkflowStateFilter();
        final WorkflowAction     action               = new WorkflowAction();
        final ContentletStateOptions optionsNew       = new ContentletStateOptions(true, false, false, true, true, WorkflowAPI.RenderMode.EDITING);

        action.setShowOn(Collections.emptySet());
        Assert.assertFalse(workflowStateFilter.filter(action, optionsNew));

        final ContentletStateOptions optionsPublished  = new ContentletStateOptions(false, false, false, true, false, WorkflowAPI.RenderMode.EDITING);

        action.setShowOn(Collections.emptySet());
        Assert.assertFalse(workflowStateFilter.filter(action, optionsPublished));
    }

    @Test()
    public void testNewFilter() throws Exception {

        final WorkflowStateFilter workflowStateFilter = new WorkflowStateFilter();
        final WorkflowAction     action               = new WorkflowAction();
        final ContentletStateOptions optionsNew       = new ContentletStateOptions(true, false, false, true, true, WorkflowAPI.RenderMode.EDITING);

        action.setShowOn(set(WorkflowState.NEW, WorkflowState.LOCKED, WorkflowState.EDITING));
        Assert.assertTrue(workflowStateFilter.filter(action, optionsNew));

        action.setShowOn(set(WorkflowState.NEW, WorkflowState.EDITING));
        Assert.assertFalse(workflowStateFilter.filter(action, optionsNew));

    }

    @Test()
    public void testPublishFilter() throws Exception {

        final WorkflowStateFilter workflowStateFilter = new WorkflowStateFilter();
        final WorkflowAction     action               = new WorkflowAction();
        final ContentletStateOptions optionsPublished = new ContentletStateOptions(false, true, false, true, true, WorkflowAPI.RenderMode.EDITING);

        action.setShowOn(set(WorkflowState.LOCKED, WorkflowState.PUBLISHED, WorkflowState.EDITING));
        Assert.assertTrue(workflowStateFilter.filter(action, optionsPublished));

        action.setShowOn(set(WorkflowState.PUBLISHED, WorkflowState.EDITING));
        Assert.assertFalse(workflowStateFilter.filter(action, optionsPublished));

        action.setShowOn(set(WorkflowState.LOCKED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING));
        Assert.assertFalse(workflowStateFilter.filter(action, optionsPublished));
    }

    @Test()
    public void testUnpublishFilter() throws Exception {

        final WorkflowStateFilter workflowStateFilter = new WorkflowStateFilter();
        final WorkflowAction     action               = new WorkflowAction();
        final ContentletStateOptions optionsUnpublished = new ContentletStateOptions(false, false, false, true, false, WorkflowAPI.RenderMode.EDITING);

        action.setShowOn(set(WorkflowState.UNLOCKED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING));
        Assert.assertTrue(workflowStateFilter.filter(action, optionsUnpublished));

        action.setShowOn(set(WorkflowState.PUBLISHED, WorkflowState.EDITING));
        Assert.assertFalse(workflowStateFilter.filter(action, optionsUnpublished));

        action.setShowOn(set(WorkflowState.LOCKED, WorkflowState.PUBLISHED, WorkflowState.EDITING));
        Assert.assertFalse(workflowStateFilter.filter(action, optionsUnpublished));
    }

    @Test()
    public void test_filter_no_editing_nor_listing_on_render_mode_false() throws Exception {

        final WorkflowStateFilter workflowStateFilter = new WorkflowStateFilter();
        final WorkflowAction     action               = new WorkflowAction();
        final ContentletStateOptions optionsArchive = new ContentletStateOptions(false, false, true, true, false, WorkflowAPI.RenderMode.EDITING);

        action.setShowOn(set(WorkflowState.UNLOCKED, WorkflowState.ARCHIVED));
        Assert.assertFalse(workflowStateFilter.filter(action, optionsArchive));

        action.setShowOn(set(WorkflowState.PUBLISHED));
        Assert.assertFalse(workflowStateFilter.filter(action, optionsArchive));

        action.setShowOn(set(WorkflowState.LOCKED, WorkflowState.PUBLISHED));
        Assert.assertFalse(workflowStateFilter.filter(action, optionsArchive));
    }

    @Test()
    public void test_filter_true_on_editing_render_mode() throws Exception {

        final WorkflowStateFilter workflowStateFilter = new WorkflowStateFilter();
        final WorkflowAction     action               = new WorkflowAction();
        final ContentletStateOptions optionsArchive = new ContentletStateOptions(false, false, true, true, false, WorkflowAPI.RenderMode.EDITING);

        action.setShowOn(set(WorkflowState.UNLOCKED, WorkflowState.ARCHIVED, WorkflowState.EDITING));
        Assert.assertTrue(workflowStateFilter.filter(action, optionsArchive));
    }

    @Test()
    public void test_filter_false_on_editing_render_mode() throws Exception {

        final WorkflowStateFilter workflowStateFilter = new WorkflowStateFilter();
        final WorkflowAction     action               = new WorkflowAction();
        final ContentletStateOptions optionsArchive = new ContentletStateOptions(false, false, true, true, false, WorkflowAPI.RenderMode.EDITING);

        action.setShowOn(set(WorkflowState.UNLOCKED, WorkflowState.ARCHIVED, WorkflowState.LISTING));
        Assert.assertFalse(workflowStateFilter.filter(action, optionsArchive));
    }

    @Test()
    public void test_filter_true_on_editing_render_mode_with_both_states() throws Exception {

        final WorkflowStateFilter workflowStateFilter = new WorkflowStateFilter();
        final WorkflowAction     action               = new WorkflowAction();
        final ContentletStateOptions optionsArchive = new ContentletStateOptions(false, false, true, true, false, WorkflowAPI.RenderMode.EDITING);

        action.setShowOn(set(WorkflowState.UNLOCKED, WorkflowState.ARCHIVED, WorkflowState.LISTING, WorkflowState.EDITING));
        Assert.assertTrue(workflowStateFilter.filter(action, optionsArchive));
    }

    /**
     * action.setShowOn(set(WorkflowState.UNLOCKED, WorkflowState.ARCHIVED, WorkflowState.LISTING));
     *         Assert.assertFalse(workflowStateFilter.filter(action, optionsArchive));
     *
     *         action.setShowOn(set(WorkflowState.PUBLISHED));
     *         Assert.assertFalse(workflowStateFilter.filter(action, optionsArchive));
     *
     *         action.setShowOn(set(WorkflowState.LOCKED, WorkflowState.PUBLISHED, WorkflowState.EDITING, WorkflowState.LISTING));
     *         Assert.assertTrue(workflowStateFilter.filter(action, optionsArchive));
     *
     *         action.setShowOn(set(WorkflowState.LOCKED, WorkflowState.PUBLISHED, WorkflowState.EDITING, WorkflowState.LISTING));
     *         Assert.assertTrue(workflowStateFilter.filter(action, optionsArchive));
     *
     *         action.setShowOn(set(WorkflowState.LOCKED, WorkflowState.PUBLISHED, WorkflowState.LISTING));
     *         Assert.assertFalse(workflowStateFilter.filter(action, optionsArchive));
     */
    @Test()
    public void test_filter_true_on_listing_render_mode() throws Exception {

        final WorkflowStateFilter workflowStateFilter = new WorkflowStateFilter();
        final WorkflowAction     action               = new WorkflowAction();
        final ContentletStateOptions optionsArchive = new ContentletStateOptions(false, false, true, true, false, WorkflowAPI.RenderMode.LISTING);

        action.setShowOn(set(WorkflowState.UNLOCKED, WorkflowState.ARCHIVED, WorkflowState.LISTING));
        Assert.assertTrue(workflowStateFilter.filter(action, optionsArchive));
    }

    @Test()
    public void test_filter_false_on_listing_render_mode() throws Exception {

        final WorkflowStateFilter workflowStateFilter = new WorkflowStateFilter();
        final WorkflowAction     action               = new WorkflowAction();
        final ContentletStateOptions optionsArchive = new ContentletStateOptions(false, false, true, true, false, WorkflowAPI.RenderMode.LISTING);

        action.setShowOn(set(WorkflowState.UNLOCKED, WorkflowState.ARCHIVED, WorkflowState.EDITING));
        Assert.assertFalse(workflowStateFilter.filter(action, optionsArchive));
    }

    @Test()
    public void test_filter_true_on_listing_render_mode_with_both_states() throws Exception {

        final WorkflowStateFilter workflowStateFilter = new WorkflowStateFilter();
        final WorkflowAction     action               = new WorkflowAction();
        final ContentletStateOptions optionsArchive = new ContentletStateOptions(false, false, true, true, false, WorkflowAPI.RenderMode.LISTING);

        action.setShowOn(set(WorkflowState.UNLOCKED, WorkflowState.ARCHIVED, WorkflowState.LISTING, WorkflowState.EDITING));
        Assert.assertTrue(workflowStateFilter.filter(action, optionsArchive));
    }
}