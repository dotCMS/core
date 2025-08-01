import { createDotCMSClient } from "@dotcms/client";

export const dotCMSClient = createDotCMSClient({
  authToken: import.meta.env.PUBLIC_DOTCMS_AUTH_TOKEN,
  dotcmsUrl: import.meta.env.PUBLIC_DOTCMS_HOST,
  siteId: import.meta.env.PUBLIC_DOTCMS_SITE_ID,
  requestOptions: {
    // In production you might want to deal with this differently
    cache: "no-cache",
  },
});

