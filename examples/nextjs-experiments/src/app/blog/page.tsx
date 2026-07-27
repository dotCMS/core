import type { Metadata } from "next";
import { redirect } from "next/navigation";

import NotFound from "@/app/not-found";
import { BlogListingPage } from "@/views/BlogListingPage";
import { ErrorPage } from "@/components/error";
import { getDotCMSPage } from "@/utils/getDotCMSPage";
import { getErrorStatus, getPageTitle, isPageError } from "@/utils/pageResponse";

export async function generateMetadata(): Promise<Metadata> {
    const pageResponse = await getDotCMSPage(`/blog`);

    if (isPageError(pageResponse)) {
        return { title: "Error" };
    }

    return { title: `${getPageTitle(pageResponse, "Not Found")} - Blog` };
}

export default async function Home() {
    const pageResponse = await getDotCMSPage(`/blog`);

    if (isPageError(pageResponse)) {
        return <ErrorPage error={{ status: getErrorStatus(pageResponse.error) }} />;
    }

    const vanityUrl = pageResponse.pageAsset?.vanityUrl;
    const action = vanityUrl?.action ?? 0;

    if (action > 200 && vanityUrl?.forwardTo) {
        redirect(vanityUrl.forwardTo);
    }

    if (!pageResponse.pageAsset) {
        return <NotFound />;
    }

    return <BlogListingPage {...pageResponse} />;
}
