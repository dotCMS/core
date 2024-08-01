package com.dotcms.ai.api;

import com.dotcms.ai.model.AIProvider;
import com.dotmarketing.util.Logger;

public class AIProxy {

    public void attemptToConsumeAI(final AIProvider provider) {
        if (!provider.isEnabled()) {
            Logger.debug(this, "Provider " + provider.getProvider() + " is not enabled.");
            return;
        }


    }

}
