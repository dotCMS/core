package com.dotcms.security.apps;

import com.dotcms.system.event.local.model.KeyFilterable;
import com.dotmarketing.beans.Host;
import java.io.Serializable;

/**
 * AppSecretSavedEvent
 * Broadcast when a secret is saved.
 */
public class AppSecretSavedEvent implements Serializable, KeyFilterable {

   private final AppSecrets appSecrets;

   private final Host host;

    /**
     * Event constructor
     * @param appSecrets
     * @param host
     */
   AppSecretSavedEvent(final AppSecrets appSecrets, final Host host) {
        this.appSecrets = appSecrets;
        this.host = host;
   }

    /**
     * AppSecrets Getter
     * @return
     */
    public AppSecrets getAppSecrets() {
        return appSecrets;
    }

    /**
     * Host Getter
     * @return
     */
    public Host getHost() {
        return host;
    }

    /**
     * Only subscribers providing this key will receive this event.
     * This way we minimize the audience receiving the secret.
     * @return
     */
    @Override
    public Comparable getKey() {
        return appSecrets.getKey();
    }
}
