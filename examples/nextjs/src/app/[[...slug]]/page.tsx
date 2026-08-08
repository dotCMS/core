import type { Metadata } from "next";
import { isRequestFromUVE } from "@dotcms/uve";
import { redirect } from "next/navigation";

import NotFound from "@/app/not-found";
import { ErrorPage } from "@/components/error";
import { getDotCMSPage } from "@/utils/getDotCMSPage";
import { getErrorStatus, getPageTitle, isPageError } from "@/utils/pageResponse";
import { Page } from "@/views/Page";

interface SlugPageProps {
    params: Promise<{ slug?: string[] }>;
    searchParams: Promise<Record<string, string | string[] | undefined>>;
}

function getPath(slug?: string[]) {
    return slug?.length ? `/${slug.join("/")}` : "/";
}

export async function generateMetadata({
    params,
}: SlugPageProps): Promise<Metadata> {
    const { slug } = await params;
    const pageContent = await getDotCMSPage(getPath(slug));

    if (isPageError(pageContent)) {
        return { title: "Error" };
    }

    return { title: getPageTitle(pageContent, "Not Found") };
}

export default async function Home({ params, searchParams }: SlugPageProps) {
    const { slug } = await params;
    const sp = await searchParams;
    const path = getPath(slug);
    const pageContent = await getDotCMSPage(path);
    const insideUVE = isRequestFromUVE(sp);

    if (isPageError(pageContent)) {
        if (insideUVE) {
            const { graphql } = pageContent;
            return <Page pageContent={graphql ? { graphql } : undefined} />;
        }

        return <ErrorPage error={{ status: getErrorStatus(pageContent.error) }} />;
    }

    const vanityUrl = pageContent.pageAsset?.vanityUrl;
    const action = vanityUrl?.action ?? 0;

    if (action > 200 && vanityUrl?.forwardTo) {
        redirect(vanityUrl.forwardTo);
    }

    if (!pageContent.pageAsset && !insideUVE) {
        return <NotFound />;
    }

    return <Page pageContent={pageContent} />;
}
