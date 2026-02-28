import type { DotCMSCustomPageResponse } from "@/types/page.model";
import type {
  DotCMSComposedPageResponse,
  DotCMSExtendedPageResponse,
} from "@dotcms/types";

import { dotCMSClient } from "./dotCMSClient";

import {
  blogQuery,
  destinationQuery,
  fragmentNav,
  navigationQuery,
} from "./queries";

export const getDotCMSPage = <
  T extends DotCMSExtendedPageResponse = DotCMSCustomPageResponse,
>(
  path: string,
): Promise<DotCMSComposedPageResponse<T>> => {
  const pageData = dotCMSClient.page.get<T>(path, {
    graphql: {
      content: {
        blogs: blogQuery,
        destinations: destinationQuery,
        navigation: navigationQuery,
      },
      fragments: [fragmentNav],
    },
  });

  return pageData;
};
