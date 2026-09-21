package com.dotcms.inference.rest.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The inbound embeddings request, in the wire shape clients send.
 *
 * <p>{@code input} is held as a {@link JsonNode} rather than as a {@code String} or a
 * {@code List<String>} because the format accepts <em>either</em>, and binding to one of them
 * would make the other a deserialization failure — a 400 with a Jackson message naming an internal
 * type, instead of a refusal naming the field. Keeping the node also preserves the difference
 * between a field that arrived as JSON {@code null} and one that was left out, which the
 * validation reports identically but has to detect separately.</p>
 *
 * <p>Unknown properties are ignored so a client's default payload does not fail on a field this
 * family has no opinion about — {@code encoding_format} and {@code user}, typically.</p>
 *
 * @param model the embeddings model to use; required, no implicit default
 * @param input a single string, or an array of strings to embed as one batch
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Embeddings request in the OpenAI-compatible shape")
public record EmbeddingsRequestView(
        @JsonProperty("model") @Schema(description = "Model id, as configured for this site's embeddings",
                example = "text-embedding-3-small") String model,
        @JsonProperty("input") @Schema(description = "A string, or an array of strings to embed as one batch",
                example = "The quick brown fox") JsonNode input) {
}
