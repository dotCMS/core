package com.dotcms.content.index;

import com.dotcms.content.index.SearchUnavailableException.Reason;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/** Fail-fast admission and a wall-clock deadline for asynchronous search I/O; no worker queue. */
public final class SearchAdmission {

    /** Starts I/O and returns its cancellation action; completion must mean the I/O has finished. */
    @FunctionalInterface
    public interface Operation<T> {
        /** Starts one request; the callback must finish only when its transport has finished. */
        Runnable start(CompletableFuture<T> completion) throws IOException;
    }

    private final Semaphore permits;
    private final int deadlineMillis;
    private final Counter rejected;
    private final Counter deadlines;
    private final Counter interrupted;
    private final Counter failures;
    private final Counter backend;
    private final Timer duration;

    /** Creates a bounded gate with private metrics, also used by deterministic concurrency tests. */
    public SearchAdmission(final int maximum, final int deadlineMillis,
            final String engine, final MeterRegistry registry) {
        if (maximum <= 0 || deadlineMillis <= 0) {
            throw new IllegalArgumentException("Search concurrency and deadline must be positive");
        }
        this.permits = new Semaphore(maximum);
        this.deadlineMillis = deadlineMillis;
        Gauge.builder("dotcms.search.admission.active", permits,
                        semaphore -> maximum - semaphore.availablePermits())
                .description("Admitted searches whose transport has not completed")
                .tag("engine", engine).register(registry);
        Gauge.builder("dotcms.search.admission.limit", () -> maximum)
                .tag("engine", engine).register(registry);
        rejected = registry.counter("dotcms.search.admission.rejected", "engine", engine);
        deadlines = registry.counter("dotcms.search.admission.deadlines", "engine", engine);
        interrupted = registry.counter("dotcms.search.admission.interrupted", "engine", engine);
        backend = registry.counter("dotcms.search.admission.backend-unavailable", "engine", engine);
        failures = registry.counter("dotcms.search.admission.failures", "engine", engine);
        duration = registry.timer("dotcms.search.admission.duration", "engine", engine);
    }

    /** Loads validated startup settings without changing the gate during in-flight operations. */
    public static SearchAdmission configured(final String engine) {
        return new SearchAdmission(positiveProperty("search.admission.max-concurrent", 8),
                positiveProperty("search.deadline-ms", 5000), engine, Metrics.globalRegistry);
    }

    /** Reads positive bounded settings; zero must never accidentally mean an infinite timeout. */
    public static int positiveProperty(final String name, final int defaultValue) {
        final int value = Config.getIntProperty(name, defaultValue);
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    /**
     * Admits without waiting, then waits at most one deadline, including transport retries.
     * A timed-out call retains its permit until transport completion, even if cancellation is
     * unsupported or delayed: freeing it on caller timeout would admit unbounded orphan I/O.
     */
    public <T> T execute(final Operation<T> operation) throws IOException {
        return execute(operation, result -> { });
    }

    /** Also disposes late responses (raw response bodies or scroll contexts) after abandonment. */
    public <T> T execute(final Operation<T> operation, final Consumer<T> discard) throws IOException {
        SearchRequestFailure.check();
        if (!permits.tryAcquire()) {
            rejected.increment();
            throw SearchRequestFailure.record(new SearchUnavailableException(Reason.REJECTED, null));
        }
        final long start = System.nanoTime();
        final CompletableFuture<T> completion = new CompletableFuture<>();
        completion.whenComplete((result, failure) -> permits.release());
        Runnable cancel = () -> { };
        try {
            try {
                cancel = operation.start(completion);
            } catch (IOException | RuntimeException | Error failure) {
                failures.increment();
                completion.completeExceptionally(failure);
                throw failure;
            }
            final long remaining = TimeUnit.MILLISECONDS.toNanos(deadlineMillis)
                    - (System.nanoTime() - start);
            if (remaining <= 0) {
                throw new TimeoutException();
            }
            return completion.get(remaining, TimeUnit.NANOSECONDS);
        } catch (TimeoutException failure) {
            deadlines.increment();
            completion.thenAccept(discard);
            cancel(cancel);
            throw SearchRequestFailure.record(new SearchUnavailableException(Reason.DEADLINE, failure));
        } catch (InterruptedException failure) {
            interrupted.increment();
            completion.thenAccept(discard);
            cancel(cancel);
            Thread.currentThread().interrupt();
            throw SearchRequestFailure.record(new SearchUnavailableException(Reason.INTERRUPTED, failure));
        } catch (CancellationException failure) {
            failures.increment();
            throw unavailable(failure);
        } catch (ExecutionException failure) {
            failures.increment();
            Throwable cause = failure.getCause();
            while (cause instanceof java.util.concurrent.CompletionException && cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof CancellationException) {
                throw unavailable(cause);
            }
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IOException("Search execution failed", cause);
        } finally {
            duration.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    /** Counts backend rejection or partial/timed-out responses without returning cacheable data. */
    public SearchUnavailableException unavailable(final Throwable cause) {
        backend.increment();
        return SearchRequestFailure.record(new SearchUnavailableException(Reason.BACKEND, cause));
    }

    /** Cancellation must not hide the original deadline or interruption. */
    private static void cancel(final Runnable cancellation) {
        try {
            cancellation.run();
        } catch (RuntimeException failure) {
            Logger.debug(SearchAdmission.class, "Unable to cancel search transport", failure);
        }
    }
}
