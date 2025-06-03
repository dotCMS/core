import type { DotCMSNavigationItem, DotCMSPageAsset } from "@dotcms/types";
import { dotCMSClient } from "./dotCMSClient";

import {
  blogQuery,
  destinationQuery,
  fragmentNav,
  navigationQuery,
} from "./queries";

export const getDotCMSPage = async (
  path: string,
  searchParams?: URLSearchParams,
) => {
  // Avoid passing mode if you have a read only auth token
  const { mode, ...params } =
    Object.fromEntries(searchParams?.entries() ?? []) ?? {};
  try {
    const pageData = await dotCMSClient.page.get<{
      pageAsset: DotCMSPageAsset;
      content?: {
        navigation: {
          children: DotCMSNavigationItem[];
        };
      };
    }>(path, {
      ...params,
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
  } catch (e: any) {
    console.error("ERROR FETCHING PAGE: ", e.message);

    return null;
  }
};
