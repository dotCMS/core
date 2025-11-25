import { DotCmsClient, getPageRequestParams } from "@dotcms/client";
import type { DotcmsNavigationItem, DotCMSPageAsset } from "@dotcms/types";

export const client = DotCmsClient.init({
  authToken: import.meta.env.PUBLIC_DOTCMS_AUTH_TOKEN,
  dotcmsUrl: import.meta.env.PUBLIC_DOTCMS_HOST,
  siteId: import.meta.env.PUBLIC_DOTCMS_SITE_ID,
  requestOptions: {
    // In production you might want to deal with this differently
    cache: "no-cache",
  },
});

export type GetPageDataResponse = {
  pageAsset?: DotCMSPageAsset;
  nav?: DotcmsNavigationItem;
  error?: unknown;
};

export const getPageData = async (
  slug: string | undefined,
  params: URLSearchParams,
): Promise<GetPageDataResponse> => {
  try {
    const path = slug || "/";

    const pageRequestParams = getPageRequestParams({
      path,
      params,
    });

    const pageAsset = (await client.page.get({
      ...pageRequestParams,
      depth: 3,
    })) as DotCMSPageAsset;

    const { entity } = (await client.nav.get({
      path: "/",
      depth: 2,
      languageId: pageRequestParams.languageId as number,
    })) as { entity: DotcmsNavigationItem };

    return { pageAsset, nav: entity };
  } catch (error) {
    return { error };
  }
};
