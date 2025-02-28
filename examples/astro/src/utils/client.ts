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
) => {
  const path = slug || "/";
  const pageParams = getPageRequestParams({
    path,
    params,
  });

  const { pageAsset, error: pageError } = await fetchPageData(pageParams);
  const { nav, error: navError } = await fetchNavData(pageParams.language_id);

  return {
    nav,
    pageAsset,
    error: pageError || navError,
  };
};

const fetchPageData = async (params: any) => {
  try {
    const pageAsset = (await client.page.get({
      ...params,
      depth: 3,
    })) as DotCMSPageAsset;

    return { pageAsset };
  } catch (error: any) {
    if (error?.status === 404) {
      return { pageAsset: undefined, error: undefined };
    }

    return { pageAsset: undefined, error };
  }
};

const fetchNavData = async (languageId = 1) => {
  try {
    const { entity } = (await client.nav.get({
      path: "/",
      depth: 2,
      languageId,
    })) as { entity: DotcmsNavigationItem };

    return { nav: entity };
  } catch (error) {
    return { nav: undefined, error };
  }
};
