import { createDotCMSClient } from '@dotcms/client';

import {
    dotCMSAuthToken,
    dotCMSHost,
    dotCMSLogLevel,
    dotCMSSiteId
} from '@/config/dotcms.config';

export const dotCMSClient = createDotCMSClient({
    dotcmsUrl: dotCMSHost,
    authToken: dotCMSAuthToken,
    siteId: dotCMSSiteId,
    logLevel: dotCMSLogLevel,
    requestOptions: {
        // UVE needs fresh data so in-context edits are reflected immediately.
        cache: 'no-cache'
    }
});
