package com.dotcms.ai.util;

import com.dotcms.ai.app.ConfigService;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import io.vavr.Lazy;

/**
 * Utility class for handling encoding operations related to AI models.
 * It provides a registry for encoding and a lazy-loaded encoding instance based on the current model.
 * The class uses the ConfigService to retrieve the current model configuration.
 */
public class EncodingUtil {

    public static final EncodingRegistry REGISTRY = Encodings.newDefaultEncodingRegistry();
    public static final String MODEL = ConfigService.INSTANCE.config().getEmbeddingsModel().getCurrentModel();
    public static final Lazy<Encoding> ENCODING = Lazy.of(() -> REGISTRY.getEncodingForModel(MODEL).get());

    private EncodingUtil() {
    }

}
