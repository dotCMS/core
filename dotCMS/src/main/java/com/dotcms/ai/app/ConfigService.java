package com.dotcms.ai.app;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
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

    private final AppsAPI appsAPI;

    @VisibleForTesting
    ConfigService(final AppsAPI appsAPI) {
        this.appsAPI = appsAPI;
    }

    public ConfigService() {
        this(APILocator.getAppsAPI());
    }

    /**
     * Gets the secrets from the App - this will check the current host then the SYSTEM_HOST for a valid configuration. This lookup is low overhead and cached
     * by dotCMS.
     */
    public AppConfig config(final Host host) {
        final User systemUser = APILocator.systemUser();
        final Host resolved = resolveHost(host);
        final Optional<AppSecrets> appSecrets = getAiSecrets(resolved, systemUser);

        if (appSecrets.isEmpty() && !resolved.isSystemHost()) {
            final Host systemHost = APILocator.systemHost();
            final Optional<AppSecrets> systemSecrets = getAiSecrets(systemHost, systemUser);
            return new AppConfig(systemHost.getHostname(), systemSecrets.map(AppSecrets::getSecrets).orElse(Map.of()));
        }

        return new AppConfig(resolved.getHostname(), appSecrets.map(AppSecrets::getSecrets).orElse(Map.of()));
    }

    /**
     * Gets the secrets from the App - this will check the current host then the SYSTEM_HOST for a valid configuration. This lookup is low overhead and cached
     * by dotCMS.
     */
    public AppConfig config() {
        return config(null);
    }
    /**
     * If we have a host, send it, otherwise, system_host
     *
     * @param incoming incoming host
     * @return Host to use
     */
    private Host resolveHost(final Host incoming) {
        logHost(incoming, "initial");
        final Host resolved = Optional
                .ofNullable(incoming)
                .orElseGet(() -> Try
                        .of(() -> WebAPILocator
                                .getHostWebAPI()
                                .getCurrentHostNoThrow(HttpServletRequestThreadLocal.INSTANCE.getRequest()))
                        .getOrElse(APILocator.systemHost()));
        logHost(incoming, "resolved");
        return resolved;
    }

    private void logHost(final Host host, final String hostLabel) {
        Logger.debug(
                ConfigService.class,
                () -> String.format(
                        "Getting appConfig for %s host [%s]",
                        hostLabel,
                        Optional.ofNullable(host).map(Host::getHostname).orElse("null")));
    }

    private Optional<AppSecrets> getAiSecrets(final Host host, final User systemUser) {
        // getOrElse, not get(): Try.get() unwraps by RETHROWING the failure, so the Try was doing
        // nothing here. That went unnoticed while an unreadable App secrets store silently wiped
        // itself and returned no secrets; since issue #36724 it raises instead, and this method
        // would have propagated that into dotAI's config resolution rather than reporting the app
        // as unconfigured -- unlike every other consumer, which degrades.
        return Try
                .of(() -> appsAPI.getSecrets(AppKeys.APP_KEY, false, host, systemUser))
                .onFailure(e -> Logger.warnAndDebug(ConfigService.class,
                        "Unable to read dotAI secrets; treating the app as unconfigured: "
                                + e.getMessage(), e))
                .getOrElse(Optional.empty());
    }
}
