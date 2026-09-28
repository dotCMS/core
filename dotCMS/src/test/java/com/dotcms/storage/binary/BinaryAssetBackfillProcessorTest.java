package com.dotcms.storage.binary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.storage.AssetStorageFeature;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

class BinaryAssetBackfillProcessorTest {
    private final AtomicReference<Job> persisted = new AtomicReference<>();
    private final User admin = mock(User.class);
    private MockedStatic<APILocator> locator;
    private MockedStatic<DbConnectionFactory> connections;
    private MockedStatic<BinaryAssetBackfill> copies;
    private String previousFlag;

    @BeforeEach
    void configure() throws Exception {
        previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        Config.setProperty(AssetStorageFeature.FLAG, true);
        locator = mockStatic(APILocator.class);
        connections = mockStatic(DbConnectionFactory.class);
        copies = mockStatic(BinaryAssetBackfill.class);
        final var users = mock(UserAPI.class);
        when(users.loadUserById("administrator")).thenReturn(admin);
        when(admin.isAdmin()).thenReturn(true);
        when(admin.isActive()).thenReturn(true);
        locator.when(APILocator::getUserAPI).thenReturn(users);
        persisted.set(Job.builder().id("backfill-test").queueName(BinaryAssetBackfillProcessor.QUEUE)
                .state(JobState.RUNNING).parameters(Map.of("userId", "administrator", "batchSize", 1)).build());
        final var jobs = mock(JobQueueManagerAPI.class);
        when(jobs.getJob("backfill-test")).thenAnswer(call -> persisted.get());
        locator.when(APILocator::getJobQueueManagerAPI).thenReturn(jobs);
    }

    @AfterEach
    void close() {
        copies.close();
        connections.close();
        locator.close();
        Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
    }

    @ParameterizedTest
    @ValueSource(strings = {"copy", "before-commit", "after-commit"})
    void newWorkerResumesCommittedCheckpointAfterFailure(String failure) throws Exception {
        final Job originalRequest = persisted.get();
        copies.when(() -> BinaryAssetBackfill.runBatch("", 1))
                .thenReturn(new BinaryAssetBackfill.Result("aa", 2, false));
        if (failure.equals("copy")) {
            copies.when(() -> BinaryAssetBackfill.runBatch("aa", 1))
                    .thenThrow(new DotDataException("S3 unavailable"))
                    .thenReturn(new BinaryAssetBackfill.Result("bb", 3, true));
        } else {
            copies.when(() -> BinaryAssetBackfill.runBatch("aa", 1))
                    .thenReturn(new BinaryAssetBackfill.Result("bb", 3, true));
        }
        try (var checkpoints = checkpointStore(failure)) {
            assertThrows(JobProcessingException.class,
                    () -> new BinaryAssetBackfillProcessor().process(originalRequest));
            final String expectedCursor = failure.equals("before-commit") ? "" : "aa";
            assertEquals(expectedCursor, persisted.get().parameters().getOrDefault("afterInode", ""));
            final var restarted = new BinaryAssetBackfillProcessor();
            restarted.process(originalRequest); // Intentionally stale job: the new worker must reload it.
            assertEquals(Map.of("afterInode", "bb", "verifiedBinaries", 5L, "complete", true),
                    restarted.getResultMetadata(originalRequest));
            assertEquals("administrator", persisted.get().parameters().get("userId"));
            assertEquals(1, persisted.get().parameters().get("batchSize"));
            copies.verify(() -> BinaryAssetBackfill.runBatch("", 1),
                    times(failure.equals("before-commit") ? 2 : 1));
        }
    }

    @Test
    void cancellationKeepsLastCompletedBatchAndDoesNotClaimCompletion() throws Exception {
        final Job request = persisted.get();
        final var processor = new BinaryAssetBackfillProcessor();
        copies.when(() -> BinaryAssetBackfill.runBatch("", 1)).thenAnswer(call -> {
            processor.cancel(request);
            return new BinaryAssetBackfill.Result("aa", 2, false);
        });
        try (var checkpoints = checkpointStore("none")) {
            processor.process(request);
            assertEquals(Map.of("afterInode", "aa", "verifiedBinaries", 2L, "complete", false),
                    processor.getResultMetadata(request));
            assertEquals("aa", persisted.get().parameters().get("afterInode"));
            copies.verify(() -> BinaryAssetBackfill.runBatch("", 1));
            copies.verifyNoMoreInteractions();
        }
    }

    @Test
    void lostCheckpointRaceStopsWithoutAdvancing() throws Exception {
        copies.when(() -> BinaryAssetBackfill.runBatch("", 1))
                .thenReturn(new BinaryAssetBackfill.Result("aa", 2, false));
        try (var queries = mockConstruction(DotConnect.class, withSettings().defaultAnswer(RETURNS_SELF),
                (query, context) -> when(query.loadObjectResults()).thenReturn(List.of()))) {
            final var processor = new BinaryAssetBackfillProcessor();
            assertThrows(JobProcessingException.class, () -> processor.process(persisted.get()));
            assertEquals("", processor.getResultMetadata(persisted.get()).get("afterInode"));
            assertFalse(persisted.get().parameters().containsKey("afterInode"));
            copies.verify(() -> BinaryAssetBackfill.runBatch("", 1));
            copies.verifyNoMoreInteractions();
        }
    }

    @Test
    void disabledAndUnauthorizedJobsCannotCopyOrCheckpoint() throws Exception {
        final var processor = new BinaryAssetBackfillProcessor();
        Config.setProperty(AssetStorageFeature.FLAG, false);
        assertThrows(JobValidationException.class, () -> processor.validate(persisted.get().parameters()));
        assertThrows(JobProcessingException.class, () -> processor.process(persisted.get()));
        locator.verifyNoInteractions();
        Config.setProperty(AssetStorageFeature.FLAG, true);
        when(admin.isAdmin()).thenReturn(false);
        assertThrows(JobValidationException.class, () -> processor.validate(persisted.get().parameters()));
        assertThrows(JobProcessingException.class, () -> processor.process(persisted.get()));
        when(admin.isAdmin()).thenReturn(true);
        when(admin.isActive()).thenReturn(false);
        assertThrows(JobProcessingException.class, () -> processor.process(persisted.get()));
        when(admin.isActive()).thenReturn(true);
        assertThrows(JobValidationException.class, () -> processor.validate(Map.of("userId", "administrator", "batchSize", 0)));
        assertThrows(JobValidationException.class, () -> processor.validate(Map.of("userId", "administrator", "afterInode", "../other")));
        connections.when(DbConnectionFactory::inTransaction).thenReturn(true);
        assertThrows(JobProcessingException.class, () -> processor.process(persisted.get()));
        copies.verifyNoInteractions();
    }

    private MockedConstruction<DotConnect> checkpointStore(String failure) {
        final AtomicBoolean firstSave = new AtomicBoolean(true);
        return mockConstruction(DotConnect.class, withSettings().defaultAnswer(RETURNS_SELF), (query, context) -> {
            final List<String> arguments = new ArrayList<>();
            when(query.addParam(anyString())).thenAnswer(call -> {
                arguments.add(call.getArgument(0));
                return query;
            });
            when(query.loadObjectResults()).thenAnswer(call -> {
                final boolean first = firstSave.getAndSet(false);
                if (first && failure.equals("before-commit")) {
                    throw new DotDataException("checkpoint write failed");
                }
                assertEquals("backfill-test", arguments.get(1));
                assertEquals(BinaryAssetBackfillProcessor.QUEUE, arguments.get(2));
                assertEquals(persisted.get().parameters().getOrDefault("afterInode", ""), arguments.get(3));
                final var parameters = new HashMap<>(persisted.get().parameters());
                parameters.putAll(new ObjectMapper().readValue(arguments.get(0), Map.class));
                persisted.set(Job.builder().from(persisted.get()).parameters(parameters).build());
                if (first && failure.equals("after-commit")) {
                    throw new DotDataException("connection lost after checkpoint committed");
                }
                return List.of(Map.of("id", "backfill-test"));
            });
        });
    }
}
