package com.dotcms.enterprise.publishing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

public class PublishDateUpdaterTest {

    private static final String FLAG_KEY = "PUBLISH_JOB_QUEUE_RESPECT_WORKFLOW_RESOLUTION";
    private static final String SCHEMES_KEY =
            "PUBLISH_JOB_QUEUE_RESPECT_WORKFLOW_RESOLUTION_SCHEMES";
    private static final String CONTENTLET_IDENTIFIER = "contentlet-id";
    private static final String WORKFLOW_SCHEME_ID = "workflow-scheme-id";

    private ContentletAPI contentletAPI;
    private WorkflowAPI workflowAPI;
    private Contentlet contentlet;
    private User systemUser;
    private PublishDateUpdater.Publisher publisher;
    private MockedStatic<Config> configMock;

    @Before
    public void setUp() {
        contentletAPI = mock(ContentletAPI.class);
        workflowAPI = mock(WorkflowAPI.class);
        contentlet = mock(Contentlet.class);
        systemUser = mock(User.class);
        publisher = new PublishDateUpdater.Publisher(contentletAPI, workflowAPI);
        when(contentlet.getIdentifier()).thenReturn(CONTENTLET_IDENTIFIER);

        configMock = mockStatic(Config.class);
        configMock.when(() -> Config.getBooleanProperty(eq(FLAG_KEY), anyBoolean()))
                .thenReturn(false);
        configMock.when(() -> Config.getStringProperty(eq(SCHEMES_KEY), eq("")))
                .thenReturn("");
    }

    @After
    public void tearDown() {
        configMock.close();
    }

    @Test
    public void publisherPreservesHistoricalBehaviorWhenFlagIsDisabled() throws Exception {
        when(workflowAPI.findCurrentStep(contentlet))
                .thenThrow(new DotDataException("workflow must not be queried"));

        assertTrue(publisher.process(contentlet, systemUser));

        verify(workflowAPI, never()).findCurrentStep(contentlet);
        verify(contentletAPI).publish(contentlet, systemUser, false);
    }

    @Test
    public void publisherBlocksForcedScheduledPublishAtUnresolvedStep() throws Exception {
        enableWorkflowResolutionCheck();
        when(workflowAPI.findCurrentStep(contentlet))
                .thenReturn(Optional.of(step("review", false)));

        assertFalse(publisher.process(contentlet, systemUser));

        verify(contentletAPI, never()).publish(contentlet, systemUser, false);
    }

    @Test
    public void publisherAllowsScheduledPublishAtResolvedStep() throws Exception {
        enableWorkflowResolutionCheck();
        when(workflowAPI.findCurrentStep(contentlet))
                .thenReturn(Optional.of(step("approved", true)));

        assertTrue(publisher.process(contentlet, systemUser));

        verify(contentletAPI).publish(contentlet, systemUser, false);
    }

    @Test
    public void publisherBlocksScheduledPublishWithoutCurrentStep() throws Exception {
        enableWorkflowResolutionCheck();
        when(workflowAPI.findCurrentStep(contentlet)).thenReturn(Optional.empty());

        assertFalse(publisher.process(contentlet, systemUser));

        verify(contentletAPI, never()).publish(contentlet, systemUser, false);
    }

    @Test
    public void publisherBlocksUnresolvedStepForConfiguredWorkflowVariable() throws Exception {
        enableWorkflowResolutionCheck();
        configureWorkflowSchemes("otherWorkflow, targetWorkflow");
        when(workflowAPI.findCurrentStep(contentlet))
                .thenReturn(Optional.of(step("review", WORKFLOW_SCHEME_ID, false)));
        when(workflowAPI.findScheme(WORKFLOW_SCHEME_ID))
                .thenReturn(scheme(WORKFLOW_SCHEME_ID, "targetWorkflow"));

        assertFalse(publisher.process(contentlet, systemUser));

        verify(contentletAPI, never()).publish(contentlet, systemUser, false);
    }

    @Test
    public void publisherPreservesScheduledPublishForWorkflowOutsideConfiguredList()
            throws Exception {
        enableWorkflowResolutionCheck();
        configureWorkflowSchemes("targetWorkflow");
        when(workflowAPI.findCurrentStep(contentlet))
                .thenReturn(Optional.of(step("review", WORKFLOW_SCHEME_ID, false)));
        when(workflowAPI.findScheme(WORKFLOW_SCHEME_ID))
                .thenReturn(scheme(WORKFLOW_SCHEME_ID, "differentWorkflow"));

        assertTrue(publisher.process(contentlet, systemUser));

        verify(contentletAPI).publish(contentlet, systemUser, false);
    }

    @Test
    public void publisherBlocksContentWithoutStepWhenConfiguredWorkflowIsAssigned()
            throws Exception {
        enableWorkflowResolutionCheck();
        configureWorkflowSchemes("targetWorkflow");
        final ContentType contentType = mock(ContentType.class);
        when(contentlet.getContentType()).thenReturn(contentType);
        when(workflowAPI.findCurrentStep(contentlet)).thenReturn(Optional.empty());
        when(workflowAPI.findSchemesForContentType(contentType))
                .thenReturn(List.of(scheme(WORKFLOW_SCHEME_ID, "targetWorkflow")));

        assertFalse(publisher.process(contentlet, systemUser));

        verify(contentletAPI, never()).publish(contentlet, systemUser, false);
    }

    @Test
    public void publisherAllowsContentWithoutStepWhenConfiguredWorkflowIsNotAssigned()
            throws Exception {
        enableWorkflowResolutionCheck();
        configureWorkflowSchemes("targetWorkflow");
        final ContentType contentType = mock(ContentType.class);
        when(contentlet.getContentType()).thenReturn(contentType);
        when(workflowAPI.findCurrentStep(contentlet)).thenReturn(Optional.empty());
        when(workflowAPI.findSchemesForContentType(contentType))
                .thenReturn(List.of(scheme(WORKFLOW_SCHEME_ID, "differentWorkflow")));

        assertTrue(publisher.process(contentlet, systemUser));

        verify(contentletAPI).publish(contentlet, systemUser, false);
    }

    @Test
    public void publisherFailsClosedWhenWorkflowLookupFails() throws Exception {
        enableWorkflowResolutionCheck();
        when(workflowAPI.findCurrentStep(contentlet))
                .thenThrow(new DotDataException("workflow unavailable"));

        assertThrows(DotDataException.class, () -> publisher.process(contentlet, systemUser));

        verify(contentletAPI, never()).publish(contentlet, systemUser, false);
    }

    @Test
    public void unpublisherIsNotBlockedWhenWorkflowResolutionFlagIsEnabled() throws Exception {
        enableWorkflowResolutionCheck();
        final PublishDateUpdater.Unpublisher unpublisher =
                new PublishDateUpdater.Unpublisher(contentletAPI);

        assertTrue(unpublisher.process(contentlet, systemUser));

        verify(contentletAPI).unpublish(contentlet, systemUser, false);
    }

    @Test
    public void batchClosesTransactionWhenProcessorFails() throws Exception {
        final UserAPI userAPI = mock(UserAPI.class);
        final ContentletSearch searchResult = mock(ContentletSearch.class);
        final PublishDateUpdater.ContentProcessor processor =
                mock(PublishDateUpdater.ContentProcessor.class);
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(searchResult.getInode()).thenReturn("failed-inode");
        when(contentletAPI.searchIndex(anyString(), anyInt(), anyInt(), isNull(),
                eq(systemUser), eq(false))).thenReturn(List.of(searchResult));
        when(contentletAPI.find("failed-inode", systemUser, false)).thenReturn(contentlet);
        when(processor.getOperationName()).thenReturn("publish");
        when(processor.process(contentlet, systemUser))
                .thenThrow(new DotDataException("workflow unavailable"));

        try (MockedStatic<APILocator> apiLocatorMock = mockStatic(APILocator.class);
                MockedStatic<HibernateUtil> hibernateMock = mockStatic(HibernateUtil.class)) {
            apiLocatorMock.when(APILocator::getUserAPI).thenReturn(userAPI);
            apiLocatorMock.when(APILocator::getContentletAPI).thenReturn(contentletAPI);

            assertEquals(0, PublishDateUpdater.processContentInBatch(
                    "query", 1, 10, processor));

            hibernateMock.verify(HibernateUtil::startTransaction, times(1));
            hibernateMock.verify(HibernateUtil::closeAndCommitTransaction, times(1));
        }
    }

    private void enableWorkflowResolutionCheck() {
        configMock.when(() -> Config.getBooleanProperty(eq(FLAG_KEY), anyBoolean()))
                .thenReturn(true);
    }

    private void configureWorkflowSchemes(final String schemes) {
        configMock.when(() -> Config.getStringProperty(eq(SCHEMES_KEY), eq("")))
                .thenReturn(schemes);
    }

    private WorkflowStep step(final String id, final boolean resolved) {
        return step(id, WORKFLOW_SCHEME_ID, resolved);
    }

    private WorkflowStep step(final String id, final String schemeId, final boolean resolved) {
        final WorkflowStep step = new WorkflowStep();
        step.setId(id);
        step.setSchemeId(schemeId);
        step.setResolved(resolved);
        return step;
    }

    private WorkflowScheme scheme(final String id, final String variableName) {
        final WorkflowScheme scheme = new WorkflowScheme();
        scheme.setId(id);
        scheme.setVariableName(variableName);
        return scheme;
    }
}
