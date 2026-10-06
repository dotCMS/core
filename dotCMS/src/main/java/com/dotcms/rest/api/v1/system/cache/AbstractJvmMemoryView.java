package com.dotcms.rest.api.v1.system.cache;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * JVM memory statistics view.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = JvmMemoryView.class)
@JsonDeserialize(as = JvmMemoryView.class)
@Schema(description = "JVM memory statistics in bytes")
public interface AbstractJvmMemoryView {

    @Schema(
            description = "Maximum memory the JVM will attempt to use (Xmx)",
            example = "8589934592",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    long maxMemory();

    @Schema(
            description = "Total memory currently allocated by the JVM",
            example = "4294967296",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    long allocatedMemory();

    @Schema(
            description = "Memory currently in use (allocated minus free)",
            example = "2147483648",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    long usedMemory();

    @Schema(
            description = "Available memory (max minus used)",
            example = "6442450944",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    long freeMemory();
}
