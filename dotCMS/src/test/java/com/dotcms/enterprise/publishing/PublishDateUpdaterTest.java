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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.publishing.PublishDateUpdater.WorkflowResolutionConfig;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

public class PublishDateUpdaterTest {

    private static final String FLAG_KEY = "PUBLISH_JOB_QUEUE_RESPECT_WORKFLOW_RESOLUTION";
    private static final String SCHEMES_KEY =
            "PUBLISH_JOB_QUEUE_RESPECT_WORKFLOW_RESOLUTION_SCHEMES";
    private static final String CONTENTLET_IDENTIFIER = "contentlet-id";
    private static final String WORKFLOW_SCHEME_ID = "workflow-scheme-id";
    private static final Date FIRE_TIME = new Date(1700000000000L);
    private static final List<String> CONTENT_TYPES = List.of("calendarEvent");

    private ContentletAPI contentletAPI;
    private WorkflowAPI workflowAPI;
    private Contentlet contentlet;
    private User systemUser;

    @Before
    public void setUp() {
        contentletAPI = mock(ContentletAPI.class);
        workflowAPI = mock(WorkflowAPI.class);
        contentlet = mock(Contentlet.class);
        systemUser = mock(User.class);
        when(contentlet.getIdentifier()).thenReturn(CONTENTLET_IDENTIFIER);
    }

    @Test
    public void publisherPreservesHistoricalBehaviorWhenFlagIsDisabled() throws Exception {
        when(workflowAPI.findCurrentStep(contentlet))
                .thenThrow(new DotDataException("workflow must not be queried"));

        assertTrue(publisher(disabled()).process(contentlet, systemUser));

        verify(workflowAPI, never()).findCurrentStep(contentlet);
        verify(contentletAPI).publish(contentlet, systemUser, false);
    }

    @Test
    public void publisherBlocksForcedScheduledPublishAtUnresolvedStep() throws Exception {
        when(workflowAPI.findCurrentStep(contentlet))
                .thenReturn(Optional.of(step("review", false)));

        assertFalse(publisher(global()).process(contentlet, systemUser));

        verify(contentletAPI, never()).publish(contentlet, systemUser, false);
    }

    @Test
    public void publisherAllowsScheduledPublishAtResolvedStep() throws Exception {
        when(workflowAPI.findCurrentStep(contentlet))
                .thenReturn(Optional.of(step("approved", true)));

        assertTrue(publisher(global()).process(contentlet, systemUser));

        verify(workflowAPI).findCurrentStep(contentlet);
        verify(contentletAPI).publish(contentlet, systemUser, false);
    }

    @Test
    public void publisherBlocksScheduledPublishWithoutCurrentStep() throws Exception {
        when(workflowAPI.findCurrentStep(contentlet)).thenReturn(Optional.empty());

        assertFalse(publisher(global()).process(contentlet, systemUser));

        verify(contentletAPI, never()).publish(contentlet, systemUser, false);
    }

    @Test
    public void publisherBlocksUnresolvedStepForConfiguredWorkflowVariable() throws Exception {
        when(workflowAPI.findCurrentStep(contentlet))
                .thenReturn(Optional.of(step("review", WORKFLOW_SCHEME_ID, false)));
        when(workflowAPI.findScheme(WORKFLOW_SCHEME_ID))
                .thenReturn(scheme(WORKFLOW_SCHEME_ID, "targetWorkflow"));

        assertFalse(publisher(scoped("otherWorkflow", "targetWorkflow"))
                .process(contentlet, systemUser));

        verify(contentletAPI, never()).publish(contentlet, systemUser, false);
    }

    @Test
    public void publisherPreservesScheduledPublishForWorkflowOutsideConfiguredList()
            throws Exception {
        when(workflowAPI.findCurrentStep(contentlet))
                .thenReturn(Optional.of(step("review", WORKFLOW_SCHEME_ID, false)));
        when(workflowAPI.findScheme(WORKFLOW_SCHEME_ID))
                .thenReturn(scheme(WORKFLOW_SCHEME_ID, "differentWorkflow"));

        assertTrue(publisher(scoped("targetWorkflow")).process(contentlet, systemUser));

        verify(contentletAPI).publish(contentlet, systemUser, false);
    }

    @Test
    public void publisherBlocksContentWithoutStepWhenConfiguredWorkflowIsAssigned()
            throws Exception {
        final ContentType contentType = mock(ContentType.class);
        when(contentlet.getContentType()).thenReturn(contentType);
        when(workflowAPI.findCurrentStep(contentlet)).thenReturn(Optional.empty());
        when(workflowAPI.findSchemesForContentType(contentType))
                .thenReturn(List.of(scheme(WORKFLOW_SCHEME_ID, "targetWorkflow")));

        assertFalse(publisher(scoped("targetWorkflow")).process(contentlet, systemUser));

        verify(contentletAPI, never()).publish(contentlet, systemUser, false);
    }

    @Test
    public void publisherAllowsContentWithoutStepWhenConfiguredWorkflowIsNotAssigned()
            throws Exception {
        final ContentType contentType = mock(ContentType.class);
        when(contentlet.getContentType()).thenReturn(contentType);
        when(workflowAPI.findCurrentStep(contentlet)).thenReturn(Optional.empty());
        when(workflowAPI.findSchemesForContentType(contentType))
                .thenReturn(List.of(scheme(WORKFLOW_SCHEME_ID, "differentWorkflow")));

        assertTrue(publisher(scoped("targetWorkflow")).process(contentlet, systemUser));

        verify(contentletAPI).publish(contentlet, systemUser, false);
    }

    @Test
    public void publisherFailsClosedWhenWorkflowLookupFails() throws Exception {
        when(workflowAPI.findCurrentStep(contentlet))
                .thenThrow(new DotDataException("workflow unavailable"));

        assertThrows(DotDataException.class,
                () -> publisher(global()).process(contentlet, systemUser));

        verify(contentletAPI, never()).publish(contentlet, systemUser, false);
    }

    @Test
    public void unpublisherIsNotBlockedWhenWorkflowResolutionFlagIsEnabled() throws Exception {
        final PublishDateUpdater.Unpublisher unpublisher =
                new PublishDateUpdater.Unpublisher(contentletAPI);

        assertTrue(unpublisher.process(contentlet, systemUser));

        verify(contentletAPI).unpublish(contentlet, systemUser, false);
        verifyNoInteractions(workflowAPI);
    }

    @Test
    public void workflowResolutionConfigIsReadOncePerJobRunAndNotPerContentlet() throws Exception {
        final WorkflowResolutionConfig config;
        try (MockedStatic<Config> configMock = mockStatic(Config.class)) {
            configMock.when(() -> Config.getBooleanProperty(eq(FLAG_KEY), anyBoolean()))
                    .thenReturn(true);
            configMock.when(() -> Config.getStringProperty(eq(SCHEMES_KEY), eq("")))
                    .thenReturn(" targetWorkflow ,, ");

            config = WorkflowResolutionConfig.fromConfig();

            assertTrue(config.respectWorkflowResolution());
            assertEquals(Set.of("targetWorkflow"), config.configuredSchemes());
            configMock.verify(() -> Config.getBooleanProperty(eq(FLAG_KEY), anyBoolean()),
                    times(1));
            configMock.verify(() -> Config.getStringProperty(eq(SCHEMES_KEY), eq("")), times(1));

            when(workflowAPI.findCurrentStep(contentlet))
                    .thenReturn(Optional.of(step("approved", true)));
            when(workflowAPI.findScheme(WORKFLOW_SCHEME_ID))
                    .thenReturn(scheme(WORKFLOW_SCHEME_ID, "targetWorkflow"));
            final PublishDateUpdater.Publisher publisher = publisher(config);
            assertTrue(publisher.process(contentlet, systemUser));
            assertTrue(publisher.process(contentlet, systemUser));

            configMock.verifyNoMoreInteractions();
        }
    }

    @Test
    public void publishQueryIsUnchangedAndAvoidsWorkflowLookupsWhenFlagIsDisabled() {
        final String query = publishQuery(disabled());

        assertEquals(basePublishQuery(), query);
        verifyNoInteractions(workflowAPI);
    }

    @Test
    public void publishQueryRestrictsGlobalModeToResolvedSteps() throws Exception {
        final WorkflowScheme workflowScheme = scheme(WORKFLOW_SCHEME_ID, "targetWorkflow");
        when(workflowAPI.findSchemes(false)).thenReturn(List.of(workflowScheme));
        when(workflowAPI.findSteps(workflowScheme)).thenReturn(List.of(
                step("published-step", true),
                step("review-step", false),
                step("archived-step", true)));

        final String query = publishQuery(global());

        assertTrue(query.startsWith(basePublishQuery()));
        assertTrue(query.contains(" +(wfstep:published-step wfstep:archived-step )"));
        assertFalse(query.contains("review-step"));
    }

    @Test
    public void publishQueryMatchesNoContentWhenNoResolvedStepExists() throws Exception {
        final WorkflowScheme workflowScheme = scheme(WORKFLOW_SCHEME_ID, "targetWorkflow");
        when(workflowAPI.findSchemes(false)).thenReturn(List.of(workflowScheme));
        when(workflowAPI.findSteps(workflowScheme))
                .thenReturn(List.of(step("review-step", false)));

        final String query = publishQuery(global());

        assertTrue(query.contains(" +(wfstep:no_resolved_workflow_step )"));
        assertFalse(query.contains("review-step"));
    }

    @Test
    public void publishQueryExcludesOnlyUnresolvedConfiguredStepsInScopedMode() throws Exception {
        final WorkflowScheme workflowScheme = scheme(WORKFLOW_SCHEME_ID, "targetWorkflow");
        when(workflowAPI.findScheme("targetWorkflow")).thenReturn(workflowScheme);
        when(workflowAPI.findSteps(workflowScheme)).thenReturn(List.of(
                step("scoped-open-step", false),
                step("scoped-done-step", true)));

        final String query = publishQuery(scoped("targetWorkflow"));

        assertTrue(query.startsWith(basePublishQuery()));
        assertTrue(query.contains(" -(wfstep:scoped-open-step )"));
        assertFalse(query.contains("scoped-done-step"));
        // No positive wfstep clause: content without a step and content on unconfigured
        // workflows stay candidates and are decided by the Java gate
        assertFalse(query.contains("+(wfstep"));
        verify(workflowAPI, never()).findSchemes(anyBoolean());
    }

    @Test
    public void publishQueryStaysUnfilteredWhenConfiguredSchemesHaveNoUnresolvedSteps()
            throws Exception {
        final WorkflowScheme workflowScheme = scheme(WORKFLOW_SCHEME_ID, "targetWorkflow");
        when(workflowAPI.findScheme("targetWorkflow")).thenReturn(workflowScheme);
        when(workflowAPI.findSteps(workflowScheme))
                .thenReturn(List.of(step("scoped-done-step", true)));

        assertEquals(basePublishQuery(), publishQuery(scoped("targetWorkflow")));
    }

    @Test
    public void publishQueryFallsBackToUnfilteredQueryWhenWorkflowLookupFails() throws Exception {
        when(workflowAPI.findSchemes(false))
                .thenThrow(new DotDataException("workflow unavailable"));

        assertEquals(basePublishQuery(), publishQuery(global()));
    }

    @Test
    public void expireQueryNeverContainsWorkflowStepFilter() {
        final String query;
        try (MockedStatic<APILocator> apiLocatorMock = mockStatic(APILocator.class)) {
            apiLocatorMock.when(APILocator::systemTimeZone)
                    .thenReturn(TimeZone.getTimeZone("UTC"));
            query = PublishDateUpdater.getExpireLuceneQuery(FIRE_TIME);
        }

        assertTrue(query.contains("+live:true"));
        assertFalse(query.contains("wfstep"));
        verifyNoInteractions(workflowAPI);
    }

    @Test
    public void batchClosesTransactionWhenProcessorFails() throws Exception {
        final UserAPI userAPI = mock(UserAPI.class);
        final PublishDateUpdater.ContentProcessor processor =
                mock(PublishDateUpdater.ContentProcessor.class);
        final ContentletSearch failedSearchResult = searchResult("failed-inode");
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(contentletAPI.searchIndex(anyString(), anyInt(), anyInt(), isNull(),
                eq(systemUser), eq(false))).thenReturn(List.of(failedSearchResult));
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

    @Test
    public void batchCommitsAtIntermediateThresholdAndProcessesRemainingQueueAfterFailure()
            throws Exception {
        final UserAPI userAPI = mock(UserAPI.class);
        final PublishDateUpdater.ContentProcessor processor =
                mock(PublishDateUpdater.ContentProcessor.class);
        final Contentlet failing = mock(Contentlet.class);
        final Contentlet second = mock(Contentlet.class);
        final Contentlet third = mock(Contentlet.class);
        final List<ContentletSearch> searchResults = List.of(searchResult("failing-inode"),
                searchResult("second-inode"), searchResult("third-inode"));
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(contentletAPI.searchIndex(anyString(), anyInt(), anyInt(), isNull(),
                eq(systemUser), eq(false))).thenReturn(searchResults);
        when(contentletAPI.find("failing-inode", systemUser, false)).thenReturn(failing);
        when(contentletAPI.find("second-inode", systemUser, false)).thenReturn(second);
        when(contentletAPI.find("third-inode", systemUser, false)).thenReturn(third);
        when(processor.getOperationName()).thenReturn("publish");
        when(processor.process(failing, systemUser))
                .thenThrow(new DotDataException("workflow unavailable"));
        when(processor.process(second, systemUser)).thenReturn(true);
        when(processor.process(third, systemUser)).thenReturn(true);

        try (MockedStatic<APILocator> apiLocatorMock = mockStatic(APILocator.class);
                MockedStatic<HibernateUtil> hibernateMock = mockStatic(HibernateUtil.class)) {
            apiLocatorMock.when(APILocator::getUserAPI).thenReturn(userAPI);
            apiLocatorMock.when(APILocator::getContentletAPI).thenReturn(contentletAPI);

            // Three visited inodes with a transaction batch of two: the failed first inode still
            // counts as visited, so the intermediate commit happens exactly at the threshold, a
            // new transaction covers the remaining queue and the final commit closes it
            assertEquals(2, PublishDateUpdater.processContentInBatch(
                    "query", 10, 2, processor));

            hibernateMock.verify(HibernateUtil::startTransaction, times(2));
            hibernateMock.verify(HibernateUtil::closeAndCommitTransaction, times(2));
        }

        verify(processor).process(second, systemUser);
        verify(processor).process(third, systemUser);
    }

    private PublishDateUpdater.Publisher publisher(final WorkflowResolutionConfig config) {
        return new PublishDateUpdater.Publisher(contentletAPI, workflowAPI, config);
    }

    private static WorkflowResolutionConfig disabled() {
        return new WorkflowResolutionConfig(false, Set.of());
    }

    private static WorkflowResolutionConfig global() {
        return new WorkflowResolutionConfig(true, Set.of());
    }

    private static WorkflowResolutionConfig scoped(final String... schemes) {
        return new WorkflowResolutionConfig(true, Set.of(schemes));
    }

    private String publishQuery(final WorkflowResolutionConfig config) {
        try (MockedStatic<APILocator> apiLocatorMock = mockStatic(APILocator.class)) {
            apiLocatorMock.when(APILocator::systemTimeZone)
                    .thenReturn(TimeZone.getTimeZone("UTC"));
            return PublishDateUpdater.getPublishLuceneQuery(FIRE_TIME, CONTENT_TYPES, config,
                    workflowAPI);
        }
    }

    private String basePublishQuery() {
        try (MockedStatic<APILocator> apiLocatorMock = mockStatic(APILocator.class)) {
            apiLocatorMock.when(APILocator::systemTimeZone)
                    .thenReturn(TimeZone.getTimeZone("UTC"));
            return PublishDateUpdater.getPublishLuceneQuery(FIRE_TIME, CONTENT_TYPES);
        }
    }

    private ContentletSearch searchResult(final String inode) {
        final ContentletSearch searchResult = mock(ContentletSearch.class);
        when(searchResult.getInode()).thenReturn(inode);
        return searchResult;
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
