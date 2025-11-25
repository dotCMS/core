import { DotCmsClient } from "@dotcms/client";

// Client for content fetching
export const client = DotCmsClient.init({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    authToken: process.env.NEXT_PUBLIC_DOTCMS_AUTH_TOKEN,
    siteId: "59bb8831-6706-4589-9ca0-ff74016e02b2",
    requestOptions: {
        // In production you might want to deal with this differently
        cache: "no-cache",
    }
});
