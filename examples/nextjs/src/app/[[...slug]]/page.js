import { cache } from "react";

import { Page } from "@/pages/Page";
import { getPage } from "@/utils/getPage";

export async function generateMetadata({ params, searchParams }) {
    try {
        const path = params?.slug?.join("/") || "/";
        const { pageAsset } = await getPage(path, searchParams);
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
    const path = params?.slug?.join("/") || "/";
    const pageContent = await getPage(path, searchParams);
    return <Page pageContent={pageContent} />;
}
