package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.annotations.Nullable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.immutables.value.Value;

/**
 * Immutable view representing a full JVM thread dump.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = ThreadDumpView.class)
@JsonDeserialize(as = ThreadDumpView.class)
@Schema(description = "Full JVM thread dump with deadlock detection")
public interface AbstractThreadDumpView {

    @Schema(
            description = "Identifier of the cluster node that produced this dump",
            example = "01HXX2J3K4M5N6P7Q8R9S0T1U2",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String serverId();

    @Schema(
            description = "Human-readable name of the cluster node, when configured",
            example = "node-1"
    )
    @Nullable
    String serverName();

    @Schema(
            description = "Wall-clock timestamp when the dump was captured",
            example = "Thu Apr 03 10:30:00 UTC 2026",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String timestamp();

    @Schema(
            description = "JVM identification: vm name and runtime version",
            example = "OpenJDK 64-Bit Server VM 21.0.2+13-58",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String vmInfo();

    @Schema(
            description = "Number of threads included in the response (after filtering)",
            example = "42",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    int threadCount();

    @Schema(
            description = "Total number of deadlocked threads detected JVM-wide",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    int deadlockedCount();

    @Schema(
            description = "Per-thread descriptors",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<ThreadDescriptorView> threads();
}
