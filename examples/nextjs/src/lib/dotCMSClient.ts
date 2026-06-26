import { createDotCMSClient } from "@dotcms/client";

import {
  dotCMSAuthToken,
  dotCMSHost,
  dotCMSSiteId,
} from "@/config/dotcms.config";

export const dotCMSClient = createDotCMSClient({
  dotcmsUrl: dotCMSHost,
  authToken: dotCMSAuthToken,
  siteId: dotCMSSiteId,
  logLevel: process.env.NODE_ENV === "development" ? "verbose" : "default",
  requestOptions: {
    // UVE needs fresh data so in-context edits are reflected immediately.
    cache: "no-cache",
  },
});
