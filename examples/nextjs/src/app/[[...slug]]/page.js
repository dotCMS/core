import { MyPage } from "@/components/my-page";
import { ErrorPage } from "@/components/error";

import { handleVanityUrlRedirect } from "@/utils/vanityUrlHandler";
import { client } from "@/utils/dotcmsClient";
import { getPageRequestParams } from "@dotcms/client";
import { fetchNavData, fetchPageData } from "@/utils/page.utils";

/**
 * Generate metadata
 *
 * @export
 * @param {*} { params, searchParams }
 * @return {*}
 */
export async function generateMetadata({ params, searchParams }) {
    const path = params?.slug?.join("/") || "/";
    const pageRequestParams = getPageRequestParams({
        path,
        params: searchParams,
    });

    try {
        const data = await client.page.get(pageRequestParams);
        const page = data.page;
        const title = page?.friendlyName || page?.title;

        return {
            title,
        };
    } catch (e) {
        return {
            title: "not found",
        };
    }
}

export default async function Home({ searchParams, params }) {
    const getPageData = async () => {
        const path = params?.slug?.join("/") || "/";
        const pageParams = getPageRequestParams({
            path,
            params: searchParams,
        });

        const { pageAsset, error: pageError } = await fetchPageData(pageParams);
        const { nav, error: navError } = await fetchNavData(pageParams.language_id);

        return {
            nav,
            pageAsset,
            error: pageError || navError,
        };
    };
    const { pageAsset, nav, error } = await getPageData();

    // Move this to MyPage
    if (error) {
        return <ErrorPage error={error} />;
    }

    if (pageAsset?.vanityUrl) {
        handleVanityUrlRedirect(pageAsset?.vanityUrl);
    }

    return <MyPage nav={nav?.entity.children} pageAsset={pageAsset} />;
}
