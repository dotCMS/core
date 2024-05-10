package com.dotcms.ai.app;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import io.vavr.control.Try;

import java.util.Map;
import java.util.Optional;

public class ConfigService {

    public static final ConfigService INSTANCE = new ConfigService();




    public AppConfig config() {
        return config(null);
    }
    /**
     * Gets the secrets from the App - this will check the current host then the SYSTEM_HOST for a valid configuration. This lookup is low overhead and cached
     * by dotCMS.
     */
    public AppConfig config(final Host host) {

        final Optional<AppSecrets> appSecrets = Try.of(() -> APILocator.getAppsAPI().getSecrets(AppKeys.APP_KEY, true, resolveHost(host), APILocator.systemUser())).getOrElse(Optional.empty());

        final Map<String, Secret> secrets = appSecrets.isPresent() ? appSecrets.get().getSecrets() : Map.of();


        return new AppConfig(secrets);
    }

    /**
     * if we have a host, send it, otherwise, system_host
     * @param incoming
     * @return
     */
    Host resolveHost(Host incoming){
        if(incoming!=null){
            return incoming;
        }
        return Try.of(() -> WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(HttpServletRequestThreadLocal.INSTANCE.getRequest())).getOrElse(APILocator.systemHost());

    }


}
