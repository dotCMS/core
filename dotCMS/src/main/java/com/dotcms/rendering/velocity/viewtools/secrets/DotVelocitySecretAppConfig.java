package com.dotcms.rendering.velocity.viewtools.secrets;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Encapsulates the velocity secrets (title + extra parameters)
 * @author jsanca
 */
@JsonDeserialize(builder = DotVelocitySecretAppConfig.Builder.class)
public class DotVelocitySecretAppConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String title;
    public final Map<String, String> extraParameters;

    public final String getValue(final String key, final String defaultValue) {
        return extraParameters.compute(key, (k, v) -> (v == null) ? defaultValue : v);
    }

    public final String[] getArrayValue(final String key) {

        return UtilMethods.isSet(extraParameters.get(key))
                        ? Arrays.stream(extraParameters.get(key).split(",")).map(String::trim).toArray(String[]::new)
                        : new String[0];
    }

    private DotVelocitySecretAppConfig(Builder builder) {
        this.title = builder.title;
        this.extraParameters = builder.extraParameters;
    }

    public static Optional<DotVelocitySecretAppConfig> config() {

        return config(HttpServletRequestThreadLocal.INSTANCE.getRequest());
    }

    /**
     * Gets the secrets from the App - this will check the current host then the SYSTEM_HOST for a valid
     * configuration. This lookup is low overhead and cached by dotCMS.
     * 
     * @param request {@link HttpServletRequest}
     * @return
     */
    public static Optional<DotVelocitySecretAppConfig> config(final HttpServletRequest request) {

        return config(null != request? WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request) : APILocator.systemHost());
    }

    /**
     * Gets the secrets from the App - this will check the current host then the SYSTEM_HOST for a valid
     * configuration. This lookup is low overhead and cached by dotCMS.
     *
     * @param host {@link Host}
     * @return
     */
    public static Optional<DotVelocitySecretAppConfig> config(final Host host) {

        if (DotVelocitySecretAppConfigThreadLocal.INSTANCE.getConfig(host.getIdentifier()).isPresent()) {

            return DotVelocitySecretAppConfigThreadLocal.INSTANCE.getConfig(host.getIdentifier());
        }

        final Optional<AppSecrets> appSecrets = Try.of(
                        () -> APILocator.getAppsAPI().getSecrets(DotVelocitySecretAppKeys.APP_KEY, true, host, APILocator.systemUser()))
                .getOrElse(Optional.empty());

        if (!appSecrets.isPresent()) {

            return Optional.empty();
        }

        final Map<String, Secret> secrets = new HashMap<>(appSecrets.get().getSecrets());
        final Map<String, String> extraParameters = new HashMap<>();
        final String title =
                Try.of(() -> secrets.get(DotVelocitySecretAppKeys.TITLE.key).getString()).getOrNull();

        if (!UtilMethods.isSet(title)) {

            return Optional.empty();
        }

        for (String key : secrets.keySet()) {
            extraParameters.put(key, secrets.get(key).getString());
        }

        final DotVelocitySecretAppConfig config = DotVelocitySecretAppConfig.builder()
                .withTitle(title)
                .withExtraParameters(extraParameters).build();

        DotVelocitySecretAppConfigThreadLocal.INSTANCE.setConfig(host.getIdentifier(), Optional.ofNullable(config));

        return DotVelocitySecretAppConfigThreadLocal.INSTANCE.getConfig(host.getIdentifier());
    }

    /**
     * Creates builder to build {@link DotVelocitySecretAppConfig}.
     * 
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder to build {@link DotVelocitySecretAppConfig} and initialize it with the given object.
     * 
     * @param appConfig to initialize the builder with
     * @return created builder
     */
    public static Builder from(DotVelocitySecretAppConfig appConfig) {
        return new Builder(appConfig);
    }

    /**
     * Builder to build {@link DotVelocitySecretAppConfig}.
     */
    public static final class Builder {
        private String title;
        private Map<String, String> extraParameters = Collections.emptyMap();
        private Builder() {}

        private Builder(DotVelocitySecretAppConfig appConfig) {
            this.title = appConfig.title;
            this.extraParameters = appConfig.extraParameters;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withExtraParameters(final Map<String, String> extraParameters) {
            this.extraParameters = extraParameters;
            return this;
        }

        public DotVelocitySecretAppConfig build() {
            return new DotVelocitySecretAppConfig(this);
        }
    }
}
