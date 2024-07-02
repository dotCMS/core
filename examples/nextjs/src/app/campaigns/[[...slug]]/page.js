import { notFound } from "next/navigation";

import { graphqlToPageEntity, getPageRequestParams } from "@dotcms/client";
import { MyPage } from "@/components/my-page";

import { getGraphQLPageData } from "@/utils/gql";
import { client } from "@/utils/dotcmsClient";

const getPath = (params) => {
    const defaultPath = "colorado-preseason-special";
    const path = "/campaigns/" + (params?.slug?.join("/") || defaultPath);

    return path;
};

/**
 * Generate metadata
 *
 * @export
 * @param {*} { params, searchParams }
 * @return {*}
 */
export async function generateMetadata({ params, searchParams }) {
    const path = getPath(params);
    const pageRequestParams = getPageRequestParams({
        path,
        params: searchParams,
    });

    try {
        const data = await client.page.get(pageRequestParams);
        const page = data.entity?.page;
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
    const path = getPath(params);
    const pageRequestParams = getPageRequestParams({
        path,
        params: searchParams,
    });
    const nav = await client.nav.get({
        path: "/",
        depth: 2,
        languageId: searchParams.language_id,
    });

    const data = await getGraphQLPageData(pageRequestParams);
    const pageAsset = graphqlToPageEntity(data);

    // GraphQL returns null if the page is not found
    // It does not throw an error
    if (!pageAsset) {
        notFound();
    }

    return <MyPage nav={nav.entity.children} pageAsset={pageAsset}></MyPage>;
}
