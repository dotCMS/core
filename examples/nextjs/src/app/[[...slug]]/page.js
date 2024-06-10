import { notFound } from "next/navigation";

import { dotcmsClient } from "@dotcms/client";
import { MyPage } from "@/components/my-page";

import { handleVanityUrlRedirect } from "../../utils/vonityUrlHandler";

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
    return {
        path: params?.slug ? params.slug.join("/") : "index",
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
    const data = await client.page.get(requestData);
    const nav = await client.nav.get({
        path: "/",
        depth: 2,
        languageId: searchParams.language_id,
    });

    if (!data.entity) {
        notFound(); // NextJS 14 way to handle "Not Found" pages
    }

    const { vanityUrl } = data?.entity;

    if (vanityUrl) {
        handleVanityUrlRedirect(vanityUrl);
    }

    return <MyPage nav={nav.entity.children} pageAsset={data.entity}></MyPage>;
}
