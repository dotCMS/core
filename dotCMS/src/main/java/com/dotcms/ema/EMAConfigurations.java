package com.dotcms.ema;

import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.vavr.control.Try;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Provides all the EMA Configurations for a given Site in dotCMS. Based on the new approach, Users can now map specific
 * URL patterns to specific third-party servers, along with their very own authentication tokens and additional
 * configuration parameters.
 *
 * @author Jose Castro
 * @since May 12th, 2023
 */
public class EMAConfigurations {

    private final List<EMAConfigurationEntry> emaConfiguration;

    /**
     * Creates a new EMA Configurations object.
     *
     * @param emaConfiguration A list of {@link EMAConfigurationEntry} objects that match specific URL patterns.
     */
    @JsonCreator
    public EMAConfigurations(final List<EMAConfigurationEntry> emaConfiguration) {
        this.emaConfiguration = emaConfiguration;
    }

    /**
     * Returns the list of EMA configuration objects.
     *
     * @return The list of {@link EMAConfigurationEntry} objects.
     */
    public List<EMAConfigurationEntry> getEmaConfigurations() {
        return this.emaConfiguration;
    }

    /**
     * Returns the appropriate {@link EMAConfigurationEntry} based on the matching incoming URL.
     *
     * @param url The incoming URL.
     *
     * @return An {@link Optional} with the {@link EMAConfigurationEntry} object, if found.
     */
    public Optional<EMAConfigurationEntry> byUrl(final String url) {
        EMAConfigurationEntry emaConfig = null;
        if (UtilMethods.isSet(this.emaConfiguration)) {
            for (final EMAConfigurationEntry config : this.emaConfiguration) {
                final String decodedURL =
                        Try.of(() -> URLDecoder.decode(url, StandardCharsets.UTF_8)).getOrElse(() -> url);
                if (RegEX.containsCaseInsensitive(decodedURL, config.getPattern())) {
                    emaConfig = config;
                    break;
                }
            }
        }
        return Optional.ofNullable(emaConfig);
    }

}
