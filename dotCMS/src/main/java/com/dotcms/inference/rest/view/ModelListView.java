package com.dotcms.inference.rest.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The models a site has configured, in the wire shape clients already parse.
 *
 * <p>Not wrapped in the dotCMS {@code ResponseEntityView} envelope, for the same reason nothing
 * else in this family is: a client library has to deserialize this into its own model-list type
 * with no adapter.</p>
 *
 * <p>This listing is load-bearing rather than decorative. There is no implicit default model and
 * no reserved alias, so the set of names here is exactly the set of {@code model} values the chat
 * endpoint accepts — which makes it the only way a caller can learn what to ask for. Adding
 * anything synthetic would advertise a name that endpoint refuses.</p>
 *
 * @param object always {@code list}
 * @param data   one entry per configured model, in configured order; the first is the primary
 */
@Schema(description = "Model listing in the OpenAI-compatible shape")
public record ModelListView(
        @JsonProperty("object") @Schema(description = "Object type", example = "list") String object,
        @JsonProperty("data") @Schema(description = "The configured models, primary first")
        List<ModelView> data) {

    /** Object type of the listing envelope. */
    public static final String OBJECT = "list";

    /**
     * One model a caller may name.
     *
     * @param id      the name to send as {@code model}
     * @param object  always {@code model}
     * @param created epoch seconds; dotCMS serves configuration rather than a catalogue, so this
     *                is when the listing was read rather than when a vendor published the model
     * @param ownedBy always {@code dotcms} — whoever built the model, dotCMS is what serves it,
     *                and reporting a vendor here would imply a provenance dotCMS cannot vouch for
     */
    @Schema(description = "One configured model")
    public record ModelView(
            @JsonProperty("id") @Schema(description = "Model id", example = "gpt-4o") String id,
            @JsonProperty("object") @Schema(description = "Object type", example = "model") String object,
            @JsonProperty("created") @Schema(description = "Epoch seconds") long created,
            @JsonProperty("owned_by") @Schema(description = "Owner", example = "dotcms") String ownedBy) {

        /** Object type of a listing entry. */
        public static final String OBJECT = "model";

        /** Owner every entry reports. */
        public static final String OWNED_BY = "dotcms";
    }
}
