import { cache } from "react";

import { dotCMSClient } from "@/lib/dotCMSClient";
import type { PageExtraContent } from "@/types/content";
import {
  blogQuery,
  destinationQuery,
  fragmentNav,
  navigationQuery,
} from "@/utils/queries";

/**
 * Fetch a dotCMS page plus the extra GraphQL content (blogs, destinations,
 * navigation) the layout needs. Wrapped in React `cache()` so multiple callers
 * within a single request (e.g. `generateMetadata` + the page body) share one
 * network round-trip.
 *
 * On failure it returns `{ error }` so callers can branch without try/catch;
 * use the guards in `@/utils/pageResponse` to narrow the result.
 */
export const getDotCMSPage = cache(async (path: string, variantName?: string) => {
  try {
    return await dotCMSClient.page.get<{ content: PageExtraContent }>(path, {
      variantName,
      graphql: {
        content: {
          blogs: blogQuery,
          destinations: destinationQuery,
          navigation: navigationQuery,
        },
        fragments: [fragmentNav],
      },
    });
  } catch (error) {
    return { error };
  }
});
