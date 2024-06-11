import { dotcmsClient } from "@dotcms/client";

// Client for content fetching
export const client = dotcmsClient.init({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    authToken: "NO_TOKEN",
    siteId: "59bb8831-6706-4589-9ca0-ff74016e02b2",
});
