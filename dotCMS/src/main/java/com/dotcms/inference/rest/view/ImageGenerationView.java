package com.dotcms.inference.rest.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * A generated image, in the wire shape clients deserialize.
 *
 * <p>Note what this type does not have: a {@code url} component. That is the decision rather than
 * an omission. A hosted link would mean deciding storage, authentication and lifetime for an
 * artifact generated from a prompt that may carry customer data, so this family declines to create
 * a separately-addressable artifact at all and hands the bytes back inline instead. Encoding that
 * in the type — rather than in a branch that happens never to be taken — is what stops a provider's
 * own URL being passed through later by an implementation that no longer remembers why it must
 * not.</p>
 *
 * <p>Should a URL form ever be offered, it has to be authenticated and time-limited, and it would
 * be a new component here rather than a reinstated passthrough.</p>
 *
 * @param created epoch seconds
 * @param data    the generated images, one per image the caller asked for
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Image generation response in the OpenAI-compatible shape")
public record ImageGenerationView(
        @JsonProperty("created") @Schema(description = "Epoch seconds") long created,
        @JsonProperty("data") @Schema(description = "The generated images") List<ImageView> data) {

    /**
     * One image, always inline.
     *
     * @param b64Json       the image bytes, base64 encoded
     * @param revisedPrompt the prompt the provider says it actually used, when it says so
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "One generated image, delivered inline as base64")
    public record ImageView(
            @JsonProperty("b64_json") @Schema(description = "The image bytes, base64 encoded") String b64Json,
            @JsonProperty("revised_prompt") @Schema(description = "The prompt the provider actually used")
            String revisedPrompt) {
    }
}
