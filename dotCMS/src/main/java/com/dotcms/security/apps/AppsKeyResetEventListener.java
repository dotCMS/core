package com.dotcms.security.apps;

import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.type.security.CompanyKeyResetEvent;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import java.io.IOException;

/**
 * Once registered this listener takes care of resetting the
 */
public final class AppsKeyResetEventListener implements EventSubscriber<CompanyKeyResetEvent> {

    private final AppsAPI appsAPI;

    private AppsKeyResetEventListener(final AppsAPI appsAPI) {
        this.appsAPI = appsAPI;
    }

    private AppsKeyResetEventListener() {
       this(APILocator.getAppsAPI());
    }

    @Override
    public void notify(final CompanyKeyResetEvent event) {
        try {
            appsAPI.resetSecrets(APILocator.systemUser());
        } catch (IOException | DotDataException e) {
            Logger.error(AppsKeyResetEventListener.class, "An exception occurred while handling a company key reset event.  ", e);
        }
    }

    public enum INSTANCE {
        SINGLETON;
        final AppsKeyResetEventListener provider = new AppsKeyResetEventListener();

        public static AppsKeyResetEventListener get() {
            return SINGLETON.provider;
        }

    }
}
