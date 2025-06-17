import { BlogListingPage } from "@/views/BlogListingPage";
import { getDotCMSPage } from "@/utils/getDotCMSPage";

import NotFound from '../not-found';

export async function generateMetadata() {
    try {
        const { pageAsset } = await getDotCMSPage(`/blog`);
        const title = pageAsset?.page?.friendlyName;
        return {
            title: `${title} - Blog`
        };
    } catch (e) {
        return {
            title: 'Blog - Page not found'
        };
    }
}

export default async function Home() {
    const pageResponse = await getDotCMSPage(`/blog`);

    if (!pageResponse) {
        return <NotFound />;
    }

    return <BlogListingPage {...pageResponse} />;
}
