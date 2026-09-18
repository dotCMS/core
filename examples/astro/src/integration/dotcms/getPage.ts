import type { DotCMSCustomPageResponse } from "@/types/page.model";
import type {
  DotCMSComposedPageResponse,
  DotCMSExtendedPageResponse,
} from "@dotcms/types";
import { DotErrorPage } from "@dotcms/types";

import { dotCMSClient } from "./dotCMSClient";

import {
  blogQuery,
  destinationQuery,
  fragmentNav,
  navigationQuery,
} from "./queries";

export const getDotCMSPage = async <
  T extends DotCMSExtendedPageResponse = DotCMSCustomPageResponse,
>(
  path: string = "/",
): Promise<DotCMSComposedPageResponse<T> | { error: DotErrorPage }> => {
  try {
    return await dotCMSClient.page.get<T>(path, {
      graphql: {
        content: {
          blogs: blogQuery,
          destinations: destinationQuery,
          navigation: navigationQuery,
        },
        fragments: [fragmentNav],
      },
    });
  } catch (e) {
    if (e instanceof DotErrorPage) {
      return { error: e };
    }

    return { error: new DotErrorPage(e instanceof Error ? e.message : String(e)) };
  }
};

/**
 * Narrows a {@link getDotCMSPage} result to the failure case.
 *
 * `"error" in response` does not work here: a *successful* page response also carries an
 * optional `error` field (the deprecated first GraphQL error), so the `in` check is true for
 * both branches and TypeScript keeps the union. Testing for the thrown `DotErrorPage`
 * instance is what actually separates "the page could not be fetched" from "the page loaded
 * and GraphQL reported something".
 *
 * @param response what getDotCMSPage returned
 * @returns true when the page could not be fetched
 */
export const isPageFetchError = <T extends DotCMSExtendedPageResponse>(
  response: DotCMSComposedPageResponse<T> | { error: DotErrorPage },
): response is { error: DotErrorPage } => response.error instanceof DotErrorPage;
