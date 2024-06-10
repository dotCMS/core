import { notFound } from "next/navigation";

import { dotcmsClient, graphqlToPageEntity } from "@dotcms/client";
import { MyPage } from "@/components/my-page";

import { getGraphQLPageData } from "../../../utils/gql";

const client = dotcmsClient.init({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    authToken: process.env.DOTCMS_AUTH_TOKEN,
    siteId: "59bb8831-6706-4589-9ca0-ff74016e02b2",
    requestOptions: {
        // In production you might want to deal with this differently
        cache: "no-cache",
    },
});

/**
 * Get request params
 *
 * @param {*} { params, searchParams }
 * @return {*}
 */
export const getRequestParams = ({ params, searchParams }) => {
    const slug = params?.slug.join("/") || "";
    const path = "/campaigns/" + slug;

    return {
        path,
        language_id: searchParams.language_id,
        "com.dotmarketing.persona.id":
            searchParams["com.dotmarketing.persona.id"] || "",
        mode: searchParams.mode,
        variantName: searchParams["variantName"],
    };
};

/**
 * Generate metadata
 *
 * @export
 * @param {*} { params, searchParams }
 * @return {*}
 */
export async function generateMetadata({ params, searchParams }) {
    const requestData = getRequestParams({ params, searchParams });
    const data = await client.page.get(requestData);
    const page = data.entity?.page;

    const title = page?.friendlyName || page?.title || "not found";

    return {
        title,
    };
}

export default async function Home({ searchParams, params }) {
    const requestData = getRequestParams({ params, searchParams });
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
