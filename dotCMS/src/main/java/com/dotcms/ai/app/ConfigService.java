package com.dotcms.ai.app;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.security.apps.AppSecrets;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.Map;
import java.util.Optional;

/**
 * ConfigService is a service that provides access to the configuration for the AI application.
 */
public class ConfigService {

    public static final ConfigService INSTANCE = new ConfigService();

    private ConfigService() {
    }

    /**
     * Gets the secrets from the App - this will check the current host then the SYSTEM_HOST for a valid configuration. This lookup is low overhead and cached
     * by dotCMS.
     */
    public AppConfig config(final Host host) {
        final User systemUser = APILocator.systemUser();
        final Host resolved = resolveHost(host);
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
