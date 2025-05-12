import { client } from "@/utils/dotClient";

import { DetailPage } from "@/pages/DetailPage";
import { ErrorPage } from "@/components/error";
import { fetchNavData, fetchPageData } from "@/utils/page.utils";
import { getPageRequestParams } from "@dotcms/client";
import { handleVanityUrlRedirect } from "@/utils/vanityUrlHandler";

/**
 * Generate metadata
 *
 * @export
 * @param {*} { params, searchParams }
 * @return {*}
 */
export async function generateMetadata({ params, searchParams }) {
    const path = params?.slug?.join("/");
    const pageRequestParams = getPageRequestParams({
        path: `blog/post/${path}`,
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
        const path = params?.slug?.join("/");
        console.log(path)
        const pageParams = getPageRequestParams({
            path: `blog/post/${path}`,
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

    return <DetailPage nav={nav?.entity.children} initialPageAsset={pageAsset} />;
}
