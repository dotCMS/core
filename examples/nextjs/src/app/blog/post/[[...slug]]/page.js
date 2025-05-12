import { cache } from "react";

import { dotClient } from "@/utils/dotClient";
import { DetailPage } from "@/pages/DetailPage";

export async function generateMetadata({ params, searchParams }) {
    const path = params.slug?.join("/");
    const { pageAsset } = await getPageData(path, searchParams);
    const page = pageAsset?.page;
    const title = page?.friendlyName || page?.title || "Page not found";
    return {
        title
    };
}

export default async function Home({ searchParams, params }) {
    const path = params?.slug?.join("/");
    const pageContent = await getPageData(path, searchParams);
    return <DetailPage pageContent={pageContent} />;
}

export const getPageData = cache(async (path, searchParams) => {
    try {
        const BASE_PATH = "/blog/post";
        const pageData = await dotClient.page.get(`${BASE_PATH}/${path}`);
        return pageData;
    } catch (e) {
        return {
            pageAsset: null,
        };
    }
});