package com.dotcms.analytics.listener;


import com.dotcms.analytics.AnalyticsAPI;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AnalyticsAppProperty;
import com.dotcms.security.apps.AbstractProperty;
import com.dotcms.security.apps.AppSecretSavedEvent;
import com.dotcms.security.apps.Secret;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * Analytics app listener
 *
 * @author vico
 */
public final class AnalyticsAppListener implements EventSubscriber<AppSecretSavedEvent> {

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

        resolveAnalyticsKey(event)
            .map(AbstractProperty::getString)
            .filter(StringUtils::isNotBlank)
            .ifPresent(analyticsKey -> {
                try {
                    final Host host = hostAPI.find(event.getHostIdentifier(), APILocator.systemUser(), false);
                    analyticsAPI.resetAnalyticsKey(AnalyticsHelper.getHostApp(host));
                } catch (Exception e) {
                    Logger.error(this, String.format("Cannot process event %s due to: %s", event, e.getMessage()), e);
                }
            });
    }

    private Optional<Secret> resolveAnalyticsKey(final AppSecretSavedEvent event) {
        return Optional.ofNullable(
            event.getAppSecrets()
                .getSecrets()
                .get(AnalyticsAppProperty.ANALYTICS_KEY.getPropertyName()));
    }

    public enum INSTANCE {
        SINGLETON;

        private final AnalyticsAppListener provider = new AnalyticsAppListener();

        public static AnalyticsAppListener get() {
            return INSTANCE.SINGLETON.provider;
        }
    }

}
