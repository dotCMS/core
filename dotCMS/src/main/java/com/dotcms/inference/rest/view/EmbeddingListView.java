package com.dotcms.inference.rest.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The vectors an embeddings request produced, in the wire shape clients deserialize.
 *
 * <p>Always a list, even for a single string, because the request accepts a batch and a caller
 * should not have to branch on which form they sent to read the answer.</p>
 *
 * <p>{@code index} on each entry is the position of the input it embedded, not the position of the
 * entry in {@code data}. It is the caller's only means of correlating a vector back to the text
 * they sent, which is why a batch with one bad element is refused whole rather than embedded
 * partially: dropping an element would shift every index after it.</p>
 *
 * @param object always {@code list}
 * @param model  the model that actually served, after any fallback hop
 * @param data   one entry per input, in the order the inputs were given
 * @param usage  tokens consumed by the whole batch, omitted when the provider reported none
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Embeddings response in the OpenAI-compatible shape")
public record EmbeddingListView(
        @JsonProperty("object") @Schema(description = "Object type", example = "list") String object,
        @JsonProperty("model") @Schema(description = "Model that served the request",
                example = "text-embedding-3-small") String model,
        @JsonProperty("data") @Schema(description = "One entry per input") List<EmbeddingView> data,
        @JsonProperty("usage") @Schema(description = "Tokens consumed by the whole batch") UsageView usage) {

    /** Object type of the listing envelope. */
    public static final String OBJECT = "list";

    /**
     * One vector.
     *
     * @param object    always {@code embedding}
     * @param index     the position of the input this vector embeds
     * @param embedding the vector itself
     */
    @Schema(description = "One embedding")
    public record EmbeddingView(
            @JsonProperty("object") @Schema(description = "Object type", example = "embedding") String object,
            @JsonProperty("index") @Schema(description = "Position of the input this vector embeds") int index,
            @JsonProperty("embedding") @Schema(description = "The vector") List<Float> embedding) {

        /** Object type of a listing entry. */
        public static final String OBJECT = "embedding";
    }

    /**
     * Tokens consumed.
     *
     * <p>There is no completion count: an embeddings call generates nothing, so the standard shape
     * carries only the prompt and the total.</p>
     *
     * @param promptTokens tokens across every input in the batch
     * @param totalTokens  the same figure the provider reported as the total
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Tokens consumed")
    public record UsageView(
            @JsonProperty("prompt_tokens") @Schema(description = "Tokens across every input") Integer promptTokens,
            @JsonProperty("total_tokens") @Schema(description = "Total tokens") Integer totalTokens) {
    }
}
