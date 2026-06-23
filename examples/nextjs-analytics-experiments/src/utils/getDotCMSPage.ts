import { cache } from "react";

import { dotCMSClient } from "@/lib/dotCMSClient";

export const getDotCMSPage = cache(async (path: string) => {
  try {
    return await dotCMSClient.page.get(path);
  } catch (error) {
    return { error };
  }
});
