package com.dotcms.ai.listener;

import com.dotcms.ai.app.AIModels;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.security.apps.AppSecretSavedEvent;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.model.KeyFilterable;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * This class listens to events related to the AI application and performs actions based on those events.
 * It implements the EventSubscriber interface and overrides its methods to provide custom functionality.
 * The class also implements the KeyFilterable interface to filter events based on a specific key.
 *
 * @author vico
 */
public final class AIAppListener implements EventSubscriber<AppSecretSavedEvent>, KeyFilterable {

    private final HostAPI hostAPI;

    public AIAppListener(final HostAPI hostAPI) {
        this.hostAPI = hostAPI;
    }

    public AIAppListener() {
        this(APILocator.getHostAPI());
    }

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

        final String hostId = event.getHostIdentifier();
        final Host host = Try.of(() -> hostAPI.find(hostId, APILocator.systemUser(), false)).getOrNull();

        Optional.ofNullable(host).ifPresent(found -> AIModels.get().resetModels(found));
    }

    @Override
    public Comparable<String> getKey() {
        return AppKeys.APP_KEY;
    }

    public enum Instance {
        SINGLETON;

        private final AIAppListener provider = new AIAppListener();

        public static AIAppListener get() {
            return AIAppListener.Instance.SINGLETON.provider;
        }
    }

}
