import { cache } from "react";

import { dotCMSClient } from "@/utils/dotCMSClient";
import { BlogListingPage } from "@/pages/BlogListingPage";
import { fragmentNav, navigationQuery } from "@/utils/queries";

export async function generateMetadata() {
    const { pageAsset } = await getDotCMSPage(`/blog`);
    const title = pageAsset?.page?.friendlyName || "Page not found";
    return {
        title: `${title} - Blog`
    };
}

export default async function Home() {
    const pageResponse = await getDotCMSPage(`/blog`);
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
