package com.dotcms.ai.app;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.util.LicenseValiditySupplier;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.Map;
import java.util.Optional;

/**
 * ConfigService is a service that provides access to the configuration for the AI application.
 */
public class ConfigService {

    public static final ConfigService INSTANCE = new ConfigService();

    private final LicenseValiditySupplier licenseValiditySupplier;

    private ConfigService() {
        this(new LicenseValiditySupplier() {});
    }

    @VisibleForTesting
    ConfigService(final LicenseValiditySupplier licenseValiditySupplier) {
        this.licenseValiditySupplier = licenseValiditySupplier;
    }

    /**
     * Gets the secrets from the App - this will check the current host then the SYSTEM_HOST for a valid configuration. This lookup is low overhead and cached
     * by dotCMS.
     */
    public AppConfig config(final Host host) {
        final Host resolved = resolveHost(host);

        if (!licenseValiditySupplier.hasValidLicense()) {
            Logger.debug(this, "No valid license found, returning empty configuration");
            return new AppConfig(resolved.getHostname(), Map.of());
        }

        final User systemUser = APILocator.systemUser();
        Optional<AppSecrets> appSecrets = Try
                .of(() -> APILocator.getAppsAPI().getSecrets(AppKeys.APP_KEY, false, resolved, systemUser))
                .get();
        final Host realHost;
        if (appSecrets.isEmpty()) {
            realHost = APILocator.systemHost();
            appSecrets = Try
                    .of(() -> APILocator.getAppsAPI().getSecrets(AppKeys.APP_KEY, false, realHost, systemUser))
                    .get();
        } else {
            realHost = resolved;
        }

        return new AppConfig(realHost.getHostname(), appSecrets.map(AppSecrets::getSecrets).orElse(Map.of()));
    }

    /**
     * Gets the secrets from the App - this will check the current host then the SYSTEM_HOST for a valid configuration. This lookup is low overhead and cached
     * by dotCMS.
     */
    public AppConfig config() {
        return config(null);
    }

    /**
     * Ff we have a host, send it, otherwise, system_host
     *
     * @param incoming
     * @return Host to use
     */
    private Host resolveHost(final Host incoming){
        return Optional
                .ofNullable(incoming)
                .orElseGet(() -> Try
                        .of(() -> WebAPILocator
                                .getHostWebAPI()
                                .getCurrentHostNoThrow(HttpServletRequestThreadLocal.INSTANCE.getRequest()))
                        .getOrElse(APILocator.systemHost()));
    }

}
