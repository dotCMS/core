import { notFound } from "next/navigation";

import { MyPage } from "@/components/my-page";

import { handleVanityUrlRedirect } from "@/utils/vanityUrlHandler";
import { client, getRequestParams } from "@/utils/dotcmsClient";

/**
 * Generate metadata
 *
 * @export
 * @param {*} { params, searchParams }
 * @return {*}
 */
export async function generateMetadata({ params, searchParams }) {
    const requestData = getRequestParams({ params, searchParams, defaultPath: '/' });
    const data = await client.page.get(requestData);
    const page = data.entity?.page;

    const title = page?.friendlyName || page?.title || "not found";

    return {
        title,
    };
}

export default async function Home({ searchParams, params }) {
    const requestData = getRequestParams({ params, searchParams, defaultPath: '/' });
    const data = await client.page.get(requestData);
    const nav = await client.nav.get({
        path: "/",
        depth: 2,
        languageId: searchParams.language_id,
    });
    
    if(!data?.entity) {
        notFound(); 
    }

    const { vanityUrl } = data?.entity;

    if (vanityUrl) {
        handleVanityUrlRedirect(vanityUrl);
    }

    return <MyPage nav={nav.entity.children} pageAsset={data.entity}></MyPage>;
}
