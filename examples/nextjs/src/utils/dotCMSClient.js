import { createDotCMSClient } from "@dotcms/client";

export const dotCMSClient = createDotCMSClient({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    authToken: process.env.NEXT_PUBLIC_DOTCMS_AUTH_TOKEN,
    siteId: process.env.NEXT_PUBLIC_DOTCMS_SITE_ID,
    requestOptions: {
        cache: "no-cache",
    },
});
