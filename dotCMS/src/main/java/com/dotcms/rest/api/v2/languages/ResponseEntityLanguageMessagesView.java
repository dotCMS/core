package com.dotcms.rest.api.v2.languages;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.LanguageBundleVersion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * Entity View for language messages collection responses.
 * Contains the language key-value pairs and a separate, build-derived bundle version.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityLanguageMessagesView extends ResponseEntityView<Map<Object, Object>> {

    private final String languageBundleVersion;

    /**
     * Wraps translations without inserting build metadata into their key space.
     *
     * @param entity the merged language messages
     */
    public ResponseEntityLanguageMessagesView(final Map<Object, Object> entity) {
        super(entity);
        this.languageBundleVersion = LanguageBundleVersion.get();
    }

    /**
     * Returns the same build identity advertised in app configuration.
     *
     * @return the language bundle version, independent of all translation keys
     */
    @Schema(description = "Build-derived SHA-256 language bundle version, matching config.languageBundleVersion "
            + "in /api/v1/appconfiguration. This is envelope metadata, not a translation key.",
            accessMode = Schema.AccessMode.READ_ONLY, pattern = "^[0-9a-f]{64}$")
    public String getLanguageBundleVersion() {
        return languageBundleVersion;
    }
}
