import { createDotCMSVue } from '@dotcms/vue';

import {
    dotCMSAuthToken,
    dotCMSHost,
    dotCMSLogLevel,
    dotCMSSiteId
} from '@/config/dotcms.config';

/**
 * The dotCMS Vue plugin. Installed once in `main.ts` with `app.use(dotCMSVue)`,
 * which provides the client to the whole app — components retrieve it with
 * `useDotCMSClient()` instead of importing a singleton.
 */
export const dotCMSVue = createDotCMSVue({
    dotcmsUrl: dotCMSHost,
    authToken: dotCMSAuthToken,
    siteId: dotCMSSiteId,
    logLevel: dotCMSLogLevel,
    requestOptions: {
        // UVE needs fresh data so in-context edits are reflected immediately.
        cache: 'no-cache'
    }
});

/**
 * The same client instance the plugin provides. Use this from code that runs
 * outside a component `setup` — e.g. the router's page loader — where
 * `useDotCMSClient()` cannot be called.
 */
export const dotCMSClient = dotCMSVue.client;
