package com.dotcms.analytics.listener;


import com.dotcms.analytics.AnalyticsAPI;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AnalyticsAppProperty;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.security.apps.AbstractProperty;
import com.dotcms.security.apps.AppSecretSavedEvent;
import com.dotcms.security.apps.Secret;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.model.KeyFilterable;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageUtil;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;


/**
 * Analytics app listener
 *
 * @author vico
 */
public final class AnalyticsAppListener implements EventSubscriber<AppSecretSavedEvent>, KeyFilterable {

    private final AnalyticsAPI analyticsAPI;
    private final HostAPI hostAPI;

    private AnalyticsAppListener(final AnalyticsAPI analyticsAPI, final HostAPI hostAPI) {
        this.analyticsAPI = analyticsAPI;
        this.hostAPI = hostAPI;
    }

    private AnalyticsAppListener() {
        this(APILocator.getAnalyticsAPI(), APILocator.getHostAPI());
    }

    /**
     * @return key to use when filtering event
     */
    @Override
    public Comparable<String> getKey() {
        return AnalyticsApp.ANALYTICS_APP_KEY;
    }

    /**
     * Run notifiable logic to detect when analytics app's key is empty and take action by resetting it by requesting
     * a new key to config server.
     *
     * @param event {@link AppSecretSavedEvent} instance
     */
    @Override
    public void notify(final AppSecretSavedEvent event) {
        if (Objects.isNull(event)) {
            Logger.debug(this, "Missing event, aborting");
            return;
        }

        if (StringUtils.isBlank(event.getHostIdentifier())) {
            Logger.debug(this, "Missing event's host id, aborting");
            return;
        }

        // detect blank analytics key to reset it
        resolveAnalyticsKey(event)
            .map(AbstractProperty::getString)
            .filter(StringUtils::isWhitespace)
            .ifPresent(analyticsKey -> {
                try {
                    final Host host = hostAPI.find(event.getHostIdentifier(), APILocator.systemUser(), false);
                    if (Objects.nonNull(host)) {
                        analyticsAPI.resetAnalyticsKey(AnalyticsHelper.getHostApp(host));
                    }
                } catch (Exception e) {
                    Logger.error(this, String.format("Cannot process event %s due to: %s", event, e.getMessage()), e);
                }
            });

        // detect is there are properties set through env vars
        final boolean appHasEnvVars = AnalyticsAppProperty.ENV_VAR_PROPERTIES
            .stream()
            .anyMatch(appProperty -> Config.isKeyEnvBased(appProperty.getEnvVarName()));
        if (appHasEnvVars) {
            pushEnvVarOverrideNotAllowedMessage();
        }
    }

    /**
     * Pushes message to notify that even though analytics app env-var properties were changed though the Analytics
     * App, those changes were ignored since they are govern by env-vars.
     */
    private void pushEnvVarOverrideNotAllowedMessage() {
        final String message = Try.of(() ->
            LanguageUtil.get(
                APILocator.systemUser(),
                AnalyticsApp.ANALYTICS_APP_OVERRIDE_NOT_ALLOWED_KEY))
            .getOrElse(() -> "dotAnalytics is configured using environmental variables - any configuration done within the App screen will be ignored");
        SystemMessageEventUtil.getInstance().pushMessage(
            SystemEventType.ANALYTICS_APP,
            new SystemMessageBuilder()
                .setMessage(message)
                .setLife(DateUtil.FIVE_SECOND_MILLIS)
                .setSeverity(MessageSeverity.WARNING).create(),
            null);
    }

    /**
     * Get an {@link Optional<Secret>} instance holding the value associated to the analytics key.
     *
     * @param event app secret saved event broadcast event
     * @return the secret in matter
     */
    private Optional<Secret> resolveAnalyticsKey(final AppSecretSavedEvent event) {
        return Optional.ofNullable(
            event.getAppSecrets()
                .getSecrets()
                .get(AnalyticsAppProperty.ANALYTICS_KEY.getPropertyName()));
    }

    public enum Instance {
        SINGLETON;

        private final AnalyticsAppListener provider = new AnalyticsAppListener();

        public static AnalyticsAppListener get() {
            return Instance.SINGLETON.provider;
        }
    }

}
