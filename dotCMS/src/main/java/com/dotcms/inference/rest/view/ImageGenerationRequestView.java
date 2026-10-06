package com.dotcms.inference.rest.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The inbound image-generation request, in the wire shape clients send.
 *
 * <p>{@code n} is boxed rather than a primitive because "not sent" is a value a caller can express
 * and this family has to answer: omitting it means one image, while an {@code n} of {@code 0} is a
 * mistake that must be refused. A primitive would turn the first into the second silently.</p>
 *
 * <p>Unknown properties are ignored so a client's default payload does not fail on a field this
 * family has no opinion about. Notably {@code response_format} is among them: the answer is always
 * base64, so a caller asking for a URL is told nothing here — the response itself is the
 * answer.</p>
 *
 * @param model  the image model to use; required, no implicit default
 * @param prompt what to generate; required, there is nothing to generate without it
 * @param n      how many images; at least {@code 1}, and omitting it means {@code 1}
 * @param size   the image size, passed through to the provider where it accepts one
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Image generation request in the OpenAI-compatible shape")
public record ImageGenerationRequestView(
        @JsonProperty("model") @Schema(description = "Model id, as configured for this site's images",
                example = "dall-e-3") String model,
        @JsonProperty("prompt") @Schema(description = "What to generate",
                example = "A cat in a hammock") String prompt,
        @JsonProperty("n") @Schema(description = "Number of images; at least 1, defaults to 1", example = "1") Integer n,
        @JsonProperty("size") @Schema(description = "Image size", example = "1024x1024") String size) {
}
