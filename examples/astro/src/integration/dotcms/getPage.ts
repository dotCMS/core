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

/**
 * The outcome of a page fetch, discriminated by an explicit `ok` flag.
 *
 * The flag is deliberate. A successful page response carries its own optional
 * `error` for GraphQL problems, so checking for the presence of an `error` key
 * cannot tell a failed fetch from a successful one and leaves both branches
 * untyped at every call site.
 */
export type DotCMSPageResult<
  T extends DotCMSExtendedPageResponse = DotCMSCustomPageResponse,
> =
  | ({ ok: true } & DotCMSComposedPageResponse<T>)
  | { ok: false; error: DotErrorPage };

export const getDotCMSPage = async <
  T extends DotCMSExtendedPageResponse = DotCMSCustomPageResponse,
>(
  path: string = "/",
): Promise<DotCMSPageResult<T>> => {
  try {
    const response = await dotCMSClient.page.get<T>(path, {
      graphql: {
        content: {
          blogs: blogQuery,
          destinations: destinationQuery,
          navigation: navigationQuery,
        },
        fragments: [fragmentNav],
      },
    });

    return { ...response, ok: true };
  } catch (e) {
    if (e instanceof DotErrorPage) {
      return { ok: false, error: e };
    }

    return {
      ok: false,
      error: new DotErrorPage(e instanceof Error ? e.message : String(e)),
    };
  }
};
