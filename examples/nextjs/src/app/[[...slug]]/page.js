import { cache } from "react";

import NotFound from "@/app/not-found";
import { Page } from "@/pages/Page";
import { getDotCMSPage } from "@/utils/getDotCMSPage";

export async function generateMetadata({ params, searchParams }) {
    try {
        const path = params?.slug?.join("/") || "/";
        const { pageAsset } = await getDotCMSPage(path, searchParams);
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
    const pageContent = await getDotCMSPage(path, searchParams);

    if (!pageContent) {
        return <NotFound />;
    }

    return <Page pageContent={pageContent} />;
}
