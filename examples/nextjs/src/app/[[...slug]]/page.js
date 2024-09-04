import { MyPage } from "@/components/my-page";
import { ErrorPage } from "@/components/error";

import { handleVanityUrlRedirect } from "@/utils/vanityUrlHandler";
import { client } from "@/utils/dotcmsClient";
import { getPageRequestParams } from "@dotcms/client";

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
        try {
            const path = params?.slug?.join("/") || "/";
            const pageRequestParams = getPageRequestParams({
                path,
                params: searchParams,
            });
            const pageAsset = await client.page.get({
                ...pageRequestParams,
                depth: 3,
            });
            const nav = await client.nav.get({
                path: "/",
                depth: 2,
                languageId: searchParams.language_id,
            });

            return { pageAsset, nav };
        } catch (error) {
            return { pageAsset: null, nav: null, error };
        }
    };
    const { pageAsset, nav, error } = await getPageData();

    if (error) {
        return <ErrorPage error={error} />;
    }

    const { vanityUrl } = pageAsset;

    if (vanityUrl) {
        handleVanityUrlRedirect(vanityUrl);
    }

    return <MyPage nav={nav.entity.children} pageAsset={pageAsset}></MyPage>;
}
