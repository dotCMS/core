package com.dotcms.ema.resolver;

import com.dotcms.ema.EMAConfigurationEntry;
import com.dotcms.ema.EMAConfigurations;
import com.dotcms.ema.EMAWebInterceptor;
import com.dotcms.ema.RewriteBean;
import com.dotcms.ema.RewritesBean;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsUtil;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This Resolver will take the EMA configuration parameters and will determine what specific Strategy will be able to
 * successfully transform them into the appropriate {@link EMAConfigurations} bean. From a UI perspective, the legacy
 * approach uses separate fields, and the new approach uses a single more flexible JSON field for all of them.
 *
 * @author Jose Castro
 * @since May 16th, 2023
 */
public class EMAConfigStrategyResolver {

    private final List<EMAConfigStrategy> strategies = this.getDefaultStrategies();
    private EMAConfigStrategy defaultStrategy = null;

    /**
     * Creates an instance of this class.
     */
    public EMAConfigStrategyResolver() {

    }

    /**
     * Returns the default Strategy Resolver for EMA configurations.
     *
     * @return The default {@link EMAConfigStrategy}.
     */
    public EMAConfigStrategy getDefaultStrategy() {
        return this.defaultStrategy;
    }

    /**
     * Sets the default Strategy Resolver for EMA configurations.
     *
     * @param strategy The default {@link EMAConfigStrategy}.
     */
    public void setDefaultStrategy(final EMAConfigStrategy strategy) {
        if (null != strategy) {
            this.defaultStrategy = strategy;
        }
    }

    /**
     * Returns all the default Strategy Resolvers for retrieving EMA configurations.
     *
     * @return The list of {@link EMAConfigStrategy} objects.
     */
    private List<EMAConfigStrategy> getDefaultStrategies() {
        final ImmutableList.Builder<EMAConfigStrategy> builder = new ImmutableList.Builder<>();
        final JSONEMAConfig jsonEmaConfig = new JSONEMAConfig();
        this.setDefaultStrategy(jsonEmaConfig);
        builder.add(jsonEmaConfig);
        builder.add(new LegacyEMAConfig());
        return builder.build();
    }

    /**
     * Returns the appropriate EMA Configuration Strategy for a specific Site.
     *
     * @param site The {@link Host} object whose EMA configuration will be retrieved.
     *
     * @return An Optional with the appropriate {@link EMAConfigStrategy}, if any.
     */
    public Optional<EMAConfigStrategy> get(final Host site) {
        final Optional<AppSecrets> appSecrets = AppsUtil.getAppSecrets(EMAWebInterceptor.EMA_APP_CONFIG_KEY, site,
                true);
        EMAConfigStrategy emaConfigStrategy = null;
        if (appSecrets.isPresent()) {
            for (final EMAConfigStrategy strategy : this.getDefaultStrategies()) {
                if (strategy.test(appSecrets.get())) {
                    emaConfigStrategy = strategy;
                    break;
                }
            }
            appSecrets.get().destroy();
        }
        return Optional.ofNullable(emaConfigStrategy);
    }

    /**
     * Retrieves the EMA configuration parameters based on the <b>legacy approach</b>. This is the one that takes three
     * separate fields and didn't allow for more flexibility. This approach will be removed in the future.
     */
    private static class LegacyEMAConfig implements EMAConfigStrategy {

        private boolean includeRendered = Boolean.FALSE;
        private String authenticationToken = StringPool.BLANK;
        private String proxyUrl = StringPool.BLANK;

        private RewritesBean rewritesBean = null;

        @Override
        public boolean test(final AppSecrets appSecrets) {
            final String paramValue =
                    Try.of(() -> appSecrets.getSecrets().get(EMAWebInterceptor.PROXY_EDIT_MODE_URL_VAR).getString()).getOrElse(StringPool.BLANK);
            if (paramValue.isEmpty()) {
                return false;
            } else if (StringUtils.isJson(paramValue)) {
                rewritesBean =
                        Try.of(() -> JsonUtil.getObjectFromJson(appSecrets.getSecrets().get(EMAWebInterceptor.PROXY_EDIT_MODE_URL_VAR).getString(), RewritesBean.class))
                                .onFailure(e -> Logger.debug(LegacyEMAConfig.class, e.getMessage(), e))
                                .getOrNull();
                return null != rewritesBean;
            } else {
                proxyUrl = paramValue;
            }
            this.includeRendered =
                    Try.of(() -> appSecrets.getSecrets().get(EMAWebInterceptor.INCLUDE_RENDERED_VAR).getBoolean()).getOrElse(Boolean.FALSE);
            this.authenticationToken =
                    Try.of(() -> appSecrets.getSecrets().get(EMAWebInterceptor.AUTHENTICATION_TOKEN_VAR).getString()).getOrElse(StringPool.BLANK);
            return true;
        }

        @Override
        public Optional<EMAConfigurations> resolveConfig() {
            EMAConfigurations emaConfigurations = null;
            if (UtilMethods.isSet(this.proxyUrl)) {
                final EMAConfigurationEntry configurationEntry = new EMAConfigurationEntry(ACCEPT_ALL, this.proxyUrl,
                        this.includeRendered, null);
                emaConfigurations = new EMAConfigurations(List.of(configurationEntry));
            } else {
                if (null != this.rewritesBean) {
                    final List<EMAConfigurationEntry> configurationEntries = new ArrayList<>();
                    for (final RewriteBean rewriteBean : this.rewritesBean.getRewrites()) {
                        configurationEntries.add(new EMAConfigurationEntry(rewriteBean.getSource(),
                                rewriteBean.getDestination(), this.includeRendered,
                                Map.of(EMAWebInterceptor.AUTHENTICATION_TOKEN_VAR, this.authenticationToken)));
                    }
                    emaConfigurations = new EMAConfigurations(configurationEntries);
                }
            }
            return Optional.ofNullable(emaConfigurations);
        }

    }

    /**
     * Retrieves the EMA configuration parameters based on the <b>new approach</b>. This one uses a single JSON field
     * that allows for more flexibility and the addition of parameters when required.
     */
    private static class JSONEMAConfig implements EMAConfigStrategy {

        private EMAConfigurations emaConfigurations;

        @Override
        public boolean test(final AppSecrets appSecrets) {
            final String paramValue =
                    Try.of(() -> appSecrets.getSecrets().get(EMAWebInterceptor.PROXY_EDIT_MODE_URL_VAR).getString()).getOrElse(StringPool.BLANK);
            if (StringUtils.isJson(paramValue)) {
                emaConfigurations =
                        Try.of(() -> JsonUtil.getObjectFromJson(appSecrets.getSecrets().get(EMAWebInterceptor.PROXY_EDIT_MODE_URL_VAR).getString(), EMAConfigurations.class))
                                .onFailure(e -> Logger.debug(JSONEMAConfig.class, e.getMessage(), e))
                                .getOrNull();
                return null != emaConfigurations;
            }
            return false;
        }

        @Override
        public Optional<EMAConfigurations> resolveConfig() {
            return Optional.ofNullable(this.emaConfigurations);
        }

    }

}
