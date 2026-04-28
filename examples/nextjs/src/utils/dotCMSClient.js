import { createDotCMSClient } from "@dotcms/client";

export const dotCMSClient = createDotCMSClient({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    authToken: process.env.NEXT_PUBLIC_DOTCMS_AUTH_TOKEN,
    siteId: process.env.NEXT_PUBLIC_DOTCMS_SITE_ID,
    logLevel: process.env.NODE_ENV === "development" ? "verbose" : "default",
    requestOptions: {
        cache: "no-cache",
    },
});
