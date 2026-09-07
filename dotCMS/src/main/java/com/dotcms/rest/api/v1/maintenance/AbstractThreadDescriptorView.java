package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.annotations.Nullable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.immutables.value.Value;

/**
 * Immutable view describing a single JVM thread in a thread dump.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = ThreadDescriptorView.class)
@JsonDeserialize(as = ThreadDescriptorView.class)
@Schema(description = "Single thread entry in a JVM thread dump")
public interface AbstractThreadDescriptorView {

    @Schema(
            description = "Thread name",
            example = "http-nio-8080-exec-1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String name();

    @Schema(
            description = "JVM-assigned thread id",
            example = "142",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    long id();

    @Schema(
            description = "Whether this is a daemon thread",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean daemon();

    @Schema(
            description = "Thread priority (1-10)",
            example = "5",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    int priority();

    @Schema(
            description = "Thread state name (e.g. RUNNABLE, WAITING, TIMED_WAITING, BLOCKED)",
            example = "RUNNABLE",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String state();

    @Schema(
            description = "Whether this thread is part of a detected deadlock cycle",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean deadlocked();

    @Schema(
            description = "Stack trace as a list of formatted frames",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<String> stackTrace();

    @Schema(
            description = "Locked monitors held by this thread, formatted as 'ClassName at depth N'",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<String> lockedMonitors();

    @Schema(
            description = "Locked ownable synchronizers held by this thread (LockInfo.toString())",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<String> lockedSynchronizers();

    @Schema(
            description = "String form of the lock the thread is currently waiting on; null if none",
            example = "java.util.concurrent.locks.ReentrantLock$NonfairSync@1a2b3c"
    )
    @Nullable
    String lockInfo();

    @Schema(
            description = "Name of the thread that owns the lock this thread is waiting on; null if none",
            example = "http-nio-8080-exec-7"
    )
    @Nullable
    String lockOwnerName();

    @Schema(
            description = "Id of the thread that owns the lock this thread is waiting on; null if none",
            example = "187"
    )
    @Nullable
    Long lockOwnerId();
}
