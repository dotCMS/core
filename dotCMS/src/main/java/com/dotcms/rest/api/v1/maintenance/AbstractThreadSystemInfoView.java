package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.annotations.Nullable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * Immutable view with lightweight JVM startup and thread-count information.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = ThreadSystemInfoView.class)
@JsonDeserialize(as = ThreadSystemInfoView.class)
@Schema(description = "JVM startup time and thread-count summary")
public interface AbstractThreadSystemInfoView {

    @Schema(
            description = "Identifier of the cluster node serving this request",
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
            description = "JVM startup time formatted as 'dd MMM yyyy HH:mm:ss' (server local time)",
            example = "03 Apr 2026 08:15:30",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String systemStartupTime();

    @Schema(
            description = "JVM startup time as epoch milliseconds",
            example = "1775376930000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    long startTimeMillis();

    @Schema(
            description = "JVM uptime in milliseconds",
            example = "7830000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    long uptimeMillis();

    @Schema(
            description = "Current live thread count (daemon and non-daemon)",
            example = "187",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    int currentThreadCount();

    @Schema(
            description = "Peak live thread count since the JVM started",
            example = "245",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    int peakThreadCount();
}
