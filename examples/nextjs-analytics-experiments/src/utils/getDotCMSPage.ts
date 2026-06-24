import { cache } from "react";

import { dotCMSClient } from "@/lib/dotCMSClient";

/**
 * dotCMS stores "folder index" pages as /index, /section/index, etc.
 * Map trailing-slash paths and bare "/" to their dotCMS equivalent.
 */
function toDotCMSPath(path: string): string {
  if (path === "/") return "/index";
  if (path.endsWith("/")) return `${path}index`;
  return path;
}

export const getDotCMSPage = cache(
  async (path: string, variantName?: string) => {
    try {
      return await dotCMSClient.page.get(
        toDotCMSPath(path),
        variantName ? { variantName } : undefined
      );
    } catch (error) {
      return { error };
    }
  }
);
