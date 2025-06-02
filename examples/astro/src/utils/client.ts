import { createDotCMSClient } from "@dotcms/client/next";
import type { DotCMSComposedPageResponse, DotCMSNavigationItem, DotCMSPageAsset } from "@dotcms/types";
import {
  blogQuery,
  destinationQuery,
  fragmentNav,
  navigationQuery,
} from "./queries";

export const dotCMSClient = createDotCMSClient({
  authToken: import.meta.env.PUBLIC_DOTCMS_AUTH_TOKEN,
  dotcmsUrl: import.meta.env.PUBLIC_DOTCMS_HOST,
  siteId: import.meta.env.PUBLIC_DOTCMS_SITE_ID,
  requestOptions: {
    // In production you might want to deal with this differently
    cache: "no-cache",
  },
});

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
