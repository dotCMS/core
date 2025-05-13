import { cache } from "react";

import { dotCMSClient } from "@/utils/dotCMSClient";
import { BlogListingPage } from "@/pages/BlogListingPage";
import { fragmentNav, navigationQuery } from "@/utils/queries";
import NotFound from "../not-found";

export async function generateMetadata() {
    try {
        const { pageAsset } = await getDotCMSPage(`/blog`);
        const title = pageAsset?.page?.friendlyName;
        return {
            title: `${title} - Blog`
        };
    } catch (e) {
        return {
            title: "Blog - Page not found",
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

export const getDotCMSPage = cache(async (path) => {
    try {
        const pageData = await dotCMSClient.page.get(path, {
            graphql: {
                content: {
                    blogs: blogQuery,
                    navigation: navigationQuery,

                },
                fragments: [fragmentNav],
            },
        });
        return pageData;
    } catch (e) {
        return {
            pageAsset: null,
        };
    }
});

const blogQuery = `
    search(query: "+contenttype:Blog +live:true") {
        title
        identifier
        ... on Blog {
            inode
            image {
                fileName
            }
            urlMap
            modDate
            urlTitle
            teaser
            author {
                firstName
                lastName
                inode
            }
        }
    }
`;
