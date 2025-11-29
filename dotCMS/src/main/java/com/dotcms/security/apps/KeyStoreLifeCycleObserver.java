package com.dotcms.security.apps;

import com.dotcms.security.apps.KeyStoreManager.KeyStoreCreatedEvent;
import com.dotcms.security.apps.KeyStoreManager.KeyStoreLoadedEvent;
import com.dotcms.security.apps.KeyStoreManager.KeyStoreSavedEvent;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class KeyStoreLifeCycleObserver {

    final AppsCache appsCache = CacheLocator.getAppsCache();

    public void onCreated(@Observes KeyStoreCreatedEvent event) {
        Logger.info(this, "KeyStore created: " + event.getKeyStorePath());

    }

    public void onLoaded(@Observes KeyStoreLoadedEvent event) {
        Logger.info(this, "KeyStore loaded with " + event.getEntryCount() + " entries");
    }

    public void onSaved(@Observes KeyStoreSavedEvent event) {
        Logger.info(this, "KeyStore saved with " + event.getEntryCount() + " entries");
        appsCache.flushSecret();
    }
}

