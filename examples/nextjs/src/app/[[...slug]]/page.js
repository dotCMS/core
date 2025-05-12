import { cache } from "react";

import { MyPage } from "@/components/my-page";
import { dotClient } from "@/utils/dotClient";

export async function generateMetadata({ params, searchParams }) {
    try {
        const { pageAsset } = await getPageData(params, searchParams);
        const page = pageAsset.page;
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

export default async function Home({ params, searchParams }) {
    const pageContent = await getPageData(params, searchParams);
    return <MyPage pageContent={pageContent} />;
}

export const getPageData = cache(async (params, searchParams) => {
    try {
        const path = params?.slug?.join("/") || "/";
        const pageData = await dotClient.page.get(path);
        return pageData;
    } catch (e) {
        return {
            pageAsset: null,
        };
    }
});
