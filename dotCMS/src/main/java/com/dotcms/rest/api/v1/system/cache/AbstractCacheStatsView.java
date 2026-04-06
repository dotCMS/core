package com.dotcms.rest.api.v1.system.cache;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.util.List;

/**
 * Top-level cache statistics view combining JVM memory info
 * with per-provider cache statistics.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = CacheStatsView.class)
@JsonDeserialize(as = CacheStatsView.class)
@Schema(description = "Cache statistics including JVM memory and per-provider region stats")
public interface AbstractCacheStatsView {

    @Schema(
            description = "JVM memory statistics",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    JvmMemoryView memory();

    @Schema(
            description = "Per-provider cache statistics",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<CacheProviderStatsView> providers();
}
