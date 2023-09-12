package com.dotcms.ema.resolver;

import com.dotcms.ema.EMAConfigurations;
import com.dotcms.security.apps.AppSecrets;

import java.util.Optional;

/**
 * This Strategy class takes the EMA configuration secret and extracts its values into the {@link EMAConfigurations}
 * class. This is particularly useful because of the new approach for configuring EMA, which uses a JSON to set all the
 * required properties instead of separate fixed fields in the UI. For backwards compatibility, this Strategy handles
 * both old and new approaches.
 *
 * @author Jose Castro
 * @since May 16th, 2023
 */
public interface EMAConfigStrategy {

    String ACCEPT_ALL = ".*";

    /**
     * Determines whether the App Secret configuration parameters match this Strategy or not.
     *
     * @param appSecrets The {@link AppSecrets} for the EMA configuration.
     * @return If the current values match this Strategy, returns {@code true}.
     */
    boolean test(final AppSecrets appSecrets);

    /**
     * Transfers the EMA configuration parameters into the {@link EMAConfigurations} bean so that they can be easily
     * accessed by other parts of the application.
     *
     * @return The {@link EMAConfigurations} object containing the EMA parameters.
     */
    Optional<EMAConfigurations> resolveConfig();

}
