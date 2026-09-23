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
 * <p>Callers depend on this listing to work, it is not informational. There is no implicit
 * default model and no reserved alias, so the set of names here is exactly the set of
 * {@code model} values this family's operations accept — which makes it the only way a caller can
 * learn what to ask for. Adding anything synthetic would advertise a name every operation
 * refuses.</p>
 *
 * <p>Each entry also carries a {@code type} saying which operation accepts it, because every
 * operation refuses a model configured for another one.</p>
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
     * @param type    which operation the model serves — {@code chat}, {@code embedding} or
     *                {@code image} — taken from the {@code providerConfig} section the site
     *                configured it in. Not one of the four standard fields: it is added alongside
     *                them, which standard clients ignore, and uses the vocabulary Together AI's
     *                listing already puts in the same place.
     */
    @Schema(description = "One configured model")
    public record ModelView(
            @JsonProperty("id") @Schema(description = "Model id", example = "gpt-4o") String id,
            @JsonProperty("object") @Schema(description = "Object type", example = "model") String object,
            @JsonProperty("created") @Schema(description = "Epoch seconds") long created,
            @JsonProperty("owned_by") @Schema(description = "Owner", example = "dotcms") String ownedBy,
            @JsonProperty("type") @Schema(description = "The operation this model serves: chat "
                    + "completions, embeddings or image generation. Taken from the section of the "
                    + "site's configuration the model is listed in; sending the model to a "
                    + "different operation is refused with a 404.",
                    allowableValues = {TYPE_CHAT, TYPE_EMBEDDING, TYPE_IMAGE},
                    example = TYPE_CHAT) String type) {

        /** Object type of a listing entry. */
        public static final String OBJECT = "model";

        /** Owner every entry reports. */
        public static final String OWNED_BY = "dotcms";

        /** Type of a model configured for {@code POST /chat/completions}. */
        public static final String TYPE_CHAT = "chat";

        /** Type of a model configured for {@code POST /embeddings}. */
        public static final String TYPE_EMBEDDING = "embedding";

        /** Type of a model configured for {@code POST /images/generations}. */
        public static final String TYPE_IMAGE = "image";
    }
}
