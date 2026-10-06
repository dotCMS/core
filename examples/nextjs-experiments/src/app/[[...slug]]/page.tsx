import type { Metadata } from "next";
import { redirect } from "next/navigation";

import { DotCMSExperiment } from "@dotcms/events/react";

import NotFound from "@/app/not-found";
import { ErrorPage } from "@/components/error";
import { getDotCMSPage } from "@/utils/getDotCMSPage";
import { getErrorStatus, getPageTitle, isPageError } from "@/utils/pageResponse";
import { Page } from "@/views/Page";

interface SlugPageProps {
    params: Promise<{ slug?: string[] }>;
    searchParams: Promise<{ variantName?: string }>;
}

function getPath(slug?: string[]) {
    return slug?.length ? `/${slug.join("/")}` : "/";
}

export async function generateMetadata({
    params,
    searchParams,
}: SlugPageProps): Promise<Metadata> {
    const { slug } = await params;
    const { variantName } = await searchParams;
    const pageContent = await getDotCMSPage(getPath(slug), variantName);

    if (isPageError(pageContent)) {
        return { title: "Error" };
    }

    return { title: getPageTitle(pageContent, "Not Found") };
}

export default async function Home({ params, searchParams }: SlugPageProps) {
    const { slug } = await params;
    const path = getPath(slug);
    const { variantName } = await searchParams;
    const pageContent = await getDotCMSPage(path, variantName);

    if (isPageError(pageContent)) {
        return <ErrorPage error={{ status: getErrorStatus(pageContent.error) }} />;
    }

    const vanityUrl = pageContent.pageAsset?.vanityUrl;
    const action = vanityUrl?.action ?? 0;

    if (action > 200 && vanityUrl?.forwardTo) {
        redirect(vanityUrl.forwardTo);
    }

    if (!pageContent.pageAsset) {
        return <NotFound />;
    }

    // Rendered here, in a server component, the experiment's markup costs the client no JavaScript
    return (
        <DotCMSExperiment page={pageContent.pageAsset}>
            <Page pageContent={pageContent} />
        </DotCMSExperiment>
    );
}
