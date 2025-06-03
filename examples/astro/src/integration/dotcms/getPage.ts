import type { DotCMSCustomPageResponse } from "@/types/page.model";

import { dotCMSClient } from "./dotCMSClient";

import {
  blogQuery,
  destinationQuery,
  fragmentNav,
  navigationQuery,
} from "./queries";

export const getDotCMSPage = async (
  path: string,
  _searchParams?: URLSearchParams,
): Promise<DotCMSCustomPageResponse> => {
  const pageData = await dotCMSClient.page.get<DotCMSCustomPageResponse>(path, {
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
