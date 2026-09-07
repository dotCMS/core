package com.dotcms.ai.listener;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.client.langchain4j.LangChain4jAIClient;
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

    /**
     * Notifies the listener of an {@link AppSecretSavedEvent}.
     *
     * <p>
     * This method is called when an {@link AppSecretSavedEvent} occurs. It performs the following actions:
     * <ul>
     *   <li>Logs a debug message if the event is null or the event's host identifier is blank.</li>
     *   <li>Finds the host associated with the event's host identifier.</li>
     *   <li>Evicts cached LangChain4J model instances for the host so new credentials take effect immediately.</li>
     * </ul>
     * </p>
     *
     * @param event the {@link AppSecretSavedEvent} that triggered the notification
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

        final String hostId = event.getHostIdentifier();
        final Host host = Try.of(() -> hostAPI.find(hostId, APILocator.systemUser(), false)).getOrNull();

        Optional.ofNullable(host).ifPresent(found -> LangChain4jAIClient.get().flushCachesForHost(found.getHostname()));
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
