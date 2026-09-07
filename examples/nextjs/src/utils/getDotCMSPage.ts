import { cache } from "react";

import { DotErrorPage } from "@dotcms/types";

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
 * On failure it returns `{ error, graphql }` so callers can branch without try/catch; use the
 * guards in `@/utils/pageResponse` to narrow the result. `graphql` is the query dotCMS attempted
 * before failing (present whenever the error is a `DotErrorPage`) - inside the UVE editor, passing
 * it to `useEditableDotCMSPage` lets the editor retry the fetch with edit-mode permissions and
 * deliver a draft/non-live page instead of leaving the request stuck on this failure.
 */
export const getDotCMSPage = cache(async (path: string) => {
  try {
    return await dotCMSClient.page.get<{ content: PageExtraContent }>(path, {
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
    return {
      error,
      graphql: error instanceof DotErrorPage ? error.graphql : undefined,
    };
  }
});
