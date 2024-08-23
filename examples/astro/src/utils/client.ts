import { DotCmsClient, getPageRequestParams } from "@dotcms/client";
import type { DotcmsNavigationItem, DotCMSPageAsset } from "../types";
import type { AstroGlobal } from "astro";

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
  searchParams: Record<string, string | undefined>,
): Promise<GetPageDataResponse> => {
  try {
    const path = slug || "/";

    const pageRequestParams = getPageRequestParams({
      path,
      params: searchParams,
    });

    const pageAsset = (await client.page.get(
      pageRequestParams as any,
    )) as DotCMSPageAsset;

    const { entity } = (await client.nav.get({
      path: "/",
      depth: 2,
      languageId: pageRequestParams.language_id,
    })) as { entity: DotcmsNavigationItem };

    return { pageAsset, nav: entity };
  } catch (error) {
    return { error };
  }
};
