package com.dotcms.ai.util;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import io.vavr.Lazy;

public class EncodingUtil {

    public static final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    public static final String model = ConfigService.INSTANCE.config().getConfig(AppKeys.EMBEDDINGS_MODEL);

    public static Lazy<Encoding> encoding = Lazy.of(()->
            registry.getEncodingForModel(model).get()
    );

}
