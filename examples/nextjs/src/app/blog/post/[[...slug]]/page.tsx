import type { Metadata } from "next";
import { isRequestFromUVE } from "@dotcms/uve";
import { redirect } from "next/navigation";

import NotFound from "@/app/not-found";
import { DetailPage } from "@/views/DetailPage";
import { getDotCMSPage } from "@/utils/getDotCMSPage";
import { isPageError } from "@/utils/pageResponse";

interface PostPageProps {
    params: Promise<{ slug?: string[] }>;
    searchParams: Promise<Record<string, string | string[] | undefined>>;
}

export async function generateMetadata({
    params,
}: PostPageProps): Promise<Metadata> {
    const { slug } = await params;

    try {
        const path = slug?.[0];
        const pageContent = await getDotCMSPage(`/blog/post/${path}`);

        if (isPageError(pageContent)) {
            return { title: "not found" };
        }

        const urlContentMap = pageContent.pageAsset?.urlContentMap;
        const title = urlContentMap?.title || "Page not found";

        return {
            title: `${title} - Blog`,
        };
    } catch {
        return {
            title: "not found",
        };
    }
}

export default async function Home({ params, searchParams }: PostPageProps) {
    const { slug } = await params;
    const sp = await searchParams;
    const path = slug?.[0];
    const pageContent = await getDotCMSPage(`/blog/post/${path}`);
    const insideUVE = isRequestFromUVE(sp);

    if (isPageError(pageContent)) {
        if (insideUVE) {
            const { graphql } = pageContent;
            return <DetailPage pageContent={graphql ? { graphql } : undefined} />;
        }

        return <NotFound />;
    }

    const vanityUrl = pageContent.pageAsset?.vanityUrl;
    const action = vanityUrl?.action ?? 0;

    if (action > 200 && vanityUrl?.forwardTo) {
        redirect(vanityUrl.forwardTo);
    }

    if (!pageContent.pageAsset && !insideUVE) {
        return <NotFound />;
    }

    return <DetailPage pageContent={pageContent} />;
}
