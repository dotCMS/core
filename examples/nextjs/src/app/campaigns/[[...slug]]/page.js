import { notFound } from "next/navigation";

import { graphqlToPageEntity, getPageRequestParams } from "@dotcms/client";
import { MyGraphQLPage } from "@/components/graphql-page";

import { getGraphQLPageData, getGraphQLPageQuery } from "@/utils/gql";
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
        const page = data?.page;
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

    const query = getGraphQLPageQuery(pageRequestParams);
    const data = await getGraphQLPageData(query);
    const pageAsset = graphqlToPageEntity(data);

    // GraphQL returns null if the page is not found
    // It does not throw an error
    if (!pageAsset) {
        notFound();
    }

    return <MyGraphQLPage nav={nav.entity.children} pageAsset={pageAsset} query={query} />;
}
