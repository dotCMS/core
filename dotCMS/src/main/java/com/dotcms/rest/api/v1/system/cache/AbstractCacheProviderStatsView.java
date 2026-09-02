package com.dotcms.rest.api.v1.system.cache;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;

/**
 * Per-provider cache statistics view. Each provider reports its own set of
 * stat columns, so individual stat rows are represented as {@code Map<String, String>}
 * keyed by column name.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = CacheProviderStatsView.class)
@JsonDeserialize(as = CacheProviderStatsView.class)
@Schema(description = "Cache statistics for a single cache provider")
public interface AbstractCacheProviderStatsView {

    @Schema(
            description = "Cache provider display name",
            example = "Guava Memory Cache",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String providerName();

    @Schema(
            description = "Ordered list of stat column names reported by this provider",
            example = "[\"Region\", \"Configured Size\", \"Memory Used\", \"Is Default\", \"Hit Count\", \"Miss Count\"]",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<String> columns();

    @Schema(
            description = "Per-region statistics, each row keyed by column name",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<Map<String, String>> stats();
}
