import { notFound } from "next/navigation";

import { dotcmsClient, graphqlToPageEntity } from "@dotcms/client";
import { MyPage } from "@/components/my-page";

import { getGraphQLPageData } from "@/utils/gql";
import { client, getRequestParams } from "@/utils/dotcmsClient";

/**
 * Generate metadata
 *
 * @export
 * @param {*} { params, searchParams }
 * @return {*}
 */
export async function generateMetadata({ params, searchParams }) {
    const requestData = getRequestParams({
        params,
        searchParams,
        defaultPath: "/campaigns/colorado-preseason-special",
    });
    const data = await client.page.get(requestData);
    const page = data.entity?.page;

    const title = page?.friendlyName || page?.title || "not found";

    return {
        title,
    };
}

export default async function Home({ searchParams, params }) {
    const requestData = getRequestParams({
        params,
        searchParams,
        defaultPath: "/campaigns/colorado-preseason-special",
    });
    const nav = await client.nav.get({
        path: "/",
        depth: 2,
        languageId: searchParams.language_id,
    });

    const data = await getGraphQLPageData(requestData);
    const pageAsset = graphqlToPageEntity(data);

    if (!pageAsset) {
        notFound(); // NextJS 14 way to handle "Not Found" pages
    }

    return <MyPage nav={nav.entity.children} pageAsset={pageAsset}></MyPage>;
}
