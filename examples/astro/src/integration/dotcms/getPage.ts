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
