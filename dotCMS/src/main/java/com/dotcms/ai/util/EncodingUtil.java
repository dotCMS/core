package com.dotcms.ai.util;

import com.dotcms.ai.app.ConfigService;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import io.vavr.Lazy;

import java.util.Optional;

/**
 * Utility class for handling encoding operations related to AI models.
 * It provides a registry for encoding and a lazy-loaded encoding instance based on the current model.
 * The class uses the ConfigService to retrieve the current model configuration.
 */
public class EncodingUtil {

    private static final Lazy<EncodingUtil> INSTANCE = Lazy.of(EncodingUtil::new);

    public final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    private EncodingUtil() {
    }

    public static EncodingUtil get() {
        return INSTANCE.get();
    }

    public Optional<Encoding> getEncoding() {
        return Optional
                .ofNullable(ConfigService.INSTANCE.config().getEmbeddingsModel().getCurrentModel())
                .flatMap(registry::getEncodingForModel);
    }

}
