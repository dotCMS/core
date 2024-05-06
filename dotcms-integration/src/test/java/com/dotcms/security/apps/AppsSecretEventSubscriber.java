package com.dotcms.security.apps;

import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.model.KeyFilterable;

public abstract class AppsSecretEventSubscriber implements EventSubscriber<AppSecretSavedEvent>  ,  KeyFilterable {

    static final String appKey = "any-app-key";

    @Override
    public Comparable getKey() {
        return appKey;
    }
}
