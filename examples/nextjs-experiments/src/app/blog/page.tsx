import type { Metadata } from "next";
import { redirect } from "next/navigation";

import { DotCMSExperiment } from "@dotcms/events/react";

import NotFound from "@/app/not-found";
import { BlogListingPage } from "@/views/BlogListingPage";
import { ErrorPage } from "@/components/error";
import { getDotCMSPage } from "@/utils/getDotCMSPage";
import { getErrorStatus, getPageTitle, isPageError } from "@/utils/pageResponse";

interface BlogPageProps {
    searchParams: Promise<{ variantName?: string }>;
}

export async function generateMetadata({ searchParams }: BlogPageProps): Promise<Metadata> {
    const { variantName } = await searchParams;
    const pageResponse = await getDotCMSPage(`/blog`, variantName);

    if (isPageError(pageResponse)) {
        return { title: "Error" };
    }

    return { title: `${getPageTitle(pageResponse, "Not Found")} - Blog` };
}

// The variant an experiment on this page sends the visitor to arrives as variantName
export default async function Home({ searchParams }: BlogPageProps) {
    const { variantName } = await searchParams;
    const pageResponse = await getDotCMSPage(`/blog`, variantName);

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

    return (
        <DotCMSExperiment page={pageResponse.pageAsset}>
            <BlogListingPage {...pageResponse} />
        </DotCMSExperiment>
    );
}
