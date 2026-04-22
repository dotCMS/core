import { redirect } from "next/navigation";
import { BlogListingPage } from "@/views/BlogListingPage";
import { ErrorPage } from "@/components/error";
import { getDotCMSPage } from "@/utils/getDotCMSPage";

import NotFound from "../not-found";

export async function generateMetadata() {
    const pageResponse = await getDotCMSPage(`/blog`);
    const title = pageResponse?.pageAsset?.page?.friendlyName;
    return { title: title ? `${title} - Blog` : "Blog - Page not found" };
}

export default async function Home() {
    const pageResponse = await getDotCMSPage(`/blog`);

    if (pageResponse?.error) {
        return <ErrorPage error={pageResponse.error} />;
    }

    const vanityUrl = pageResponse?.pageAsset?.vanityUrl;
    const action = vanityUrl?.action ?? 0;

    if (action > 200) {
        return redirect(pageResponse.pageAsset.vanityUrl.forwardTo);
    }

    if (!pageResponse) {
        return <NotFound />;
    }

    return <BlogListingPage {...pageResponse} />;
}
